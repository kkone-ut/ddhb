package camellia.aiwolf.demo;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.aiwolf.client.lib.AndContentBuilder;
import org.aiwolf.client.lib.BecauseContentBuilder;
import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.ContentBuilder;
import org.aiwolf.client.lib.DisagreeContentBuilder;
import org.aiwolf.client.lib.DivinationContentBuilder;
import org.aiwolf.client.lib.GuardCandidateContentBuilder;
import org.aiwolf.client.lib.IdentContentBuilder;
import org.aiwolf.client.lib.RequestContentBuilder;
import org.aiwolf.client.lib.VoteContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;
import org.aiwolf.sample.player.SampleBasePlayer;

public class CamelliaMedium extends SampleBasePlayer {

	/**自分自身の参照のための変数*/
	Agent me;
	/**最新のゲーム場面の状況を格納する変数*/
	GameInfo currentGameInfo;
	/**未報告の霊媒結果が入る待ち行列*/
	Deque<Judge> myMediumQueue = new LinkedList<>();
	/**カミングアウト済みか*/
	boolean saidCO = false;
	/**守ってもらう依頼をしたか*/
	boolean saidGuard = false;
	/**占いの指示を出したか*/
	boolean saidIns = false;
	/**会話リストの読み込んだ場所を保存*/
	int talkListHead;
	int pre_talkListHead;
	
	/********/
	List<Agent> whiteList = new ArrayList<>();
	List<Agent> blackList = new ArrayList<>();
	List<Agent> grayList;
	double dcount;
	int icount;
	Map<Agent, Double> degreeOfwolf = new HashMap<>(); //"degree of wolf" of every Agent
	Map<Agent, Role> comingoutMap = new HashMap<>();	//stored Coming out of every Agent
	Map<Agent, Integer> countTalk = new HashMap<>(); //count times every Agent talked
	Map<Agent, Map<Agent, Integer>> countToTalk = new HashMap<>(); //map<talker, map<target, count>>:count the agent's estimate to other agent.
	/********/
	
	@Override
	public void dayStart() {
		// TODO Auto-generated method stub
		
		//霊媒結果をGameInfoから取得
		Judge medium = currentGameInfo.getMediumResult();
		if(medium != null) {
			//myMediumQueueの最後尾にmediumの結果を追加
			myMediumQueue.offer(medium);
			//霊媒結果の詳細(誰を見て、その結果は？)を取り出す
			Agent target = medium.getTarget();
			Species result = medium.getResult();
			}
		//saidGuard = false;
		saidIns = false;
		//会話ログから占い結果との矛盾を見つける
		
		
		//今日の発話リストですでに読み込んだ内容を覚えておく変数
		talkListHead = 0;
		pre_talkListHead = 0;
	}

	@Override
	public void finish() {
		// TODO Auto-generated method stub

	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		//return null;
		
		return "CamelliaMedium";
	}

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		// TODO Auto-generated method stub
		
		//フィールドの初期化
		me = gameInfo.getAgent();//GameInfoから自分の情報を得る
		myMediumQueue.clear();
		saidCO = false;
		saidGuard = false;
		
