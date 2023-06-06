package jp.ac.shibaura_it.ma15082;

import java.util.ArrayList;
import java.util.List;

import org.aiwolf.common.data.Agent;

public class RequestAnalyzer {
	List<Pair<Agent, Agent>> all;
	ListMap<Agent, List<Pair<Agent, Agent>>> map;

	public RequestAnalyzer() {
		map = new ListMap<Agent, List<Pair<Agent, Agent>>>();
		all = new ArrayList<Pair<Agent, Agent>>();
	}

	public void clear() {
		map.clear();
		all.clear();
	}

	public void put(Request request) {
		Agent subject = request.getSubject();
		Agent from = request.getFrom();
		Agent object = request.getObject();
		List<Pair<Agent, Agent>> target = null;
		if (subject == null) {
			target = all;
		} else {
			target = map.get(subject);
			if (target == null) {
				target = new ArrayList<Pair<Agent, Agent>>();
				map.put(subject, target);
			}
		}
		target.add(new Pair<Agent, Agent>(from, object));

	}

	public double getPoint(Agent from, Agent target, int alivesize) {
		List<Pair<Agent, Agent>> list1 = all;
		List<Pair<Agent, Agent>> list2 = map.get(from);
		int num = 0;
		int sum = 0;
		for (Pair<Agent, Agent> pair : list1) {
			if (target.equals(pair.getValue())) {
				num++;
			}
			sum++;
		}
		if (list2 != null) {
			for (Pair<Agent, Agent> pair : list2) {
				if (target.equals(pair.getValue())) {
					num++;
				}
				sum++;
			}
		}
		return num / (double) alivesize;
	}

}
