package com.gmail.toooo1718tyan.Player15;

import java.util.ArrayList;
import java.util.List;

import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.VoteContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

import com.gmail.toooo1718tyan.Estimator.LogisticRegression15;
import com.gmail.toooo1718tyan.MetaStrategy.MostVoteAgent;
import com.gmail.toooo1718tyan.MetaStrategy.WinRateCalc;

public class Tomato15Possessed extends Tomato15Seer {

	int numWolves;
	boolean canPowerplay;

	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		numWolves = gameSetting.getRoleNumMap().get(Role.WEREWOLF);
		canPowerplay = false;
	}

	private Judge getFakeDivination() {
		List<Agent> candidates = new ArrayList<>();
		Agent target = null;
		Species result = Species.HUMAN;

		List<Agent> blackCandidates = new ArrayList<>();
		for (Agent agent : candidates) {
			if (werewolves.contains(agent))
				blackCandidates.add(agent);
		}

		for (Agent agent : aliveOthers) {
			if (myDivinationMap.containsKey(agent)
					|| comingoutMap.get(agent) == Role.SEER)
				continue;
			candidates.add(agent);
		}

		for (Agent agent : blackCandidates)
			candidates.remove(agent);

		for (Agent agent : estimateWerewolfs) {
			candidates.remove(agent);
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

		if (day <= 1 && rdm < 50 && nFakeWolves < (numWolves - 1)) {
			result = Species.WEREWOLF;
		} else if (day == 2 && rdm < 60 && nFakeWolves < (numWolves - 1)) {
			result = Species.WEREWOLF;
		} else if (day == 3 && rdm < 70 && nFakeWolves < (numWolves - 1)) {
			result = Species.WEREWOLF;
		} else if (day >= 4 && rdm < 80 && nFakeWolves < (numWolves - 1)) {
			result = Species.WEREWOLF;
		}

		return new Judge(day, me, target, result);
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
	}

	public void dayStart() {
		super.dayStart();

		if (day > 0) {
			// 偽占い
			Judge divination = getFakeDivination();
			if (divination != null) {
				divinationQueue.offer(divination);
				if (divination.getResult() == Species.HUMAN) {
					whiteDivineList.add(divination.getTarget());
					humans.add(divination.getTarget());
				} else {
					blackDivineList.add(divination.getTarget());
					werewolves.add(divination.getTarget());
				}
				myDivinationMap.put(divination.getTarget(), divination.getResult());
			}
		}

		// パワープレイが可能か判定
		if (!fakeSeer.isEmpty() && (maxNumWerewolves + 1) > (aliveOthers.size() - maxNumWerewolves)) {
			canPowerplay = true;
		}
	}

	@Override
	protected void chooseVoteCandidate() {
		List<Agent> candidates = new ArrayList<>(aliveOthers);

		// 黒を出されたエージェントを優先的に吊る
		List<Agent> blackCandidates = new ArrayList<>();
		for (Agent agent : candidates) {
			if (blackDivineList.contains(agent) || werewolves.contains(agent))
				blackCandidates.add(agent);
		}

		// 序盤は白は避ける
		for (Agent agent : aliveOthers) {
			if (day < 3 && whiteDivineList.contains(agent) || humans.contains(agent))
				candidates.remove(agent);
		}

		if (day >= 3) {
			for (Agent agent: fakeSeer) {
				candidates.remove(agent);
			}
			for (Agent agent : blackCandidates) {
				candidates.remove(agent);
			}
			for (Agent agent : estimateWerewolfs) {
				candidates.remove(agent);
			}
		}

		// 候補が残らなかったら全員から
		if (candidates.isEmpty())
			candidates = new ArrayList<>(aliveOthers);


		//投票先設定
		if(day <= 2) {
			voteCandidate = randomSelect(MostVoteAgent.getMostVoteTerget(candidates));
			if(voteCandidate == null)
				voteCandidate = randomSelect(aliveOthers);
		} else {
			voteCandidate = randomSelect(MostVoteAgent.getMostVoteTerget(estimateVillagers));
			if (voteCandidate == null)
				voteCandidate = randomSelect(MostVoteAgent.getMostVoteTerget(aliveOthers));
			if (voteCandidate == null)
				voteCandidate = randomSelect(aliveOthers);
		}

		// パワープレイに対応する
		for (Agent agent : aliveOthers) {
			if (canPowerplay && werewolves.contains(agent)) {
				voteCandidate = suspicionTarget.get(agent);

				talkQueue.clear();
				talkQueue.add(new Content(new ComingoutContentBuilder(me, Role.POSSESSED)));

			}
		}

		// 投票先を宣言
		if (voteCandidate != null && voteCandidate != declaredVoteCandidate) {
			talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
			declaredVoteCandidate = voteCandidate;
		}
	}
}
