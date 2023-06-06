package aiwolf.org.karma;

import java.util.ArrayList;
import java.util.List;

import org.aiwolf.client.lib.AgreeContentBuilder;
import org.aiwolf.client.lib.AndContentBuilder;
import org.aiwolf.client.lib.BecauseContentBuilder;
import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DivinedResultContentBuilder;
import org.aiwolf.client.lib.EstimateContentBuilder;
import org.aiwolf.client.lib.IdentContentBuilder;
import org.aiwolf.client.lib.TalkType;
import org.aiwolf.client.lib.VoteContentBuilder;
import org.aiwolf.client.lib.VotedContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;



/** 村人役エージェントクラス */
public class KarmaVillager extends KarmaBasePlayer {
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

	
	boolean f = true;
	Parameters params;
	boolean update_sh=true;
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		try {
		diff_WolfPoint=new double[numAgents];
		old_WolfPoint =new double[numAgents];
		//reason=new int[numAgents];
		Votereason_1 = null;
		Votereason_2=null;

		for(int i=0;i<numAgents;i++) {
			diff_WolfPoint[i]=0;
			old_WolfPoint[i]=0;
		}
		fagreed=false;
		fagree=false;
		COSeerAgent=new ArrayList<>();

		RequestAgent=new ArrayList<>();
		numAgents = gameInfo.getAgentList().size();
		 RequestTarget=new int[numAgents];
		 RequestDay=new int[numAgents];
		 RequestId=new int[numAgents];
		 Requestflag=new boolean[numAgents];
		super.initialize(gameInfo, gameSetting);
		if(f){
			params = new Parameters(numAgents);
			sh = new StateHolder(numAgents);
			f=false;
		}
		ArrayList<Integer> fixed = new ArrayList<Integer>();
		fixed.add(meint);
		sh.process(params, gamedata);
		
		gamedata.clear();
		sh.head = 0;
		
		sh.game_init(fixed, meint,numAgents,Util.VILLAGER,params);
		update_sh=true;
		before = -1;
		}
		catch(Exception e) {
			;
		}
	}
	
	protected Agent chooseVote() {
		gamedata.add(new GameData(DataType.VOTESTART, day, meint,meint, false));
		
		sh.process(params, gamedata);
		
		
		int c = 0;
		if(requestAgree) {
			return currentGameInfo.getAgentList().get(agreeTarget);
		}
		//if(sh.gamestate.cnt_vote(meint) * 2 >= currentGameInfo.getAliveAgentList().size()){
		if(numAgents == 5){
			c = chooseMostLikelyWerewolf();
		}else{
			c = chooseMostLikelyExecuted(getAliveAgentsCount() * 0.5);
			if(c == -1){
				c = chooseMostLikelyWerewolf();
			}
		}
		if(!isValidIdx(c)) {
			return null;
		}
		return currentGameInfo.getAgentList().get(c);
	}
	protected String chooseTalk() {
		Votereason_1 = null;
		Votereason_2=null;

		//System.out.println("Test");
		gamedata.add(new GameData(DataType.TURNSTART, day, meint,meint, false));
		//for(int i=0;i<gamedata.size();i++) {
			//System.out.println(gamedata.get(i).type+" "+gamedata.get(i).day+" "+gamedata.get(i).talker+" "+gamedata.get(i).object+" "+gamedata.get(i).white);
		//}
		sh.process(params, gamedata);
		/*for(int i=0;i<gamedata.size();i++) {
			if(gamedata.get(i).talker>=0&&gamedata.get(i).type!=DataType.TURNSTART) {
				if(gamedata.get(i).type!=DataType.TALKDIVINED&&gamedata.get(i).type!=DataType.ID) {
					if(gamedata.get(i).talker!=meint) {
						if(sh.gamestate.agents[gamedata.get(i).talker].Alive) {
							System.out.println(i+" "+gamedata.get(i).type+" "+gamedata.get(i).talker+" "+gamedata.get(i).object);
							if(diff_WolfPoint[gamedata.get(i).talker]<(1-sh.rp.probHuman(gamedata.get(i).talker))-old_WolfPoint[gamedata.get(i).talker]) {
								reason[gamedata.get(i).talker]=i;
								diff_WolfPoint[gamedata.get(i).talker]=(1-sh.rp.probHuman(gamedata.get(i).talker))-old_WolfPoint[gamedata.get(i).talker];
							}
							old_WolfPoint[gamedata.get(i).talker]=1-sh.rp.probHuman(gamedata.get(i).talker);
						}
					}
				}else {
					if(gamedata.get(i).object!=meint) {
						if(sh.gamestate.agents[gamedata.get(i).object].Alive) {
							if(diff_WolfPoint[gamedata.get(i).object]<(1-sh.rp.probHuman(gamedata.get(i).object))-old_WolfPoint[gamedata.get(i).object]) {
								reason[gamedata.get(i).object]=i;
								diff_WolfPoint[gamedata.get(i).object]=(1-sh.rp.probHuman(gamedata.get(i).object))-old_WolfPoint[gamedata.get(i).object];
							}
							old_WolfPoint[gamedata.get(i).object]=1-sh.rp.probHuman(gamedata.get(i).object);
						}
					}
				}
			}
		}*/
		AliveWolves();
		System.out.println("AliveWolves "+Alivewerewolves);
		boolean condition = false;
		if(numAgents == 5){
			condition = ((day == 1 || day == 2) && sh.gamestate.turn <= 3 && sh.gamestate.turn >= 2);
		}else{
			condition = (day < max_day && sh.gamestate.turn <= 4 && sh.gamestate.turn >= 2);
		}

		if(condition){
			int tu = sh.gamestate.turn - 2;
			/*for(int j=0;j<rs;j++){
				System.out.print(Util.role_int_to_string[j] + " ");
			}
			System.out.println();
			for(int i=0;i<numAgents;i++){
				System.out.print("agent" + i + " " + agentkoudou[day][tu][i]);
				for(int j=0;j<rs;j++){
					System.out.print( " " + agentScore[day][tu][i][j]);
				}
				System.out.println();
			}*/
			if(day == 1 && sh.gamestate.turn == 2){
				pred = 0;
				double mm = 0;
				for(int i = 0;i < numAgents; i++) if(i!=meint && sh.gamestate.agents[i].Alive){
					if(mm < agentScore[day][tu][i][Util.WEREWOLF]){
						mm = agentScore[day][tu][i][Util.WEREWOLF];
						pred = i;
					}
				}
				//System.out.println("pred = " + pred);
			}
			updateState(sh);
		}
		if(update_sh){
			update_sh = false;
			sh.serach(1000);
		}
		if(requestAgree) {
			return (new Content((new AgreeContentBuilder(TalkType.TALK,agreeDay,agreeID)))).getText();
		}

		double mn = -1;
		int c = 0;

		/*for(int i=0;i<numAgents;i++){
			System.out.print(sh.rp.getProb(i, Util.WEREWOLF) + " ");
		}
		System.out.println();
		*/
		if(numAgents == 5){
			c = chooseMostLikelyWerewolf();
		}else{
			c = chooseMostLikelyExecuted(getAliveAgentsCount() * 0.5);
			if(c == -1){
				c = chooseMostLikelyWerewolf();
			}
		}
		//System.out.println("willvote " + (c + 1) + " " + mn);


		if(sh.gamestate.cnt_vote(meint) * 2 >= currentGameInfo.getAliveAgentList().size()){
			before = -1;
		}
		if(numAgents == 5){
			if(sh.gamestate.cnt_vote(c) * 2  < currentGameInfo.getAliveAgentList().size()){
				before = -1;
			}
		}


		switch(gamedata.get(sh.most_likeWerewolf_reason[c]).type) {
		case WILLVOTE:
		{
			Votereason_1=new Content(new VoteContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).object)));
		}
		break;
		case ESTIMATE:
		{
		
			Votereason_1=new Content(new EstimateContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).object),Role.WEREWOLF));
		}
		break;
		
		case TALKDIVINED:
		{
			if(gamedata.get(sh.most_likeWerewolf_reason[c]).white==true) {
				Votereason_1=new Content(new DivinedResultContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).object),Species.HUMAN));
			}else {
				Votereason_1=new Content(new DivinedResultContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).object),Species.WEREWOLF));
			}
		}
		break;
		case ID:
		{
			if(gamedata.get(sh.most_likeWerewolf_reason[c]).white==true) {
				Votereason_1=new Content(new IdentContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).object),Species.HUMAN));
			}else {
				Votereason_1=new Content(new IdentContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).object),Species.WEREWOLF));
			}
		}
		break;
		case CO:
		{
			if(gamedata.get(sh.most_likeWerewolf_reason[c]).object==0) {
				Votereason_1=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).talker),Role.WEREWOLF));
			}
			if(gamedata.get(sh.most_likeWerewolf_reason[c]).object==1) {
				Votereason_1=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).talker),Role.VILLAGER));
			}
			if(gamedata.get(sh.most_likeWerewolf_reason[c]).object==2) {
				Votereason_1=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).talker),Role.SEER));
			}
			if(gamedata.get(sh.most_likeWerewolf_reason[c]).object==3) {
				Votereason_1=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).talker),Role.POSSESSED));
			}
			if(gamedata.get(sh.most_likeWerewolf_reason[c]).object==4) {
				Votereason_1=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).talker),Role.MEDIUM));
			}
			if(gamedata.get(sh.most_likeWerewolf_reason[c]).object==5) {
				Votereason_1=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).talker),Role.BODYGUARD));
			}
		}
		break;
		case VOTE:
		{
			Votereason_1=new Content(new VotedContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).object)));
		}
		break;
		default:
			break;


		}
		if(sh.better_likeWerewolf_reason[c]!=0) {
			switch(gamedata.get(sh.better_likeWerewolf_reason[c]).type) {
			case ESTIMATE:
			{
			
				Votereason_2=new Content(new EstimateContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).object),Role.WEREWOLF));
			}
			break;
			case WILLVOTE:
			{
				Votereason_2=new Content(new VoteContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).object)));
			}
			break;
			case TALKDIVINED:
			{
				if(gamedata.get(sh.better_likeWerewolf_reason[c]).white==true) {
					Votereason_2=new Content(new DivinedResultContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).object),Species.HUMAN));
				}else {
					Votereason_2=new Content(new DivinedResultContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).object),Species.WEREWOLF));
				}
			}
			break;
			case ID:
			{
				if(gamedata.get(sh.better_likeWerewolf_reason[c]).white==true) {
					Votereason_2=new Content(new IdentContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).object),Species.HUMAN));
				}else {
					Votereason_2=new Content(new IdentContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).object),Species.WEREWOLF));
				}
			}
			break;
			case CO:
			{
				if(gamedata.get(sh.better_likeWerewolf_reason[c]).object==0) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).talker),Role.WEREWOLF));
				}
				if(gamedata.get(sh.better_likeWerewolf_reason[c]).object==1) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).talker),Role.VILLAGER));
				}
				if(gamedata.get(sh.better_likeWerewolf_reason[c]).object==2) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).talker),Role.SEER));
				}
				if(gamedata.get(sh.better_likeWerewolf_reason[c]).object==3) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).talker),Role.POSSESSED));
				}
				if(gamedata.get(sh.better_likeWerewolf_reason[c]).object==4) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).talker),Role.MEDIUM));
				}
				if(gamedata.get(sh.better_likeWerewolf_reason[c]).object==5) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).talker),Role.BODYGUARD));
				}
			}
			break;
			case VOTE:
			{
				Votereason_2=new Content(new VotedContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).object)));
			}
			break;
			default:
				break;


			}
		}
		if(sh.reason[c]!=0&&sh.better_likeWerewolf_reason[c]==0) {
			switch(gamedata.get(sh.reason[c]).type) {
			case ESTIMATE:
			{
			
				Votereason_2=new Content(new EstimateContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).object),Role.WEREWOLF));
			}
			break;
			case WILLVOTE:
			{
				Votereason_2=new Content(new VoteContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).object)));
			}
			break;
			case TALKDIVINED:
			{
				if(gamedata.get(sh.reason[c]).white==true) {
					Votereason_2=new Content(new DivinedResultContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).object),Species.HUMAN));
				}else {
					Votereason_2=new Content(new DivinedResultContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).object),Species.WEREWOLF));

				}
			}
			break;
			case ID:
			{
				if(gamedata.get(sh.reason[c]).white==true) {
					Votereason_2=new Content(new IdentContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).object),Species.HUMAN));
				}else {
					Votereason_2=new Content(new IdentContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).object),Species.WEREWOLF));
				}
			}
			break;
			case CO:
			{
				if(gamedata.get(sh.reason[c]).object==0) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).talker),Role.WEREWOLF));
				}
				if(gamedata.get(sh.reason[c]).object==1) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).talker),Role.VILLAGER));
				}
				if(gamedata.get(sh.reason[c]).object==2) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).talker),Role.SEER));
				}
				if(gamedata.get(sh.reason[c]).object==3) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).talker),Role.POSSESSED));
				}
				if(gamedata.get(sh.reason[c]).object==4) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).talker),Role.MEDIUM));
				}
				if(gamedata.get(sh.reason[c]).object==5) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).talker),Role.BODYGUARD));
				}
			}
			break;
			case VOTE:
			{
				Votereason_2=new Content(new VotedContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).object)));
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
		before_reason2=Votereason_2;
		return Talk.SKIP;

	}

	
	
}
