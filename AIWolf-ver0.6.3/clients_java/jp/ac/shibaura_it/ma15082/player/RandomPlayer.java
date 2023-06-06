package jp.ac.shibaura_it.ma15082.player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.TalkType;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Player;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

public class RandomPlayer implements Player {
	Map<Integer, GameInfo> gameInfoMap = new HashMap<Integer, GameInfo>();

	int day;

	Agent me;

	Role myRole;

	GameSetting gameSetting;

	List<Agent> aliveAgent;

	@Override
	public Agent attack() {
		Collections.shuffle(aliveAgent);
		return aliveAgent.get(0);
	}

	@Override
	public void dayStart() {
		this.aliveAgent = this.getLatestDayGameInfo().getAliveAgentList();
		this.aliveAgent.remove(this.getLatestDayGameInfo().getAgent());
	}

	@Override
	public Agent divine() {
		Collections.shuffle(aliveAgent);
		return aliveAgent.get(0);
	}

	@Override
	public void finish() {

	}

	@Override
	public String getName() {
		return "RandomAgent";
	}

	@Override
	public Agent guard() {
		Collections.shuffle(aliveAgent);
		return aliveAgent.get(0);
	}

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		gameInfoMap.clear();
		this.gameSetting = gameSetting;
		day = gameInfo.getDay();
		gameInfoMap.put(day, gameInfo);
		myRole = gameInfo.getRole();
		me = gameInfo.getAgent();
		return;

	}

	public GameInfo getLatestDayGameInfo() {
		return gameInfoMap.get(day);
	}

	@Override
	public String talk() {
		Random r = new Random();

		TalkType[] talkTypes = TalkType.values();
		List<Agent> allAgent = this.getLatestDayGameInfo().getAgentList();
		Collections.shuffle(allAgent);
		Species[] species = Species.values();
		Role[] roles = Role.values();

		TalkFactory factory = TalkFactory.getInstance();
		Content ret = factory.over();
		switch (r.nextInt(10)) {

		case 0:
			ret = factory.agree(talkTypes[r.nextInt(talkTypes.length)], r.nextInt(100), r.nextInt(100));
		case 1:
			ret = factory.comingout(allAgent.get(0), roles[r.nextInt(roles.length)]);
		case 2:
			ret = factory.disagree(talkTypes[r.nextInt(talkTypes.length)], r.nextInt(100), r.nextInt(100));
		case 3:
			ret = factory.divined(allAgent.get(0), species[r.nextInt(species.length)]);
		case 4:
			ret = factory.estimate(allAgent.get(0), roles[r.nextInt(roles.length)]);
		case 5:
			ret = factory.guarded(allAgent.get(0));
		case 6:
			ret = factory.inquested(allAgent.get(0), species[r.nextInt(species.length)]);
		case 7:
			ret = factory.over();
		case 8:
			ret = factory.skip();
		case 9:
			ret = factory.vote(allAgent.get(0));
		}
		return ret.getText();
	}

	@Override
	public void update(GameInfo gameInfo) {
		day = gameInfo.getDay();

		gameInfoMap.put(day, gameInfo);
	}

	@Override
	public Agent vote() {
		Collections.shuffle(aliveAgent);
		return aliveAgent.get(0);
	}

	@Override
	public String whisper() {
		Random r = new Random();

		TalkType[] talkTypes = TalkType.values();
		List<Agent> allAgent = this.getLatestDayGameInfo().getAgentList();
		Collections.shuffle(allAgent);
		Species[] species = Species.values();
		Role[] roles = Role.values();

		TalkFactory factory = TalkFactory.getInstance();
		Content ret = factory.over();

		switch (r.nextInt(10)) {

		case 0:
			ret = factory.agree(talkTypes[r.nextInt(talkTypes.length)], r.nextInt(100), r.nextInt(100));
		case 1:
			ret = factory.comingout(allAgent.get(0), roles[r.nextInt(roles.length)]);
		case 2:
			ret = factory.disagree(talkTypes[r.nextInt(talkTypes.length)], r.nextInt(100), r.nextInt(100));
		case 3:
			ret = factory.divined(allAgent.get(0), species[r.nextInt(species.length)]);
		case 4:
			ret = factory.estimate(allAgent.get(0), roles[r.nextInt(roles.length)]);
		case 5:
			ret = factory.guarded(allAgent.get(0));
		case 6:
			ret = factory.inquested(allAgent.get(0), species[r.nextInt(species.length)]);
		case 7:
			ret = factory.over();
		case 8:
			ret = factory.skip();
		case 9:
			ret = factory.vote(allAgent.get(0));
		case 10:
			ret = factory.attack(allAgent.get(0));
		}

		return ret.getText();
	}

}
