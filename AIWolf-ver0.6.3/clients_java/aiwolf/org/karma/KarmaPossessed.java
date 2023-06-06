package aiwolf.org.karma;

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
import org.aiwolf.client.lib.DivinedResultContentBuilder;
import org.aiwolf.client.lib.EstimateContentBuilder;
import org.aiwolf.client.lib.IdentContentBuilder;
import org.aiwolf.client.lib.VoteContentBuilder;
import org.aiwolf.client.lib.VotedContentBuilder;
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
public class KarmaPossessed extends KarmaBasePlayer {
	
	List<Integer> COSeerAgent;
	List<Integer> RequestAgent;
	int max_seer_number=-1;
	int RequestTarget[];
	int vote_target=0;
	int RequestDay[];
	int RequestId[];
	boolean Requestflag[];
	
	
	Agent declaredVoteCandidate;
	//VoteReasonMap voteReasonMap = new VoteReasonMap();
	double diff_WolfPoint[];
	double old_WolfPoint[];
	int reason[]=new int[5];
	
	boolean fagree;
	
	
	Deque<Judge> divinationQueue = new LinkedList<>();
	Map<Agent, Species> myDivinationMap = new HashMap<>();
	List<Agent> whiteList = new ArrayList<>();
	List<Agent> blackList = new ArrayList<>();
	List<Agent> grayList;
	List<Agent> semiWolves = new ArrayList<>();
	List<Agent> possessedList = new ArrayList<>();

	StateHolder sh2;
	boolean f = true;
	Parameters params;
	boolean seer = true;
	boolean doCO = false;
	boolean houkoku = true;
	boolean[] divined;
	boolean pos = false;
	boolean update_sh=true;
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		if(f){
			params = new Parameters(numAgents);
			
			sh = new StateHolder(numAgents);
			sh2 = new StateHolder(numAgents);
			
			f=false;
		}
		Votereason_1 = null;
		Votereason_2=null;
		update_sh=true;
		doCO = false;
		houkoku = true;
		pos = false;
		divined = new boolean[numAgents];
		for(int i=0;i<numAgents;i++)divined[i] = false;
		
		if(numAgents == 5){
			seer = true;
		}else{
			seer = true;
		}
		
		ArrayList<Integer> fixed = new ArrayList<Integer>();
		fixed.add(meint);
		sh.process(params, gamedata);
		sh2.process(params, gamedata);
		

		gamedata.clear();
		
		sh.head = 0;
		sh2.head = 0;
		
