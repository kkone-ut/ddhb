package jp.ac.shibaura_it.ma15082;

import org.aiwolf.client.lib.Content;
import org.aiwolf.common.data.Species;

public enum Colour {
	WHITE("白"), BLACK("黒"), GREY("灰"), PANDA("パンダ"), NONE("");

	private String name;

	private Colour(final String s) {
		name = s;
	}

	public String getName() {
		return name;
	}

	public static Colour convert(String str) {
		for (Colour c : values()) {
			if (c.toString().equals(str)) {
				return c;
			}
		}
		return NONE;
	}

	public Colour join(Colour c) {
		if (this == NONE || c == NONE) {
			return NONE;
		}
		if (this == PANDA || c == PANDA) {
			return PANDA;
		}
		if (this == c) {
			return c;
		}
		if (this == GREY) {
			return c;
		}
		if (c == GREY) {
			return this;
		}

		return PANDA;
	}

	public static Colour analyze(Content content) {
		Colour ret = Colour.GREY;
		// 発言のAgentが黒か白かだけ調べる
		switch (content.getTopic()) {
		case AGREE:// 省略
		case DISAGREE:
			break;
		case ESTIMATE:// 人外役職か否かで分類する
			if (content.getRole().getSpecies() == Species.WEREWOLF) {
				ret = Colour.BLACK;
			} else {
				ret = Colour.WHITE;
			}
			break;
		case GUARDED:// 護衛したのは白だから
		case ATTACK:// 攻撃されるのは白だから
			ret = Colour.WHITE;
			break;
		case VOTE:// 投票するのは黒だから
			ret = Colour.BLACK;
			break;
		default:
			break;

		}

		return ret;
	}

}
