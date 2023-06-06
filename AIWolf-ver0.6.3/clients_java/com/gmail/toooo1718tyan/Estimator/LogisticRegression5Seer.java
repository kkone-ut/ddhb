package com.gmail.toooo1718tyan.Estimator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aiwolf.common.data.Agent;

public final class LogisticRegression5Seer {
	// 日
	private static double c1 = 0.400505;
	// ターン
	private static double c2 = 0.026250;
	// 生存しているか否か
	private static double c3 = 0.0;
	// 死因
	private static double c4 = 0.0;
	// 現在の占いCO数
	private static double c5 = -1.034645;
	// 生存している占いCOしたエージェント
	private static double c7 = -0.019160;
	// 人間判定を受けた数
	private static double c9 = -0.770962;
	// 人狼判定を受けた数
	private static double c10 = -0.180739;
	// 人間判定した数
	private static double c11 = 0.599217;
	// 人狼判定した数
	private static double c12 = 0.648944;
	// 何番目に占いCOしたか
	private static double c13 = 1.082723;
	// その日に何回投票発言したか
	private static double c15 = 0.037052;
	// このゲームの投票先の相違回数
	private static double c16 = 0.043507;
	// 人狼Estimate発言した回数
	private static double c17 = -0.042768;
	// 定数
	private static double c = -0.544380;

//	// エージェントの管理
//	private static Map<Agent, Double>  = new HashMap<Agent, Double>();

	private static Agent getSeer(Map<Agent, Double> agents) {
		Agent seer = null;
		double max = 0.0;

		for(Agent agent: agents.keySet()) {
			if (max < agents.get(agent)) {
				max = agents.get(agent);
				seer = agent;
			}
		}

		// debug
//		System.err.println("Agent :" + werewolf + " P :" + max);

		return seer;
	}

	public static double logisticSeerProbabilityEstimator(Agent agent) {
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

	public static Agent numSeer5Estimator(List<Agent> agentsList) {
		Agent seer = null;
		Map<Agent, Double> agentsMap = new HashMap<Agent, Double>();

		for(Agent agent : agentsList) {
			double calc = logisticSeerProbabilityEstimator(agent);
			agentsMap.put(agent, calc);
		}

		seer = getSeer(agentsMap);

		return seer;
	}

}