		/********/
		grayList = new ArrayList<>(gameInfo.getAgentList());
		grayList.remove(me);
		whiteList.clear();
		blackList.clear();
		degreeOfwolf.clear();
		comingoutMap.clear();
		countTalk.clear();
		countToTalk.clear();
		dcount = 0.0;
		icount = 0;
		/********/
	}

	@Override
	public String talk() {
		// TODO Auto-generated method stub
		//return null;
				
				/**毎回？話す内容*/
				if(!saidCO) {
						saidCO = true;
						ContentBuilder builder = new ComingoutContentBuilder(me,Role.MEDIUM);
						return new Content(builder).getText();
				}else {
					if(!myMediumQueue.isEmpty()) {//カミングアウトした後は、まだ報告していない霊媒結果を順次報告
						Judge medium = myMediumQueue.poll();
						ContentBuilder builder = new IdentContentBuilder(medium.getTarget(),medium.getResult());
						return new Content(builder).getText();
					}else if(!saidGuard) {//騎士に護衛を依頼する
						Content content = new Content(new GuardCandidateContentBuilder(me));
						ContentBuilder builder = new RequestContentBuilder(null,content);
						saidGuard = true;
						return new Content(builder).getText();
					}else if(!saidIns) {//占ってほしい人を指定する(占いを依頼する)
						List<Agent> candidates = new ArrayList<>();	//候補者リスト
						for(Agent agent:blackList) {
							if(isAlive(agent)) {
								candidates.add(agent);
							}
						}
						if(candidates.isEmpty()) {
							
						}else {
							Content content = new Content(new DivinationContentBuilder(randomSelect(candidates)));
							ContentBuilder builder = new RequestContentBuilder(null,content);
							saidIns = true;
							return new Content(builder).getText();
						}
					}
				}
				
				/**相手の発話によって話す内容*/
				for (int i = pre_talkListHead; i < currentGameInfo.getTalkList().size(); i++) {
					Talk talk = currentGameInfo.getTalkList().get(i);
					Agent talker = talk.getAgent();
					if (talker == me) {	// 発言者が自分であれば除く
						continue;
					}
					pre_talkListHead = i+1;
					Content content = new Content(talk.getText());	// 発話をparse
					switch (content.getTopic()) {
					case ESTIMATE:
						// 自分を人狼と疑われた時
						if (content.getRole() == Role.WEREWOLF && content.getTarget() == me) {
							// 意見に反論
							//return (new Content(new DisagreeContentBuilder(content.getTalkType(), content.getTalkDay(), content.getTalkID()))).getText();

						// 自分を霊媒師と言ってくれた時
						}else if(me == content.getTarget() && content.getRole() == Role.MEDIUM) {
							// 意見に同意
							//return (new Content(new AgreeContentBuilder(content.getTalkType(), content.getTalkDay(), content.getTalkID()))).getText();
						}
						break;
					case COMINGOUT:
						if((content.getRole() == Role.MEDIUM) && (content.getTarget() == talker)) {
							Content and1 = new Content(new ComingoutContentBuilder(me,Role.MEDIUM));
							Content and2 = new Content(new ComingoutContentBuilder(/*content.getSubject()*/content.getTarget(),Role.MEDIUM));
							Content reason = new Content(new AndContentBuilder(and1,and2));
							Content action = new Content(new VoteContentBuilder(me,/*content.getSubject()*/content.getTarget()));
							ContentBuilder builder = new BecauseContentBuilder(me,reason,action);
							return new Content(builder).getText();
						}
						break;
					case DIVINED:
						// 占い結果報告発話の処理
						// 偽占い師が対抗占い師の占い結果を取得するため
						Judge divination = new Judge(content.getDay(),content.getSubject(),content.getTarget(),content.getResult());
						// 自分を人狼と占った時
						if (me == content.getTarget() && content.getRole() == Role.WEREWOLF) {
							// 意見に反論
							//return (new Content(new DisagreeContentBuilder(content.getTalkType(), content.getTalkDay(), content.getTalkID()))).getText();
						}else if(me == content.getTarget() && content.getRole() == Role.VILLAGER) {// 自分を村人と占ってくれた時
							// 意見に同意
							//return (new Content(new AgreeContentBuilder(content.getTalkType(), content.getTalkDay(), content.getTalkID()))).getText();
						}
						break;
					case IDENTIFIED:
						break;
					default:
						break;
					}
				}

				return Content.SKIP.getText();//以降発言しないことを宣言する
	}

	@Override
	public void update(GameInfo gameInfo) {
		// TODO Auto-generated method stub
		
		//currentGameInfoをアップデート
		currentGameInfo = gameInfo;
		
		//talkListからCO,占い・霊媒報告を抽出
		for(int i = talkListHead;i < currentGameInfo.getTalkList().size();i++) {
			Talk talk = currentGameInfo.getTalkList().get(i);
			Agent talker = talk.getAgent();
			if(talker == me) {	//自分の発言はスルー
				continue;
			}
			
			Content content = new Content(talk.getText());	//発話内容をparse
			if(!(content.equals(Content.SKIP) || content.equals(Content.OVER))) {
				icount = countTalk.containsKey(talker) ? countTalk.get(talker) : 0;
				countTalk.put(talker, icount + 1);
			}
			switch(content.getTopic()) {
			case COMINGOUT:
				comingoutMap.put(talker,content.getRole());
				//他のエージェントが霊媒師とカミングアウトした場合
				if(content.getRole() == Role.MEDIUM){
					//人狼か狂人確定
					dcount = degreeOfwolf.containsKey(talker) ? degreeOfwolf.get(talker)	: 0;
					degreeOfwolf.put(talker, dcount + 0.8);
					blackList.add(content.getTarget());
				}
				break;
			case DIVINED:
				//占い結果で人狼とでた場合
				if(content.getRole() == Role.WEREWOLF || content.getRole() == Role.POSSESSED){
					blackList.add(content.getTarget());
				}
				break;
			case IDENTIFIED:
				
				break;
			case ESTIMATE:
				//自分のことを人狼か狂人と推測している場合
				if(content.getTarget() == me){
					if(content.getRole() == Role.WEREWOLF || content.getRole() == Role.POSSESSED){
						dcount = degreeOfwolf.containsKey(talker) ? degreeOfwolf.get(talker) : 0;
						degreeOfwolf.put(talker, dcount + 0.3);
					}
				} else {
					if(content.getRole() == Role.VILLAGER) {
						Map<Agent,Integer> tmpMap = countToTalk.containsKey(talker) ? countToTalk.get(talker) : new HashMap<>();
						icount = tmpMap.containsKey(content.getTarget()) ? tmpMap.get(content.getTarget()) : 0;
						tmpMap.put(content.getTarget(), icount + 1);
						countToTalk.put(talker,tmpMap);
					}
					//自分以外の霊媒師を本物と推測している場合
					if(comingoutMap.get(content.getTarget()) == Role.MEDIUM){
						if(content.getRole() == Role.VILLAGER || content.getRole() == Role.MEDIUM){
							dcount = degreeOfwolf.containsKey(talker) ? degreeOfwolf.get(talker) : 0;
							degreeOfwolf.put(talker, dcount + 0.3);
						}
					}
				}
				break;
			default:
				break;
			}
		}
		if(countTalk.size() > 0) {
			Map.Entry<Agent, Integer> minEntry = null;
			for(Map.Entry<Agent, Integer> entry : countTalk.entrySet()) {
				if(minEntry == null || entry.getValue().compareTo(minEntry.getValue()) < 0) {
					minEntry = entry;
				}
			}
			//Agent that count of talk is the least add degree of wolf
			dcount = degreeOfwolf.containsKey(minEntry.getKey()) ? degreeOfwolf.get(minEntry.getKey()) : 0;
			degreeOfwolf.put(minEntry.getKey(), dcount + 0.05);
		}
		
		if(countToTalk.size() > 0) {
			Map.Entry<Agent, Integer> maxEntry = null;
			Map.Entry<Agent, Map<Agent, Integer>> maxEntry2 = null;
			for(Map.Entry<Agent, Map<Agent, Integer>> entry2 : countToTalk.entrySet()) {
				for(Map.Entry<Agent, Integer> entry : entry2.getValue().entrySet()) {
					if(maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0) {
						maxEntry = entry;
						maxEntry2 = entry2;
					}
				}
			}
			//Actively defending agent and defended agent add degree of wolf
			dcount = degreeOfwolf.containsKey(maxEntry.getKey()) ? degreeOfwolf.get(maxEntry.getKey()) : 0;
			degreeOfwolf.put(maxEntry.getKey(), dcount + 0.1);
			dcount = degreeOfwolf.containsKey(maxEntry2.getKey()) ? degreeOfwolf.get(maxEntry2.getKey()) : 0;
			degreeOfwolf.put(maxEntry2.getKey(), dcount + 0.1);
		}
		
		talkListHead = currentGameInfo.getTalkList().size();
	}

	@Override
	public Agent vote() {
		// TODO Auto-generated method stub
		//return null;
		
		/********/
		List<Agent> candidates = new ArrayList<>();	//候補者リスト
		for(Agent agent:blackList) {
			if(isAlive(agent)) {
				candidates.add(agent);
			}
		}
		if(candidates.isEmpty()) {
			for(Agent agent:grayList) {
				if(degreeOfwolf.containsKey(agent)) {
					if(degreeOfwolf.get(agent) > 0.75 && isAlive(agent)) {
						candidates.add(agent);
					}
				}
			}
		}
		if(candidates.isEmpty()) {
			for(Agent agent:grayList) {
				if(isAlive(agent)) {
					candidates.add(agent);
				}
			}
		}
		if(candidates.isEmpty()) {
			return null;
		}
		return randomSelect(candidates);
		/********/
	}
	
	boolean isAlive(Agent agent) {
		return currentGameInfo.getAliveAgentList().contains(agent);
	}
	<T> T randomSelect(List<T> list) {
		if(list.isEmpty()) {
			return null;
		} else {
			return list.get((int) (Math.random()*list.size()));	//0 <= Math.random() < 1.0
		}
	}
	
}