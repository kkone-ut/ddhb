package camellia.aiwolf.demo;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import org.aiwolf.client.lib.AttackContentBuilder;
import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.ContentBuilder;
import org.aiwolf.client.lib.DivinedResultContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;
import org.aiwolf.sample.player.SampleBasePlayer;

public class CamelliaPossessed extends SampleBasePlayer {

	Agent me;
	GameInfo currentGameInfo;

	List<Agent> werewolfList = new ArrayList<>(); 		// 人狼リスト
	List<Agent> villagerList  = new ArrayList<>(); 		// 村人リスト
	List<Agent> comingoutSeerList = new ArrayList<>();	// 占い師COしたエージェントリスト
	List<Agent> estimateWerewolfList = new ArrayList<>(); // 真の人狼を人狼を推測したエージェントリスト
	List<Agent> blackList = new ArrayList<>();
	List<Agent> grayList = new ArrayList<>();
	List<Agent> votePoints = new ArrayList<>();			//投票する候補をポイントで管理(人狼はポイント低く、人間は高くする)



	boolean saidCO = false;
	int talkListHead, whisperListHead;

	Agent nextAttackAgent;
	boolean saidAttackCandidate;

	Role fakeRole = null;//by keiji
	int today;
	Deque<Judge> fakeDivinationQueue = new LinkedList<>();


	Agent werewolfAgent;//by tachi
	int Divineflag;			//1日1回占うためのフラグ

	@Override
	public void dayStart() {
		// 前日に死亡したエージェントをリストから取り除く
		List<Agent> deadAgents = currentGameInfo.getLastDeadAgentList();
		for (Agent deadAgent : deadAgents) {
			if (villagerList.contains(deadAgents)) {
				villagerList.remove(deadAgent);
			}
		}

		// 今日の発話リストですでに読み込んだ内容を覚えておく変数
		talkListHead = 0;
		whisperListHead = 0;

		nextAttackAgent = null;
		saidAttackCandidate = false;

		//1日1回占い結果を報告するためのフラグ
		Divineflag = 0;

	}

	@Override
	public void finish() {

	}

	@Override
	public String getName() {
		return "CamelliaWerewolf";
	}

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		// フィールドの初期化
		me = gameInfo.getAgent();
		comingoutSeerList.clear();
		estimateWerewolfList.clear();

		werewolfAgent = null;
		fakeRole = null;
		saidCO = false;

