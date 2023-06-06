package org.aiwolf.daisyo;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.Topic;
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

// TODO: VILLAGERでだけ話すやつを50ターン目ぐらいからWEREWOLFで話しだす．
// TODO: 同時発話じゃなくなった事による戦略のチェンジ
// TODO: 現状の15人環境内での発話状況の確認

public class BasePlayer implements Player {

	boolean DEBUG = false;

	static int gamenum = 0;
	static int gamecnt = 0;

	int numAgents;
	Map<Integer, Agent> intToAgent = new HashMap<Integer, Agent>();
	Map<Role, Integer> roleint = new TreeMap<Role, Integer>();
	AgentInfo agents[];
	StateHolder sh;
	ArrayList<GameData> gamedata = new ArrayList<GameData>();
	Agent me;
	int myidx;

	static int wincnt[];
	static int wincntbyrole[][];

	static boolean isFirst = true;
	int day;
	int before = -1;
	GameInfo currentGameInfo;
	List<Judge> identList = new ArrayList<>();
	Deque<Content> talkQueue = new LinkedList<>();
	Deque<Content> whisperQueue = new LinkedList<>();
	Agent voteCandidate;
	int talkListHead;
	List<Agent> humans = new ArrayList<>();
	List<Agent> werewolves = new ArrayList<>();
	static double[][][][][] af;
	static double[][][][] agentScore;

	static int[][][] agentkoudou;

	static int[][][][] agentkoudou2;
	static int[][][] agentkoudou2cur;

	static int[] agentcnt;
	static int N_af = Util.ACT_NUM;
	static int rs;
	static int pred = 0;

	int max_day = 19;
	int max_turn = 7;

	Random rnd = new Random();

	//
	protected void updateState(StateHolder _sh) {
		boolean condition = false;
		if (numAgents == 5) {
			if ((day == 1 || day == 2) && sh.gamestate.turn >= 2 && sh.gamestate.turn <= 5) {
				condition = true;
			}
		}
		else {
			if (day < max_day && sh.gamestate.turn >= 2 && sh.gamestate.turn <= 6) {
				condition = true;
			}
		}

		if (condition) {
			// naze
			int turn = _sh.gamestate.turn - 2;

			for (int r = 0; r < rs; ++r) {
				for (int i = 0; i < numAgents; ++i) {
					if (sh.gamestate.agents[i].isAlive) {
						_sh.scorematrix.scores[i][r][i][r] += Util.nlog(agentScore[day][turn][i][r]);
					}
				}
			}
			_sh.update();
		}
	}


	protected int chooseMostLikelyWerewolf() {
		int c = -1;
		double mn = -1e9;
		for (int i = 0; i < numAgents; ++i) {
			if (i != myidx && sh.gamestate.agents[i].isAlive) {
				double score = sh.rp.getProb(i, Util.WEREWOLF) + sh.gamestate.cnt_vote(i) * 0.0001;
				if (mn < score) {
					mn = score;
					c = i;
				}
			}
		}
		return c;
	}


	protected int chooseMostLikelyExecuted(double th) {
		int c = -1;
		double mn = th;
		for (int i = 0; i < numAgents; ++i) {
			if (i != myidx && sh.gamestate.agents[i].isAlive) {
				double score = sh.gamestate.cnt_vote(i) + sh.rp.getProb(i, Util.WEREWOLF);
				if (mn < score) {
					mn = score;
					c = i;
				}
			}
		}
		return c;
	}

	// 村人っぽいプレイヤ
	protected int chooseMostLikelyVillager(boolean[] divined) {
		int c = -1;
		double mn = -1;
		for (int i = 0; i < numAgents; ++i) {
			if (i != myidx && sh.gamestate.agents[i].isAlive && !divined[i]) {
				double score = sh.rp.probHuman(i);
				if (mn < score) {
					mn = score;
					c = i;
				}
			}
		}
		return c;
	}

	protected int getAliveAgentsCount() { return currentGameInfo.getAliveAgentList().size(); }
	protected boolean isALive(Agent agent) { return currentGameInfo.getStatusMap().get(agent) == Status.ALIVE; }
	protected boolean isHuman(Agent agent) { return humans.contains(agent); }
	protected boolean isWerewolf(Agent agent) { return werewolves.contains(agent); }
	protected <T> T randomSelect(List<T> list) {
		if (list.isEmpty()) return null;
		else return list.get((int)(Math.random() * list.size()));
	}

