package com.gmail.toooo1718tyan.Player;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Player;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

import com.gmail.toooo1718tyan.Estimator.FeatureCalclation;

public class ExtendedAbstractRoleAssignPlayer implements Player {

	private Player villagerPlayer5;
	private Player villagerPlayer15;
	private Player seerPlayer5;
	private Player seerPlayer15;
	private Player mediumPlayer5;
	private Player mediumPlayer15;
	private Player bodyguardPlayer5;
	private Player bodyguardPlayer15;
	private Player possessedPlayer5;
	private Player possessedPlayer15;
	private Player werewolfPlayer5;
	private Player werewolfPlayer15;

	private Player rolePlayer;

	public final Player getVillagerPlayer5() {
		return villagerPlayer5;
	}

	public final void setVillagerPlayer5(Player villagerPlayer5) {
		this.villagerPlayer5 = villagerPlayer5;
	}

	public final Player getVillagerPlayer15() {
		return villagerPlayer15;
	}

	public final void setVillagerPlayer15(Player villagerPlayer15) {
		this.villagerPlayer15 = villagerPlayer15;
	}

	public final Player getSeerPlayer5() {
		return seerPlayer5;
	}

	public final void setSeerPlayer5(Player seerPlayer5) {
		this.seerPlayer5 = seerPlayer5;
	}

	public final Player getSeerPlayer15() {
		return seerPlayer15;
	}

	public final void setSeerPlayer15(Player seerPlayer15) {
		this.seerPlayer15 = seerPlayer15;
	}

	public final Player getMediumPlayer5() {
		return mediumPlayer5;
	}

	public final void setMediumPlayer5(Player mediumPlayer5) {
		this.mediumPlayer5 = mediumPlayer5;
	}

	public final Player getMediumPlayer15() {
		return mediumPlayer15;
	}

	public final void setMediumPlayer15(Player mediumPlayer15) {
		this.mediumPlayer15 = mediumPlayer15;
	}

	public final Player getBodyguardPlayer5() {
		return bodyguardPlayer5;
	}

	public final void setBodyguardPlayer5(Player bodyGuardPlayer5) {
		this.bodyguardPlayer5 = bodyGuardPlayer5;
	}

	public final Player getBodyguardPlayer15() {
		return bodyguardPlayer15;
	}

	public final void setBodyguardPlayer15(Player bodyGuardPlayer15) {
		this.bodyguardPlayer15 = bodyGuardPlayer15;
	}

	public final Player getPossessedPlayer5() {
		return possessedPlayer5;
	}

	public final void setPossessedPlayer5(Player possesedPlayer5) {
		this.possessedPlayer5 = possesedPlayer5;
	}

	public final Player getPossessedPlayer15() {
		return possessedPlayer15;
	}

	public final void setPossessedPlayer15(Player possesedPlayer15) {
		this.possessedPlayer15 = possesedPlayer15;
	}

	public final Player getWerewolfPlayer5() {
		return werewolfPlayer5;
	}

	public final void setWerewolfPlayer5(Player werewolfPlayer5) {
		this.werewolfPlayer5 = werewolfPlayer5;
	}

	public final Player getWerewolfPlayer15() {
		return werewolfPlayer15;
	}

	public final void setWerewolfPlayer15(Player werewolfPlayer15) {
		this.werewolfPlayer15 = werewolfPlayer15;
	}


	//---------------------------------------------------------

