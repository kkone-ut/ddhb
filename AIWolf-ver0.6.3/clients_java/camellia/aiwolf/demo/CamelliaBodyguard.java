package camellia.aiwolf.demo;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.ContentBuilder;
import org.aiwolf.client.lib.Operator;
import org.aiwolf.client.lib.Topic;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.data.Vote;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;
import org.aiwolf.sample.player.SampleBasePlayer;

public class CamelliaBodyguard extends SampleBasePlayer {
	Agent me;
	Agent guardAgent; //ガードするエージェント
	Agent secondVotedAgent; //2番目に投票が多かったエージェント
	int guardflag = 0; //ガード成功時1
	int guardRequest = 0;

	GameInfo currentGameInfo;
	List<Agent> whiteList = new ArrayList<>();
	List<Agent> blackList  = new ArrayList<>();
	List<Agent> grayList;
	List<Agent> comingoutSeerList = new ArrayList<>();	// 占い師COしたエージェントリスト
	List<Agent> comingoutMediumList = new ArrayList<>();	// 霊媒師COしたエージェントリスト
	List<Agent> guardCandidateList = new ArrayList<>(); //ガード候補のエージェントのリスト
	List<Integer> estimateIDList = new ArrayList<>(); //推測やAttackされた発話のIDを保存


	boolean saidCO = false;
	int estimateWEREWOLF = 0; //自分が何度人狼と疑われたか
	int talkListHead;
	Map<Agent, Role> comingoutMap = new HashMap<>();


	@Override
	public String getName() {
		return "CamelliaBodyguard";
	}

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		// フィールドの初期化
		me = gameInfo.getAgent();
		grayList = new ArrayList<>(gameInfo.getAgentList());
		grayList.remove(me);
		whiteList.clear();
		blackList.clear();
		comingoutSeerList.clear();
		comingoutMediumList.clear();
		guardCandidateList.clear();
		estimateIDList.clear();


