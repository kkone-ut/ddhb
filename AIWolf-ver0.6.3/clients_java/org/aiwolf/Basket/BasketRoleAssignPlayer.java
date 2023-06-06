package org.aiwolf.Basket;

import org.aiwolf.sample.lib.AbstractRoleAssignPlayer;

public class BasketRoleAssignPlayer extends AbstractRoleAssignPlayer {

	public BasketRoleAssignPlayer() {
		setVillagerPlayer(new BasketVillager());
		setBodyguardPlayer(new BasketBodyguard());
		setMediumPlayer(new BasketMedium());
		setSeerPlayer(new BasketSeer());
		setPossessedPlayer(new BasketPossessed());
		setWerewolfPlayer(new BasketWerewolf());
	} 

	@Override
	public String getName() {
		return "Basket";
	}
}
