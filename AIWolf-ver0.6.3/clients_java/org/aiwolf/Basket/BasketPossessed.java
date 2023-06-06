package org.aiwolf.Basket;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.aiwolf.client.lib.AndContentBuilder;
import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DivinedResultContentBuilder;
import org.aiwolf.client.lib.EstimateContentBuilder;
import org.aiwolf.client.lib.GuardedAgentContentBuilder;
import org.aiwolf.client.lib.IdentContentBuilder;
import org.aiwolf.client.lib.NotContentBuilder;
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
 * 狂人役エージェントクラス
 */
public class BasketPossessed extends BasketBasePlayer {

	// パラメータたち
	Deque<Judge> divinationQueue = new LinkedList<>();
	Map<Agent, Species> myDivinationMap = new HashMap<>();
	List<Agent> whiteList = new ArrayList<>();
	List<Agent> blackList = new ArrayList<>();
	List<Agent> grayList;
	List<Agent> semiWolves = new ArrayList<>();
	List<Agent> possessedList = new ArrayList<>();
	StateHolder sh2;
	boolean f = true;
	Parameters params;
	// 何にCOするかのフラグ（0: seer, 1: medium, 2: bodyguard）
	int whatCO;
	// initializeで使う
	double COnum;
	boolean doCO = false;
	boolean doFO = false;
	boolean houkoku = true;
	boolean[] divined;
	boolean pos = false;
	boolean update_sh = true;
	int black_count;
	// 狩人騙りで使うやつ
	GameData lastGameData;
	boolean guarded;
	int target;
	// 多弁で使う
	double ContentRand;

	// ゲーム開始時の初期設定 +　五十嵐 doFO 追加
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		if (f) {
			params = new Parameters(numAgents);
			sh = new StateHolder(numAgents);
			sh2 = new StateHolder(numAgents);
			f = false;
		}
		// その他のパラメータの初期化
		update_sh = true;
		doCO = false;
		doFO = false;
		houkoku = true;
		pos = false;
		divined = new boolean[numAgents];
		for (int i = 0; i < numAgents; i++)
			divined[i] = false;

		// 方針決め
		// 5人なら絶対占い師
		if (numAgents == 5) {
			whatCO = 0;
		}
		// 15人村
		else {
			COnum = Math.random();
			// 65%で占い師
			if(COnum <= 0.65) {whatCO = 0;}
			// 35%で霊媒師
			else {whatCO = 1;}
		}

		ArrayList<Integer> fixed = new ArrayList<Integer>();
		fixed.add(meint);
		sh.process(params, gamedata);
		sh2.process(params, gamedata);
		gamedata.clear();

		sh.head = 0;
		sh2.head = 0;
		sh.game_init(fixed, meint, numAgents, Util.POSSESSED, params);
		sh2.game_init(fixed, meint, numAgents, Util.SEER, params);

