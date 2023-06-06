package com.gmail.toooo1718tyan.Player15;

import java.util.ArrayList;
import java.util.List;

import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.VoteContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

import com.gmail.toooo1718tyan.Estimator.LogisticRegression15;
import com.gmail.toooo1718tyan.MetaStrategy.MostVoteAgent;

public class Tomato15Villager extends Tomato15BasePlayer {

	boolean isEstimateTalk;
	int count;
	boolean isCameout;
	boolean fakePoss;

	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		isCameout = false;
		fakePoss = false;
		super.initialize(gameInfo, gameSetting);
	}

	public void dayStart() {
		super.dayStart();
		isEstimateTalk = false;
		count = 0;
	}

	protected void chooseVoteCandidate() {
		List<Agent> candidates = new ArrayList<>();
		Agent mostVoteAgent = null;
		Agent nearWerewolf = null;

		// 黒を出されたエージェントを優先的に吊る
		List<Agent> blackCandidates = new ArrayList<>();
		for (Agent agent : aliveOthers) {
			if (blackDivineList.contains(agent) || werewolves.contains(agent)) {
				blackCandidates.add(agent);
				candidates.add(agent);
			}
			if(grayAgetnList.contains(agent)) {
				candidates.add(agent);
			}
		}

		// もっとも投票されているエージェント
		mostVoteAgent = randomSelect(MostVoteAgent.getMostVoteTerget(aliveOthers));

		// 狼ぽいエージェント
		nearWerewolf = LogisticRegression15.getEstimateWerewolf(candidates);

		if(day >= 4) {
			// 役職ローラー
			for (Agent agent : aliveOthers) {
				if (comingoutMap.get(agent) == Role.SEER) {
					candidates.add(agent);
				} else if(comingoutMap.get(agent) == Role.MEDIUM){
					candidates.add(agent);
				} else if(comingoutMap.get(agent) == Role.WEREWOLF){
					candidates.add(agent);
				}
				if (myRole == Role.SEER) {
					if (fakeSeer.contains(agent)) {
						candidates.add(agent);
					}
				}
			}
		}

		//投票先設定
		if(myRole == Role.BODYGUARD){
			candidates.remove(successGurdeAgent);
		}
		// 人間ポイ人を避ける
		for(Agent agent : humans) {
			candidates.remove(agent);
		}

		if(day <= 2) {
			voteCandidate = randomSelect(MostVoteAgent.getMostVoteTerget(candidates));
			if(voteCandidate == null)
				voteCandidate = mostVoteAgent;
		} else {
			voteCandidate = nearWerewolf;
			if (voteCandidate == null)
				voteCandidate = estimateWerewolfAgent;
		}

		// 候補が残らなかったら全員から
		if (candidates.isEmpty()) {
			candidates = new ArrayList<>(aliveOthers);
		}
		if (voteCandidate == null) {
			voteCandidate = randomSelect(candidates);
		}

		// 投票先を宣言
		if (voteCandidate != null && voteCandidate != declaredVoteCandidate) {
			talkQueue.offer(new Content(new VoteContentBuilder(voteCandidate)));
			declaredVoteCandidate = voteCandidate;
		}
	}

	public String talk() {
		if(myRole == Role.VILLAGER || myRole == Role.BODYGUARD) {
			if(!isCameout) {
//				talkQueue.offer(new Content(new ComingoutContentBuilder(me, Role.VILLAGER)));
				isCameout = true;
			}
		}

		if (talkQueue.isEmpty())
			chooseVoteCandidate();

		return super.talk();
	}

	public String whisper() {
		throw new UnsupportedOperationException();
	}

	public Agent attack() {
		throw new UnsupportedOperationException();
	}

	public Agent divine() {
		throw new UnsupportedOperationException();
	}

	public Agent guard() {
		throw new UnsupportedOperationException();
	}

}
