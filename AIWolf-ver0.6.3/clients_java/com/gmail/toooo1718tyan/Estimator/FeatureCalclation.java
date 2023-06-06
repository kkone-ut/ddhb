package com.gmail.toooo1718tyan.Estimator;

import java.util.HashMap;
import java.util.Map;

import org.aiwolf.client.lib.Content;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Vote;
import org.aiwolf.common.net.GameInfo;

public final class FeatureCalclation {
	// 日
	private static int day;
	// ターン
	private static int turn;
	// 生存しているか否か
	private static Map<Agent, Integer> alive = new HashMap<Agent, Integer>();
	// 死因
	private static Map<Agent, Integer> death = new HashMap<Agent, Integer>();
	// 現在の占いCO数
	private static int numSeerCO;
	// 現在の霊媒CO数
	private static int numMediumCO;
	// 生存している占いCOしたエージェント
	private static int numAliveSeerCO;
	// 生存してい霊媒COしたエージェント
	private static int numAliveMediumCO;
	// 人間判定を受けた数
	private static Map<Agent, Integer> receiveHumanDivine = new HashMap<Agent, Integer>();
	// 人狼判定を受けた数
	private static Map<Agent, Integer> receiveWolfDivine = new HashMap<Agent, Integer>();
	// 人間判定した数
	private static Map<Agent, Integer> humanDivine = new HashMap<Agent, Integer>();
	// 人狼判定した数
	private static Map<Agent, Integer> wolfDivine = new HashMap<Agent, Integer>();
	// 何番目に占いCOしたか
	private static Map<Agent, Integer> seerCoNumber = new HashMap<Agent, Integer>();
	// 何番目に霊媒COしたか
	private static Map<Agent, Integer> mediumCoNumber = new HashMap<Agent, Integer>();
	// その日に何回投票発言したか
	private static Map<Agent, Integer> voteCount = new HashMap<Agent, Integer>();
	// このゲームの投票先の相違回数
	private static Map<Agent, Integer> differentVote = new HashMap<Agent, Integer>();
	// 人狼Estimate発言した回数
	private static Map<Agent, Integer> wolfEstimate = new HashMap<Agent, Integer>();


	// 誰が投票してきたかを保持<talker, target>
	private static Map<Agent, Agent> playerToVotePlayer = new HashMap<Agent, Agent>();
	// 現在何人COしているか
	private static int seerCoCount;
	private static int mediumCoCount;
	private static Map<Agent, Role> comingoutMap = new HashMap<>();


	// ini
	public static void initFeature(GameInfo gameInfo) {
		day = 0;
		turn = 0;
		numSeerCO = 0;
		numMediumCO = 0;
		numAliveSeerCO = 0;
		numAliveMediumCO = 0;
		alive.clear();
		death.clear();
		receiveHumanDivine.clear();
		receiveWolfDivine.clear();
		humanDivine.clear();
		wolfDivine.clear();
		seerCoNumber.clear();
		mediumCoNumber.clear();
		voteCount.clear();
		differentVote.clear();
		wolfEstimate.clear();

		playerToVotePlayer.clear();
		seerCoCount = 1;
		mediumCoCount = 1;
		comingoutMap.clear();

		// 全エージェントの情報を更新
		for(Agent agent : gameInfo.getAgentList()) {
			// map初期化
			alive.put(agent,1);
			death.put(agent, 0);
			receiveHumanDivine.put(agent, 0);
			receiveWolfDivine.put(agent, 0);
			humanDivine.put(agent, 0);
			wolfDivine.put(agent, 0);
			seerCoNumber.put(agent, -1);
			mediumCoNumber.put(agent, -1);
			voteCount.put(agent, 0);
			differentVote.put(agent, 0);
			wolfEstimate.put(agent, 0);
		}
	}

	// day ini
	public static void initDayFeature(GameInfo gameInfo) {
		humanDivine.clear();
		wolfDivine.clear();
		voteCount.clear();
		wolfEstimate.clear();

		playerToVotePlayer.clear();
		// 全エージェントの情報を更新
		for(Agent agent : gameInfo.getAgentList()) {
			// map初期化
			humanDivine.put(agent, 0);
			wolfDivine.put(agent, 0);
			voteCount.put(agent, 0);
			wolfEstimate.put(agent, 0);
		}
	}

