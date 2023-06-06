package com.gmail.toooo1718tyan.Player15;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

public class Tomato15Bodyguard extends Tomato15Villager {

	Agent guardedAgent;
	Set<Agent> successGuard = new HashSet<>();
	boolean deadAgentDay;
	boolean seer1Guard;

	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);

		guardedAgent = null;
		successGuard.clear();
		deadAgentDay = false;
		seer1Guard = false;
	}

	public void dayStart() {
		super.dayStart();

		List<Agent> deadAgentList = currentGameInfo.getLastDeadAgentList();
		// 護衛成功
		if (deadAgentList.isEmpty()) {
			// 護衛成功
			if (deadAgentList.isEmpty()) {
				humans.add(guardedAgent);
				successGuard.add(guardedAgent);
				successGurdeAgent = guardedAgent;
				deadAgentDay = false;
			}
		} else {
			//護衛失敗
			if(seer1Guard)
				deadAgentDay = true;
		}
	}

	public void update(GameInfo gameInfo) {
		super.update(gameInfo);
	}

	public Agent guard() {

		Agent guardCandidate = null;
		List<Agent> candidates = new ArrayList<>();

		if (guardedAgent != null && isAlive(guardedAgent) && successGuard.size() >= 1) {
			guardCandidate = guardedAgent;
			return guardCandidate;

		}

		// CO霊能を守る
		if (candidates.isEmpty()) {
			for (Agent agent : aliveOthers) {
				if (mediumCOlist.contains(agent)
						&& !fakeSeer.contains(agent)
						&& !werewolves.contains(agent)) {
					candidates.add(agent);
				}
			}
		}

		// CO占いを守る
		for (Agent agent : aliveOthers) {
			if (seerCOList.contains(agent)
					&& !fakeSeer.contains(agent)
					&& !werewolves.contains(agent)) {
				candidates.add(agent);
			}
		}

		// 推定エージェントを守る
		if (!fakeSeer.contains(estimateSeerAgent)
				&& !werewolves.contains(estimateSeerAgent)) {
			candidates.add(estimateSeerAgent);
		}
		if (!fakeSeer.contains(estimateMediumAgent)
				&& !werewolves.contains(estimateMediumAgent)) {
				candidates.add(estimateMediumAgent);
		}

		// 黒とニセ占い以外から適当に選ぶ
		if (candidates.isEmpty()) {
			for (Agent agent : aliveOthers) {
				if (!blackDivineList.contains(agent)
						&& !werewolves.contains(agent)
						&& !fakeSeer.contains(agent)) {
					candidates.add(agent);
				}
			}
		}

		//全員から
		if (candidates.isEmpty()) {
			candidates.addAll(aliveOthers);
		}

		guardCandidate = randomSelect(candidates);
		guardedAgent = guardCandidate;

		return guardCandidate;
	}
}
