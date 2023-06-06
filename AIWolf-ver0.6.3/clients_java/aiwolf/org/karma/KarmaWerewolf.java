package aiwolf.org.karma;

import java.util.ArrayList;
import java.util.List;

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
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

/**
 * 人狼役エージェントクラス
 */
public class KarmaWerewolf extends KarmaBasePlayer {
	int whisper_head;
	boolean fagreed;
	
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
	Content before_reason=null;
	Content Votereason_1 = null;
	Content Votereason_2=null;
	boolean fagree;
	;
	float diff_werewolf=0;
	StateHolder sh2;
	boolean f = true;
	Parameters params;
	boolean seer = false;
	boolean doCO = false;
	boolean houkoku = true;
	boolean pos = false;
	boolean[] divined;
	boolean[] nakama;
	int votecnt = 0;
	boolean update_sh=true;
	boolean kyoujin_ikiteru = false;
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		whisper_head=0;
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
		kyoujin_ikiteru = false;
		divined = new boolean[numAgents];
		for(int i=0;i<numAgents;i++)divined[i] = false;

		ArrayList<Integer> fixed = new ArrayList<Integer>();

		nakama = new boolean[numAgents];
		for(int i=0;i<numAgents;i++)nakama[i] = false;
		for( Agent a : gameInfo.getRoleMap().keySet()){
			fixed.add(a.getAgentIdx() - 1);
			nakama[a.getAgentIdx() - 1] = true;
		}
		sh.process(params, gamedata);
		sh2.process(params, gamedata);


		gamedata.clear();

		sh.head = 0;
		sh2.head = 0;

		sh.game_init(fixed, meint,numAgents,Util.WEREWOLF,params);
		fixed.clear();
		fixed.add(meint);

		if(numAgents == 5 && rnd.nextDouble() < 0.3){
			//seer = true;
		}else{
			seer = false;
		}
		if(numAgents == 15&&rnd.nextDouble() < 0.3){
			seer = true;
		}

		if(seer){
			sh2.game_init(fixed, meint,numAgents,Util.SEER,params);
		}else{
			sh2.game_init(fixed, meint,numAgents,Util.VILLAGER,params);
		}

