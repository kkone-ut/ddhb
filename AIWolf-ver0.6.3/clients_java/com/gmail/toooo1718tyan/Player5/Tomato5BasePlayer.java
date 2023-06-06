package com.gmail.toooo1718tyan.Player5;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.aiwolf.client.lib.AttackContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Player;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Status;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

import com.gmail.toooo1718tyan.Estimator.FeatureCalclation;
import com.gmail.toooo1718tyan.Estimator.LogisticRegression5;
import com.gmail.toooo1718tyan.MetaStrategy.MostVoteAgent;
import com.gmail.toooo1718tyan.MetaStrategy.WinRateCalc;


/** すべての役職のベースとなるクラス */
public class Tomato5BasePlayer implements Player {

	/** このエージェント */
	protected Agent me;
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
	protected List<Agent> humans = new ArrayList<>();
	/** 人狼リスト */
	protected List<Agent> werewolves = new ArrayList<>();
	/** 予言者COした人数 */
	protected List<Agent> seerCOList = new ArrayList<>();
	/** 狂人COした人数 */
	protected List<Agent> possesseCOList = new ArrayList<>();
	/** 人狼COした人数 */
	protected List<Agent> werewolfCO5List = new ArrayList<>();
	protected int numFirstCo;
	protected int numAliveCo;
	protected int numWerewolfCo;
	protected Set<Agent> firstDayCo = new HashSet<>();
	/** 乱数変数（Boolean) */
	protected boolean rdm;
	Random rnd = new Random();
	protected Map<Agent, Agent> voteTarget = new HashMap<>();
	protected int turn;
	protected Map<Agent, Integer> voteCountTarget = new HashMap<>();
	protected int myVoteCount;
	// 自分に人狼判定して来たエージェント
	protected List<Agent> blackDivineMe = new ArrayList<>();
	protected Agent day2blackDivineAgent;
	protected Agent estimateWerewolfAgent;
	protected Agent estimateSeerAgent;
	protected Agent estimatePossessedAgent;
	protected Agent estimateVillagerAgent1;
	protected Agent estimateVillagerAgent2;

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
		werewolfCO5List.clear();
		this.gameSetting = gameSetting;
		numFirstCo = 0;
		numAliveCo = 0;
		numWerewolfCo = 0;
		firstDayCo.clear();
		turn = 0;
		myVoteCount = 0;
		blackDivineMe.clear();
		day2blackDivineAgent = null;
		estimateWerewolfAgent = null;
		estimateSeerAgent = null;
		estimatePossessedAgent = null;
		estimateVillagerAgent1 = null;
		estimateVillagerAgent2 = null;

