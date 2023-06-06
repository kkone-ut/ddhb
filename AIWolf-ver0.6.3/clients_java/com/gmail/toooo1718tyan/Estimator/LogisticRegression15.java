package com.gmail.toooo1718tyan.Estimator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.net.GameInfo;

public final class LogisticRegression15 {

	public enum Agent15Role {
		WEREWOLF1,
		WEREWOLF2,
		WEREWOLF3,
		SEER,
		POSSESSED,
		MEDIUM,
		BODYGUARD,
		VILLAGER1,
		VILLAGER2,
		VILLAGER3,
		VILLAGER4,
		VILLAGER5,
		VILLAGER6,
		VILLAGER7,
		VILLAGER8;
	}

	public final static int NUM_TOTAL_AGENT = 15;

	private static Map<Agent, Map<Role, Double>> probabilityMap = new HashMap<Agent, Map<Role, Double>>();


	public static void initProbabilityMap(List<Agent> agentsList, GameInfo gameInfo) {
		probabilityMap = new HashMap<Agent, Map<Role, Double>>();

		Map<Role, Double> agentMap = new HashMap<Role, Double>();

		switch (gameInfo.getRole()) {
			case WEREWOLF:
				agentMap.put(Role.WEREWOLF, 1.0);
				agentMap.put(Role.SEER, 0.0);
				agentMap.put(Role.POSSESSED, 0.0);
				agentMap.put(Role.MEDIUM, 0.0);
				agentMap.put(Role.BODYGUARD, 0.0);
				agentMap.put(Role.VILLAGER, 0.0);
				break;
			case SEER:
				agentMap.put(Role.WEREWOLF, 0.0);
				agentMap.put(Role.SEER, 1.0);
				agentMap.put(Role.POSSESSED, 0.0);
				agentMap.put(Role.MEDIUM, 0.0);
				agentMap.put(Role.BODYGUARD, 0.0);
				agentMap.put(Role.VILLAGER, 0.0);
				break;
			case POSSESSED:
				agentMap.put(Role.WEREWOLF, 0.0);
				agentMap.put(Role.SEER, 0.0);
				agentMap.put(Role.POSSESSED, 1.0);
				agentMap.put(Role.MEDIUM, 0.0);
				agentMap.put(Role.BODYGUARD, 0.0);
				agentMap.put(Role.VILLAGER, 0.0);
				break;
			case MEDIUM:
				agentMap.put(Role.WEREWOLF, 0.0);
				agentMap.put(Role.SEER, 0.0);
				agentMap.put(Role.POSSESSED, 0.0);
				agentMap.put(Role.MEDIUM, 1.0);
				agentMap.put(Role.BODYGUARD, 0.0);
				agentMap.put(Role.VILLAGER, 0.0);
				break;
			case BODYGUARD:
				agentMap.put(Role.WEREWOLF, 0.0);
				agentMap.put(Role.SEER, 0.0);
				agentMap.put(Role.POSSESSED, 0.0);
				agentMap.put(Role.MEDIUM, 0.0);
				agentMap.put(Role.BODYGUARD, 1.0);
				agentMap.put(Role.VILLAGER, 0.0);
				break;
			case VILLAGER:
				agentMap.put(Role.WEREWOLF, 0.0);
				agentMap.put(Role.SEER, 0.0);
				agentMap.put(Role.POSSESSED, 0.0);
				agentMap.put(Role.MEDIUM, 0.0);
				agentMap.put(Role.BODYGUARD, 0.0);
				agentMap.put(Role.VILLAGER, 1.0);
				break;
			default:
				break;
		}
		probabilityMap.put(gameInfo.getAgent(), agentMap);

		for (Agent agent : agentsList) {
			agentMap = new HashMap<Role, Double>();
			agentMap.put(Role.WEREWOLF, 1.0);
			agentMap.put(Role.SEER, 1.0);
			agentMap.put(Role.POSSESSED, 1.0);
			agentMap.put(Role.MEDIUM, 1.0);
			agentMap.put(Role.BODYGUARD, 1.0);
			agentMap.put(Role.VILLAGER, 1.0);
			probabilityMap.put(agent, agentMap);
		}

		return;
	}


