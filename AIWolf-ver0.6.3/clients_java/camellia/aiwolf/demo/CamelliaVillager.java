package camellia.aiwolf.demo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.ContentBuilder;
import org.aiwolf.client.lib.DivinationContentBuilder;
import org.aiwolf.client.lib.EstimateContentBuilder;
import org.aiwolf.client.lib.Operator;
import org.aiwolf.client.lib.RequestContentBuilder;
import org.aiwolf.client.lib.Topic;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;
import org.aiwolf.sample.player.SampleBasePlayer;

public class CamelliaVillager extends SampleBasePlayer {

	Agent me;
	Agent killedAgent;
	Agent voteCandidate;
	GameInfo currentGameInfo;
	List<Agent> agentList = new ArrayList<>();
	List<Agent> whiteList = new ArrayList<>();
	List<Agent> blackList  = new ArrayList<>();
	List<Agent> grayList;
	List<Agent> killedAgents = new ArrayList<>();
	List<Judge> divinationList = new ArrayList<>();
	List<Judge> identList = new ArrayList<>();
	int talkListHead;
	Map<Agent, Role> comingoutMap = new HashMap<>();
	boolean divineWhiteMe;
	boolean requestDivineFlag;
	boolean estimateFlag;
	boolean requestVoteFlag;
	int day;
	double dcount;
	int icount;
	Map<Agent, Double> degreeOfwolf = new HashMap<>(); //"degree of wolf" of every Agent
	Map<Agent, Integer> countTalk = new HashMap<>(); //count times every Agent talked
	Map<Agent, Map<Agent, Integer>> countToTalk = new HashMap<>(); //map<talker, map<target, count>>:count the agent's estimate to other agent.

	@Override
	public void dayStart() {
		// 今日の発話リストですでに読み込んだ内容を覚えておく変数
		talkListHead = 0;
		day++;
		requestDivineFlag = false;
		estimateFlag = false;
		requestVoteFlag = false;
		// 前日に殺されたエージェントがいる場合
		if (!currentGameInfo.getLastDeadAgentList().isEmpty()) {
			// killedAgentに殺されたエージェントを代入
			killedAgent = currentGameInfo.getLastDeadAgentList().get(0);
			// grayListから殺されたエージェントを除外
			grayList.remove(killedAgent);
			// killedAgentsリストに存在しない場合
			if(!killedAgents.contains(killedAgent)) {
				// killedAgentsリストに代入
				killedAgents.add(killedAgent);
			}
		}
		// 2日目で占い師が一人の場合真目で見る(占い対抗が出ていない為)
		if (day == 1 && comingoutMap.containsValue(Role.SEER)) {
			for(Agent key : comingoutMap.keySet()) {
				if(comingoutMap.get(key) == Role.SEER) {
					whiteList.add(key);
				}
			}
		}
		// 3日目で霊媒師が一人の場合真目で見る(霊媒対抗が出ていない為)
		if (agentList.size() == 15 && day == 2 && comingoutMap.containsValue(Role.MEDIUM)) {
			for(Agent key : comingoutMap.keySet()) {
				if(comingoutMap.get(key) == Role.MEDIUM) {
					whiteList.add(key);
				}
			}
		}
	}

	@Override
	public void finish() {

	}

