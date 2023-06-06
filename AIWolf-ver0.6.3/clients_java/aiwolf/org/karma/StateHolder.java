package aiwolf.org.karma;

import java.util.ArrayList;
import java.util.List;

public class StateHolder {
	double[] old_WolfPoint;
	double[] diff_WolfPoint;
	int[] reason;
	int[] most_likeWerewolf_reason;
	double[] most_likeWerewolf;
	int[] better_likeWerewolf_reason;
	double[] better_likeWerewolf;
	
	RolePrediction rp;
	ScoreMatrix scorematrix;
	GameState gamestate;
	int head;

	int v = 0;
	int times = 50;
	boolean debug = false;
	int N;
	StateHolder(int _N){
		diff_WolfPoint=new double[_N];
		old_WolfPoint=new double[_N];
		reason=new int[_N];
		most_likeWerewolf_reason=new int[_N];
		most_likeWerewolf=new double[_N];
		better_likeWerewolf=new double[_N];
		better_likeWerewolf_reason=new int[_N];
	
		N=_N;
		rp = new RolePrediction() ;
		scorematrix = new ScoreMatrix();
		gamestate = new GameState(N);
		head = 0;

		v = 0;
		times = 50;
		debug = false;
		if(N==15){
			times = 500;
		}
		for(int i=0;i<_N;i++) {
			diff_WolfPoint[i]=0;
			old_WolfPoint[i]=0;
			reason[i]=0;
			most_likeWerewolf_reason[i]=0;
			most_likeWerewolf[i]=0;
			better_likeWerewolf[i]=0;
			better_likeWerewolf_reason[i]=0;
		}
	}
	
	void game_init(List<Integer> fixed, int me, int N, int role, Parameters params){
		for(int i=0;i<N;i++) {
			diff_WolfPoint[i]=0;
			old_WolfPoint[i]=1.0/N;
			reason[i]=0;
			most_likeWerewolf_reason[i]=0;
			most_likeWerewolf[i]=0;
			better_likeWerewolf_reason[i]=0;
		}
		v=0;
		rp = new RolePrediction(N, fixed, role);
		gamestate.game_init(N);
		gamestate.me = me;
		scorematrix = new ScoreMatrix();
		scorematrix.init(N);
		scorematrix.params = params;
		if(debug)
			System.out.println("GAMESTART, ME: " + me);
	}
	
	void update(){
		rp.recalc(scorematrix, gamestate);
		rp.search(scorematrix, gamestate, times);
	}
	
