package org.aiwolf.Basket;

import java.util.ArrayList;

import org.aiwolf.client.lib.AndContentBuilder;
import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.EstimateContentBuilder;
import org.aiwolf.client.lib.GuardedAgentContentBuilder;
import org.aiwolf.client.lib.NotContentBuilder;
import org.aiwolf.client.lib.RequestContentBuilder;
import org.aiwolf.client.lib.VoteContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

/**
 * 狩人役エージェントクラス
 */
public class BasketBodyguard extends BasketVillager {

	Agent guardedAgent;
	boolean doCO = false;	//市田追加
	boolean houkoku = true;	//市田追加
	int target;	//市田追加
	GameData lastGameData;

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
		
		doCO = false;	//市田追加
		houkoku = true;	//市田追加

		gamedata.clear();
		sh.head = 0;
		sh.game_init(fixed, meint, numAgents, Util.BODYGUARD, params);
		update_sh = true;
		before = -1;
	}
	
	//ここから市田追加
	//護衛が成功したか判定
	public void dayStart() {
		super.dayStart();
		
		if (!gamedata.isEmpty()) {
			lastGameData = gamedata.get(gamedata.size() - 1);
			if (lastGameData.type != DataType.KILLED && day > 1 ) {
				houkoku = false;
			}
		}
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
		
		//3人以上が自分に敵意を見せた場合にhostilityを1にする
		int hostility = 0;
		if (sh.gamestate.cnt_vote(meint) >= 3) {
			hostility = 1;
		}
		//hostilityが1になったらCOする		
		if (!doCO && hostility == 1) {
			doCO = true;
			return (new Content(new ComingoutContentBuilder(me, Role.BODYGUARD))).getText();
		}
		
		//護衛成功したらまずCOする
		if (!houkoku && !doCO) {
			doCO = true;
			return (new Content(new ComingoutContentBuilder(me, Role.BODYGUARD))).getText();			
		}
		//護衛成功したら護衛対象を報告する
		if (!houkoku && doCO) {
			houkoku = true;
			return (new Content(new AndContentBuilder(new Content(new GuardedAgentContentBuilder(currentGameInfo.getAgentList().get(target))),
					new Content(new NotContentBuilder(new Content(new EstimateContentBuilder(currentGameInfo.getAgentList().get(target), Role.WEREWOLF))))))).getText();
		}

		if (before != c) {
			if (!isValidIdx(c)) {
				return null;
			}
			voteCandidate = currentGameInfo.getAgentList().get(c);
			 {
					ContentRand = Math.random();
					// 30%
					if(ContentRand <= 0.3) {return (new Content(new VoteContentBuilder(voteCandidate))).getText();}
					// 40%
					else if( ContentRand > 0.3 && ContentRand >= 0.7){return (new Content(new EstimateContentBuilder(voteCandidate, Role.WEREWOLF))).getText();}
					// 30% REQUEST ANY (VOTE [agent])
					else {return (new Content(new RequestContentBuilder(Content.ANY, new Content(new VoteContentBuilder(voteCandidate))))).getText();}
			 }
		}
//		before = c;
		return null;
	}	
	//ここまで市田追加

	//護衛対象の選択
	//村人らしさ + 3 * 占い師らしさ + 霊媒師らしさ　に勝率を補正して最も高いプレイヤーを選択
	public Agent guard() {
		//gamedataを最新まで読み込み、再度役職推定
		sh.process(params, gamedata); 
		sh.update();

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
		target = c;		//護衛対象を記録しておく。市田追加
		return guardedAgent;
	}

}