	@Override
	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
		try {
			Role myRole = gameInfo.getRole();
			int playerNum = gameSetting.getPlayerNum();
			FeatureCalclation.initFeature(gameInfo);
			switch(myRole) {
			case VILLAGER:
				rolePlayer = (playerNum == 5) ? villagerPlayer5 : villagerPlayer15;
				break;
			case SEER:
				rolePlayer = (playerNum == 5) ? seerPlayer5 : seerPlayer15;
				break;
			case MEDIUM:
				rolePlayer = (playerNum == 5) ? mediumPlayer5 : mediumPlayer15;
				break;
			case BODYGUARD:
				rolePlayer = (playerNum == 5) ? bodyguardPlayer5 : bodyguardPlayer15;
				break;
			case POSSESSED:
				rolePlayer = (playerNum == 5) ? possessedPlayer5 : possessedPlayer15;
				break;
			case WEREWOLF:
				rolePlayer = (playerNum == 5) ? werewolfPlayer5 : werewolfPlayer15;
				break;
			default:
				rolePlayer = (playerNum == 5) ? villagerPlayer5 : villagerPlayer15;
				break;
			}
			rolePlayer.initialize(gameInfo, gameSetting);
		} catch (Exception e) {

		}
	}

	@Override
	public void update(GameInfo gameInfo) {
		try {
			rolePlayer.update(gameInfo);
		} catch (Exception e) {

		}
	}

	@Override
	public void dayStart() {
		try {
			rolePlayer.dayStart();
		} catch (Exception e) {

		}
	}

	@Override
	public String talk() {
		String res = null;
		try {
			res = rolePlayer.talk();
		} catch (Exception e) {

		}
		return res;
	}

	@Override
	public String whisper() {
		String res = null;
		try {
			res = rolePlayer.whisper();
		} catch (Exception e) {

		}
		return res;
	}

	@Override
	public Agent vote() {
		Agent res = null;
		try {
			res = rolePlayer.vote();
		} catch (Exception e) {

		}
		return res;
	}

	@Override
	public Agent attack() {
		Agent res = null;
		try {
			res = rolePlayer.attack();
		} catch (Exception e) {

		}
		return res;
	}

	@Override
	public Agent divine() {
		Agent res = null;
		try {
			res = rolePlayer.divine();
		} catch (Exception e) {

		}
		return res;
	}

	@Override
	public Agent guard() {
		Agent res = null;
		try {
			res = rolePlayer.guard();
		} catch (Exception e) {

		}
		return res;
	}

	@Override
	public void finish() {
		try {
			rolePlayer.finish();
		} catch (Exception e) {

		}
	}

	public String getName() {
		return "Tomato";
	}


	//------------------------------------------------------

//	@Override
//	public void initialize(GameInfo gameInfo, GameSetting gameSetting) {
////		try {
//			Role myRole = gameInfo.getRole();
//			int playerNum = gameSetting.getPlayerNum();
//			FeatureCalclation.initFeature(gameInfo);
//			switch(myRole) {
//			case VILLAGER:
//				rolePlayer = (playerNum == 5) ? villagerPlayer5 : villagerPlayer15;
//				break;
//			case SEER:
//				rolePlayer = (playerNum == 5) ? seerPlayer5 : seerPlayer15;
//				break;
//			case MEDIUM:
//				rolePlayer = (playerNum == 5) ? mediumPlayer5 : mediumPlayer15;
//				break;
//			case BODYGUARD:
//				rolePlayer = (playerNum == 5) ? bodyguardPlayer5 : bodyguardPlayer15;
//				break;
//			case POSSESSED:
//				rolePlayer = (playerNum == 5) ? possessedPlayer5 : possessedPlayer15;
//				break;
//			case WEREWOLF:
//				rolePlayer = (playerNum == 5) ? werewolfPlayer5 : werewolfPlayer15;
//				break;
//			default:
//				rolePlayer = (playerNum == 5) ? villagerPlayer5 : villagerPlayer15;
//				break;
//			}
//			rolePlayer.initialize(gameInfo, gameSetting);
////		} catch (Exception e) {
////
////		}
//	}
//
//	@Override
//	public void update(GameInfo gameInfo) {
////		try {
//			rolePlayer.update(gameInfo);
////		} catch (Exception e) {
////
////		}
//	}
//
//	@Override
//	public void dayStart() {
////		try {
//			rolePlayer.dayStart();
////		} catch (Exception e) {
////
////		}
//	}
//
//	@Override
//	public String talk() {
//		String res = null;
////		try {
//			res = rolePlayer.talk();
////		} catch (Exception e) {
////
////		}
//		return res;
//	}
//
//	@Override
//	public String whisper() {
//		String res = null;
////		try {
//			res = rolePlayer.whisper();
////		} catch (Exception e) {
////
////		}
//		return res;
//	}
//
//	@Override
//	public Agent vote() {
//		Agent res = null;
////		try {
//			res = rolePlayer.vote();
////		} catch (Exception e) {
////
////		}
//		return res;
//	}
//
//	@Override
//	public Agent attack() {
//		Agent res = null;
////		try {
//			res = rolePlayer.attack();
////		} catch (Exception e) {
////
////		}
//		return res;
//	}
//
//	@Override
//	public Agent divine() {
//		Agent res = null;
////		try {
//			res = rolePlayer.divine();
////		} catch (Exception e) {
////
////		}
//		return res;
//	}
//
//	@Override
//	public Agent guard() {
//		Agent res = null;
////		try {
//			res = rolePlayer.guard();
////		} catch (Exception e) {
////
////		}
//		return res;
//	}
//
//	@Override
//	public void finish() {
////		try {
//			rolePlayer.finish();
////		} catch (Exception e) {
////
////		}
//	}
//
//	public String getName() {
//		return "Tomato";
//	}

}
