package com.gmail.toooo1718tyan.MetaStrategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.net.GameInfo;

public final class MostVoteAgent {
	private static Map<Agent, Agent> voteTergetMap = new HashMap<Agent, Agent>();
	private static Map<Agent, Agent> voteForMe = new HashMap<Agent, Agent>();

	// day init
	public static void mostVoteAgentInit() {
		voteTergetMap.clear();
	}


	// update
	public static void updateVoteTerget(Agent talker, Agent terget, GameInfo gameInfo) {
		if(terget != gameInfo.getAgent()) {
			voteTergetMap.put(talker, terget);
			if(voteForMe.containsKey(talker)) {
				voteForMe.remove(talker);
			}
		} else {
			voteForMe.put(talker, terget);
			if(voteTergetMap.containsKey(talker)) {
				voteTergetMap.remove(talker);
			}
		}
	}


	public static List<Agent> getMostVoteTerget() {
		List<Agent> player = new ArrayList<Agent>();
		Map<Agent,Integer> voteCount = new HashMap<Agent, Integer>();
		int maxVote = 0;

		for(Agent agent : voteTergetMap.keySet()) {
			Agent terget = voteTergetMap.get(agent);
			if(voteCount.containsKey(terget)) {
				voteCount.put(terget, voteCount.get(terget) + 1);
			} else {
				voteCount.put(terget, 0);
			}
		}

		for(Agent agent : voteCount.keySet()) {
			if(maxVote < voteCount.get(agent)) {
				maxVote = voteCount.get(agent);
			}
		}

		for(Agent agent : voteCount.keySet()) {
			if(maxVote <= voteCount.get(agent)) {
				player.add(agent);
			}
		}

		return player;
	}

	public static List<Agent> getMostVoteTerget(List<Agent> pickupAgents) {
		List<Agent> player = new ArrayList<Agent>();
		Map<Agent,Integer> voteCount = new HashMap<Agent, Integer>();
		int maxVote = 0;

		for(Agent agent : voteTergetMap.keySet()) {
			Agent terget = voteTergetMap.get(agent);
			if(!pickupAgents.contains(terget))
				continue;
			if(voteCount.containsKey(terget)) {
				voteCount.put(terget, voteCount.get(terget) + 1);
			} else {
				voteCount.put(terget, 0);
			}
		}

		for(Agent agent : voteCount.keySet()) {
			if(maxVote < voteCount.get(agent)) {
				maxVote = voteCount.get(agent);
			}
		}

		for(Agent agent : voteCount.keySet()) {
			if(maxVote <= voteCount.get(agent)) {
				player.add(agent);
			}
		}

		return player;
	}


	public static boolean getMyVoteJudgment() {
		boolean f = false;
		Map<Agent,Integer> voteCount = new HashMap<Agent, Integer>();
		int maxVote = 0;

		for(Agent agent : voteTergetMap.keySet()) {
			Agent terget = voteTergetMap.get(agent);
			if(voteCount.containsKey(terget)) {
				voteCount.put(terget, voteCount.get(terget) + 1);
			} else {
				voteCount.put(terget, 0);
			}
		}

		for(Agent agent : voteCount.keySet()) {
			if(maxVote < voteCount.get(agent)) {
				maxVote = voteCount.get(agent);
			}
		}

		if(maxVote <= voteForMe.size()) {
			f = true;
		}

		return f;
	}
}
