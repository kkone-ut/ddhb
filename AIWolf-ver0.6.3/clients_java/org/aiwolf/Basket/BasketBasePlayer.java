package org.aiwolf.Basket;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.aiwolf.client.lib.Content;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Player;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Status;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.data.Vote;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

// ベースプレイヤー
public class BasketBasePlayer implements Player {

	// これはなんだ？
	int winCount = 0;

	double winWeight = 1;
	double allwinWeight = 1;

	// 今何試合目か
	static int gamenum;
	static int gamecount = 0;
	
	// エージェントの数(何人村か)
	int numAgents;

	//エージェント名と数字(インデックス)を対応付けるもの ※サーバーから送られてくるエージェントのインデックスは1始まりのため、扱いやすいように0始まりに振り直し
	Map<Integer, Agent> intToAgent = new HashMap<Integer, Agent>();
	//役職と数字を対応付けるもの.人狼：0, 村人:1, 占い師:2, 狂人:3, 霊能:4, 狩人:5
	Map<Role, Integer> roleint = new TreeMap<Role, Integer>();

	AgentInfo agents[];

	Random rnd = new Random();

	StateHolder sh; //NumAgentを渡してはっきりと定義するのはここではなく、各役職の中で

	ArrayList<GameData> gamedata = new ArrayList<GameData>();
	Agent me;
	//自分のインデックス
	int meint;
	//各エージェントの勝利数
	static int wincnt[];
	//各エージェントの役職別勝利数
	static int wincntbyrole[][];
	
	//なにこれ
	int before = -1;
	boolean debug = false;
	
	//勝ち数などを、一度だけinitializeするときに使う
	static boolean first = true;
	//今、何日目か
	int day;
	GameInfo currentGameInfo;
	List<Judge> identList = new ArrayList<>();
	Deque<Content> talkQueue = new LinkedList<>();
	Deque<Content> whisperQueue = new LinkedList<>();
	Agent voteCandidate;
	int talkListHead;
	int lastTurn;
	int lastTalkTurn;
	List<Agent> humans = new ArrayList<>();
	List<Agent> werewolves = new ArrayList<>();
	static double[][][][][] af; //[day][turn][プレイヤーのインデックス][行動の数字(少し下のN_afを参照)][役職]のスコアを取っておくもの。〇日目の〇ターン目にプレイヤー〇が〇〇という行動をするとき、〇〇という役職であるスコアを記録しておくもの。試合の中で行動パターンを学習している。
	static double[][][][] agentScore; //[day][turn][プレイヤーのインデックス][役職]。〇日目の〇ターン目のプレイヤーの行動から、ある役職であるスコアを記録しておく。afから作る。
	static int[][][] agentkoudou; //[day][turn][プレイヤーのインデックス]。〇日目の〇ターン目にプレイヤーがした行動の数字を記録

	static int[] agentcnt; //役職ごとの人数を記録　
	static int N_af = 11;
	/*
	 * 読み取るtalk(行動)の種類は最大で11とする。
	 * 1:村人CO
	 * 2:占い師CO
	 * 3:霊媒師CO
	 * 4:占い結果白報告
	 * 5:占い結果黒報告
	 * 6:霊能結果報告
	 * 7:投票先宣言
	 * 8:疑う発言
	 * 9:skip
	 * 10:over(発言終了) 
	 * 0:その他
	 * */
	
	static int rs; //役職の数
	static int pred = 0; //人狼だと思うプレイヤーの番号。villager(とそれを継承するbodyguard)だけが使っている

	static double seerExecuted;//大河内追加。占い師が吊られやすいかどうか(村の傾向を見る)
	static boolean[] executedChecked;//大河内追加。データを処理したかどうかを記録

	protected boolean isValidIdx(int a) {
		//index out of rangeしないためのもの
		return a >= 0 && a < numAgents;
	}

