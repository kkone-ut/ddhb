package org.aiwolf.daisyo;

import java.util.HashMap;
public class Util {
	static HashMap<String, Integer> role_string_to_int = new HashMap<String, Integer>();
	static String[] role_int_to_string = new String[6];
	static int[] roles05 = new int[4];
	static int[] roles15 = new int[6];
	static final int WEREWOLF = 0;
	static final int VILLAGER = 1;
	static final int SEER = 2;
	static final int POSSESSED = 3;
	static final int MEDIUM = 4;
	static final int BODYGUARD = 5;
	static int[] humans05 = new int[2];
	static int[] humans15 = new int[4];
	static int[] nothumans = new int[2];



	// 数 (max + 1)
	static final int ACT_NUM = 20;

	static final int ACT_NONE = -1;
	static final int ACT_INIT = 0;
	static final int ACT_CO_VILLAGER = 1;
	static final int ACT_CO_SEER = 2;
	static final int ACT_CO_MEDIUM = 3;
	static final int ACT_DIVINED_WHITE = 4;
	static final int ACT_DIVINED_BLACK = 5;
	static final int ACT_VOTE = 7;
	static final int ACT_ESTIMATE = 8;
	static final int ACT_SKIP = 9;
	static final int ACT_OVER = 10;

	static final int ACT_CO_POSSESSED = 11;
	static final int ACT_CO_WEREWOLF = 12;
	static final int ACT_CO_BODYGUARD = 13;

	static final int ACT_IDENT = 6;
	static final int ACT_IDENT_BLACK = -1; // NONE
	static final int ACT_IDENT_WHITE = 14;

	static final int ACT_ESTIMATE_VILLAGER = 15;
	static final int ACT_ESTIMATE_WEREWOLF = 16;

	static final int ACT_BECAUSE = 17;
	static final int ACT_DAY = 18;
	static final int ACT_OPERATOR = 19;


	Util(){

		humans05[0] = VILLAGER;
		humans05[1] = SEER;

		humans15[0] = VILLAGER;
		humans15[1] = SEER;
		humans15[2] = BODYGUARD;
		humans15[3] = MEDIUM;

		nothumans[0] = WEREWOLF;
		nothumans[1] = POSSESSED;

		role_int_to_string[0] = "WEREWOLF";
		role_int_to_string[1] = "VILLAGER";
		role_int_to_string[2] = "SEER";
		role_int_to_string[3] = "POSSESSED";
		role_int_to_string[4] = "MEDIUM";
		role_int_to_string[5] = "BODYGUARD";
		for(int i=0;i<6;i++){
			role_string_to_int.put(role_int_to_string[i], i);
		}
		roles05[0] = 0;
		roles05[1] = 1;
		roles05[2] = 2;
		roles05[3] = 3;

	}
	static String toString(int a, int d){
		String res = "";
		for(int i=0;i<d;i++){
			res += (a%10);
			a/=10;
		}
		res = new StringBuilder(res).reverse().toString();
		return res;
	}
	static double nlog(double a){
		return -Math.log(Math.max(1e-4, a));
	}
}

