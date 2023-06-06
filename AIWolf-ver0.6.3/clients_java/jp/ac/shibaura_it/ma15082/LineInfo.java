package jp.ac.shibaura_it.ma15082;

import java.util.ArrayList;
import java.util.List;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;

public class LineInfo {
	public static WasabiAnalyzer wa;
	private JudgeInfo si;
	private JudgeInfo mi;
	private double score;
	private ListMap<Agent, Double> clist;
	private final boolean none_flag;
	private final double seer_certain;
	private final double medium_certain;

	public LineInfo(JudgeInfo seer, JudgeInfo medium, List<PlayerInfo> pis, int s_num, int m_num) {
		si = seer;
		mi = medium;
		clist = new ListMap<Agent, Double>(16);
		score = -1;
		seer_certain = wa.getRoleNum(Role.POSSESSED) / (double) Tools.square(wa.getPlayerNum());
		medium_certain = wa.getRoleNum(Role.POSSESSED) / (double) Tools.square(wa.getPlayerNum());
		// seer_certain=0;
		// medium_certain=0;
		none_flag = (si == null && mi == null);
		init(pis, s_num, m_num);
	}

	public void init(List<PlayerInfo> plist, int s_num, int m_num) {
		List<Agent> greylist = new ArrayList<Agent>();
		score = -1;
		int seer_liar = 0;
		int medium_liar = 0;
		int max_black = 0;
		int min_black = 0;
		int black_num = 0;
		int black_liar = 0;
		int white_liar = 0;
		int black_dead = 0;
		int bitten_liar = 0;
		int dead_liar = 0;
		int temp;
		boolean hatan_flag = false;

		black_liar = (wa.getRoleNum(Role.WEREWOLF));
		white_liar = (wa.getRoleNum(Role.POSSESSED));

		seer_liar = wa.seerSize() - wa.getRoleNum(Role.SEER) - 1;
		medium_liar = wa.mediumSize() - wa.getRoleNum(Role.MEDIUM) - 1;

		for (PlayerInfo pi : plist) {
			Colour cm = Colour.GREY;
			Colour cs = Colour.GREY;
			Colour cd = Colour.GREY;
			Colour c = Colour.GREY;
			Agent agent = pi.getAgent();
			boolean grey_flag = false;
			switch (pi.getRole()) {
			case SEER: {
				if (si == null) {
					clist.put(agent, seer_certain);
					continue;
				} else if (agent.equals(si.getAgent())) {
					clist.put(agent, 1.0);
					c = Colour.WHITE;
				} else {
					clist.put(agent, seer_certain);
					if (pi.isDead()) {
						dead_liar++;
					}
					if (pi.isBitten()) {
						bitten_liar++;
					} else if (si != null && si.getColour(agent) == Colour.WHITE) {
						bitten_liar++;
					} else if (pi.isVoted()) {
						if (mi != null && mi.getColour(agent) == Colour.WHITE) {
							bitten_liar++;
						}
					}

				}
			}
				break;

			case MEDIUM: {
				if (mi == null) {
					clist.put(agent, medium_certain);
					continue;
				} else if (agent.equals(mi.getAgent())) {
					clist.put(agent, 1.0);
					c = Colour.WHITE;
				} else {
					clist.put(agent, medium_certain);
					if (pi.isDead()) {
						dead_liar++;
					}
					if (pi.isBitten()) {
						bitten_liar++;
					} else if (si != null && si.getColour(agent) == Colour.WHITE) {
						bitten_liar++;
					} else if (pi.isVoted()) {
						if (mi != null && mi.getColour(agent) == Colour.WHITE) {
							bitten_liar++;
						}
					}

				}
			}
				break;

			default:
				grey_flag = true;
				break;

			}

			if (mi != null) {
				cm = mi.getColour(pi.getAgent());
			}
			if (si != null) {
				cs = si.getColour(pi.getAgent());
			}
			if (pi.isBitten()) {
				cd = Colour.WHITE;
			}

			c = c.join(cs);
			c = c.join(cm);
			c = c.join(cd);
			switch (c) {
			case WHITE:
				if (grey_flag) {
					clist.put(agent, 1.0);
				}
				break;
			case BLACK:
				if (grey_flag) {
					clist.put(agent, 0.0);
					black_num++;
				}
				break;
			case GREY:
				if (grey_flag) {
					greylist.add(agent);
				}
				break;
			default:
				if (grey_flag) {
					greylist.add(agent);
				}
				clist.put(agent, 0.0);
				hatan_flag = true;
				break;

			}

		}

		temp = (seer_liar + medium_liar) - white_liar;

		if (temp < 0) {
			max_black = black_liar - black_num;
		} else {
			max_black = black_liar - temp - black_num;
		}

		temp = (seer_liar + medium_liar);
		if (temp < 0) {
			min_black = black_liar - black_num;
		} else {
			min_black = black_liar - temp - black_num;
		}

		if (si != null && si.isSlideCo()) {
			hatan_flag = true;
		} else if (mi != null && mi.isSlideCo()) {
			hatan_flag = true;
		} else if (max_black < 0) {
			hatan_flag = true;
		}
		// TODO:
		else if (max_black <= 0 && dead_liar >= (seer_liar + medium_liar)) {
			hatan_flag = true;
		} else if (min_black > greylist.size()) {
			hatan_flag = true;
		} else if (bitten_liar > white_liar) {
			hatan_flag = true;
		} else if (si != null && si.tooMany()) {
			hatan_flag = true;
		} else if (mi != null && (wa.getAliveSize() - 1)
				/ 2 < (wa.getRoleNum(Role.WEREWOLF) - mi.blackNum() - wa.getDay() + mi.size())) {
			hatan_flag = true;
		} else if (black_dead >= wa.getRoleNum(Role.WEREWOLF)) {
			hatan_flag = true;
		}

		if (hatan_flag && !none_flag) {
			score = 0;
			for (Agent s : greylist) {
				clist.put(s, 0.0);
			}
			return;
		}

		score = 1.0 / (bitten_liar + 1.0);
		// 役がかけているときは信頼度を下げる
		// NOTE:
		// 潜伏死が多いから少し大きめに設定する
		if (mi == null) {
			score *= 3.0 / wa.getPlayerNum();
			// score*=1.0/wa.getPlayerNum();
		}
		if (si == null) {
			score *= 1.5 / wa.getPlayerNum();
			// score*=1.0/wa.getPlayerNum();
		}

		double point = (double) (greylist.size() - max_black) / greylist.size();
		if (point < 0) {
			point = 0;
		}

		for (Agent s : greylist) {
			clist.put(s, point);
		}
		return;

	}

	public double getScore() {
		return score;
	}

	public ListMap<Agent, Double> getScoreList() {
		return clist;
	}

	public JudgeInfo getSeer() {
		return si;
	}

	public JudgeInfo getMedium() {
		return mi;
	}

}
