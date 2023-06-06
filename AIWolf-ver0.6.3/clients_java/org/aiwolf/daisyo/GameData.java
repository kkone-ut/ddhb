package org.aiwolf.daisyo;

public class GameData {

	DataType type;
	int day;
	int turn;
	int talker;
	int object;
	boolean white;

	GameData(DataType _type, int _day, int _turn, int _talker, int _object, boolean _white){
		type = _type;
		day = _day;
		turn = _turn;
		talker = _talker;
		object = _object;
		white = _white;
	}

}
