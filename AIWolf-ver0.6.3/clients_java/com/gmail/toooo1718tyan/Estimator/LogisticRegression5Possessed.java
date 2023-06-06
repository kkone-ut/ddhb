package com.gmail.toooo1718tyan.Estimator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aiwolf.common.data.Agent;

public final class LogisticRegression5Possessed {
	// 日
	private static double c1 = -0.5820023;
	// ターン
	private static double c2 = 0.0682799;
	// 生存しているか否か
	private static double c3 = 0.0;
	// 死因
	private static double c4 = 0.0;
	// 現在の占いCO数
	private static double c5 = -0.7343281;
	// 生存している占いCOしたエージェント
	private static double c7 = 0.1647705;
	// 人間判定を受けた数
	private static double c9 = 0.1790521;
	// 人狼判定を受けた数
	private static double c10 = -0.1233817;
	// 人間判定した数
	private static double c11 = 0.3673691;
	// 人狼判定した数
	private static double c12 = -0.3698807;
	// 何番目に占いCOしたか
	private static double c13 = 0.7169072;
	// その日に何回投票発言したか
	private static double c15 = -0.4821845;
	// このゲームの投票先の相違回数
	private static double c16 = 0.0001079;
	// 人狼Estimate発言した回数
	private static double c17 = 0.0131413;
	// 定数
	private static double c = 0.6938273;

//	// エージェントの管理
//	private static Map<Agent, Double>  = new HashMap<Agent, Double>();

	private static Agent getPossessed(Map<Agent, Double> agents) {
		Agent possessed = null;
		double max = 0.0;

		for(Agent agent: agents.keySet()) {
			if (max < agents.get(agent)) {
				max = agents.get(agent);
				possessed = agent;
			}
		}

		// debug
//		System.err.println("Agent :" + werewolf + " P :" + max);

		return possessed;
	}

	public static double logisticPossessedProbabilityEstimator(Agent agent) {
		double calc = 0.0;

		calc = c1 * FeatureCalclation.getFeatureDay()
			+ c2 * FeatureCalclation.getFeatureTrun()
			+ c3 * FeatureCalclation.getFeatureAlive(agent)
			+ c4 * FeatureCalclation.getFeatureDeath(agent)
			+ c5 * FeatureCalclation.getFeatureNumSeerCO()
			+ c7 * FeatureCalclation.getFuatureNumAliveSeerCO()
			+ c9 * FeatureCalclation.getFeatureReceiveHumanDivine(agent)
			+ c10 * FeatureCalclation.getFeatureReceiveWolfDivine(agent)
			+ c11 * FeatureCalclation.getFeatureHumanDivine(agent)
			+ c12 * FeatureCalclation.getFeatureWolfDivine(agent)
			+ c13 * FeatureCalclation.getFeatureSeerCoNumber(agent)
			+ c15 * FeatureCalclation.getFeatureVoteCount(agent)
			+ c16 * FeatureCalclation.getFeatureDifferentVote(agent)
			+ c17 * FeatureCalclation.getFeatureWolfEstimate(agent)
			+ c;
		calc = 1.0 + Math.exp(-calc);
		calc = 1.0 / calc;

		return calc;
	}

	public static Agent numPossessed5Estimator(List<Agent> agentsList) {
		Agent possessed = null;
		Map<Agent, Double> agentsMap = new HashMap<Agent, Double>();

		for(Agent agent : agentsList) {
			double calc = logisticPossessedProbabilityEstimator(agent);
			agentsMap.put(agent, calc);
		}

		possessed = getPossessed(agentsMap);

		return possessed;
	}

}
