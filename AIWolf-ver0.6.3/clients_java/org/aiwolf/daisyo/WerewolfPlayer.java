package org.aiwolf.daisyo;

import java.util.ArrayList;

import org.aiwolf.client.lib.AttackContentBuilder;
import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DivinedResultContentBuilder;
import org.aiwolf.client.lib.EstimateContentBuilder;
import org.aiwolf.client.lib.VoteContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

public class WerewolfPlayer extends BasePlayer {

	StateHolder sh2;
	boolean isFirst = true;
	Parameters params;
	boolean seer = false;
	boolean doCO = false;
	boolean isReported = false;
	boolean pos = false;
	boolean[] divined;
	boolean[] friend;
	int votecnt = 0;
	boolean update_sh = true;
	boolean kyoujin_ikiteru = false;

	Agent attakcVoteCandidate = null;
	int beforeAttack = -1;
	int attackCur = 0;

	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		try {
			super.initialize(gameInfo, gameSetting);
			if (isFirst) {
				params = new Parameters(numAgents);
				sh = new StateHolder(numAgents);
				sh2 = new StateHolder(numAgents);
				isFirst = false;
			}

			update_sh = true;
			doCO = false;
			isReported = false;
			pos = false;
			kyoujin_ikiteru = false;
			divined = new boolean[numAgents];
			for (int i = 0; i < numAgents; ++i) {
				divined[i] = false;
			}

			ArrayList<Integer> fixed = new ArrayList<Integer>();
			friend = new boolean[numAgents];
			for (int i = 0; i < numAgents; ++i) friend[i] = false;

			for (Agent a : gameInfo.getRoleMap().keySet()) {
				fixed.add(a.getAgentIdx()-1);
				friend[a.getAgentIdx()-1] = true;
			}

			sh.process(params, gamedata);
			sh2.process(params, gamedata);

			gamedata.clear();
			sh.head = 0;
			sh2.head = 0;
			seer = false;

			sh.game_init(fixed, myidx, numAgents, Util.WEREWOLF, params);
			fixed.clear();
			fixed.add(myidx);

			// COはしない
			//		if (numAgents == 15 && gamecnt >= 70 && rnd.nextDouble() < 0.8) {
			//			seer = true;
			//		}

			if(numAgents == 5 && rnd.nextDouble() < 0.3) {
				seer = true;
			}else {
				seer = false;
			}
			
			if (numAgents == 15 && gamecnt >= 60) {
				seer = true;
			}

			if (seer) sh2.game_init(fixed, myidx, numAgents, Util.SEER, params);
			else sh2.game_init(fixed, myidx, numAgents, Util.VILLAGER, params);

			before = -1;

			attakcVoteCandidate = null;
		}
		catch (Exception e) {
			;
		}
	}

	public void dayStart() {
		super.dayStart();
		isReported = false;
		votecnt = 0;

		attakcVoteCandidate = null;
		beforeAttack = -1;
		attackCur = 0;
	}

	protected Agent chooseVote() {
		gamedata.add(new GameData(DataType.VOTESTART, day, -1, myidx, myidx, false));
		sh.process(params, gamedata);
		sh2.process(params, gamedata);

		double mn = -1;
		int c = 0;
		if (getAliveAgentsCount() <= 3) {
			votecnt++;
			if (votecnt == 1) {
				for (int i = 0; i < numAgents; ++i) {
					if (i != myidx && sh.gamestate.agents[i].isAlive) {
						double score = 1 - sh.rp.getProb(i, Util.POSSESSED);
						if (mn < score) {
							mn = score;
							c = i;
						}
					}
				}
			}
			else {
				c = -1;
				for (int i = 0; i < numAgents; ++i) {
					if (i != myidx && sh.gamestate.agents[i].isAlive && sh.gamestate.agents[i].votefor == myidx) {
						c = i;
					}
				}
				if (c == -1) {
					for (int i = 0; i < numAgents; ++i) {
						if (i != myidx && sh.gamestate.agents[i].isAlive) {
							double score = sh.rp.getProb(i, Util.POSSESSED);
							if (mn < score) {
								mn = score;
								c = i;
							}
						}
					}
				}
			}
		}
		else {
			if (numAgents == 5) {
				c = -1;
				mn = -1;
				for (int i = 0; i < numAgents; ++i) {
					if (i != myidx && sh.gamestate.agents[i].isAlive) {
						double score = sh.gamestate.cnt_vote(i) + sh2.rp.getProb(i, Util.WEREWOLF);
						if (mn < score) {
							mn = score;
							c = i;
						}
					}
				}

				for (int i = 0; i < numAgents; ++i) {
					if (i != myidx && sh.gamestate.agents[i].isAlive) {
						double score = sh2.rp.getProb(i, Util.WEREWOLF);
						if (mn < score) {
							mn = score;
							c = i;
						}
					}
				}
			}
			else {
				for (int i = 0; i < numAgents; ++i) {
					if (i != myidx && sh.gamestate.agents[i].isAlive) {
						double score = sh.gamestate.cnt_vote(i);
						if (mn < score) {
							mn = score;
							c = i;
						}
					}
				}
				if (mn * 2 < currentGameInfo.getAliveAgentList().size()) {
					mn = -1;
					for (int i = 0; i < numAgents; ++i) {
						if (i != myidx && sh.gamestate.agents[i].isAlive) {
							double score = sh2.rp.getProb(i, Util.WEREWOLF);
							if (mn < score) {
								mn = score;
								c = i;
							}
						}
					}
				}
			}
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

		if(seer) {

			if(!doCO){
				doCO = true;
				return (new Content(new ComingoutContentBuilder(me, Role.SEER))).getText();
			}

			if(!isReported){

				isReported = true;
				for(int i=0;i<numAgents;i++){
					if(i!=myidx){
						if(sh.gamestate.agents[i].isAlive){
							if(!divined[i]){
								double score = sh2.rp.getProb(i, Util.VILLAGER);
								if(mn < score){
									mn = score;
									c=i;
								}
							}
						}
					}
				}

				if (numAgents == 5) {
					divined[c] = true;
					sh2.scorematrix.divined(sh2.gamestate, myidx, c, true);
					return (new Content(new DivinedResultContentBuilder(currentGameInfo.getAgentList().get(c), Species.HUMAN))).getText();
				}
				else {
					divined[c] = true;
					sh2.scorematrix.divined(sh2.gamestate, myidx, c, false);
					return (new Content(new DivinedResultContentBuilder(currentGameInfo.getAgentList().get(c), Species.WEREWOLF))).getText();
				}
			}
		}


		if (numAgents == 5) {
			if (gamecnt >= 5) {
				if (day == 1 && sh.gamestate.turn == 1) {
					return Talk.SKIP;
				}
			}
		}
		else if (numAgents == 15) {
			if (gamecnt >= 10) {
				if (day == 1 && sh.gamestate.turn == 1) {
					return Talk.SKIP;
				}
			}
		}

		if (numAgents == 5) {
			c = -1;
			mn = -1;
			for (int i = 0; i < numAgents; ++i) {
				if (i != myidx && sh.gamestate.agents[i].isAlive) {
					double score = sh2.rp.getProb(i, Util.WEREWOLF);
					if (mn < score) {
						mn = score;
						c = i;
					}
				}
			}
			if (c == -1) mn = -1;
			for (int i = 0; i < numAgents; ++i) {
				if (i != myidx && sh.gamestate.agents[i].isAlive) {
					double score = sh2.rp.getProb(i, Util.WEREWOLF);
					if (mn < score) {
						mn = score;
						c = i;
					}
				}
			}
		}
		else {
			for (int i = 0; i < numAgents; ++i) {
				if (i != myidx && sh.gamestate.agents[i].isAlive) {
					double score = sh.gamestate.cnt_vote(i);
					if (mn < score) {
						mn = score;
						c = i;
					}
				}
			}
			if (mn * 2 < getAliveAgentsCount()) {
				mn = -1;
				for (int i = 0; i < numAgents; ++i) {
					if (i != myidx && sh.gamestate.agents[i].isAlive) {
						double score = sh2.rp.getProb(i, Util.WEREWOLF);
						if (friend[i]) score -= 0.4;
						if (mn < score) {
							mn = score;
							c = i;
						}
					}
				}
			}
		}

		if (numAgents == 5) {
			if (sh.gamestate.cnt_vote(myidx) * 2 >= getAliveAgentsCount()) {
				before = -1;
			}
			if (sh.gamestate.cnt_vote(c) * 2 < getAliveAgentsCount()) {
				before = -1;
			}
		}
		else {
			if (sh.gamestate.cnt_vote(myidx) * 2 >= getAliveAgentsCount()) {
				before = -1;
			}
		}

		if (before != c) {
			voteCandidate = currentGameInfo.getAgentList().get(c);
			before = c;
			return (new Content(new VoteContentBuilder(voteCandidate))).getText();
		}

//		if (gamecnt >= 10 && rnd.nextDouble() < 0.4) {
//			return Talk.OVER;
//		}

		return Talk.SKIP;
	}

	protected Agent attackVote() {
		sh.process(params, gamedata);
		sh.update();
		double mn = 01;
		int c = 0;

		if (numAgents == 5) {
			for (int i = 0; i < numAgents; ++i) {
				if (i != myidx && sh.gamestate.agents[i].isAlive) {
					double score = 1 - sh.rp.getProb(i, Util.POSSESSED);
					if (mn < score) {
						mn = score;
						c = i;
					}
				}
			}
		}
		else {
			for (int i = 0; i < numAgents; ++i) {
				if (i != myidx && !friend[i]) {
					if (sh.gamestate.agents[i].isAlive) {
						double score = 1 - sh.rp.getProb(i, Util.POSSESSED);
						score += 0.2 * sh.rp.getProb(i, Util.SEER);
						score += 0.1 * sh.rp.getProb(i, Util.BODYGUARD);
						score += 3 * wincnt[i] / (gamecnt + 0.01);
						if (mn < score) {
							mn = score;
							c = i;
						}
					}
				}
			}
		}

		return currentGameInfo.getAgentList().get(c);
	}

	protected String chooseWhisper() {

		Agent agent = attackVote();
		if (agent.getAgentIdx() - 1 != beforeAttack) {
			beforeAttack = agent.getAgentIdx() - 1;
			return (new Content(new AttackContentBuilder(agent))).getText();
		}

		if (attackCur == 0) {
			attackCur++;

			double mn = -1;
			int c = -1;
			for (int i = 0; i < numAgents; ++i) {
				if (i != myidx) {
					double score = sh.rp.getProb(i, Util.SEER);
					if (mn < score) {
						mn = score;
						c = i;
					}
				}
			}

			if (c != -1 && sh.gamestate.agents[c].isAlive) return (new Content(new EstimateContentBuilder(currentGameInfo.getAgentList().get(c), Role.SEER))).getText();
		}
		else if (attackCur == 1) {
			attackCur++;

			double mn = -1;
			int c = -1;
			for (int i = 0; i < numAgents; ++i) {
				if (i != myidx) {
					double score = sh.rp.getProb(i, Util.POSSESSED);
					if (mn < score) {
						mn = score;
						c = i;
					}
				}
			}

			if (c != -1 && sh.gamestate.agents[c].isAlive) return (new Content(new EstimateContentBuilder(currentGameInfo.getAgentList().get(c), Role.POSSESSED))).getText();
		}

		return Talk.SKIP;
	}

}
