package aiwolf.org.karma;

import java.util.ArrayList;

import org.aiwolf.client.lib.AgreeContentBuilder;
import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DivinedResultContentBuilder;
import org.aiwolf.client.lib.EstimateContentBuilder;
import org.aiwolf.client.lib.RequestContentBuilder;
import org.aiwolf.client.lib.TalkType;
import org.aiwolf.client.lib.VoteContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

/**
 * �?�?師役エージェントクラス
 */
public class KarmaSeer extends KarmaBasePlayer {
	;
	boolean divineWerewolf;
	Content requestvote=null;
	int talk_requestId[];
	boolean needrequest=false;
	boolean agreerequest=false;
	boolean estimate=false;
	int agreeday=-1;
	int agreeID=-1;
	ArrayList<Agent> divinedWolfAgent=new ArrayList<Agent>();
	
	int comingoutDay;
	boolean isCameout;

	Judge divination;
	int old_c;
	boolean[] divined;
	boolean f = true;
	Parameters params;
	boolean doCO = false;
	boolean houkoku = true;
	boolean pos;
	boolean update_sh = true;
	boolean vote=false;
	
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		if(f){
			params = new Parameters(numAgents);
			
			sh = new StateHolder(numAgents);
			
			f=false;
		}
		update_sh=true;
		pos = false;
		doCO = false;
		houkoku = true;
		vote=false;
		divined = new boolean[numAgents];
		for(int i=0;i<numAgents;i++)divined[i] = false;
		ArrayList<Integer> fixed = new ArrayList<Integer>();
		fixed.add(meint);
		sh.process(params, gamedata);
		gamedata.clear();
		sh.head = 0;
		sh.game_init(fixed, meint,numAgents,Util.SEER,params);
		
