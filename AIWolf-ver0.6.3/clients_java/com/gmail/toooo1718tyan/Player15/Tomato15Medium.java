package com.gmail.toooo1718tyan.Player15;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.IdentContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

public class Tomato15Medium extends Tomato15Villager {

	boolean isCameout;
	Deque<Judge> identQueue = new LinkedList<>();
	Map<Agent, Species> myIdentMap = new HashMap<>();
	boolean blackDivination = false;

	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		isCameout = false;
		identQueue.clear();
		myIdentMap.clear();
	}

	public void dayStart() {
		super.dayStart();

		Judge ident = currentGameInfo.getMediumResult();
		if (ident != null) {
			identQueue.offer(ident);
			myIdentMap.put(ident.getTarget(), ident.getResult());
		}

		for (Judge j : divinationList) {
			// 霊能結果から偽占いを見つける
			if (j.getTarget() == ident.getTarget()
					&& j.getResult() != ident.getResult()) {
				fakeSeer.add(j.getAgent());
				werewolves.add(j.getAgent());
			}
		}

		for (Judge j : identList) {
			if (j.getAgent() == me && j.getResult() == Species.WEREWOLF) {
				werewolves.add(j.getTarget());
			}
		}

		// 黒判定をくらったエージェントが吊られた
		blackDivination = false;
		for (Judge j : divinationList) {
			if (j.getDay() != (day - 1))
				continue;
			if (blackDivineList.contains(currentGameInfo.getLatestExecutedAgent()))
				blackDivination = true;
		}
	}

	public String talk() {
		if (!isCameout) {
			talkQueue.offer(new Content(new ComingoutContentBuilder(me, Role.MEDIUM)));
			isCameout = true;
		}

		if (isCameout) {
			while (!identQueue.isEmpty()) {
				Judge ident = identQueue.poll();
				talkQueue.offer(new Content(
						new IdentContentBuilder(ident.getTarget(), ident.getResult())));
			}
		}

		if (talkQueue.isEmpty())
			super.chooseVoteCandidate();

		return talkQueue.isEmpty() ? Talk.SKIP : talkQueue.poll().getText();
		//return super.talk();
	}

}