	protected void updateState(StateHolder _sh) {
		boolean condition = false;
		if (numAgents == 5) {
			//5人村だったらtalkの3ターン目までしかアップデートしない(たぶん)
			//これは処理時間等による制約？ あるいは、単に必要なさそうだから？
			condition = ((day == 1 || day == 2) && sh.gamestate.turn <= 3 && sh.gamestate.turn >= 2); //ターンの最初はアップデートしている(StateHolder.process()内の, case:votestartを参照)ため、1ターン目は不要
		} else {
			//15人村だったら4ターン目まで
			condition = (day < max_day && sh.gamestate.turn <= 4 && sh.gamestate.turn >= 2);
		}
		if (condition) {
			int tu = _sh.gamestate.turn - 2; //NOTE: 
			for (int r = 0; r < rs; r++) { //rsは役職の数
				for (int i = 0; i < numAgents; i++)
					if (sh.gamestate.agents[i].Alive) {
						_sh.scorematrix.scores[i][r][i][r] += Util.nlog(agentScore[day][tu][i][r]);
						//nlog(a) returns -log(max(0.0001, a))
					}
			}
			_sh.update(); //もう一度役職推定をする
		}
	}

	protected int chooseMostLikelyWerewolf() {
	//最も人狼らしいプレイヤー
		int c = -1;
		double mn = -1e9; //-10^9
		for (int i = 0; i < numAgents; i++)
			if (i != meint) //自分じゃないなら
				if (sh.gamestate.agents[i].Alive) { //生きているなら
					double score = sh.rp.getProb(i, Util.WEREWOLF) + sh.gamestate.cnt_vote(i) * 0.0001; 
					//(stateholder -> roleprediction で推定した人狼の可能性) + (そのプレイヤーに投票宣言している人の数 × 0.0001)
					if (mn < score) { //最大値を取ってくる
						mn = score;
						c = i;
					}
				}
		return c;
	}

	protected int chooseMostLikelyExecuted(double th) { 
	//最も追放されそうなプレイヤー
		int c = -1;
		double mn = th;
		for (int i = 0; i < numAgents; i++)
			if (i != meint) //自分じゃないなら
				if (sh.gamestate.agents[i].Alive) { //生きているなら
					double score = sh.gamestate.cnt_vote(i) + sh.rp.getProb(i, Util.WEREWOLF);
					//(そのプレイヤーに投票宣言している人の数) + (人狼の可能性 ※0~1の間)
					if (mn < score) { //最大値を取ってくる
						mn = score;
						c = i;
					}
				}
		return c;
	}

	protected int getAliveAgentsCount() { //生存者の人数
		return currentGameInfo.getAliveAgentList().size();
	}

	protected boolean isAlive(Agent agent) { //引数に指定したプレイヤーが生きているかどうか
		return currentGameInfo.getStatusMap().get(agent) == Status.ALIVE;
	}

	protected boolean isHuman(Agent agent) { //TODO:humansの作り方要確認。引数に指定したプレイヤーが人間かどうか
		return humans.contains(agent);
	}

	protected boolean isWerewolf(Agent agent) { //TODO:werewolvesの作り方要確認。引数に指定したプレイヤーが人狼かどうか
		return werewolves.contains(agent);
	}

	protected <T> T randomSelect(List<T> list) {
	//いろいろな型のリストに対し、その要素をランダムで取得する関数(一般化した書き方)
		if (list.isEmpty()) {
			return null;
		} else {
			return list.get((int) (Math.random() * list.size()));
		}
	}

	public String getName() {
		return "MyBasePlayer";
	}

	protected void init() {

	}

	static int tmp = 0; //使い道不明
	int max_day = 10; // TODO: ここもおかしい気がする
	int max_turn = 5; //TODO:なぜmaxを5にしている？メモリの制約か？

	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		tmp++;
		numAgents = gameInfo.getAgentList().size();