	void serach(int t){
		rp.search(scorematrix, gamestate, t);
	}
	
	
	void process(Parameters params, ArrayList<GameData> logs){
	
		for(;head < logs.size();head++){
			GameData g = logs.get(head);
			System.out.println();
			System.out.println(g.type+" "+g.talker+" "+g.object+" "+g.white);
			switch(g.type){
			case TURNSTART:
			{
				if(debug)
					System.out.println("----------TURNSTART--------------" + gamestate.turn);
				if(N == 5){
					//System.out.println(gamestate.day + " " + gamestate.turn);
					if(gamestate.day == 1 && gamestate.turn == 1){
						scorematrix.firstTurnEnd(gamestate);
					}
				}else{
					
					if(gamestate.day == 1 && gamestate.turn == 1){
						scorematrix.firstTurnEnd(gamestate);
					}
				}
				
				gamestate.turn++;
				rp.recalc(scorematrix, gamestate);
				rp.search(scorematrix, gamestate, times);
				
				
				/*
				for(int t = 0; t < Math.min(rp.assignments.size(), 10); t++){
					System.out.print(rp.assignments.get(t).score + " ");
					for(int i=0;i<N;i++){
						System.out.print(rp.assignments.get(t).assignment.get(i) + " ");
					}
					System.out.println();
				}
				*/
				
			}
			break;
			case ROLE:
			{
				gamestate.agents[g.talker].role = g.object;
			
			}
			break;
			case DAYCHANGE:
			{
				gamestate.day++;
				gamestate.day_init(N);
				if(debug)
					System.out.println("----------DAYCHANGE--------------");
			}
			break;
			case VOTESTART:
			{
				//if(gamestate.day == 1 && v == 0){
				rp.recalc(scorematrix, gamestate);
				rp.search(scorematrix, gamestate, times);
				
				if(debug)
					System.out.println("----------VOTESTART--------------");
			}
			break;
			case WILLVOTE:
			{
				if(debug)
					System.out.println(g.talker + " willvote " + g.object);
				scorematrix.will_vote(gamestate, g.talker, g.object);
			}
			break;
			case ESTIMATE:
			{
				if(debug)
					System.out.println(g.talker + " estimate " + g.object);
				scorematrix.estimate(gamestate, g.talker, g.object,g.white);
			}
			break;
			case TALKDIVINED:
			{
				if(debug)
					System.out.println(g.talker + " divined " + g.object + " as " + (g.white ? "V" : "W"));
				scorematrix.talk_divined(gamestate, g.talker, g.object, g.white);
			}
			break;
			case ID:
			{
				if(debug)
					System.out.println(g.talker + " idented " + g.object + " as " + (g.white ? "V" : "W"));
				scorematrix.ident(gamestate, g.talker, g.object, g.white);
			}
			break;
			case CO:
			{
				scorematrix.talk_co(gamestate, g.talker, g.object);
				gamestate.agents[g.talker].corole = g.object;
				if(debug)
					System.out.println(g.talker + " CO " + Util.role_int_to_string[g.object] );
				
			}
			break;
			case VOTE:
			{
				scorematrix.vote(gamestate, g.talker, g.object);
				if(debug)
					System.out.println(g.talker + " vote for " + g.object);
			}
			break;
			case DIVINED:
			{
				scorematrix.divined(gamestate, g.talker, g.object, g.white);
				if(debug)
					System.out.println(g.talker + " divined " + g.object + " " + (g.white ? "V" : "W"));	
			}
			break;
			case EXECUTED:
			{
				gamestate.agents[g.object].Alive = false;
				if(debug)
					System.out.println("executed " + g.object);
			}
			break;
			case KILLED:
			{
				if(g.white){
					if(gamestate.agents[g.object].Alive){
						scorematrix.killed(gamestate, g.object);
						gamestate.agents[g.object].Alive = false;
					}
				}
				if(debug)
					System.out.println("killed " + g.object + " " +  (g.white ? "" : "un") + "successfully");
			}
			break;
			case WINNER:
			{
				gamestate.agents[g.talker].wincnt++;
				
				if(debug)
					System.out.println("winner : " + g.talker);
			}
			break;
			case MATCHSTART:
			{
				gamestate = new GameState(N);
				if(debug)
					System.out.println("MATCHSTART");
			}
			break;
			case GAMEEND:
			{
				gamestate.game++;
				if(debug)
					System.out.println("GAMEEND");
			}
			break;
			default:
			{
				break;
			}
			}
			if(g.talker>=0&&g.type!=DataType.TURNSTART&&g.type!=DataType.MATCHSTART&&g.type!=DataType.GAMESTART&&g.type!=DataType.GAMEEND) {

				
				if(g.talker!=gamestate.me) {
					if(gamestate.agents[g.talker].Alive) {
						//System.out.println(1-rp.probHuman(g.talker));
						//System.out.println(head+" "+g.type+" "+g.talker+" "+g.object);
						if(diff_WolfPoint[g.talker]<(1-rp.probHuman(g.talker))-old_WolfPoint[g.talker]) {
							reason[g.talker]=head;
							diff_WolfPoint[g.talker]=(1-rp.probHuman(g.talker))-old_WolfPoint[g.talker];
						}
						old_WolfPoint[g.talker]=1-rp.probHuman(g.talker);

					}
					if(1-rp.probHuman(g.talker)>most_likeWerewolf[g.talker]) {
						if(most_likeWerewolf_reason[g.talker]!=0&&logs.get(most_likeWerewolf_reason[g.talker]).type!=logs.get(better_likeWerewolf_reason[g.talker]).type&&logs.get(most_likeWerewolf_reason[g.talker]).talker !=logs.get(better_likeWerewolf_reason[g.talker]).talker&&logs.get(most_likeWerewolf_reason[g.talker]).object !=logs.get(better_likeWerewolf_reason[g.talker]).object&&logs.get(most_likeWerewolf_reason[g.talker]).white !=logs.get(better_likeWerewolf_reason[g.talker]).white) {
							better_likeWerewolf[g.talker]=most_likeWerewolf[g.talker];
							better_likeWerewolf_reason[g.talker]=most_likeWerewolf_reason[g.talker];
						}
						most_likeWerewolf[g.talker]=1-rp.probHuman(g.talker);
						most_likeWerewolf_reason[g.talker]=head;
					}else if(1-rp.probHuman(g.talker)>better_likeWerewolf[g.talker]){
						better_likeWerewolf[g.talker]=1-rp.probHuman(g.talker);
						better_likeWerewolf_reason[g.talker]=head;
					}
				}

				if(g.type==DataType.TALKDIVINED||g.type==DataType.ID||g.type==DataType.WILLVOTE){
					if(g.talker!=gamestate.me) {
						if(gamestate.agents[g.object].Alive) {
							//System.out.println(1-rp.probHuman(g.object));
							if(g.white==false) {
								if(diff_WolfPoint[g.object]<(1-rp.probHuman(g.object))-old_WolfPoint[g.object]) {
									reason[g.object]=head;
									diff_WolfPoint[g.object]=(1-rp.probHuman(g.object))-old_WolfPoint[g.object];
								}
							}else {
								if(diff_WolfPoint[g.object]<(1-rp.probHuman(g.object))-old_WolfPoint[g.object]) {
									reason[g.object]=head;
									diff_WolfPoint[g.object]=(1-rp.probHuman(g.object))-old_WolfPoint[g.object];
								}
							}
							old_WolfPoint[g.object]=1-rp.probHuman(g.object);

						}
						if(1-rp.probHuman(g.object)>most_likeWerewolf[g.object]) {
							if(most_likeWerewolf_reason[g.object]!=0&&logs.get(most_likeWerewolf_reason[g.object]).type!=logs.get(better_likeWerewolf_reason[g.object]).type&&logs.get(most_likeWerewolf_reason[g.object]).talker !=logs.get(better_likeWerewolf_reason[g.object]).talker&&logs.get(most_likeWerewolf_reason[g.object]).object !=logs.get(better_likeWerewolf_reason[g.object]).object&&logs.get(most_likeWerewolf_reason[g.object]).white !=logs.get(better_likeWerewolf_reason[g.object]).white) {
								better_likeWerewolf[g.object]=most_likeWerewolf[g.object];
								better_likeWerewolf_reason[g.object]=most_likeWerewolf_reason[g.object];
							}
							most_likeWerewolf[g.object]=1-rp.probHuman(g.object);
							most_likeWerewolf_reason[g.object]=head;
						}else if(1-rp.probHuman(g.object)>better_likeWerewolf[g.object]){
							better_likeWerewolf[g.object]=1-rp.probHuman(g.object);
							better_likeWerewolf_reason[g.object]=head;
						}
					}
				}
			}
		}
	}
}
