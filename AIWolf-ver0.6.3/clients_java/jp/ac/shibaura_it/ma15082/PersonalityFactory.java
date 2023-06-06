package jp.ac.shibaura_it.ma15082;

import java.util.ArrayList;
import java.util.List;

import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Team;

public enum PersonalityFactory {
	VILLAGER(Role.VILLAGER), SEER(Role.SEER), MEDIUM(Role.MEDIUM), BODYGUARD(Role.BODYGUARD), WEREWOLF(Role.WEREWOLF),
	POSSESSED(Role.POSSESSED), NONE(null);

	private Pair<Double, Double> oe;// 好奇心
	private Pair<Double, Double> co;// 勤勉性
	private Pair<Double, Double> ex;// 外向性
	private Pair<Double, Double> ag;// 協調性
	private Pair<Double, Double> ne;// 情緒不安定性

	private final Role role;

	private PersonalityFactory(Role r) {
		role = r;
		init();
	}

	public void init() {
		oe = null;
		co = null;
		ex = null;
		ag = null;
		ne = null;
	}

	public static void initAll() {
		for (PersonalityFactory v : values()) {
			v.init();
		}
	}

	private static PersonalityFactory get(Role role) {
		for (PersonalityFactory p : values()) {
			if (p.role == role) {
				return p;
			}
		}
		return NONE;
	}

	public static List<Role> getRoleList(Team team) {
		List<Role> ret = new ArrayList<Role>();
		for (Role r : Role.values()) {
			if (r.getTeam() == team) {
				ret.add(r);
			}
		}

		return ret;
	}

	private static Pair<Double, Double> toPair(Double d) {
		if (d == null) {
			return null;
		}
		return new Pair<Double, Double>(d, d);
	}

	public static void setPersonalityRange(Role role, Pair<Double, Double> oe, Pair<Double, Double> co,
			Pair<Double, Double> ex, Pair<Double, Double> ag, Pair<Double, Double> ne) {
		PersonalityFactory temp = get(role);
		temp.oe = oe;
		temp.co = co;
		temp.ex = ex;
		temp.ag = ag;
		temp.ne = ne;
	}

	public static void setPersonality(Role role, Double oe, Double co, Double ex, Double ag, Double ne) {
		Pair<Double, Double> e1 = toPair(oe);
		Pair<Double, Double> e2 = toPair(co);
		Pair<Double, Double> e3 = toPair(ex);
		Pair<Double, Double> e4 = toPair(ag);
		Pair<Double, Double> e5 = toPair(ne);
		setPersonalityRange(role, e1, e2, e3, e4, e5);
	}

	public static void setPersonality(Team team, Double oe, Double co, Double ex, Double ag, Double ne) {
		for (Role r : getRoleList(team)) {
			setPersonality(r, oe, co, ex, ag, ne);
		}
	}

	public static void setPersonalityRange(Team team, Pair<Double, Double> oe, Pair<Double, Double> co,
			Pair<Double, Double> ex, Pair<Double, Double> ag, Pair<Double, Double> ne) {
		for (Role r : getRoleList(team)) {
			setPersonalityRange(r, oe, co, ex, ag, ne);
		}
	}

	public static Personality getPersonality(Role role) {
		PersonalityFactory temp = get(role);
		double oe = Tools.random(temp.oe);
		double co = Tools.random(temp.co);
		double ex = Tools.random(temp.ex);
		double ag = Tools.random(temp.ag);
		double ne = Tools.random(temp.ne);
		return new Personality(oe, co, ex, ag, ne);
	}

	public static Personality getRandomPersonality() {
		return new Personality(Tools.random(), Tools.random(), Tools.random(), Tools.random(), Tools.random());
	}

	// 相関係数
	public static Personality getLearnedPersonality(Role role, ListMap<Role, ListMap<Personality, Team>> datamap) {
		List<Role> rolelist = getRoleList(role.getTeam());
		double sum = 0;
		double[] sum_x = new double[5];
		double[] sum_x2 = new double[5];
		double sum_y2 = 0;
		double sum_y = 0;
		double[] sum_xy = new double[5];

		for (int i = 0; i < 5; i++) {
			sum_x[i] = 0;
			sum_x2[i] = 0;
			sum_xy[i] = 0;
		}
		for (Role r : rolelist) {
			ListMap<Personality, Team> map = datamap.get(r);
			double w = (role == r) ? 1.0 : 0.5;
			for (int i = 0; i < map.size(); i++) {
				sum += w;
				double y = map.getValue(i) == role.getTeam() ? 1.0 : 0.0;
				sum_y += w * y;
				sum_y2 += w * y * y;
				for (int j = 0; j < 5; j++) {
					double x = map.getKey(i).getParam(j);
					sum_x[j] += w * x;
					sum_x2[j] += w * x * x;
					sum_xy[j] += w * x * y;
				}
			}
		}
		if (sum <= 0.0) {
			return getPersonality(role);
		}

		double[] avr_x = new double[5];
		for (int j = 0; j < 5; j++) {
			avr_x[j] = sum_x[j] / sum;
		}
		double avr_y = sum_y / sum;

		double[] r = new double[5];
		for (int i = 0; i < 5; i++) {
			double b = Math.sqrt((sum_x2[i] - sum * avr_x[i] * avr_x[i]) * (sum_y2 - sum * avr_y * avr_y));
			if (b <= 1.0E-15) {
				return getPersonality(role);
			}
			double a = (sum_xy[i] - avr_x[i] * sum_y - avr_y * sum_x[i] + sum * avr_x[i] * avr_y);
			r[i] = a / b;
			// System.out.print(r[i]+" ");
		} // System.out.println();

		// System.out.println(sum*max+" "+sum+" ");
		return calcLearnedPersonality(role, sum, r);
	}

