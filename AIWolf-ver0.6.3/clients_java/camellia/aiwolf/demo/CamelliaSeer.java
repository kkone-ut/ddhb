package camellia.aiwolf.demo;


import java.util.*;
import org.aiwolf.client.lib.*;
import org.aiwolf.common.data.*;
import org.aiwolf.common.net.*;
import org.aiwolf.sample.player.SampleBasePlayer;

public class CamelliaSeer extends SampleBasePlayer {
	Agent me;
	GameInfo currentGameInfo;
	Deque<ContentBuilder> myDivinationQueue = new LinkedList<>();
	List<Agent> whiteList = new ArrayList<>();
	List<Agent> blackList = new ArrayList<>();
	List<Agent> grayList;
	boolean saidCO = false;
	boolean devineTalked = false;
	int talkListHead;
	double dcount = 0.0;
	int icount = 0;
	boolean mediumFlag = false;
	Map<Agent, Double> degreeOfwolf = new HashMap<>(); //"degree of wolf" of every Agent
	Map<Agent, Role> comingoutMap = new HashMap<>();	//stored Coming out of every Agent
	Map<Agent, Integer> countTalk = new HashMap<>(); //count times every Agent talked
	Map<Agent, Map<Agent, Integer>> countToTalk = new HashMap<>(); //map<talker, map<target, count>>:count the agent's estimate to other agent.
	ContentBuilder builder;
	
	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		saidCO = false;
		mediumFlag = false;
		talkListHead = 0;
		dcount = 0.0;
		icount = 0;
		me = gameInfo.getAgent();
		grayList = new ArrayList<>(gameInfo.getAgentList());
		grayList.remove(me);
		whiteList.clear();
		blackList.clear();
		myDivinationQueue.clear();
		degreeOfwolf.clear();
		comingoutMap.clear();
		countTalk.clear();
		countToTalk.clear();
	}
	@Override
	public void update(GameInfo gameInfo) {	//他のメソッドより先に読み込まれる
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
				if(content.getRole() == Role.SEER){
					//werewolf or possessed
					dcount = degreeOfwolf.containsKey(talker) ? degreeOfwolf.get(talker)	: 0;
					degreeOfwolf.put(talker, dcount + 0.8);
				}
				break;
			case DIVINED:
				//agent divined by false SEER as Black is white
				if(content.getResult() == Species.WEREWOLF){
					whiteList.add(content.getTarget());
				}
				break;
			case IDENTIFIED:
				
				break;
			case ESTIMATE:
				//if talker stand against me,maybe talker is werewolf or possessed
				if(content.getTarget() == me){
					if(content.getRole() == Role.WEREWOLF || content.getRole() == Role.POSSESSED){
						dcount = degreeOfwolf.containsKey(talker) ? degreeOfwolf.get(talker) : 0;
						degreeOfwolf.put(talker, dcount + 0.3);
					}
				} else {
					if(content.getRole() == Role.VILLAGER) {
						//check contained talker in coutToTalk. if don't contained,new create.else increment icount.
						Map<Agent,Integer> tmpMap = countToTalk.containsKey(talker) ? countToTalk.get(talker) : new HashMap<>();
						icount = tmpMap.containsKey(content.getTarget()) ? tmpMap.get(content.getTarget()) : 0;
						tmpMap.put(content.getTarget(), icount + 1);
						countToTalk.put(talker,tmpMap);
					}
				//if talker defend false SEER,maybe talker is werewolf or possessed
					if(comingoutMap.get(content.getTarget()) == Role.SEER){
						if(content.getRole() == Role.VILLAGER || content.getRole() == Role.SEER){
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
		
		if(!mediumFlag) {
			int mediumCount = 0;
			for(Role role:comingoutMap.values()){
				switch(role){
				case MEDIUM:
					mediumCount++;
				default:
					break;
				}
			}
			//when number of mediumCO > 1,add 0.6 points to their degree of wolf
			if(mediumCount > 1){
				for(Map.Entry<Agent, Role> tmp:comingoutMap.entrySet()){
					if(tmp.getValue() == Role.MEDIUM){
						double count = degreeOfwolf.containsKey(tmp.getKey()) ? degreeOfwolf.get(tmp.getKey()) : 0;
						degreeOfwolf.put(tmp.getKey(), count + 0.6);
					}
				}
			}
		}
		talkListHead = currentGameInfo.getTalkList().size();
		//talkListは毎朝リセットされる
	}
	@Override
	public void finish() {
		
	}
	@Override
	public void dayStart() {
		devineTalked = false;
		Judge divination = currentGameInfo.getDivineResult();
		if(divination != null) {
			builder = new DivinedResultContentBuilder(divination.getTarget(),divination.getResult());
			builder = new DayContentBuilder(currentGameInfo.getDay(),new Content(builder));
			myDivinationQueue.offer(builder);
			Agent target = divination.getTarget();
			Species result = divination.getResult();
			grayList.remove(target);
			if(result == Species.HUMAN) {
				whiteList.add(target);
			} else {
				blackList.add(target);
			}
		}
		talkListHead = 0;
	}
	@Override
	public java.lang.String getName(){
		return "CamelliaSeer";
	}

	@Override
	public java.lang.String talk(){
		if(!saidCO) {	//カミングアウトしていない
			//占いをしていて、最後に占った者が狼である場合
			if(!myDivinationQueue.isEmpty() && new Content(myDivinationQueue.peekLast()).getContentList().get(0).getResult() == Species.WEREWOLF) {
				saidCO = true;
				builder = new ComingoutContentBuilder(me,Role.SEER);
				return new Content(builder).getText();
			}
		} else {	//カミングアウトしている
			if(!myDivinationQueue.isEmpty()) {//今までに占った結果を全て発言(*占った結果は失われる)
				ContentBuilder divination = myDivinationQueue.poll();
				builder = new DivinedResultContentBuilder(new Content(divination).getContentList().get(0).getTarget(),new Content(divination).getContentList().get(0).getResult());
				return new Content(divination).getText();
			} else if(!devineTalked) {
				devineTalked = true;
				return new Content(builder).getText();
			}
		}
		return Content.OVER.getText();	//以降発言しないことを宣言する
//		Judge judge = getLatestDayGameInfo().getDivineResult();
//		
//		ContentBuilder builder = new DivinedResultContentBuilder(judge.getTarget(),judge.getResult());
//		String talk = new Content(builder).getText();
	}
	@Override
	public Agent vote() {
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
	}
	
	@Override
	public Agent divine() {
		List<Agent> candidates = new ArrayList<>();	//候補者リスト
		for(Agent agent:grayList) {
			/*
			if(isAlive(agent)) {
				candidates.add(agent);
			}
			*/
			//add Agent with high degree of wolf
			if(degreeOfwolf.containsKey(agent)) {
				if(degreeOfwolf.get(agent) > 0.5 && isAlive(agent)){
					candidates.add(agent);
				}
			}
		}
		if(candidates.isEmpty()){
			for(Agent agent:grayList){
				if(isAlive(agent)){
					candidates.add(agent);
				}
			}
		}
		/*
		if(candidates.isEmpty()) {
			return null;
		}
		*/
		return randomSelect(candidates);
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