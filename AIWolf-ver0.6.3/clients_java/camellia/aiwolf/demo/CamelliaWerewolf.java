package camellia.aiwolf.demo;

import java.util.ArrayList;
import java.util.Deque;//for fakeseer
import java.util.LinkedList;//for fakeseer
import java.util.List;
import java.util.Map;

import org.aiwolf.client.lib.AgreeContentBuilder;
import org.aiwolf.client.lib.AndContentBuilder;
import org.aiwolf.client.lib.AttackContentBuilder;
import org.aiwolf.client.lib.BecauseContentBuilder;
import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.ContentBuilder;
import org.aiwolf.client.lib.DisagreeContentBuilder;
import org.aiwolf.client.lib.DivinedResultContentBuilder;
import org.aiwolf.client.lib.EstimateContentBuilder;
import org.aiwolf.client.lib.Operator;
import org.aiwolf.client.lib.RequestContentBuilder;
import org.aiwolf.client.lib.Topic;
import org.aiwolf.client.lib.VoteContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Judge;//for fakeseer
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;
import org.aiwolf.common.data.Talk;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;
import org.aiwolf.sample.player.SampleBasePlayer;

public class CamelliaWerewolf extends SampleBasePlayer {
	Agent me;
	GameInfo currentGameInfo;


	List<Agent> werewolfList = new ArrayList<>(); 		// 人狼リスト
	List<Agent> villagerList  = new ArrayList<>(); 		// 村人リスト
	List<Agent> seerList = new ArrayList<>();			// 占い師っぽいエージェントのリスト
	List<Agent> comingoutSeerList = new ArrayList<>();	// 占い師COしたエージェントリスト
	List<Agent> estimateWerewolfList = new ArrayList<>(); // 真の人狼を人狼と推測したエージェントリスト

	List<Agent> comingoutMediumList = new ArrayList<>();// 霊媒師COしたエージェントリスト

	int talkListHead, whisperListHead;
	int pretalkListHead, prewhisperListHead;

	Agent nextAttackAgent;
	Agent possessedAgent;
	int saidAttackCandidate; // 0:発言してない, 1:発言済み, 2以上:SKIP

	Role fakeRole, fakeRoleRequested;//by keiji
	int saidCO = 0; // 0:COしてない, 1:COする, 2以上:CO済み
	boolean saidDivination = false;
	Deque<Judge> fakeDivinationQueue = new LinkedList<>();
	Agent voterequest;
	List<Agent> voterequestList = new ArrayList<>();
	Agent divinerequest;



	@Override
	public void dayStart() {
		// 前日に死亡したエージェントをリストから取り除く
//		List<Agent> deadAgents = currentGameInfo.getLastDeadAgentList();
		List<Agent> deadAgents = currentGameInfo.getAgentList();//getlastdeadagentは吊るされたエージェントは取り除かれてなかった
		deadAgents.removeAll(currentGameInfo.getAliveAgentList());
		for (Agent deadAgent : deadAgents) {
			if (villagerList.contains(deadAgent)) {
				villagerList.remove(deadAgent);
			}
			if (werewolfList.contains(deadAgent)) {
				werewolfList.remove(deadAgent);
			}
			if (seerList.contains(deadAgent)) {
				seerList.remove(deadAgent);
			}
			if (comingoutSeerList.contains(deadAgent)) {
				comingoutSeerList.remove(deadAgent);
			}
			if (estimateWerewolfList.contains(deadAgent)) {
				estimateWerewolfList.remove(deadAgent);
			}
			if (comingoutMediumList.contains(deadAgent)) {
				comingoutMediumList.remove(deadAgent);
			}
		}

		if(deadAgents.contains(possessedAgent)) {
				possessedAgent = null;
		}

		// 今日の発話リストですでに読み込んだ内容を覚えておく変数
		talkListHead = 0;
		whisperListHead = 0;
		pretalkListHead = 0;
		prewhisperListHead = 0;

		nextAttackAgent = null;
		saidAttackCandidate = 0;
		saidDivination =false;
		voterequest = null;
		voterequestList.clear();
		divinerequest = null;

		//pretalkListHead = talkListHead;

	}

	@Override
	public void finish() {

	}

	@Override
	public String getName() {
		return "CamelliaWerewolf";
	}

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		// フィールドの初期化
		me = gameInfo.getAgent();
		seerList.clear();
		comingoutSeerList.clear();
		estimateWerewolfList.clear();
		fakeDivinationQueue.clear();
		comingoutMediumList.clear();
		possessedAgent = null;

