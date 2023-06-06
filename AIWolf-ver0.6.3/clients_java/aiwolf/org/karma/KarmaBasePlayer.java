package aiwolf.org.karma;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.Topic;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;
import org.aiwolf.common.data.Player;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Status;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.data.Vote;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;


public class KarmaBasePlayer implements Player {

	int winCount = 0;

	double winWeight = 1;

	double allwinWeight = 1;

	static int gamenum;

	static int gamecount = 0;

	int numAgents;
	int seerAgents;
	int mediumAgents;
	
	boolean EstimateRoleWolf[];
	ArrayList<Double> WerewolfPoint =new ArrayList<Double>();
	boolean requestAgree=false;
	int agreeTarget=-1;
	int Alivewerewolves=-1;
	int agreeID=-1;
	int agreeDay=-1;
	int agreeSubject=-1;
	
	Content before_reason=null;
	Content before_reason2=null;
	Content Votereason_1 = null;
	Content Votereason_2=null;
	
	Map<Integer, Agent> intToAgent = new HashMap<Integer, Agent>();

	Map<Role, Integer> roleint = new TreeMap<Role, Integer>();

	AgentInfo agents[];

	Random rnd = new Random();

	StateHolder sh;
	
	ArrayList<GameData> gamedata = new ArrayList<GameData>();
	Agent me;
	int meint;
	static int wincnt[];
	static int wincntbyrole[][];

	int before = -1;
	boolean debug = false;

	static boolean first = true;
	int day;
	GameInfo currentGameInfo;
	List<Judge> identList = new ArrayList<>();
	Deque<Content> talkQueue = new LinkedList<>();
	Deque<Content> whisperQueue = new LinkedList<>();
	Agent voteCandidate;
	int talkListHead;
	int lastTurn;
	int lastTalkTurn;
	List<Agent> humans = new ArrayList<>();
	List<Agent> werewolves = new ArrayList<>();
	static double[][][][][] af;
	static double[][][][] agentScore;
	static int[][][] agentkoudou;
	
	
	static int[] agentcnt;
	static int N_af = 11;
	static int rs;
	static int pred = 0;
	
	protected boolean isValidIdx(int a) {
		return a >= 0 && a < numAgents;
	}
	
	protected void updateState(StateHolder _sh) {
		boolean condition = false;
		if(numAgents == 5){
			condition = ((day == 1 || day == 2) && sh.gamestate.turn <= 3 && sh.gamestate.turn >= 2);
		}else{
			condition = (day < max_day && sh.gamestate.turn <= 4 && sh.gamestate.turn >= 2);
		}
		if(condition){
			int tu = _sh.gamestate.turn - 2;
			for(int r = 0;r < rs; r++){
				for(int i=0;i<numAgents;i++) if(sh.gamestate.agents[i].Alive){
					_sh.scorematrix.scores[i][r][i][r]
						+= Util.nlog(agentScore[day][tu][i][r]);	
				}
			}
			_sh.update();
		}
	}
	
	protected int chooseMostLikelyWerewolf() {
		int c = -1;
		double mn = -1e9;
		for(int i=0;i<numAgents;i++)if(i!=meint)if(sh.gamestate.agents[i].Alive){
			double score =  sh.rp.getProb(i, Util.WEREWOLF) + sh.gamestate.cnt_vote(i) * 0.0001;
			if(mn < score){
				mn = score;
				c = i;
			}
		}
		return c;
	}
	
	protected int chooseMostLikelyExecuted(double th) {
		int c = -1;
		double mn = th;
		for(int i=0;i<numAgents;i++)if(i!=meint)if(sh.gamestate.agents[i].Alive){
			double score = sh.gamestate.cnt_vote(i) + sh.rp.getProb(i, Util.WEREWOLF);
			if(mn < score){
				mn = score;
				c=i;
			}
		}	
		return c;
	}
	
	protected int getAliveAgentsCount() {
		return currentGameInfo.getAliveAgentList().size();
	}
	
	protected boolean isAlive(Agent agent) {
		return currentGameInfo.getStatusMap().get(agent) == Status.ALIVE;
	}

	protected boolean isHuman(Agent agent) {
		return humans.contains(agent);
	}

	protected boolean isWerewolf(Agent agent) {
		return werewolves.contains(agent);
	}