	// NOTE:
	// 回数が増えると0付近に収束する
	private static Personality calcLearnedPersonality(Role role, double point, double[] r) {
		double max = Math.abs(r[0]);
		for (int i = 1; i < r.length; i++) {
			if (max < Math.abs(r[i])) {
				max = Math.abs(r[i]);
			}
		}
		PersonalityFactory temp = get(role);
		double oe = calcParam(temp.oe, r[0] / max, point * max);
		double co = calcParam(temp.co, r[1] / max, point * max);
		double ex = calcParam(temp.ex, r[2] / max, point * max);
		double ag = calcParam(temp.ag, r[3] / max, point * max);
		double ne = calcParam(temp.ne, r[4] / max, point * max);

		return new Personality(oe, co, ex, ag, ne);
	}

	public static double calcParam(Pair<Double, Double> range, double x, double point) {
		double min = 0;
		double max = 1;
		double a = -100;
		while (a <= -1 || 1 <= a) {
			a = x + Tools.random(-1.0 / point, 1.0 / point);
		}
		if (range != null) {
			min = range.getKey();
			max = range.getValue();
		}
		return 0.5 * ((1 + a) * max + (1 - a) * min);
	}

	// 最小二乗法
	public static Personality getLearnedPersonality2(Role role, ListMap<Role, ListMap<Personality, Team>> datamap) {

		try {
			int count = 0;
			List<Role> bases = getRoleList(role.getTeam());
			double[][] A = new double[5][5];
			double[] B = new double[5];
			for (int i = 0; i < 5; i++) {
				B[i] = 0;
				for (int j = 0; j < 5; j++) {
					A[i][j] = 0;
				}
			}
			for (Role r : bases) {
				double w = (r == role ? 1.0 : 0.0);
				ListMap<Personality, Team> map = datamap.get(r);
				for (int k = 0; k < map.size(); k++) {
					count++;
					Personality p = map.getKey(k);
					int vwin = map.getValue(k).equals(role.getTeam()) ? 1 : -1;
					for (int j = 0; j < 5; j++) {
						for (int i = 0; i < 5; i++) {
							A[i][j] += p.getParam(i) * p.getParam(j) * w;
						}
						B[j] += p.getParam(j) * vwin * w;
					}
				}
			}

			double temp;
			for (int k = 0; k < 5; k++) {
				int index = k;
				double pivot = Math.abs(A[k][k]);
				for (int j = k + 1; j < 5; j++) {
					temp = Math.abs(A[k][j]);
					if (pivot < temp) {
						pivot = temp;
						index = j;
					}
				}

				if (pivot < 10E-15) {
					throw new Exception();
				}

				pivot = A[k][index];
				for (int i = k; i < 5; i++) {
					temp = A[i][k];
					A[i][k] = A[i][index];
					A[i][index] = temp;
					A[i][k] /= pivot;
				}
				temp = B[k];
				B[k] = B[index];
				B[index] = temp;
				B[k] /= pivot;

				for (int j = 0; j < 5; j++) {
					if (k == j) {
						continue;
					}
					double p = A[k][j];
					for (int i = 0; i < 5; i++) {
						A[i][j] -= p * A[i][k];
					}
					B[j] -= p * B[k];
				}

			}

			double max = Math.abs(B[0]);
			for (int i = 1; i < B.length; i++) {
				if (max < Math.abs(B[i])) {
					max = Math.abs(B[i]);
				}

			}
			/*
			 * for(int i=0;i<B.length;i++){ System.out.print(B[i]+" "); }
			 * System.out.println();
			 */
			return calcLearnedPersonality2(role, 5, count, B[0], B[1], B[2], B[3], B[4]);
		} catch (Exception e) {
			return getPersonality(role);
		}
	}

	private static Personality calcLearnedPersonality2(Role role, double a, double point, double oe, double co,
			double ex, double ag, double ne) {
		PersonalityFactory temp = get(role);
		double e1 = getParam(a, oe, temp.oe, point);
		double e2 = getParam(a, co, temp.co, point);
		double e3 = getParam(a, ex, temp.ex, point);
		double e4 = getParam(a, ag, temp.ag, point);
		double e5 = getParam(a, ne, temp.ne, point);
		return new Personality(e1, e2, e3, e4, e5);
	}

	private static double getParam(double a, double x, Pair<Double, Double> r, double num) {
		double e = 1.0 / num;
		double ret = sigmoid(a, (x + Tools.random(-e, e)) * Tools.random(0.9, 1.1));
		if (r == null) {
			return ret;
		}
		return ret * (r.getValue() - r.getKey()) + r.getKey();
	}

	private static double sigmoid(double a, double x) {
		return 1.0 / (1.0 + Math.exp(-a * x));
	}

	public static String getSetting(Team team) {
		return getSetting(team == Team.VILLAGER ? Role.VILLAGER : Role.WEREWOLF);
	}

	public static String getSetting(Role role) {
		PersonalityFactory temp = get(role);
		StringBuilder sb = new StringBuilder();
		Pair<Double, Double> value = null;
		sb.append(role);
		sb.append(",");
		if (temp.oe != null) {
			sb.append("OE");
			sb.append(",");
			value = temp.oe;
		} else if (temp.co != null) {
			sb.append("CO");
			sb.append(",");
			value = temp.co;
		} else if (temp.ex != null) {
			sb.append("EX");
			sb.append(",");
			value = temp.ex;
		} else if (temp.ag != null) {
			sb.append("AG");
			sb.append(",");
			value = temp.ag;
		} else if (temp.ne != null) {
			sb.append("NE");
			sb.append(",");
			value = temp.ne;
		}
		sb.append(value);
		sb.append(",");

		return sb.toString();
	}

}
