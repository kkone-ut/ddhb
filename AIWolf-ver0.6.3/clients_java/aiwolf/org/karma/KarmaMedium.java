package aiwolf.org.karma;

import java.util.ArrayList;
import java.util.List;

import org.aiwolf.client.lib.AndContentBuilder;
import org.aiwolf.client.lib.BecauseContentBuilder;
import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DivinedResultContentBuilder;
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
 * 霊媒師役エージェントクラス
 */

public class KarmaMedium extends KarmaBasePlayer {

	boolean fagreed;
	List<Integer> COSeerAgent;
	List<Integer> RequestAgent;
	int max_seer_number=-1;
	int RequestTarget[];
	int vote_target=0;
	int RequestDay[];
	int RequestId[];
	boolean Requestflag[];
	double diff_WolfPoint[];
	double old_WolfPoint[];
	int reason[]=new int[5];
	Content before_reason=null;
	Content Votereason_1 = null;
	Content Votereason_2=null;
	boolean fagree;
	Judge ident;

	int before = -1;
	boolean f = true;
	Parameters params;
	boolean doCO = false;
	boolean houkoku = true;
	int target;
	boolean white;
	boolean update_sh = true;

	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		super.initialize(gameInfo, gameSetting);
		if(f){
			params = new Parameters(numAgents);

			sh = new StateHolder(numAgents);

			f=false;
		}

		doCO = false;
		houkoku = true;
		update_sh = true;

		sh.process(params, gamedata);
		gamedata.clear();
		sh.head = 0;

		ArrayList<Integer> fixed = new ArrayList<Integer>();
		fixed.add(meint);
		sh.game_init(fixed, meint,numAgents,Util.MEDIUM,params);

		before = -1;

	}

	public void dayStart() {
		super.dayStart();

		Judge ident = currentGameInfo.getMediumResult();
		if (ident != null) {
			houkoku = false;
			gamedata.add(new GameData(DataType.ID,
					day, meint,
					ident.getTarget().getAgentIdx() - 1,
					ident.getResult() == Species.HUMAN));
			target =ident.getTarget().getAgentIdx() - 1;
			white = (ident.getResult() == Species.HUMAN);
		}
		sh.process(params, gamedata);
	}


	protected void init() {

	}

	protected Agent chooseVote() {
		gamedata.add(new GameData(DataType.VOTESTART, day, meint,meint, false));

		sh.process(params, gamedata);


		double mn = -1;
		int c = 0;
		for(int i=0;i<numAgents;i++){
			if(i!=meint){
				if(sh.gamestate.agents[i].Alive){
					if(mn < sh.rp.getProb(i, Util.WEREWOLF)){
						mn = sh.rp.getProb(i, Util.WEREWOLF);
						c=i;
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

		sh.process(params, gamedata);

		lastTalkTurn = lastTurn;

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

		c = chooseMostLikelyExecuted(getAliveAgentsCount() * 0.7);
		if(c == -1){
			c = chooseMostLikelyWerewolf();
		}
		//System.out.println("willvote " + (c + 1));

		if(currentGameInfo.getDay()>=2&&(mediumAgents>0||!white||rnd.nextDouble() < 0.5)) {
		if(!doCO){
			doCO = true;
			return (new Content(new ComingoutContentBuilder(me, Role.MEDIUM))).getText();

			}
		}
		
		if(doCO&&!houkoku){
			houkoku = true;
			return (new Content(new IdentContentBuilder(currentGameInfo.getAgentList().get(target),
					white ? Species.HUMAN : Species.WEREWOLF))).getText();
		}

		switch(gamedata.get(sh.most_likeWerewolf_reason[c]).type) {
		case WILLVOTE:
		{
			Votereason_1=new Content(new VoteContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).object)));
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
			Votereason_1=new Content(new IdentContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).object),Species.WEREWOLF));
		}
		break;
		case CO:
		{
			if(gamedata.get(sh.most_likeWerewolf_reason[c]).object==0) {
				Votereason_1=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).talker),Role.WEREWOLF));
			}
			if(gamedata.get(sh.most_likeWerewolf_reason[c]).object==1) {
				Votereason_1=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).talker),Role.VILLAGER));
			}
			if(gamedata.get(sh.most_likeWerewolf_reason[c]).object==2) {
				Votereason_1=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).talker),Role.SEER));
			}
			if(gamedata.get(sh.most_likeWerewolf_reason[c]).object==3) {
				Votereason_1=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).talker),Role.POSSESSED));
			}
			if(gamedata.get(sh.most_likeWerewolf_reason[c]).object==4) {
				Votereason_1=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).talker),Role.MEDIUM));
			}
			if(gamedata.get(sh.most_likeWerewolf_reason[c]).object==5) {
				Votereason_1=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.most_likeWerewolf_reason[c]).talker),Role.BODYGUARD));
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
				Votereason_2=new Content(new IdentContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).object),Species.WEREWOLF));
			}
			break;
			case CO:
			{
				if(gamedata.get(sh.better_likeWerewolf_reason[c]).object==0) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).talker),Role.WEREWOLF));
				}
				if(gamedata.get(sh.better_likeWerewolf_reason[c]).object==1) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).talker),Role.VILLAGER));
				}
				if(gamedata.get(sh.better_likeWerewolf_reason[c]).object==2) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).talker),Role.SEER));
				}
				if(gamedata.get(sh.better_likeWerewolf_reason[c]).object==3) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).talker),Role.POSSESSED));
				}
				if(gamedata.get(sh.better_likeWerewolf_reason[c]).object==4) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).talker),Role.MEDIUM));
				}
				if(gamedata.get(sh.better_likeWerewolf_reason[c]).object==5) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.better_likeWerewolf_reason[c]).talker),Role.BODYGUARD));
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
				Votereason_2=new Content(new IdentContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).talker),currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).object),Species.WEREWOLF));
			}
			break;
			case CO:
			{
				if(gamedata.get(sh.reason[c]).object==0) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).talker),Role.WEREWOLF));
				}
				if(gamedata.get(sh.reason[c]).object==1) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).talker),Role.VILLAGER));
				}
				if(gamedata.get(sh.reason[c]).object==2) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).talker),Role.SEER));
				}
				if(gamedata.get(sh.reason[c]).object==3) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).talker),Role.POSSESSED));
				}
				if(gamedata.get(sh.reason[c]).object==4) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).talker),Role.MEDIUM));
				}
				if(gamedata.get(sh.reason[c]).object==5) {
					Votereason_2=new Content(new ComingoutContentBuilder(currentGameInfo.getAgentList().get(gamedata.get(sh.reason[c]).talker),Role.BODYGUARD));
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

		if (before != c||before_reason!=Votereason_1) {
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
			}else {
				return (new Content((new VoteContentBuilder(voteCandidate)))).getText();
			}

		}

		before = c;
		before_reason=Votereason_1;
		return Talk.SKIP;
	}


}