	protected <T> T randomSelect(List<T> list) {
		if (list.isEmpty()) {
			return null;
		} else {
			return list.get((int) (Math.random() * list.size()));
		}
	}

	public String getName() {
		return "MyBasePlayer";
	}

	protected void init() {
		
	}
	static int tmp = 0;
	int max_day = 10;
	int max_turn = 5;
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		tmp++;
		numAgents = gameInfo.getAgentList().size();
		seerAgents=0;
		requestAgree=false;
		agreeTarget=-1;
		EstimateRoleWolf=new boolean[numAgents];
		
		before_reason=null;
		before_reason2=null;
		Votereason_1 = null;
		Votereason_2=null;
		
		if (first) {
			first = false;
			new Util();
			
			if(numAgents == 5)rs=4;
			else rs = 6;
			
			wincnt = new int[numAgents];
			wincntbyrole = new int[numAgents][rs];
			
			
			
			af = new double[max_day][max_turn][numAgents][N_af][rs];
			agentScore = new double[max_day][max_turn][numAgents][rs];
			agentkoudou = new int[max_day][max_turn][numAgents];
			agentcnt = new int[rs];
			for(int d = 0;d < max_day;d++){
				for(int t = 0;t < max_turn;t++){
					for(int i=0;i<numAgents;i++){
						for(int j=0;j<N_af;j++){
							for(int k=0;k<rs;k++){
								af[d][t][i][j][k] = 0.1;
							}
						}
					}
				}
			}
		
			if(numAgents == 5){
		
				agentcnt[Util.VILLAGER] = 2;
				agentcnt[Util.SEER] = 1;
				agentcnt[Util.POSSESSED] = 1;
				agentcnt[Util.WEREWOLF] = 1;
				for(int i=0;i<numAgents;i++){
					af[1][0][i][2][Util.SEER] = 4;
					af[1][0][i][2][Util.POSSESSED] = 4;
				}
					
			}else{
				
				agentcnt[Util.VILLAGER] = 8;
				agentcnt[Util.SEER] = 1;
				agentcnt[Util.BODYGUARD] = 1;
				agentcnt[Util.MEDIUM] = 1;
				agentcnt[Util.POSSESSED] = 1;
				agentcnt[Util.WEREWOLF] = 3;
				for(int i=0;i<numAgents;i++){
					af[1][0][i][2][Util.SEER] = 4;
					af[1][0][i][2][Util.POSSESSED] = 4;
					af[1][0][i][3][Util.MEDIUM] = 4;
				}
				
				
			}
			init();
		}
		
		
		for(int d = 0;d < max_day;d++){
			for(int t = 0;t < max_turn;t++){
				for(int i=0;i<numAgents;i++){
					agentkoudou[d][t][i] = -1;
				}
			}
		}
				
		roleint.put(Role.WEREWOLF, 0);
		roleint.put(Role.VILLAGER, 1);
		roleint.put(Role.SEER, 2);
		roleint.put(Role.POSSESSED, 3);
		roleint.put(Role.MEDIUM, 4);
		roleint.put(Role.BODYGUARD, 5);
		gamenum = gamecount;
		day = -1;
		me = gameInfo.getAgent();
		meint = me.getAgentIdx() - 1;
		pred = -1;
	
		for(Agent a : gameInfo.getAgentList()){
			intToAgent.put(a.getAgentIdx() - 1, a);
		}
		
		agents = new AgentInfo[numAgents];
		for (int i = 0; i < numAgents; i++) {
			agents[i] = new AgentInfo();
		}
		for (Agent a : gameInfo.getAgentList()) {
			int id = a.getAgentIdx() - 1;
			agents[id].alive = true;
			agents[id].index = id;
		}
		// myindex = agentToInt.get(me);
		
