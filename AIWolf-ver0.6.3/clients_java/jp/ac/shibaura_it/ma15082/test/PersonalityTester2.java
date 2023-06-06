package jp.ac.shibaura_it.ma15082.test;

import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import jp.ac.shibaura_it.ma15082.ListMap;
import jp.ac.shibaura_it.ma15082.Pair;
import jp.ac.shibaura_it.ma15082.Personality;
import jp.ac.shibaura_it.ma15082.PersonalityFactory;
import jp.ac.shibaura_it.ma15082.Tools;
import jp.ac.shibaura_it.ma15082.player.WasabiPlayer;
import jp.ac.shibaura_it.ma15082.player.WasabiRoleAssignPlayer;

import org.aiwolf.common.data.Player;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Team;
import org.aiwolf.common.net.GameSetting;
import org.aiwolf.server.AIWolfGame;
import org.aiwolf.server.net.DirectConnectServer;
import org.aiwolf.server.net.GameServer;

public class PersonalityTester2 {
	static protected int PLAYER_NUM = 15;
	static protected int GAME_NUM = 5000;
	// 1 42[s]
	// 10 426[s]
	// 100 71[min]
	// 1000 12[h]
	// 5000 60[h]
	// 10000 118[h]

	static protected String path = "jtest/";

	public static void main(String[] args) {
		int villagerWinNum = 0;
		int werewolfWinNum = 0;

		PrintWriter pw = null;
		try {
			pw = new PrintWriter(path + "xxxx.csv");
		} catch (Exception e) {
			e.printStackTrace();
		}

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");

		long begin = System.currentTimeMillis();
		try {

			for (Role role : Role.values()) {
				if (role.equals(Role.FREEMASON)) {
					continue;
				}
				ListMap<Player, Role> playerMap = new ListMap<Player, Role>();
				playerMap.add(new WasabiPlayer(), role);
				for (int j = 1; j < PLAYER_NUM; j++) {
					playerMap.add(new WasabiPlayer(), null);
				}

				// Calendar c=Calendar.getInstance();
				// File logFile =new File(path+sdf.format(c.getTime())+".log");
				GameServer gameServer = new DirectConnectServer(playerMap);
				GameSetting gameSetting = GameSetting.getDefaultGame(PLAYER_NUM);
				AIWolfGame game = new AIWolfGame(gameSetting, gameServer);
				game.setRand(new Random(gameSetting.getRandomSeed()));
				game.setShowConsoleLog(false);
				// game.setLogFile(logFile);

				List<Pair<Double, Double>> aaa = new ArrayList<Pair<Double, Double>>();
				for (int j = 0; j < 5; j++) {
					aaa.add(null);
				}

				for (int k = 0; k < 55; k++) {
					int temp = k;
					int p = temp % 11;
					int q = temp / 11;
					for (int j = 0; j < 5; j++) {
						if (j == q) {
							aaa.set(j, new Pair<Double, Double>(p * 0.1, p * 0.1));
						} else {
							aaa.set(j, null);
						}
					}
					PersonalityFactory.initAll();

					PersonalityFactory.setPersonalityRange(role, aaa.get(0), aaa.get(1), aaa.get(2), aaa.get(3),
							aaa.get(4));

					for (int i = 0; i < GAME_NUM; i++) {
						game.start();
						if (game.getWinner() == Team.VILLAGER) {
							villagerWinNum++;
						} else {
							werewolfWinNum++;
						}
					}

					int t_w = 0;
					int t_n = 0;
					List<Player> playerList = playerMap.keyList();
					for (int i = 0; i < playerList.size(); i++) {
						WasabiPlayer wp = (WasabiPlayer) (playerList.get(i));

						int win = wp.getWinNum(role);
						int num = wp.getGameNum(role);
						if (wp.getAgent().getAgentIdx() == 1) {
							t_w = win;
							t_n = num;
						}
						wp.setDataMap(null);
					}
					pw.println(role + "," + q + "," + p + "," + t_w + "," + t_n + "," + t_w / (double) t_n);
					pw.flush();

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println((System.currentTimeMillis() - begin) / 1000.0 + "[s]");
		System.out.println("Villager:" + villagerWinNum + " Werewolf:" + werewolfWinNum);
		pw.close();

	}
}