		if (first) {
			first = false;
			new Util();

			if (numAgents == 5)
				rs = 4; //5人村なら役職は4種類
			else
				rs = 6; //15人村なら役職は6種類

			wincnt = new int[numAgents]; //各プレイヤーの勝数を記録
			wincntbyrole = new int[numAgents][rs]; //各プレイヤーの勝数を役職ごとに記録

			af = new double[max_day][max_turn][numAgents][N_af][rs]; //N_af = 11
			agentScore = new double[max_day][max_turn][numAgents][rs];
			agentkoudou = new int[max_day][max_turn][numAgents];
			agentcnt = new int[rs];
			for (int d = 0; d < max_day; d++) {
				for (int t = 0; t < max_turn; t++) {
					for (int i = 0; i < numAgents; i++) {
						for (int j = 0; j < N_af; j++) {
							for (int k = 0; k < rs; k++) {
								af[d][t][i][j][k] = 0.1; //afをinitialize。すべての行動のスコアをとりあえず0.1に
							}
						}
					}
				}
			}
			
			//agentcnt(役職別人数)を設定、afも一部設定
			if (numAgents == 5) {
				//5人村の場合
				agentcnt[Util.VILLAGER] = 2;
				agentcnt[Util.SEER] = 1;
				agentcnt[Util.POSSESSED] = 1;
				agentcnt[Util.WEREWOLF] = 1;
				for (int i = 0; i < numAgents; i++) {
					af[1][0][i][2][Util.SEER] = 4; //初日の最初のターンの占い師COは占い師スコア4
					af[1][0][i][2][Util.POSSESSED] = 4; //狂人スコアも4
				}

			} else {
				//15人村の場合
				agentcnt[Util.VILLAGER] = 8;
				agentcnt[Util.SEER] = 1;
				agentcnt[Util.BODYGUARD] = 1;
				agentcnt[Util.MEDIUM] = 1;
				agentcnt[Util.POSSESSED] = 1;
				agentcnt[Util.WEREWOLF] = 3;
				for (int i = 0; i < numAgents; i++) {
					af[1][0][i][2][Util.SEER] = 4; //初日の最初のターンの占い師COは占い師スコア4
					af[1][0][i][2][Util.POSSESSED] = 4; //狂人スコアも4
					af[1][0][i][3][Util.MEDIUM] = 4; //初日の最初のターンの霊能者COは霊能者スコア4
				}

			}
			init(); //なにもしない
			seerExecuted = 0; //大河内追加
		}

		for (int d = 0; d < max_day; d++) {
			for (int t = 0; t < max_turn; t++) {
				for (int i = 0; i < numAgents; i++) {
					agentkoudou[d][t][i] = -1; //agentkoudouをinitialize
				}
			}
		}
		//役職を表す数字をroleintに設定
		roleint.put(Role.WEREWOLF, 0);
		roleint.put(Role.VILLAGER, 1);
		roleint.put(Role.SEER, 2);
		roleint.put(Role.POSSESSED, 3);
		roleint.put(Role.MEDIUM, 4);
		roleint.put(Role.BODYGUARD, 5);
		gamenum = gamecount;
		day = -1;
		me = gameInfo.getAgent();
		meint = me.getAgentIdx() - 1;
		pred = -1; //人狼だと思うプレイヤーの番号を初期化。villagerの中でのみ書き換えられる

		for (Agent a : gameInfo.getAgentList()) {
			//getAgentIdx()
			intToAgent.put(a.getAgentIdx() - 1, a); //番号とプレイヤーを対応付けるintToAgentを設定
		}

		//agentsをinitialize
		agents = new AgentInfo[numAgents];
		for (int i = 0; i < numAgents; i++) {
			agents[i] = new AgentInfo();
		}
		for (Agent a : gameInfo.getAgentList()) {
			int id = a.getAgentIdx() - 1;
			agents[id].alive = true; //最初は生きている
			agents[id].index = id; //インデックスを設定
		}
		// myindex = agentToInt.get(me);

		identList.clear();
		humans.clear();
		werewolves.clear();
		System.out.println(meint + ", seerExecuted = " + seerExecuted);

