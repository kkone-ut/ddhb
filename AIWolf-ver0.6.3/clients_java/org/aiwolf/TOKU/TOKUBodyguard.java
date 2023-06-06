package org.aiwolf.TOKU;

import java.util.ArrayList;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

/**
 * 狩人役エージェントクラス
 */
public class TOKUBodyguard extends TOKUVillager {

	Agent guardedAgent;

	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);

		if (f) {
			params = new Parameters(numAgents);
			sh = new StateHolder(numAgents);
			f = false;
		}
		ArrayList<Integer> fixed = new ArrayList<Integer>();
		fixed.add(meint);
		sh.process(params, gamedata);

		gamedata.clear();
		sh.head = 0;
		sh.game_init(fixed, meint, numAgents, Util.BODYGUARD, params);
		update_sh = true;
		before = -1;
	}

	public Agent guard() {

		double mn = -1;
		int c = 0;

		for (int i = 0; i < numAgents; i++) {
			System.out.print(sh.rp.getProb(i, Util.WEREWOLF) + " ");
		}
		System.out.println();
		for (int i = 0; i < numAgents; i++) {
			if (i != meint) {
				if (sh.gamestate.agents[i].Alive) {
					double score = sh.rp.getProb(i, Util.VILLAGER) + 3 * sh.rp.getProb(i, Util.SEER)
							+ sh.rp.getProb(i, Util.MEDIUM);
					score += 1.0 * wincnt[i] / (gamecount + 0.01);
					if (mn < score) {
						mn = score;
						c = i;
					}
				}
			}
		}
		guardedAgent = currentGameInfo.getAgentList().get(c);
		return guardedAgent;
	}

}
