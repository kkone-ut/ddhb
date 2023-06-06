package org.aiwolf.daisyo;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//
public class Assignment {

	Random rnd = new Random();

	List<Integer> assignment = new ArrayList<Integer>();
	int numAgents;
	double score = 0;

	Assignment(){
		;
	}

	// fixedList :
	Assignment(int _numAgents, ArrayList<Integer> _assignment) {
		numAgents = _numAgents;
		assignment = _assignment;
	}

	Assignment(int r1, int r2, int r3, int r4, int r5) {
		numAgents = 5;
		assignment.add(r1);
		assignment.add(r2);
		assignment.add(r3);
		assignment.add(r4);
		assignment.add(r5);
	}

	Assignment(int r1, int r2, int r3, int r4, int r5, int r6, int r7, int r8, int r9, int r10, int r11, int r12, int r13, int r14, int r15) {
		numAgents = 15;
		assignment.add(r1);
		assignment.add(r2);
		assignment.add(r3);
		assignment.add(r4);
		assignment.add(r5);
		assignment.add(r6);
		assignment.add(r7);
		assignment.add(r8);
		assignment.add(r9);
		assignment.add(r10);
		assignment.add(r11);
		assignment.add(r12);
		assignment.add(r13);
		assignment.add(r14);
		assignment.add(r15);
	}

	// ランダムに作成する
	Assignment(int _numAgents, ArrayList<Integer> fixedList, int role){
		numAgents = _numAgents;

		boolean[] isFixed = new boolean[numAgents];
		for(int i = 0; i < numAgents; i++) {
			isFixed[i] = false;
		}
		for (int i : fixedList) {
			isFixed[i] = true;
		}

		assignment.clear();
		if(numAgents == 5){

			assignment.add(Util.VILLAGER);
			assignment.add(Util.VILLAGER);
			assignment.add(Util.SEER);
			assignment.add(Util.WEREWOLF);
			assignment.add(Util.POSSESSED);

		}else{

			for (int i = 0; i < 8; ++i) {
				assignment.add(Util.VILLAGER);
			}
			assignment.add(Util.SEER);
			assignment.add(Util.MEDIUM);
			assignment.add(Util.BODYGUARD);
			for (int i = 0; i < 3; ++i) {
				assignment.add(Util.WEREWOLF);
			}
			assignment.add(Util.POSSESSED);
		}

		List<Integer> extra = new ArrayList<Integer>();
		for(int i : fixedList) {
			if(assignment.get(i) != role){
				extra.add(assignment.get(i));
				assignment.set(i, role);
			}
		}

		{
			int cur = 0;
			for (int i = 0; i < numAgents; ++i) {
				if(cur >= extra.size())break;
				if(!isFixed[i] && assignment.get(i) == role){

					assignment.set(i, extra.get(cur));
					cur++;

				}
			}
		}
	}

	Assignment returncopy(){
		Assignment res = new Assignment();
		res.copyfrom(this);
		return res;
	}

	void copyfrom(Assignment c) {
		assignment.clear();
		for (int i = 0; i < c.assignment.size(); i++) {
			assignment.add(c.assignment.get(i));
		}
		numAgents = c.numAgents;
	}

	void randomSwap(List<Integer> notfixed) {
		int i = 0;
		int j = 0;
		while(true){
			i = notfixed.get(rnd.nextInt(notfixed.size()));
			j = notfixed.get(rnd.nextInt(notfixed.size()));
			if(assignment.get(i)!= assignment.get(j)) break;
		}
		int t = assignment.get(i);
		assignment.set(i, assignment.get(j));
		assignment.set(j, t);
	}

	void calcScore(ScoreMatrix scorematrix){
		score = 0;
		for (int idx1 = 0; idx1 < numAgents; ++idx1) {
			for (int idx2 = 0; idx2 < numAgents; ++idx2) {
				score += scorematrix.scores[idx1][assignment.get(idx1)][idx2][assignment.get(idx2)];
			}
		}
	}

	long getHash(){
		long res = 0;
		for(int t : assignment){
			res = res * 8 + t;
		}
		return res;
	}

	@Override
	public String toString() {
		String res = "";
		int idx = 0;
		for (int v : assignment) {
			if (idx < numAgents) 			res += ",";
			if (v == Util.VILLAGER) 		res += Integer.toString(idx) +  ":VILLAGER";
			else if (v == Util.SEER) 		res += Integer.toString(idx) + ":SEER";
			else if (v == Util.MEDIUM) 		res += Integer.toString(idx) + ":MEDIUM";
			else if (v == Util.BODYGUARD) 	res += Integer.toString(idx) + ":BODYGUARD";
			else if (v == Util.POSSESSED) 	res += Integer.toString(idx) + ":POSSESSED";
			else if (v == Util.WEREWOLF) 	res += Integer.toString(idx) + ":WEREWOLF";
			else res += "ERR";
			idx++;
		}
		return res;
	}
}

