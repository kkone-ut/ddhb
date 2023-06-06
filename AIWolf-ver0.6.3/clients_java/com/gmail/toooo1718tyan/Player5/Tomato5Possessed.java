package com.gmail.toooo1718tyan.Player5;

import java.util.ArrayList;
import java.util.List;

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

import com.gmail.toooo1718tyan.Estimator.LogisticRegression5Villager;
import com.gmail.toooo1718tyan.Estimator.LogisticRegression5Werewolf;

public class Tomato5Possessed extends Tomato5BasePlayer {

	boolean isDivind;
	boolean rdmVote;
	boolean seerPlay;
	boolean isVote;
	boolean VoteFromWerewolfCoFlag;


	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		isDivind = false;
		rdmVote = false;
		isVote = false;
		VoteFromWerewolfCoFlag = false;
	}


	private void VoteFromWerewolfCo() {
		if(numWerewolfCo == 1) {
			for(Agent agent : aliveOthers) {
				if(!werewolfCO5List.contains(agent)) {
					voteCandidate = agent;
				}
			}
			talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
		} else {
			Agent werewolf = estimateWerewolfAgent;
			for(Agent agent : aliveOthers) {
				if(agent != werewolf) {
					voteCandidate = agent;
				}
			}
		}
	}


	public void dayStart() {
		super.dayStart();
		//初日適当占いCO釣り
		if (day == 1) {
			if(numFirstCo < 2) {
				talkQueue.offer(new Content(new ComingoutContentBuilder(me, Role.SEER)));
				comingoutMap.put(me, Role.SEER);
				seerPlay = true;
			} else {
				// 人狼がすでに占いCO済み 村人偽り
				seerPlay = false;
			}
		}

		if (day == 2) {
			talkQueue.offer(new Content(new ComingoutContentBuilder(me, Role.POSSESSED)));

			Agent notWerewolf = null;
			Agent werewolf = estimateWerewolfAgent;
			for(Agent agent : aliveOthers) {
				if(agent != werewolf)
					notWerewolf = agent;
			}

			voteCandidate = notWerewolf;
			if(voteCandidate == null) voteCandidate = randomSelect(aliveOthers);

			if(seerPlay) {
				if (numFirstCo == 1) {
					VoteFromWerewolfCoFlag = true;
				} else if (numFirstCo == 2) {
					if (numAliveCo == 1) {
						//狼狂村
						VoteFromWerewolfCoFlag = true;
					} else if (numAliveCo == 2) {
						// 狂狼占　CO吊り
						for (Agent agent : aliveOthers) {
							if (firstDayCo.contains(agent))
								notWerewolf = agent;
							else
								werewolf = agent;
						}
					}
				} else if (numFirstCo == 3) {
					if (numAliveCo == 1) {
						VoteFromWerewolfCoFlag = true;
					} else if (numAliveCo == 2) {
						// 狂狼村　非CO吊り
						for (Agent agent : aliveOthers) {
							if (!firstDayCo.contains(agent))
								notWerewolf = agent;
							else
								werewolf = agent;
						}
					} else if (numAliveCo == 3) {
						VoteFromWerewolfCoFlag = true;
					}
				}
			} else {
				if (numFirstCo == 1) {
					// 謎
					VoteFromWerewolfCoFlag = true;
				} else if (numFirstCo == 2) {
					if (numAliveCo == 1) {
						//狼狂村 非CO吊り
						for (Agent agent : aliveOthers) {
							if (!firstDayCo.contains(agent))
								notWerewolf = agent;
							else
								werewolf = agent;
						}
					} else if (numAliveCo == 2) {
						// 狂狼占
						VoteFromWerewolfCoFlag = true;
					}
				} else if (numFirstCo == 3) {
					// 謎
					VoteFromWerewolfCoFlag = true;
				}
			}

			//黒出しされたエージェント死亡，黒出ししたエージェント狼
			Agent fakeSeer = null;
			for (Judge j : divinationList) {
				if (j.getTarget() == currentGameInfo.getLatestExecutedAgent()
						&& j.getResult() == Species.WEREWOLF) {
					fakeSeer = j.getAgent();
				}
			}
			if (fakeSeer != null && fakeSeer == LogisticRegression5Werewolf.numWerewolf5Estimator(aliveOthers)) {
				for (Agent agent : aliveOthers) {
					if (agent == fakeSeer)
						werewolf = agent;
					else
						notWerewolf = agent;
				}
			}

			if (notWerewolf != null && werewolf != null) {
				voteCandidate = notWerewolf;
			}

			if (voteCandidate == null) {
				voteCandidate = randomSelect(aliveOthers);
			}

			talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
			talkQueue.offer(new Content(
					new RequestContentBuilder(
							null, new Content(
									new VoteContentBuilder(voteCandidate)))));

		}
	}

	public String talk() {
		if (day == 1 && turn > 0) {
			if(seerPlay) {
				if (!isDivind) {
					List<Agent> candidates = new ArrayList<>(aliveOthers);
					voteCandidate = LogisticRegression5Villager.numVillager5Estimator(candidates);
					if (voteCandidate == null) {
						voteCandidate = randomSelect(aliveOthers);
					}
					talkQueue.offer(new Content(
							new DivinedResultContentBuilder(voteCandidate, Species.WEREWOLF)));
					talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
					isDivind = true;
				}
			} else {
				List<Agent> candidates = new ArrayList<>(aliveOthers);
				// 非CO釣り
				for(Agent agent : aliveOthers) {
					if(comingoutMap.get(agent) == Role.SEER) {
						candidates.remove(agent);
					}
				}

				// 投票先を設定
				voteCandidate = LogisticRegression5Villager.numVillager5Estimator(candidates);
				if(voteCandidate == null) voteCandidate = randomSelect(aliveOthers);

				if (voteCandidate != null && voteCandidate != declaredVoteCandidate) {
					talkQueue.clear();
					talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
					declaredVoteCandidate = voteCandidate;
				}
			}
		} else if(day == 2) {
			if (VoteFromWerewolfCoFlag) {
				VoteFromWerewolfCo();
			}
		}

		return super.talk();

	}

}