	public static void initProbabilityMap(List<Agent> agentsList, GameInfo gameInfo, Set<Agent> werewolfList) {
		probabilityMap = new HashMap<Agent, Map<Role, Double>>();

		Map<Role, Double> agentMap = new HashMap<Role, Double>();

		switch (gameInfo.getRole()) {
			case WEREWOLF:
				agentMap.put(Role.WEREWOLF, 1.0);
				agentMap.put(Role.SEER, 0.0);
				agentMap.put(Role.POSSESSED, 0.0);
				agentMap.put(Role.MEDIUM, 0.0);
				agentMap.put(Role.BODYGUARD, 0.0);
				agentMap.put(Role.VILLAGER, 0.0);
				break;
			case SEER:
				agentMap.put(Role.WEREWOLF, 0.0);
				agentMap.put(Role.SEER, 1.0);
				agentMap.put(Role.POSSESSED, 0.0);
				agentMap.put(Role.MEDIUM, 0.0);
				agentMap.put(Role.BODYGUARD, 0.0);
				agentMap.put(Role.VILLAGER, 0.0);
				break;
			case POSSESSED:
				agentMap.put(Role.WEREWOLF, 0.0);
				agentMap.put(Role.SEER, 0.0);
				agentMap.put(Role.POSSESSED, 1.0);
				agentMap.put(Role.MEDIUM, 0.0);
				agentMap.put(Role.BODYGUARD, 0.0);
				agentMap.put(Role.VILLAGER, 0.0);
				break;
			case MEDIUM:
				agentMap.put(Role.WEREWOLF, 0.0);
				agentMap.put(Role.SEER, 0.0);
				agentMap.put(Role.POSSESSED, 0.0);
				agentMap.put(Role.MEDIUM, 1.0);
				agentMap.put(Role.BODYGUARD, 0.0);
				agentMap.put(Role.VILLAGER, 0.0);
				break;
			case BODYGUARD:
				agentMap.put(Role.WEREWOLF, 0.0);
				agentMap.put(Role.SEER, 0.0);
				agentMap.put(Role.POSSESSED, 0.0);
				agentMap.put(Role.MEDIUM, 0.0);
				agentMap.put(Role.BODYGUARD, 1.0);
				agentMap.put(Role.VILLAGER, 0.0);
				break;
			case VILLAGER:
				agentMap.put(Role.WEREWOLF, 0.0);
				agentMap.put(Role.SEER, 0.0);
				agentMap.put(Role.POSSESSED, 0.0);
				agentMap.put(Role.MEDIUM, 0.0);
				agentMap.put(Role.BODYGUARD, 0.0);
				agentMap.put(Role.VILLAGER, 1.0);
				break;
			default:
				break;
		}
		probabilityMap.put(gameInfo.getAgent(), agentMap);

		for (Agent agent : agentsList) {
			agentMap = new HashMap<Role, Double>();

			if (werewolfList.contains(agent)) {
				agentMap.put(Role.WEREWOLF, 1.0);
				agentMap.put(Role.SEER, 0.0);
				agentMap.put(Role.POSSESSED, 0.0);
				agentMap.put(Role.MEDIUM, 0.0);
				agentMap.put(Role.BODYGUARD, 0.0);
				agentMap.put(Role.VILLAGER, 0.0);
			} else {
				agentMap.put(Role.WEREWOLF, 1.0);
				agentMap.put(Role.SEER, 1.0);
				agentMap.put(Role.POSSESSED, 1.0);
				agentMap.put(Role.MEDIUM, 1.0);
				agentMap.put(Role.BODYGUARD, 1.0);
				agentMap.put(Role.VILLAGER, 1.0);
			}

			probabilityMap.put(agent, agentMap);
		}

		return;
	}


