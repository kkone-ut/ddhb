package org.aiwolf.Basket;
import java.util.ArrayList;

public class ScoreMatrix {
	double[][][][] scores;
	Parameters params;
	int seer_number;
	ArrayList<Integer> seer_id;

	//初期化
	void init(int N) {
		int M = 0;
		if (N == 5)
			M = 4;
		else if (N == 15)
			M = 6;
		seer_number = 0;
		seer_id = new ArrayList<Integer>();
		scores = new double[N][M][N][M];
		for (int i1 = 0; i1 < N; i1++) {
			for (int r1 = 0; r1 < M; r1++) {
				for (int i2 = 0; i2 < N; i2++) {
					for (int r2 = 0; r2 < M; r2++) {
						scores[i1][r1][i2][r2] = 0;
					}
				}
			}
		}
	}
	
	//以下の全ては基本的にStateHolderのprocessから呼び出される
		//例外：人狼・狂人が占い師を騙っている時、占い結果についてdivinedが呼び出されている
		//例外：Evaluatorでも使われてるが、StateHolderとコードが類似している。2つの使い分けが分からない
	
	
	// 襲撃された人が人狼である可能性は限りなく0にする
	void killed(GameState gamestate, int a) {
		scores[a][Util.WEREWOLF][a][Util.WEREWOLF] += 30;
	}

	void talk_co(GameState gamestate, int a, int role) {
		if (a < 0 || a >= gamestate.N)
			return;
		if (gamestate.N == 15) {
			//15人村の場合（5人村の場合のCOについてはfirstTurnEndで反映）

			if (role == Util.SEER) {
				//自分が占い師なのに、他の誰かが占い師COしてきた場合で分岐
				if (gamestate.me != a && gamestate.agents[gamestate.me].role == Util.SEER) {
					scores[a][Util.SEER][a][Util.SEER] += 50;
					scores[a][Util.VILLAGER][a][Util.VILLAGER] += 30;
					scores[a][Util.BODYGUARD][a][Util.BODYGUARD] += 10;
					scores[a][Util.MEDIUM][a][Util.MEDIUM] += 10;
				} else {
					//占い師COについて、発言者が村人である可能性、狩人である可能性、霊媒師である可能性をほぼ0にする	
					if (!seer_id.contains(a)) {
						seer_number += 1;
						seer_id.add(a);
						if (seer_number == 1) {
							scores[a][Util.VILLAGER][a][Util.VILLAGER] += 30;
							scores[a][Util.MEDIUM][a][Util.MEDIUM] += 10;
							scores[a][Util.BODYGUARD][a][Util.BODYGUARD] += 10;
							scores[a][Util.POSSESSED][a][Util.POSSESSED] += 5;
							scores[a][Util.WEREWOLF][a][Util.WEREWOLF] += 5;
						} else if (seer_number == 2) {
							scores[seer_id.get(0)][Util.POSSESSED][seer_id.get(0)][Util.POSSESSED] -= 5;
							scores[seer_id.get(0)][Util.WEREWOLF][seer_id.get(0)][Util.WEREWOLF] -= 4.6;
							
							scores[a][Util.MEDIUM][a][Util.MEDIUM] += 10;
							scores[a][Util.BODYGUARD][a][Util.BODYGUARD] += 10;
							scores[a][Util.VILLAGER][a][Util.VILLAGER] += 30;
							scores[a][Util.WEREWOLF][a][Util.WEREWOLF] += 0.4;
						} else if (seer_number == 3) {
							scores[seer_id.get(0)][Util.WEREWOLF][seer_id.get(0)][Util.WEREWOLF] -= 0.4;
							scores[seer_id.get(1)][Util.WEREWOLF][seer_id.get(1)][Util.WEREWOLF] -= 0.4;
							
							scores[a][Util.MEDIUM][a][Util.MEDIUM] += 10;
							scores[a][Util.BODYGUARD][a][Util.BODYGUARD] += 10;
							scores[a][Util.VILLAGER][a][Util.VILLAGER] += 30;
						} else {
							scores[a][Util.MEDIUM][a][Util.MEDIUM] += 10;
							scores[a][Util.BODYGUARD][a][Util.BODYGUARD] += 10;
							scores[a][Util.VILLAGER][a][Util.VILLAGER] += 30;
							for (int id = 0; id < seer_number; id++) {
								scores[seer_id.get(id)][Util.WEREWOLF][seer_id.get(id)][Util.WEREWOLF] -= 0.4;
							}
						}
					}
				}
			} else if (role == Util.MEDIUM) {
				//自分が霊媒師なのに、他の誰かが霊媒師COしてきた場合の分岐
				if (gamestate.me != a && gamestate.agents[gamestate.me].role == Util.MEDIUM) {
					scores[a][Util.MEDIUM][a][Util.MEDIUM] += 50;
					scores[a][Util.VILLAGER][a][Util.VILLAGER] += 30;
					scores[a][Util.BODYGUARD][a][Util.BODYGUARD] += 30;
					scores[a][Util.SEER][a][Util.SEER] += 30;
				} else {
					//霊媒師COについて、発言者が村人、狩人、占い師である可能性をほぼ0にする
					scores[a][Util.VILLAGER][a][Util.VILLAGER] += 30;
					scores[a][Util.BODYGUARD][a][Util.BODYGUARD] += 30;
					scores[a][Util.SEER][a][Util.SEER] += 30;
				}
				
			} else if(role == Util.BODYGUARD) {
				//自分が狩人なのに、他の誰かが狩人COしてきた場合の分岐
				if (gamestate.me != a && gamestate.agents[gamestate.me].role == Util.BODYGUARD) {
					scores[a][Util.VILLAGER][a][Util.VILLAGER] += 5;
					scores[a][Util.SEER][a][Util.SEER] += 10;
					scores[a][Util.MEDIUM][a][Util.MEDIUM] += 10;
					scores[a][Util.BODYGUARD][a][Util.BODYGUARD] += 50;
				} else {
					scores[a][Util.VILLAGER][a][Util.VILLAGER] += 5;
					scores[a][Util.SEER][a][Util.SEER] += 10;
					scores[a][Util.MEDIUM][a][Util.MEDIUM] += 10;
					scores[a][Util.BODYGUARD][a][Util.BODYGUARD] -= 0.01;
				}
			}
		}
	}

