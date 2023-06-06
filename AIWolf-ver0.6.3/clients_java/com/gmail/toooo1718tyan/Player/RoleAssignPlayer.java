package com.gmail.toooo1718tyan.Player;

import com.gmail.toooo1718tyan.Player15.Tomato15Bodyguard;
import com.gmail.toooo1718tyan.Player15.Tomato15Medium;
import com.gmail.toooo1718tyan.Player15.Tomato15Possessed;
import com.gmail.toooo1718tyan.Player15.Tomato15Seer;
import com.gmail.toooo1718tyan.Player15.Tomato15Villager;
import com.gmail.toooo1718tyan.Player15.Tomato15Werewolf;
import com.gmail.toooo1718tyan.Player5.Tomato5Bodyguard;
import com.gmail.toooo1718tyan.Player5.Tomato5Medium;
import com.gmail.toooo1718tyan.Player5.Tomato5Possessed;
import com.gmail.toooo1718tyan.Player5.Tomato5Seer;
import com.gmail.toooo1718tyan.Player5.Tomato5Villager;
import com.gmail.toooo1718tyan.Player5.Tomato5Werewolf;

public class RoleAssignPlayer extends ExtendedAbstractRoleAssignPlayer {


	public RoleAssignPlayer() {
		setVillagerPlayer5(new Tomato5Villager());
		setBodyguardPlayer5(new Tomato5Bodyguard());
		setMediumPlayer5(new Tomato5Medium());
		setSeerPlayer5(new Tomato5Seer());
		setPossessedPlayer5(new Tomato5Possessed());
		setWerewolfPlayer5(new Tomato5Werewolf());

		setVillagerPlayer15(new Tomato15Villager());
		setBodyguardPlayer15(new Tomato15Bodyguard());
		setMediumPlayer15(new Tomato15Medium());
		setSeerPlayer15(new Tomato15Seer());
		setPossessedPlayer15(new Tomato15Possessed());
		setWerewolfPlayer15(new Tomato15Werewolf());
	}

	public String getName() {
		return "Tomato";
	}


}
