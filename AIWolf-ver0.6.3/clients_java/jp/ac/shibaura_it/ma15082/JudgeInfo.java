package jp.ac.shibaura_it.ma15082;

import java.util.List;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Species;

public class JudgeInfo {
	Agent agent;
	ListMap<Judge, Integer> judgelist;
	int day;
	boolean safe;
	boolean slideCo;

	public JudgeInfo(Agent a, int d) {
		agent = a;
		judgelist = new ListMap<Judge, Integer>();
		day = (d > 0) ? d : 1;
		safe = true;
		slideCo = false;
	}

	// NOTE:Talkのdayは-1になっているからいつの結果かわからない
	// なぜかtalkindexが初期化されて繰り返し読まれるときがある
	public void put(int d, Agent p, Species s, int count) {
		// すでに結果を言っているなら無視する
		for (Judge j : judgelist.keyList()) {
			if (j.getTarget().equals(p)) {
				safe = false;
				return;
			}
		}
		judgelist.put(new Judge(day, agent, p, s), count);
		day = d;
	}

	// 結果の数が多すぎる
	public boolean tooMany() {
		return judgelist.size() > day;
	}

	public Agent getAgent() {
		return agent;
	}

	public Colour getColour(Agent target) {
		for (Judge j : judgelist.keyList()) {
			if (j.getTarget().equals(target)) {
				return j.getResult() == Species.HUMAN ? Colour.WHITE : Colour.BLACK;
			}

		}
		return Colour.GREY;
	}

	public void setSlideCo(boolean flag) {
		slideCo = flag;
	}

	public boolean isSlideCo() {
		return slideCo;
	}

	public List<Judge> getJudgeList() {
		return judgelist.keyList();
	}

	public int getDay() {
		return day;
	}

	public int size() {
		return judgelist.size();
	}

	public int blackNum() {
		int ret = 0;
		for (Judge j : judgelist.keyList()) {
			if (j.getResult() == Species.WEREWOLF) {
				ret++;
			}
		}
		return ret;
	}

	public int blackNum(List<Agent> agentlist) {
		int ret = 0;
		for (Judge j : judgelist.keyList()) {
			if (j.getResult() == Species.WEREWOLF && agentlist.contains(j.getTarget())) {
				ret++;
			}
		}
		return ret;
	}

	public double avrCount() {
		double ret = 0;
		for (int i : judgelist.valueList()) {
			ret += i;
		}
		if (ret == 0) {
			return 0;
		}
		return ret / judgelist.size();
	}

}
