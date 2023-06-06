package com.gmail.toooo1718tyan.MetaStrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Team;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.util.Counter;


public final class WinRateCalc {
	private static Map<Agent, Counter<Role>> winCounterMap = new HashMap<Agent, Counter<Role>>();
	private static Map<Agent, Counter<Role>> roleCounterMap = new HashMap<Agent, Counter<Role>>();
	private static int updateNum;


	public static void updateWinRate(GameInfo gameInfo) {
		updateNum++;
		// 勝利チーム
		Team win = Team.VILLAGER;
		for(Agent agent : gameInfo.getAliveAgentList()) {
			if(gameInfo.getRoleMap().get(agent) == Role.WEREWOLF) {
				win = Team.WEREWOLF;
			}
		}
		// 全エージェントの情報を更新
		for(Agent agent : gameInfo.getAgentList()) {
			// 自分は無視
			if(agent == gameInfo.getAgent()) {
				continue;
			}
			// Mapにkeyがなければ追加
			if(!winCounterMap.containsKey(agent)) {
				winCounterMap.put(agent, new Counter<Role>());
			}
			if(!roleCounterMap.containsKey(agent)) {
				roleCounterMap.put(agent, new Counter<Role>());
			}
			// agentの役職を取得
			Role role = gameInfo.getRoleMap().get(agent);
			// roleを担当した回数を更新
			roleCounterMap.get(agent).add(role);
			// 所属チームが勝利勝利していたら，勝利回数Mapも更新
			if(role.getTeam() == win) {
				winCounterMap.get(agent).add(role);
			}
		}
	}


	public static Map<Agent, Integer> getTotalWinMap() {
		Map<Agent, Integer> tmp = new HashMap<Agent, Integer>() {{
			for(Agent agent : winCounterMap.keySet()) {
				put(agent, winCounterMap.get(agent).getTotalCount());
			}
		}};
		return tmp;
	}


	public static List<Agent> getMostWinners() {
		Map<Agent, Integer> totalWinMap = getTotalWinMap();
		int winN = -1;
		List<Agent> winners = new ArrayList<Agent>();
		for(Agent agent : totalWinMap.keySet()) {
			if(winN < totalWinMap.get(agent)) {
				winners.clear();
				winN = totalWinMap.get(agent);
				winners.add(agent);
			} else if (winN == totalWinMap.get(agent)) {
				winners.add(agent);
			}
		}
		return winners;
	}


	public static List<Agent> getMostWinners(List<Agent> pickupAgents) {
		Map<Agent, Integer> totalWinMap = getTotalWinMap();
		int winN = -1;
		List<Agent> winners = new ArrayList<Agent>();
		for(Agent agent : totalWinMap.keySet()) {
			if(!pickupAgents.contains(agent)) {
				continue;
			}
			if(winN < totalWinMap.get(agent)) {
				winners.clear();
				winN = totalWinMap.get(agent);
				winners.add(agent);
			} else if (winN == totalWinMap.get(agent)) {
				winners.add(agent);
			}
		}
		return winners;
	}


	public static List<Agent> getMostWinners(Role targetRole) {
		List<Agent> winners = new ArrayList<Agent>();
		double higher = -1.0;
		TreeSet<Agent> nameSet = new TreeSet<Agent>() {{
			addAll(roleCounterMap.keySet());
			addAll(winCounterMap.keySet());
		}};
		for(Agent agent : nameSet) {
			double win = winCounterMap.get(agent).get(targetRole);
			double cnt = roleCounterMap.get(agent).get(targetRole);
			if(higher < (win/cnt)) {
				winners.clear();
				higher = win/cnt;
				winners.add(agent);
			} else if (higher == (win/cnt)) {
				winners.add(agent);
			}
		}
		return winners;
	}


	public static int getUpdateNum() {
		return updateNum;
	}
}