	void firstTurnEnd(GameState gamestate) {
		if (gamestate.N == 5) {
			//5人村の場合
			for (int i = 0; i < gamestate.N; i++) {
				if (gamestate.agents[i].corole == -1) {
					//占い師COを行っていない人が占い師である可能性を10下げる
					scores[i][Util.SEER][i][Util.SEER] += 10;
					//人狼と狂人については、1-COレートに対応した数値だけ可能性を下げる
					scores[i][Util.WEREWOLF][i][Util.WEREWOLF] += Util.nlog(1 - params.werewolf_co_rate.value);
					scores[i][Util.POSSESSED][i][Util.POSSESSED] += Util.nlog(1 - params.possessed_co_rate.value);

				} else if (gamestate.agents[i].corole == Util.SEER) {
					//自分が占い師なのに、他の誰かが占い師COしてきた場合の処理
					if (gamestate.me != i && gamestate.agents[gamestate.me].role == Util.SEER) {
						scores[i][Util.SEER][i][Util.SEER] += 50;
					}					
					//占い師COを行っている人について、村人である可能性を大きく下げる
					scores[i][Util.VILLAGER][i][Util.VILLAGER] += 10;
					//人狼と狂人については、COレートに対応した数値だけ可能性を下げる
					scores[i][Util.WEREWOLF][i][Util.WEREWOLF] += Util.nlog(params.werewolf_co_rate.value);
					scores[i][Util.POSSESSED][i][Util.POSSESSED] += Util.nlog(params.possessed_co_rate.value);
				}
			}
		}
	}

	//自分が占い師の場合の占い結果の反映
	//他人が占い師の場合は talk_divined が呼ばれる。
	void divined(GameState gamestate, int a, int b, boolean white) {
		if (b < 0 || b >= gamestate.N)
			return;
		if (white) {
			scores[b][Util.WEREWOLF][b][Util.WEREWOLF] += 50;
		} else {
			scores[b][Util.WEREWOLF][b][Util.WEREWOLF] -= 50;
		}
	}

