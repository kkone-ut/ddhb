package com.gmail.toooo1718tyan.Player15;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DivinedResultContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

import com.gmail.toooo1718tyan.Estimator.LogisticRegression15;

public class Tomato15Seer extends Tomato15Villager {
	boolean isCameout;
	Deque<Judge> divinationQueue = new LinkedList<>();
	Map<Agent, Species> myDivinationMap = new HashMap<>();


	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);

		isCameout = false;
		divinationQueue.clear();
		myDivinationMap.clear();
	}

	public void dayStart() {
		super.dayStart();

		if (day > 0) {
			Judge divination = currentGameInfo.getDivineResult();
			if (divination != null) {
				divinationQueue.offer(divination);
				if (divination.getResult() == Species.HUMAN) {
					whiteDivineList.add(divination.getTarget());
					humans.add(divination.getTarget());
				} else {
					blackDivineList.add(divination.getTarget());
					werewolves.add(divination.getTarget());
					myBlackDivinedList.add(divination.getTarget());
				}
				myDivinationMap.put(divination.getTarget(), divination.getResult());
			}
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

		// 自分以外の占いCOは人狼陣営確定
		for (Agent agent : aliveOthers) {
			if (comingoutMap.get(agent) == Role.SEER) {
				fakeSeer.add(agent);
			}
		}
	}

	public String talk() {
		// 初日にCO
		if (!isCameout) {
			talkQueue.offer(new Content(new ComingoutContentBuilder(me, Role.SEER)));
			isCameout = true;
		}

		if (isCameout) {
			while (!divinationQueue.isEmpty()) {
				Judge divination = divinationQueue.poll();
				if (divination.getTarget() == null || divination.getResult() == null)
					break;
				talkQueue.offer(new Content(
						new DivinedResultContentBuilder(divination.getTarget(), divination.getResult())));
			}
		}
//		if (talkQueue.isEmpty())
//			super.chooseVoteCandidate();
//		return talkQueue.isEmpty() ? Talk.SKIP : talkQueue.poll().getText();
		return super.talk();
	}

	public Agent divine() {
		Agent target = null;
		List<Agent> candidates = new ArrayList<>();
		for (Agent agent : aliveOthers) {
			if (myDivinationMap.containsKey(agent)) continue;
			candidates.add(agent);
		}
		if (candidates.isEmpty())
			candidates.addAll(aliveOthers);

		// 人狼陣営の可能性が高いエージェントを占う
		List<Agent> targetList = new ArrayList<>();
		for (Agent agent: candidates) {
			if (fakeSeer.contains(agent))
				targetList.add(agent);
			if (seerCOList.contains(agent))
				targetList.add(agent);
			if (mediumCOlist.contains(agent))
				targetList.add(agent);
			if (estimateWerewolfs.contains(agent))
				targetList.add(agent);
		}

		if(!targetList.isEmpty()) {
			target = LogisticRegression15.getEstimateWerewolf(targetList);
			if(target == null) target = randomSelect(targetList);
		} else {
			target = LogisticRegression15.getEstimateWerewolf(candidates);
			if(target == null) target = randomSelect(candidates);
		}
		return target;
	}

}
