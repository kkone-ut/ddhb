package com.gmail.toooo1718tyan.Estimator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aiwolf.common.data.Agent;

public final class LogisticRegression5Villager {
	// 日
	private static double c1 = 0.09299;
	// ターン
	private static double c2 = 0.01617;
	// 生存しているか否か
	private static double c3 = 0.0;
	// 死因
	private static double c4 = 0.0;
	// 現在の占いCO数
	private static double c5 = 1.248;
	// 生存している占いCOしたエージェント
	private static double c7 = 0.1266;
	// 人間判定を受けた数
	private static double c9 = 0.006763;
	// 人狼判定を受けた数
	private static double c10 = -1.054;
	// 人間判定した数
	private static double c11 = -13.35;
	// 人狼判定した数
	private static double c12 = -14.64;
	// 何番目に占いCOしたか
	private static double c13 = -9.547;
	// その日に何回投票発言したか
	private static double c15 = 0.0004583;
	// このゲームの投票先の相違回数
	private static double c16 = -0.08421;
	// 人狼Estimate発言した回数
	private static double c17 = -0.04181;
	// 定数
	private static double c = -10.99;

//	// エージェントの管理
//	private static Map<Agent, Double>  = new HashMap<Agent, Double>();

	private static Agent getVillager(Map<Agent, Double> agents) {
		Agent villafer = null;
		double max = 0.0;

		for(Agent agent: agents.keySet()) {
			if (max < agents.get(agent)) {
				max = agents.get(agent);
				villafer = agent;
			}
		}

		// debug
//		System.err.println("Agent :" + werewolf + " P :" + max);

		return villafer;
	}

	public static double logisticVillagerProbabilityEstimator(Agent agent) {
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

	public static Agent numVillager5Estimator(List<Agent> agentsList) {
		Agent villafer = null;
		Map<Agent, Double> agentsMap = new HashMap<Agent, Double>();

		for(Agent agent : agentsList) {
			double calc = logisticVillagerProbabilityEstimator(agent);
			agentsMap.put(agent, calc);
		}

		villafer = getVillager(agentsMap);

		return villafer;
	}

}
