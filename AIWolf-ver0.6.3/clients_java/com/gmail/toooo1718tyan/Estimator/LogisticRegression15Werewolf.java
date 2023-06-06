package com.gmail.toooo1718tyan.Estimator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aiwolf.common.data.Agent;

public final class LogisticRegression15Werewolf {
	// 日
	private static double c1 = 0.0438494;
	// ターン
	private static double c2 = 0.0227432;
	// 生存しているか否か
	private static double c3 = 0.0;
	// 死因
	private static double c4 = 0.0;
	// 現在の占いCO数
	private static double c5 = -0.0804541;
	// 現在の霊媒CO数
	private static double c6 = 0.0063909;
	// 生存している占いCOしたエージェント
	private static double c7 = 0.0525164;
	// 生存してい霊媒COしたエージェント
	private static double c8 = 0.0137688;
	// 人間判定を受けた数
	private static double c9 = -0.4482058;
	// 人狼判定を受けた数
	private static double c10 = 0.6794657;
	// 人間判定した数
	private static double c11 = 0.2367359;
	// 人狼判定した数
	private static double c12 = 0.2360822;
	// 何番目に占いCOしたか
	private static double c13 = 0.2578134;
	// 何番目に霊媒COしたか
	private static double c14 = 0.0849207;
	// その日に何回投票発言したか
	private static double c15 = -0.1606994;
	// このゲームの投票先の相違回数
	private static double c16 = -0.1263795;
	// 人狼Estimate発言した回数
	private static double c17 = 0.1938595;
	// 定数
	private static double c = -0.9625313;


	private static Agent getWerewolf(Map<Agent, Double> agents) {
		Agent werewolf = null;
		double max = 0.0;

		for(Agent agent: agents.keySet()) {
			if (max < agents.get(agent)) {
				max = agents.get(agent);
				werewolf = agent;
			}
		}

		return werewolf;
	}


	public static double logisticWerewolfProbabilityEstimator(Agent agent) {

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


	public static Agent numWerewolf5Estimator(List<Agent> agentsList) {
		Agent werewolf = null;
		Map<Agent, Double> agentsMap = new HashMap<Agent, Double>();

		for(Agent agent : agentsList) {
			double calc = logisticWerewolfProbabilityEstimator(agent);
			agentsMap.put(agent, calc);
		}

		werewolf = getWerewolf(agentsMap);

		return werewolf;
	}

}
