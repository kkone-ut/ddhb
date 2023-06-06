package org.aiwolf.Basket;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class RolePrediction {
	boolean[] isfixed = new boolean[15];
	List<Assignment> assignments;
	final int MAX = 100;
	ScoreMatrix scorematrix;
	boolean updated = false;
	double[][] prob;
	int N, M;
	ArrayList<Integer> fixed_list;
	ArrayList<Integer> not_fixed;
	HashSet<Long> hash = new HashSet<Long>();
	int role;
	Random rnd = new Random();

	RolePrediction() {
	}

	RolePrediction(int _N, List<Integer> fixed, int _role) {
		N = _N;
		fixed_list = new ArrayList<Integer>(fixed);
		not_fixed = new ArrayList<Integer>();
		for (int i = 0; i < N; i++) {
			if (!fixed_list.contains(i)) {
				not_fixed.add(i);
			}
		}
		/*if(false){

		System.out.println("not_fixed");
		for(int i : not_fixed){
			System.out.print(i + " ");
		}
		System.out.println();
		}
		*/
		role = _role;
		assignments = new ArrayList<Assignment>();
		//assignments.add(new Assignment(N, fixed_list, role));
		M = 0;
		if (N == 5)
			M = 4;
		if (N == 15)
			M = 6;
		prob = new double[N][M];
	}

	//生きている人狼が0人になっているような役職割当（assignment）を役職割当のリスト（assignments）から削除
	//その後、役職割当ごとにscoreをアップデート
	//最後に役職割当のリストをscoreの小さい順にソート
	void recalc(ScoreMatrix scorematrix, GameState gamestate) {
		updated = false;
		for (int t = assignments.size() - 1; t >= 0; t--) {
			Assignment as = assignments.get(t);
			int x = 0;
			for (int i = 0; i < N; i++) {
				if (gamestate.agents[i].Alive && as.assignment.get(i) == Util.WEREWOLF) {
					x = 1;
				}
			}
			if (x == 0) {
				assignments.remove(t);
			}
			as.calcScore(scorematrix);
		}
		assignments.sort((a, b) -> Double.compare(a.score, b.score));
	}

	//たとえばStateHolderではtimesは50に設定されてる
	//ランダムに新たな役職割当を生成して探索を行う
	void search(ScoreMatrix scorematrix, GameState gamestate, int times) {
		updated = false;
		OUTER: for (int t = 0; t < times; t++) {
			Assignment as = new Assignment();
			//役職割当のリストが空 or 20%の確率で役職割当を新たに生成
			if (assignments.size() == 0 || rnd.nextDouble() < 0.2) {
				as = new Assignment(N, fixed_list, role);

				//10回入れ替えることでランダム化
				for (int loop = 0; loop < 10; loop++)
					as.randomswap(not_fixed);

			} else {
				//残り80%の確率で、現在の役職割当のリストの中からランダムに1つ選んでそれのコピーを生成
				as.copyfrom(assignments.get(rnd.nextInt(assignments.size())));

				//数回入れ替えることでランダム化
				for (int loop = 0; loop < rnd.nextInt(3) + 1; loop++)
					as.randomswap(not_fixed);

			}
			//新たに生成した役職割当のscoreを計算
			as.calcScore(scorematrix);
			//System.out.print(as.score + " ");
			//for(int i=0;i<N;i++){
			//	System.out.print(as.assignment.get(i) + " ");
			//}
			//System.out.println();

			//既にその役職割当が役職割当のリストに入ってる場合は特に何もせずに終了
			if (hash.contains(as.getHash())) {
				continue OUTER;
			}
			//その役職割当にのっとった場合に既に人狼が全員死んでいるような状況なら、その役職割当は無効と見做し、特に何もせずに終了
			int x = 0;
			for (int i = 0; i < N; i++) {
				if (gamestate.agents[i].Alive && as.assignment.get(i) == Util.WEREWOLF) {
					x = 1;
				}
			}
			if (x == 0) {
				continue OUTER;
			}

			//以上の2条件（新規性がある＆無効ではない）をクリアした場合、役職割当を役職割当のリストに加える
			assignments.add(as);
			//その役職割当がリストに存在することを記録する
			hash.add(as.getHash());
			//scoreに従って役職割当のリストをソート
			assignments.sort((a, b) -> Double.compare(a.score, b.score));
			//役職割当のリストが最大数（100）を上回っていた場合、最もありえない（＝scoreの値が大きい）役職割当をリストから削除
			while (assignments.size() > MAX) {
				hash.remove(assignments.get(assignments.size() - 1).getHash());
				assignments.remove(assignments.size() - 1);
			}
		}

	}

	double getProb(int agent, int role) {
		//あるエージェントがある役職である確率を推定して返す
		//評価の元となるスコアを（多分ScoreMatrixから）持ってきて、0が最大値になるように調整する
		//調整後のスコアを指数関数に入れて、評価値を計算
		//最後に各役職の和が1になるように評価値を正規化する
		if (!updated) {
			for (int i = 0; i < N; i++) {
				for (int j = 0; j < M; j++) {
					prob[i][j] = 0;
				}
			}
			double[] ascore = new double[assignments.size()];
			double mn = 1e9;
			if (assignments.size() > 0) {
				mn = assignments.get(0).score;
			}
			for (int i = 0; i < assignments.size(); i++) {
				mn = Math.max(mn, assignments.get(i).score);
			}

			for (int i = 0; i < assignments.size(); i++) {
				ascore[i] = assignments.get(i).score - mn;
			}
			for (int i = 0; i < assignments.size(); i++) {
				for (int j = 0; j < N; j++) {
					prob[j][assignments.get(i).assignment.get(j)] += Math.exp(-ascore[i]);
				}
			}
			for (int i = 0; i < N; i++) {
				double sum = 0;
				for (int j = 0; j < M; j++) {
					sum += prob[i][j];
				}
				if (sum > 0) {
					for (int j = 0; j < M; j++) {
						prob[i][j] /= sum;
					}
				}
			}
			updated = true;
		}
		return prob[agent][role];
	}

	double probHuman(int a) {
		double res = 0;
		if (N == 5) {
			res += getProb(a, Util.VILLAGER);
			res += getProb(a, Util.SEER);
		} else {
			res += getProb(a, Util.VILLAGER);
			res += getProb(a, Util.SEER);
			res += getProb(a, Util.BODYGUARD);
			res += getProb(a, Util.MEDIUM);
		}

		return res;
	}

	int predict(int role) {
		int res = -1;
		double mn = -1;
		getProb(0, 0);
		for (int i = 0; i < N; i++) {
			if (mn < prob[i][role]) {
				mn = prob[i][role];
				res = i;
			}
		}
		return res;
	}
}
