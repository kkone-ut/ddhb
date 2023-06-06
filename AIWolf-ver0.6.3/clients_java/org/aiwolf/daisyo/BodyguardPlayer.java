package org.aiwolf.daisyo;

import java.util.ArrayList;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

public class BodyguardPlayer extends VillagerPlayer {

	Agent guardedAgent;

	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {

		try {
			super.initialize(gameInfo, gameSetting);

			if (isFirst) {
				params = new Parameters(numAgents);
				sh = new StateHolder(numAgents);
				isFirst = false;
			}

			ArrayList<Integer> fixed = new ArrayList<Integer>();
			fixed.add(myidx);
			sh.process(params, gamedata);

			gamedata.clear();
			sh.head = 0;

			sh.game_init(fixed, myidx, numAgents, Util.BODYGUARD, params);
			update_sh = true;
			before = -1;
		}
		catch (Exception e) {
			;
		}
	}

	public Agent guard() {
		try {
			double mn = -1;
			int c = 0;
			for (int i = 0; i < numAgents; ++i) {
				if (i != myidx && sh.gamestate.agents[i].isAlive) {
					double score = sh.rp.getProb(i, Util.VILLAGER) + sh.rp.getProb(i, Util.SEER) * 1.15 + sh.rp.getProb(i, Util.MEDIUM);
					score += 3 * (wincnt[i] / (gamecnt + 0.01));
					if (mn < score) {
						mn = score;
						c = i;
					}
				}
			}

			guardedAgent = currentGameInfo.getAgentList().get(c);
			return guardedAgent;
		}
		catch (Exception e) {
			return null;
		}
	}
}
