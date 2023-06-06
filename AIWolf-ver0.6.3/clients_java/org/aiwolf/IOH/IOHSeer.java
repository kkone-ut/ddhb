package org.aiwolf.IOH;

import java.util.ArrayList;

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
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;


// 占い師役エージェントクラス
public class IOHSeer extends IOHBasePlayer {
	// COしたかどうか
	int comingoutDay;
	boolean isCameout;
    boolean doFO = false;

	Judge divination;

	boolean[] divined;
	boolean f = true;
	Parameters params;
	boolean doCO = false;
	boolean houkoku = true;
	boolean pos;
	boolean update_sh = true;
    double ContentRand;

	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		if (f) {
			params = new Parameters(numAgents);

			sh = new StateHolder(numAgents);

			f = false;
		}
		update_sh = true;
		pos = false;
		doCO = false;
		houkoku = true;

		divined = new boolean[numAgents];
		for (int i = 0; i < numAgents; i++)
			divined[i] = false;
		ArrayList<Integer> fixed = new ArrayList<Integer>();
		fixed.add(meint);
		sh.process(params, gamedata);
		gamedata.clear();
		sh.head = 0;
		sh.game_init(fixed, meint, numAgents, Util.SEER, params);

		before = -1;

	}

	public void dayStart() {
		super.dayStart();
		divination = currentGameInfo.getDivineResult();
		if (divination != null) {
			divined[divination.getTarget().getAgentIdx() - 1] = true;
			houkoku = false;
			gamedata.add(new GameData(DataType.DIVINED,
				day, meint,
				divination.getTarget().getAgentIdx() - 1,
				divination.getResult() == Species.HUMAN));
		}
		sh.process(params, gamedata);
	}
	
	// これはなんだ？
	protected void init() {

	}

	// 投票の時に使うメソッド？
	protected Agent chooseVote() {
		gamedata.add(new GameData(DataType.VOTESTART, day, meint, meint, false));

		sh.process(params, gamedata);

		int c = chooseMostLikelyWerewolf();

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

		// フルオープンの処理
		if(!doFO) {
			doFO = true;
			
			return (new Content(new RequestContentBuilder(Content.ANY, new Content(new ComingoutContentBuilder(Content.ANY,Role.ANY))))).getText();
		}
		
		if (update_sh) {
			System.out.println("SEARCH");
			update_sh = false;
			sh.serach(1000);
		}

		int c = 0;

		for (int i = 0; i < numAgents; i++) {
			System.out.print(sh.rp.getProb(i, Util.WEREWOLF) + " ");
		}
		System.out.println();
		c = chooseMostLikelyWerewolf();
		if (getAliveAgentsCount() <= 3) {
			if (!pos) {
				pos = true;
				double all = 0;
				double alive = 0;
				for (int i = 0; i < numAgents; i++) {
					all += sh.rp.getProb(i, Util.POSSESSED);
					if (sh.gamestate.agents[i].Alive) {
						alive += sh.rp.getProb(i, Util.POSSESSED);
					}
				}
				if (alive > 0.5 * all) {
					doCO = true;
					houkoku = true;
					return (new Content(new ComingoutContentBuilder(me, Role.WEREWOLF))).getText();
				}
			}
		}

		if (!doCO) {
			doCO = true;
			return (new Content(new ComingoutContentBuilder(me, Role.SEER))).getText();

		}
		if (!houkoku) {
			houkoku = true;

			if (numAgents == 5 && day == 1) {
				return (new Content(new DivinedResultContentBuilder(currentGameInfo.getAgentList().get(c),
						Species.WEREWOLF))).getText();
			} else {
				return (new Content(new DivinedResultContentBuilder(divination.getTarget(),
						divination.getResult()))).getText();
			}
		}
		if (before != c) {
			if (!isValidIdx(c)) {return null;}
			voteCandidate = currentGameInfo.getAgentList().get(c);
//			before = c;
			{
					ContentRand = Math.random();
					// 30%
					if(ContentRand <= 0.3) {return (new Content(new VoteContentBuilder(voteCandidate))).getText();}
					// 40%
					else if( ContentRand > 0.3 && ContentRand <= 0.7){return (new Content(new EstimateContentBuilder(voteCandidate, Role.WEREWOLF))).getText();}
					// 30% REQUEST ANY (VOTE [agent])
					else {return (new Content(new RequestContentBuilder(Content.ANY, new Content(new VoteContentBuilder(voteCandidate))))).getText();}
			}
		}
		// before = c;
		return Talk.SKIP;
	}

	public Agent divine() {
		sh.process(params, gamedata);
		sh.update();
		double mn = -1;
		int c = -1;

		for (int i = 0; i < numAgents; i++) {
			if (i != meint) {
				if (sh.gamestate.agents[i].Alive) {
					if (!divined[i]) {
						double score = sh.rp.getProb(i, Util.WEREWOLF);
						if (mn < score) {
							mn = score;
							c = i;
						}
					}
				}
			}
		}

		if (!isValidIdx(c)) {
			return null;
		}

		return currentGameInfo.getAgentList().get(c);
	}

}