	private static double chooseLogosticRegression15(Agent agent, Role role) {
		double calc = 1.0;

		switch (role) {
			case WEREWOLF:
				calc = LogisticRegression15Werewolf.logisticWerewolfProbabilityEstimator(agent);
				break;
			case SEER:
				calc = LogisticRegression15Seer.logisticSeerProbabilityEstimator(agent);
				break;
			case POSSESSED:
				calc = LogisticRegression15Possessed.logisticPossessedProbabilityEstimator(agent);
				break;
			case MEDIUM:
				calc = LogisticRegression15Medium.logisticMediumProbabilityEstimator(agent);
				break;
			case BODYGUARD:
				calc = LogisticRegression15Bodyguard.logisticBodyguardProbabilityEstimator(agent);
				break;
			case VILLAGER:
				calc = LogisticRegression15Villager.logisticVillagerProbabilityEstimator(agent);
				break;
			default:
				break;
		}

		return calc;
	}


	// 村人・狂人・狩人
	public static void updateProbabilityMap(List<Agent> agentList) {

		for (Agent agent : agentList) {
			Map<Role, Double> tmpMap = probabilityMap.get(agent);

			for (Role role : tmpMap.keySet()) {
				double tmpProbability = tmpMap.get(role);
				tmpProbability = tmpProbability * chooseLogosticRegression15(agent,role);
				tmpMap.replace(role, tmpProbability);
			}

			probabilityMap.replace(agent, tmpMap);

		}

		return;
	}


	// 人狼
	public static void updateProbabilityMap(List<Agent> agentList, Set<Agent> werewolfList) {

		for (Agent agent : agentList) {
			if (werewolfList.contains(agent)) {
				continue;
			}

			Map<Role, Double> tmpMap = probabilityMap.get(agent);

			for (Role role : tmpMap.keySet()) {
				double tmpProbability = tmpMap.get(role);
				tmpProbability = tmpProbability * chooseLogosticRegression15(agent,role);
				tmpMap.replace(role, tmpProbability);
			}

			probabilityMap.replace(agent, tmpMap);

		}

		return;
	}


	// 占
	public static void updateProbabilityMap(List<Agent> agentList, Set<Agent> whiteList, Set<Agent> blackList) {

		for (Agent agent : agentList) {
			Map<Role, Double> tmpMap = probabilityMap.get(agent);

			if (whiteList.contains(agent)) {
				tmpMap.replace(Role.WEREWOLF, 0.0);
			}

			if (blackList.contains(agent)) {
				tmpMap.replace(Role.SEER, 0.0);
				tmpMap.replace(Role.POSSESSED, 0.0);
				tmpMap.replace(Role.MEDIUM, 0.0);
				tmpMap.replace(Role.BODYGUARD, 0.0);
				tmpMap.replace(Role.VILLAGER, 0.0);
			}

			for (Role role : tmpMap.keySet()) {
				double tmpProbability = tmpMap.get(role);
				tmpProbability = tmpProbability * chooseLogosticRegression15(agent,role);
				tmpMap.replace(role, tmpProbability);
			}

			probabilityMap.replace(agent, tmpMap);

		}

		return;
	}


	// 霊媒師
	public static void updateProbabilityMap(List<Agent> agentList, Judge ident) {

		for (Agent agent : agentList) {
			Map<Role, Double> tmpMap = probabilityMap.get(agent);

			for (Role role : tmpMap.keySet()) {
				double tmpProbability = tmpMap.get(role);
				tmpProbability = tmpProbability * chooseLogosticRegression15(agent,role);
				tmpMap.replace(role, tmpProbability);
			}

			probabilityMap.replace(agent, tmpMap);
		}


		return;
	}