		sh.game_init(fixed, meint,numAgents,Util.POSSESSED,params);
		sh2.game_init(fixed, meint,numAgents,Util.SEER,params);
		
		
		before = -1;
		
	}
	public void dayStart() {
		super.dayStart();
		houkoku = false;
		before_reason=null;
		before_reason2=null;
		
	}
	protected Agent chooseVote() {
		gamedata.add(new GameData(DataType.VOTESTART, day, meint,meint, false));
		
		sh.process(params, gamedata);
		//System.out.println("alive = " + currentGameInfo.getAliveAgentList().size());
		
		/*for(int i=0;i<numAgents;i++){
			System.out.print(sh.rp.getProb(i, Util.WEREWOLF) + " ");
		}
		System.out.println();
		
		
		for(int i=0;i<numAgents;i++){
			System.out.print(sh.rp.getProb(i, Util.SEER) + " ");
		}
		System.out.println();
		*/
		
		double mn = -1;
		int c = 0;
		if(currentGameInfo.getAliveAgentList().size() <= 3){
			for(int i=0;i<numAgents;i++){
				if(i!=meint){
					if(sh.gamestate.agents[i].Alive){
						double score = 1 - sh.rp.getProb(i, Util.WEREWOLF);
						if(mn < score){
							mn = score;
							c=i;
						}
					}
				}
			}
		}else{
			//jinnrou rashii
			mn = -100;
			
			
			if(numAgents == 5){
				for(int i=0;i<numAgents;i++){
					if(i!=meint){
						if(sh.gamestate.agents[i].Alive){
							double score = sh2.rp.getProb(i, Util.WEREWOLF) - sh.rp.getProb(i, Util.WEREWOLF);
							if(mn < score){
								mn = score;
								c=i;
							}
						}
					}
				}
			}
			else{
				for(int i=0;i<numAgents;i++){
					if(i!=meint){
						if(sh.gamestate.agents[i].Alive){
							double score = sh.rp.getProb(i, Util.WEREWOLF);
							if(mn < score){
								mn = score;
								c=i;
							}
						}
					}
				}
				int t = sh.gamestate.agents[c].will_vote;
				
				mn = -100;
				if(t==-1){
					for(int i=0;i<numAgents;i++){
						if(i!=meint){
							if(sh.gamestate.agents[i].Alive){
								double score = 1-sh.rp.getProb(i, Util.WEREWOLF);
								if(mn < score){
									mn = score;
									t=i;
								}
							}
						}
					}
				}
				c = t;
			}
			
		}
		if(!isValidIdx(c)) {
			return null;
		}
		return currentGameInfo.getAgentList().get(c);
	}
	protected String chooseTalk() {
		
		if(lastTurn == -1 || (lastTalkTurn == lastTurn)) {
			gamedata.add(new GameData(DataType.TURNSTART, day, meint,meint, false));
			lastTurn++;
		}
		
		sh.process(params, gamedata);
		sh2.process(params, gamedata);
		
		lastTalkTurn = lastTurn;
		
		updateState(sh);
		updateState(sh2);
		
		if(update_sh){
			System.out.println("SEARCH");
			update_sh = false;
			sh.serach(1000);
			sh2.serach(1000);
		}
		double mn = -1;
		int c = 0;
		
		if(seer){
		
			if(!doCO){
				doCO = true;
				return (new Content(new ComingoutContentBuilder(me, Role.SEER))).getText();
			}
			if(!houkoku){
				houkoku = true;
				if(numAgents == 5){
					if(day==1) {
					c = -1;
					for(int i=0;i<numAgents;i++){
						if(i!=meint){
							if(sh.gamestate.agents[i].Alive){
								if(!divined[i]){
									if(sh2.rp.getProb(i, Util.WEREWOLF)>0.8){
										c = i;
									}
								}
							}
						}
					}
					//c=-1;
					if(c!=-1){
	
						divined[c] = true;
						sh2.scorematrix.divined(sh2.gamestate, meint, c, false);
						return (new Content(new DivinedResultContentBuilder
								(currentGameInfo.getAgentList().get(c),
								Species.WEREWOLF))).getText();
					}
					else{
						mn = -1;
						for(int i=0;i<numAgents;i++){
							if(i!=meint){
								if(sh.gamestate.agents[i].Alive){
									if(!divined[i]&&sh.gamestate.agents[i].corole!=Util.SEER){
										if(mn < sh.rp.getProb(i, Util.WEREWOLF)){
											mn = sh.rp.getProb(i, Util.WEREWOLF);
											c=i;
										}
									}
								}
							}
						}
						
						divined[c] = true;
						sh2.scorematrix.divined(sh2.gamestate, meint, c, true);
						return (new Content(new DivinedResultContentBuilder
								(currentGameInfo.getAgentList().get(c),
								Species.HUMAN))).getText();
					}
					
					}
				}else{
					
					if(day == 1){
						mn = -100;
						for(int i=0;i<numAgents;i++){
							if(i!=meint){
								if(sh.gamestate.agents[i].Alive){
									if(!divined[i]){
										double score = 1 - sh.rp.getProb(i, Util.WEREWOLF);
										if(sh.gamestate.agents[i].corole !=-1){
											score -= 1.0;
										}
										if(mn < score){
											mn = score;
											c = i;
										}
									}
								}
							}
						}
						
						divined[c] = true;
						sh2.scorematrix.divined(sh2.gamestate, meint, c, true);
						return (new Content(new DivinedResultContentBuilder
								(currentGameInfo.getAgentList().get(c),
								Species.HUMAN))).getText();
					}else{
						mn = -100;
						for(int i=0;i<numAgents;i++){
							if(i!=meint){
								if(sh.gamestate.agents[i].Alive){
									if(!divined[i]){
										double score = sh.rp.getProb(i, Util.WEREWOLF);
										if(sh.gamestate.agents[i].corole !=-1){
											score -= 1.0;
										}
										if(mn < score){
											mn = score;
											c = i;
										}
									}
								}
							}
						}
						if(sh.rp.getProb(c, Util.WEREWOLF)<0.7) {
							divined[c] = true;
							sh2.scorematrix.divined(sh2.gamestate, meint, c, true);
							return (new Content(new DivinedResultContentBuilder
									(currentGameInfo.getAgentList().get(c),
											Species.HUMAN))).getText();
						}else {
							divined[c] = true;
							sh2.scorematrix.divined(sh2.gamestate, meint, c, false);
							return (new Content(new DivinedResultContentBuilder
									(currentGameInfo.getAgentList().get(c),
											Species.WEREWOLF))).getText();
						}
					}
				}	
			}
		}
		
		if(currentGameInfo.getAliveAgentList().size() <= 3){
			if(!pos){
				pos = true;
				return (new Content(new ComingoutContentBuilder(me, Role.POSSESSED))).getText();
			}
		}
		
		sh2.update();
		
		/*for(int i=0;i<numAgents;i++){
			System.out.print(sh2.rp.getProb(i, Util.WEREWOLF) + " ");
		}
		System.out.println();
		*/
		
		
		for(int i=0;i<numAgents;i++){
			if(i!=meint){
				if(sh.gamestate.agents[i].Alive){
					double score = sh2.rp.getProb(i, Util.WEREWOLF);
					if(mn < score){
						mn = score;
						c=i;
					}
				}
			}
		}
		
		
		//System.out.println("willvote " + (c + 1));
		
		switch(gamedata.get(sh2.most_likeWerewolf_reason[c]).type) {
		case WILLVOTE:
		{
			Votereason_1=new Content(new VoteContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.most_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.most_likeWerewolf_reason[c]).object)));
		}
		break;
		case ESTIMATE:
		{
		
			Votereason_1=new Content(new EstimateContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.most_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.most_likeWerewolf_reason[c]).object),Role.WEREWOLF));
		}
		break;
		
		case TALKDIVINED:
		{
			if(gamedata.get(sh2.most_likeWerewolf_reason[c]).white==true) {
				Votereason_1=new Content(new DivinedResultContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.most_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.most_likeWerewolf_reason[c]).object),Species.HUMAN));
			}else {
				Votereason_1=new Content(new DivinedResultContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.most_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.most_likeWerewolf_reason[c]).object),Species.WEREWOLF));
			}
		}
		break;
		case ID:
		{
			if(gamedata.get(sh2.most_likeWerewolf_reason[c]).white==true) {
				Votereason_1=new Content(new IdentContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.most_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.most_likeWerewolf_reason[c]).object),Species.HUMAN));
			}else {
				Votereason_1=new Content(new IdentContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.most_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.most_likeWerewolf_reason[c]).object),Species.WEREWOLF));
			}
		}
		break;
		case CO:
		{
			if(gamedata.get(sh2.most_likeWerewolf_reason[c]).object==0) {
				Votereason_1=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.most_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.most_likeWerewolf_reason[c]).talker),Role.WEREWOLF));
			}
			if(gamedata.get(sh2.most_likeWerewolf_reason[c]).object==1) {
				Votereason_1=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.most_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.most_likeWerewolf_reason[c]).talker),Role.VILLAGER));
			}
			if(gamedata.get(sh2.most_likeWerewolf_reason[c]).object==2) {
				Votereason_1=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.most_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.most_likeWerewolf_reason[c]).talker),Role.SEER));
			}
			if(gamedata.get(sh2.most_likeWerewolf_reason[c]).object==3) {
				Votereason_1=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.most_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.most_likeWerewolf_reason[c]).talker),Role.POSSESSED));
			}
			if(gamedata.get(sh2.most_likeWerewolf_reason[c]).object==4) {
				Votereason_1=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.most_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.most_likeWerewolf_reason[c]).talker),Role.MEDIUM));
			}
			if(gamedata.get(sh2.most_likeWerewolf_reason[c]).object==5) {
				Votereason_1=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.most_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.most_likeWerewolf_reason[c]).talker),Role.BODYGUARD));
			}
		}
		break;
		case VOTE:
		{
			Votereason_1=new Content(new VotedContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.most_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.most_likeWerewolf_reason[c]).object)));
		}
		break;
		default:
			break;


		}
		if(sh2.better_likeWerewolf_reason[c]!=0) {
			switch(gamedata.get(sh2.better_likeWerewolf_reason[c]).type) {
			case ESTIMATE:
			{
			
				Votereason_2=new Content(new EstimateContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.better_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.better_likeWerewolf_reason[c]).object),Role.WEREWOLF));
			}
			break;
			case WILLVOTE:
			{
				Votereason_2=new Content(new VoteContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.better_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.better_likeWerewolf_reason[c]).object)));
			}
			break;
			case TALKDIVINED:
			{
				if(gamedata.get(sh2.better_likeWerewolf_reason[c]).white==true) {
					Votereason_2=new Content(new DivinedResultContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.better_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.better_likeWerewolf_reason[c]).object),Species.HUMAN));
				}else {
					Votereason_2=new Content(new DivinedResultContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.better_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.better_likeWerewolf_reason[c]).object),Species.WEREWOLF));
				}
			}
			break;
			case ID:
			{
				if(gamedata.get(sh2.better_likeWerewolf_reason[c]).white==true) {
					Votereason_2=new Content(new IdentContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.better_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.better_likeWerewolf_reason[c]).object),Species.HUMAN));
				}else {
					Votereason_2=new Content(new IdentContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.better_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.better_likeWerewolf_reason[c]).object),Species.WEREWOLF));
				}
			}
			break;
			case CO:
			{
				if(gamedata.get(sh2.better_likeWerewolf_reason[c]).object==0) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.better_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.better_likeWerewolf_reason[c]).talker),Role.WEREWOLF));
				}
				if(gamedata.get(sh2.better_likeWerewolf_reason[c]).object==1) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.better_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.better_likeWerewolf_reason[c]).talker),Role.VILLAGER));
				}
				if(gamedata.get(sh2.better_likeWerewolf_reason[c]).object==2) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.better_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.better_likeWerewolf_reason[c]).talker),Role.SEER));
				}
				if(gamedata.get(sh2.better_likeWerewolf_reason[c]).object==3) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.better_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.better_likeWerewolf_reason[c]).talker),Role.POSSESSED));
				}
				if(gamedata.get(sh2.better_likeWerewolf_reason[c]).object==4) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.better_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.better_likeWerewolf_reason[c]).talker),Role.MEDIUM));
				}
				if(gamedata.get(sh2.better_likeWerewolf_reason[c]).object==5) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.better_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.better_likeWerewolf_reason[c]).talker),Role.BODYGUARD));
				}
			}
			break;
			case VOTE:
			{
				Votereason_2=new Content(new VotedContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.better_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.better_likeWerewolf_reason[c]).object)));
			}
			break;
			default:
				break;


			}
		}
		if(sh2.reason[c]!=0&&sh2.better_likeWerewolf_reason[c]==0) {
			switch(gamedata.get(sh2.reason[c]).type) {
			case ESTIMATE:
			{
			
				Votereason_2=new Content(new EstimateContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.reason[c]).object),Role.WEREWOLF));
			}
			break;
			case WILLVOTE:
			{
				Votereason_2=new Content(new VoteContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.reason[c]).object)));
			}
			break;
			case TALKDIVINED:
			{
				if(gamedata.get(sh2.reason[c]).white==true) {
					Votereason_2=new Content(new DivinedResultContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.reason[c]).object),Species.HUMAN));
				}else {
					Votereason_2=new Content(new DivinedResultContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.reason[c]).object),Species.WEREWOLF));

				}
			}
			break;
			case ID:
			{
				if(gamedata.get(sh2.reason[c]).white==true) {
					Votereason_2=new Content(new IdentContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.reason[c]).object),Species.HUMAN));
				}else {
					Votereason_2=new Content(new IdentContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.reason[c]).object),Species.WEREWOLF));
				}
			}
			break;
			case CO:
			{
				if(gamedata.get(sh2.reason[c]).object==0) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.reason[c]).talker),Role.WEREWOLF));
				}
				if(gamedata.get(sh2.reason[c]).object==1) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.reason[c]).talker),Role.VILLAGER));
				}
				if(gamedata.get(sh2.reason[c]).object==2) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.reason[c]).talker),Role.SEER));
				}
				if(gamedata.get(sh2.reason[c]).object==3) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.reason[c]).talker),Role.POSSESSED));
				}
				if(gamedata.get(sh2.reason[c]).object==4) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.reason[c]).talker),Role.MEDIUM));
				}
				if(gamedata.get(sh2.reason[c]).object==5) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.reason[c]).talker),Role.BODYGUARD));
				}
			}
			break;
			case VOTE:
			{
				Votereason_2=new Content(new VotedContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh2.reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh2.reason[c]).object)));
			}
			break;
			default:
				break;


			}
		}
		if(numAgents == 5){
			if(day == 1 && sh.gamestate.turn == 1){
				return Talk.SKIP;
			}
		}else{
			if(day == 1 && sh.gamestate.turn == 1){
				return Talk.SKIP;
			}
		}
		if (before != c||before_reason!=Votereason_1||before_reason2!=Votereason_2) {
		//if(true){

			voteCandidate = currentGameInfo.getAgentList().get(c);
			before = c;
			before_reason=Votereason_1;
			
			
			//System.out.println(Votereason_1);
			//System.out.println(Votereason_2);
			if(Votereason_1!=null&&Votereason_2!=null&&!Votereason_1.getText().equals(Votereason_2.getText())) {
				return (new Content(new BecauseContentBuilder((new Content(new AndContentBuilder(Votereason_1,Votereason_2))),new Content((new VoteContentBuilder(voteCandidate)))))).getText();
			}else if(Votereason_1!=null&&Votereason_2!=null&&Votereason_1.getText().equals(Votereason_2.getText())) {
				return (new Content(new BecauseContentBuilder(Votereason_1,new Content((new VoteContentBuilder(voteCandidate)))))).getText();
			}else if(Votereason_1!=null){
				return (new Content(new BecauseContentBuilder(Votereason_1,new Content((new VoteContentBuilder(voteCandidate)))))).getText();
			}else {
				return (new Content((new VoteContentBuilder(voteCandidate)))).getText();
			}

		}
		
		before = c;
		before_reason=Votereason_1;
		before_reason2=Votereason_1;
		return Talk.SKIP;
	}
}