		before = -1;

	}
	public void dayStart() {
		super.dayStart();
		houkoku = false;
		votecnt = 0;
	}
	protected Agent chooseVote() {
		gamedata.add(new GameData(DataType.VOTESTART, day, meint,meint, false));

		sh.process(params, gamedata);
		sh2.process(params, gamedata);
		//System.out.println("alive = " + currentGameInfo.getAliveAgentList().size());


		double mn = -1;
		int c = 0;
		if(currentGameInfo.getAliveAgentList().size() <= 3){
			votecnt++;
			if(votecnt  == 1){
				for(int i=0;i<numAgents;i++){
					if(i!=meint){
						if(sh.gamestate.agents[i].Alive){
							double score = 1 - sh.rp.getProb(i, Util.POSSESSED);
							if(mn < score){
								mn = score;
								c=i;
							}
						}
					}
				}
			}else{
				c = -1;
				//System.out.println("PP");
				for(int i=0;i<numAgents;i++){
					if(i!=meint){
						if(sh.gamestate.agents[i].Alive){
							if(sh.gamestate.agents[i].votefor == meint){
								c = i;
							}
						}
					}
				}
				if(c==-1){
					for(int i=0;i<numAgents;i++)if(i!=meint)if(sh.gamestate.agents[i].Alive){
						double score = sh.rp.getProb(i, Util.POSSESSED);
						if(mn < score){ mn = score;c = i;}
					}
				}
			}
		}else{
			if(numAgents == 5){
			c = -1;
			mn = -1;
			for(int i=0;i<numAgents;i++)if(i!=meint)if(sh.gamestate.agents[i].Alive){
				double score = sh.gamestate.cnt_vote(i) + sh2.rp.getProb(i, Util.WEREWOLF);
				if(mn < score){
					mn = score;
					c=i;
				}
			}
			if(c == -1){
				mn = -1;
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
			}
			}else{
			for(int i=0;i<numAgents;i++){
				if(i!=meint){
					if(sh.gamestate.agents[i].Alive){
						double score = sh.gamestate.cnt_vote(i);
						if(mn < score){
							mn = score;
							c=i;
						}
					}
				}
			}
			if(mn * 2 < currentGameInfo.getAliveAgentList().size()){
				mn = -1;
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
			}
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
		if(day==1&&lastTurn<1&&!(seerAgents<2)) {
			seer=false;
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
			for(int i=0;i<numAgents;i++){
				if(i!=meint){
					
					if(sh.gamestate.agents[i].Alive){
						if(!divined[i]){
							double score = sh2.rp.getProb(i, Util.WEREWOLF);
							if(nakama[i])score-=0.1;
							if(mn < score){
								mn = score;
								c=i;
							}
						}
					}
				}
			}
			if(nakama[c]&&sh2.rp.getProb(c, Util.WEREWOLF)<0.5){
				divined[c] = true;
				sh2.scorematrix.divined(sh2.gamestate, meint, c, true);
				return (new Content(new DivinedResultContentBuilder
						(currentGameInfo.getAgentList().get(c),
						Species.HUMAN))).getText();
			}else if(agents[c].COrole!=Role.SEER&&sh2.rp.getProb(c, Util.WEREWOLF)>0.6) {
				divined[c] = true;
				sh2.scorematrix.divined(sh2.gamestate, meint, c, false);
				return (new Content(new DivinedResultContentBuilder
						(currentGameInfo.getAgentList().get(c),
						Species.WEREWOLF))).getText();
			}else {
				divined[c] = true;
				sh2.scorematrix.divined(sh2.gamestate, meint, c, true);
				return (new Content(new DivinedResultContentBuilder
						(currentGameInfo.getAgentList().get(c),
						Species.HUMAN))).getText();
			}
		}

		}
		//System.out.println(day + " " +  sh.gamestate.turn);
		if(numAgents == 5){
			if(gamecount >= 50){
				if(day == 1 && sh.gamestate.turn == 1){
					return Talk.SKIP;
				}
			}

		}else if(numAgents == 15){
			if(gamecount >= 50){
				if(day == 1 && sh.gamestate.turn == 1){
					return Talk.SKIP;
				}
			}
		}
		if(currentGameInfo.getAliveAgentList().size() <= 3){
			if(!pos){
				pos = true;
				if(numAgents == 5){
					double all = 0;
					double alive = 0;
					for(int i=0;i<numAgents;i++){
						all+=sh.rp.getProb(i, Util.POSSESSED);
						if(sh.gamestate.agents[i].Alive){
							alive +=sh.rp.getProb(i, Util.POSSESSED);
						}
					}
					if(alive > 0.5 * all){
						kyoujin_ikiteru = true;
						System.out.println("kyojin");
						return (new Content(new ComingoutContentBuilder(me, Role.WEREWOLF))).getText();
					}
				}else{
					double all = 0;
					double alive = 0;
					for(int i=0;i<numAgents;i++){
						all+=sh.rp.getProb(i, Util.POSSESSED);
						if(sh.gamestate.agents[i].Alive){
							alive +=sh.rp.getProb(i, Util.POSSESSED);
						}
					}
					if(alive > 0.5 * all){
						kyoujin_ikiteru = true;
						return (new Content(new ComingoutContentBuilder(me, Role.WEREWOLF))).getText();
					}

				}
			}else if(kyoujin_ikiteru){
				if(numAgents == 0){
					//System.out.println("kyojin");
					mn = -1;
					for(int i=0;i<numAgents;i++)if(i!=meint)if(sh.gamestate.agents[i].Alive){
						double score = 1 - sh.rp.getProb(i, Util.POSSESSED);
						if(mn < score){
							mn = score;
							c = i;
						}
					}

					voteCandidate = currentGameInfo.getAgentList().get(c);
					before = c;
					return (new Content(new VoteContentBuilder(voteCandidate))).getText();
				}
			}
		}

		/*for(int i=0;i<numAgents;i++){
			System.out.print(sh2.rp.getProb(i, Util.WEREWOLF) + " ");
		}*/
		System.out.println();
		if(numAgents == 5){
			c = -1;
			mn = -1;
			for(int i=0;i<numAgents;i++)if(i!=meint)if(sh.gamestate.agents[i].Alive){
				double score = sh2.rp.getProb(i, Util.WEREWOLF);
				if(day != 1 || sh.gamestate.turn > 2){
					//score += sh.gamestate.cnt_vote(i);
				}
				//double score = sh2.rp.getProb(i, Util.WEREWOLF);
				if(mn < score){
					mn = score;
					c=i;
				}
			}
			if(c == -1){
				mn = -1;
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
			}
		}else{
			for(int i=0;i<numAgents;i++){
				if(i!=meint){
					if(sh.gamestate.agents[i].Alive){
						double score = sh.gamestate.cnt_vote(i);
						if(mn < score){
							mn = score;
							c=i;
						}
					}
				}
			}
			if(mn * 2 < currentGameInfo.getAliveAgentList().size()){
				mn = -1;
				for(int i=0;i<numAgents;i++){
					if(i!=meint){
						if(sh.gamestate.agents[i].Alive){
							double score = sh2.rp.getProb(i, Util.WEREWOLF);
							if(nakama[i])score -= 0.1;
							if(mn < score){
								mn = score;
								c=i;
							}
						}
					}
				}
			}

		}


		//System.out.println("willvote " + (c + 1) + " " + mn);
		if(numAgents == 5){
			if(sh.gamestate.cnt_vote(meint) * 2  >= currentGameInfo.getAliveAgentList().size()){
				before = -1;
			}
			if(sh.gamestate.cnt_vote(c) * 2  < currentGameInfo.getAliveAgentList().size()){
				before = -1;
			}



		}else{
			if(sh.gamestate.cnt_vote(meint) * 2 >= currentGameInfo.getAliveAgentList().size()){
				before = -1;
			}
		}
		
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
			}else{
				return (new Content((new VoteContentBuilder(voteCandidate)))).getText();
			}

		}
		
		before = c;
		before_reason=Votereason_1;
		before_reason2=Votereason_2;
		return Talk.SKIP;
	}

	protected Agent attackVote() {
		sh.process(params, gamedata);
		sh.update();
		double mn = -1;
		int c = 0;

		if(numAgents == 5){
			for(int i=0;i<numAgents;i++){
				if(i!=meint){
					if(sh.gamestate.agents[i].Alive){
						double score = 1 - sh.rp.getProb(i, Util.POSSESSED);
						//double score = sh.rp.getProb(i, Util.SEER);
						if(mn < score){
							mn = score;
							c=i;
						}
					}
				}
			}
		}else{
			for(int i=0;i<numAgents;i++){
				if(i!=meint && !nakama[i]){
					if(sh.gamestate.agents[i].Alive){
						double score = 1 - sh.rp.getProb(i, Util.POSSESSED);
						score += 0.15 * sh.rp.getProb(i, Util.SEER);
						score += 0.2 * sh.rp.getProb(i, Util.BODYGUARD);
						score += 0.1 * sh.rp.getProb(i, Util.MEDIUM);
						
						if(mn < score){
							mn = score;
							c=i;
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

	@Override
	public void update(GameInfo gameInfo) {
		super.update(gameInfo);


			for (int i = whisper_head; i < currentGameInfo.getWhisperList().size(); i++) {
				Talk talk = currentGameInfo.getWhisperList().get(i);
				Agent talker = talk.getAgent();
				int da = talk.getDay();
				int tu = talk.getTurn();
				int italker = talker.getAgentIdx()-1;
				Content content = new Content(talk.getText());
				//if(da < max_day &&  tu < max_turn)agentkoudou[da][tu][italker] = 0;

				if(lastTurn < tu) {
					lastTurn = tu;
					//gamedata.add(new GameData(DataType.TURNSTART, day, meint,meint, false));
				}
				if (content.getSubject() == Content.UNSPEC) {
					content = replaceSubject(content, talker);
				}
				parseWhisperSentence(content, talker, da, tu, italker);
			}
			whisper_head = currentGameInfo.getWhisperList().size();
			if(!seer&&day==0) {
				ArrayList<Integer> fixed = new ArrayList<Integer>();
				fixed.clear();
				fixed.add(meint);
				sh2.game_init(fixed, meint,numAgents,Util.VILLAGER,params);
			}

	}
	protected void parseWhisperSentence(Content content, Agent talker, int da, int tu, int italker) {
		switch (content.getTopic()) {
		case COMINGOUT:

			if(roleint.containsKey(content.getRole()) ){
				agents[italker].COrole = content.getRole();

				//gamedata.add(new GameData(DataType.CO, day, italker, roleint.get(content.getRole()), false));
			// System.out.println("CO " + italker + " " +
			// content.getRole().toString());
				if(content.getRole() == Role.VILLAGER){
					//if(da < max_day &&  tu < max_turn) agentkoudou[da][tu][italker] = 1;
				}else if(content.getRole() == Role.SEER){
					seer=false;
					//if(da < max_day &&  tu < max_turn) agentkoudou[da][tu][italker] = 2;
				}else if(content.getRole() == Role.MEDIUM){
					//if(da < max_day &&  tu < max_turn) agentkoudou[da][tu][italker] = 3;
				}else{
					//if(da < max_day &&  tu < max_turn) agentkoudou[da][tu][italker] = 0;
				}
			}
			break;

		default:
			//if(da < max_day &&  tu < max_turn) agentkoudou[da][tu][italker] = 0;
			break;
		}

	}
}
