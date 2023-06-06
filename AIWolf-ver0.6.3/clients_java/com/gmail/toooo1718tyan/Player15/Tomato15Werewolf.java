package com.gmail.toooo1718tyan.Player15;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.aiwolf.client.lib.AttackContentBuilder;
import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DivinedResultContentBuilder;
import org.aiwolf.client.lib.EstimateContentBuilder;
import org.aiwolf.client.lib.Operator;
import org.aiwolf.client.lib.Topic;
import org.aiwolf.client.lib.VoteContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

import com.gmail.toooo1718tyan.Estimator.LogisticRegression15;
import com.gmail.toooo1718tyan.MetaStrategy.MostVoteAgent;
import com.gmail.toooo1718tyan.MetaStrategy.WinRateCalc;

public class Tomato15Werewolf extends Tomato15BasePlayer {
	int numWolves;
	Role fakeRole;
	boolean isCameout;
	boolean isWhisperCameout;
	int whisperTurn;
	Role requestedFakeRole;
	boolean gjGuarded;
	boolean findPossessed;
	List<Agent> possessedList = new ArrayList<>();
	List<Agent> villagers;
	Map<Agent, Role> fakeRoleMap = new HashMap<>();
	Map<Agent, Agent> atackTargetMap;
	boolean canPowerplay;
	int numAliveWerewolves;
	// 占い関連
	Deque<Judge> divinationQueue = new LinkedList<>();
	Map<Agent, Species> myDivinationMap = new HashMap<>();
	List<Agent> whiteList = new ArrayList<>();
	List<Agent> blackList = new ArrayList<>();
	List<Agent> grayList;
	int talkTurn;
	int whisperListHead;
	boolean isEstimateTalk;
	List<Agent> estimateAgent = new ArrayList<>();

	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		// 初期化
		numWolves = gameSetting.getRoleNumMap().get(Role.WEREWOLF);
		werewolves = new HashSet<>(gameInfo.getRoleMap().keySet());
		humans = new HashSet<>();
		for (Agent a : aliveOthers) {
			if (!werewolves.contains(a)) {
				humans.add(a);
			}
		}
		villagers = new ArrayList<>();
		possessedList.clear();
		findPossessed = false;
		fakeRoleMap.clear();
		for (Agent agent : werewolves) {
			fakeRoleMap.put(agent, Role.VILLAGER);
		}
		atackTargetMap = new HashMap<>();
		requestedFakeRole = null;
		canPowerplay = false;
		numAliveWerewolves = 3;
		isCameout = false;
		isWhisperCameout = false;
		divinationQueue.clear();
		myDivinationMap.clear();
		whiteList.clear();
		blackList.clear();
		grayList = new ArrayList<>();
		// 騙る役職を決定する
		if (Math.random() < 0.3) {
			fakeRole = Role.SEER;
		} else {
			fakeRole = Role.VILLAGER;
		}
	}

	public void update(GameInfo gameInfo) {
		super.update(gameInfo);

		// 自分以外の占い結果は除外
		for (Judge judge : divinationList) {
			if (judge.getAgent() != me) {
				blackDivineList.remove(judge.getTarget());
				whiteDivineList.remove(judge.getTarget());
			}
		}

		villagers.clear();
		for (Agent agent : aliveOthers) {
			if (!werewolves.contains(agent) && !fakeSeer.contains(agent)) {
				villagers.add(agent);
			}
		}

		possessedList.clear();
		for (Agent agent : fakeSeer) {
			if (!werewolves.contains(agent) && isAlive(agent)) {
				possessedList.add(agent);
			}
		}
	}

	private Judge getFakeDivination() {
		List<Agent> candidates = new ArrayList<>();
		Agent target = null;
		Species result = Species.HUMAN;

		for (Agent agent : aliveOthers) {
			if (myDivinationMap.containsKey(agent)
					|| comingoutMap.get(agent) == Role.SEER)
				continue;

			candidates.add(agent);
		}

		//ターゲットでっち上げ
		if (!candidates.isEmpty()) {
			target = randomSelect(WinRateCalc.getMostWinners(candidates));
			if(target == null) target = randomSelect(candidates);
		} else {
			target = LogisticRegression15.getEstimateVillager(candidates);
			if(target == null) target = randomSelect(candidates);
		}

		//占い結果でっち上げ
		int nFakeWolves = 0;
		for (Species s : myDivinationMap.values()) {
			if (s == Species.WEREWOLF) {
				nFakeWolves++;
			}
		}

		if (day <= 1 && Math.random() < 0.5 && nFakeWolves < (numWolves - 1) && werewolves.contains(target)) {
			result = Species.WEREWOLF;
		} else if (day == 2 && Math.random() < 0.6 && nFakeWolves < (numWolves - 1) && werewolves.contains(target)) {
			result = Species.WEREWOLF;
		} else if (day == 3 && Math.random() < 0.7 && nFakeWolves < (numWolves - 1) && werewolves.contains(target)) {
			result = Species.WEREWOLF;
		} else if (day >= 4 && Math.random() < 0.8 && nFakeWolves < (numWolves - 1) && werewolves.contains(target)) {
			result = Species.WEREWOLF;
		}

		return new Judge(day, me, target, result);
	}

	public void dayStart() {
		super.dayStart();

		isEstimateTalk = false;
		estimateAgent.clear();

		// 仲間が吊られた
		Agent lastExecutedTarget = currentGameInfo.getLatestExecutedAgent();
		if (lastExecutedTarget != null) {
			if (werewolves.contains(lastExecutedTarget)) {
				numAliveWerewolves -= 1;
			}
		}

		// 護衛をされたか判定
		List<Agent> lastAttackTarget = currentGameInfo.getLastDeadAgentList();
		gjGuarded = (lastAttackTarget.isEmpty()) && (day >= 2);

		// 偽占いを実行
		if (day > 0 && fakeRole == Role.SEER) {
			Judge divination = getFakeDivination();
			if (divination != null) {
				divinationQueue.offer(divination);
				grayList.remove(divination.getTarget());
				if (divination.getResult() == Species.HUMAN) {
					whiteDivineList.add(divination.getTarget());
				} else {
					blackDivineList.add(divination.getTarget());
				}
				myDivinationMap.put(divination.getTarget(), divination.getResult());
			}
		}

		// パワープレイが可能か判定
		if (!possessedList.isEmpty() && (numAliveWerewolves + 1) > villagers.size()) {
			canPowerplay = true;
		}
		if (numAliveWerewolves > villagers.size()) {
			canPowerplay = true;
		}

		whisperTurn = -1;
		whisperListHead = 0;
		atackTargetMap.clear();
	}

	protected void chooseVoteCandidate() {
		List<Agent> candidates = new ArrayList<>(aliveOthers);

		//序盤黒を出されたエージェントを優先的に吊る
		List<Agent> blackCandidates = new ArrayList<>();
		if (day < 3) {
			for (Agent agent : candidates) {
				if (blackDivineList.contains(agent))
					blackCandidates.add(agent);
			}
		}

		// 序盤は白は避ける
		for (Agent agent : aliveOthers) {
			if (day < 3 && whiteDivineList.contains(agent))
				candidates.remove(agent);
		}

		//裏切り者も避ける
		if (!fakeSeer.isEmpty()) {
			for (Agent agent : fakeSeer)
				candidates.remove(agent);
		}

		//中盤から人狼への投票を避ける
		if (day >= 3) {
			for (Agent agent : werewolves) {
				candidates.remove(agent);
				blackCandidates.remove(agent);
			}
		}

		// 候補が残らなかったら全員から
		if (candidates.isEmpty())
			candidates = new ArrayList<>(aliveOthers);

		if(day <= 2) {
			voteCandidate = randomSelect(MostVoteAgent.getMostVoteTerget(candidates));
			if(voteCandidate == null)
				voteCandidate = randomSelect(aliveOthers);
		} else {
			voteCandidate = randomSelect(MostVoteAgent.getMostVoteTerget(villagers));
			if (voteCandidate == null)
				voteCandidate = randomSelect(MostVoteAgent.getMostVoteTerget(aliveOthers));
			if (voteCandidate == null)
				voteCandidate = randomSelect(aliveOthers);
		}

		if (canPowerplay) {
			talkQueue.offer(new Content(new ComingoutContentBuilder(me, Role.WEREWOLF)));
			voteCandidate = randomSelect(MostVoteAgent.getMostVoteTerget(candidates));
		}

		// 投票先を宣言
		if (voteCandidate != null && voteCandidate != declaredVoteCandidate) {
			talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
			declaredVoteCandidate = voteCandidate;
		}
	}

	public String talk() {
		if (fakeRole == Role.SEER) {
			// 初日にCO
			if (!isCameout) {
				talkQueue.offer(new Content(new ComingoutContentBuilder(me, Role.SEER)));
				isCameout = true;
			}

			if (isCameout) {
				while (!divinationQueue.isEmpty()) {
					Judge ident = divinationQueue.poll();
					if (ident.getTarget() == null || ident.getResult() == null)
						break;
					talkQueue.offer(new Content(
							new DivinedResultContentBuilder(ident.getTarget(), ident.getResult())));
				}
			}
		} else {
			// 初日にCO
			if (!isCameout) {
//				talkQueue.offer(new Content(new ComingoutContentBuilder(me, Role.VILLAGER)));
				isCameout = true;
			}
		}

		// パワープレイに対応する
		for (Agent agent : werewolves) {
			if (canPowerplay && comingoutMap.get(agent) == Role.WEREWOLF) {
				voteCandidate = suspicionTarget.get(agent);
			}
		}

		if (talkQueue.isEmpty())
			chooseVoteCandidate();

		return talkQueue.isEmpty() ? Talk.SKIP : talkQueue.poll().getText();
	}

	protected void whisperFakeRole() {
		if (whisperTurn == 0) {
			whisperQueue.offer(new Content(new ComingoutContentBuilder(me, fakeRole)));
		} else {
			Map<Role, Integer> fakeRoleCount = new HashMap<>();
			for (Role role : fakeRoleMap.values()) {
				Integer count = fakeRoleCount.get(role);
				if (count == null)
					count = 0;
				fakeRoleCount.put(role, count + 1);
			}

			// 役職騙りの重複があるか
			if (fakeRole != Role.VILLAGER && fakeRoleCount.get(fakeRole) >= 2) {
				// 自分が騙りをやめる
				if (requestedFakeRole == Role.VILLAGER || Math.random() < 0.75) {
					fakeRole = Role.VILLAGER;
					whisperQueue.clear();
					whisperQueue.offer(new Content(new ComingoutContentBuilder(me, fakeRole)));
				}
			}

			// 誰も占いの騙りがいなければ調整
			if (fakeRoleCount.get(Role.SEER) == null) {
				// 自分が騙る
				if (requestedFakeRole == Role.SEER || Math.random() < 0.25) {
					fakeRole = Role.SEER;
					whisperQueue.clear();
					whisperQueue.offer(new Content(new ComingoutContentBuilder(me, fakeRole)));
				}
			}
		}
	}

	protected void chooseAttackVoteCandidate() {
		List<Agent> candidates = new ArrayList<>();

		if (declaredAttackVoteCandidate != null) {
			for (Agent agent : aliveOthers) {
				if (atackTargetMap.containsKey(agent)) {
					attackVoteCandidate = agent;
					declaredAttackVoteCandidate = attackVoteCandidate;
					whisperQueue.offer(new Content(new AttackContentBuilder(attackVoteCandidate)));
					break;
				}
			}
			return;
		}

		// 役職候補を優先的に襲う
		for (Agent agent : villagers) {
			if (comingoutMap.get(agent) == Role.BODYGUARD) {
				candidates.add(agent);
			}
		}

		if (candidates.isEmpty() && !gjGuarded) {
			for (Agent agent : villagers) {
				if (gjGuarded && currentGameInfo.getLastDeadAgentList().contains(agent)) {
					continue;
				}
				if (seerCOList.contains(agent) && isAlive(agent)) {
					candidates.add(agent);
				}
			}
		}
		if (candidates.isEmpty()) {
			for (Agent agent : villagers) {
				if (gjGuarded && currentGameInfo.getLastDeadAgentList().contains(agent))
					continue;
				if (mediumCOlist.contains(agent) && isAlive(agent)) {
					candidates.add(agent);
				}
			}
		}
		if (candidates.isEmpty())
			candidates.addAll(villagers);
		if (candidates.isEmpty())
			candidates.addAll(humans);
		if (!candidates.isEmpty()) {
			if(attackVoteCandidate == null) attackVoteCandidate = randomSelect(WinRateCalc.getMostWinners(candidates));
			if(attackVoteCandidate == null) attackVoteCandidate = randomSelect(candidates);

			for (Agent agent : candidates) {
				if (atackTargetMap.containsKey(agent)) {
					attackVoteCandidate = agent;
					declaredAttackVoteCandidate = attackVoteCandidate;
					break;
				}
			}

			whisperQueue.offer(new Content(new AttackContentBuilder(attackVoteCandidate)));
		}
	}

	public String whisper() {
		whisperTurn++;
		requestedFakeRole = null;

		// 囁きの処理
		whisperList = currentGameInfo.getWhisperList();
		for (int i = whisperListHead; i < whisperList.size(); i++) {
			Talk whisper = whisperList.get(i);
			Agent talker = whisper.getAgent();
			Content content = new Content(whisper.getText());
			Topic topic = content.getTopic();

			switch (topic) {
			case ATTACK:
				atackTargetMap.put(talker, content.getTarget());
				break;
			case COMINGOUT:
				fakeRoleMap.put(talker, content.getRole());
				break;
			case OPERATOR:
				if (content.getOperator() == Operator.REQUEST && content.getTarget() == me) {
					for (Content c : content.getContentList()) {
						if (c.getTopic() == Topic.COMINGOUT) {
							Role role = c.getRole();
							requestedFakeRole = role;
						}
					}
				}
				break;
			default:
				break;
			}
		}
		whisperListHead = whisperList.size();

		// 狂人を見つけた
		if (!findPossessed && possessedList.size() == 1) {
			whisperQueue.offer(new Content(new EstimateContentBuilder(possessedList.get(0), Role.POSSESSED)));
			findPossessed = true;
		}

		if (day == 0)
			whisperFakeRole();

		if (day > 0 && attackVoteCandidate == null)
			chooseAttackVoteCandidate();

		return whisperQueue.isEmpty() ? Talk.SKIP : whisperQueue.poll().getText();
	}

	public Agent attack() {
		return attackVoteCandidate;
	}

}