	// day update
	public static void updateDay(GameInfo gameInfo) {
		day = gameInfo.getDay();
		// 生死情報の更新
		for(Agent agent : gameInfo.getAgentList()) {
			if(gameInfo.getAliveAgentList().contains(agent)) {
				alive.put(agent, 0);
			} else {
				alive.put(agent, 1);
			}
		}

		// 生存CO数
		numAliveSeerCO = 0;
		numAliveMediumCO = 0;
		for (Agent agent : gameInfo.getAliveAgentList()) {
			if (comingoutMap.get(agent) == Role.SEER)
				numAliveSeerCO++;
			if(comingoutMap.get(agent) == Role.MEDIUM)
				numAliveMediumCO++;
		}

		// 襲撃・追放・投票先
		if(day > 1) {
			// 死因の更新
			for(Agent agent : gameInfo.getLastDeadAgentList()) {
				if(gameInfo.getExecutedAgent() == agent) {
					death.put(agent, 1);
				} else {
					death.put(agent, -1);
				}
			}
			// 投票先の相違をカウント
			for(Vote vote : gameInfo.getVoteList()) {
				// 自分は無視
				if(vote.getAgent() == gameInfo.getAgent()) {
					continue;
				}
				if(vote.getTarget() != playerToVotePlayer.get(vote.getAgent())) {
					differentVote.put(vote.getAgent(), differentVote.get(vote.getAgent()) + 1);
				}
			}
		}
	}

	// turn update
	public static void updateTurn() {
		turn++;
	}

	// feature update
	public static void updateFeature(Content content, Agent talker) {
		switch (content.getTopic()) {
			case COMINGOUT:
				if (content.getRole() == Role.SEER) {
					numSeerCO++;
					seerCoNumber.put(talker, seerCoCount);
					seerCoCount++;
				}
				if(content.getRole() == Role.MEDIUM) {
					numMediumCO++;
					mediumCoNumber.put(talker, mediumCoCount);
					mediumCoCount++;
				}
				comingoutMap.put(content.getTarget(), content.getRole());
				break;
			case VOTE:
				voteCount.put(talker, voteCount.get(talker) + 1);
				playerToVotePlayer.put(talker, content.getTarget());
				break;
			case DIVINED:
				if(comingoutMap.get(content.getTarget()) != Role.SEER)
					break;
				if (content.getResult() == Species.WEREWOLF) {
					wolfDivine.put(talker, wolfDivine.get(talker) + 1);
					receiveWolfDivine.put(content.getTarget(), receiveWolfDivine.get(content.getTarget()) + 1);
				}
				if (content.getResult() == Species.HUMAN) {
					humanDivine.put(talker, humanDivine.get(talker) + 1);
					receiveHumanDivine.put(content.getTarget(), receiveHumanDivine.get(content.getTarget()) + 1);
				}
				break;
			case ESTIMATE:
				if(content.getRole() == Role.WEREWOLF) {
					wolfEstimate.put(content.getTarget(), wolfEstimate.get(content.getTarget()) + 1);
				}
				break;
			case OPERATOR:
				parseOperator(content, talker);
			default:
				break;
		}
	}

	// 演算子文を解析する
	private static void parseOperator(Content content, Agent talker) {
		switch (content.getOperator()) {
		case AND:
		case OR:
		case XOR:
			for (Content c : content.getContentList()) {
				updateFeature(c, talker);
			}
			break;
		default:
			break;
		}
	}


	public static double getFeatureDay() {
		return (double) day;
	}

	public static double getFeatureTrun() {
		return (double) turn;
	}

	public static double getFeatureAlive(Agent agent) {
		return (double) alive.get(agent);
	}

	public static double getFeatureDeath(Agent agent) {
		return (double) death.get(agent);
	}

	public static double getFeatureNumSeerCO() {
		return (double) numSeerCO;
	}

	public static double getFeatureNumMediumCO() {
		return (double) numMediumCO;
	}

	public static double getFuatureNumAliveSeerCO() {
		return (double) numAliveSeerCO;
	}

	public static double getFuatureNumAliveMediumCO() {
		return (double) numAliveMediumCO;
	}

	public static double getFeatureReceiveHumanDivine(Agent agent) {
		return (double) receiveHumanDivine.get(agent);
	}

	public static double getFeatureReceiveWolfDivine(Agent agent) {
		return (double) receiveWolfDivine.get(agent);
	}

	public static double getFeatureHumanDivine(Agent agent) {
		return (double) humanDivine.get(agent);
	}

	public static double getFeatureWolfDivine(Agent agent) {
		return (double) wolfDivine.get(agent);
	}

	public static double getFeatureSeerCoNumber(Agent agent) {
		return (double) seerCoNumber.get(agent);
	}

	public static double getFeatureMediumCoNumber(Agent agent) {
		return (double) mediumCoNumber.get(agent);
	}

	public static double getFeatureVoteCount (Agent agent) {
		return (double) voteCount.get(agent);
	}

	public static double getFeatureDifferentVote(Agent agent) {
		return (double) differentVote.get(agent);
	}

	public static double getFeatureWolfEstimate(Agent agent) {
		return (double) wolfEstimate.get(agent);
	}

}
