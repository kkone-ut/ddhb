package com.gmail.toooo1718tyan.Player5;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DivinedResultContentBuilder;
import org.aiwolf.client.lib.RequestContentBuilder;
import org.aiwolf.client.lib.VoteContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

import com.gmail.toooo1718tyan.Estimator.LogisticRegression5Werewolf;
import com.gmail.toooo1718tyan.MetaStrategy.WinRateCalc;

public class Tomato5Seer extends Tomato5BasePlayer {

	boolean isDivined;
	boolean Divinedresult;
	Agent werewolf;
	Agent notWerewolf;
	Agent possessed;
	Judge divination;
	Deque<Judge> divinationQueue = new LinkedList<>();
	Map<Agent, Species> myDivinationMap = new HashMap<>();
	List<Agent> whiteList = new ArrayList<>();
	List<Agent> blackList = new ArrayList<>();
	Agent lieBlackResultAgent;

	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		divinationQueue.clear();
		myDivinationMap.clear();
		whiteList.clear();
		blackList.clear();
		lieBlackResultAgent = null;
		werewolf = null;
		notWerewolf = null;
		possessed = null;
	}

	public void dayStart() {
		super.dayStart();
		isDivined = false;
		Divinedresult = false;
		divination = currentGameInfo.getDivineResult();

		if (divination != null) {
			if (divination.getResult() == Species.HUMAN) {
				whiteList.add(divination.getTarget());
			} else {
				blackList.add(divination.getTarget());
				Divinedresult = true;
			}
			myDivinationMap.put(divination.getTarget(), divination.getResult());
		}

		//即占いCO
		if (day == 1) {
			talkQueue.offer(new Content(new ComingoutContentBuilder(me, Role.SEER)));
			comingoutMap.put(me, Role.SEER);
		}
		if (day == 2) {
			werewolf = randomSelect(blackList);

			if (werewolf == null) {
				for (Agent agent : aliveOthers) {
					if (whiteList.contains(agent))
						continue;
					werewolf = agent;
				}
			}
			notWerewolf = null;
			for (Agent agent : aliveOthers) {
				if (agent == werewolf)
					continue;
				notWerewolf = agent;
			}
			voteCandidate = werewolf;
			if (numFirstCo == 1) {
				// 狼が自分を噛んでこない理由がないので、2日目生存は絶望的
				// 生存者内訳：占村狼 or 占狂狼
				if(estimatePossessedAgent == notWerewolf) {
					fakeWerewolf(werewolf, notWerewolf);
				} else if (estimateVillagerAgent1 == notWerewolf || estimateVillagerAgent2 == notWerewolf) {
					talkQueue.offer(new Content(new DivinedResultContentBuilder(werewolf, Species.WEREWOLF)));
				}
			} else if (numFirstCo == 2) {
				// 真狂陣形or占村狼
				if (numAliveCo == 1) {
					// 狂人が噛まれた
					// 生存者内訳：占村狼
					talkQueue.offer(new Content(new DivinedResultContentBuilder(werewolf, Species.WEREWOLF)));
				} else {
					// 狼は狂人噛みを避けて村人を噛んだ or 狂が占いCOせず，狼のみが占いCO
					// 占狂狼, 占村狼
					if (seerCOList.contains(werewolf)) {
						// 狼が占いCOをした
						// 狂人のふりをする
						// 占狂狼, 占村狼
						talkQueue.offer(new Content(new DivinedResultContentBuilder(notWerewolf, Species.WEREWOLF)));
						fakePossessed(werewolf, notWerewolf);
					} else {
						// 狼は狂人噛みを避けて村人を噛んだ
						// 狼のふりをする
						// 占狂狼
						fakeWerewolf(werewolf, notWerewolf);
					}
				}
			} else {
				// 真狂狼陣形
				if (numAliveCo == 1) {
					// 狼占村
					talkQueue.offer(new Content(new DivinedResultContentBuilder(werewolf, Species.WEREWOLF)));
				} else if (numAliveCo == 2) {
					// 狂が吊られた
					// 生存者内訳：占村狼
					talkQueue.offer(new Content(new DivinedResultContentBuilder(werewolf, Species.WEREWOLF)));
				} else {
					// 生存者内訳：占狂狼
					fakeWerewolf(werewolf, notWerewolf);
				}
			}
		}
	}

	private void fakePossessed(Agent werewolf, Agent notWerewolf) {
		talkQueue.offer(new Content(new ComingoutContentBuilder(me, Role.POSSESSED)));
		talkQueue.offer(new Content(new VoteContentBuilder(notWerewolf)));
		talkQueue.offer(new Content(
				new RequestContentBuilder(
						werewolf, new Content(
								new VoteContentBuilder(notWerewolf)))));
		lieBlackResultAgent = werewolf;
	}

	private void fakeWerewolf(Agent werewolf, Agent notWerewolf) {
		talkQueue.offer(new Content(new ComingoutContentBuilder(me, Role.WEREWOLF)));
		talkQueue.offer(new Content(new VoteContentBuilder(werewolf)));
		talkQueue.offer(new Content(
				new RequestContentBuilder(
						null, new Content(
								new VoteContentBuilder(werewolf)))));
		lieBlackResultAgent = werewolf;
	}

	public String talk() {
		List<Agent> currentAgent = super.currentGameInfo.getAliveAgentList();
		currentAgent.remove(currentGameInfo.getAgent());
		if (day == 1) {
			if(turn >= 1) {
				//占い結果が人狼かどうか
				if (!isDivined && talkQueue.isEmpty() && Divinedresult) {
					//占い結果：人狼判定
					if (blackList.size() > 0) {
						lieBlackResultAgent = blackList.get(0);
						talkQueue.offer(new Content(new DivinedResultContentBuilder(lieBlackResultAgent, Species.WEREWOLF)));
						isDivined = true;
						talkQueue.offer(new Content(new VoteContentBuilder(lieBlackResultAgent)));
					}
				} else if (!isDivined && talkQueue.isEmpty() && !Divinedresult) {
					//占い結果：人間判定
					for (Agent agent : whiteList)
						currentAgent.remove(agent);

					if(numFirstCo <= 1) {

					} else if(numFirstCo == 2) {
						for(Agent agent : aliveOthers) {
							if(comingoutMap.get(agent) == Role.SEER) {
								currentAgent.remove(agent);
							}
						}
					} else if(numFirstCo >= 3) {
						for(Agent agent : aliveOthers) {
							if(comingoutMap.get(agent) != Role.SEER) {
								currentAgent.remove(agent);
							}
						}
					}
					lieBlackResultAgent = LogisticRegression5Werewolf.numWerewolf5Estimator(currentAgent);
					if(lieBlackResultAgent == null) lieBlackResultAgent = randomSelect(currentAgent);
					talkQueue.offer(new Content(new DivinedResultContentBuilder(lieBlackResultAgent, Species.WEREWOLF)));
					isDivined = true;
					talkQueue.offer(new Content(new VoteContentBuilder(lieBlackResultAgent)));
				}
			}
		} else if (day == 2) {

		}

		return super.talk();
	}

	protected void chooseVoteCandidate() {
		if (day == 1) {
			voteCandidate = lieBlackResultAgent;
		} else if (day == 2) {
			voteCandidate = lieBlackResultAgent;
		}
	}

	public Agent divine() {
		List<Agent> candidates = new ArrayList<>();
		List<Agent> currentAgent = super.currentGameInfo.getAliveAgentList();
		currentAgent.remove(currentGameInfo.getAgent());
		Agent divineAgent = null;
		if (day >= 1) {
			if (!blackList.isEmpty()) {
				currentAgent.remove(blackList.get(0));
				divineAgent = randomSelect(WinRateCalc.getMostWinners(currentAgent));
				if(divineAgent == null) divineAgent = randomSelect(currentAgent);
				return divineAgent;
			}
			if (seerCOList.size() == 1) {
				currentAgent.remove(seerCOList.get(0));
				for (Agent a : currentAgent) {
					if (!myDivinationMap.containsKey(a) && isAlive(a)) {
						candidates.add(a);
					}
				}
				divineAgent = randomSelect(WinRateCalc.getMostWinners(candidates));
				if(divineAgent == null) divineAgent = randomSelect(candidates);
				return divineAgent;
			} else if (seerCOList.size() > 1) {
				for (Agent a : seerCOList) {
					if (!myDivinationMap.containsKey(a) && isAlive(a)) {
						candidates.add(a);
					}
				}
				divineAgent = randomSelect(WinRateCalc.getMostWinners(candidates));
				if(divineAgent == null) divineAgent = randomSelect(candidates);
				return divineAgent;
			}
		}
		for (Agent a : aliveOthers) {
			if (!myDivinationMap.containsKey(a)) {
				candidates.add(a);
			}
		}
		if (candidates.isEmpty()) {
			return null;
		}
		return randomSelect(candidates);
	}

}
