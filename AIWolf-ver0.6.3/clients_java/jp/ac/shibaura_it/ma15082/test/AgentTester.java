package jp.ac.shibaura_it.ma15082.test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import jp.ac.shibaura_it.ma15082.player.RandomPlayer;
import jp.ac.shibaura_it.ma15082.player.WasabiPlayer;
import jp.ac.shibaura_it.ma15082.player.WasabiRoleAssignPlayer;

import org.aiwolf.common.data.Player;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Team;
import org.aiwolf.common.net.GameSetting;
import org.aiwolf.server.AIWolfGame;
import org.aiwolf.server.net.DirectConnectServer;

public class AgentTester {

	public static void main(String[] args) throws InstantiationException, IllegalAccessException {

		int villagerWinNum = 0;
		int werewolfWinNum = 0;

		Player player = new WasabiRoleAssignPlayer();

		Class<Player> pcls = (Class<Player>) player.getClass();
		for (int j = 0; j < 100; j++) {
			for (Role requestRole : Role.values()) {
				if (requestRole == Role.FREEMASON) {
					continue;
				}

				player = pcls.newInstance();

				Map<Player, Role> playerMap = new HashMap<Player, Role>();
				playerMap.put(player, requestRole);
				for (int i = 0; i < 14; i++) {
					playerMap.put(new RandomPlayer(), null);
				}

				DirectConnectServer gameServer = new DirectConnectServer(playerMap);

				GameSetting gameSetting = GameSetting.getDefaultGame(playerMap.size());
				AIWolfGame game = new AIWolfGame(gameSetting, gameServer);
				game.setRand(new Random());
				game.setShowConsoleLog(false);
				game.start();

				Team team = game.getWinner();
				if (team == Team.VILLAGER) {
					villagerWinNum++;
				} else {
					werewolfWinNum++;
				}

			}
		}

		System.out.println("Villager:" + villagerWinNum + " Werewolf:" + werewolfWinNum);

	}
}