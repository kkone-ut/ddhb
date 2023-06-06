package org.aiwolf.daisyo;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DivinedResultContentBuilder;
import org.aiwolf.client.lib.VoteContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

public class PossessedPlayer extends BasePlayer {

	Deque<Judge> divinationQueue = new LinkedList<>();
	Map<Agent, Species> myDIvinationMap = new HashMap<>();

	List<Agent> whiteList = new ArrayList<>();
	List<Agent> blackList = new ArrayList<>();
	List<Agent> grayList;
	List<Agent> semiWolves = new ArrayList<>();
	List<Agent> possessedList = new ArrayList<>();

	StateHolder sh2;
	boolean isFirst = true;
	Parameters params;
	boolean seer = true;
	boolean doCO = false;
	boolean isReported = true;
	boolean [] divined;
	boolean pos = false;
	boolean update_sh = true;

	Agent firstDayDivineTarget;

	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {

		try {
			super.initialize(gameInfo, gameSetting);

			if (isFirst) {
				params = new Parameters(numAgents);
				sh = new StateHolder(numAgents);
				sh2 = new StateHolder(numAgents);
				isFirst =false;
			}

			update_sh = true;
			doCO = false;
			isReported = true;
			pos = false;
			divined = new boolean[numAgents];
			for (int i = 0; i < numAgents; ++i) {
				divined[i] = false;
			}

			if (numAgents == 5) seer = true;
			else seer = true;

			ArrayList<Integer> fixed = new ArrayList<>();
			fixed.add(myidx);
			sh.process(params, gamedata);
			sh2.process(params, gamedata);

			gamedata.clear();
			sh.head = 0;
			sh2.head= 0;

			sh.game_init(fixed, myidx, numAgents, Util.POSSESSED, params);
			sh2.game_init(fixed, myidx, numAgents, Util.SEER, params);

			before = -1;

			firstDayDivineTarget = null;
		}
		catch (Exception e) {
			;
		}
	}

	public void dayStart() {
		super.dayStart();
		isReported = false;
	}

	protected Agent chooseVote() {

		gamedata.add(new GameData(DataType.VOTESTART, day, -1, myidx, myidx, false));
		sh.process(params, gamedata);

//		if (day == 1 && numAgents == 5 && firstDayDivineTarget != null) {
//			return firstDayDivineTarget;
//		}

		double mn = -1;
		int c = 0;
		if (currentGameInfo.getAliveAgentList().size() <= 3) {
			for (int i = 0; i < numAgents; ++i) {
				if (i != myidx && sh.gamestate.agents[i].isAlive) {
					double score = 1 - sh.rp.getProb(i, Util.WEREWOLF);
					if (mn < score) {
						mn = score;
						c = i;
					}
				}
			}
		}
		else {

			mn = -100;
			for (int i = 0; i < numAgents; ++i) {
				if (i != myidx && sh.gamestate.agents[i].isAlive) {
					double score = sh.rp.getProb(i, Util.WEREWOLF);
					if (mn < score) {
						mn = score;
						c = i;
					}
				}
			}
			int t = sh.gamestate.agents[c].will_vote;

			mn = -100;
			if (t == -1) {
				for (int i = 0; i < numAgents; ++i) {
					if (i != myidx && sh.gamestate.agents[i].isAlive) {
						double score = 1 - sh.rp.getProb(i, Util.WEREWOLF);
						if (mn < score) {
							mn = score;
							t = i;
						}
					}
				}
			}
			c = t;
		}

		return currentGameInfo.getAgentList().get(c);
	}


	protected String chooseTalk() {
		gamedata.add(new GameData(DataType.TURNSTART, day, -1, myidx, myidx, false));
		sh.process(params, gamedata);
		sh2.process(params, gamedata);

		updateState(sh);
		updateState(sh2);

		if (update_sh) {
			update_sh = false;
			sh.search(1000);
			sh2.search(1000);
		}
		double mn = -1;
		int c = 0;

		if (seer) {
			if (!doCO) {
				doCO = true;
				return (new Content(new ComingoutContentBuilder(me, Role.SEER))).getText();
			}

			if (!isReported) {
				isReported = true;
				if (numAgents == 5) {

					if (day == 1) {
						c = chooseMostLikelyVillager(divined);
						divined[c] = true;
						sh2.scorematrix.divined(sh2.gamestate, myidx, c, true);
						if (day == 1) {
							firstDayDivineTarget = currentGameInfo.getAgentList().get(c);
						}
						return (new Content(new DivinedResultContentBuilder(currentGameInfo.getAgentList().get(c), Species.WEREWOLF))).getText();
					}
					else {
						c = chooseMostLikelyWerewolf();
						return (new Content(new DivinedResultContentBuilder(currentGameInfo.getAgentList().get(c), Species.HUMAN))).getText();
					}

				}
				else {

					mn = -100;
					for (int i = 0; i < numAgents; ++i) {
						if (i != myidx && sh.gamestate.agents[i].isAlive && !divined[i]) {
							double score = sh.rp.getProb(i, Util.WEREWOLF);
							if (sh.gamestate.agents[i].corole != -1) {
								score -= 1.0;
							}
							if (mn < score) {
								mn = score;
								c = i;
							}
						}
					}

					if (c != -1) {
						divined[c] = true;
						sh2.scorematrix.divined(sh2.gamestate, myidx, c, true);
						return (new Content(new DivinedResultContentBuilder(currentGameInfo.getAgentList().get(c), Species.HUMAN))).getText();
					}
				}
			}
		}

		sh2.update();

		for (int i = 0; i < numAgents; ++i) {
			if (i != myidx && sh.gamestate.agents[i].isAlive) {
				double score = sh2.rp.getProb(i, Util.WEREWOLF);
				if (mn < score) {
					mn = score;
					c = i;
				}
			}
		}

		if (numAgents == 5) {
			mn = -1;
			for (int i = 0; i < numAgents; ++i) {
				if (i != myidx && sh.gamestate.agents[i].isAlive) {
					double score = 1 - sh.rp.getProb(i, Util.WEREWOLF);
					if (mn < score) {
						mn = score;
						c = i;
					}
				}
			}
		}

		if (day == 1 && firstDayDivineTarget != null) {
			if (voteCandidate != firstDayDivineTarget) {
				voteCandidate = firstDayDivineTarget;
				before = c;
				return (new Content(new VoteContentBuilder(voteCandidate))).getText();
			}
		}
		else if (before != c) {
			voteCandidate = currentGameInfo.getAgentList().get(c);
			before = c;
			return (new Content(new VoteContentBuilder(voteCandidate))).getText();
		}

		return Talk.SKIP;
	}

}