	//自分が霊媒師の場合の霊媒結果の反映
	//他人からの霊媒結果の報告は反映していない（talk_identが存在していないため）
	void ident(GameState gamestate, int a, int b, boolean white) {
		if (b < 0 || b >= gamestate.N)
			return;
		if (gamestate.N == 15) {
			if (white) {
				scores[a][Util.MEDIUM][b][Util.WEREWOLF] += 50;
			} else {
				scores[a][Util.MEDIUM][b][Util.WEREWOLF] -= 50;
			}
		}
	}
	//他人の占い結果の報告を反映
	void talk_divined(GameState gamestate, int a, int b, boolean white) {
		if (b < 0 || b >= gamestate.N)
			return;
		if (gamestate.agents[a].target == -1) {
			gamestate.agents[a].target = b;
		} else {
			return;
		}
		//占い結果が白の場合
		if (white) {

			//5人村の場合
			if (gamestate.N == 5) {
				//自分が人狼かつ白出しされた対象の場合と、そうでない場合で分ける
				if (gamestate.me == b && gamestate.agents[gamestate.me].role == Util.WEREWOLF) {
					scores[a][Util.SEER][a][Util.SEER] += 10;
				} else {
					//報告者が占い師で報告対象が人狼である可能性を減らす
					scores[a][Util.SEER][b][Util.WEREWOLF] += 10;
					//報告者が狂人・人狼である可能性を0.69減らす。0.69 = -ln(1-0.5)
					scores[a][Util.POSSESSED][a][Util.POSSESSED] += Util.nlog(1 - params.possessed_divined_black.value);
					scores[a][Util.WEREWOLF][a][Util.WEREWOLF] += Util.nlog(1 - params.werewolf_divined_black.value);
				}
			} else {
				//15人村の場合
				//自分が人狼かつ白出しされた対象の場合と、そうでない場合で分ける
				if (gamestate.me == b && gamestate.agents[gamestate.me].role == Util.WEREWOLF) {
					scores[a][Util.SEER][a][Util.SEER] += 10;
				} else {
					//報告者が占い師で報告対象が人狼である可能性を減らす
					scores[a][Util.SEER][b][Util.WEREWOLF] += 30;
					if (gamestate.agents[gamestate.me].role == Util.WEREWOLF || gamestate.agents[gamestate.me].role == Util.POSSESSED || gamestate.agents[gamestate.me].role == Util.MEDIUM) {
						scores[a][Util.WEREWOLF][b][Util.WEREWOLF] -= 2;
					} else {
						scores[a][Util.WEREWOLF][b][Util.WEREWOLF] -= 2;
					}
				}
			}
		} else {	//占い結果が黒の場合

			//5人村の場合
			if (gamestate.N == 5) {
				//自分が人狼ではなくかつ黒出しされた対象の場合と、そうでない場合で分ける
				if (gamestate.me == b && gamestate.agents[gamestate.me].role != Util.WEREWOLF) {
					scores[a][Util.SEER][a][Util.SEER] += 0.1;
				} else {
					//報告者が狂人・人狼である可能性を0.69減らす。0.69 = -ln(1-0.5)
					scores[a][Util.POSSESSED][a][Util.POSSESSED] += Util.nlog(params.possessed_divined_black.value);
					scores[a][Util.WEREWOLF][a][Util.WEREWOLF] += Util.nlog(params.werewolf_divined_black.value);

					//報告者が占い師で報告対象が村人陣営である可能性を1.0減らす
					scores[a][Util.SEER][b][Util.VILLAGER] += 0.8;
					scores[a][Util.SEER][b][Util.POSSESSED] += 1.0;
				}
			} else {
				//15人村の場合
				if (gamestate.me == b && gamestate.agents[gamestate.me].role != Util.WEREWOLF) {
					scores[a][Util.SEER][a][Util.SEER] += 10;
				} else {
					//報告者が占い師で報告対象が村人、狂人、狩人、霊媒師である可能性を減らす
					scores[a][Util.SEER][b][Util.VILLAGER] += 0.2;
					scores[a][Util.SEER][b][Util.POSSESSED] += 0.5;
					scores[a][Util.SEER][b][Util.BODYGUARD] += 0.5;
					scores[a][Util.SEER][b][Util.MEDIUM] += 0.5;
					//両者が人狼である可能性を減らす
					scores[a][Util.WEREWOLF][b][Util.WEREWOLF] += 5;
				}
			}
		}
	}

