package org.aiwolf.daisyo;

public class GameState {
	int gameCount = 0;
	int day = 0;
	int turn = 0;
	AgentStatus[] agents;
	int numAgents;
	int me;
	int w;

	GameState(int _numAgents) {
		numAgents = _numAgents;
		agents = new AgentStatus[numAgents];
		for (int i = 0; i < numAgents; ++i) {
			agents[i] = new AgentStatus();
		}
	}

	void game_init(int numAgents) {
		day = 0;
		turn = 0;
		for (int i = 0; i < numAgents; ++i) {
			agents[i].game_init();
		}
	}

	void day_init(int numAgents) {
		turn = 0;
		for (int i = 0; i < numAgents; ++i) {
			agents[i].day_init();
		}
	}

	int cnt_vote(int agent) {
		int res = 0;
		for (int i = 0; i < numAgents; ++i) {
			if (agents[i].will_vote == agent) {
				res++;
			}
		}
		return res;
	}
	
}

