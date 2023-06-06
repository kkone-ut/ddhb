package jp.ac.shibaura_it.ma15082;

import java.util.ArrayList;
import java.util.List;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Vote;
import org.aiwolf.common.net.GameInfo;

public class TalkInfo {
	List<Agent> agents;
	double[][] s, t, u, r;
	double[][] p;
	List<double[]> d;
	double[][] v;
	double[] b, c, e;
	int day;
	int[] vs;
	int[] ws;
	int[] es;

	RequestAnalyzer attackAnalyzer;
	RequestAnalyzer voteAnalyzer;
	RequestAnalyzer divineAnalyzer;
	RequestAnalyzer guardAnalyzer;

	final int length;

	public TalkInfo(GameInfo gi) {
		agents = gi.getAgentList();
		day = -1;
		length = agents.size();
		p = new double[length][length];
		u = new double[length][length];
		s = new double[length][length];
		t = new double[length][length];
		r = new double[length][length];
		c = new double[length];
		b = new double[length];
		d = new ArrayList<double[]>();
		e = new double[length];
		vs = new int[length];
		ws = new int[length];
		es = new int[length];
		v = new double[length][length];
		attackAnalyzer = new RequestAnalyzer();
		voteAnalyzer = new RequestAnalyzer();
		divineAnalyzer = new RequestAnalyzer();
		guardAnalyzer = new RequestAnalyzer();

	}

	private void setMessages(List<Pair<Agent, Agent>> list, int[] table) {
		for (Pair<Agent, Agent> pair : list) {
			int i = agents.indexOf(pair.getKey());
			int j = agents.indexOf(pair.getValue());
			if (i < 0 || j < 0) {
				continue;
			}
			table[i] = j;
		}
	}

	private void setCount(Agent agent, int[] table) {
		int i = agents.indexOf(agent);
		if (i < 0) {
			return;
		}
		table[i]++;
	}

	private void setCounts(List<Agent> list, int[] table) {
		for (Agent agent : list) {
			setCount(agent, table);
		}
	}

	public double getPoint(Agent target, int alivesize, int[] table) {
		double num = 0;
		int j = agents.indexOf(target);
		for (int i = 0; i < table.length; i++) {
			if (table[i] == j) {
				num++;
			}
		}
		return Tools.root(num / (double) alivesize);
	}

	public double getCount(Agent target, int[] table) {
		int num = 0;
		int sum = 0;
		int j = agents.indexOf(target);
		if (j < 0) {
			return num;
		}
		num = table[j];
		if (num <= 0) {
			return num;
		}
		for (int i = 0; i < table.length; i++) {
			sum += table[i];
		}
		return Tools.root((num) / (double) (sum));

	}

	public void setAttackMessages(List<Pair<Agent, Agent>> attacklist) {
		setMessages(attacklist, ws);
	}

	private double getAttackPoint(Agent target, int alivesize) {
		return getPoint(target, alivesize, ws);
	}

	public double getAttackPoint(Agent from, Agent target, int alivesize) {
		double point = getAttackPoint(target, alivesize) + attackAnalyzer.getPoint(from, target, alivesize);
		return point > 1 ? 1 : point;
		// return point;
	}

	public void setVoteMessages(List<Pair<Agent, Agent>> votelist) {
		setMessages(votelist, vs);
	}

	private double getVotePoint(Agent target, int alivesize) {
		return getPoint(target, alivesize, vs);
	}

	public double getVotePoint(Agent from, Agent target, int alivesize) {
		double point = getVotePoint(target, alivesize) + voteAnalyzer.getPoint(from, target, alivesize);
		return point > 1 ? 1 : point;
		// return point;
	}

	public double getDivinePoint(Agent from, Agent target, int alivesize) {
		double point = divineAnalyzer.getPoint(from, target, alivesize);
		return point > 1 ? 1 : point;
		// return point;
	}

	public double getGuardPoint(Agent from, Agent target, int alivesize) {
		double point = guardAnalyzer.getPoint(from, target, alivesize);
		return point > 1 ? 1 : point;
		// return point;
	}

	public void setErrors(List<Agent> errorlist) {
		setCounts(errorlist, es);
	}

	public double getErrorPoint(Agent target) {
		return getCount(target, es);
	}