	public static Role emun2role(Agent15Role emunRole) {
		Role retRole = null;

		switch (emunRole) {
			case WEREWOLF1:
			case WEREWOLF2:
			case WEREWOLF3:
				retRole = Role.WEREWOLF;
				break;
			case SEER:
				retRole = Role.SEER;
				break;
			case POSSESSED:
				retRole = Role.POSSESSED;
				break;
			case MEDIUM:
				retRole = Role.MEDIUM;
				break;
			case BODYGUARD:
				retRole = Role.BODYGUARD;
				break;
			case VILLAGER1:
			case VILLAGER2:
			case VILLAGER3:
			case VILLAGER4:
			case VILLAGER5:
			case VILLAGER6:
			case VILLAGER7:
			case VILLAGER8:
				retRole = Role.VILLAGER;
				break;
			default:
				break;
		}

		return retRole;
	}


	public static Agent15Role role2emun(Role role, boolean f) {
		Agent15Role ret = null;

		switch (role) {
			case WEREWOLF:
				if (f) {
					ret = Agent15Role.WEREWOLF1;
				} else {

				}
				break;
			case SEER:
				ret = Agent15Role.SEER;
				break;
			case POSSESSED:
				ret = Agent15Role.POSSESSED;
				break;
			case MEDIUM:
				ret = Agent15Role.MEDIUM;
				break;
			case BODYGUARD:
				ret = Agent15Role.BODYGUARD;
				break;
			case VILLAGER:
				if (f) {
					ret = Agent15Role.VILLAGER1;
				} else {

				}
				break;
			default:
				break;
		}

		return ret;
	}


	private static double calcCoincidentProbability(Map<Agent15Role, Agent> estimateRoleMap) {
		double ret = 1.0;

		for (Map.Entry<Agent15Role, Agent> entry : estimateRoleMap.entrySet()) {
			if (entry.getValue() != null) {
				Map<Role, Double> agentMap = new HashMap<>(probabilityMap.get(entry.getValue()));
				ret *= agentMap.get(emun2role(entry.getKey()));
			}
		}

		return ret;
	}


	private static boolean estimateWerewolfCheck(Map<Agent15Role, Agent> estimateRoleMap) {
		boolean ret = false;

		if (estimateRoleMap.get(Agent15Role.WEREWOLF1) == null
		 || estimateRoleMap.get(Agent15Role.WEREWOLF2) == null
		 || estimateRoleMap.get(Agent15Role.WEREWOLF3) == null) {
			ret = true;
		}

		return ret;
	}


	private static boolean pruningCheck(Map.Entry<Agent15Role, Agent> entry, Map.Entry<Agent15Role, Agent> previousEntry) {
		boolean ret = false;

		if (previousEntry != null) {
			Role entryRole = emun2role(entry.getKey());
			Role previousEntryRole = emun2role(previousEntry.getKey());
			if (entryRole == previousEntryRole) {
				if (previousEntry.getValue() == null) {
					ret = true;
				}
			}
		}

		return ret;
	}


	private static Map<Agent15Role, Agent> dfs(Map<Agent15Role, Agent> estimateRoleMap, List<Agent> agentList) {
		Map<Agent15Role, Agent> ret_estimateRoleMap = new HashMap<>(estimateRoleMap);
		List<Agent> loc_agentList = new ArrayList<>(agentList);
		Agent estimateAgent = loc_agentList.get(0);
		loc_agentList.remove(estimateAgent);
		double maxProbability = 0.0;
		Map.Entry<Agent15Role, Agent> previousEntry = null;

		for (Map.Entry<Agent15Role, Agent> entry : estimateRoleMap.entrySet()) {
			if (pruningCheck(entry, previousEntry)) {
				previousEntry = entry;
				continue;
			}
			Map<Agent15Role, Agent> loc_estimateRoleMap = new HashMap<>(estimateRoleMap);
			boolean fWerewolfCheck;
			if (entry.getValue() == null) {
				loc_estimateRoleMap.replace(entry.getKey(), estimateAgent);
				if (loc_agentList.size() != 0) {
					loc_estimateRoleMap = dfs(loc_estimateRoleMap, loc_agentList);
				}
				double calc_tmp = calcCoincidentProbability(loc_estimateRoleMap);

				fWerewolfCheck = estimateWerewolfCheck(loc_estimateRoleMap);

				if (calc_tmp > maxProbability && !fWerewolfCheck) {
					ret_estimateRoleMap = loc_estimateRoleMap;
				}
			}

			previousEntry = entry;
		}

		return ret_estimateRoleMap;
	}