		before = -1;
		guarded = false;
		target = -1;
	}

	// 1日が始まるときの処理
	public void dayStart() {
		super.dayStart();
		if(whatCO != 2) {houkoku = false;}
		// 狩人騙りのとき、護衛成功したかどうかを見る
		if (whatCO == 2 &&  !gamedata.isEmpty()) {
			lastGameData = gamedata.get(gamedata.size() - 1);
			if (lastGameData.type != DataType.KILLED && day > 1 ) {
				houkoku = false;
			}
		}
	}

	protected Agent chooseVote() {
		gamedata.add(new GameData(DataType.VOTESTART, day, meint, meint, false));

		sh.process(params, gamedata);
		//System.out.println("alive = " + currentGameInfo.getAliveAgentList().size());

		for (int i = 0; i < numAgents; i++) {
			System.out.print(sh.rp.getProb(i, Util.WEREWOLF) + " ");
		}
		System.out.println();

		for (int i = 0; i < numAgents; i++) {
			System.out.print(sh.rp.getProb(i, Util.SEER) + " ");
		}
		System.out.println();

		double mn = -1;
		int c = 0;
		if (currentGameInfo.getAliveAgentList().size() <= 3) {
			for (int i = 0; i < numAgents; i++) {
				if (i != meint && sh.gamestate.agents[i].Alive) {
					double score = 1 - sh.rp.getProb(i, Util.WEREWOLF);
					if (mn < score) {
						mn = score;
						c = i;
					}
				}
			}
		}
		else {
			mn = -100;
			
			// 5人村だったら、人狼っぽくない人に投票する
			if (numAgents == 5) {
				for (int i = 0; i < numAgents; i++) {
					if (i != meint && sh.gamestate.agents[i].Alive) {
						double score = sh2.rp.getProb(i, Util.WEREWOLF) - sh.rp.getProb(i, Util.WEREWOLF);
						if (mn < score) {
							mn = score;
							c = i;
						}
					}
				}
			} 
			// 15人村だったら、人狼っぽい人に投票
			else {
				for (int i = 0; i < numAgents; i++) {
					if (i != meint && sh.gamestate.agents[i].Alive) {
						double score = sh.rp.getProb(i, Util.WEREWOLF);
						if (mn < score) {
							mn = score;
							c = i;
						}
					}
				}
				int t = sh.gamestate.agents[c].will_vote;

				mn = -100;
				if (t == -1) {
					for (int i = 0; i < numAgents; i++) {
						if (i != meint && sh.gamestate.agents[i].Alive) {
							double score = 1 - sh.rp.getProb(i, Util.WEREWOLF);
							if (mn < score) {
								mn = score;
								t = i;
							}
						}
					}
				}
				c = t;
			}
		}
		if (!isValidIdx(c)) {return null;}
		return currentGameInfo.getAgentList().get(c);
	}

	// ここに行動が書かれる
	protected String chooseTalk() {
		if (lastTurn == -1 || (lastTalkTurn == lastTurn)) {
			gamedata.add(new GameData(DataType.TURNSTART, day, meint, meint, false));
			lastTurn++;
		}
		// ゲーム状況の更新
		sh.process(params, gamedata);
		sh2.process(params, gamedata);
		lastTalkTurn = lastTurn;
		updateState(sh);
		updateState(sh2);
		if (update_sh) {
			System.out.println("SEARCH");
			update_sh = false;
			sh.serach(1000);
			sh2.serach(1000);
		}
		double mn = -1;
		int c = 0;
		
		// フルオープンの処理
		if(!doFO) {
			doFO = true;
			return (new Content(new RequestContentBuilder(Content.ANY, new Content(new ComingoutContentBuilder(Content.ANY,Role.ANY))))).getText();
		}

		switch (whatCO) {
			// 占い師を騙る時の処理
			case 0: 
				// 即CO
				if (!doCO) {
					// seerに2人以上出ていないか確認
					int num_seer = 0;
					for (int i = 0; i < numAgents; i++) {
						if (i != meint && sh.gamestate.agents[i].Alive && (agents[i].COrole == Role.SEER)) {
								num_seer += 1;
						}
					}
					if(num_seer >= 2) {
						whatCO = 2;
						houkoku = true;
						break;
					}
					doCO = true;
					return (new Content(new ComingoutContentBuilder(me, Role.SEER))).getText();
				}
				if (!houkoku) {
					houkoku = true;
					// 5人村の時
					if (numAgents == 5) {
						c = -1;
						// 対抗がいたら、対抗が黒だと言う
						for (int i = 0; i < numAgents; i++) {
							if (i != meint && sh.gamestate.agents[i].Alive) {
									if (!divined[i] && sh.gamestate.agents[i].corole == Util.SEER) {c = i;}
								}
						}
						if (c != -1) {
							divined[c] = true;
							sh2.scorematrix.divined(sh2.gamestate, meint, c, false);
							Agent target = currentGameInfo.getAgentList().get(c);
							return (new Content(new DivinedResultContentBuilder(target, Species.WEREWOLF))).getText();
						} 
						// 対抗がいなければ、最も人狼っぽい人を白だと言う
						else {
							mn = -1;
							for (int i = 0; i < numAgents; i++) {
								if (i != meint && sh.gamestate.agents[i].Alive) {
									if (!divined[i] && mn < sh.rp.getProb(i, Util.WEREWOLF)) {
										mn = sh.rp.getProb(i, Util.WEREWOLF);
										c = i;
									}
								}
							}
							divined[c] = true;
							sh2.scorematrix.divined(sh2.gamestate, meint, c, true);
							Agent target = currentGameInfo.getAgentList().get(c);
							return (new Content(new DivinedResultContentBuilder(target, Species.HUMAN))).getText();
						}
					}
					// 15人村の時
					else {
						// 2日目以外
						if (day != 2) {
							mn = -100;
							// 最も村人っぽい人を探す
							for (int i = 0; i < numAgents; i++) {
								if (i != meint && sh.gamestate.agents[i].Alive) {
									if (!divined[i]) {
										double score = 1 - sh.rp.getProb(i, Util.WEREWOLF);
										if (sh.gamestate.agents[i].corole != -1) {
											score -= 1.0;
										}
										if (mn < score) {
											mn = score;
											c = i;
										}
									}
								}
							}
							// 村人っぽい人に対して黒を出す
							divined[c] = true;
							sh2.scorematrix.divined(sh2.gamestate, meint, c, false);
							Agent target = currentGameInfo.getAgentList().get(c);
							return (new Content(new DivinedResultContentBuilder(target,Species.WEREWOLF))).getText();
						} 
						// 2日目
						else {
							mn = -100;
							// 最も人狼っぽい人を探す
							for (int i = 0; i < numAgents; i++) {
								if (i != meint && sh.gamestate.agents[i].Alive) {
									if (!divined[i]) {
										double score = sh.rp.getProb(i, Util.WEREWOLF);
										if (sh.gamestate.agents[i].corole != -1) {
											score -= 1.0;
										}
										if (mn < score) {
											mn = score;
											c = i;
										}
									}
								}
							}
							// 人狼っぽい人に対して白を出す
							divined[c] = true;
							sh2.scorematrix.divined(sh2.gamestate, meint, c, true);
							Agent target = currentGameInfo.getAgentList().get(c);
							return (new Content(new DivinedResultContentBuilder(target,Species.WEREWOLF))).getText();
						}
					}
				}
				break;

			// 霊媒師を騙るときの処理
			case 1: 
				// 即CO
				if (!doCO) {
					// mediumに2人以上出ていないか確認
					int num_medium = 0;
					for (int i = 0; i < numAgents; i++) {
						if (i != meint && sh.gamestate.agents[i].Alive && (agents[i].COrole == Role.MEDIUM)) {
								num_medium += 1;
						}
					}
					if(num_medium >= 2) {
						whatCO = 2;
						houkoku = true;
						break;
					}
					doCO = true;
					return (new Content(new ComingoutContentBuilder(me, Role.MEDIUM))).getText();
				}
				// 霊媒結果の報告
				if(!houkoku) {
					houkoku = true;
					if (currentGameInfo.getExecutedAgent() != null) {
						Agent target = currentGameInfo.getExecutedAgent();
						c = target.getAgentIdx() - 1;
						 // 占い師COしていたプレイヤーは白判定にしておく
						if (agents[c].COrole == Role.SEER) {
							return (new Content(new IdentContentBuilder(target, Species.HUMAN))).getText();
						}
						// 占い師CO以外はとりあえず黒を出しておく(2人まで)
						else if (black_count <= 1) {
							black_count ++;
							return (new Content(new IdentContentBuilder(target, Species.WEREWOLF))).getText();
						}
						// それ以外は白
						return (new Content(new IdentContentBuilder(target, Species.HUMAN))).getText();
					}
				}
				break;

			// 狩人騙りの処理
			case 2: 
				//3人以上が自分に敵意を見せた場合にhostilityを1にする
				int hostility = 0;
				if (sh.gamestate.cnt_vote(meint) >= 3) {
					hostility = 1;
				}
				
				// 護衛成功もしくはhostilityが1になったらCOする		
				if (!doCO) {
					if( hostility == 1 || !houkoku) {
						doCO = true;
						return (new Content(new ComingoutContentBuilder(me, Role.BODYGUARD))).getText();	
					}
				}
				if(doCO && !houkoku && day >= 2) {
					// 最も人狼っぽい人を探す
					for (int i = 0; i < numAgents; i++) {
						if (i != meint && sh.gamestate.agents[i].Alive) {
							double score = sh2.rp.getProb(i, Util.WEREWOLF);
							if (mn < score) {
								mn = score;
								c = i;
							}
						}
					}
					// 最も人狼っぽい怪しい人に対して、その人を護衛した旨の発言
					if (!isValidIdx(c)) {return null;}
					houkoku = true;
					target = c;
					voteCandidate = currentGameInfo.getAgentList().get(c);
					return (new Content(new AndContentBuilder(new Content(new GuardedAgentContentBuilder(currentGameInfo.getAgentList().get(c))),
							new Content(new NotContentBuilder(new Content(new EstimateContentBuilder(currentGameInfo.getAgentList().get(c), Role.WEREWOLF))))))).getText();
				}
				break;
		}
		// 共通の処理
		// 生きている人が3人以下だったら、人狼COする
		if (currentGameInfo.getAliveAgentList().size() <= 3) {
			if (!pos) {
				pos = true;
				return (new Content(new ComingoutContentBuilder(me, Role.WEREWOLF))).getText();
			}
		}
		
		// 各々のAgentに対して人狼の可能性を出力
		sh2.update();
		for (int i = 0; i < numAgents; i++) {
			System.out.print(sh2.rp.getProb(i, Util.WEREWOLF) + " ");
		}
		System.out.println();

		// 乱数で方針を選ぶ
		// 最も人狼っぽい人を探す
		for (int i = 0; i < numAgents; i++) {
			if (i != meint && sh.gamestate.agents[i].Alive) {
				double score = sh2.rp.getProb(i, Util.WEREWOLF);
				if (mn < score) {
					mn = score;
					c = i;
				}
			}
		}
		System.out.println("willvote " + (c + 1));
		// 最も人狼っぽい怪しい人に対して、その人に投票する旨の発言
		if (before != c && target != c) {
			if (!isValidIdx(c)) {return null;}
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

		// ここはなんだ？？
		if (numAgents == 5) {
			if (sh.gamestate.cnt_vote(c) * 2 <= currentGameInfo.getAliveAgentList().size())
				;
		}
		// ここまでで何もなければ、SKIP
		return Talk.SKIP;
	}
}