	public void setMessages(List<Message> mss, GameInfo gi) {
		if (gi.getDay() > day) {
			day = gi.getDay();
			Agent a = gi.getAttackedAgent();

			if (a != null) {
				double[] temp1 = new double[length];
				double[] temp2 = new double[length];
				double[] temp3 = new double[length];

				for (int i = 0; i < length; i++) {
					temp1[i] = getScore(agents.indexOf(a), i, 0);
					temp2[i] = s[agents.indexOf(a)][i];
					temp3[i] = t[agents.indexOf(a)][i];
				}
				d.add(temp1);
				d.add(temp2);
				d.add(temp3);
				e = avrDead();
			}

			List<Vote> votes = gi.getVoteList();
			for (int i = 0; i < length; i++) {
				for (int j = 0; j < length; j++) {
					p[i][j] = u[i][j];
				}
			}
			for (int i = 0; i < length; i++) {
				for (int j = 0; j < length; j++) {
					u[i][j] = (i == j) ? 5 : (Tools.random() - 0.5) * 2;
				}
			}

			if (votes != null)
				for (Vote vote : votes) {
					int i = agents.indexOf(vote.getAgent());
					int j = agents.indexOf(vote.getTarget());
					if (i < 0) {
						continue;
					}
					if (j < 0) {
						j = i;
					}
					u[i][j] -= 4;
				}

			for (int i = 0; i < length; i++) {
				vs[i] = -1;
				ws[i] = -1;
			}

			attackAnalyzer.clear();
			voteAnalyzer.clear();
			divineAnalyzer.clear();
			guardAnalyzer.clear();

		}

		for (Message m : mss) {
			int i = agents.indexOf(m.getFrom());
			int j = agents.indexOf(m.getSubject());
			if (i < 0 || j < 0) {
				continue;
			}
			double d = m.getObject().equals(Colour.WHITE) ? 1 : -1;
			u[i][j] += d;
			u[i][i] += 0.5;
		}

	}

	public void setRequests(List<Request> mss) {
		for (Request r : mss) {
			try {
				if (r.getVerb() == null || r.getObject() == null) {
					// 指示が不正
					throw new Exception();
				}
				switch (r.getVerb()) {
				case VOTE:
					voteAnalyzer.put(r);
					break;
				case DIVINED:
				case DIVINATION:
					divineAnalyzer.put(r);
					break;
				case ATTACK:
					attackAnalyzer.put(r);
					break;
				case GUARD:
				case GUARDED:
					guardAnalyzer.put(r);
					break;
				default:
					break;
				}
			} catch (Exception e) {
				// エラーになる指示をしたエージェントを記録する
				setCount(r.getFrom(), es);
			}
		}
	}

	public void calcScore() {
		final double prev = 1.0;
		for (int i = 0; i < length; i++) {
			for (int j = 0; j < length; j++) {
				r[i][j] = u[i][j] + prev * p[i][j];
			}
		}

		for (int i = 0; i < length; i++) {
			c[i] = 0;
			b[i] = 0;
			for (int j = 0; j < length; j++) {
				c[i] += r[i][j] * r[i][j];
				b[i] += r[j][i] * r[j][i];
			}
			c[i] = Math.sqrt(c[i]);
			b[i] = Math.sqrt(b[i]);
		}

		for (int i = 0; i < length; i++) {
			for (int j = 0; j < length; j++) {
				if (i == j) {
					t[i][j] = 1;
					s[i][j] = 1;
					continue;
				}
				t[i][j] = 0;
				s[i][j] = 0;
				for (int k = 0; k < length; k++) {
					t[i][j] += r[i][k] * r[k][j];
					s[i][j] += r[i][k] * r[j][k];
				}
				t[i][j] /= c[i] * b[j];
				s[i][j] /= c[i] * c[j];

			}

		}

	}

	public double getScore(Agent from, Agent to, int d) {
		return getScore(agents.indexOf(from), agents.indexOf(to), d);
	}

	public double getScore(int i, int j, int d) {
		double x = s[i][j] + t[i][j] + 2 + e[j];
		// return x*x/16;
		return x / 5;
	}

	//
	public double[] avrDead() {
		double[] ret = new double[length];
		int[] rank = new int[length];
		for (double[] temp : d) {
			for (int i = 0; i < length; i++) {
				rank[i] = i;
			}
			for (int i = 0; i < length; i++) {
				for (int j = i + 1; j < length; j++) {
					if (temp[rank[i]] < temp[rank[j]]) {
						int buf = rank[i];
						rank[i] = rank[j];
						rank[j] = buf;
					}
				}
			}
			for (int i = 0; i < length; i++) {
				ret[i] -= (double) rank[i] / d.size();
			}
		}

		for (int i = 0; i < length; i++) {
			rank[i] = i;
		}
		for (int i = 0; i < length; i++) {
			for (int j = i + 1; j < length; j++) {
				if (ret[rank[i]] < ret[rank[j]]) {
					int buf = rank[i];
					rank[i] = rank[j];
					rank[j] = buf;
				}
			}
		}
		// 疑われている方がrankは大きい

		for (int i = 0; i < length; i++) {
			ret[i] = (double) (length - rank[i]) / length;
		}

		return ret;
	}

}
