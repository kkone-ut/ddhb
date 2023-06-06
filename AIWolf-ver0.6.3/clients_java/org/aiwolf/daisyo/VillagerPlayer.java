package org.aiwolf.daisyo;

import java.util.ArrayList;
import java.util.List;

import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.VoteContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

public class VillagerPlayer extends BasePlayer {

	boolean isFirst = true;
	Parameters params;
	boolean update_sh = true;
	boolean kyoujin_ikiteru = false;
	boolean pos = false;

	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		try {
			super.initialize(gameInfo, gameSetting);
			if (isFirst) {
				isFirst = false;

				params = new Parameters(numAgents);
				sh = new StateHolder(numAgents);
			}

			ArrayList<Integer> fixed = new ArrayList<Integer>();
			fixed.add(myidx);
			sh.process(params, gamedata);

			gamedata.clear();
			sh.head = 0;

			sh.game_init(fixed, myidx, numAgents, Util.VILLAGER, params);
			update_sh = true;
			before = -1;

			kyoujin_ikiteru = false;
			pos = false;
		}
		catch (Exception e) {
			;
		}
	}


	protected String chooseTalk() {
		gamedata.add(new GameData(DataType.TURNSTART, day, sh.gamestate.turn, myidx, myidx, false));
		sh.process(params, gamedata);

		boolean condition = false;
		if (numAgents == 5) {
			if ((day == 1 || day == 2) && sh.gamestate.turn <= 3 && sh.gamestate.turn >= 2) {
				condition = true;
			}
		}
		else {
			if (day < max_turn && sh.gamestate.turn >= 2 && sh.gamestate.turn <= 4) {
				condition = true;
			}
		}

		if (condition) {
			// ここに入るのは2ターン目からだから
			int turn = sh.gamestate.turn - 2;
			if (day == 1 && sh.gamestate.turn == 2) {
				pred = 0;
				double mm = 0;
				for (int i = 0; i < numAgents; ++i) {
					if (i != myidx && sh.gamestate.agents[i].isAlive && mm < agentScore[day][turn][i][Util.WEREWOLF]) {
						mm = agentScore[day][turn][i][Util.WEREWOLF];
						pred = i;
					}
				}
				// DEBUG
				System.err.println("***** pred WEREWOLF = " + pred);
			}
			updateState(sh);
		}

		if (update_sh) {
			update_sh = false;
			sh.search(1000);
		}

		double mn = -1;
		int c = 0;

		if (numAgents == 5) {
			if (day == 1 && sh.gamestate.turn == 1) {
				return Talk.SKIP;
			}
		}
		else {
			if (day == 1 && sh.gamestate.turn == 1) {
				return Talk.SKIP;
			}
		}

		if (numAgents == 5) {
			c = chooseMostLikelyWerewolf();
		}
		else {

			if (getAliveAgentsCount() <= 3) {
				c = chooseMostLikelyWerewolf();
			}
			else {
				if (sh.gamestate.day < 3) {
					c = chooseMostLikelyExecuted(getAliveAgentsCount() * 0.3);
				}
				else if (sh.gamestate.day < 5) {
					c = chooseMostLikelyExecuted(getAliveAgentsCount() * 0.5);
				}
				else {
					c = chooseMostLikelyWerewolf();
				}

				if (c == -1) {
					c = chooseMostLikelyWerewolf();
				}
			}
		}
//
//		if (getAliveAgentsCount() <= 3) {
//			if (!pos) {
//				pos = true;
//				if (numAgents == 5) {
//					if (numAgents == 5) {
//						double all = 0;
//						double alive = 0;
//						for (int i = 0; i < numAgents; ++i) {
//							all += sh.rp.getProb(i, Util.POSSESSED);
//							if (sh.gamestate.agents[i].isAlive) {
//								alive += sh.rp.getProb(i, Util.POSSESSED);
//							}
//						}
//						if (alive > 0.5 * all) {
//							kyoujin_ikiteru = true;
//							return (new Content(new ComingoutContentBuilder(me, Role.WEREWOLF))).getText();
//						}
//					}
//				}
//			}
//		}

		// debug
		System.err.println("will_vote " + (c + 1) + " " + mn);

		if (sh.gamestate.cnt_vote(myidx) * 2 >= currentGameInfo.getAliveAgentList().size()) {
			before = -1;
		}
		if (numAgents == 5) {
			if (sh.gamestate.cnt_vote(c) * 2 < currentGameInfo.getAliveAgentList().size()) {
				before = -1;
			}
		}

		if (before != c) {
			voteCandidate = currentGameInfo.getAgentList().get(c);
			before = c;
			return (new Content(new VoteContentBuilder(voteCandidate))).getText();
		}
		before = c;

//		if (rnd.nextDouble() < 0.4) {
//			return Talk.OVER;
//		}

		return Talk.SKIP;
	}

	protected Agent chooseVote() {
		gamedata.add(new GameData(DataType.VOTESTART, day, -1, myidx, myidx, false));
		sh.process(params, gamedata);

		int c = 0;

		if (numAgents == 5) {
			c = chooseMostLikelyWerewolf();
		}
		else {

			if (getAliveAgentsCount() <= 3) {
				c = chooseMostLikelyWerewolf();
			}
			else {
				if (sh.gamestate.day < 3) {
					c = chooseMostLikelyExecuted(getAliveAgentsCount() * 0.3);
				}
				else if (sh.gamestate.day < 5) {
					c = chooseMostLikelyExecuted(getAliveAgentsCount() * 0.5);
				}
				else {
					c = chooseMostLikelyWerewolf();
				}

				if (c == -1) {
					c = chooseMostLikelyWerewolf();
				}
			}
			if (c == -1) {
				c = chooseMostLikelyWerewolf();
			}
		}

		if (c != -1) return currentGameInfo.getAgentList().get(c);

		// random
		List<Agent> tmp = new ArrayList<>(currentGameInfo.getAliveAgentList());
		if (tmp.contains(me)) tmp.remove(me);
		return randomSelect(tmp);
	}
}