		// GameInfoから人狼のMapを取り出して, List型に変換
		Map<Agent, Role> werewolfMap = gameInfo.getRoleMap();
		werewolfList = new ArrayList<>(werewolfMap.keySet());

		// 村人リストを作成
		villagerList = new ArrayList<>(gameInfo.getAgentList());
		for (Agent werewolf : werewolfList) {
			villagerList.remove(werewolf);
		}

		saidCO = 0; fakeRole = null;
		saidDivination = false;
		voterequestList.clear();
		saidAttackCandidate = 0;

	}

	@Override
	public String talk() {
		//TODO ↓２重で書いてるでなんとかしないと
		// GameInfo.talkListからカミングアウト・占い報告・霊媒報告を抽出
		for (int i = pretalkListHead; i < currentGameInfo.getTalkList().size(); i++) {
			Talk talk = currentGameInfo.getTalkList().get(i);
			Agent talker = talk.getAgent();
			if (talker == me) {	// 発言者が自分であれば除く
				continue;
			}
			Content content = new Content(talk.getText());	// 発話をparse

			//pretalkListHead = i;

			switch (content.getTopic()) {
			case ESTIMATE:// 推測発言の処理
				// 自分または仲間人狼を疑った時 →囲みになる?
				if (content.getRole() == Role.WEREWOLF && werewolfList.contains(content.getTarget()) && saidCO == 0) {
					// 村人CO
					saidCO += 1; fakeRole = Role.VILLAGER;
					// 意見に反論
					Content reason1 = new Content(new EstimateContentBuilder(talker,Role.WEREWOLF));
					Content reason2 = new Content(new ComingoutContentBuilder(me,fakeRole));
					Content and     = new Content(new AndContentBuilder(reason1,reason2));
					Content action  = new Content(new EstimateContentBuilder(talker,Role.WEREWOLF));
					return new Content(new BecauseContentBuilder(and, action)).getText();

				// 自分を村人と言ってくれた時
				}else if(content.getRole() == Role.VILLAGER && me == content.getTarget() && saidCO == 0) {
					// 村人CO
					saidCO += 1; fakeRole = Role.VILLAGER;
					// 意見に同意
					Content reason1 = new Content(new EstimateContentBuilder(talker,Role.VILLAGER));
					Content reason2 = new Content(new ComingoutContentBuilder(me,fakeRole));
					Content and     = new Content(new AndContentBuilder(reason1,reason2));
					Content action  = new Content(new EstimateContentBuilder(talker,Role.SEER));
					return new Content(new BecauseContentBuilder(and, action)).getText();
				}
				break;
			case COMINGOUT:// 自分の役職を断定してきた時の処理
			case DIVINED:  // 占い結果報告の処理

				getDivination(content, talker);//対抗占い師の占い結果を取得

				// 自分を人狼と占った(断定した)時
				if (content.getResult() == Species.WEREWOLF && me == content.getTarget() && saidCO == 0) {
					//他の人狼が誰も占い師を騙っていない&複数人占い師がいない（狂人も騙っていない）& 3日目以内(普通それまでにCOしてるから)の時
					if (seerCOwerewolfNumber()==0 && comingoutSeerList.size() <= 1 && currentGameInfo.getDay() <= 3 ) {
						// 占い師CO
						saidCO += 1; fakeRole = Role.SEER;
					}else{
						// 村人CO
						saidCO += 1; fakeRole = Role.VILLAGER;
					}
					// 意見に反論
					Content reason1 = new Content(new DivinedResultContentBuilder(talker,me,Species.WEREWOLF));
					Content reason2 = new Content(new ComingoutContentBuilder(me,fakeRole));
					Content and     = new Content(new AndContentBuilder(reason1,reason2));
					Content action  = new Content(new EstimateContentBuilder(talker,Role.WEREWOLF));
					return new Content(new BecauseContentBuilder(and, action)).getText();

				// 自分を村人と占ってくれた（断定してくれた）時 → その占い師はたぶん狂人
				}else if(content.getResult() == Species.HUMAN && me == content.getTarget() && saidCO == 0) {
					// 村人CO
					saidCO += 1; fakeRole = Role.VILLAGER;
					// 意見に同意
					Content reason1 = new Content(new DivinedResultContentBuilder(talker,me,Species.HUMAN));
					Content reason2 = new Content(new ComingoutContentBuilder(me,fakeRole));
					Content and     = new Content(new AndContentBuilder(reason1,reason2));
					Content action  = new Content(new EstimateContentBuilder(talker,Role.SEER));
					return new Content(new BecauseContentBuilder(and, action)).getText();
				}
				break;

			case OPERATOR:
				if (content.getOperator() == Operator.REQUEST) {//リクエスト情報を得る
					for (Content c : content.getContentList()) {
						if (c.getTopic() == Topic.VOTE) {
							if(!werewolfList.contains(c.getTarget()) && talker != possessedAgent ) {//投票先が人狼でない＆発言者が裏切者でない時、投票先候補とする
								voterequestList.add(c.getTarget());
							}
							if(c.getTarget() == me) {//投票先が自分なら言い返す？kl
								Content vote = new Content(new VoteContentBuilder(talker));
								return new Content(new RequestContentBuilder(Content.ANY,vote)).getText();
							}
						}
						if (c.getTopic() == Topic.DIVINED && content.getTarget() == me) {//自分に占い依頼があったら受け入れる
							divinerequest = c.getTarget();
						}
					}
					//TODO 本当は最も投票が多いAgentを取得したい。がランダムに選択
					voterequest = GeneralFunction.randomSelect(voterequestList);
				}


			    break;
			case VOTE:
				break;
			default:
				break;
			}
		}
		pretalkListHead = currentGameInfo.getTalkList().size();

		// fakeRoleに従い、偽の役職をカミングアウトをする
		if(saidCO == 1) {// 一度だけカミングアウトさせるための処理
			if(fakeRole == Role.SEER) {
				saidCO += 1;
				return (new Content(new ComingoutContentBuilder(me, Role.SEER))).getText();
			}
		}

		// 占い師騙る時
		if (fakeRole == Role.SEER) {
			if(!saidDivination) {
				saidDivination = true;
				// 偽占い結果を報告
				if(!fakeDivinationQueue.isEmpty()) {//今までに占った結果を全て発言(*占った結果は失われる)
					Judge divination = fakeDivinationQueue.poll();
					return new Content(new DivinedResultContentBuilder(divination.getTarget(),divination.getResult())).getText();
				}else {//占い結果が取得できなかった場合
					if(null == divinerequest) {//他者から占って欲しいrequestがない場合→ランダムに村人リストから白出し
						Agent shirodashi = GeneralFunction.randomSelect(villagerList);
						if (shirodashi != null) {
							return new Content(new DivinedResultContentBuilder(GeneralFunction.randomSelect(villagerList), Species.HUMAN)).getText();
						}
					}else {//requestがある場合
						if(comingoutSeerList.contains(divinerequest)) {//対抗占い師へのrequestの場合→黒出し
							return new Content(new DivinedResultContentBuilder(divinerequest , Species.WEREWOLF)).getText();
						}else {//それ以外→白出し
							Agent shirodashi = GeneralFunction.randomSelect(villagerList);
							if (shirodashi != null) {
								return new Content(new DivinedResultContentBuilder(GeneralFunction.randomSelect(villagerList), Species.HUMAN)).getText();
							} else {
								return Content.SKIP.getText();
							}
						}
					}
				}
			}
		}

		// 人狼＋狂人の数 > 村人の数 になったら,人狼COしてPP
		if(possessedAgent != null) {
        	if(werewolfList.size() >= villagerList.size()) {
        		// 人狼CO
        		return new Content(new ComingoutContentBuilder(me, Role.WEREWOLF)).getText();
        	}
		}
		return Content.SKIP.getText();
	}

	@Override
	public void update(GameInfo gameInfo) {
		currentGameInfo = gameInfo;

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
				// 占い師カミングアウトの処理
				if(talker == content.getTarget()) { // 発言者自身のカミングアウトの処理
					if (content.getRole() == Role.SEER && !comingoutSeerList.contains(talker) && !werewolfList.contains(talker)) {
						comingoutSeerList.add(talker);
					}
					// 霊媒師カミングアウトの処理
					if (content.getRole() == Role.MEDIUM && !comingoutMediumList.contains(talker) && !werewolfList.contains(talker)) {
						comingoutMediumList.add(talker);
					}
				}
				break;
			case ESTIMATE:
				// 推測発言の処理
				// 真の人狼を人狼だと発言(ESTIMATE)したエージェントのリスト
				if (content.getRole() == Role.WEREWOLF && werewolfList.contains(content.getTarget())) {
					if (!estimateWerewolfList.contains(talker)) {
						estimateWerewolfList.add(talker);
					}
				}
				break;
			case DIVINED:
				// 占い結果報告発話の処理
				// 人狼を人狼と占ったとき -> 占い師の可能性大
				if (content.getResult() == Species.WEREWOLF && werewolfList.contains(content.getTarget())
				 && villagerList.contains(talker) ){
					if (!seerList.contains(talker)) {
						seerList.add(talker);
					}
				}
				// 人狼を村人と占ったとき -> 狂人の可能性大 -> 狂人確定とする || 村人を人狼と占うのも狂人の可能性大
				if (((content.getResult() == Species.HUMAN && werewolfList.contains(content.getTarget()))
				  || (content.getResult() == Species.WEREWOLF && villagerList.contains(content.getTarget())))
				  && villagerList.contains(talker)) {
					// 占い師を見つけるためのリストから取り除く
					seerList.remove(talker);
					comingoutSeerList.remove(talker);
					estimateWerewolfList.remove(talker);
//					villagerList.remove(talker);	// 村人リストからも取り除く
					possessedAgent = talker;	// 狂人エージェントとして記憶
				}
				break;
			case IDENTIFIED:
				// 人狼を村人と霊媒したとき -> 狂人の可能性大 -> 狂人確定とする
				if (((content.getResult() == Species.HUMAN && werewolfList.contains(content.getTarget()))
				  || (content.getResult() == Species.WEREWOLF && villagerList.contains(content.getTarget())))
				  && villagerList.contains(talker)) {
					// 占い師を見つけるためのリストから取り除く
					comingoutMediumList.remove(talker);
					possessedAgent = talker;	// 狂人エージェントとして記憶
				}
				break;
			default:
				break;
			}
		}
		pretalkListHead = talkListHead;
		talkListHead = currentGameInfo.getTalkList().size();

		// GameInfo.whisperListから発言を取り出す
		for (int i = whisperListHead; i < currentGameInfo.getWhisperList().size(); i++) {

		}
		prewhisperListHead = whisperListHead;
		whisperListHead = currentGameInfo.getWhisperList().size();
	}

	@Override
	public Agent vote() {
		if (voterequest != null) {
			return voterequest;
		}
		// 村人リストからランダムに選択
		return GeneralFunction.randomSelect(villagerList);
	}

	@Override
	public Agent attack() {
		if (nextAttackAgent != null) {
			return nextAttackAgent;
		}
		// 村人リストからランダムに選択
		return GeneralFunction.randomSelect(villagerList);
	}

	@Override
	public String whisper() {
		// 今日の昼に追放されたエージェントを村人リストから取り除く
		Agent executedAgent = currentGameInfo.getLatestExecutedAgent();
		if (villagerList.contains(executedAgent)) {
			villagerList.remove(executedAgent);
		}

		// 誰を襲撃したいか
		if (saidAttackCandidate == 0) {//発言してない
			saidAttackCandidate++;//発言済み
			if(attackCandidate() != null) {
				nextAttackAgent = attackCandidate();
				ContentBuilder builder = new AttackContentBuilder(nextAttackAgent);
				return new Content(builder).getText();
			}

		}

		if (saidAttackCandidate == 1) {//発言済みなら
			saidAttackCandidate++;
			//襲撃失敗してたら、襲撃投票先を合わせるようにリクエストする
			if(GeneralFunction.isAlive(currentGameInfo,currentGameInfo.getAttackedAgent())) {
				Content nextattack = new Content(new AttackContentBuilder(nextAttackAgent));
				return new Content(new RequestContentBuilder(Content.ANY,nextattack)).getText();
			}
		}

		// 騙る事の事前報告 → ComingoutContentBuilderで相手に伝わるか分からないが
		if(saidCO <= 2){// 何度もカミングアウトしないために
			saidCO++;
			if (fakeRole == Role.VILLAGER ) {
				//村人を騙ります
				return new Content(new ComingoutContentBuilder(me, Role.VILLAGER)).getText();
			}else if(fakeRole == Role.SEER ) {
				//占い師を騙ります
				return new Content(new ComingoutContentBuilder(me, Role.SEER)).getText();
			}
		}

		// GameInfo.whisperListから発言を取り出す
		for (int i = prewhisperListHead; i < currentGameInfo.getWhisperList().size(); i++) {
			Talk talk = currentGameInfo.getWhisperList().get(i);
			Agent talker = talk.getAgent();
			if (talker == me) {	// 発言者が自分であれば除く
				continue;
			}
			Content content = new Content(talk.getText());	// 発話をparse
			switch (content.getTopic()) {
			case COMINGOUT://ここでは騙る意味になる
				// List or MAP で記憶?
				break;
			case ATTACK://襲撃宣言を得る
				if(content.getTarget() == attackCandidate()) {//襲撃対象が同じなら同意
					if(content.getTalkType() != null && content.getTalkDay() != -1 &&  content.getTalkID() != -1) {
					return new Content(new AgreeContentBuilder(content.getTalkType(), content.getTalkDay(), content.getTalkID())).getText();
					}
				}else{//襲撃対象が違うなら反論
					if(content.getTalkType() != null && content.getTalkDay() != -1 &&  content.getTalkID() != -1) {
					return new Content(new DisagreeContentBuilder(content.getTalkType(), content.getTalkDay(), content.getTalkID())).getText();
					}
				}
				//break;//到達不能なので
			case OPERATOR://リクエスト情報を得る
				if (content.getOperator() == Operator.REQUEST && content.getTarget() == me) {
					for (Content c : content.getContentList()) {
						if (c.getTopic() == Topic.COMINGOUT) {//騙って欲しいRoleを得る
							fakeRoleRequested = c.getRole();
						}
					}
				}
				break;
			default:
				break;
			}
		}
		prewhisperListHead = currentGameInfo.getWhisperList().size();

		return Content.SKIP.getText();
	}

	// 次の襲撃対象候補を返す
	Agent attackCandidate() {
		if(GeneralFunction.isAlive( currentGameInfo,currentGameInfo.getAttackedAgent())) {//襲撃失敗したら、ランダム系
			if((comingoutMediumList.size()==1 && comingoutSeerList.size()==1) || (comingoutMediumList.size()>=2 && comingoutSeerList.size()<=1)) {
				// 霊媒師カミングアウトしているエージェントを選択
				Agent mediumCandidate = GeneralFunction.randomSelect(comingoutMediumList);
				if(mediumCandidate != null) {
					return mediumCandidate;
				}
			}else if(comingoutSeerList.size()>=2) {
				// 占い師っぽいエージェントを選択
				Agent seerCandidate = GeneralFunction.randomSelect(seerList);
				if (seerCandidate != null) {
					return seerCandidate;
				}

				// 占い師カミングアウトしているエージェントを選択
				Agent seerComingoutAgent = GeneralFunction.randomSelect(comingoutSeerList);
				if (seerComingoutAgent != null) {
					return seerComingoutAgent;
				}
			}else if(comingoutMediumList.size()==1 || comingoutSeerList.size()==1) {
				// 村人からランダム
				return GeneralFunction.randomSelect(villagerList);
			}

		}else {//襲撃成功したら、占い師狙い系

			// 占い師っぽいエージェントを選択
			Agent seerCandidate = GeneralFunction.randomSelect(seerList);
			if (seerCandidate != null) {
				return seerCandidate;
			}

			// 占い師カミングアウトしているエージェントを選択
			Agent seerComingoutAgent = GeneralFunction.randomSelect(comingoutSeerList);
			if (seerComingoutAgent != null) {
				return seerComingoutAgent;
			}

			// 真の人狼を人狼だと発言(ESTIMATE)したエージェント
			Agent estimateWerewolfAgent = GeneralFunction.randomSelect(estimateWerewolfList);
			if(estimateWerewolfAgent != null) {
				return estimateWerewolfAgent;
			}

			// 霊媒師カミングアウトしているエージェントを選択
			Agent mediumCandidate = GeneralFunction.randomSelect(comingoutMediumList);
			if(mediumCandidate != null) {
				return mediumCandidate;
			}
		}
		// 村人からランダム
		return GeneralFunction.randomSelect(villagerList);
	}

	// 占い師を騙った（talkで占い師とCOした）人狼の数を返す keiji
	int seerCOwerewolfNumber() {
		List<Agent> seerCOwerewolfList = (new ArrayList<>(werewolfList));
		seerCOwerewolfList.retainAll(comingoutSeerList);//werewolfList と comingoutSeerListの重複を探す
		return seerCOwerewolfList.size();
	}

	// 対抗占い師の占い結果を取得しDequeに追加 keiji
	// content:占いの文章, talker:主語(話者)のAgent
	void getDivination(Content content, Agent talker) {
		if(content.getResult() != null) {//占い結果の場合、Speciesを返すため
			// 偽占い師が対抗占い師の占い結果を取得するため
			Judge divination = new Judge(content.getDay(),talker,content.getTarget(),content.getResult());
			if(divination != null) {
				if(divination.getResult() != Species.WEREWOLF) {// 人狼が占われた時は真似をしない
					fakeDivinationQueue.offerFirst(divination);//offer(末尾)ではなく、offerFirst（先頭）に追加
				}
			}
		}
	}

}