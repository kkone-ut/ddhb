package org.aiwolf.daisyo;

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

public class MediumPlayer extends BasePlayer {

	Judge ident;
	int before = -1;
	boolean isFirst = true;
	Parameters params;
	boolean doCO = false;
	boolean isReported = false;
	int target;
	boolean isWhite;
	boolean update_sh = true;

	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {

		try {
			super.initialize(gameInfo, gameSetting);

			if (isFirst) {
				params = new Parameters(numAgents);
				sh = new StateHolder(numAgents);
				isFirst = false;
			}

			doCO = false;
			isReported = true;
			update_sh = true;

			sh.process(params, gamedata);
			gamedata.clear();
			sh.head = 0;

			ArrayList<Integer> fixed = new ArrayList<Integer>();
			fixed.add(myidx);
			sh.game_init(fixed, myidx, numAgents, Util.MEDIUM, params);
			before = -1;
		}
		catch (Exception e) {
			;
		}
	}

	public void dayStart() {

		super.dayStart();

		try {

			Judge identJudge = currentGameInfo.getMediumResult();
			if (ident != null) {
				isReported = false;
				gamedata.add(new GameData(DataType.ID, day, -1, myidx, identJudge.getTarget().getAgentIdx()-1, ident.getResult() == Species.HUMAN));
				target = ident.getTarget().getAgentIdx() - 1;
				isWhite = (ident.getResult() == Species.HUMAN);
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

		double mn = -1;
		int c = -1;
		for (int i = 0; i < numAgents; ++i) {
			if (i != myidx && sh.gamestate.agents[i].isAlive && mn < sh.rp.getProb(i, Util.WEREWOLF)) {
				mn = sh.rp.getProb(i, Util.WEREWOLF);
				c = i;
			}
		}

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

		double mn = -1;
		int c = 0;
		for (int i = 0; i < numAgents; ++i) {
			if (i != myidx && sh.gamestate.agents[i].isAlive && mn < sh.rp.getProb(i, Util.WEREWOLF)) {
				mn = sh.rp.getProb(i, Util.WEREWOLF);
				c = i;
			}
		}

		if (!doCO) {
			doCO = true;
			return (new Content(new ComingoutContentBuilder(me, Role.MEDIUM))).getText();
		}

		if (!isReported) {
			isReported = true;
			return (new Content(new IdentContentBuilder(currentGameInfo.getAgentList().get(target), isWhite ? Species.HUMAN : Species.WEREWOLF))).getText();
		}

		if (before != c) {
			voteCandidate = currentGameInfo.getAgentList().get(c);
			before = c;
			return (new Content(new VoteContentBuilder(voteCandidate))).getText();
		}
		before = c;

		return Talk.SKIP;
	}
}
