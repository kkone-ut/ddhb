package jp.ac.shibaura_it.ma15082.test;

import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import jp.ac.shibaura_it.ma15082.player.WasabiRoleAssignPlayer;

import org.aiwolf.common.data.Player;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Team;
import org.aiwolf.common.net.GameSetting;
import org.aiwolf.sample.player.SampleRoleAssignPlayer;
import org.aiwolf.server.AIWolfGame;
import org.aiwolf.server.net.DirectConnectServer;
import org.aiwolf.server.net.GameServer;

public class Starter {
	static protected int PLAYER_NUM = 15;
	static protected int GAME_NUM = 1000;

	static protected String path = "jtest/";

	public static void main(String[] args) {
		int villagerWinNum = 0;
		int werewolfWinNum = 0;

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		System.out.println("start");

		long begin = System.currentTimeMillis();
		try {
			List<Player> playerList = new ArrayList<Player>();
			for (int j = 0; j < PLAYER_NUM; j++) {
				// playerList.add(new WasabiPlayer());
				if (j % 2 == 0) {
					playerList.add(new WasabiRoleAssignPlayer());
				} else {
					playerList.add(new SampleRoleAssignPlayer());
				}
			}

			GameServer gameServer = new DirectConnectServer(playerList);
			GameSetting gameSetting = GameSetting.getDefaultGame(PLAYER_NUM);
			AIWolfGame game = new AIWolfGame(gameSetting, gameServer);
			game.setRand(new Random(gameSetting.getRandomSeed()));
			game.setShowConsoleLog(false);
			// Calendar c=Calendar.getInstance();
			// File logFile =new File(path+sdf.format(c.getTime())+".log");
			// game.setLogFile(logFile);

			for (int i = 0; i < GAME_NUM; i++) {
				System.gc();
				game.start();
				if (game.getWinner() == Team.VILLAGER) {
					villagerWinNum++;
				} else {
					werewolfWinNum++;
				}
				// System.out.println();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println((System.currentTimeMillis() - begin) / 1000.0 + "[s]");
		System.out.println("Villager:" + villagerWinNum + " Werewolf:" + werewolfWinNum);

	}
}
