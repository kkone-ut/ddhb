package camellia.aiwolf.demo;

import java.util.List;

import org.aiwolf.common.data.Agent;
import org.aiwolf.common.net.GameInfo;

public class GeneralFunction {

	// メソッド名：isAlive
	// 引数：GameInfoとAgent
	// 戻り値：boolean
	// 説明：Agentがまだ生きているかGameInfoを調べて結果を返す (True or False)
	static boolean isAlive(GameInfo currentGameInfo, Agent agent) {
		return currentGameInfo.getAliveAgentList().contains(agent);
	}

	// メソッド名：randomSelect
	// 引数：任意の型のリスト
	// 戻り値：リストの型の要素
	// 説明：引数のリストからランダムに1つ要素を取り出す
	//			空リストの場合はnullを返す

	static <T> T randomSelect(List<T> list) {
		if (list.isEmpty()) {
			return null;
		} else {
			return list.get((int) (Math.random() * list.size()));
		}
	}
}