		//乱数決定
		int tmp = 0;
		tmp = rnd.nextInt(100);
		if (tmp < 80) {
			rdm = false;
		} else {
			rdm = true;
		}
	}


	protected Agent estimateVillagerAgent() {
		Agent ret = null;

		if (estimateVillagerAgent1 != null) {
			ret = estimateVillagerAgent1;
		} else if (estimateVillagerAgent2 != null) {
			ret = estimateVillagerAgent2;
		} else {

		}

		return ret;
	}

	public void update(GameInfo gameInfo) {
		currentGameInfo = gameInfo;
		// 1日の最初の呼び出しはdayStart()の前なので何もしない
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
		// 初日のCO状況
		if (day == 1) {
			numFirstCo = 0;
			for (Agent agent : currentGameInfo.getAgentList()) {
				if (comingoutMap.get(agent) == Role.SEER)
					numFirstCo++;
			}
		}
		// 生き残りCO数
		numAliveCo = 0;
		for (Agent agent : currentGameInfo.getAliveAgentList()) {
			if (comingoutMap.get(agent) == Role.SEER)
				numAliveCo++;
		}
		// 人狼COの数
		numWerewolfCo = 0;
		for(Agent agent : currentGameInfo.getAliveAgentList()) {
			if(agent == me) {
				continue;
			}
			if(comingoutMap.get(agent) == Role.WEREWOLF) {
				numWerewolfCo++;
			}
		}
		// 1日目のCO状況
		if (day <= 1) {
			for (Agent agent : currentGameInfo.getAgentList()) {
				if (comingoutMap.get(agent) == Role.SEER)
					firstDayCo.add(agent);
			}
		}

		for (Map.Entry<LogisticRegression5.Agent5Role, Agent> entry : LogisticRegression5.numPlayer5Estimator(aliveOthers, currentGameInfo).entrySet()) {
			//System.err.println(turn + " " +  entry.getValue() + " " + entry.getKey());
			switch (entry.getKey()) {
				case WEREWOLF:
					estimateWerewolfAgent = entry.getValue();
					break;
				case SEER:
					estimateSeerAgent = entry.getValue();
					break;
				case POSSESSED:
					estimatePossessedAgent = entry.getValue();
					break;
				case VILLAGER1:
					estimateVillagerAgent1 = entry.getValue();
					break;
				case VILLAGER2:
					estimateVillagerAgent2 = entry.getValue();
					break;
				default:
					break;
			}
		}

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
			if (content.getRole() == Role.WEREWOLF && !werewolfCO5List.contains(content.getTarget())) {
				if(day == 2) {
					werewolfCO5List.add(talker);
				}
			}
			comingoutMap.put(talker, content.getRole());
			break;
		case DIVINED:
			divinationList.add(new Judge(day, content.getTarget(), content.getTarget(), content.getResult()));
			if (content.getResult() == Species.WEREWOLF) {
				if(!blackDivineList.contains(content.getTarget()))
					blackDivineList.add(content.getTarget());
				// 自分に黒出し判定して来たエージェント
				if(content.getTarget() == me && !blackDivineMe.contains(talker)) {
					blackDivineMe.add(talker);
				}
				// 二日目に黒出し
				if(day > 1) {
					if(content.getTarget() != me) {
						day2blackDivineAgent = content.getTarget();
					}
				}
			}
			if (content.getResult() == Species.HUMAN) {
				if(!whiteDivineList.contains(content.getTarget()))
					whiteDivineList.add(content.getTarget());
			}
			break;
		case IDENTIFIED:
			identList.add(new Judge(day, content.getTarget(), content.getTarget(), content.getResult()));
			break;
		case VOTE:
			voteTarget.put(talker, content.getTarget());
			MostVoteAgent.updateVoteTerget(talker, content.getTarget(), currentGameInfo);
			//ターゲット投票回数計測
			int count = 0;
			if (voteCountTarget.containsKey(content.getTarget())) {
				count = voteCountTarget.get(content.getTarget());
			}
			count++;
			voteCountTarget.put(content.getTarget(), count);
			break;
		case OPERATOR:
			parseOperator(content, talker);
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
			break;
		case INQUIRE:
			break;
		default:
			break;
		}
	}

	public void dayStart() {
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
		voteCountTarget.clear();
		myVoteCount = 0;
		turn = 0;

		// 前日に追放されたエージェントを登録
		addExecutedAgent(currentGameInfo.getExecutedAgent());
		// 昨夜死亡した（襲撃された）エージェントを登録
		if (!currentGameInfo.getLastDeadAgentList().isEmpty()) {
			addKilledAgent(currentGameInfo.getLastDeadAgentList().get(0));
		}
		voteTarget.clear();

		for(Agent a : currentGameInfo.getAliveAgentList()) {
			voteCountTarget.put(a, 0);
		}

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

		chooseVoteCandidate();
		String returnstr = talkQueue.isEmpty() ? Talk.SKIP : talkQueue.poll().getText();
		return returnstr;
	}

	/** 襲撃先候補を選びattackVoteCandidateにセットする */
	protected void chooseAttackVoteCandidate() {
	}

	public String whisper() {
		chooseAttackVoteCandidate();
		if (attackVoteCandidate != null && attackVoteCandidate != declaredAttackVoteCandidate) {
			whisperQueue.offer(new Content(new AttackContentBuilder(attackVoteCandidate)));
			declaredAttackVoteCandidate = attackVoteCandidate;
		}
		return whisperQueue.isEmpty() ? Talk.SKIP : whisperQueue.poll().getText();
	}

	public Agent vote() {
		canTalk = false;
		chooseVoteCandidate();
		return voteCandidate;
	}

	public Agent attack() {
		canWhisper = false;
		chooseAttackVoteCandidate();
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
