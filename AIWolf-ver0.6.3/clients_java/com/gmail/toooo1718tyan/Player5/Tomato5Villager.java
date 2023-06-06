package com.gmail.toooo1718tyan.Player5;

import java.util.ArrayList;
import java.util.List;

import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.RequestContentBuilder;
import org.aiwolf.client.lib.VoteContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

import com.gmail.toooo1718tyan.Estimator.LogisticRegression5Werewolf;

public class Tomato5Villager extends Tomato5BasePlayer {

	Agent lieBlackResultAgent;
	Agent wolfCoAgent;
	boolean myBlackDivaindFlag;
	int blackDivineCount;
	int firstCoCount;
	List<Agent> posseEstimateDoneList = new ArrayList<>();
	Role fakeRole;

	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		lieBlackResultAgent = null;
		wolfCoAgent = null;
		myBlackDivaindFlag = false;
		blackDivineCount = 0;
		firstCoCount = 0;
		posseEstimateDoneList.clear();
		fakeRole = null;
	}

	// 1日目
	protected void firstDayTalk() {

		List<Agent> candidates = new ArrayList<>(aliveOthers);

		//白だしされたエジェーントの除外
		for (Agent agent : aliveOthers) {
			if (whiteDivineList.contains(agent)) {
				candidates.remove(agent);
			}
		}

		// 黒を出されたエージェントを優先的に吊る
		List<Agent> blackCandidates = new ArrayList<>();
		for (Agent agent : candidates) {
			if (blackDivineList.contains(agent)) {
				blackCandidates.add(agent);
			}
		}

		// 候補が残らなかったら全員から
		if (candidates.isEmpty()) {
			candidates = new ArrayList<>(aliveOthers);
		}

		//投票先の決定
		if (!blackCandidates.isEmpty()) {
			lieBlackResultAgent = LogisticRegression5Werewolf.numWerewolf5Estimator(blackCandidates);
			voteCandidate = lieBlackResultAgent;
		} else {
			voteCandidate = estimateWerewolfAgent;
		}
		if (voteCandidate == null) {
			lieBlackResultAgent = LogisticRegression5Werewolf.numWerewolf5Estimator(candidates);
			if(voteCandidate == null) voteCandidate = randomSelect(aliveOthers);
		}

//		System.err.println(voteCandidate + " " + declaredVoteCandidate);

		// 投票先を宣言
		if (voteCandidate != null && voteCandidate != declaredVoteCandidate) {
			talkQueue.clear();
			talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
			declaredVoteCandidate = voteCandidate;
		}
	}


	protected void secondDayTalk() {
		Agent werewolf = estimateWerewolfAgent;
		Agent notWerewolf = null;
		for(Agent agent : aliveOthers) {
			if(agent != werewolf)
				notWerewolf = agent;
		}
		Agent notVoteCandidate = null;


		if (numFirstCo == 1) {
			if(numAliveCo == 1) {
				//真占いのみ
				for (Agent agent : aliveOthers) {
					if (firstDayCo.contains(agent)) {
						notWerewolf = agent;
					} else {
						werewolf = agent;
					}
				}
			} else {
				if (notWerewolf == estimateVillagerAgent2) {
					// 狼村村
					fakeRole = Role.POSSESSED;
				} else if (notWerewolf == estimatePossessedAgent) {
					// 狼狂村
					fakeRole = Role.WEREWOLF;
				} else {
					// 狂を占いと誤判定？
					fakeRole = Role.WEREWOLF;
				}
			}
		} else if (numFirstCo == 2) {
			if(numAliveCo == 2) {
				if (seerCOList.contains(werewolf)) {
					// 狼が占いCOした
					// 狼占村
					for (Agent agent : aliveOthers) {
						if (blackDivineMe.contains(agent)) {
							werewolf = agent;
						} else {
							notWerewolf = agent;
						}
						if (voteTarget.get(agent) == me) {
							werewolf = agent;
						} else {
							notWerewolf = agent;
						}
					}
				} else {
					// 狼が占いCOしていない
					// 狼占村，狼狂村，狼村村？
					for (Agent agent : aliveOthers) {
						if (blackDivineMe.contains(agent)) {
							werewolf = agent;
						} else {
							notWerewolf = agent;
						}
						if (voteTarget.get(agent) == me) {
							werewolf = agent;
						} else {
							notWerewolf = agent;
						}
					}
				}
			} else if(numAliveCo == 1){
				// 生き残りが狼か占か狂かわからない
				if (seerCOList.contains(werewolf)) {
					// 狼狂村，狼村村
					if (notWerewolf == estimateVillagerAgent2) {
						// 狼村村
						fakeRole = Role.POSSESSED;
					} else if (notWerewolf == estimatePossessedAgent) {
						// 狼狂村
						fakeRole = Role.WEREWOLF;
					} else {
						// 狂を占いと誤判定？
						fakeRole = Role.WEREWOLF;
					}
				} else {
					//村狂狼or村狼占
					for (Agent agent : aliveOthers) {
						if (!firstDayCo.contains(agent)) {
							werewolf = agent;
						} else {
							notWerewolf = agent;
						}
						if (notWerewolf == estimatePossessedAgent) {
							//パワープレイ阻止
							fakeRole = Role.WEREWOLF;
						}
					}
				}
			}
		} else if (numFirstCo == 3) {
			if (numAliveCo == 1) {
				//村村狼のためCO釣り
				for (Agent agent : aliveOthers) {
					if (firstDayCo.contains(agent)) {
						werewolf = agent;
					} else {
						notWerewolf = agent;
					}
				}
			} else if (numAliveCo == 2) {
				for (Agent agent : aliveOthers) {
					if (blackDivineMe.contains(agent)) {
						werewolf = agent;
					} else {
						notWerewolf = agent;
					}
					if (voteTarget.get(agent) == me) {
						werewolf = agent;
					} else {
						notWerewolf = agent;
					}
				}
			}
		}

		//投票先選択
		if (voteCandidate == null)
			voteCandidate = estimateWerewolfAgent;
		if (voteCandidate == null)
			voteCandidate = LogisticRegression5Werewolf.numWerewolf5Estimator(aliveOthers);
		if (voteCandidate == null)
			voteCandidate = randomSelect(aliveOthers);

		for (Agent agent : aliveOthers) {
			if (voteCandidate != agent) {
				notVoteCandidate = agent;
			}
		}


		//発言
		talkQueue.clear();
		if (fakeRole != null) {
			if(notWerewolf != null && werewolf != null) {
				talkQueue.offer(new Content(new ComingoutContentBuilder(me, fakeRole)));
				talkQueue.offer(new Content(new VoteContentBuilder(notWerewolf)));
				talkQueue.offer(new Content(
						new RequestContentBuilder(
								null, new Content(
										new VoteContentBuilder(notWerewolf)))));
				voteCandidate = werewolf;
			} else {
				talkQueue.offer(new Content(new ComingoutContentBuilder(me, fakeRole)));
				talkQueue.offer(new Content(new VoteContentBuilder(notVoteCandidate)));
				talkQueue.offer(new Content(
						new RequestContentBuilder(
								null, new Content(
										new VoteContentBuilder(notVoteCandidate)))));
			}
		} else {
			if(notWerewolf != null && werewolf != null) {
				talkQueue.offer(new Content(new VoteContentBuilder(werewolf)));
				talkQueue.offer(new Content(
						new RequestContentBuilder(
								null, new Content(
										new VoteContentBuilder(werewolf)))));
				voteCandidate = werewolf;
			} else {
				talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
				talkQueue.offer(new Content(
						new RequestContentBuilder(
								null, new Content(
										new VoteContentBuilder(voteCandidate)))));
			}
		}

	}

	protected void chooseVoteCandidate() {
		Agent werewolf = estimateWerewolfAgent;
		Agent notWerewolf = null;
		for(Agent agent : aliveOthers) {
			if(agent != werewolf)
				notWerewolf = agent;
		}

		if (numFirstCo == 1) {
			if(numAliveCo == 1) {
				//真占いのみ
				for (Agent agent : aliveOthers) {
					if (firstDayCo.contains(agent)) {
						notWerewolf = agent;
					} else {
						werewolf = agent;
					}
				}
			} else {
				if (notWerewolf == estimateVillagerAgent2) {
					// 狼村村
					fakeRole = Role.POSSESSED;
				} else if (notWerewolf == estimatePossessedAgent) {
					// 狼狂村
					fakeRole = Role.WEREWOLF;
				} else {
					// 狂を占いと誤判定？
					fakeRole = Role.WEREWOLF;
				}
			}
		} else if (numFirstCo == 2) {
			if(numAliveCo == 2) {
				if (seerCOList.contains(werewolf)) {
					// 狼が占いCOした
					// 狼占村
					for (Agent agent : aliveOthers) {
						if (blackDivineMe.contains(agent)) {
							werewolf = agent;
						} else {
							notWerewolf = agent;
						}
						if (voteTarget.get(agent) == me) {
							werewolf = agent;
						} else {
							notWerewolf = agent;
						}
					}
				} else {
					// 狼が占いCOしていない
					// 狼占村，狼狂村，狼村村？
					for (Agent agent : aliveOthers) {
						if (blackDivineMe.contains(agent)) {
							werewolf = agent;
						} else {
							notWerewolf = agent;
						}
						if (voteTarget.get(agent) == me) {
							werewolf = agent;
						} else {
							notWerewolf = agent;
						}
					}
				}
			} else if(numAliveCo == 1){
				// 生き残りが狼か占か狂かわからない
				if (seerCOList.contains(werewolf)) {
					// 狼狂村，狼村村
					if (notWerewolf == estimateVillagerAgent2) {
						// 狼村村
						fakeRole = Role.POSSESSED;
					} else if (notWerewolf == estimatePossessedAgent) {
						// 狼狂村
						fakeRole = Role.WEREWOLF;
					} else {
						// 狂を占いと誤判定？
						fakeRole = Role.WEREWOLF;
					}
				} else {
					//村狂狼or村狼占
					for (Agent agent : aliveOthers) {
						if (!firstDayCo.contains(agent)) {
							werewolf = agent;
						} else {
							notWerewolf = agent;
						}
						if (notWerewolf == estimatePossessedAgent) {
							//パワープレイ阻止
							fakeRole = Role.WEREWOLF;
						}
					}
				}
			}
		} else if (numFirstCo == 3) {
			if (numAliveCo == 1) {
				//村村狼のためCO釣り
				for (Agent agent : aliveOthers) {
					if (firstDayCo.contains(agent)) {
						werewolf = agent;
					} else {
						notWerewolf = agent;
					}
				}
			} else if (numAliveCo == 2) {
				for (Agent agent : aliveOthers) {
					if (blackDivineMe.contains(agent)) {
						werewolf = agent;
					} else {
						notWerewolf = agent;
					}
					if (voteTarget.get(agent) == me) {
						werewolf = agent;
					} else {
						notWerewolf = agent;
					}
				}
			}
		}

		voteCandidate = werewolf;
	}

	public void dayStart() {
		super.dayStart();
		if (day == 2)
			secondDayTalk();
	}

	public String talk() {

		if(day == 1) {
			if (turn > 0) {
				firstDayTalk();
			}
		}

		if (day == 2)
			chooseVoteCandidate();

		return super.talk();
	}

	public String whisper() {
		throw new UnsupportedOperationException();
	}

	public Agent attack() {
		throw new UnsupportedOperationException();
	}

	public Agent divine() {
		throw new UnsupportedOperationException();
	}

	public Agent guard() {
		throw new UnsupportedOperationException();
	}

}
