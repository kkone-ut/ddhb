package jp.ac.shibaura_it.ma15082.player;

import java.util.EnumMap;

import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.SkipContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Player;
import org.aiwolf.common.net.GameInfo;
import org.aiwolf.common.net.GameSetting;

import jp.ac.shibaura_it.ma15082.Method;

public class SafePlayer implements Player {

	private final Class<? extends Player> playerClass;
	private Player rolePlayer;
	private final String teamName;
	private final boolean showErrorLog;
	private final EnumMap<Method, Double> thMap;

	private long begin;
	private long all;
	private long max;

	public SafePlayer(Class<? extends Player> p) {
		this(p, null);
	}

	public SafePlayer(Player player) {
		this(null, player);
	}

	private SafePlayer(Class<? extends Player> p, Player player) {
		playerClass = p;
		rolePlayer = player;
		teamName = "wasabi";
		showErrorLog = true;
		thMap = new EnumMap<Method, Double>(Method.class);
		for (Method m : Method.values()) {
			setThreshold(m, null);
		}
		setThreshold(Method.UPDATE, null);
		setThreshold(Method.MAX, null);
		setThreshold(Method.ALL, null);
	}

	private void setThreshold(Method m, Double th) {
		thMap.put(m, th);
	}

	private void print(Method m, Double th, double time) {
		double temp = time * 1.0E-6;
		if (th != null) {
			if (th < temp) {
				System.out.println(m + ":" + temp + "[ms]");
			}
		}
	}

	private void begin() {
		begin = System.nanoTime();
	}

	private void end(Method m) {
		Double th = thMap.get(m);
		long time = (System.nanoTime() - begin);
		print(m, th, time);
		all += time;
		if (max < time) {
			max = time;
		}
	}

	private void game_begin() {
		all = 0;
		max = 0;
	}

	private void game_end() {
		Method m = Method.ALL;
		Double th = thMap.get(m);
		print(m, th, all);

		m = Method.MAX;
		th = thMap.get(m);
		print(m, th, max);
	}

	@Override
	public String getName() {
		begin();
		String ret = teamName;
		try {
			// rolePlayer.getName();
		} catch (Exception e) {
			if (showErrorLog) {
				e.printStackTrace();
			}
		}
		end(Method.GETNAME);
		return ret;
	}

	@Override
	public String talk() {
		begin();
		String ret = new Content(new SkipContentBuilder()).getText();
		try {
			ret = rolePlayer.talk();
		} catch (Exception e) {
			if (showErrorLog) {
				e.printStackTrace();
			}
		}
		end(Method.TALK);
		return ret;
	}

	@Override
	public String whisper() {
		begin();
		String ret = new Content(new SkipContentBuilder()).getText();
		try {
			ret = rolePlayer.whisper();
		} catch (Exception e) {
			if (showErrorLog) {
				e.printStackTrace();
			}
		}
		end(Method.WHISPER);
		return ret;
	}

	@Override
	public void initialize(GameInfo arg0, GameSetting arg1) {
		game_begin();
		begin();
		try {
			if (rolePlayer == null) {
				rolePlayer = playerClass.newInstance();
			}
			rolePlayer.initialize(arg0, arg1);
		} catch (Exception e) {
			if (showErrorLog) {
				e.printStackTrace();
			}
		}
		end(Method.INITIALIZE);
		return;
	}

	@Override
	public void dayStart() {
		begin();
		try {
			rolePlayer.dayStart();
		} catch (Exception e) {
			if (showErrorLog) {
				e.printStackTrace();
			}
		}
		end(Method.DAYSTART);
		return;
	}

	@Override
	public void finish() {
		begin();
		try {
			rolePlayer.finish();
		} catch (Exception e) {
			if (showErrorLog) {
				e.printStackTrace();
			}
		}
		end(Method.FINISH);
		game_end();
		return;
	}

	@Override
	public void update(GameInfo arg0) {
		begin();
		try {
			rolePlayer.update(arg0);
		} catch (Exception e) {
			if (showErrorLog) {
				e.printStackTrace();
			}
		}
		end(Method.UPDATE);
		return;
	}

	@Override
	public Agent attack() {
		begin();
		Agent ret = null;
		try {
			ret = rolePlayer.attack();
		} catch (Exception e) {
			if (showErrorLog)
				if (showErrorLog) {
					e.printStackTrace();
				}
		}
		end(Method.ATTACK);
		return ret;
	}

	@Override
	public Agent divine() {
		begin();
		Agent ret = null;
		try {
			ret = rolePlayer.divine();
		} catch (Exception e) {
			if (showErrorLog) {
				e.printStackTrace();
			}
		}
		end(Method.DIVINE);
		return ret;
	}

	@Override
	public Agent guard() {
		begin();
		Agent ret = null;
		try {
			ret = rolePlayer.guard();
		} catch (Exception e) {
			if (showErrorLog) {
				e.printStackTrace();
			}
		}
		end(Method.GUARD);
		return ret;
	}

	@Override
	public Agent vote() {
		begin();
		Agent ret = null;
		try {
			ret = rolePlayer.vote();
		} catch (Exception e) {
			if (showErrorLog) {
				e.printStackTrace();
			}
			return null;
		}
		end(Method.VOTE);
		return ret;
	}

}