	//投票行動について反映
	void vote(GameState gamestate, int a, int b) {
		if (b < 0 || b >= gamestate.N)
			return;
		gamestate.agents[a].votefor = b;
		if (gamestate.N == 5) {
			//5人村の場合
			if (gamestate.me != a) {
				//自分の役職によって村人への同調度合いを変化
				if (gamestate.agents[gamestate.me].role == Util.VILLAGER) {
					scores[a][Util.VILLAGER][b][Util.WEREWOLF] += 0.1;
				} else if (gamestate.agents[gamestate.me].role == Util.WEREWOLF) {
					scores[a][Util.VILLAGER][b][Util.WEREWOLF] -= 0;
				} else {
					scores[a][Util.VILLAGER][b][Util.WEREWOLF] -= 0.1;
				}
				//投票者が占い師で投票対象が人狼である可能性を0.3高める
				scores[a][Util.SEER][b][Util.WEREWOLF] -= 0.3;
			}
		} else {
			//15人村の場合
			if (gamestate.me != a) {
				for (int i = 0; i < 2; i++) {
					//投票者が村人陣営である場合で投票対象が人狼陣営である可能性を0.01高める
					//scores[a][Util.VILLAGER][b][Util.nothumans[i]] -= 0.01;
					scores[a][Util.SEER][b][Util.nothumans[i]] -= 0.02;
					scores[a][Util.BODYGUARD][b][Util.nothumans[i]] -= 0.01;
					scores[a][Util.MEDIUM][b][Util.nothumans[i]] -= 0.01;
				}
				//投票者が人狼で投票対象が人狼である可能性を0.05低める（ラインを見ている）
				scores[a][Util.WEREWOLF][b][Util.WEREWOLF] += 0.05;
			}
		}
	}

	//投票意思について反映
	void will_vote(GameState gamestate, int a, int b) {
		if (b < 0 || b >= gamestate.N)
			return;
		if (gamestate.N == 5) {
			//5人村の場合
			if (gamestate.agents[a].will_vote == b)
				return;
			if (gamestate.day != 1 || gamestate.turn != 1) {
				//1日目でなくて1ターン目でもない場合
				if (gamestate.me != a) {
					//発言者が村人・占い師で対象が人狼である可能性をその人の信頼度に比例して上げる
					scores[a][Util.VILLAGER][b][Util.WEREWOLF] -= params.trust.value * 0.1;
					scores[a][Util.SEER][b][Util.WEREWOLF] -= params.trust.value;
					//発言者が人狼である可能性を、人狼のtabenパラメータに比例して上げる
					scores[a][Util.WEREWOLF][a][Util.WEREWOLF] -= params.werewolf_taben.value;
				}
				gamestate.agents[a].will_vote = b;
			}
		} else {
			//15人村の場合
			if (gamestate.agents[a].will_vote == b)
				return;
			gamestate.agents[a].will_vote = b;
			if (gamestate.me != a) {
				// 発言者の役職ごとに対象の人狼の可能性を上下させる
				//scores[a][Util.VILLAGER][b][Util.WEREWOLF] -= 0.01;
				scores[a][Util.BODYGUARD][b][Util.WEREWOLF] -= 0.01;
				scores[a][Util.MEDIUM][b][Util.WEREWOLF] -= 0.01;
				scores[a][Util.SEER][b][Util.WEREWOLF] -= 0.02;
				//人狼間で投票意思を見せることは少なからずあり
				//交互に宣言されると数値が積み重なって意図的にライン切りをされてしまうため
				//その脆弱性を取り除く目的で、人狼間のラインを切る計算は削除

				//発言者が人狼である可能性を、人狼の発言頻度が高いほど上げる
				scores[a][Util.WEREWOLF][a][Util.WEREWOLF] -= params.werewolf_taben.value;
			}
		}
	}
	
}
