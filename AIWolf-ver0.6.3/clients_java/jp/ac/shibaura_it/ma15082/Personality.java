package jp.ac.shibaura_it.ma15082;

public class Personality {
	public final double openness_to_experimence;// 好奇心
	public final double conscientiousness;// 勤勉性
	public final double extroversion;// 外向性
	public final double agreeableness;// 協調性
	public final double neuroticism;// 情緒不安定性

	public Personality(double oe, double co, double ex, double ag, double ne) {
		openness_to_experimence = oe;
		conscientiousness = co;
		extroversion = ex;
		agreeableness = ag;
		neuroticism = ne;
	}

	public String toString() {
		return openness_to_experimence + "," + conscientiousness + "," + extroversion + "," + agreeableness + ","
				+ neuroticism;
	}

	public double getParam(int i) {
		switch (i) {
		case 0:
			return openness_to_experimence;
		case 1:
			return conscientiousness;
		case 2:
			return extroversion;
		case 3:
			return agreeableness;
		case 4:
			return neuroticism;
		}
		return 0;
	}

	public double getWeightAlong() {
		return openness_to_experimence;
	}

	public double getWeightSubjective() {
		return 1 - conscientiousness;
	}

	public double getWeightVolume() {
		return extroversion;
	}

	public double getWeightTalkWhite() {
		return agreeableness;
	}

	public double getWeightRandom() {
		return neuroticism;
	}

}
