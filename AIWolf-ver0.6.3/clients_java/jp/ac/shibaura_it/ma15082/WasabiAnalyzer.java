package jp.ac.shibaura_it.ma15082;

import java.util.ArrayList;
import java.util.List;

import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.Operator;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.data.Vote;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

//NOTE:GameInfo dead_data,gurad_dataなし

public class WasabiAnalyzer {
	private GameInfo gameinfo;
	private GameSetting gamesetting;
	private ListMap<Agent, JudgeInfo> seer_data;
	private ListMap<Agent, JudgeInfo> medium_data;
	private ListMap<Agent, PlayerInfo> player_data;
	private List<LineInfo> lis;

	private TalkInfo talkinfo;
	private int talkindex;
	private int talkday;
	private int whisperindex;
	private int[] talkcount;
	List<Message> estimatelist;
	List<Pair<Agent, Agent>> votelist;
	List<Pair<Agent, Agent>> attacklist;
	List<Agent> errorlist;
	List<Request> requestlist;
	private int dead_num;
	private int dead_s;
	private int dead_m;

	public WasabiAnalyzer(GameInfo gi, GameSetting gs) {
		gameinfo = gi;
		gamesetting = gs;
		seer_data = new ListMap<Agent, JudgeInfo>();
		medium_data = new ListMap<Agent, JudgeInfo>();
		player_data = new ListMap<Agent, PlayerInfo>();
		talkinfo = new TalkInfo(gi);
		talkindex = 0;
		talkday = -1;
		talkcount = new int[gi.getAgentList().size() + 1];
		whisperindex = 0;
		estimatelist = new ArrayList<Message>();
		votelist = new ArrayList<Pair<Agent, Agent>>();
		attacklist = new ArrayList<Pair<Agent, Agent>>();
		errorlist = new ArrayList<Agent>();
		requestlist = new ArrayList<Request>();
		for (Agent a : gi.getAgentList()) {
			player_data.put(a, new PlayerInfo(a));
		}
		dead_num = 0;
		dead_s = 0;
		dead_m = 0;
		lis = new ArrayList<LineInfo>();

	}

	public int getRoleNum(Role role) {
		return gamesetting.getRoleNum(role);
	}

	public int getDay() {
		return gameinfo.getDay();
	}

	public int getAliveSize() {
		return gameinfo.getAliveAgentList().size();
	}

	public int getPlayerNum() {
		return gamesetting.getPlayerNum();
	}
	/*
	 * public List<Agent> getAliveAgent(){ return gameinfo.getAliveAgentList(); }
	 * public List<PlayerInfo> getAlivePlayerInfo(){ List<PlayerInfo> ret=new
	 * ArrayList<PlayerInfo>(); for(int i=0;i<player_data.size();i++){
	 * if(player_data.getValue(i).isAlive()){ ret.add(player_data.getValue(i)); } }
	 * return ret; }
	 */

	private void countDeadPlayer(Agent agent) {
		dead_num++;
		if (seer_data.containsKey(agent)) {
			dead_s++;
		} else if (medium_data.containsKey(agent)) {
			dead_m++;
		}
		// System.out.println(agent+" "+dead_s + " " + " " + dead_m + " " +
		// dead_num);
		return;
	}