		executedChecked = new boolean[numAgents];
		for (int i = 0; i < numAgents; i++) {
			executedChecked[i] = false;
		}

	}

	public void update(GameInfo gameInfo) {
		currentGameInfo = gameInfo;
		
		//生存者を更新
		for (int i = 0; i < numAgents; i++)
			agents[i].alive = false;
		for (Agent a : gameInfo.getAliveAgentList()) {
			int id = a.getAgentIdx() - 1;
			agents[id].alive = true;
		}
		//追放者を確認
		addExecutedAgent(currentGameInfo.getLatestExecutedAgent());

		//大河内追加
		if (currentGameInfo.getExecutedAgent() != null) {
			int idx = currentGameInfo.getExecutedAgent().getAgentIdx() - 1;
			if ((!executedChecked[idx]) && (agents[idx].COrole == Role.SEER)) {
				executedChecked[idx] = true;
				System.out.println("-------------seer executed-------------");
				int COseer = 0;
				for (int i = 0; i < numAgents; i++) {
					if (agents[i].COrole == Role.SEER)
						COseer++;
				}
				seerExecuted += 1 / (double)COseer;
				System.out.println(seerExecuted);
			}
		}

		//行動(発言)の解析
		for (int i = talkListHead; i < currentGameInfo.getTalkList().size(); i++) {
			Talk talk = currentGameInfo.getTalkList().get(i);
			Agent talker = talk.getAgent();
			int da = talk.getDay();
			int tu = talk.getTurn();
			int italker = talker.getAgentIdx() - 1; //発言したプレイヤーのインデックス
			Content content = new Content(talk.getText());
			if (da < max_day && tu < max_turn)
				agentkoudou[da][tu][italker] = 0; //とりあえずagentkoudouを0(その他)にしておく

			if (lastTurn < tu) {
				lastTurn = tu; //turnが変わったとき、tuを更新
				gamedata.add(new GameData(DataType.TURNSTART, day, meint, meint, false)); //GameDataに「1.タイプ：ターン開始、2.日、3.特に意味はない、4.特に意味はない、5.特に意味はない」を記録。※引数3,4,5は占い結果などの記録の時に使う
			}
			switch (content.getTopic()) {
			case COMINGOUT: //CO発言の場合

				if (roleint.containsKey(content.getRole())) {
					agents[italker].COrole = content.getRole();

					gamedata.add(new GameData(DataType.CO, day, italker, roleint.get(content.getRole()), false)); //GameDataに「1.タイプ：CO、2.日、3.発言者、4.COした役職、5.特に意味はない」を記録。
					// System.out.println("CO " + italker + " " +
					// content.getRole().toString());
					if (content.getRole() == Role.VILLAGER) {
						if (da < max_day && tu < max_turn)
							agentkoudou[da][tu][italker] = 1; //村人COは1
					} else if (content.getRole() == Role.SEER) {
						if (da < max_day && tu < max_turn)
							agentkoudou[da][tu][italker] = 2; //占い師COは2
					} else if (content.getRole() == Role.MEDIUM) {
						if (da < max_day && tu < max_turn)
							agentkoudou[da][tu][italker] = 3; //霊能者COは3
					} else {
						if (da < max_day && tu < max_turn)
							agentkoudou[da][tu][italker] = 0; //その他のCOは0
					}
				}
				break;
			case DIVINED: //占い結果報告の場合
				//System.out.println("DIVINED " + italker + " " + content.getTarget().getAgentIdx()-1-1 + " "
				//		+ content.getResult().toString());
				gamedata.add(new GameData(DataType.TALKDIVINED, day, italker, content.getTarget().getAgentIdx() - 1,
						content.getResult() == Species.HUMAN)); //GameDataに「1.タイプ：占い結果、2.日、3.発言者、4.占い対象、5.人間かどうか」を記録。
				if (da < max_day && tu < max_turn) {
					if (content.getResult() == Species.HUMAN) {
						agentkoudou[da][tu][italker] = 4; //誰かに白を出したとき、4
					} else {
						agentkoudou[da][tu][italker] = 5; //誰かに黒を出したとき、5
					}
				}
				break;
			case IDENTIFIED: //霊能結果報告の場合
				identList.add(new Judge(day, talker, content.getTarget(), content.getResult()));
				gamedata.add(new GameData(DataType.ID, day, italker, content.getTarget().getAgentIdx() - 1,
						content.getResult() == Species.HUMAN)); //GameDataに「1.タイプ：霊能結果、2.日、3.発言者、4.霊能対象、5.人間かどうか」を記録。
				if (da < max_day && tu < max_turn)
					agentkoudou[da][tu][italker] = 6; //霊能結果を伝えたとき、6 (結果の白黒は問わない)
				break;
			case VOTE: //投票先宣言の場合
				agents[italker].voteFor = content.getTarget().getAgentIdx() - 1; //そのプレイヤーのagentinfoの投票先に記録
				gamedata.add(
						new GameData(DataType.WILLVOTE, day, italker, content.getTarget().getAgentIdx() - 1, false)); //GameDataに「1.タイプ：投票先、2.日、3.発言者、4.投票先、5.特に意味はない」を記録。
				// System.out.println("vote " + italker + " " +
				// agentToInt.get(content.getTarget()));
				if (da < max_day && tu < max_turn)
					agentkoudou[da][tu][italker] = 7; //誰かに投票宣言したとき、7
				break;
			case ESTIMATE: //疑い先発言の場合
				if (content.getRole() == Role.WEREWOLF) {
					gamedata.add(
							new GameData(DataType.WILLVOTE, day, italker, content.getTarget().getAgentIdx() - 1,
									false)); //GameDataに「1.タイプ：投票先、2.日、3.発言者、4.疑う相手、5.特に意味はない」を記録。投票先宣言と同じ扱い。
				}
				// System.out.println("vote " + italker + " " +
				// agentToInt.get(content.getTarget()));
				if (da < max_day && tu < max_turn)
					agentkoudou[da][tu][italker] = 8; //誰かを疑う発言をしたとき、8。GameDataでは投票先宣言と同じ扱いをしたが、行動番号は別にしている。TODO:rolepredictionでの投票先宣言と疑い発言の違いを確認。
				break;
			case SKIP:
				if (da < max_day && tu < max_turn)
					agentkoudou[da][tu][italker] = 9; //スキップは9
				break;
			case OVER:
				if (da < max_day && tu < max_turn)
					agentkoudou[da][tu][italker] = 10; //overは10
				break;
			default:
				if (da < max_day && tu < max_turn)
					agentkoudou[da][tu][italker] = 0; //その他は0
				break;
			}
			if (da < max_day && tu < max_turn) {
				double ssum = 0;
				for (int k = 0; k < rs; k++) { //各役職について
					double sum = 0;
					for (int r = 0; r < N_af; r++) { //行動の合計を計算(あとで割って0~1にスケールするため)
						sum += af[da][tu][italker][r][k];
					}
					agentScore[da][tu][italker][k] = af[da][tu][italker][agentkoudou[da][tu][italker]][k] / sum; //プレイヤーがあるday,turnでした発言(行動)は役職kらしさがどのくらいなのかを(0~1の範囲で)計算
					//agentScore[da][tu][italker][k]*=agentcnt[k];
					ssum += agentScore[da][tu][italker][k]; //上で求めたスコアを役職ごとに合計(あとで割って0~1にスケールするため)
				}
				for (int k = 0; k < rs; k++) {
					agentScore[da][tu][italker][k] /= ssum;
				}
			}

		}
		talkListHead = currentGameInfo.getTalkList().size(); //次にtalkを読むときの開始位置を設定
	}

	public void dayStart() { //日が始まったときに呼ばれる

		if (day != currentGameInfo.getDay()) { //もし日が変わっているならば
			day = currentGameInfo.getDay(); //dayを更新
			before = -1;
			System.out.println("daystart " + day);
			List<Vote> votelist = currentGameInfo.getLatestVoteList(); //前日の投票結果を取得
			for (Vote v : votelist) {
				gamedata.add(new GameData(DataType.VOTE, day, v.getAgent().getAgentIdx() - 1,
						v.getTarget().getAgentIdx() - 1,
						false)); //gamedataに「1.タイプ：投票結果、2.日、3.投票者、4.投票先、5.特に意味はない」を記録
			}
			if (day != 0)
				gamedata.add(new GameData(DataType.DAYCHANGE, -1, -1, -1, false));
			for (int i = 0; i < numAgents; i++) {
				agents[i].voteFor = -1; //投票先をリセット
				agents[i].NvotedBy = 0; //得票数をリセット
			}
			//各変数をinitialize
			talkQueue.clear();
			whisperQueue.clear();
			voteCandidate = null;
			talkListHead = 0;
			lastTurn = -1;
			lastTalkTurn = -1;

			addExecutedAgent(currentGameInfo.getExecutedAgent()); //追放者を取得

			if (!currentGameInfo.getLastDeadAgentList().isEmpty()) {
				addKilledAgent(currentGameInfo.getLastDeadAgentList().get(0)); //犠牲者を記録
			}
		}
	}

	private void addExecutedAgent(Agent executedAgent) {
		if (executedAgent != null) {
			gamedata.add(new GameData(DataType.EXECUTED, day, -1, executedAgent.getAgentIdx() - 1, false)); //GameDataに「1.タイプ：追放、2.日、3.特に意味はない、4.追放されたプレイヤー、5.特に意味はない」を記録。
		}
	}

	private void addKilledAgent(Agent killedAgent) {
		if (killedAgent != null) {
			gamedata.add(new GameData(DataType.KILLED, day, -1, killedAgent.getAgentIdx() - 1, true)); //GameDataに「1.タイプ：襲撃、2.日、3.特に意味はない、4.襲撃されたプレイヤー、5.特に意味はない」を記録。
		}
	}

	protected void chooseVoteCandidate() {
	}

	protected Agent chooseVote() {
		return null;
	}

	protected String chooseTalk() {
		return null;
	}

	public String talk() {
		return chooseTalk();
	}

	protected void chooseAttackVoteCandidate() {
	}

	protected Agent attackVote() {
		return null;
	}

	public String whisper() { //whisperを使っていない(werewolfの中でもオーバーライドしていない)

		return Talk.SKIP;
	}

	public Agent vote() {
		return chooseVote();
	}

	public Agent attack() {
		return attackVote();
	}

	public Agent divine() {
		return null;
	}

	public Agent guard() {
		return null;
	}

	static int seikai = 0; //人狼の予想を的中させた数
	static int all = 0; //人狼を予想した総数

	static int ww = 0; //人狼陣営の勝利数

	public void finish() { //gameが終了したときに呼ばれる
		if (gamenum == gamecount) {

			gamecount++; //gameの数を増やす
			//System.out.println("finish-------------------------" + gamecount + "------------------------");

			//生存者を確認
			for (int i = 0; i < numAgents; i++)
				agents[i].alive = false;
			for (Agent a : currentGameInfo.getAliveAgentList()) {
				int id = a.getAgentIdx() - 1;
				agents[id].alive = true;
			}
			Map<Agent, Role> result = currentGameInfo.getRoleMap(); //プレイヤーと本当の役職を取得
			boolean werewolfWins = false;
			for (Map.Entry<Agent, Role> entry : result.entrySet()) {

				agents[entry.getKey().getAgentIdx() - 1].role = entry.getValue(); //agentinfoに役職を記録
				
				//人狼が生きていれば、人狼陣営勝利
				if (entry.getValue() == Role.WEREWOLF) {
					if (agents[entry.getKey().getAgentIdx() - 1].alive) {
						werewolfWins = true;
					}
				}
				
				int id = entry.getKey().getAgentIdx() - 1;
				for (int d = 0; d < max_day; d++) {
					for (int t = 0; t < max_turn; t++) {
						if (agentkoudou[d][t][id] >= 0) {
							af[d][t][id][agentkoudou[d][t][id]][roleint.get(entry.getValue())]++; //すべての行動を学習しておく。TODO:行動の分類は適切か、重み付けは必要か検討
						}
					}
				}
			}

			if (werewolfWins) {
				ww++;
			}
			
			//各プレイヤーがどの役職で勝利したかを記録
			for (Map.Entry<Agent, Role> entry : result.entrySet()) {

				int id = entry.getKey().getAgentIdx() - 1;
				int ro = roleint.get(entry.getValue());
				if ((ro == Util.POSSESSED || ro == Util.WEREWOLF) == werewolfWins) {
					wincnt[id]++;
					wincntbyrole[id][ro]++;
				}

			}
			
			//自分が村人で人狼を予想していた時、答え合わせをする(debug用と思われる)
			if (agents[meint].role == Role.VILLAGER && pred >= 0) {
				all++;
				if (agents[pred].role == Role.WEREWOLF) {
					seikai++;
				}
			}
			System.out.println(ww / 100.0);
			System.out.println(seikai + " " + all + " " + seikai / (double) all);

			//System.out.println(winWeight / allwinWeight);

		}
	}
}
