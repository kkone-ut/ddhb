package org.aiwolf.TOKU;

import java.util.ArrayList;

import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.IdentContentBuilder;
import org.aiwolf.client.lib.VoteContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

/**
 * 霊媒師役エージェントクラス
 */

public class TOKUMedium extends TOKUBasePlayer {
	Judge ident;

	int before = -1;
	boolean f = true;
	Parameters params;
	boolean doCO = false;
	boolean houkoku = true;
	int target;
	boolean white;
	boolean update_sh = true;

	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		if (f) {
			params = new Parameters(numAgents);

			sh = new StateHolder(numAgents);

			f = false;
		}

		doCO = false;
		houkoku = true;
		update_sh = true;

		sh.process(params, gamedata);
		gamedata.clear();
		sh.head = 0;

		ArrayList<Integer> fixed = new ArrayList<Integer>();
		fixed.add(meint);
		sh.game_init(fixed, meint, numAgents, Util.MEDIUM, params);

		before = -1;

	}

	public void dayStart() {
		super.dayStart();

		Judge ident = currentGameInfo.getMediumResult();
		if (ident != null) {
			houkoku = false;
			gamedata.add(new GameData(DataType.ID,
					day, meint,
					ident.getTarget().getAgentIdx() - 1,
					ident.getResult() == Species.HUMAN));
			target = ident.getTarget().getAgentIdx() - 1;
			white = (ident.getResult() == Species.HUMAN);
		}
		sh.process(params, gamedata);
	}

	protected void init() {

	}

	protected Agent chooseVote() {
		gamedata.add(new GameData(DataType.VOTESTART, day, meint, meint, false));

		sh.process(params, gamedata);

		double mn = -1;
		int c = 0;
		for (int i = 0; i < numAgents; i++) {
			if (i != meint) {
				if (sh.gamestate.agents[i].Alive) {
					if (mn < sh.rp.getProb(i, Util.WEREWOLF)) {
						mn = sh.rp.getProb(i, Util.WEREWOLF);
						c = i;
					}
				}
			}
		}
		if (!isValidIdx(c)) {
			return null;
		}
		return currentGameInfo.getAgentList().get(c);
	}

	protected String chooseTalk() {

		if (lastTurn == -1 || (lastTalkTurn == lastTurn)) {
			gamedata.add(new GameData(DataType.TURNSTART, day, meint, meint, false));
			lastTurn++;
		}

		sh.process(params, gamedata);

		lastTalkTurn = lastTurn;

		updateState(sh);
		if (update_sh) {
			System.out.println("SEARCH");
			update_sh = false;
			sh.serach(1000);
		}

		double mn = -1;
		int c = 0;

		for (int i = 0; i < numAgents; i++) {
			System.out.print(sh.rp.getProb(i, Util.WEREWOLF) + " ");
		}
		System.out.println();

		c = chooseMostLikelyExecuted(getAliveAgentsCount() * 0.7);
		if (c == -1) {
			c = chooseMostLikelyWerewolf();
		}
		System.out.println("willvote " + (c + 1));

		if (!doCO) {
			doCO = true;
			return (new Content(new ComingoutContentBuilder(me, Role.MEDIUM))).getText();

		}
		if (!houkoku) {
			houkoku = true;
			return (new Content(new IdentContentBuilder(currentGameInfo.getAgentList().get(target),
					white ? Species.HUMAN : Species.WEREWOLF))).getText();
		}
		if (before != c) {
			if (!isValidIdx(c)) {
				return null;
			}
			voteCandidate = currentGameInfo.getAgentList().get(c);
			before = c;
			return (new Content(new VoteContentBuilder(voteCandidate))).getText();
		}
		before = c;
		return Talk.SKIP;
	}

}
