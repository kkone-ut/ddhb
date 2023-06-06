package com.gmail.toooo1718tyan.Estimator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aiwolf.common.data.Agent;

public final class LogisticRegression15Seer {
	// 日
	private static double c1 = 0.245846;
	// ターン
	private static double c2 = 0.011812;
	// 生存しているか否か
	private static double c3 = 0.0;
	// 死因
	private static double c4 = 0.0;
	// 現在の占いCO数
	private static double c5 = -0.921148;
	// 現在の霊媒CO数
	private static double c6 = 0.058541;
	// 生存している占いCOしたエージェント
	private static double c7 = 0.122346;
	// 生存してい霊媒COしたエージェント
	private static double c8 = 0.020503;
	// 人間判定を受けた数
	private static double c9 = 0.045534;
	// 人狼判定を受けた数
	private static double c10 = -0.035779;
	// 人間判定した数
	private static double c11 = 1.162929;
	// 人狼判定した数
	private static double c12 = 0.549838;
	// 何番目に占いCOしたか
	private static double c13 = 1.357729;
	// 何番目に霊媒COしたか
	private static double c14 = -7.176462;
	// その日に何回投票発言したか
	private static double c15 = 0.095619;
	// このゲームの投票先の相違回数
	private static double c16 = -0.287103;
	// 人狼Estimate発言した回数
	private static double c17 = -0.235146;
	// 定数
	private static double c = -8.745791;


	private static Agent getSeer(Map<Agent, Double> agents) {
		Agent seer = null;
		double max = 0.0;

		for(Agent agent: agents.keySet()) {
			if (max < agents.get(agent)) {
				max = agents.get(agent);
				seer = agent;
			}
		}

		return seer;
	}


	public static double logisticSeerProbabilityEstimator(Agent agent) {

		double calc = c1 * FeatureCalclation.getFeatureDay()
				+ c2 * FeatureCalclation.getFeatureTrun()
				+ c3 * FeatureCalclation.getFeatureAlive(agent)
				+ c4 * FeatureCalclation.getFeatureDeath(agent)
				+ c5 * FeatureCalclation.getFeatureNumSeerCO()
				+ c6 * FeatureCalclation.getFeatureNumMediumCO()
				+ c7 * FeatureCalclation.getFuatureNumAliveSeerCO()
				+ c8 * FeatureCalclation.getFuatureNumAliveMediumCO()
				+ c9 * FeatureCalclation.getFeatureReceiveHumanDivine(agent)
				+ c10 * FeatureCalclation.getFeatureReceiveWolfDivine(agent)
				+ c11 * FeatureCalclation.getFeatureHumanDivine(agent)
				+ c12 * FeatureCalclation.getFeatureWolfDivine(agent)
				+ c13 * FeatureCalclation.getFeatureSeerCoNumber(agent)
				+ c14 * FeatureCalclation.getFeatureMediumCoNumber(agent)
				+ c15 * FeatureCalclation.getFeatureVoteCount(agent)
				+ c16 * FeatureCalclation.getFeatureDifferentVote(agent)
				+ c17 * FeatureCalclation.getFeatureWolfEstimate(agent)
				+ c;
		calc = 1.0 + Math.exp(-calc);
		calc = 1.0 / calc;

		return calc;
	}


	public static Agent numSeer15Estimator(List<Agent> agentsList) {
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