		before = -1;
		talk_requestId=new int[numAgents];
		
	}
	@Override
	public void update(GameInfo gameInfo) {
		currentGameInfo = gameInfo;
		
		for (int i = 0; i < numAgents; i++)
			agents[i].alive = false;
		for (Agent a : gameInfo.getAliveAgentList()) {
			int id = a.getAgentIdx()-1;
			agents[id].alive = true;
		}
		addExecutedAgent(currentGameInfo.getLatestExecutedAgent());
		
		for (int i = talkListHead; i < currentGameInfo.getTalkList().size(); i++) {
			Talk talk = currentGameInfo.getTalkList().get(i);
			Agent talker = talk.getAgent();
			int da = talk.getDay();
			int tu = talk.getTurn();
			int italker = talker.getAgentIdx()-1;
			Content content = new Content(talk.getText());
			if(da < max_day &&  tu < max_turn)agentkoudou[da][tu][italker] = 0;
			
			if(lastTurn < tu) {
				lastTurn = tu;
				gamedata.add(new GameData(DataType.TURNSTART, day, meint,meint, false));
			}
			switch (content.getTopic()) {
			case COMINGOUT:
				
				if(roleint.containsKey(content.getRole()) ){
					agents[italker].COrole = content.getRole();
					
					gamedata.add(new GameData(DataType.CO, day, italker, roleint.get(content.getRole()), false));
				// System.out.println("CO " + italker + " " +
				// content.getRole().toString());
					if(content.getRole() == Role.VILLAGER){
						if(da < max_day &&  tu < max_turn) agentkoudou[da][tu][italker] = 1;
					}else if(content.getRole() == Role.SEER){
						if(da < max_day &&  tu < max_turn) agentkoudou[da][tu][italker] = 2;
					}else if(content.getRole() == Role.MEDIUM){
						if(da < max_day &&  tu < max_turn) agentkoudou[da][tu][italker] = 3;
					}else{
						if(da < max_day &&  tu < max_turn) agentkoudou[da][tu][italker] = 0;
					}
				}
				break;
			case DIVINED:
				//System.out.println("DIVINED " + italker + " " + content.getTarget().getAgentIdx()-1-1 + " "
				//		+ content.getResult().toString());
				gamedata.add(new GameData(DataType.TALKDIVINED, day, italker, content.getTarget().getAgentIdx()-1,
						content.getResult() == Species.HUMAN));
				if(da < max_day &&  tu < max_turn) {
					if(content.getResult() == Species.HUMAN){
						agentkoudou[da][tu][italker] = 4;
					}else{
						agentkoudou[da][tu][italker] = 5;
					}
				}
				break;
			case IDENTIFIED:
				identList.add(new Judge(day, talker, content.getTarget(), content.getResult()));
				gamedata.add(new GameData(DataType.ID, day, italker, content.getTarget().getAgentIdx()-1,
						content.getResult() == Species.HUMAN));
				if(da < max_day &&  tu < max_turn) agentkoudou[da][tu][italker] = 6;
				break;
			case VOTE:
				agents[italker].voteFor = content.getTarget().getAgentIdx()-1;
				gamedata.add(new GameData(DataType.WILLVOTE, day, italker, content.getTarget().getAgentIdx()-1, false));
				// System.out.println("vote " + italker + " " +
				// agentToInt.get(content.getTarget()));
				if(da < max_day &&  tu < max_turn) agentkoudou[da][tu][italker] = 7;	
				break;
			case ESTIMATE:
				if (content.getRole() == Role.WEREWOLF) {
					gamedata.add(
							new GameData(DataType.WILLVOTE, day, italker, content.getTarget().getAgentIdx()-1, false));
				}
				// System.out.println("vote " + italker + " " +
				// agentToInt.get(content.getTarget()));
				if(da < max_day &&  tu < max_turn) agentkoudou[da][tu][italker] = 8;
				break;
			case SKIP:
				if(da < max_day &&  tu < max_turn) agentkoudou[da][tu][italker] = 9;
			break;
			case OVER:
				if(da < max_day &&  tu < max_turn) agentkoudou[da][tu][italker] = 10;
			break;
			case OPERATOR:
				String[] data_talk=currentGameInfo.getTalkList().get(i).toString().replace("Agent[","").replace("]","").replace("(","").replace(")","").split("\\s+");
				//for(int j=0;j<data_talk.length;j++) {System.out.println(data_talk[j]);}
				Agent Target=content.getTarget();
				//System.out.println(data_talk[2]);
				if(data_talk[3].equals("REQUEST")) {
					/*if(data_talk[4].equals("ANY")) {
						System.out.println("ANY");
					}else {
						System.out.println(Target.getAgentIdx());
					}*/

					if(data_talk[5].equals("VOTE")) {
						//System.out.println("check");
						int target=Integer.parseInt(data_talk[6]);
						//System.out.println(data_talk[6]);
						Content Vote=new Content(new VoteContentBuilder(Agent.getAgent(target)));
						Agent Vote_Target=Vote.getTarget();
						//System.out.println(Integer.parseInt(data_talk[2])-1);
						//System.out.println(RequestTarget[Integer.parseInt(data_talk[2])-1]);
						//RequestTarget[Integer.parseInt(data_talk[2])-1]=target;
						talk_requestId[Integer.parseInt(data_talk[2])-1]=i;
					}

					if(data_talk[5].equals("AGREE")) {
						//Requestflag[Integer.parseInt(data_talk[2])-1]=true;
						//RequestDay[Integer.parseInt(data_talk[2])-1]=Integer.parseInt(data_talk[7].replace("day",""));
						//System.out.println(RequestDay[Integer.parseInt(data_talk[2])-1]);
						//RequestId[Integer.parseInt(data_talk[2])-1]=Integer.parseInt(data_talk[8].replace("ID:",""));
						//System.out.println(RequestId[Integer.parseInt(data_talk[2])-1]);
					}
				}
			break;
			default:
				if(da < max_day &&  tu < max_turn) agentkoudou[da][tu][italker] = 0;
				break;
			}
			if(da < max_day && tu < max_turn){
				double ssum = 0;
				for(int k=0;k<rs;k++){
					double sum = 0;
					for(int r = 0;r < N_af; r++){
						sum += af[da][tu][italker][r][k];
					}
					agentScore[da][tu][italker][k] = af[da][tu][italker][agentkoudou[da][tu][italker]][k] / sum;
					//agentScore[da][tu][italker][k]*=agentcnt[k];
					ssum += agentScore[da][tu][italker][k];
				}
				for(int k=0;k<rs;k++){
					agentScore[da][tu][italker][k]/=ssum;
				}
			}
				
		}
		talkListHead = currentGameInfo.getTalkList().size();
	}
	public void dayStart() {
		needrequest=false;
		agreerequest=false;
		estimate=false;
		vote=false;
		
		divineWerewolf=false;
		super.dayStart();
		divination = currentGameInfo.getDivineResult();
		if (divination != null) {
			divined[divination.getTarget().getAgentIdx() - 1] = true;
			houkoku = false;
			gamedata.add(new GameData(DataType.DIVINED, 
					day, meint, 
					divination.getTarget().getAgentIdx() - 1, 
					divination.getResult() == Species.HUMAN));
		}
		sh.process(params, gamedata);
		if(divinedWolfAgent.size()!=0) {
			for(int i=0;i<divinedWolfAgent.size();i++) {
				if(divinedWolfAgent.get(i)==currentGameInfo.getExecutedAgent()) {
					divinedWolfAgent.remove(i);
				}
			}
		}
	}


	protected void init() {

	}

	protected Agent chooseVote() {
		gamedata.add(new GameData(DataType.VOTESTART, day, meint,meint, false));
		
		sh.process(params, gamedata);
		
		int c = chooseMostLikelyWerewolf();
		
		if(!isValidIdx(c)) {
			return null;
		}
		
		return currentGameInfo.getAgentList().get(c);
	}
	
	protected String chooseTalk() {
		gamedata.add(new GameData(DataType.TURNSTART, day, meint,meint, false));

		sh.process(params, gamedata);

		updateState(sh);

		if(update_sh){
			System.out.println("SEARCH");
			update_sh = false;
			sh.serach(1000);
		}

		double mn = -1;
		int c = 0;
		

		/*for(int i=0;i<numAgents;i++){
			System.out.print(sh.rp.getProb(i, Util.WEREWOLF) + " ");
		}
		System.out.println();
		*/
		c = chooseMostLikelyWerewolf();
		
		
		if(getAliveAgentsCount() <= 3){
			if(!pos){
				pos = true;
				double all = 0;
				double alive = 0;
				for(int i=0;i<numAgents;i++){
					all+=sh.rp.getProb(i, Util.POSSESSED);
					if(sh.gamestate.agents[i].Alive){
						alive +=sh.rp.getProb(i, Util.POSSESSED);
					}
				}
				if(alive > 0.5 * all){
					doCO=true;
					houkoku = true;
					return (new Content(new ComingoutContentBuilder(me, Role.WEREWOLF))).getText();
				}
			}
		}
		
		if(!doCO){
			doCO = true;
			return (new Content(new ComingoutContentBuilder(me, Role.SEER))).getText();

		}
		if(!houkoku){
			houkoku = true;

			if(numAgents == 5 && day == 1&&divination.getResult()==Species.WEREWOLF){
				return (new Content(new DivinedResultContentBuilder(currentGameInfo.getAgentList().get(c),
						Species.WEREWOLF ))).getText();
			}else{
				return (new Content(new DivinedResultContentBuilder(divination.getTarget(),
						divination.getResult()) )).getText();
			}
		}
		if(before!=c) {
			estimate=false;
			vote=false;
		}
		if(!estimate) {
			
			before=c;
			estimate=true;
			if(divination!=null) {
				if(divination.getResult()==Species.WEREWOLF) {
				return (new Content(new EstimateContentBuilder(divination.getTarget(),Role.WEREWOLF))).getText();
				}
				else {
					return (new Content(new EstimateContentBuilder(currentGameInfo.getAgentList().get(c),Role.WEREWOLF))).getText();
				}
			}else {
				return (new Content(new EstimateContentBuilder(currentGameInfo.getAgentList().get(c),Role.WEREWOLF))).getText();
			}
		}
		if(!vote){
			if(divination!=null) {
				if(divination.getResult()==Species.WEREWOLF) {
				voteCandidate=divination.getTarget();
				}else {
					voteCandidate = currentGameInfo.getAgentList().get(c);
				}
			}else {
			voteCandidate = currentGameInfo.getAgentList().get(c);
			}
			before = c;
			return (new Content(new VoteContentBuilder(voteCandidate))).getText();
		}
		//if (before != c) {
		if(divination!=null) {
			if(divination.getResult()==Species.WEREWOLF) {
				divineWerewolf=true;
				divinedWolfAgent.add(divination.getTarget());
			}
		}
		
		if(divinedWolfAgent.size()!=0) {
			agreeday=currentGameInfo.getDay();
			
		}
		if(!needrequest) {
			needrequest=true;						
			agreeID=currentGameInfo.getTalkList().size();
			return (new Content(new RequestContentBuilder(Content.ANY,new Content(new VoteContentBuilder(voteCandidate))) )).getText();
		}
		if(!agreerequest) {
			agreerequest=true;
			return (new Content(new RequestContentBuilder(Content.ANY,new Content(new AgreeContentBuilder(TalkType.TALK, agreeday, talk_requestId[currentGameInfo.getAgent().getAgentIdx()-1]))) )).getText();
		}
		before = c;
		return Talk.SKIP;
	}


	public Agent divine() {
		sh.process(params, gamedata);
		sh.update();
		double mn = -1;
		int c = -1;
	
		for(int i=0;i<numAgents;i++){
			if(i!=meint){
				if(sh.gamestate.agents[i].Alive){
					if(!divined[i]){
						double score = sh.rp.getProb(i, Util.WEREWOLF);
						if(mn < score){
							mn = score;
							c = i;
						}
					}
				}
			}
		}
		
		if(!isValidIdx(c)) {
			return null;
		}
		
		return currentGameInfo.getAgentList().get(c);
	}

}