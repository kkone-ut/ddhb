package org.aiwolf.Basket;

import org.aiwolf.common.data.Role;

// インターフェースっぽいところなので気にしなくてよさそう
//agents[]として用いられているのはこちら(sh.gamestate.agents[]とは異なる)
public class AgentInfo {
	boolean alive;
	int index;
	Role role;
	Role COrole = null;
	int state;
	int voteFor;
	int NvotedBy;
	int wincnt;

	AgentInfo() {
		state = -1;
		alive = true;
		wincnt = 0;
	}
}
