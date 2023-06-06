package com.gmail.toooo1718tyan.Player15;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.Operator;
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

import com.gmail.toooo1718tyan.Estimator.FeatureCalclation;
import com.gmail.toooo1718tyan.Estimator.LogisticRegression15;
import com.gmail.toooo1718tyan.MetaStrategy.MostVoteAgent;
import com.gmail.toooo1718tyan.MetaStrategy.WinRateCalc;

/** すべての役職のベースとなるクラス */
public class Tomato15BasePlayer implements Player {

	/** このエージェント */
	protected Agent me;
	/** 役職 */
	Role myRole;
	/** 日付 */
	protected int day;
	/** talk()できるか時間帯か */
	protected boolean canTalk;
	/** whisper()できるか時間帯か */
	protected boolean canWhisper;
	/** 最新のゲーム情報 */
	protected GameInfo currentGameInfo;
	protected GameSetting gameSetting;
	/** 自分以外の生存エージェント */
	protected List<Agent> aliveOthers;
	/** 追放されたエージェント */
	protected List<Agent> executedAgents = new ArrayList<>();
	/** 殺されたエージェント */
	protected List<Agent> killedAgents = new ArrayList<>();
	/** 発言された占い結果報告のリスト */
	protected List<Judge> divinationList = new ArrayList<>();
	/** 黒出しされたエージェント */
	protected List<Agent> blackDivineList = new ArrayList<>();
	/** 白だしされたエージェント */
	protected List<Agent> whiteDivineList = new ArrayList<>();
	/** 発言された霊媒結果報告のリスト */
	protected List<Judge> identList = new ArrayList<>();
	/** 発言用待ち行列 */
	protected Deque<Content> talkQueue = new LinkedList<>();
	/** 囁き用待ち行列 */
	protected Deque<Content> whisperQueue = new LinkedList<>();
	/** 投票先候補 */
	protected Agent voteCandidate;
	/** 宣言済み投票先候補 */
	protected Agent declaredVoteCandidate;
	/** 襲撃投票先候補 */
	protected Agent attackVoteCandidate;
	/** 宣言済み襲撃投票先候補 */
	protected Agent declaredAttackVoteCandidate;
	/** カミングアウト状況 */
	protected Map<Agent, Role> comingoutMap = new HashMap<>();
	/** GameInfo.talkList読み込みのヘッド */
	protected int talkListHead;
	/** 人間リスト */
	protected Set<Agent> humans = new HashSet<>();
	/** 人狼リスト */
	protected Set<Agent> werewolves = new HashSet<>();
	/** 予言者COした人数 */
	protected Set<Agent> seerCOList = new HashSet<>();
	/** 狂人COした人数 */
	protected Set<Agent> possesseCOList = new HashSet<>();
	protected Set<Agent> mediumCOlist = new HashSet<>();
	/** 乱数変数（Boolean) */
	protected int rdm;
	Random rnd = new Random();
	protected Map<Agent, Agent> voteTarget = new HashMap<>();
	protected Map<Agent, Integer> voteCountTarget = new HashMap<>();
	protected int turn;
	/** 狂人リスト */
	protected Set<Agent> fakeSeer = new HashSet<>();
	protected List<Agent> dangerAgent = new ArrayList<>();
	/** 嘆き */
	protected List<Talk> whisperList = new ArrayList<>();
	/** 投票 */
	protected List<Vote> voteList = new ArrayList<>();
	protected Map<Agent, Boolean> voteWhite = new HashMap<>();
	protected Map<Agent, Boolean> voteBlack = new HashMap<>();
	protected boolean turn0;
	protected Map<Agent, Agent> suspicionTarget = new HashMap<>();
	protected int maxNumWerewolves;
	protected List<Agent> myBlackDivinedList = new ArrayList<>();
	protected Agent successGurdeAgent;
	protected List<Agent> werewolfCoList = new ArrayList<>();
	protected List<Agent> grayAgetnList = new ArrayList<>();
	protected Map<Agent, Integer> blackDivineCount = new HashMap<>();
	protected List<Agent> estimateWerewolfs = new ArrayList<>();
	protected List<Agent> estimateVillagers = new ArrayList<>();
	protected Agent estimateSeerAgent;
	protected Agent estimatePossessedAgent;
	protected Agent estimateMediumAgent;
	protected Agent estimateBodyguardAgent;
	protected Agent estimateWerewolfAgent;


