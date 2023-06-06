package com.gmail.toooo1718tyan.Estimator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.net.GameInfo;

public final class LogisticRegression5 {

	public enum Agent5Role {
		WEREWOLF,
		SEER,
		POSSESSED,
		VILLAGER1,
		VILLAGER2;
	}


	public static Role emun2role(Agent5Role emunRole) {
		Role retRole = null;

		switch (emunRole) {
			case WEREWOLF:
				retRole = Role.WEREWOLF;
				break;
			case SEER:
				retRole = Role.SEER;
				break;
			case POSSESSED:
				retRole = Role.POSSESSED;
				break;
			case VILLAGER1:
			case VILLAGER2:
				retRole = Role.VILLAGER;
				break;
			default:
				break;
		}

		return retRole;
	}


	public static Agent5Role role2emun(Role role, boolean f) {
		Agent5Role ret = null;

		switch (role) {
			case WEREWOLF:
				ret = Agent5Role.WEREWOLF;
				break;
			case SEER:
				ret = Agent5Role.SEER;
				break;
			case POSSESSED:
				ret = Agent5Role.POSSESSED;
				break;
			case VILLAGER:
				if (f) {
					ret = Agent5Role.VILLAGER1;
				} else {
					ret = Agent5Role.VILLAGER2;
				}
				break;
			default:
				break;
		}

		return ret;
	}


	private static double calcCoincidentProbability(Map<Agent, Map<Role, Double>> probabilityMap, Map<Agent5Role, Agent> estimateRoleMap) {
		double ret = 1.0;

		for (Map.Entry<Agent5Role, Agent> entry : estimateRoleMap.entrySet()) {
			if (entry.getValue() != null) {
				Map<Role, Double> agentMap = new HashMap<>(probabilityMap.get(entry.getValue()));
				ret *= agentMap.get(emun2role(entry.getKey()));
			}
		}

		return ret;
	}


	private static boolean estimateWerewolfCheck(Map<Agent5Role, Agent> estimateRoleMap) {
		boolean ret = false;

		if (estimateRoleMap.get(Agent5Role.WEREWOLF) == null) {
			ret = true;
		}

		return ret;
	}


	private static Map<Agent5Role, Agent> dfs(Map<Agent, Map<Role, Double>> probabilityMap, Map<Agent5Role, Agent> estimateRoleMap, List<Agent> agentList) {
		Map<Agent5Role, Agent> ret_estimateRoleMap = new HashMap<>(estimateRoleMap);
		List<Agent> loc_agentList = new ArrayList<>(agentList);
		Agent estimateAgent = loc_agentList.get(0);
		loc_agentList.remove(estimateAgent);
		double maxProbability = 0.0;

		for (Map.Entry<Agent5Role, Agent> entry : estimateRoleMap.entrySet()) {
			Map<Agent5Role, Agent> loc_estimateRoleMap = new HashMap<>(estimateRoleMap);
			boolean fWerewolfCheck;
			if (entry.getValue() == null) {
				loc_estimateRoleMap.replace(entry.getKey(), estimateAgent);
				if (loc_agentList.size() != 0) {
					loc_estimateRoleMap = dfs(probabilityMap, loc_estimateRoleMap, loc_agentList);
				}
				double calc_tmp = calcCoincidentProbability(probabilityMap, loc_estimateRoleMap);

				fWerewolfCheck = estimateWerewolfCheck(loc_estimateRoleMap);

				if (calc_tmp > maxProbability && !fWerewolfCheck) {
					ret_estimateRoleMap = loc_estimateRoleMap;
				}
			}
		}

		return ret_estimateRoleMap;
	}


	public static Map<Agent5Role, Agent> numPlayer5Estimator(List<Agent> agentsList, GameInfo gameInfo) {
		Map<Agent, Map<Role, Double>> probabilityMap = new HashMap<Agent, Map<Role, Double>>();
		Map<Agent5Role, Agent> estimateRoleMap = new HashMap<Agent5Role, Agent>();

		for (Agent5Role role : Agent5Role.values()) {
			estimateRoleMap.put(role, null);
		}
		estimateRoleMap.replace(role2emun(gameInfo.getRole(), true), gameInfo.getAgent());

		Map<Role, Double> agentMap = new HashMap<Role, Double>();
		switch (gameInfo.getRole()) {
			case WEREWOLF:
				agentMap.put(Role.WEREWOLF, 1.0);
				agentMap.put(Role.SEER, 0.0);
				agentMap.put(Role.POSSESSED, 0.0);
				agentMap.put(Role.VILLAGER, 0.0);
				break;
			case SEER:
				agentMap.put(Role.WEREWOLF, 0.0);
				agentMap.put(Role.SEER, 1.0);
				agentMap.put(Role.POSSESSED, 0.0);
				agentMap.put(Role.VILLAGER, 0.0);
				break;
			case POSSESSED:
				agentMap.put(Role.WEREWOLF, 0.0);
				agentMap.put(Role.SEER, 0.0);
				agentMap.put(Role.POSSESSED, 1.0);
				agentMap.put(Role.VILLAGER, 0.0);
				break;
			case VILLAGER:
				agentMap.put(Role.WEREWOLF, 0.0);
				agentMap.put(Role.SEER, 0.0);
				agentMap.put(Role.POSSESSED, 0.0);
				agentMap.put(Role.VILLAGER, 1.0);
				break;
			default:
				break;
		}
		probabilityMap.put(gameInfo.getAgent(), agentMap);

		for (Agent agent : agentsList) {
			agentMap = new HashMap<Role, Double>();
			agentMap.put(Role.WEREWOLF, LogisticRegression5Werewolf.logisticWerewolfProbabilityEstimator(agent));
			agentMap.put(Role.SEER, LogisticRegression5Seer.logisticSeerProbabilityEstimator(agent));
			agentMap.put(Role.POSSESSED, LogisticRegression5Possessed.logisticPossessedProbabilityEstimator(agent));
			agentMap.put(Role.VILLAGER, LogisticRegression5Villager.logisticVillagerProbabilityEstimator(agent));

			probabilityMap.put(agent, agentMap);
		}

		estimateRoleMap = dfs(probabilityMap, estimateRoleMap, agentsList);

		return estimateRoleMap;
	}

}
