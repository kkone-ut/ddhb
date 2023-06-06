package org.aiwolf.Basket;

import java.util.ArrayList;

import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.EstimateContentBuilder;
import org.aiwolf.client.lib.IdentContentBuilder;
import org.aiwolf.client.lib.RequestContentBuilder;
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

public class BasketMedium extends BasketBasePlayer {
	Judge ident;

	int before = -1;
	boolean f = true;
	Parameters params;
	boolean doCO = false;
	boolean houkoku = true;
	int target;
	boolean black;
	boolean update_sh = true;
	static int[][] koudouList;
	// ゲーム状況を取得・対抗が出ていないかを確認
	boolean taikou = false;
	// 多弁のために五十嵐追加
	double ContentRand;

	// 初期設定
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
		taikou = false;

		// Stateholder.java参照
		sh.process(params, gamedata);
		
		gamedata.clear();
		sh.head = 0;

		ArrayList<Integer> fixed = new ArrayList<Integer>();
		// 自分のインデックスを追加
		fixed.add(meint);
		sh.game_init(fixed, meint, numAgents, Util.MEDIUM, params);

		before = -1;

	}

	// 1日ごとに行われる処理
	public void dayStart() {
		super.dayStart();

		// 霊媒の結果の取得
		Judge ident = currentGameInfo.getMediumResult();
		if (ident != null) {
			houkoku = false;
			gamedata.add(new GameData(DataType.ID,
					day, meint,
					ident.getTarget().getAgentIdx() - 1,
					ident.getResult() == Species.HUMAN));
			target = ident.getTarget().getAgentIdx() - 1;
			black = (ident.getResult() == Species.WEREWOLF);
			
		}
		// Stateholder.java参照
		sh.process(params, gamedata);
	}
	// これはなに？
	protected void init() {

	}

	// 投票する人を選ぶ
	protected Agent chooseVote() {
		gamedata.add(new GameData(DataType.VOTESTART, day, meint, meint, false));

		sh.process(params, gamedata);
		
		// どのAgentが人狼の可能性が高いかを取得する
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
		// その人に投票する
		return currentGameInfo.getAgentList().get(c);
	}

	// このメソッドに行動が書かれてる
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

		// Agentそれぞれに対する、人狼の可能性を出力
		for (int i = 0; i < numAgents; i++) {
			System.out.print(sh.rp.getProb(i, Util.WEREWOLF) + " ");
		}

		// 一番黒っぽい人の選出
		c = chooseMostLikelyExecuted(getAliveAgentsCount() * 0.7);
		if (c == -1) {
			c = chooseMostLikelyWerewolf();
		}
		System.out.println("willvote " + (c + 1));

		/*
		// 論点1: COするタイミング
		// パターン1: COしてなかったら、しちゃう
		if (!doCO) {
			doCO = true;
			return (new Content(new ComingoutContentBuilder(me, Role.MEDIUM))).getText();
		}
		*/
		
		// パターン2: 黒が出たらCOする		
		// 対抗が出ていない場合のみこの処理を実行
		if(!taikou) {
			// ここは応急処置
			if(day >= max_day) {
				taikou = true;
			}
			else {
				// その日のターンのどこかで誰かがCOしていたら、taikouをtrue
				koudouList = agentkoudou[day];
				for(int i = 0; i < koudouList.length; i++) {
					for(int j = 0; j < numAgents; j++) {
						if(koudouList[i][j] == 3) {
							taikou = true;
						}
					}	
				}	
			}
		}
		// 3人以上が自分に敵意を見せた場合にhostilityを1にする
		// TODO: 人数を変えて実験する
		int hostility = 0;
		if (sh.gamestate.cnt_vote(meint) >= 2) {
			hostility = 1;
		}
		
		
		// COしてない状況でCOする
		if (!doCO) {
			// 黒が出たらCOする
			// 対抗が出たらCOする
			// hostilityが1になったらCOする
			if(black || taikou || hostility == 1) {
				doCO = true;
				return (new Content(new ComingoutContentBuilder(me, Role.MEDIUM))).getText();
			}
		}
		
		// 報告してなかったら、正直に報告
		if (doCO && !houkoku) {
			houkoku = true;
			return (new Content(new IdentContentBuilder(currentGameInfo.getAgentList().get(target),
					black ? Species.WEREWOLF : Species.HUMAN))).getText();
		}
		// だれに投票するべきか考えてる
		if (before != c) {
			if (!isValidIdx(c)) {
				return Talk.SKIP;
			}
			voteCandidate = currentGameInfo.getAgentList().get(c);
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
}