	/** エージェントが生きているかどうかを返す */
	protected boolean isAlive(Agent agent) {
		return currentGameInfo.getStatusMap().get(agent) == Status.ALIVE;
	}

	/** エージェントが殺されたかどうかを返す */
	protected boolean isKilled(Agent agent) {
		return killedAgents.contains(agent);
	}

	/** エージェントがカミングアウトしたかどうかを返す */
	protected boolean isCo(Agent agent) {
		return comingoutMap.containsKey(agent);
	}

	/** 役職がカミングアウトされたかどうかを返す */
	protected boolean isCo(Role role) {
		return comingoutMap.containsValue(role);
	}

	/** エージェントが人間かどうかを返す */
	protected boolean isHuman(Agent agent) {
		return humans.contains(agent);
	}

	/** エージェントが人狼かどうかを返す */
	protected boolean isWerewolf(Agent agent) {
		return werewolves.contains(agent);
	}

	/** リストからランダムに選んで返す */
	protected <T> T randomSelect(List<T> list) {
		if (list.isEmpty()) {
			return null;
		} else {
			return list.get((int) (Math.random() * list.size()));
		}
	}

	public String getName() {
		return "Tomato5BasePlayer";
	}

	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		day = -1;
		me = gameInfo.getAgent();
		myRole = gameInfo.getRole();
		aliveOthers = new ArrayList<>(gameInfo.getAliveAgentList());
		aliveOthers.remove(me);
		executedAgents.clear();
		killedAgents.clear();
		divinationList.clear();
		blackDivineList.clear();
		whiteDivineList.clear();
		identList.clear();
		comingoutMap.clear();
		humans.clear();
		werewolves.clear();
		seerCOList.clear();
		possesseCOList.clear();
		mediumCOlist.clear();
		this.gameSetting = gameSetting;
		turn = -1;
		fakeSeer.clear();
		whisperList.clear();
		voteWhite.clear();
		voteBlack.clear();
		currentGameInfo = gameInfo;
		myBlackDivinedList.clear();
		werewolfCoList.clear();
		for (Agent agent : currentGameInfo.getAgentList()) {
			voteWhite.put(agent, false);
			voteBlack.put(agent, false);
		}
		maxNumWerewolves = gameSetting.getRoleNum(Role.WEREWOLF);
		successGurdeAgent = null;
		grayAgetnList.clear();
		blackDivineCount.clear();
		for(Agent a : currentGameInfo.getAliveAgentList()) {
			blackDivineCount.put(a, 0);
		}
		estimateWerewolfs.clear();
		estimateVillagers.clear();
		estimateSeerAgent = null;
		estimatePossessedAgent = null;
		estimateMediumAgent = null;
		estimateBodyguardAgent = null;
		estimateWerewolfAgent = null;