	// OK
	protected <T> T randomWeightSelect(List<T> list, List<Double> prob) {
		double per = rnd.nextDouble();
		double sum = 0;
		for (int i = 0; i < prob.size(); ++i) {
			if (sum <= per && per < sum + prob.get(i)) {
				return list.get(i);
			}
			sum += prob.get(i);
		}
		try {
			return list.get(list.size()-1);
		} catch (Exception e) {
			return null;
		}
	}


	private void addExecutedAgent(Agent executedAgent) {
		if (executedAgent != null) {
			gamedata.add(new GameData(DataType.EXECUTED, day, -1, -1, executedAgent.getAgentIdx()-1, false));
		}
	}
	private void addKilledAgent(Agent killedAgent) {
		if (killedAgent != null) {
			gamedata.add(new GameData(DataType.KILLED, day, -1, -1, killedAgent.getAgentIdx()-1, false));
		}
	}

	protected void chooseVoteCandidate() {}
	protected Agent chooseVote() { return null; }
	protected String chooseTalk() { return Talk.SKIP; }
	protected String chooseWhisper() { return Talk.SKIP; }
	protected void chooseAttackVoteCandidate() {}
	protected Agent attackVote() { return null; }


	@Override
	public String getName() {
		return "daisyo";
	}

	protected void init() { }

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		try {
			numAgents = gameInfo.getAgentList().size();
			if (isFirst) {
				isFirst = false;
				new Util();

				if (numAgents == 5) rs = 4;
				else rs = 6;

				wincnt = new int[numAgents];
				wincntbyrole = new int[numAgents][rs];

				af = new double[max_day][max_turn][numAgents][N_af][rs];
				agentScore = new double[max_day][max_turn][numAgents][rs];
				agentkoudou = new int[max_day][max_turn][numAgents];
				agentkoudou2 = new int[max_day][max_turn][numAgents][100];
				agentkoudou2cur = new int[max_day][max_turn][numAgents];
				agentcnt = new int[rs];
				for (int d = 0; d < max_day; ++d) {
					for (int t = 0; t < max_turn; ++t) {
						for (int i = 0; i < numAgents; ++i) {
							for (int j = 0; j < N_af; ++j) {
								for (int k = 0; k < rs; ++k) {
									af[d][t][i][j][k] = 0.1;
								}
							}
						}
					}
				}

				/* 事前知識？ */
				if (numAgents == 5) {
					agentcnt[Util.VILLAGER] = 2;
					agentcnt[Util.SEER] = 1;
					agentcnt[Util.POSSESSED] = 1;
					agentcnt[Util.WEREWOLF] = 1;

					for (int i = 0; i < numAgents; ++i) {
						af[1][0][i][Util.ACT_CO_SEER][Util.SEER] 		= 5;
						af[1][0][i][Util.ACT_CO_SEER][Util.POSSESSED] 	= 5;
						af[1][0][i][Util.ACT_CO_SEER][Util.WEREWOLF] 	= 4;
					}
				}
				else {
					agentcnt[Util.VILLAGER] = 8;
					agentcnt[Util.SEER] = 1;
					agentcnt[Util.BODYGUARD] = 1;
					agentcnt[Util.MEDIUM ] = 1;
					agentcnt[Util.POSSESSED] = 1;
					agentcnt[Util.WEREWOLF] = 1;

					for (int i = 0; i < numAgents; ++i) {
						af[1][0][i][Util.ACT_CO_SEER][Util.SEER] 		= 4;
						af[1][0][i][Util.ACT_CO_SEER][Util.POSSESSED] 	= 4;
						af[1][0][i][Util.ACT_CO_SEER][Util.WEREWOLF] 	= 2;
						af[1][0][i][Util.ACT_CO_MEDIUM][Util.MEDIUM] 	= 4;
					}

				}
				init();
			}


			for (int d = 0; d < max_day; ++d) {
				for (int t = 0; t < max_turn; ++t) {
					for (int i = 0; i < numAgents; ++i) {
						agentkoudou[d][t][i] = Util.ACT_NONE;
						//					for (int j = 0; j < 100; ++j) {
						//						agentkoudou2cur[d][t][i] = 1;
						//						agentkoudou2[d][t][i][agentkoudou2cur[d][t][i]] = Util.ACT_NONE;
						//					}
					}
				}
			}

			roleint.put(Role.WEREWOLF, 0);
			roleint.put(Role.VILLAGER, 1);
			roleint.put(Role.SEER, 2);
			roleint.put(Role.POSSESSED, 3);
			roleint.put(Role.MEDIUM, 4);
			roleint.put(Role.BODYGUARD, 5);
			gamenum = gamecnt;
			day = -1;
			me = gameInfo.getAgent();
			myidx = me.getAgentIdx() - 1;
			pred = -1;

			for (Agent a : gameInfo.getAgentList()) {
				intToAgent.put(a.getAgentIdx() - 1, a);
			}

			agents = new AgentInfo[numAgents];
			for (int i = 0; i < numAgents; ++i) {
				agents[i] = new AgentInfo();
			}

			for (Agent a : gameInfo.getAgentList()) {
				int idx = a.getAgentIdx() - 1;
				agents[idx].isAlive = true;
				agents[idx].idx = idx;
			}

			identList.clear();
			humans.clear();
			werewolves.clear();
		}
		catch (Exception e) {
			;
		}
	}

	@Override
	public void update(GameInfo gameInfo) {
		try {
			currentGameInfo = gameInfo;

			for (int i = 0; i < numAgents; ++i) {
				agents[i].isAlive = false;
			}
			for (Agent a : gameInfo.getAliveAgentList()) {
				int idx = a.getAgentIdx() - 1;
				agents[idx].isAlive = true;
			}
			addExecutedAgent(currentGameInfo.getLatestExecutedAgent());

			// 同時発話ではなくなった
			// つまり，一番最初に発話できることもあれば，できないこともある
			for (int i = talkListHead; i < currentGameInfo.getTalkList().size(); ++i) {
				Talk talk = currentGameInfo.getTalkList().get(i);

				Agent talker = talk.getAgent();
				int day = talk.getDay();
				int turn = talk.getTurn();
				int italker = talker.getAgentIdx()-1;
				Content content = new Content(talk.getText());
				if (content.getSubject() == Content.UNSPEC) {
					content = replaceSubject(content, talker);
				}

				parseSentence(content, talker, day, turn, italker);
			}
			talkListHead = currentGameInfo.getTalkList().size();
		}
		catch (Exception e) {
			;
		}
	}

	@Override
	public void dayStart() {
		try {
			if (day != currentGameInfo.getDay()) {
				day = currentGameInfo.getDay();
				before = -1;

				List<Vote> votelist = currentGameInfo.getLatestVoteList();
				for (Vote v : votelist) {
					gamedata.add(new GameData(DataType.VOTE, day, -1, v.getAgent().getAgentIdx()-1, v.getTarget().getAgentIdx()-1, false));
				}
				if (day != 0) {
					gamedata.add(new GameData(DataType.DAYCHANGE, -1, -1, -1, -1, false));
				}
				for (int i = 0; i < numAgents; ++i) {
					agents[i].votefor = -1;
					agents[i].nvotedby = 0;
				}
				talkQueue.clear();
				whisperQueue.clear();
				voteCandidate = null;
				talkListHead = 0;

				addExecutedAgent(currentGameInfo.getExecutedAgent());

				if (!currentGameInfo.getLastDeadAgentList().isEmpty()) {
					addKilledAgent(currentGameInfo.getLastDeadAgentList().get(0));
				}
			}
		}
		catch (Exception e) {
			;
		}
	}

	static int seikai = 0;
	static int all = 0;
	static int ww = 0;

	@Override
	public void finish() {
		try {
			if (gamenum == gamecnt) {
				gamecnt++;

				for (int i = 0; i < numAgents; ++i) {
					agents[i].isAlive = false;
				}
				for (Agent a : currentGameInfo.getAliveAgentList()) {
					int idx = a.getAgentIdx() - 1;
					agents[idx].isAlive = true;
				}

				Map<Agent, Role> result = currentGameInfo.getRoleMap();
				boolean werewolfWins = false;
				for (Map.Entry<Agent, Role> e : result.entrySet()) {
					agents[e.getKey().getAgentIdx()-1].role = e.getValue();
					if (e.getValue() == Role.WEREWOLF) {
						if (agents[e.getKey().getAgentIdx()-1].isAlive) {
							werewolfWins = true;
						}
					}

					int idx = e.getKey().getAgentIdx() - 1;
					for (int d = 0; d < max_day; ++d) {
						for (int t = 0; t < max_turn; ++t) {
							if (agentkoudou[d][t][idx] >= Util.ACT_INIT) {
								af[d][t][idx][agentkoudou[d][t][idx]][roleint.get(e.getValue())]++;
							}
						}
					}
				}

				if (werewolfWins) {
					ww++;
				}
				for (Map.Entry<Agent, Role> e : result.entrySet()) {
					int idx = e.getKey().getAgentIdx() - 1;
					int role = roleint.get(e.getValue());
					if ((role == Util.POSSESSED || role == Util.WEREWOLF) == werewolfWins) {
						wincnt[idx]++;
						wincntbyrole[idx][role]++;
					}
				}
				//
				//			if (agents[myidx].role == Role.VILLAGER && pred >= 0) {
				//				all++;
				//				if (agents[pred].role == Role.WEREWOLF) {
				//					seikai++;
				//				}
				//			}
				//
				//			System.err.println("A = " + agents[myidx].role);
				//			System.err.println("pred = " + pred);
				//			System.err.println(ww / 100.0);
				//			System.err.println(seikai + " " + all + " " + seikai / (double)all);
			}
		}
		catch (Exception e) {
			;
		}
	}

	@Override
	public String talk() {
		// TODO 自動生成されたメソッド・スタブ
		try {
			return chooseTalk();
		}
		catch (Exception e) {
			return null;
		}
	}

	@Override
	public String whisper() {
		// TODO 自動生成されたメソッド・スタブ
		try {
			return chooseWhisper();
		}
		catch (Exception e) {
			return null;
		}
	}

	@Override
	public Agent vote() {
		// TODO 自動生成されたメソッド・スタブ
		try {
			return chooseVote();
		}
		catch (Exception e) {
			return null;
		}
	}

	@Override
	public Agent divine() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	public Agent attack() {
		// TODO 自動生成されたメソッド・スタブ
		try {
			return attackVote();
		}
		catch (Exception e) {
			return null;
		}
	}

	@Override
	public Agent guard() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	/**
	 * 主語を省略されたtalkを扱えるようにするため，主語を発話者に置き換えるメソッド
	 * @param content
	 * @param newSubject
	 */
	static Content replaceSubject(Content content, Agent newSubject) {
		if (content.getTopic() == Topic.SKIP || content.getTopic() == Topic.OVER) {
			return content;
		}
		if (newSubject == Content.UNSPEC) {
			return new Content(Content.stripSubject(content.getText()));
		} else {
			return new Content(newSubject + " " + Content.stripSubject(content.getText()));
		}
	}

	protected void parseSentence(Content content, Agent talker, int day, int turn, int italker) {
		//
		if (day < max_day && turn < max_turn) {
			agentkoudou[day][turn][italker] = Util.ACT_INIT;
			//agentkoudou2cur[day][turn][italker] = 0;
			//agentkoudou2[day][turn][italker][agentkoudou2cur[day][turn][italker]] = Util.ACT_INIT;
		}

		switch (content.getTopic()) {
		// カミングアウト
		case COMINGOUT:

			if (roleint.containsKey(content.getRole())) {
				agents[italker].corole = content.getRole();

				gamedata.add(new GameData(DataType.CO, day, turn, italker, roleint.get(content.getRole()), false));

				if (content.getRole() == Role.VILLAGER) {
					if (day < max_day && turn < max_turn) {
						agentkoudou[day][turn][italker] = Util.ACT_CO_VILLAGER;
						//agentkoudou2[day][turn][italker][agentkoudou2cur[day][turn][italker]++] = Util.ACT_CO_VILLAGER;
					}
				}
				else if (content.getRole() == Role.SEER) {
					if (day < max_day && turn < max_turn) {
						agentkoudou[day][turn][italker] = Util.ACT_CO_SEER;
						//agentkoudou2[day][turn][italker][agentkoudou2cur[day][turn][italker]++] = Util.ACT_CO_SEER;//
					}

				}
				else if (content.getRole() == Role.MEDIUM) {
					if (day < max_day && turn < max_turn) {
						agentkoudou[day][turn][italker] = Util.ACT_CO_MEDIUM;
						//agentkoudou2[day][turn][italker][agentkoudou2cur[day][turn][italker]++] = Util.ACT_CO_MEDIUM;
					}
				}
				else if (content.getRole() == Role.POSSESSED) {
					if (day < max_day && turn < max_turn) {
						agentkoudou[day][turn][italker] = Util.ACT_CO_POSSESSED;
						//agentkoudou2[day][turn][italker][agentkoudou2cur[day][turn][italker]++] = Util.ACT_CO_POSSESSED;
					}
				}
				else if (content.getRole() == Role.WEREWOLF) {
					if (day < max_day && turn < max_turn) {
						agentkoudou[day][turn][italker] = Util.ACT_CO_WEREWOLF;
						//agentkoudou2[day][turn][italker][agentkoudou2cur[day][turn][italker]++] = Util.ACT_CO_WEREWOLF;
					}
				}
				else if (content.getRole() == Role.BODYGUARD) {
					if (day < max_day && turn < max_turn) {
						agentkoudou[day][turn][italker] = Util.ACT_CO_BODYGUARD;
						//agentkoudou2[day][turn][italker][agentkoudou2cur[day][turn][italker]++] = Util.ACT_CO_BODYGUARD;
					}
				}
			}

			break;
		// 占い報告
		case DIVINED:

			gamedata.add(new GameData(DataType.TALKDIVINED, day, turn, italker, content.getTarget().getAgentIdx()-1, content.getResult() == Species.HUMAN));

			if (day < max_turn && turn < max_turn) {
				if (content.getResult() == Species.HUMAN) {
					agentkoudou[day][turn][italker] = Util.ACT_DIVINED_WHITE;
					//agentkoudou2[day][turn][italker][agentkoudou2cur[day][turn][italker]++] = Util.ACT_DIVINED_WHITE;
				}
				else {
					agentkoudou[day][turn][italker] = Util.ACT_DIVINED_BLACK;
					//agentkoudou2[day][turn][italker][agentkoudou2cur[day][turn][italker]++] = Util.ACT_DIVINED_BLACK;
				}
			}

			break;
		case IDENTIFIED:

			identList.add(new Judge(day, talker, content.getTarget(), content.getResult()));
			gamedata.add(new GameData(DataType.ID, day, turn, italker, content.getTarget().getAgentIdx()-1, content.getResult() == Species.HUMAN));
			if (day < max_turn && turn < max_turn) {
				agentkoudou[day][turn][italker] = Util.ACT_IDENT;
				//agentkoudou2[day][turn][italker][agentkoudou2cur[day][turn][italker]++] = Util.ACT_IDENT;
//				if (content.getResult() == Species.HUMAN) {
//					agentkoudou[day][turn][italker] = Util.ACT_IDENT_WHITE;
//				}
//				else {
//					agentkoudou[day][turn][italker] = Util.ACT_IDENT_BLACK;
//				}
			}

			break;
		case VOTE:

			agents[italker].votefor = content.getTarget().getAgentIdx()-1;
			gamedata.add(new GameData(DataType.WILLVOTE, day, turn, italker, content.getTarget().getAgentIdx()-1, false));
			if (day < max_day && turn < max_turn) {
				agentkoudou[day][turn][italker] = Util.ACT_VOTE;
				//agentkoudou2[day][turn][italker][agentkoudou2cur[day][turn][italker]++] = Util.ACT_VOTE;
			}

			break;

		case ESTIMATE:

			if (content.getRole() == Role.WEREWOLF) {
				gamedata.add(new GameData(DataType.WILLVOTE, day, turn, italker, content.getTarget().getAgentIdx()-1, false));
				if (day < max_day && turn < max_turn) {
					agentkoudou[day][turn][italker] = Util.ACT_ESTIMATE_WEREWOLF;
					//agentkoudou2[day][turn][italker][agentkoudou2cur[day][turn][italker]++] = Util.ACT_ESTIMATE_WEREWOLF;
				}
			}
			else {
				if (day < max_day && turn < max_turn) {
					agentkoudou[day][turn][italker] = Util.ACT_ESTIMATE;
					//agentkoudou2[day][turn][italker][agentkoudou2cur[day][turn][italker]++] = Util.ACT_ESTIMATE;
				}
			}
//			else if (content.getRole() == Role.VILLAGER) {
//				if (day < max_day && turn < max_turn) {
//					agentkoudou[day][turn][italker] = Util.ACT_ESTIMATE_VILLAGER;
//				}
//			}
//			else if (day < max_turn && turn < max_turn) {
//				agentkoudou[day][turn][italker] = Util.ACT_ESTIMATE;
//			}

			break;
		case SKIP:
			if (day < max_turn && turn < max_turn) {
				agentkoudou[day][turn][italker] = Util.ACT_SKIP;
				//agentkoudou2[day][turn][italker][agentkoudou2cur[day][turn][italker]++] = Util.ACT_SKIP;
			}
			break;
		case OVER:
			if (day < max_turn && turn < max_turn) {
				agentkoudou[day][turn][italker] = Util.ACT_OVER;
				//agentkoudou2[day][turn][italker][agentkoudou2cur[day][turn][italker]++] = Util.ACT_OVER;
			}
			break;
		// 演算子(OR,AND,NOT...)
		case OPERATOR:
			//if (agentkoudou2cur[day][turn][italker] > 80) break;
			parseOperator(content, talker, day, turn, italker);
			return;
		// それ以外はここでは無視
		default:
			if (day < max_turn && turn < max_turn) {
				agentkoudou[day][turn][italker] = Util.ACT_INIT;
				//agentkoudou2[day][turn][italker][agentkoudou2cur[day][turn][italker]++] = Util.ACT_INIT;
			}
			break;
		}

		if (day < max_day && turn < max_turn) {
			double ssum = 0;
			for (int k = 0; k < rs; ++k) {
				double sum = 0;
				for (int r = 0; r < N_af; ++r) {
					sum += af[day][turn][italker][r][k];
				}
				agentScore[day][turn][italker][k] = af[day][turn][italker][agentkoudou[day][turn][italker]][k] / sum;
				ssum += agentScore[day][turn][italker][k];
			}
			for (int k = 0; k < rs; ++k) {
				agentScore[day][turn][italker][k] /= ssum;
			}
		}

	}

	// 演算子の分析をするメソッド
	protected void parseOperator(Content content, Agent talker, int day, int turn, int italker) {
		switch (content.getOperator()) {
		case BECAUSE:
			// [0]に理由，[1]に結論が入っているので，結論だけ処理
			//parseSentence(content.getContentList().get(1), talker, day, turn, italker);
			// TODO: OPERATORを付けたTOPICの作成
			if (day < max_day && turn < max_turn) {
				agentkoudou[day][turn][italker] = Util.ACT_BECAUSE;
			}
			break;
		case DAY:
			// 特定の日付について言及しているが，内容だけ処理
			parseSentence(content.getContentList().get(0), talker, day, turn, italker);
//			if (day < max_day && turn < max_turn) {
//				agentkoudou[day][turn][italker] = Util.ACT_DAY;
//			}
			break;
			// AND(全て真)，OR(1つは真)，XOR(どちらかを主張≒どちらかが真)
		case AND:
			for (Content c : content.getContentList()) {
				parseSentence(c, talker, day, turn, italker);
				break;
			}
			break;
		case OR:
			break;
		case XOR:

//			for (Content c : content.getContentList()) {
//				parseSentence(c, talker, day, turn, italker);
//			}
//			break;
			// それ以外はここでは無視

//			if (day < max_day && turn < max_turn) {
//				agentkoudou[day][turn][italker] = Util.ACT_OPERATOR;
//			}

			break;
		default:
			break;
		}
	}
}
