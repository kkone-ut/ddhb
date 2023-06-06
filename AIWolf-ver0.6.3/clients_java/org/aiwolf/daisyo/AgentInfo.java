package org.aiwolf.daisyo;

import org.aiwolf.common.data.Role;

public class AgentInfo {
	boolean isAlive;
	int idx;
	Role role;
	Role corole = null;
	int state;
	int votefor;
	int nvotedby;
	int wincnt;
	AgentInfo() {
		state = -1;
		isAlive = true;
		wincnt = 0;
	}
}