		if (myRole == Role.WEREWOLF) {
			LogisticRegression15.initProbabilityMap(aliveOthers, gameInfo, werewolves);
		} else {
			LogisticRegression15.initProbabilityMap(aliveOthers, gameInfo);
		}


	}

	public void update(GameInfo gameInfo) {
		currentGameInfo = gameInfo;
		// 初日は何もしない
		if (currentGameInfo.getDay() == 0) {
			return;
		}

		// 2回目の呼び出し以降
		// （夜限定）追放されたエージェントを登録
		addExecutedAgent(currentGameInfo.getLatestExecutedAgent());

		// GameInfo.talkListからカミングアウト・占い報告・霊媒報告を抽出
		for (int i = talkListHead; i < currentGameInfo.getTalkList().size(); i++) {
			Talk talk = currentGameInfo.getTalkList().get(i);
			Agent talker = talk.getAgent();
			if (talker == me) {
				continue;
			}
			Content content = new Content(talk.getText());

			parseSentence(content, talker);
			FeatureCalclation.updateFeature(content, talker);
		}
		talkListHead = currentGameInfo.getTalkList().size();

		//狂人判定
		for (Judge j : divinationList) {
			//黒出ししたエージェント死亡
			if (j.getResult() == Species.WEREWOLF && isKilled(j.getTarget())) {
				if(!fakeSeer.contains(j.getAgent()))
					fakeSeer.add(j.getAgent());
				if(!grayAgetnList.contains(j.getAgent()))
					grayAgetnList.add(j.getAgent());
			}
			//人狼の時，狼に白だしor狼以外に黒出し
			if (myRole == Role.WEREWOLF) {
				if (j.getResult() == Species.HUMAN && werewolves.contains(j.getTarget())
						|| j.getResult() == Species.WEREWOLF && humans.contains(j.getTarget())) {
					if(!fakeSeer.contains(j.getAgent()))
						fakeSeer.add(j.getAgent());
					if(!grayAgetnList.contains(j.getAgent()))
						grayAgetnList.add(j.getAgent());
				}
			}
			//人狼以外の時，自分に黒出し
			if (myRole != Role.WEREWOLF) {
				if (j.getResult() == Species.WEREWOLF && j.getTarget() == me) {
					if(!fakeSeer.contains(j.getAgent()))
						fakeSeer.add(j.getAgent());
					if(!grayAgetnList.contains(j.getAgent()))
						grayAgetnList.add(j.getAgent());
				}
			}
		}

		// 4回以上黒出ししたエージェント
		for(Agent agent : blackDivineCount.keySet()) {
			if(blackDivineCount.get(agent) >= 4) {
				if(!fakeSeer.contains(agent))
					fakeSeer.add(agent);
				if(!grayAgetnList.contains(agent))
					grayAgetnList.add(agent);
			}
		}

		// 囁きの処理
		whisperList = currentGameInfo.getWhisperList();

		// 投票の処理
		voteList = currentGameInfo.getVoteList();
		for (Vote vote : voteList) {
			if (whiteDivineList.contains(vote.getTarget())) {
				voteWhite.put(vote.getAgent(), true);
			}
			if (blackDivineList.contains(vote.getTarget())) {
				voteBlack.put(vote.getAgent(), true);
			}
		}

		// エージェントの各役職の確率を更新
		switch (myRole) {
			case VILLAGER:
			case POSSESSED:
			case BODYGUARD:
				LogisticRegression15.updateProbabilityMap(aliveOthers);
				break;
			case WEREWOLF:
				LogisticRegression15.updateProbabilityMap(aliveOthers, werewolves);
				break;
			case SEER:
				LogisticRegression15.updateProbabilityMap(aliveOthers, humans, werewolves);
				break;
			case MEDIUM:
				LogisticRegression15.updateProbabilityMap(aliveOthers, currentGameInfo.getMediumResult());
				break;
			default:
				break;
		}

		// 役職推定
//		if (aliveOthers.size() <= 4) {
//			estimateWerewolfs.clear();
//			estimateVillagers.clear();
//			for (Map.Entry<LogisticRegression15.Agent15Role, Agent> entry : LogisticRegression15.numPlayer15Estimator(aliveOthers, currentGameInfo).entrySet()) {
//				System.err.println(turn + " " +  entry.getValue() + " " + entry.getKey());
//				switch (entry.getKey()) {
//					case WEREWOLF1:
//					case WEREWOLF2:
//					case WEREWOLF3:
//						if (entry.getValue() != null) {
//							estimateWerewolfs.add(entry.getValue());
//						}
//						break;
//					case SEER:
//						estimateSeerAgent = entry.getValue();
//						break;
//					case POSSESSED:
//						estimatePossessedAgent = entry.getValue();
//						break;
//					case MEDIUM:
//						estimateMediumAgent = entry.getValue();
//						break;
//					case BODYGUARD:
//						estimateBodyguardAgent = entry.getValue();
//						break;
//					case VILLAGER1:
//					case VILLAGER2:
//					case VILLAGER3:
//					case VILLAGER4:
//					case VILLAGER5:
//					case VILLAGER6:
//					case VILLAGER7:
//					case VILLAGER8:
//						if (entry.getValue() != null) {
//							estimateVillagers.add(entry.getValue());
//						}
//						break;
//					default:
//						break;
//				}
//			}
//		}
		estimateWerewolfs = new ArrayList<Agent>(LogisticRegression15.getEstimateWerewolfs(aliveOthers));
		estimateVillagers = new ArrayList<Agent>(LogisticRegression15.getEstimateVillagers(aliveOthers));
		estimateSeerAgent = LogisticRegression15.getEstimateSeer(aliveOthers);
		estimatePossessedAgent = LogisticRegression15.getEstimatePossessed(aliveOthers);
		estimateMediumAgent = LogisticRegression15.getEstimateMedium(aliveOthers);
		estimateBodyguardAgent = LogisticRegression15.getEstimateBodyguard(aliveOthers);
		estimateWerewolfAgent = LogisticRegression15.getEstimateWerewolf(aliveOthers);


	}

	// 再帰的に文を解析する
	void parseSentence(Content content, Agent talker) {
		switch (content.getTopic()) {
		case COMINGOUT:
			if (content.getRole() == Role.SEER && !seerCOList.contains(content.getTarget())) {
				seerCOList.add(content.getTarget());
			}
			if (content.getRole() == Role.POSSESSED && !possesseCOList.contains(content.getTarget())) {
				possesseCOList.add(content.getTarget());
			}
			if (content.getRole() == Role.MEDIUM && !mediumCOlist.contains(content.getTarget())) {
				mediumCOlist.add(content.getTarget());
			}
			if (content.getRole() == Role.WEREWOLF && !werewolfCoList.contains(content.getTarget())) {
				werewolfCoList.add(content.getTarget());
			}
			comingoutMap.put(content.getTarget(), content.getRole());
			break;
		case DIVINED:
			if (comingoutMap.get(content.getTarget()) != Role.SEER)
				break;
			divinationList.add(new Judge(day, talker, content.getTarget(), content.getResult()));
			if (content.getResult() == Species.WEREWOLF) {
				if(blackDivineList.contains(content.getTarget())) {

				} else {
					blackDivineList.add(content.getTarget());
				}
				blackDivineCount.put(talker, blackDivineCount.get(talker) + 1);
			}
			if (content.getResult() == Species.HUMAN) {
				if(whiteDivineList.contains(content.getTarget())) {

				} else {
					whiteDivineList.add(content.getTarget());
				}
			}
			break;
		case IDENTIFIED:
			if (comingoutMap.get(content.getTarget()) != Role.MEDIUM)
				break;
			identList.add(new Judge(day, talker, content.getTarget(), content.getResult()));
			break;
		case VOTE:
			voteTarget.put(talker, content.getTarget());
			suspicionTarget.put(talker, content.getTarget());
			MostVoteAgent.updateVoteTerget(talker, content.getTarget(), currentGameInfo);
			break;
		case ESTIMATE:
			if (content.getRole() == Role.WEREWOLF)
				suspicionTarget.put(talker, content.getTarget());
			break;
		case OPERATOR:
			parseOperator(content, talker);
			if (content.getOperator() == Operator.REQUEST) {

			}
		default:
			break;
		}
	}

	// 演算子文を解析する
	void parseOperator(Content content, Agent talker) {
		switch (content.getOperator()) {
		case BECAUSE:
			parseSentence(content.getContentList().get(1), talker);
			break;
		case DAY:
			parseSentence(content.getContentList().get(0), talker);
			break;
		case AND:
		case OR:
		case XOR:
			for (Content c : content.getContentList()) {
				parseSentence(c, talker);
			}
			break;
		case REQUEST:
			for (Content c : content.getContentList()) {
				if (c.getTopic() == Topic.VOTE) {
					suspicionTarget.put(talker, content.getTarget());
				}
			}
			break;
		case INQUIRE:
			break;
		default:
			break;
		}
	}

	public void dayStart() {
		maxNumWerewolves = Math.min(3, (aliveOthers.size() + 1) / 2);
		canTalk = true;
		canWhisper = false;
		if (currentGameInfo.getRole() == Role.WEREWOLF) {
			canWhisper = true;
		}
		day = currentGameInfo.getDay();
		talkQueue.clear();
		whisperQueue.clear();
		declaredVoteCandidate = null;
		voteCandidate = null;
		declaredAttackVoteCandidate = null;
		attackVoteCandidate = null;
		talkListHead = 0;
		voteList.clear();
		whisperList.clear();
		suspicionTarget.clear();
		voteCountTarget.clear();
		// 前日に追放されたエージェントを登録
		addExecutedAgent(currentGameInfo.getExecutedAgent());
		// 昨夜死亡した（襲撃された）エージェントを登録
		if (!currentGameInfo.getLastDeadAgentList().isEmpty()) {
			addKilledAgent(currentGameInfo.getLastDeadAgentList().get(0));
		}
		Collections.shuffle(aliveOthers);
		voteTarget.clear();
		turn0 = true;
		turn = 0;
		//乱数決定;
		rdm = rnd.nextInt(100);
		List<Agent> tmpList = new ArrayList<>();
		for(Agent a: myBlackDivinedList) {
			if(currentGameInfo.getExecutedAgent() == a) {
				tmpList.add(a);
				continue;
			}
			for(Agent agent : currentGameInfo.getLastDeadAgentList()) {
				if(a == agent) {
					tmpList.add(a);
				}
			}
		}
		for(Agent agent : tmpList) {
			myBlackDivinedList.remove(agent);
		}
		for(Agent a : currentGameInfo.getAliveAgentList()) {
			voteCountTarget.put(a, 0);
		}

		// init
		FeatureCalclation.initDayFeature(currentGameInfo);
		FeatureCalclation.updateDay(currentGameInfo);
		MostVoteAgent.mostVoteAgentInit();
	}

	private void addExecutedAgent(Agent executedAgent) {
		if (executedAgent != null) {
			aliveOthers.remove(executedAgent);
			if (!executedAgents.contains(executedAgent)) {
				executedAgents.add(executedAgent);
			}
		}
	}

	private void addKilledAgent(Agent killedAgent) {
		if (killedAgent != null) {
			aliveOthers.remove(killedAgent);
			if (!killedAgents.contains(killedAgent)) {
				killedAgents.add(killedAgent);
			}
		}
	}

	/** 投票先候補を選びvoteCandidateにセットする */
	protected void chooseVoteCandidate() {
	}

	public String talk() {
		turn++;
		FeatureCalclation.updateTurn();
		if (currentGameInfo.getDay() < 1) {
			return "Over";
		}
		String returnstr = talkQueue.isEmpty() ? Talk.SKIP : talkQueue.poll().getText();
		return returnstr;
	}

	/** 襲撃先候補を選びattackVoteCandidateにセットする */
	protected void chooseAttackVoteCandidate() {
	}

	public String whisper() {
		return whisperQueue.isEmpty() ? Talk.SKIP : whisperQueue.poll().getText();
	}

	public Agent vote() {
		canTalk = false;
		return voteCandidate != null ? voteCandidate : randomSelect(aliveOthers);
	}

	public Agent attack() {
		canWhisper = false;
		canWhisper = true;
		return attackVoteCandidate;
	}

	public Agent divine() {
		return null;
	}

	public Agent guard() {
		return null;
	}

	public void finish() {
		WinRateCalc.updateWinRate(currentGameInfo);
	}

}