	private int checkTalkContent(Content content, Agent agent, int count) {
		int ret = count;

		// System.out.println(agent+" "+count+" "+content);
		switch (content.getTopic()) {
		case OPERATOR:
			if (content.getOperator() == Operator.REQUEST) {
				List<Content> list = content.getContentList();
				for (Content c : list) {
					Request request = new Request(agent, c.getSubject(), c.getTopic(), c.getTarget());
					// System.out.println(request.getFrom()+"
					// "+request.getSubject()+" "+request.getVerb()+"
					// "+request.getObject());
					requestlist.add(request);
				}

			}
			break;

		case COMINGOUT:// CO 霊能と占いだけ調べる。
			// 自分のことを言っていない発言は無視する
			// System.out.println("TEST:"+content.getTarget());
			if (!agent.equals(content.getTarget())) {
				player_data.get(agent).setRole(Role.WEREWOLF);
				;
				break;
			}
			if (content.getRole() == Role.SEER && !seer_data.containsKey(agent)) {
				seer_data.put(agent, new JudgeInfo(agent, gameinfo.getDay()));
				boolean flag = player_data.get(agent).setRole(Role.SEER);
				if (flag) {
					JudgeInfo temp;
					temp = seer_data.get(agent);
					if (temp != null) {
						temp.setSlideCo(true);
					}
					temp = medium_data.get(agent);
					if (temp != null) {
						temp.setSlideCo(true);
					}
				}
			} else if (content.getRole() == Role.MEDIUM && !medium_data.containsKey(agent)) {
				medium_data.put(agent, new JudgeInfo(agent, gameinfo.getDay()));
				boolean flag = player_data.get(agent).setRole(Role.MEDIUM);
				if (flag) {
					JudgeInfo temp;
					temp = seer_data.get(agent);
					if (temp != null) {
						temp.setSlideCo(true);
					}
					temp = medium_data.get(agent);
					if (temp != null) {
						temp.setSlideCo(true);
					}
				}
			} else {
				boolean flag = player_data.get(agent).setRole(content.getRole());
				if (flag) {
					JudgeInfo temp;
					temp = seer_data.get(agent);
					if (temp != null) {
						temp.setSlideCo(true);
					}
					temp = medium_data.get(agent);
					if (temp != null) {
						temp.setSlideCo(true);
					}
				}
			}
			break;
		case IDENTIFIED:// sence
			// 霊能をCOしているときだけ考慮する
			if (medium_data.containsKey(agent)) {
				if (content.getTarget() != null) {
					medium_data.get(agent).put(gameinfo.getDay(), content.getTarget(), content.getResult(),
							talkcount[agent.getAgentIdx()]);
					player_data.get(content.getTarget()).setMedium(agent, content.getResult());
				}
			}
			break;
		case DIVINED:
			// 占いをCOしているときだけ考慮する
			if (seer_data.containsKey(agent)) {
				if (content.getTarget() != null) {
					seer_data.get(agent).put(gameinfo.getDay(), content.getTarget(), content.getResult(),
							talkcount[agent.getAgentIdx()]);
					player_data.get(content.getTarget()).setSeer(agent, content.getResult());
				}
			}
			break;

		case VOTE:
			votelist.add(new Pair<Agent, Agent>(agent, content.getTarget()));
			// break;
		default:// それ以外は疑っているか信じているかの判定だけする
			Colour c = Colour.analyze(content);
			if (c != Colour.GREY) {
				estimatelist.add(new Message(agent, content.getTarget(), c));
			}
			break;
		}
		// 処理数が多い場合はスキップする
		if (ret > 50) {
			return ret;
		}

		List<Content> childList = content.getContentList();
		if (childList == null || childList.size() <= 0) {
			return ret + 1;
		} else {
			for (Content c : childList) {
				int temp = checkTalkContent(c, agent, ret);
				if (ret < temp) {
					ret = temp;
				}
			}
		}
		return ret + 1;
	}

	private int checkWhisperContent(Content content, Agent agent, int count) {
		int ret = count;

		switch (content.getTopic()) {
		case OPERATOR:
			if (content.getOperator() == Operator.REQUEST) {
				List<Content> list = content.getContentList();
				for (Content c : list) {
					Request request = new Request(agent, c.getSubject(), c.getTopic(), c.getTarget());
					requestlist.add(request);
				}
			}
			break;
		case ATTACK:
			attacklist.add(new Pair<Agent, Agent>(agent, content.getTarget()));
			break;
		default:// それ以外は疑っているか信じているかの判定だけする
			Colour c = Colour.analyze(content);
			if (c != Colour.GREY) {
				estimatelist.add(new Message(agent, content.getTarget(), c));
			}
			break;
		}

		// 処理数が多い場合はスキップする
		if (ret > 50) {
			return ret;
		}

		List<Content> childList = content.getContentList();
		if (childList == null || childList.size() <= 0) {
			return ret + 1;
		} else {
			for (Content c : childList) {
				int temp = checkWhisperContent(c, agent, ret);
				if (ret < temp) {
					ret = temp;
				}
			}
		}
		return ret + 1;
	}

