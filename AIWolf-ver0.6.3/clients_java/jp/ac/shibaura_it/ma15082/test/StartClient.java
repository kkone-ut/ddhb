package jp.ac.shibaura_it.ma15082.test;

import org.aiwolf.common.bin.ClientStarter;

public class StartClient {

	public static void main(String args[]) throws Exception {
		ClientStarter cs = new ClientStarter();
		int num = 15;
		if (args == null || args.length <= 0) {
			args = new String[] { "-h", "localhost", "-p", "10000", "-c",
					"jp.ac.shibaura_it.ma15082.player.WasabiRoleAssignPlayer", "-n", "Wasabi" };

		}
		for (int i = 0; i < num; i++) {
			cs.main(args);
		}
	}

}