		//ガード候補となりうる自分以外の生きているAgentが入ったリストを作成
		guardCandidateList = new ArrayList<>(gameInfo.getAgentList());
		guardCandidateList.remove(me);

	}

	@Override
	public void dayStart() {
		guardflag = 0;
		Agent executed = currentGameInfo.getExecutedAgent();
		List<Agent> deadAgents = currentGameInfo.getLastDeadAgentList();
		for (Agent deadAgent : deadAgents) {
			// 前日に死亡したエージェントをリストから取り除く
			if (guardCandidateList.contains(deadAgent)) {
				guardCandidateList.remove(deadAgent);
			}
		}
		if(guardCandidateList.contains(executed)){
			//前日に追放されたエージェントを除く
			guardCandidateList.remove(executed);
		}
		//二日目以降で死んだエージェントがいなかった時ガード成功,その対象をwhiteListに
		if(deadAgents.size() == 0 && currentGameInfo.getDay() > 1) {
			guardflag = 1;
			whiteList.add(guardAgent);
		}
		//占い師COしていた人が襲撃にあったらその人が占い師だったとして、COした他のエージェントをブラックリストに
		for(Agent deadAgent : deadAgents) {
			if(comingoutSeerList.contains(deadAgent) && currentGameInfo.getExecutedAgent() != deadAgent) {
				for(Agent seer : comingoutSeerList) {
					if(!blackList.contains(seer) && isAlive(seer)) {
						blackList.add(seer);
					}
				}
			}
		}
		//霊媒師COしていた人が襲撃にあったらその人が霊媒師だったとして、COした他のエージェントをブラックリストに
		for(Agent deadAgent : deadAgents) {
			if(comingoutMediumList.contains(deadAgent) && currentGameInfo.getExecutedAgent() != deadAgent) {
				for(Agent medium : comingoutMediumList) {
					if(!blackList.contains(medium) && isAlive(medium)) {
						blackList.add(medium);
					}
				}
			}
		}
		/*2番目に多く投票されたエージェントをsecondVotedAgentに入れる*/
		List<Agent> votedAgentList = new ArrayList<>(); //投票されたエージェントリスト
		List<Integer> votedcountList = new ArrayList<>(); //投票された回数のリスト
		for(int i = 0; i<currentGameInfo.getVoteList().size();i++) {
			Vote vote = currentGameInfo.getVoteList().get(i); //前日に投票されたエージェントを順番に取り出す
			if(!votedAgentList.contains(vote.getTarget()) && vote.getTarget() != executed) {
				//追放されたエージェント以外の投票されたエージェントがリストに追加されていなかったら追加して投票回数を1回とする
				votedAgentList.add(vote.getTarget());
				votedcountList.add(1);
			}else {
				//投票されたエージェントがすでにリストにいた場合投票回数を+1
				for(int j=0;j<votedAgentList.size();j++) {
					if(votedAgentList.get(j) == vote.getTarget()) {
						votedcountList.set(j, votedcountList.get(j)+1);
					}
				}
			}
		}
		int votedcount,num=0,max=0;
		/*追放されていないエージェントの中で一番投票数が多いエージェントをsecondVotedAgentに*/
		for(int i = 0;i < votedcountList.size();i++) {
			votedcount=votedcountList.get(i);
			if(max<votedcount) {
				max=votedcount;
				num=i;
			}else if(max==votedcount && whiteList.contains(votedAgentList.get(num))) {
				//countが同じだった場合whiteListにいない方をsecondVotedAgent候補に残す
				max=votedcount;
				num=i;
			}
		}
		if(!votedAgentList.isEmpty()) {
			secondVotedAgent=votedAgentList.get(num);
		}

		// 今日の発話リストですでに読み込んだ内容を覚えておく変数
		talkListHead = 0;

	}


	public void update(GameInfo gameinfo) {
		currentGameInfo = gameinfo;
		// GameInfo.talkListからカミングアウト・占い報告・霊媒報告を抽出
		for (int i = talkListHead; i < currentGameInfo.getTalkList().size(); i++) {
			Talk talk = currentGameInfo.getTalkList().get(i);
			Agent talker = talk.getAgent();
			if (talker == me) {	// 発言者が自分であれば除く
				continue;
			}
			Content content = new Content(talk.getText());	// 発話をparse

			switch (content.getTopic()) {
			case COMINGOUT:
				// カミングアウト発話の処理,何してるかわからない
				comingoutMap.put(talker, content.getRole());
				// 占い師カミングアウトの処理
				if (!comingoutSeerList.contains(talker)) {
					comingoutSeerList.add(talker);
				}
				// 霊媒師カミングアウトの処理
				if (!comingoutMediumList.contains(talker)) {
					comingoutMediumList.add(talker);
				}
				break;
			case DIVINED:
				// 占い結果報告発話の処理
				//自分をHUMAN以外と占えばブラックリストに
				if(content.getResult() != Species.HUMAN && content.getTarget() == me) {
					if(!blackList.contains(talker)) {
						blackList.add(talker);
					}
				}
				//2日目以降でCOした占い師が一人の時,占い師が人狼と占った人をblackListに追加,占い師が人間と占った人をwhiteListに追加
				if(currentGameInfo.getDay() > 1 && comingoutSeerList.size() == 1 && content.getResult() == Species.WEREWOLF) {
					blackList.add(content.getTarget());
				}else if(currentGameInfo.getDay() > 1 && comingoutSeerList.size() == 1 && content.getResult() == Species.HUMAN) {
					whiteList.add(content.getTarget());
				}
				break;
			case IDENTIFIED:
				//霊媒師の発話結果処理
				//2日目以降で霊媒師CO一人の場合,その人を信じることとする
				//霊媒師が死んだエージェントが人狼といった場合そのエージェントを占い師COしていたら占い師COリストから除く
				if(currentGameInfo.getDay() > 1 && comingoutMediumList.size() == 1) {
					if(content.getResult() == Species.WEREWOLF) {
						if(comingoutSeerList.contains(content.getTarget())) {
							comingoutSeerList.remove(content.getTarget());
						}
					}
				}
				break;
			case ESTIMATE:
				//人狼と疑われたら疑われた回数+1でその発話のID保存
				if(content.getTarget() == me && content.getRole() == Role.WEREWOLF) {
					estimateIDList.add(content.getTalkID());
					estimateWEREWOLF++;
				}
				break;
			case AGREE:
				//人狼と疑われた発話に賛成されたら疑われた回数+1
				if(estimateIDList.contains(content.getTalkID())) {
					estimateWEREWOLF++;
				}
				break;
			case ATTACK:
				//投票対象にされたら疑われた回数+1
				if(content.getTarget()==me) {
					estimateWEREWOLF++;
				}
				break;
			case OPERATOR:
				//ガードリクエストされた時そのエージェントがblackListにいないなら守る
				if(content.getOperator() == Operator.REQUEST) {
					for(Content c : content.getContentList()) {
						if(c.getTopic() == Topic.GUARD && !blackList.contains(talker)) {
							guardAgent = talker;
							guardRequest = 1;
						}
					}
				}
				break;
			default:
				break;
			}

		}
		talkListHead = currentGameInfo.getTalkList().size();
	}

	@Override
	public String talk() {
		//3回以上人狼だと疑われている場合騎士だとカミングアウトする
		if(estimateWEREWOLF>=3 && !saidCO) {
			ContentBuilder builder = new ComingoutContentBuilder(me, Role.BODYGUARD);
			return new Content(builder).getText();
		}
		return Content.SKIP.getText();
	}

	// 引数のエージェントがまだ生きているか
	boolean isAlive(Agent agent) {
		return currentGameInfo.getAliveAgentList().contains(agent);
	}

	// 引数のリストからランダムに1つ要素を取り出す
	<T> T randomSelect(List<T> list) {
		if (list.isEmpty()) {
			return null;
			} else {
				return list.get((int) (Math.random() * list.size()));
				}

	}

	@Override
	public Agent vote() {
		// 候補者リスト
		List<Agent> candidates = new ArrayList<>();
		// 生きている人狼を候補者リストに加える
		for (Agent agent : blackList) {
			if (isAlive(agent)) {		// 生きているかどうかを判定
				candidates.add(agent);	// 候補リストへの追加
			}
		}

		//blackListに誰もいないなら前日に2番目に多く投票された人に投票
		if(candidates.isEmpty()) {
			if(isAlive(secondVotedAgent) && secondVotedAgent!=null) {
				candidates.add(secondVotedAgent);
			}
		}
	// 候補者がいない場合は生きている灰色のプレイヤーを候補者リストに加える(white除く)
		if (candidates.isEmpty()) {
			for (Agent agent : grayList) {
				if (isAlive(agent) && !whiteList.contains(agent)) {
					candidates.add(agent);
				}
			}
		}

		// 候補者がいない場合はnullを返す (自分以外の生存プレイヤーからランダム)
		if (candidates.isEmpty()) {
			return null;
		}

		return randomSelect(candidates);
	}




	public Agent guard() {
		/*ガード候補の中からblackListの人を取り除く*/
		int i;
		Agent rem;
		//占い師COコピー
		List<Agent> candidateSeerList = new ArrayList<>();

		for(i=0;i<comingoutSeerList.size();i++) {
			if(isAlive(comingoutSeerList.get(i))) {
				candidateSeerList.add(comingoutSeerList.get(i));
			}
		}

		//霊媒師COコピー
		List<Agent> candidateMediumList = new ArrayList<>();

		for(i=0;i<comingoutMediumList.size();i++) {
			if(isAlive(comingoutMediumList.get(i))) {
				candidateMediumList.add(comingoutMediumList.get(i));
			}
		}


		for(Agent blackAgent : blackList) {
			if(guardCandidateList.contains(blackAgent)) {
				guardCandidateList.remove(blackAgent);
			}
		}

		/*占い師COのコピーからガード候補でない人を消去
		 ConcurrentModificationExceptionを避けるためにイテレータ使用*/
		Iterator<Agent> seeriter = candidateSeerList.iterator();
		while(seeriter.hasNext()) {
			rem = seeriter.next();
			if(!guardCandidateList.contains(rem) && isAlive(rem) && candidateSeerList.contains(rem)) {
				seeriter.remove();
			}
		}

		/*霊媒師COのコピーからガード候補でない人を消去*/
		Iterator<Agent> mediumiter = candidateMediumList.iterator();
		while(mediumiter.hasNext()) {
			rem = mediumiter.next();
			if(!guardCandidateList.contains(rem) && isAlive(rem) && candidateMediumList.contains(rem)) {
				mediumiter.remove();
			}
		}

		/*whiteListから既にいないものは消去*/
		Iterator<Agent> whiteListiter = whiteList.iterator();
		while(whiteListiter.hasNext()) {
			rem = whiteListiter.next();
			if(!guardCandidateList.contains(rem) && isAlive(rem)) {
				whiteListiter.remove();
			}
		}
		int numSeer = candidateSeerList.size(); //占い師COの中のガード候補の人数
		int numMedium = candidateMediumList.size(); //霊媒師COの中のガード候補の人数

		if(guardRequest == 1) {
			/*guardRequestがあった場合はそのエージェントを守る。guardAgentはリクエスト時に書き換え済み*/
			guardRequest = 0;
			if(isAlive(guardAgent)) {
				return guardAgent;
			}
		}
		if(numSeer == 1) {
			/*占い師CO一人の時そのエージェントを守る*/
			guardAgent = candidateSeerList.get(0);
			if(isAlive(guardAgent)) {
				return guardAgent;
			}
		}
		if(numMedium == 1) {
			/*占い師COなしor複数で霊媒師CO一人の時その人を守る*/
			guardAgent = comingoutMediumList.get(0);
			if(isAlive(guardAgent)) {
				return guardAgent;
			}
		}
		if(guardflag == 1) {
			//ガードに成功していたら同じエージェントを守る
			guardflag=0;
			if(isAlive(guardAgent)) {
				return guardAgent;
			}
		}
		if(whiteList.size() > 0){
			//whiteListにいるエージェントをランダムで守る
			guardAgent = randomSelect(whiteList);
			if(isAlive(guardAgent)) {
				return guardAgent;
			}
		}
		/*上の条件に当てはまらなかった場合ガード候補からランダムで守る*/
		while(true) {
			guardAgent = randomSelect(guardCandidateList);
			if(isAlive(guardAgent)) {
				return guardAgent;
			}
		}
	}


}
