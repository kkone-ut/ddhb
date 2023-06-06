package jp.ac.shibaura_it.ma15082.test;

import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Random;

import jp.ac.shibaura_it.ma15082.Pair;
import jp.ac.shibaura_it.ma15082.Personality;
import jp.ac.shibaura_it.ma15082.PersonalityFactory;
import jp.ac.shibaura_it.ma15082.player.WasabiPlayer;
import jp.ac.shibaura_it.ma15082.player.WasabiRoleAssignPlayer;

import org.aiwolf.common.data.Player;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Team;
import org.aiwolf.common.net.GameSetting;
import org.aiwolf.server.AIWolfGame;
import org.aiwolf.server.net.DirectConnectServer;
import org.aiwolf.server.net.GameServer;
import org.aiwolf.server.net.TcpipServer;

public class PersonalityTester {
	static protected int PLAYER_NUM = 15;
	static protected int GAME_NUM = 100;
	// 1:14[s]
	// 10:135[s];
	// 100:20[min]
	// 1000:3.6[h]
	// 10000:36[h]
	static protected String path = "jtest/";

	public static void main(String[] args) {
		int villagerWinNum = 0;
		int werewolfWinNum = 0;

		/*
		 * PrintWriter pw=null; try{ pw=new PrintWriter(path+"a.csv"); }catch(Exception
		 * e){ e.printStackTrace(); }
		 */

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");

		long begin = System.currentTimeMillis();
		try {

			List<Player> playerList = new ArrayList<Player>();
			for (int j = 0; j < PLAYER_NUM; j++) {
				playerList.add(new WasabiPlayer());
			}
			Calendar c = Calendar.getInstance();
			File logFile = new File(path + sdf.format(c.getTime()) + ".log");
			GameServer gameServer = new DirectConnectServer(playerList);
			GameSetting gameSetting = GameSetting.getDefaultGame(PLAYER_NUM);

			AIWolfGame game = new AIWolfGame(gameSetting, gameServer);
			game.setRand(new Random(gameSetting.getRandomSeed()));
			game.setShowConsoleLog(false);
			// game.setLogFile(logFile);
			PrintWriter pw = new PrintWriter(path + "sss.csv");
			List<Pair<Double, Double>> aaa = new ArrayList<Pair<Double, Double>>();
			Team team;
			for (int j = 0; j < 5; j++) {
				aaa.add(null);
			}

			for (int k = 0; k < 110; k++) {
				int temp = k;
				if (k > 54) {
					temp -= 55;
					team = (Team.WEREWOLF);
				} else {
					team = (Team.VILLAGER);
				}
				int p = temp % 11;
				int q = temp / 11;
				for (int j = 0; j < 5; j++) {
					if (j == q) {
						aaa.set(j, new Pair<Double, Double>(p * 0.1, p * 0.1));
					} else {
						aaa.set(j, null);
					}
				}
				// PersonalityFactory.setPersonality(team,aaa.get(0),
				// aaa.get(1), aaa.get(2), aaa.get(3), aaa.get(4));
				PersonalityFactory.setPersonalityRange(Team.VILLAGER, null, null, null, null,
						new Pair<Double, Double>(0.9, 1.0));
				PersonalityFactory.setPersonalityRange(Team.WEREWOLF, null, null, null, null,
						new Pair<Double, Double>(0.9, 1.0));

				if (aaa.get(4) != null) {
					PersonalityFactory.setPersonalityRange(team, aaa.get(0), aaa.get(1), aaa.get(2), aaa.get(3),
							aaa.get(4));
				} else {
					PersonalityFactory.setPersonalityRange(team, aaa.get(0), aaa.get(1), aaa.get(2), aaa.get(3),
							new Pair<Double, Double>(0.9, 1.0));
				}

				for (int i = 0; i < GAME_NUM; i++) {
					game.start();
					if (game.getWinner() == Team.VILLAGER) {
						villagerWinNum++;
					} else {
						werewolfWinNum++;
					}
				}
				pw.println(PersonalityFactory.getSetting(team) + villagerWinNum);
				pw.flush();

				villagerWinNum = 0;
				werewolfWinNum = 0;

				/*
				 * int win=game.getWinner()==Team.VILLAGER?1:0; for(Player temp : playerList){
				 * WasabiPlayer wp=(WasabiPlayer)temp; Personality p=wp.getPersonality(); Role
				 * role=wp.getRole(); pw.println(role+","+p.openness_to_experimence+","+p.
				 * conscientiousness+","+p.extroversion+","+p.agreeableness+","+
				 * p.neuroticism+","+win); }
				 */

			}

			pw.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println((System.currentTimeMillis() - begin) / 1000.0 + "[s]");
		System.out.println("Villager:" + villagerWinNum + " Werewolf:" + werewolfWinNum);
		// pw.close();

	}
}