	public void update(GameInfo gi) {
		gameinfo = gi;
		List<Talk> talks = gameinfo.getTalkList();
		List<Talk> whispers = gameinfo.getWhisperList();
		Agent agent;
		estimatelist.clear();
		votelist.clear();
		attacklist.clear();
		errorlist.clear();
		requestlist.clear();
		// 発言データの初期化
		if (getDay() != talkday) {
			talkday = getDay();
			talkindex = 0;
			for (int i = 0; i < talkcount.length; i++) {
				talkcount[i] = 0;
			}
			whisperindex = 0;
		}
		// 死亡データの更新
		// 襲撃先
		// agent=gameinfo.getAttackedAgent();
		for (Agent a : gameinfo.getLastDeadAgentList()) {
			PlayerInfo pi = player_data.get(a);
			if (!pi.isDead()) {
				player_data.get(a).setBitten();
				countDeadPlayer(a);
			}
		}
		// 処刑先
		agent = gameinfo.getExecutedAgent();
		if (agent != null) {
			PlayerInfo pi = player_data.get(agent);
			if (!pi.isDead()) {
				player_data.get(agent).setVoted();
				countDeadPlayer(agent);
			}
		}
		// 役職者用の処刑先
		agent = gameinfo.getLatestExecutedAgent();
		if (agent != null) {
			PlayerInfo pi = player_data.get(agent);
			if (!pi.isDead()) {
				player_data.get(agent).setVoted();
				countDeadPlayer(agent);
			}
		}

		// 発言の解析
		int count = 0;
		// int prev=talkindex;
		for (; whisperindex < whispers.size(); whisperindex++) {
			Talk talk = whispers.get(whisperindex);
			Content content = new Content(talk.getText());

			agent = talk.getAgent();
			checkWhisperContent(content, agent, 0);
		}

		for (; talkindex < talks.size(); talkindex++) {
			Talk talk = talks.get(talkindex);
			Content content = new Content(talk.getText());
			agent = talk.getAgent();
			talkcount[agent.getAgentIdx()]++;
			try {
				checkTalkContent(content, agent, 0);
			} catch (Exception e) {
				// エラーになる発言をしたエージェントを記録する
				// e.printStackTrace();
				errorlist.add(agent);
			}
			// 読み込む発言が多すぎると時間内に処理できない。
			if (++count > 20) {
				break;
			}
		}

		talkinfo.setAttackMessages(attacklist);

		talkinfo.setMessages(estimatelist, gameinfo);
		talkinfo.setRequests(requestlist);
		talkinfo.setVoteMessages(votelist);
		talkinfo.setErrors(errorlist);

		talkinfo.calcScore();

		// 占い・霊能ラインの信頼度の計算
		lis.clear();
		List<PlayerInfo> plist = player_data.valueList();

		if (seer_data.size() + medium_data.size() > 8) {
			Agent a = gameinfo.getAgent();
			lis.add(new LineInfo(null, null, plist, seer_data.size(), medium_data.size()));

			if (seer_data.containsKey(a)) {
				lis.add(new LineInfo(seer_data.get(a), null, plist, seer_data.size(), medium_data.size()));
			} else if (medium_data.containsKey(a)) {
				lis.add(new LineInfo(null, medium_data.get(a), plist, seer_data.size(), medium_data.size()));
			}
			return;
		}

		for (int s = 0; s < seer_data.size(); s++) {
			JudgeInfo si = seer_data.getValue(s);
			for (int m = 0; m < medium_data.size(); m++) {
				JudgeInfo mi = medium_data.getValue(m);
				lis.add(new LineInfo(si, mi, plist, seer_data.size(), medium_data.size()));
			}
			// int m=medium_data.size();
			JudgeInfo mi = null;
			lis.add(new LineInfo(si, mi, plist, seer_data.size(), medium_data.size()));
		}
		// int s=seer_data.size();
		JudgeInfo si = null;
		for (int m = 0; m < medium_data.size(); m++) {
			JudgeInfo mi = medium_data.getValue(m);
			lis.add(new LineInfo(si, mi, plist, seer_data.size(), medium_data.size()));
		}
		// int m=medium_data.size();
		JudgeInfo mi = null;
		lis.add(new LineInfo(si, mi, plist, seer_data.size(), medium_data.size()));

	}

	public List<PlayerInfo> getPlayerInfos() {
		return player_data.valueList();
	}

	public double getScore(Agent from, Agent to, int d) {

		return talkinfo.getScore(from, to, d);
	}

	public int seerSize() {
		return seer_data.size() + 1;
	}

	public int mediumSize() {
		return medium_data.size() + 1;
	}

	public int seerDead() {
		return dead_s;
	}

	public int mediumDead() {
		return dead_m;
	}

	public List<Agent> getSeerList() {
		List<Agent> ret = new ArrayList<Agent>(seer_data.keyList());
		ret.add(null);
		return ret;
	}

	public List<Agent> getMediumList() {
		List<Agent> ret = new ArrayList<Agent>(medium_data.keyList());
		ret.add(null);
		return ret;
	}

	public List<JudgeInfo> getSeerInfos() {
		return seer_data.valueList();
	}

	public List<JudgeInfo> getMediumInfos() {
		return medium_data.valueList();
	}

	public LineInfo getLineInfo(Agent seer, Agent medium) {
		for (LineInfo li : lis) {
			if (((li.getSeer() == null && seer == null)
					|| (li.getSeer() != null && li.getSeer().getAgent().equals(seer)))
					&& ((li.getMedium() == null && medium == null)
							|| (li.getMedium() != null && li.getMedium().getAgent().equals(medium)))

			) {
				return li;
			}

		}

		return null;
	}

	public List<LineInfo> getLineInfos() {
		return lis;
	}

	public TalkInfo getTalkInfo() {
		return talkinfo;
	}

	public JudgeInfo getSeerInfo(Agent key) {
		return seer_data.get(key);
	}

	public JudgeInfo getMediumInfo(Agent key) {
		return medium_data.get(key);
	}

	public List<Vote> getLatestVoteList() {
		return gameinfo.getLatestVoteList();
	}

	public List<Vote> getLatestAttackVoteList() {
		return gameinfo.getLatestAttackVoteList();
	}

	public Agent getLatestAttack() {
		return gameinfo.getAttackedAgent();
	}

}