		identList.clear();
		humans.clear();
		werewolves.clear();
		WerewolfPoint.clear();
	}
	
	@Override
	public void update(GameInfo gameInfo) {
		try {
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
			int day = talk.getDay();
			int turn = talk.getTurn();
			int italker = talker.getAgentIdx()-1;
			
			Content content = new Content(talk.getText());
			//System.out.println(content);
			/*if(day < max_day &&  turn < max_turn)agentkoudou[day][turn][italker] = 0;
			
			if(lastTurn < turn) {
				lastTurn = turn;
				gamedata.add(new GameData(DataType.TURNSTART, day, meint,meint, false));
			}*/
			if (content.getSubject() == Content.UNSPEC) {
				content = replaceSubject(content, talker);
			}
			/*if(content.getTopic()==Topic.DIVINED) {
			System.out.println(content.getResult());
			}*/
			parseSentence(content, talker, day, turn, italker,i);
		}
		talkListHead = currentGameInfo.getTalkList().size();
		}
		catch(Exception e) {
			;
		}
	}
	
	@Override
	public void dayStart() {
		try {
		if(day != currentGameInfo.getDay()){
			day = currentGameInfo.getDay();
			before = -1;
			requestAgree=false;
			agreeTarget=-1;
			before_reason=null;
			before_reason2=null;
			Votereason_1 = null;
			Votereason_2=null;
			
			//System.out.println("daystart " + day);
			List<Vote> votelist = currentGameInfo.getVoteList();
			for (Vote v : votelist) {
				gamedata.add(new GameData(DataType.VOTE, day, v.getAgent().getAgentIdx()-1, v.getTarget().getAgentIdx()-1,
						false));
			}
			if(day!=0)gamedata.add(new GameData(DataType.DAYCHANGE, -1, -1, -1, false));
			for(int i=0;i<numAgents;i++){
				agents[i].voteFor = -1;
				agents[i].NvotedBy = 0;
			}
			talkQueue.clear();
			whisperQueue.clear();
			voteCandidate = null;
			talkListHead = 0;
			lastTurn = -1;
			lastTalkTurn = -1;
			
			addExecutedAgent(currentGameInfo.getExecutedAgent());
			
			if (!currentGameInfo.getLastDeadAgentList().isEmpty()) {
				addKilledAgent(currentGameInfo.getLastDeadAgentList().get(0));
			}
		}
		}
		catch(Exception e) {
			;
		}
	}

	public void addExecutedAgent(Agent executedAgent) {
		if (executedAgent != null) {
			gamedata.add(new GameData(DataType.EXECUTED, day, -1, executedAgent.getAgentIdx()-1, false));
		}
	}

	public void addKilledAgent(Agent killedAgent) {
		if (killedAgent != null) {
			gamedata.add(new GameData(DataType.KILLED, day, -1, killedAgent.getAgentIdx()-1, true));
		}
	}

	protected void chooseVoteCandidate() {
	}
	protected Agent chooseVote() {
		return null;
	}
	protected String chooseTalk() {
		return null;
	}
	public String talk() {
		return chooseTalk();
	}

	protected void chooseAttackVoteCandidate() {
	}
	protected Agent attackVote() {
		return null;
	}

	public String whisper() {
		
		return Talk.SKIP;
	}

	public Agent vote() {
		return chooseVote();
	}

	public Agent attack() {
		return attackVote();
	}

	public Agent divine() {
		return null;
	}

	public Agent guard() {
		return null;
	}

	static int seikai = 0;
	static int all = 0;
	
	static int ww = 0;
	public void finish() {
		if (gamenum == gamecount) {
			
			gamecount++;
			//System.out.println("finish-------------------------" + gamecount + "------------------------");
			
			for (int i = 0; i < numAgents; i++)
				agents[i].alive = false;
			for (Agent a : currentGameInfo.getAliveAgentList()) {
				int id = a.getAgentIdx()-1;
				agents[id].alive = true;
			}
			Map<Agent, Role> result = currentGameInfo.getRoleMap();
			boolean werewolfWins = false;
			for (Map.Entry<Agent, Role> entry : result.entrySet()) {
				
				agents[entry.getKey().getAgentIdx() - 1].role = entry.getValue();
				if (entry.getValue() == Role.WEREWOLF) {
					if (agents[entry.getKey().getAgentIdx() - 1].alive) {
						werewolfWins = true;
					}
				}
				int id = entry.getKey().getAgentIdx() - 1; 
				for(int d=0;d<max_day;d++){
					for(int t = 0;t < max_turn;t++){
						if(agentkoudou[d][t][id] >= 0){
							af[d][t][id][agentkoudou[d][t][id]][roleint.get(entry.getValue())]++;	
						}
					}
				}
			}
			
			if(werewolfWins){
				ww++;
			}
			for (Map.Entry<Agent, Role> entry : result.entrySet()) {
				
				int id = entry.getKey().getAgentIdx() - 1; 
				int ro = roleint.get(entry.getValue());
				if((ro == Util.POSSESSED || ro == Util.WEREWOLF) == werewolfWins){
					wincnt[id]++;
					wincntbyrole[id][ro]++;
				}
				
			}
			
			
			
			
			
			if(agents[meint].role == Role.VILLAGER && pred >= 0){		
				all++;
				if(agents[pred].role == Role.WEREWOLF){
					seikai++;
				}
			}
			//System.out.println(ww / 100.0);
			//System.out.println(seikai +" " + all +" " + seikai/ (double)all);
			
			//System.out.println(winWeight / allwinWeight);
			for(int i=0;i<gamedata.size();i++) {
				
				System.out.println(gamedata.get(i).type+" "+gamedata.get(i).day+" "+gamedata.get(i).talker+" "+gamedata.get(i).object+" "+(gamedata.get(i).white));
				
			}
		}
	}
	static Content replaceSubject(Content content, Agent newSubject) {
		if (content.getTopic() == Topic.SKIP || content.getTopic() == Topic.OVER) {
			return content;
		}
		if (newSubject == Content.UNSPEC) {
			return new Content(Content.stripSubject(content.getText()));
		} else {
			return new Content(newSubject + " " + Content.stripSubject(content.getText()));
		}
	}
	
	protected void parseSentence(Content content, Agent talker, int da, int tu, int italker,int id) {
		//System.out.println(content.getTarget().getAgentIdx()-1);
		switch (content.getTopic()) {
		case COMINGOUT:
			if(italker<1) {
				break;
			}
			
			if(roleint.containsKey(content.getRole()) ){
				agents[italker].COrole = content.getRole();
				
				gamedata.add(new GameData(DataType.CO, da, italker, roleint.get(content.getRole()), false));
			// System.out.println("CO " + italker + " " +
			// content.getRole().toString());
				if(content.getRole() == Role.VILLAGER){
					if(da < max_day &&  tu < max_turn) agentkoudou[da][tu][italker] = 1;
				}else if(content.getRole() == Role.SEER){
					seerAgents++;
					if(da < max_day &&  tu < max_turn) agentkoudou[da][tu][italker] = 2;
				}else if(content.getRole() == Role.MEDIUM){
					mediumAgents++;
					if(da < max_day &&  tu < max_turn) agentkoudou[da][tu][italker] = 3;
				}else{
					if(da < max_day &&  tu < max_turn) agentkoudou[da][tu][italker] = 0;
				}
			}
			break;
		case DIVINED:
			if(italker<0) {
				break;
			}
			//System.out.println("DIVINED " + italker + " " + (content.getTarget().getAgentIdx()-1) + " "+ content.getResult().toString());
			//System.out.println(content.getText()+" "+content.getResult()+" "+ (content.getResult()==Species.HUMAN));
			gamedata.add(new GameData(DataType.TALKDIVINED, da, italker, content.getTarget().getAgentIdx()-1,content.getResult() == Species.HUMAN));
			//System.out.println(gamedata.get(gamedata.size()-1).type+" "+gamedata.get(gamedata.size()-1).day+" "+gamedata.get(gamedata.size()-1).white);
			if(da < max_day &&  tu < max_turn) {
				if(content.getResult()==Species.HUMAN) {
					agentkoudou[da][tu][italker] = 4;
				}else{
					agentkoudou[da][tu][italker] = 5;
				}
			}
			break;
		case IDENTIFIED:
			if(italker<0) {
				break;
			}
			identList.add(new Judge(day, talker, content.getTarget(), content.getResult()));
			gamedata.add(new GameData(DataType.ID, da, italker, content.getTarget().getAgentIdx()-1,content.getResult() == Species.HUMAN));
			if(da < max_day &&  tu < max_turn) agentkoudou[da][tu][italker] = 6;
			break;
		case VOTE:
			if(italker<0) {
				break;
			}
			agents[italker].voteFor = content.getTarget().getAgentIdx()-1;
			gamedata.add(new GameData(DataType.WILLVOTE, da, italker, content.getTarget().getAgentIdx()-1, false));
			// System.out.println("vote " + italker + " " +
			// agentToInt.get(content.getTarget()));
			if(da < max_day &&  tu < max_turn) agentkoudou[da][tu][italker] = 7;	
			break;
		case ESTIMATE:
			if(italker<0) {
				break;
			}
			if (content.getRole() == Role.WEREWOLF||content.getRole() == Role.POSSESSED) {
				gamedata.add(
						new GameData(DataType.ESTIMATE, da, italker, content.getTarget().getAgentIdx()-1, false));
			}else {
				gamedata.add(
						new GameData(DataType.ESTIMATE, da, italker, content.getTarget().getAgentIdx()-1, true));
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
			
			parseOperator(content,talker,da,tu,italker,id);
			return;
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
	
	protected void parseOperator(Content content, Agent talker, int day, int turn, int italker,int id) {
		switch (content.getOperator()) {
		case BECAUSE:
			// [0]に理由，[1]に結論が入っているので，結論だけ処理
			//parseSentence(content.getContentList().get(0), talker, day, turn, italker);
			parseSentence(content.getContentList().get(1), talker, day, turn, italker,id);
			// TODO: OPERATORを付けたTOPICの作成
			/*if (day < max_day && turn < max_turn) {
				agentkoudou[day][turn][italker] = Util.ACT_BECAUSE;
			}*/
			break;
		case DAY:
			// 特定の日付について言及しているが，内容だけ処理
			//System.out.println(content.getContentList().get(0));
			parseSentence(content.getContentList().get(0), talker, day, turn, italker,id);
//			if (day < max_day && turn < max_turn) {
//				agentkoudou[day][turn][italker] = Util.ACT_DAY;
//			}
			break;
			// AND(全て真)，OR(1つは真)，XOR(どちらかを主張≒どちらかが真)
		case AND:
			//System.out.println(content.getContentList());
			for (Content c : content.getContentList()) {
				parseSentence(c, talker, day, turn, italker,id);
				//break;
			}
			break;
		case REQUEST:
			if(content.getContentList().get(0).getTopic()==Topic.VOTE) {
				if(!requestAgree) {
					if(currentGameInfo.getAliveAgentList().size()-2>Alivewerewolves) {
						agreeTarget=content.getContentList().get(0).getTarget().getAgentIdx()-1;
						if(EstimateRoleWolf[agreeTarget]) {
						requestAgree=true;
						agreeSubject=italker;
						
						agreeID=id;
						agreeDay=currentGameInfo.getDay();
					}
					}
				}else {
					if(currentGameInfo.getAliveAgentList().size()-2>Alivewerewolves&&sh.rp.probHuman(agreeSubject)<sh.rp.probHuman(italker)) {
						agreeTarget=content.getContentList().get(0).getTarget().getAgentIdx()-1;
						if(EstimateRoleWolf[agreeTarget]) {
						requestAgree=true;
						agreeSubject=italker;
						
						agreeID=id;
						agreeDay=currentGameInfo.getDay();
					}
					}
				}
			}
			break;
		case OR:
			break;
		case XOR:

			/*for (Content c : content.getContentList()) {
				parseSentence(c, talker, day, turn, italker);
			}*/
//			break;
			// それ以外はここでは無視

//			if (day < max_day && turn < max_turn) {
//				agentkoudou[day][turn][italker] = Util.ACT_OPERATOR;
//			}

			break;
		default:
			break;
		}
	}
	
	public void AliveWolves() {
		WerewolfPoint.clear();
		for(int i=0;i<numAgents;i++) {
			WerewolfPoint.add(1-sh.rp.probHuman(i));
			EstimateRoleWolf[i]=false;
		}
		ArrayList<Double> sorted=WerewolfPoint;
		Collections.sort(sorted, Collections.reverseOrder());
		for(int i=0;i<agentcnt[Util.WEREWOLF]+agentcnt[Util.POSSESSED];i++) {
			for(int j=0;j<numAgents;j++) {
				if(sorted.get(i)==WerewolfPoint.get(j)) {
					EstimateRoleWolf[j]=true;
					break;
				}
			}
		}
		Alivewerewolves=0;
		for(int i=0;i<numAgents;i++) {
			if(EstimateRoleWolf[i]&&sh.gamestate.agents[i].Alive) {
				Alivewerewolves+=1;
			}
		}
	}
	
	
	
}