	@Override
	public String getName() {
		return "CamelliaVillager";
	}

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		// フィールドの初期化
		me = gameInfo.getAgent();
		voteCandidate = null;
		grayList = new ArrayList<>(gameInfo.getAgentList());
		grayList.remove(me);
		agentList = gameInfo.getAgentList();
		whiteList.clear();
		blackList.clear();
		killedAgents.clear();
		divinationList.clear();
		identList.clear();
		comingoutMap.clear();
		day = -1;
		divineWhiteMe = false;
	}

	@Override
	public String talk() {
		// 占いCOが出ていない場合リクエスト
		if(!comingoutMap.containsValue(Role.SEER)) {
			ContentBuilder coSeer = new RequestContentBuilder(Content.ANY, new Content(new ComingoutContentBuilder(Content.ANY, Role.SEER)));

			return new Content(coSeer).getText();
		}

		// 占いCOに対し，次に占ってほしい人をブラックリストからランダムに選択しリクエスト
		if(!requestDivineFlag && comingoutMap.containsValue(Role.SEER) && !blackList.isEmpty()) {
			for(Agent key : comingoutMap.keySet()) {
				if(comingoutMap.get(key) == Role.SEER) {
					ContentBuilder requestDivine = new RequestContentBuilder(key, new Content(new DivinationContentBuilder(randomSelect(blackList))));

					return new Content(requestDivine).getText();
				}
			}
		}

		// 15人村かつ二日目で霊媒COが出ていない場合リクエスト
		if (agentList.size() == 15 && day == 1 && !comingoutMap.containsValue(Role.MEDIUM)) {
			ContentBuilder coMedium = new RequestContentBuilder(Content.ANY, new Content(new ComingoutContentBuilder(Content.ANY, Role.MEDIUM)));

			return new Content(coMedium).getText();
		}

		// 現状の推測を提示
		if (!blackList.isEmpty()) {
			for(Agent x : blackList) {
				ContentBuilder estimate = new EstimateContentBuilder(x, Role.WEREWOLF);

				return new Content(estimate).getText();
			}
		}

		// 投票リクエスト(保留)
//		if (requestVoteFlag && !blackList.isEmpty()) {
//			voteCandidate = randomSelect(blackList);
//			ContentBuilder requestVote = new RequestContentBuilder(null, new Content(new VoteContentBuilder(voteCandidate)));
//
//			return new Content(requestVote).getText();
//		}

		// 自分に投票リクエストされている場合，disagreeする

		return Content.SKIP.getText();
	}

	@Override
	public void update(GameInfo gameInfo) {
		currentGameInfo = gameInfo;

		//dayの更新
		if (currentGameInfo.getDay() == day + 1) {
			day = currentGameInfo.getDay();
		}

		// GameInfo.talkListからカミングアウト・占い報告・霊媒報告を抽出
		for (int i = talkListHead; i < currentGameInfo.getTalkList().size(); i++) {
			Talk talk = currentGameInfo.getTalkList().get(i);
			Agent talker = talk.getAgent();
			if (talker == me) {	// 発言者が自分であれば除く
				continue;
			}
			Content content = new Content(talk.getText());	// 発話をparse

			switch (content.getTopic()) {

			case COMINGOUT:
				// カミングアウト発話の処理
				comingoutMap.put(talker, content.getRole());
				break;
			case DIVINED:
				// 占い結果報告発話の処理
				divinationList.add(new Judge(day, talker, content.getTarget(), content.getResult()));
				break;
				// 霊媒結果報告発話の処理
			case IDENTIFIED:
				identList.add(new Judge(day, talker, content.getTarget(), content.getResult()));
				break;
				// リクエスト報告発話の処理
			case OPERATOR:
				if(content.getOperator() == Operator.REQUEST) {
					for(Content c : content.getContentList()) {
						if(c.getTopic() == Topic.VOTE) {
							Agent target = c.getTarget();
							// 話者が白だったら対象をblackListへ
							if(whiteList.contains(talker)) {
								blackList.add(c.getTarget());
							}
							// 自分が白と占われているのに，投票対象を自分にしている場合，対象をblackListへ
							if(divineWhiteMe && target == me) {
								blackList.add(talker);
							}
						}
					}
				}
				break;
			case ESTIMATE:
				//自分のことを人狼か狂人と推測している場合
				if(content.getTarget() == me){
					if(content.getRole() == Role.WEREWOLF || content.getRole() == Role.POSSESSED){
						dcount = degreeOfwolf.containsKey(talker) ? degreeOfwolf.get(talker) : 0;
						degreeOfwolf.put(talker, dcount + 0.3);
					}
				} else if(content.getRole() == Role.VILLAGER) {
					Map<Agent,Integer> tmpMap = countToTalk.containsKey(talker) ? countToTalk.get(talker) : new HashMap<>();
					icount = tmpMap.containsKey(content.getTarget()) ? tmpMap.get(content.getTarget()) : 0;
					tmpMap.put(content.getTarget(), icount + 1);
					countToTalk.put(talker,tmpMap);
				}
				break;
			default:
				break;
			}
		}
		Map.Entry<Agent, Integer> minEntry = null;
		for(Map.Entry<Agent, Integer> entry : countTalk.entrySet()) {
			if(minEntry == null || entry.getValue().compareTo(minEntry.getValue()) < 0) {
				minEntry = entry;
			}
		}
		estimateDivined();
		estimateIdentified();
		talkListHead = currentGameInfo.getTalkList().size();
	}

	// 占い結果から推定(Sampleから一部流用)
	public void estimateDivined() {
		// divinationListをイテレータ化
//		Iterator<Judge> iterator = divinationList.iterator();
		Iterator<Judge> iterator = divinationList.iterator();
		// イテレータの最後まで参照
		while(iterator.hasNext()) {
			// 占い結果の取り出し
			Judge divination = iterator.next();
			Agent he = divination.getAgent();
			Agent target = divination.getTarget();
			Species result = divination.getResult();
			// もし占い師が生きている状態で，ブラックリストに入っておらず，占い結果で白と発言したら
			if (isAlive(he) && !blackList.contains(he) && result == Species.HUMAN) {
				// 対象が自分だった場合話者の人狼度-0.5
				if (target == me) {
					divineWhiteMe = true;
					degreeOfwolf.put(he, dcount - 0.5);
				}
			}
			// もし占い師が生きている状態で，ブラックリストに入っておらず，占い結果で黒と発言したら
			if (isAlive(he) && !blackList.contains(he) && result != Species.HUMAN) {
				// もし対象が自分である場合，自分は村人であるため，嘘をついた占い師をブラックリストに登録
				if (target == me) {
					blackList.add(he);
				// もし，殺されたターゲットを対象として占っていた場合，占えないはずなので，占い師をブラックリストに登録
				} else if (isKilled(target)) {
					blackList.add(he);
				// 占い結果を元に，対象をblackListに追加
				} else {
					blackList.add(target);
				}
			}
			// もし真目占い師が，白と言った場合
			if (isAlive(he) && whiteList.contains(he) && result == Species.HUMAN) {
				// 対象をwhiteListへ
				whiteList.add(target);
			}
			// もし真目占い師が，黒と言った場合
			if (isAlive(he) && whiteList.contains(he) && result != Species.HUMAN) {
				// 対象をblackListへ
				blackList.add(target);
			}
		}
	}

	// 霊媒結果から推定
		public void estimateIdentified() {
			// 15人村の場合
			if(agentList.size() == 15) {
				// identListをイテレータ化
				Iterator<Judge> iterator = identList.iterator();
				// イテレータの最後まで参照
				while(iterator.hasNext()) {
					// 占い結果の取り出し
					Judge identified = (Judge)iterator.next();
					Agent he = identified.getAgent();
					// もし霊媒師が生きている状態で，ブラックリストに入っていない場合
					if (isAlive(he) && !blackList.contains(he)) {
						Agent target = identified.getTarget();
						// もし対象が自分である場合，自分は生存しているため，嘘をついた霊媒師をブラックリストに登録
						if (target == me) {
							blackList.add(he);
						// もし，生きているターゲットを対象として占っていた場合，占えないはずなので，霊媒師をブラックリストに登録
						} else if (isAlive(target)) {
							blackList.add(he);
						}
					}
				}
			}
		}

	@Override
	public Agent vote() {
		// 候補者リスト
		List<Agent> candidates = new ArrayList<>();
		// 生きている人狼を候補者リストに加える
		for (Agent agent : blackList) {
			if (isAlive(agent)) {		// 生きているかどうかを判定
				candidates.add(agent);	// 候補リストへの追加
			}
		}

		// 候補者がいない場合は生きている灰色のプレイヤーを候補者リストに加える
		if (candidates.isEmpty()) {
			for (Agent agent : grayList) {
				if (isAlive(agent)) {
					candidates.add(agent);
				}
			}
		}

		// VoteRequestしている場合，その対象へ投票する
//		if(requestVoteFlag) {
//			return voteCandidate;
//		}

		// 候補者がいない場合はnullを返す (自分以外の生存プレイヤーからランダム)
		if (candidates.isEmpty()) {
			return null;
		}

		return randomSelect(candidates);
	}

	// 引数のエージェントがまだ生きているか
	boolean isAlive(Agent agent) {
		return currentGameInfo.getAliveAgentList().contains(agent);
	}

	// 引数のエージェントが殺されたエージェントか
	boolean isKilled(Agent agent) {
		return killedAgents.contains(agent);
	}

	// 引数のリストからランダムに1つ要素を取り出す
	<T> T randomSelect(List<T> list) {
		if (list.isEmpty()) {
			return null;
		} else {
			return list.get((int) (Math.random() * list.size()));
		}
	}

}
