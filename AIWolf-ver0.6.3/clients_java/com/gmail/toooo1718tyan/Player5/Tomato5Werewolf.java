package com.gmail.toooo1718tyan.Player5;

import java.util.ArrayList;
import java.util.List;

import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DivinedResultContentBuilder;
import org.aiwolf.client.lib.EstimateContentBuilder;
import org.aiwolf.client.lib.RequestContentBuilder;
import org.aiwolf.client.lib.VoteContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

import com.gmail.toooo1718tyan.Estimator.LogisticRegression5Villager;
import com.gmail.toooo1718tyan.MetaStrategy.MostVoteAgent;
import com.gmail.toooo1718tyan.MetaStrategy.WinRateCalc;

public class Tomato5Werewolf extends Tomato5BasePlayer {

	boolean seerPlay;
	boolean isDivind;
	boolean hiddenPlay;
	boolean pawerPlay;
	List<Agent> possessedCandidate = new ArrayList<>();
	int seerCoCount;
	List<Agent> posseEstimateDoneList = new ArrayList<>();

	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		possessedCandidate.clear();
		isDivind = false;
		seerCoCount = 0;
		// 騙る役職を決定する
		if (Math.random() < 0.3) {
			seerPlay = true;
		} else {
			seerPlay = false;
		}
		hiddenPlay = false;
		pawerPlay = false;
	}


	public void dayStart() {
		super.dayStart();

		Agent notVotoCandidate = null;

		// 初日確率で占い偽り==狂人
		if (day == 1 && seerPlay) {
			talkQueue.offer(new Content(new ComingoutContentBuilder(me, Role.SEER)));
			comingoutMap.put(me, Role.SEER);
		}

		if (day == 2) {
			if (seerPlay) {
				//狂人プレイ
				if(numFirstCo == 1) {
					// 狼狂村・狼村村
					if (estimatePossessedAgent != null) {
						// 狼狂村
						// パワープレイ
						pawerPlay = true;
						voteCandidate = estimateVillagerAgent();
						talkQueue.offer(new Content(new ComingoutContentBuilder(me, Role.WEREWOLF)));
						talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
						talkQueue.offer(new Content(
								new RequestContentBuilder(
										null, new Content(
												new VoteContentBuilder(voteCandidate)))));
					} else {
						// 狼村村
						voteCandidate = randomSelect(aliveOthers);
						for (Agent agent : aliveOthers) {
							if (voteCandidate != agent) {
								notVotoCandidate = agent;
							}
						}
						talkQueue.offer(new Content(new DivinedResultContentBuilder(notVotoCandidate, Species.HUMAN)));
						talkQueue.offer(new Content(new EstimateContentBuilder(voteCandidate, Role.WEREWOLF)));
						talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
						talkQueue.offer(new Content(
								new RequestContentBuilder(
										null, new Content(
												new VoteContentBuilder(voteCandidate)))));
					}
				} else if (numFirstCo == 2) {
					if(numAliveCo == 1) {
						// 狼狂村，狼村村
						if (estimatePossessedAgent != null) {
							// 狼狂村
							// パワープレイ
							pawerPlay = true;
							voteCandidate = estimateVillagerAgent();
							talkQueue.offer(new Content(new ComingoutContentBuilder(me, Role.WEREWOLF)));
							talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
							talkQueue.offer(new Content(
									new RequestContentBuilder(
											null, new Content(
													new VoteContentBuilder(voteCandidate)))));
						} else {
							// 狼村村
							voteCandidate = randomSelect(aliveOthers);
							for (Agent agent : aliveOthers) {
								if (voteCandidate != agent) {
									notVotoCandidate = agent;
								}
							}
							talkQueue.offer(new Content(new DivinedResultContentBuilder(notVotoCandidate, Species.HUMAN)));
							talkQueue.offer(new Content(new EstimateContentBuilder(voteCandidate, Role.WEREWOLF)));
							talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
							talkQueue.offer(new Content(
									new RequestContentBuilder(
											null, new Content(
													new VoteContentBuilder(voteCandidate)))));
						}
					} else if(numAliveCo == 2) {
						// 狼占村，狼占狂
						if (estimatePossessedAgent != null) {
							// 狼占狂
							// パワープレイ
							pawerPlay = true;
							voteCandidate = estimateVillagerAgent();
							talkQueue.offer(new Content(new ComingoutContentBuilder(me, Role.WEREWOLF)));
							talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
							talkQueue.offer(new Content(
									new RequestContentBuilder(
											null, new Content(
													new VoteContentBuilder(voteCandidate)))));
						} else {
							// 狼占村
							for(Agent agent : aliveOthers) {
								if(firstDayCo.contains(agent)) {
									voteCandidate = agent;
								} else {
									notVotoCandidate = agent;
								}
							}
							talkQueue.offer(new Content(new DivinedResultContentBuilder(voteCandidate, Species.WEREWOLF)));
							talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
							talkQueue.offer(new Content(
									new RequestContentBuilder(
											null, new Content(
													new VoteContentBuilder(voteCandidate)))));
						}
					}
				} else if (numFirstCo == 3) {
					if(numAliveCo == 1) {
						// 狼村村
						voteCandidate = randomSelect(aliveOthers);
						for (Agent agent : aliveOthers) {
							if (voteCandidate != agent) {
								notVotoCandidate = agent;
							}
						}
						talkQueue.offer(new Content(new DivinedResultContentBuilder(notVotoCandidate, Species.HUMAN)));
						talkQueue.offer(new Content(new EstimateContentBuilder(voteCandidate, Role.WEREWOLF)));
						talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
						talkQueue.offer(new Content(
								new RequestContentBuilder(
										null, new Content(
												new VoteContentBuilder(voteCandidate)))));
					} else if(numAliveCo == 2) {
						// 狼占村，狼狂村
						if (estimateSeerAgent != null) {
							// 狼占村
							for(Agent agent : aliveOthers) {
								if(firstDayCo.contains(agent)) {
									voteCandidate = agent;
								} else {
									notVotoCandidate = agent;
								}
							}
							talkQueue.offer(new Content(new DivinedResultContentBuilder(voteCandidate, Species.WEREWOLF)));
							talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
							talkQueue.offer(new Content(
									new RequestContentBuilder(
											null, new Content(
													new VoteContentBuilder(voteCandidate)))));
						} else if (estimatePossessedAgent != null) {
							// 狼狂村
							// 占と狂の見分けが難しいのでパワープレイはおこなわない
							for(Agent agent : aliveOthers) {
								if(firstDayCo.contains(agent)) {
									notVotoCandidate = agent;
								} else {
									voteCandidate = agent;
								}
							}
							talkQueue.offer(new Content(new DivinedResultContentBuilder(notVotoCandidate, Species.HUMAN)));
							talkQueue.offer(new Content(new EstimateContentBuilder(voteCandidate, Role.WEREWOLF)));
							talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
							talkQueue.offer(new Content(
									new RequestContentBuilder(
											null, new Content(
													new VoteContentBuilder(voteCandidate)))));
						} else {
							voteCandidate = randomSelect(aliveOthers);
							for (Agent agent : aliveOthers) {
								if (voteCandidate != agent) {
									notVotoCandidate = agent;
								}
							}
							talkQueue.offer(new Content(new DivinedResultContentBuilder(notVotoCandidate, Species.HUMAN)));
							talkQueue.offer(new Content(new EstimateContentBuilder(voteCandidate, Role.WEREWOLF)));
							talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
							talkQueue.offer(new Content(
									new RequestContentBuilder(
											null, new Content(
													new VoteContentBuilder(voteCandidate)))));
						}
					} else if(numAliveCo == 3) {
						//狼狂占．パワープレイ
						pawerPlay = true;
						voteCandidate = randomSelect(aliveOthers);
						talkQueue.offer(new Content(new ComingoutContentBuilder(me, Role.WEREWOLF)));
						talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
						talkQueue.offer(new Content(
								new RequestContentBuilder(
										null, new Content(
												new VoteContentBuilder(voteCandidate)))));
					}
				}
			} else {
				//村人潜伏
				if (numFirstCo == 1) {
					if(numAliveCo == 1) {
						// 狼占村，狼占狂
						if (estimatePossessedAgent != null) {
							// パワープレイ待ちもおこなう
							pawerPlay = true;
							for(Agent agent : aliveOthers) {
								if(firstDayCo.contains(agent)) {
									voteCandidate = agent;
								} else {
									notVotoCandidate = agent;
								}
							}
						} else {
							// パワープレイ待ちもおこなう
							pawerPlay = true;
							hiddenPlay = true;
							voteCandidate = randomSelect(aliveOthers);
							for (Agent agent : aliveOthers) {
								if (voteCandidate != agent) {
									notVotoCandidate = agent;
								}
							}
						}
						talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
						talkQueue.offer(new Content(
								new RequestContentBuilder(
										null, new Content(
												new VoteContentBuilder(voteCandidate)))));
					} else {
						// 狼村村，狼村狂，
						// パワープレイ待ちもおこなう
						pawerPlay = true;
						hiddenPlay = true;
						voteCandidate = randomSelect(aliveOthers);
						for (Agent agent : aliveOthers) {
							if (voteCandidate != agent) {
								notVotoCandidate = agent;
							}
						}
						talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
						talkQueue.offer(new Content(
								new RequestContentBuilder(
										null, new Content(
												new VoteContentBuilder(voteCandidate)))));
					}
				} else if (numFirstCo == 2) {
					if(numAliveCo == 0) {
						// 狼村村
						// 潜伏投票合わせ
						hiddenPlay = true;
						voteCandidate = randomSelect(aliveOthers);
						talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
						talkQueue.offer(new Content(
								new RequestContentBuilder(
										null, new Content(
												new VoteContentBuilder(voteCandidate)))));
					} else if(numAliveCo == 1) {
						if (estimateSeerAgent != null) {
							// 狼占村
							for(Agent agent : aliveOthers) {
								if(firstDayCo.contains(agent)) {
									voteCandidate = agent;
								} else {
									notVotoCandidate = agent;
								}
							}
							talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
							talkQueue.offer(new Content(
									new RequestContentBuilder(
											null, new Content(
													new VoteContentBuilder(voteCandidate)))));
						} else if (estimatePossessedAgent != null) {
							// 狼狂村
							for(Agent agent : aliveOthers) {
								if(firstDayCo.contains(agent)) {
									notVotoCandidate = agent;
								} else {
									voteCandidate = agent;
								}
							}
							talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
							talkQueue.offer(new Content(
									new RequestContentBuilder(
											null, new Content(
													new VoteContentBuilder(voteCandidate)))));
						} else {
							voteCandidate = randomSelect(aliveOthers);
							for (Agent agent : aliveOthers) {
								if (voteCandidate != agent) {
									notVotoCandidate = agent;
								}
							}
							talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
							talkQueue.offer(new Content(
									new RequestContentBuilder(
											null, new Content(
													new VoteContentBuilder(voteCandidate)))));
						}
					} else if(numAliveCo == 2) {
						// 狼占狂
						// パワープレイ
						//狼狂占．パワープレイ
						pawerPlay = true;
						voteCandidate = randomSelect(aliveOthers);
						talkQueue.offer(new Content(new ComingoutContentBuilder(me, Role.WEREWOLF)));
						talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
						talkQueue.offer(new Content(
								new RequestContentBuilder(
										null, new Content(
												new VoteContentBuilder(voteCandidate)))));
					}
				}
			}
			if(voteCandidate == null) {
				hiddenPlay = true;
				voteCandidate = randomSelect(aliveOthers);
				talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
				talkQueue.offer(new Content(
						new RequestContentBuilder(
								null, new Content(
										new VoteContentBuilder(voteCandidate)))));
			}
		}
	}

	public void update(GameInfo gameInfo) {
		super.update(gameInfo);
		possessedCandidate.clear();
		for (Judge j : divinationList) {
			if (possessedCandidate.contains(j.getAgent()))
				;
			//自分以外に黒出し
			if (j.getTarget() != me && j.getResult() == Species.WEREWOLF) {
				possessedCandidate.add(j.getAgent());
			}
			//自分に白だし
			if (j.getTarget() == me && j.getResult() == Species.HUMAN) {
				possessedCandidate.add(j.getAgent());
			}
		}
	}

	//１日目村人偽り
	protected void firstDayTalk() {
		List<Agent> candidates = new ArrayList<>(aliveOthers);

		// 非CO釣り
		for(Agent agent : aliveOthers) {
			if(comingoutMap.get(agent) == Role.SEER) {
				candidates.remove(agent);
			}
		}

		// 投票先を設定
		voteCandidate = estimateVillagerAgent();
		if(voteCandidate == null) voteCandidate = randomSelect(aliveOthers);

		// 投票先を宣言
		if (voteCandidate != null && voteCandidate != declaredVoteCandidate) {
			talkQueue.clear();
			talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
			// 自分が釣られそうな場合，票合わせをおこない無効票にする
			// もっとも投票されているエージェントを取得
			if(MostVoteAgent.getMyVoteJudgment()) {
				voteCandidate = randomSelect(MostVoteAgent.getMostVoteTerget());
			}
			declaredVoteCandidate = voteCandidate;
		}
	}

	public String talk() {
		if (day == 1 && !seerPlay) {
			// 潜伏
			if(turn >= 1) {
				firstDayTalk();
				seerCoCount = numFirstCo;
			}
		} else if (day == 1 && seerPlay) {
			if (!isDivind && turn >= 1) {
				List<Agent> candidates = new ArrayList<>(aliveOthers);
				//初日占いCO＝２
				if (numFirstCo <= 2) {
					for(Agent agent : aliveOthers) {
						if(comingoutMap.get(agent) != Role.SEER) {
							candidates.remove(agent);
						}
					}
				} else {
					//初日占いCO＞＝３
					for(Agent agent : aliveOthers) {
						if(comingoutMap.get(agent) == Role.SEER) {
							candidates.remove(agent);
						}
					}
				}
				voteCandidate = LogisticRegression5Villager.numVillager5Estimator(candidates);
				if (voteCandidate == null) {
					voteCandidate = randomSelect(candidates);
				}
				if (voteCandidate == null) {
					voteCandidate = randomSelect(aliveOthers);
				}
				talkQueue.offer(new Content(
						new DivinedResultContentBuilder(voteCandidate, Species.WEREWOLF)));
				talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
				isDivind = true;
			}
		}

		if (day == 2) {
			if(hiddenPlay) {
				voteCandidate = randomSelect(MostVoteAgent.getMostVoteTerget());
				if(voteCandidate == null) {
					voteCandidate = randomSelect(aliveOthers);
				}
			}
			if (pawerPlay) {
				// 狂CO待ち
				for (Agent agent : aliveOthers) {
					if (comingoutMap.get(agent) == Role.POSSESSED && !possessedCandidate.contains(agent))
						possessedCandidate.add(agent);
				}
				// 狂人が特定できているとき
				if (possessedCandidate.size() == 1) {
					for (Agent agent : aliveOthers) {
						if (!possessedCandidate.contains(agent)) {
							voteCandidate = agent;
							talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
						}
					}
				}
			}
		}
		return super.talk();
	}

	protected void chooseAttackVoteCandidate() {
		List<Agent> candidates = new ArrayList<>();

		if (seerPlay) {
			if(numAliveCo == 2) {
				// 狼占，狼狂，狼狂の可能性低そう
				for (Agent agent : aliveOthers) {
					if (possessedCandidate.contains(agent))
						continue;
					if (firstDayCo.contains(agent))
						candidates.add(agent);
				}
			} else if(numAliveCo == 3) {
				// 狼占狂
				for (Agent agent : aliveOthers) {
					if (possessedCandidate.contains(agent))
						continue;
					if (!firstDayCo.contains(agent))
						candidates.add(agent);
				}
			}
		} else {
			// 潜伏 CO内訳は真狂
			if (numAliveCo == 1) {
				// 真占が残って詰むのを防ぐためにCO噛み
				for (Agent agent : aliveOthers) {
					if (firstDayCo.contains(agent))
						candidates.add(agent);
				}
			} else if (numAliveCo == 2) {
				// 狂噛みを避けるために村人を噛む
				for (Agent agent : aliveOthers) {
					if (!firstDayCo.contains(agent))
						candidates.add(agent);
				}
			}
		}
		if (candidates.isEmpty()) {
			for (Agent agent : aliveOthers) {
				if (possessedCandidate.contains(agent))
					continue;
				candidates.add(agent);
			}
		}
		attackVoteCandidate = randomSelect(WinRateCalc.getMostWinners(candidates));
		if(attackVoteCandidate == null) attackVoteCandidate = randomSelect(candidates);
		if (attackVoteCandidate == null) attackVoteCandidate = randomSelect(aliveOthers);
	}

}
