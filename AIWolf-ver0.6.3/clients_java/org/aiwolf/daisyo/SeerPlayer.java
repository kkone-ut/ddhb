package org.aiwolf.daisyo;

import java.util.ArrayList;

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

public class SeerPlayer extends BasePlayer {

	int comingoutDay;
	boolean isCameout;
	boolean doCO = false;
	Judge divination;
	boolean[] divined;
	boolean isFirst = true;
	Parameters params;
	boolean isReported = true;
	boolean pos;
	boolean update_sh = true;

	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {

		try {
			super.initialize(gameInfo, gameSetting);

			if (isFirst) {
				params = new Parameters(numAgents);
				sh = new StateHolder(numAgents);
				isFirst = false;
			}

			update_sh = true;
			pos = false;
			doCO = false;
			isReported = true;

			divined = new boolean[numAgents];
			for (int i = 0; i < numAgents; ++i) {
				divined[i] = false;
			}

			ArrayList<Integer> fixed = new ArrayList<>();
			fixed.add(myidx);
			sh.process(params, gamedata);
			gamedata.clear();
			sh.head = 0;
			sh.game_init(fixed, myidx, numAgents, Util.SEER, params);
			before = -1;
		}
		catch (Exception e) {
			;
		}
	}

	public void dayStart() {
		try {
			super.dayStart();
			divination = currentGameInfo.getDivineResult();

			if (divination != null) {
				divined[divination.getTarget().getAgentIdx() - 1] = true;
				isReported = false;
				gamedata.add(new GameData(DataType.DIVINED, day, -1, myidx, divination.getTarget().getAgentIdx()-1, divination.getResult() == Species.HUMAN));

				if (divination.getResult() == Species.WEREWOLF) {
					werewolves.add(divination.getTarget());
				}
				else {
					humans.add(divination.getTarget());
				}
			}

			sh.process(params, gamedata);
		}
		catch (Exception e) {
			;
		}
	}

	protected Agent chooseVote() {
		gamedata.add(new GameData(DataType.VOTESTART, day, -1, myidx, myidx, false));
		sh.process(params, gamedata);
		int c = chooseMostLikelyWerewolf();
		return currentGameInfo.getAgentList().get(c);
	}

	protected String chooseTalk() {
		gamedata.add(new GameData(DataType.TURNSTART, day, -1, myidx, myidx, false));

		sh.process(params, gamedata);
		updateState(sh);

		if (update_sh) {
			update_sh = false;
			sh.search(1000);
		}

		int c = 0;

		// 一番人狼っぽい人
		c = chooseMostLikelyWerewolf();

		// 残り人数
//		if (getAliveAgentsCount() <= 3) {
//			if (!pos) {
//				pos = true;
//				double all = 0;
//				double alive = 0;
//				for (int i = 0; i < numAgents; ++i) {
//					all += sh.rp.getProb(i, Util.POSSESSED);
//					if (sh.gamestate.agents[i].isAlive) {
//						alive += sh.rp.getProb(i, Util.POSSESSED);
//					}
//				}
//				if (alive > 0.5 * all) {
//					doCO = true;
//					isReported = true;
//					return (new Content(new ComingoutContentBuilder(me,  Role.WEREWOLF))).getText();
//				}
//			}
//		}

		if (!doCO) {
			doCO = true;
			return (new Content(new ComingoutContentBuilder(me, Role.SEER))).getText();
		}

		if (!isReported) {
			isReported = true;

			if (numAgents == 5 && day == 1) {
				return (new Content(new DivinedResultContentBuilder(currentGameInfo.getAgentList().get(c), Species.WEREWOLF))).getText();
			}
			else {
				return (new Content(new DivinedResultContentBuilder(divination.getTarget(), divination.getResult()))).getText();
			}

		}

		voteCandidate = currentGameInfo.getAgentList().get(c);

		if (before != c) {
			before = c;
			return (new Content(new VoteContentBuilder(voteCandidate))).getText();
		}

		return Talk.SKIP;
	}

	public Agent divine() {
		try {
			sh.process(params, gamedata);
			sh.update();

			double mn = -1;
			int c = -1;
			for (int i = 0; i < numAgents; ++i) {
				if (i != myidx && sh.gamestate.agents[i].isAlive && !divined[i]) {
					double score = sh.rp.getProb(i, Util.WEREWOLF);
					if (mn < score) {
						mn = score;
						c = i;
					}
				}
			}

			if (c == -1) return null;
			return currentGameInfo.getAgentList().get(c);
		}
		catch (Exception e) {
			return null;
		}
	}
}