	public static Map<Agent15Role, Agent> numPlayer15Estimator(List<Agent> agentsList, GameInfo gameInfo) {
		Map<Agent15Role, Agent> estimateRoleMap = new HashMap<Agent15Role, Agent>();

		for (Agent15Role role : Agent15Role.values()) {
			estimateRoleMap.put(role, null);
		}
		estimateRoleMap.replace(role2emun(gameInfo.getRole(), true), gameInfo.getAgent());

		estimateRoleMap = dfs(estimateRoleMap, agentsList);

		return estimateRoleMap;
	}


	private static Agent getHighestProbabilityAgent(List<Agent> agentsList, Role role) {
		Agent ret = null;
		double max = 0.0;

		for (Agent agent : agentsList) {
			double tmp = 0.0;
			tmp = probabilityMap.get(agent).get(role);

			if (max < tmp) {
				max = tmp;
				ret = agent;
			}
		}

		return ret;
	}


	private static List<Agent> getNumProbabilityAgents(List<Agent> agentsList, Role role, int num) {
		List<Agent> retList = new ArrayList<Agent>();
		Map<Agent,Double> tmpMap = new HashMap<Agent, Double>();
		int count = 0;

		for (Agent agent : agentsList) {
			double tmp = 0.0;
			tmp = probabilityMap.get(agent).get(role);

			tmpMap.put(agent, tmp);
		}

		List<Double> tmpList = new ArrayList<Double>(tmpMap.values());
		List<Agent> tmpList2 = new ArrayList<Agent>();
		Collections.sort(tmpList,Comparator.reverseOrder());

		for (int i=0; i<tmpList.size(); i++) {
			if (count >= num) {
				break;
			}
			for (Map.Entry<Agent,Double> entry : tmpMap.entrySet()) {
				if (tmpList.get(i)==entry.getValue() && !tmpList2.contains(entry.getKey())) {
					retList.add(entry.getKey());
					tmpList2.add(entry.getKey());
					count++;
					break;
				}
			}
		}




		return retList;
	}


	public static List<Agent> getEstimateWerewolfs(List<Agent> agentsList) {
		List<Agent> retList = new ArrayList<Agent>(getNumProbabilityAgents(agentsList, Role.WEREWOLF, 3));
		return retList;
	}


	public static List<Agent> getEstimateVillagers(List<Agent> agentsList) {
		List<Agent> retList = new ArrayList<Agent>(getNumProbabilityAgents(agentsList, Role.VILLAGER, 8));
		return retList;
	}


	public static Agent getEstimateWerewolf(List<Agent> agentsList) {
		Agent ret = getHighestProbabilityAgent(agentsList, Role.WEREWOLF);
		return ret;
	}


	public static Agent getEstimateVillager(List<Agent> agentsList) {
		Agent ret = getHighestProbabilityAgent(agentsList, Role.VILLAGER);
		return ret;
	}


	public static Agent getEstimateSeer(List<Agent> agentsList) {
		Agent ret = getHighestProbabilityAgent(agentsList, Role.SEER);
		return ret;
	}


	public static Agent getEstimatePossessed(List<Agent> agentsList) {
		Agent ret = getHighestProbabilityAgent(agentsList, Role.POSSESSED);
		return ret;
	}


	public static Agent getEstimateMedium(List<Agent> agentsList) {
		Agent ret = getHighestProbabilityAgent(agentsList, Role.MEDIUM);
		return ret;
	}


	public static Agent getEstimateBodyguard(List<Agent> agentsList) {
		Agent ret = getHighestProbabilityAgent(agentsList, Role.BODYGUARD);
		return ret;
	}

}
