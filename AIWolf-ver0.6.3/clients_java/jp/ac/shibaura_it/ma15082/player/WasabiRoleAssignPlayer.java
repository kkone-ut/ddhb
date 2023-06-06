package jp.ac.shibaura_it.ma15082.player;

import org.aiwolf.common.data.Player;
import org.aiwolf.common.data.Team;
import org.aiwolf.sample.lib.AbstractRoleAssignPlayer;

import jp.ac.shibaura_it.ma15082.Pair;
import jp.ac.shibaura_it.ma15082.PersonalityFactory;

public class WasabiRoleAssignPlayer extends AbstractRoleAssignPlayer {

	public WasabiRoleAssignPlayer() {
		PersonalityFactory.setPersonalityRange(Team.VILLAGER, new Pair<Double, Double>(0.2, 0.8),
				new Pair<Double, Double>(0.2, 0.8), new Pair<Double, Double>(0.2, 0.8),
				new Pair<Double, Double>(0.2, 0.8), new Pair<Double, Double>(0.2, 0.8));
		PersonalityFactory.setPersonalityRange(Team.WEREWOLF, new Pair<Double, Double>(0.2, 0.8),
				new Pair<Double, Double>(0.2, 0.8), new Pair<Double, Double>(0.2, 0.8),
				new Pair<Double, Double>(0.2, 0.8), new Pair<Double, Double>(0.2, 0.8));

		/*
		 * Pair<Double,Double> r=new Pair<Double,Double>(0.3,0.7);
		 * PersonalityFactory.setPersonalityRange(Team.VILLAGER,r,r,r,r,r);
		 * PersonalityFactory.setPersonalityRange(Team.WEREWOLF,r,r,r,r,r);
		 */

		/*
		 * setVillagerPlayer(new SafePlayer(WasabiPlayer.class)); setBodyguardPlayer(new
		 * SafePlayer(WasabiPlayer.class)); setMediumPlayer(new
		 * SafePlayer(WasabiPlayer.class)); setPossessedPlayer(new
		 * SafePlayer(WasabiPlayer.class)); setSeerPlayer(new
		 * SafePlayer(WasabiPlayer.class)); setWerewolfPlayer(new
		 * SafePlayer(WasabiPlayer.class));
		 */

		Player wasabi = new SafePlayer(WasabiPlayer.class);
		// AbstractVillager wasabi=new SafePlayer();
		// wasabi=new WasabiPlayer();

		// setVillagerPlayer(new WasabiVillager(wasabi));
		// setBodyguardPlayer(new WasabiBodyguard(wasabi));
		// setMediumPlayer(new WasabiMedium(wasabi));
		// setPossessedPlayer(new WasabiPossessed(wasabi));
		// setSeerPlayer(new WasabiSeer(wasabi));
		// setWerewolfPlayer(new WasabiWerewolf(wasabi));

		setVillagerPlayer((wasabi));
		setBodyguardPlayer((wasabi));
		setMediumPlayer((wasabi));
		setPossessedPlayer((wasabi));
		setSeerPlayer((wasabi));
		setWerewolfPlayer((wasabi));

	}

	@Override
	public String getName() {
		return "wasabi";
	}

}