		// 村人リストを作成
		villagerList = new ArrayList<>(gameInfo.getAgentList());
		for (Agent werewolf : werewolfList) {
			villagerList.remove(werewolf);
		}
	}

	@Override
	public String talk() {

		// 人狼＋狂人の数 > 村人の数 になったら,狂人COしてPP
		// そもそもCOする必要ないかも
				if(werewolfAgent != null) {
		        	if(werewolfList.size() >= villagerList.size()) {
		        		// 狂人CO
		        		return new Content(new ComingoutContentBuilder(me, Role.POSSESSED)).getText();
		        	}
				}



// fakeRoleに従い、偽の役職をカミングアウトをする //2回以上、占われたり、疑われたりする時の対処をしてない、何回もカミングアウトしてしまう
		if(!saidCO) {
			saidCO =true;//COしたらtrueにする

			return new Content(new ComingoutContentBuilder(me, Role.SEER)).getText();

// 占い師騙る時
//今回は確定で占い師を騙る
//		fakeRole = Role.SEER;

/*
		if (fakeRole == Role.SEER) {
			// 偽占い結果を報告
			if(!fakeDivinationQueue.isEmpty()) {//今までに占った結果を全て発言(*占った結果は失われる)

				Judge divination = fakeDivinationQueue.poll();
				return new Content(new DivinedResultContentBuilder(divination.getTarget(),divination.getResult())).getText();

			}else {

			}
		}
*/

		}

		//占い結果が取得できなかった場合→ランダムに村人リストから白出し
		//村人が確定しているなら、黒出ししていい

		if(Divineflag == 0) {

			Divineflag++;
			return new Content(new DivinedResultContentBuilder(randomSelect(villagerList), Species.HUMAN)).getText();

		}






		return Content.OVER.getText();
	}

	@Override
	public void update(GameInfo gameInfo) {
		currentGameInfo = gameInfo;

		// GameInfo.talkListからカミングアウト・占い報告・霊媒報告を抽出
		for (int i = talkListHead; i < currentGameInfo.getTalkList().size(); i++) {
			Talk talk = currentGameInfo.getTalkList().get(i);
			Agent talker = talk.getAgent();
			if (talker == me) {	// 発言者が自分であれば除く
				continue;
			}
			Content content = new Content(talk.getText());	// 発話をparse

			switch (content.getTopic()) {

			case COMINGOUT:
				// 占い師カミングアウトの処理
				if (!comingoutSeerList.contains(talker)) {
					comingoutSeerList.add(talker);
				}
				break;
			case ESTIMATE:
				// 推測発言の処理
				// 真の人狼を人狼だと発言(ESTIMATE)したエージェントのリスト
				if (content.getRole() == Role.WEREWOLF && werewolfList.contains(content.getTarget())) {
					if (!estimateWerewolfList.contains(talker)) {
						estimateWerewolfList.add(talker);
					}
				}
				break;
			case DIVINED:
				// 占い結果報告発話の処理
				// 人狼を人狼と占ったとき -> 占い師の可能性大
				if (content.getRole() == Role.WEREWOLF && werewolfList.contains(content.getTarget())) {

				}
				// 人狼を村人と占ったとき -> 狂人の可能性大
				//狂人は一人なのでこれはいらない

				break;
			case IDENTIFIED:
				break;
			default:
				break;
			}
		}
		talkListHead = currentGameInfo.getTalkList().size();



	}

	@Override
	public Agent vote() {

	/* // 候補者リスト
				List<Agent> candidates = new ArrayList<>();

	// 生きている人狼を候補者リストに加える

				for (Agent agent : blackList) {
					if (isAlive(agent)){
						candidates.add(agent);
						}
					}
	// 候補者がいない場合は生きている灰色のプレイヤーを候補者リストに加える

				if (candidates.isEmpty()){
					for (Agent agent : grayList) {
						if (isAlive(agent)) {
							candidates.add(agent);
							}
						}
					}
	// 候補者がいない場合はnullを返す（自分以外の生存プレイヤーからランダム）

				if (candidates.isEmpty()) { return null;

				}
	// 候補者リストからランダムに投票先を選ぶ

				return randomSelect(candidates);
	*/


		if (votePoints.isEmpty()){
			for (Agent agent : grayList) {
				if (isAlive(agent)) {
					votePoints.add(agent);
				}
			}
		}



			if (nextAttackAgent != null) {
				return nextAttackAgent;
			}

			// 村人リストからランダムに選択
			return randomSelect(villagerList);


		}





	@Override
	public Agent attack() {
		if (nextAttackAgent != null) {
			return nextAttackAgent;
		}

		// 村人リストからランダムに選択
		return randomSelect(villagerList);
	}

	@Override
	public String whisper() {
		// 今日の昼に追放されたエージェントを村人リストから取り除く
		Agent executedAgent = currentGameInfo.getLatestExecutedAgent();
		if (villagerList.contains(executedAgent)) {
			villagerList.remove(executedAgent);
		}

		// 誰を襲撃したいか
		if (!saidAttackCandidate) {
			saidAttackCandidate = true;
			if (attackCandidate() != null) {
				nextAttackAgent = attackCandidate();
				ContentBuilder builder = new AttackContentBuilder(nextAttackAgent);
				return new Content(builder).getText();
			}
		} else {
			// 同意・不同意

		}
		return null;
	}

	// 引数のエージェントがまだ生きているか
	boolean isAlive(Agent agent) {
		return currentGameInfo.getAliveAgentList().contains(agent);
	}

	// 引数のリストからランダムに1つ要素を取り出す
	<T> T randomSelect(List<T> list) {
		if (list.isEmpty()) {
			return null;
		} else {
			return list.get((int) (Math.random() * list.size()));
		}
	}

	// 次の襲撃対象候補を返す
	Agent attackCandidate() {
		// 占い師カミングアウトしているエージェントを選択
		for (Agent seerCandidate : comingoutSeerList) {
			// TODO : 狂人ならリストから排除する機能の追加
			if (villagerList.contains(seerCandidate)) {
				return seerCandidate;
			}
		}

		// 真の人狼を人狼だと発言(ESTIMATE)したエージェント
		for (Agent agent : estimateWerewolfList) {
			if (villagerList.contains(agent)) {
				return agent;
			}
		}
		return null;
	}

}