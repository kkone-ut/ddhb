package org.aiwolf.IOH;

import java.util.ArrayList;

import org.aiwolf.client.lib.AttackContentBuilder;
import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DivinedResultContentBuilder;
import org.aiwolf.client.lib.EstimateContentBuilder;
import org.aiwolf.client.lib.GuardedAgentContentBuilder;
import org.aiwolf.client.lib.IdentContentBuilder;
import org.aiwolf.client.lib.RequestContentBuilder;
import org.aiwolf.client.lib.VoteContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

/**
 * 人狼役エージェントクラス
 */
public class IOHWerewolf extends IOHBasePlayer {
	 //ヘイトが集まったときに狩人や霊媒師COする
	boolean bodyguard = false;
	boolean medium = false;
	int black_count = 0; //占いor霊媒師COするときに使う。破綻しないように黒判定の数を数えておく。
	int co_count = 3; //自分へ投票を宣言する人がこの数を超えたとき、COする
	ArrayList<ArrayList<Double>> taikou; //対抗
	boolean[] guarded; //襲撃しようとしたが護衛されたプレイヤー。護衛されたらtrueにしておく。
	boolean alive_bodyguard = true; //狩人が生きているかどうか
	boolean attack_houkoku = false; //襲撃先をwhisperしたかどうか
	int whisper_called = -1; //whisperを行った場合はwhisperした襲撃先を襲撃するように、記録しておく。
	boolean co_whisper = false;
	boolean check_nakamaseer = false; //自分が占い師COしたいとき、talkの1ターン目に仲間のCOを確認。これを1回だけ実行するためのフラグ
	double ContentRand;
	StateHolder sh2; //stateholderを2つ持っておく(占い師騙りまたは村人として潜伏のため)
	boolean f = true; //たぶんfirst.一番最初だけおこなう処理
	Parameters params;
	boolean seer = false; //その試合で占いCOするかどうか
	boolean doCO = false; //占いCOしたかどうかのフラグ
	boolean houkoku = true; //占いCOしたゲームで使う。その日の結果を報告したかどうか
	boolean pos = false;
	int[] divined; //length = NumAgents. 占いCOしたゲームで使う。占った(ことになっている)プレイヤー。0→占っていない、1→白、-1→黒
	boolean[] nakama; //length = NumAgents. 仲間の人狼(仲間ならTrue)
	int votecnt = 0; //その日の投票回数(決選投票に対応するためのもの)
	boolean update_sh = true;
	boolean kyoujin_ikiteru = false; //PPの時に使うフラグ

	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting); //親(Baseplayer)のinitializeをまずおこなう
		if (f) { //100セットの中の最初の1回だけ
			params = new Parameters(numAgents);

			sh = new StateHolder(numAgents);
			sh2 = new StateHolder(numAgents);

			f = false;
		}
		update_sh = true;
		doCO = false;
		houkoku = true;
		pos = false;
		kyoujin_ikiteru = false; //PP用なので、最初に狂人は生きているが、falseにしておく
		divined = new int[numAgents];
		for (int i = 0; i < numAgents; i++)
			divined[i] = 0;

		ArrayList<Integer> fixed = new ArrayList<Integer>(); //StateHolderに渡すもの。自分目線、役職が確定しているプレイヤー(自分と仲間の人狼)

		nakama = new boolean[numAgents];
		for (int i = 0; i < numAgents; i++)
			nakama[i] = false;
		for (Agent a : gameInfo.getRoleMap().keySet()) { //仲間の人狼のリストを確認(読んでないけどたぶんそう)
			fixed.add(a.getAgentIdx() - 1);
			nakama[a.getAgentIdx() - 1] = true;
		}
		sh.process(params, gamedata); //initialize(1試合目→何もしない。それ以降→前の試合のデータを読み込む)
		sh2.process(params, gamedata);

		gamedata.clear(); //shに読み込んだら消去

		sh.head = 0;
		sh2.head = 0;

		sh.game_init(fixed, meint, numAgents, Util.WEREWOLF, params); //(自分を含む)人狼の役職を固定したうえで、役職推定に使うものをinitialize
		fixed.clear();
		fixed.add(meint); //sh2(自分が村陣営としたときのもの)のため、fixedの中身を自分だけにしておく
		if (gamecount == 0)
			seer = true;
		else if (rnd.nextDouble() > seerExecuted / gamecount) //占い師の吊られやすさに応じてCO確率を決める
			seer = true;
		else seer = false;

		if (seer) { //占い師COするなら
			sh2.game_init(fixed, meint, numAgents, Util.SEER, params); //sh2を自分が占い師だとしてinitialize
		} else { //占い師COしないなら
			sh2.game_init(fixed, meint, numAgents, Util.VILLAGER, params); //sh2を自分が村人だとしてinitialize
		}

		before = -1;
		if (numAgents == 15) {
			bodyguard = false;
			//最初から霊媒として出てみる(要検討)
			if (rnd.nextDouble() < 0.3)
				medium = true;
			else
				medium = false;
			//
			black_count = 0;
			alive_bodyguard = true;
			guarded = new boolean[numAgents];
			for (int i = 0; i < numAgents; i++)
				guarded[i] = false;
			check_nakamaseer = false;
		}
	}

	public void dayStart() {
		super.dayStart();
		houkoku = false;
		votecnt = 0;
		if (numAgents == 15) {
			attack_houkoku = false;
			whisper_called = -1;
			co_whisper = false;
			taikou = new ArrayList<ArrayList<Double>>();
			if (day >= 2) { //2日目以降(人狼の襲撃が発生して以降)
				if (currentGameInfo.getLastDeadAgentList().isEmpty()) { //死亡者がいないならば
					guarded[currentGameInfo.getAttackedAgent().getAgentIdx() - 1] = true; //襲撃対象を記録
					alive_bodyguard = true;
					System.out.println("guarded " + currentGameInfo.getAttackedAgent().toString());
				}
			}
		}
	}

	protected Agent chooseVote() {
		gamedata.add(new GameData(DataType.VOTESTART, day, meint, meint, false));

		sh.process(params, gamedata); //StateHolderの中でgamedataに追加された部分(すぐ上で追加したVoteStartの情報)を読み込み、RolePredictionのやり直し(recalc)、新しいassignnmentを設定(search)
		sh2.process(params, gamedata);
		double mn = -1;
		int c = 0;
		if (currentGameInfo.getAliveAgentList().size() <= 3) { //生存者が3人以下なら
			votecnt++;
			if (votecnt == 1) { //その日1回目の投票なら
				for (int i = 0; i < numAgents; i++) { //すべてのプレイヤーの中で
					if (i != meint) { //自分以外で
						if (sh.gamestate.agents[i].Alive) { //生きているなら
							double score = 1 - sh.rp.getProb(i, Util.POSSESSED); //狂人の可能性を取ってきて
							if (mn < score) { //最小値を記録する(1から引いたものの最大値を取っているため)
								mn = score;
								c = i;
							}
						}
					}
				}
			} else { //投票がその日2回目以降なら(決選投票)
				c = -1;
				System.out.println("PP"); //PP盤面になっていることを確認
				for (int i = 0; i < numAgents; i++) { //すべてのプレイヤーの中で
					if (i != meint) { //自分以外で
						if (sh.gamestate.agents[i].Alive) { //生きているなら
							if (sh.gamestate.agents[i].votefor == meint) { //自分に投票宣言しているなら TODO:投票宣言は実際の投票先と違う可能性があるため、要検討
								c = i; //記録する
							}
						}
					}
				}
				if (c == -1) { //自分に投票宣言した人がいなかったら
					for (int i = 0; i < numAgents; i++) //すべてのプレイヤーの中で
						if (i != meint) //自分以外で
							if (sh.gamestate.agents[i].Alive) { //生きているなら
								double score = sh.rp.getProb(i, Util.POSSESSED); //狂人の可能性を取ってきて
								if (mn < score) { //最大値を記録する(投票先を変える)
									mn = score;
									c = i;
								}
							}
				}
			}
		} else { //生存者が4人以上なら
			if (numAgents == 5) { //5人村なら
				c = -1;
				mn = -1;
				for (int i = 0; i < numAgents; i++) //すべてのプレイヤーの中で
					if (i != meint) //自分以外で
						if (sh.gamestate.agents[i].Alive) { //生きているなら
							double score = sh.gamestate.cnt_vote(i) + sh2.rp.getProb(i, Util.WEREWOLF); //そのプレイヤーの得票数(投票先として宣言されている数) + 自分が村陣営(村人または占い師)だとしたときのそのプレイヤーの人狼の可能性
							if (mn < score) {
								mn = score;
								c = i;
							}
						}
				if (c == -1) { //上で求めたscoreが-1以上となるプレイヤーがいない場合 NOTE:おそらくここが実行されることはない。上の条件でscoreが-1より小さくなるのならば、こちらの条件でもscoreは-1より小さくなってしまう(ため、この部分は意味がないように思われる)
					mn = -1;
					for (int i = 0; i < numAgents; i++) { //すべてのプレイヤーの中で
						if (i != meint) { //自分以外で
							if (sh.gamestate.agents[i].Alive) { //生きているなら
								double score = sh2.rp.getProb(i, Util.WEREWOLF); //村視点(村人または占い師)でのそのプレイヤーの人狼の可能性を取ってきて
								if (mn < score) { //最大値を記録する
									mn = score;
									c = i;
								}
							}
						}
					}
				}
			} else { //15人村なら
				for (int i = 0; i < numAgents; i++) { //すべてのプレイヤーの中で
					if (i != meint) { //自分以外で
						if (sh.gamestate.agents[i].Alive) { //生きているなら
							double score = sh.gamestate.cnt_vote(i); //そのプレイヤーの得票数を取ってきて
							if (mn < score) { //最大値を記録する
								mn = score;
								c = i;
							}
						}
					}
				}
				//票が割れているときにライン切り
				if (mn * 2 < currentGameInfo.getAliveAgentList().size()) { //(記録した得票数の最大値) × 2 が生存者の数より少なければ(つまり、半数以上の投票でなければ)
					if ((day < 3) && (!nakama[c])) { //3日目までで、最多得票が仲間でなければ
						int tmp = (int)mn; //最多得票を記録しておく
						mn = -1; //投票先を選びなおす
						for (int i = 0; i < numAgents; i++) { //すべてのプレイヤーの中で
							if (i != meint) { //自分以外で
								if (sh.gamestate.agents[i].Alive && nakama[i]) { //生きていて、仲間なら
									double score = sh2.rp.getProb(i, Util.WEREWOLF); //村視点(村人または占い師)でのそのプレイヤーの人狼の可能性を取ってきて
									if ((mn < score) && sh.gamestate.cnt_vote(i) + 1 < tmp) { //自分が投票しても大丈夫そうなら最大値を記録する
										mn = score;
										c = i;
									}
								}
							}
						}
					} else {
					mn = -1; //投票先を選びなおす
					for (int i = 0; i < numAgents; i++) { //すべてのプレイヤーの中で
						if (i != meint) { //自分以外で
							if (sh.gamestate.agents[i].Alive) { //生きているなら
								double score = sh2.rp.getProb(i, Util.WEREWOLF); //村視点(村人または占い師)でのそのプレイヤーの人狼の可能性を取ってきて
								if (mn < score) { //最大値を記録する
									mn = score;
									c = i;
								}
							}
						}
					}
				}
				}
			}
		}
		if (!isValidIdx(c)) { //投票先のインデックスがout of rangeしないようにする
			return null;
		}
		return currentGameInfo.getAgentList().get(c); //投票先を返す
	}

	protected String chooseTalk() {
		if (lastTurn == -1 || (lastTalkTurn == lastTurn)) {
			gamedata.add(new GameData(DataType.TURNSTART, day, meint, meint, false)); //ターンの更新をgamedataへ記録
			lastTurn++;
		}
		sh.process(params, gamedata); //gamedataを最新の情報まで読み込む
		sh2.process(params, gamedata);
		lastTalkTurn = lastTurn;

		updateState(sh); 
		updateState(sh2);
		if (update_sh) { //updateする必要があるなら
			System.out.println("SEARCH");
			update_sh = false;
			sh.serach(1000); //新しく1000個のassignmentを作る
			sh2.serach(1000);
		}
		double mn = -1;
		int c = 0;

		if (seer) { //占い師COする試合なら
			
			//3CO回避(talk turnの順番による。少なくとも、味方の人狼と被るのは回避)
			if (!check_nakamaseer) {
				System.out.println("--------seer check---------");
				check_nakamaseer = true;
				boolean nakamaseer = false;
				int num_seer = 0;
				for (int i = 0; i < numAgents; i++) {
					if (i != meint) {
						if (sh.gamestate.agents[i].Alive && (agents[i].COrole == Role.SEER)) {
							num_seer += 1;
							if (nakama[i]) nakamaseer = true;
						}
					}
				}
				if ((num_seer >= 2) || nakamaseer) { //すでに2COされているか、仲間がCOしている場合、占い師COをやめる
					seer = false;
					System.out.println("-------stop CO seer--------");
					lastTalkTurn = -1;
					return chooseTalk();
				}
			}
			
			if (!doCO) { //まだCOしていなければ
				doCO = true;
				bodyguard = false;
				medium = false;
				return (new Content(new ComingoutContentBuilder(me, Role.SEER))).getText(); //COする
			}

			if (!houkoku) { //まだ結果を報告していなければ
				houkoku = true;
				for (int i = 0; i < numAgents; i++) { //すべてのプレイヤーの中で
					if (i != meint) { //自分以外で
						if (sh.gamestate.agents[i].Alive) { //生きていて
							if (divined[i] == 0) { //まだ自分が占っていないなら
								double score = sh2.rp.getProb(i, Util.WEREWOLF); //占い師視点のそのプレイヤーの人狼の可能性を取ってきて
								if (mn < score) { //最大値を記録
									mn = score;
									c = i;
								}
							}
						}
					}
				}
				//占い結果の破綻を防ぐ
				if ((black_count >= 2) || nakama[c]) { //3人に黒を出し終えるか、最も人狼らしいプレイヤーが本当に人狼(仲間)だった場合
					divined[c] = 1;
					sh2.scorematrix.divined(sh2.gamestate, meint, c, true); //占い結果を人間として記録
					return (new Content(new DivinedResultContentBuilder(currentGameInfo.getAgentList().get(c),
							Species.HUMAN))).getText(); //占い結果を人間として報告
				} else {
					divined[c] = -1;
					black_count++;
					sh2.scorematrix.divined(sh2.gamestate, meint, c, false); //占い結果を人狼として記録
					return (new Content(new DivinedResultContentBuilder(currentGameInfo.getAgentList().get(c),
							Species.WEREWOLF))).getText(); //占い結果を人狼として報告
				}
			}
		}
		
		//狩人または霊媒師COとCOしたあとの行動
		else if (bodyguard || medium) {
			if (!doCO) { //まだCOしていないなら
				int mediumCount = 0;
				boolean nakamaBodyguard = false, nakamaMedium = false; //仲間のCOを確認して
				for (int i = 0; i < numAgents; i++) {
					if (nakama[i]) {
						if (agents[i].COrole == Role.BODYGUARD) {
							nakamaBodyguard = true;
						}
						else if (agents[i].COrole == Role.MEDIUM) {
							nakamaMedium = true;
						}
					}
					else if (agents[i].COrole == Role.MEDIUM)
						mediumCount++;
				}
				if ((!nakamaMedium) && (day == 1) && (mediumCount <= 1)) { //初日で、仲間が霊媒師COしておらず、1CO以下　なら霊媒師CO
					medium = true;
					bodyguard = false;
					doCO = true;
					return (new Content(new ComingoutContentBuilder(me, Role.MEDIUM))).getText();
				}
				if (!nakamaBodyguard) { //仲間が狩人COしていないなら狩人CO
					medium = false;
					bodyguard = true;
					doCO = true;
					return (new Content(new ComingoutContentBuilder(me, Role.BODYGUARD))).getText();
				}
				//それもだめなら、村人CO
				medium = false;
				bodyguard = false;
				doCO = true; 
				return (new Content(new ComingoutContentBuilder(me, Role.VILLAGER))).getText();
			}
			if (bodyguard) {
				if((!houkoku) && (day >= 2)) { //報告がまだなら,「占い師CO→霊媒師CO→仲間→狂人の可能性が高いプレイヤー」という優先度で護衛対象を報告
					houkoku = true;
					int[] seers = {-1, -1}, mediums = {-1, -1}; //面倒なので3人以上いても無視します
					int seer_count = 0, medium_count = 0;
					for (int i = 0; i < numAgents; i++) {
						if (i != meint) {
							if (sh.gamestate.agents[i].Alive) {
								if (agents[i].COrole == Role.SEER) {
									seers[seer_count] = i;
									seer_count = 1;
								} else if (agents[i].COrole == Role.MEDIUM) {
									mediums[medium_count] = i;
									medium_count = 1;
								}
							}
						}
					}
					if (seer_count == 1) { //占い師COが生きているならば
						if (seers[1] != -1) { //2人生きているなら、狂人の可能性が高い方を選ぶ
							if (sh.rp.getProb(seers[0], Util.POSSESSED) > sh.rp.getProb(seers[1],  Util.POSSESSED)) {
								c = seers[0];
							}
							else {
								c = seers[1];
							}
						}
						else c = seers[0];
					}
					else if (medium_count == 1) { //占い師COが生きておらず、霊媒師COが生きているならば
						if (mediums[1] != -1) {
							if (sh.rp.getProb(mediums[0], Util.POSSESSED) > sh.rp.getProb(mediums[1], Util.POSSESSED)) {
								c = mediums[0];
							}
							else {
								c = mediums[1];
							}
						}
						else c = mediums[0];
					}
					else { //占い師COも霊媒師COもいないならば
						mn = -1;
						for (int i = 0; i < numAgents; i++) {
							if (i != meint) {
								if (sh.gamestate.agents[i].Alive) {
									if (nakama[i]) { //仲間が生きているなら選択
										c = i;
										break;
									}
									else {
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
					return (new Content(new GuardedAgentContentBuilder(currentGameInfo.getAgentList().get(c)))).getText();
				}
			}
			else { //霊媒師COしていて
				if (!houkoku) { //報告がまだなら
					houkoku = true;
					if (currentGameInfo.getExecutedAgent() != null) {
						Agent target = currentGameInfo.getExecutedAgent();
						c = target.getAgentIdx() - 1;
						if (agents[c].COrole == Role.SEER) { //占い師COしていたプレイヤーは白判定にしておく
							return (new Content(new IdentContentBuilder(target, Species.HUMAN))).getText();
						}
						else if (black_count <= 1) { //占い師CO以外はとりあえず黒を出しておく(2人まで)
							black_count ++;
							return (new Content(new IdentContentBuilder(target, Species.WEREWOLF))).getText();
						}
						return (new Content(new IdentContentBuilder(target, Species.HUMAN))).getText();
					}
				}
			}
		}
		
		if (numAgents == 5) { //5人村で
			if (gamecount >= 50) { //50試合目以降で
				if (day == 1 && sh.gamestate.turn == 1) { //初日の最初のターンなら
					return Talk.SKIP; //skipする
				}
			}

		} else if (numAgents == 15) { //15人村で
			if (gamecount >= 50) { //50試合目以降で
				if (day == 1 && sh.gamestate.turn == 1) { //初日の最初のターンなら
					return Talk.SKIP; //skipする
				}
			}
		}
		if (currentGameInfo.getAliveAgentList().size() <= 3) { //生存者が3人で)
			if (!pos) { //狂人が生きているかどうかをまだ判断していないなら
				pos = true;
				if (numAgents == 5) { //5人村なら
					double all = 0;
					double alive = 0;
					for (int i = 0; i < numAgents; i++) { //すべてのプレイヤーの
						all += sh.rp.getProb(i, Util.POSSESSED); //人狼視点での狂人の可能性をallに合計する
						if (sh.gamestate.agents[i].Alive) { //生きているプレイヤーの狂人の可能性は
							alive += sh.rp.getProb(i, Util.POSSESSED); //aliveにも合計しておく
						}
					}
					if (alive > 0.5 * all) { //生きているプレイヤーの狂人の可能性の合計がすべてのプレイヤーの狂人の可能性の合計の半分以上であれば(5人村で2人死亡した状況のため、死亡した2人と自分以外に生き残っている2人の狂人の可能性を比べていることになる) NOTE:PPに走る基準が緩すぎるかも
						kyoujin_ikiteru = true; //狂人が生きていると判断
						System.out.println("kyojin");
						return (new Content(new ComingoutContentBuilder(me, Role.WEREWOLF))).getText(); //人狼COする
					}
				} else { //15人村なら
					double all = 0;
					double alive = 0;
					for (int i = 0; i < numAgents; i++) { //すべてのプレイヤーの
						all += sh.rp.getProb(i, Util.POSSESSED); //人狼視点での狂人の可能性をallに合計する
						if (sh.gamestate.agents[i].Alive) { //生きているプレイヤーの狂人の可能性は
							alive += sh.rp.getProb(i, Util.POSSESSED); //aliveにも合計しておく
						}
					}
					if (alive > 0.5 * all) { //生きているプレイヤーの狂人の可能性の合計がすべてのプレイヤーの狂人の可能性の合計の半分以上であれば
						kyoujin_ikiteru = true; //狂人が生きていると判断
						return (new Content(new ComingoutContentBuilder(me, Role.WEREWOLF))).getText(); //人狼COする
					}

				}
			} else if (kyoujin_ikiteru) { //狂人が生きているかどうかを判断済みで、かつ狂人が生きていると思われるなら
				if (numAgents == 0) { //TODO:これはミスか？
					System.out.println("kyojin");
					mn = -1;
					for (int i = 0; i < numAgents; i++)
						if (i != meint)
							if (sh.gamestate.agents[i].Alive) {
								double score = 1 - sh.rp.getProb(i, Util.POSSESSED);
								if (mn < score) {
									mn = score;
									c = i;
								}
							}

					voteCandidate = currentGameInfo.getAgentList().get(c);
//					before = c;
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
			}
		}

		if (numAgents == 15) { //15人村で
			if (sh.gamestate.cnt_vote(meint) >= co_count) { //ヘイトが集まっていて
				
				System.out.println("comingout. meint = " + meint + " " + sh.gamestate.cnt_vote(meint) + " players will vote to me"); //debug
				for (int i = 0; i < numAgents; i++) {
					System.out.print(i + " will vote " + sh.gamestate.agents[i].will_vote + " ");
				}
				
				if (!doCO) { //まだCOしていないなら,狩人や霊媒師としてCOする。COの処理は別に書いてある。
					bodyguard = true;
					medium = true;
					lastTalkTurn = -1;
					return chooseTalk();
				}
			}
		}
		
		for (int i = 0; i < numAgents; i++) {
			System.out.print(sh2.rp.getProb(i, Util.WEREWOLF) + " "); //村視点、各プレイヤーの人狼の可能性を出力しておく(発言ではない)
		}
		System.out.println();
		if (numAgents == 5) { //5人村なら
			c = -1;
			mn = -1;
			for (int i = 0; i < numAgents; i++) //すべてのプレイヤーの中で
				if (i != meint) //自分以外で
					if (sh.gamestate.agents[i].Alive) { //生きているなら
						double score = sh2.rp.getProb(i, Util.WEREWOLF); //村視点での人狼の可能性を取ってきて
						if (day != 1 || sh.gamestate.turn > 2) {
							//score += sh.gamestate.cnt_vote(i); //乗っかり戦略
						}
						//double score = sh2.rp.getProb(i, Util.WEREWOLF);
						if (mn < score) { //最大値を記録
							mn = score;
							c = i;
						}
					}
			if (c == -1) { //もし上の条件でscoreが-1以上になるプレイヤーがいないなら(voteと同様) NOTE:乗っかり戦略がコメントアウトされているため、まったく同じ文の繰り返しになっている
				mn = -1;
				for (int i = 0; i < numAgents; i++) {
					if (i != meint) {
						if (sh.gamestate.agents[i].Alive) {
							double score = sh2.rp.getProb(i, Util.WEREWOLF);
							if (mn < score) {
								mn = score;
								c = i;
							}
						}
					}
				}
			}
		} else { //15人村なら
			if (seer || bodyguard || medium) { //占い師、狩人、霊媒師COしている場合、対抗を投票宣言の対象にしておく
				for (int i = 0; i < numAgents; i++) {
					if (i != meint) {
						if (sh.gamestate.agents[i].Alive) {
							if (seer) { //占い師COのとき
								if (agents[i].COrole == Role.SEER) {
									ArrayList<Double> data = new ArrayList<Double>();
									data.add((double)i);
									data.add(sh.rp.getProb(i, Util.SEER)); //真占いの可能性が高いプレイヤーへの投票を宣言する
									int j = 0;
									for (; j < taikou.size(); j++) {
										if (taikou.get(j).get(1) < data.get(1)) break; //降順になるようにしておく
									}
									taikou.add(j, data);
								}
							}
							else if (bodyguard) { //狩人COのとき								
								if (agents[i].COrole == Role.BODYGUARD) {
									ArrayList<Double> data = new ArrayList<Double>();
									data.add((double)i);
									data.add(sh.rp.getProb(i, Util.BODYGUARD)); //真狩人の可能性が高いプレイヤーへの投票を宣言する
									int j = 0;
									for (; j < taikou.size(); j++) {
										if (taikou.get(j).get(1) < data.get(1)) break; //降順になるようにしておく
									}
									taikou.add(j, data);
								}
							}
							else { //霊媒師COのとき
								if (agents[i].COrole == Role.MEDIUM) {
									ArrayList<Double> data = new ArrayList<Double>();
									data.add((double)i);
									data.add(sh.rp.getProb(i, Util.MEDIUM)); //真霊媒の可能性が高いプレイヤーへの投票を宣言する
									int j = 0;
									for (; j < taikou.size(); j++) {
										if (taikou.get(j).get(1) < data.get(1)) break; //降順になるようにしておく
									}
									taikou.add(j, data);
								}
							}
						}
					}
				}
				if (taikou.size() >= 1)
					c = taikou.get(0).get(0).intValue();
					System.out.println("--------taikou---------\n" + taikou);
			}
			if (taikou.size() == 0) {
				for (int i = 0; i < numAgents; i++) { //すべてのプレイヤーの中で
					if (i != meint) { //自分以外で
						if ((sh.gamestate.agents[i].Alive) && (divined[i] != 1)) { //生きていて、占い結果を白として出していないなら
							double score = sh.gamestate.cnt_vote(i); //得票数(投票宣言された数)を取ってきて
							if (mn < score) { //最大値を記録
								mn = score;
								c = i;
							}
						}
					}
				}
				if (mn * 2 < currentGameInfo.getAliveAgentList().size()) { //得票数の最大値が生存者の半数以上でなければ
					mn = -1;
					for (int i = 0; i < numAgents; i++) { //すべてのプレイヤーの中で
						if (i != meint) { //自分以外で
							if (sh.gamestate.agents[i].Alive) { //生きているなら
								double score = sh2.rp.getProb(i, Util.WEREWOLF); //村視点での人狼の可能性を取ってきて
								if (nakama[i]) //本当に人狼(仲間)ならスコアを減らして
									score -= 0.4;
								if (mn < score) { //最大値を記録
									mn = score;
									c = i;
								}
							}
						}
					}
				}
			}

		}

		System.out.println("willvote " + (c + 1) + " " + mn); //投票先として出力する(まだ発言はしていない)
		if (numAgents == 5) { //5人村なら
			if (sh.gamestate.cnt_vote(meint) * 2 >= currentGameInfo.getAliveAgentList().size()) { //自分の得票数が半数以上(吊られそう)ならば
				before = -1; //まずい状況だというフラグを立てておく
			}
			if (sh.gamestate.cnt_vote(c) * 2 < currentGameInfo.getAliveAgentList().size()) { //自分が投票先として宣言したプレイヤーの得票数が半数以下のときも
				before = -1;
			}

		} else { //15人村なら
			if (sh.gamestate.cnt_vote(meint) * 2 >= currentGameInfo.getAliveAgentList().size()) { //自分の得票数が半数以上(吊られそう)ならば
				before = -1;
			}
			if (sh.gamestate.cnt_vote(c) * 2 < currentGameInfo.getAliveAgentList().size()) {
				before = -1;
			}
		}

		if (before != c) { //フラグが立っていたら
			if (!isValidIdx(c)) { //out of rangeだけ気を付けて
				return null;
			}
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
		before = c;
		return Talk.SKIP;
	}

	protected Agent attackVote() {
		sh.process(params, gamedata); //gamedataを最新まで読み込む
		sh.update(); //再度役職推定
		double mn = -1;
		int c = 0;
		int seervote = -1;
		int yesterday = 17;

		ArrayList<ArrayList<Double>> votelist = new ArrayList<ArrayList<Double>>(); //大河内追加[index、score]のリスト※Javaでタプルは使えない
		
		if (numAgents == 5) { //5人村なら
			for (int i = 0; i < numAgents; i++) { //すべてのプレイヤーの中で
				if (i != meint) { //自分以外で
					if (sh.gamestate.agents[i].Alive) { //生きているなら
						double score = 1 - sh.rp.getProb(i, Util.POSSESSED); //狂人の確率を取ってきて
						//double score = sh.rp.getProb(i, Util.SEER);
						if (mn < score) { //最小値を記録(1-getProb(POSSESED)の最大値なので)
							mn = score;
							c = i;
						}
					}
				}
			}
		} else { //15人村なら
			if (whisper_called != -1) {
				return currentGameInfo.getAgentList().get(whisper_called); //whisperしていたなら、そこを襲撃する
			}
			if (alive_bodyguard) {
				for (int i = 0; i < numAgents; i++) { //すべてのプレイヤーの中で
					if (i != meint && !nakama[i]) { //自分と仲間(人狼)以外で
						if (agents[i].COrole == Role.BODYGUARD) { //狩人COしている人がいて
							if (sh.gamestate.agents[i].Alive) { //生きているなら襲撃
								return currentGameInfo.getAgentList().get(i);
								//TODO:狩人COをすぐに襲撃するのではなく,狩人COならば以下で計算しているスコアに加点するなどの処理をした方が対応力が上がる(複数人の狩人COなどにも対応できるようになる)
							}
							else {
								alive_bodyguard = false;
							}
						}
					}
				}
			}
			for (int i = 0; i < numAgents; i++) { //すべてのプレイヤーの中で
				if (i != meint && !nakama[i]) { //自分と仲間(人狼)以外で
					if (sh.gamestate.agents[i].Alive) { //生きているなら
						double score = 1 - sh.rp.getProb(i, Util.POSSESSED); //狂人の確率を取ってきて大小を反転し(scoreが高いほうが狂人の可能性が低い)
						score += 0.2 * sh.rp.getProb(i, Util.SEER); //占い師の確率×0.2を足す
						score += 0.1 * sh.rp.getProb(i, Util.BODYGUARD); //狩人の確率×0.1を足す
						score += 0.1 * sh.rp.getProb(i, Util.MEDIUM); //霊媒師の確率×0.1を足す
						score += 3 * wincnt[i] / (gamecount + 0.01); //勝率×3を足す
						if (agents[i].COrole == Role.SEER) //占い師COしているプレイヤーは
							seervote = i; //記憶しておく(複数いる場合は、インデックスが最も大きいプレイヤーだけを記憶)
						ArrayList<Double> data = new ArrayList<Double>();
						data.add((double)i);
						data.add(score);
						int j = 0;
						for (; j < votelist.size(); j++) {
							if (votelist.get(j).get(1) < score) break; //scoreについて降順になるようにしておく
						}
						votelist.add(j, data);
						//ここまで
					}
				}
			}
			c = votelist.get(0).get(0).intValue();
			System.out.println("votelist: "+votelist);
		}
		if ((yesterday - currentGameInfo.getAliveAgentList().size()) >= 2)
			if (seervote >= 0)
				c = seervote; //FIXME: おそらく、昨日の生存者と今日の生存者の差が2の(つまり、護衛が成功していない)ときには占い師COのプレイヤーを襲撃するということを実装したかったようだが、この書き方だと毎回それを実行することになっている(yesterdayをこの関数内で定義しているため)。また、正しく動くようにしたとして本当にこの戦略が効果的なのかは不明

		yesterday = currentGameInfo.getAliveAgentList().size();
		if (numAgents == 15) {
			if (alive_bodyguard) { //狩人が生きている場合
				int tmp = c; //候補者全員がguardedだった場合、tmpを襲撃する
				for (int i = 0; i < votelist.size(); i++){
					if (guarded[c]) { //護衛されたことがある人の場合、次へ
						c = votelist.get(i).get(0).intValue();
					}
					else break; //護衛されていなければbreak
					if (i == votelist.size()) { //全員護衛されていたら、もう狩人はいない
						c = tmp;
						alive_bodyguard = false;
					}
				}
			}
		}
		
		if (!isValidIdx(c)) {
			return null;
		}
		return currentGameInfo.getAgentList().get(c);
	}
	
	@Override
	public String whisper() {
		if (day > 0) {
			if (!attack_houkoku) { //襲撃先の報告(以下、attackvoteのコピペ
				attack_houkoku = true;
				
				sh.process(params, gamedata); //gamedataを最新まで読み込む
				sh.update(); //再度役職推定
				double mn = -1;
				int c = 0;
				int seervote = -1;
				int yesterday = 17;
				
				ArrayList<ArrayList<Double>> votelist = new ArrayList<ArrayList<Double>>();
				
				if (alive_bodyguard) {
					for (int i = 0; i < numAgents; i++) { //すべてのプレイヤーの中で
						if (i != meint && !nakama[i]) { //自分と仲間(人狼)以外で
							if (agents[i].COrole == Role.BODYGUARD) { //狩人COしている人がいて
								if (sh.gamestate.agents[i].Alive) { //生きているなら襲撃
									whisper_called = i;
									return (new Content(new AttackContentBuilder(currentGameInfo.getAgentList().get(i)))).getText();
								}
								else {
									alive_bodyguard = false;
								}
							}
						}
					}
				}
				
				for (int i = 0; i < numAgents; i++) { //すべてのプレイヤーの中で
					if (i != meint && !nakama[i]) { //自分と仲間(人狼)以外で
						if (sh.gamestate.agents[i].Alive) { //生きているなら
							double score = 1 - sh.rp.getProb(i, Util.POSSESSED); //狂人の確率を取ってきて大小を反転し(scoreが高いほうが狂人の可能性が低い)
							score += 0.2 * sh.rp.getProb(i, Util.SEER); //占い師の確率×0.2を足す
							score += 0.1 * sh.rp.getProb(i, Util.BODYGUARD); //狩人の確率×0.1を足す
							score += 0.1 * sh.rp.getProb(i, Util.MEDIUM); //霊媒師の確率×0.1を足す
							score += 3 * wincnt[i] / (gamecount + 0.01); //勝率×3を足す
							if (agents[i].COrole == Role.SEER) //占い師COしているプレイヤーは
								seervote = i; //記憶しておく(複数いる場合は、インデックスが最も大きいプレイヤーだけを記憶)

							ArrayList<Double> data = new ArrayList<Double>();
							data.add((double)i);
							data.add(score);
							int j = 0;
							for (; j < votelist.size(); j++) {
								if (votelist.get(j).get(1) < score) break; //scoreについて降順になるようにしておく
							}
							votelist.add(j, data);
						}
					}
				}
				System.out.println("votelist: "+votelist);
				c = votelist.get(0).get(0).intValue();
				
				if ((yesterday - currentGameInfo.getAliveAgentList().size()) >= 2)
					if (seervote >= 0)
						c = seervote; //FIXME: おそらく、昨日の生存者と今日の生存者の差が2の(つまり、護衛が成功していない)ときには占い師COのプレイヤーを襲撃するということを実装したかったようだが、この書き方だと毎回それを実行することになっている(yesterdayをこの関数内で定義しているため)。また、正しく動くようにしたとして本当にこの戦略が効果的なのかは不明
				
				yesterday = currentGameInfo.getAliveAgentList().size();
				
				if (alive_bodyguard) { //狩人が生きている場合
					int tmp = c; //候補者全員がguardedだった場合、tmpを襲撃する
					for (int i = 0; i < votelist.size(); i++){
						if (guarded[c]) { //護衛されたことがある人の場合、次へ
							c = votelist.get(i).get(0).intValue();
						}
						else break; //護衛されていなければbreak
						if (i == votelist.size()) { //全員護衛されていたら、もう狩人はいない
							c = tmp;
							alive_bodyguard = false;
						}
					}
				}
				if (!isValidIdx(c)) {
					return Talk.SKIP;
				}
				whisper_called = c;
				return (new Content(new AttackContentBuilder(currentGameInfo.getAgentList().get(c)))).getText();
			}
		}
		else {
			if (!co_whisper) {
				co_whisper = true;
				if (seer) {
					return (new Content(new ComingoutContentBuilder(me, Role.SEER))).getText();
				}
				if (medium) {
					return (new Content(new ComingoutContentBuilder(me, Role.MEDIUM))).getText();
				}
			}
		}
		return Talk.SKIP;
	}
}
