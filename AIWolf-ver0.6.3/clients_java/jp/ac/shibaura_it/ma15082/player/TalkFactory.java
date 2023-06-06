package jp.ac.shibaura_it.ma15082.player;

import org.aiwolf.client.lib.AgreeContentBuilder;
import org.aiwolf.client.lib.AttackContentBuilder;
import org.aiwolf.client.lib.ComingoutContentBuilder;
import org.aiwolf.client.lib.Content;
import org.aiwolf.client.lib.DisagreeContentBuilder;
import org.aiwolf.client.lib.DivinationContentBuilder;
import org.aiwolf.client.lib.DivinedResultContentBuilder;
import org.aiwolf.client.lib.EstimateContentBuilder;
import org.aiwolf.client.lib.GuardCandidateContentBuilder;
import org.aiwolf.client.lib.GuardedAgentContentBuilder;
import org.aiwolf.client.lib.IdentContentBuilder;
import org.aiwolf.client.lib.OverContentBuilder;
import org.aiwolf.client.lib.RequestContentBuilder;
import org.aiwolf.client.lib.SkipContentBuilder;
import org.aiwolf.client.lib.TalkType;
import org.aiwolf.client.lib.VoteContentBuilder;
import org.aiwolf.common.data.Agent;
import org.aiwolf.common.data.Role;
import org.aiwolf.common.data.Species;

public enum TalkFactory {
	instance;

	final Content skip, over;

	public static TalkFactory getInstance() {
		return instance;
	}

	private TalkFactory() {
		skip = new Content(new SkipContentBuilder());
		over = new Content(new OverContentBuilder());
	}

	public Content agree(TalkType type, int d, int i) {
		return new Content(new AgreeContentBuilder(type, d, i));
	}

	public Content disagree(TalkType type, int d, int i) {
		return new Content(new DisagreeContentBuilder(type, d, i));
	}

	public Content comingout(Agent a, Role r) {
		return new Content(new ComingoutContentBuilder(a, r));
	}

	public Content attack(Agent a) {
		return new Content(new AttackContentBuilder(a));
	}

	public Content divined(Agent a, Species s) {
		return new Content(new DivinedResultContentBuilder(a, s));
	}

	public Content inquested(Agent a, Species s) {
		return new Content(new IdentContentBuilder(a, s));
	}

	public Content guarded(Agent a) {
		return new Content(new GuardedAgentContentBuilder(a));
	}

	public Content estimate(Agent a, Role r) {
		return new Content(new EstimateContentBuilder(a, r));
	}

	public Content divine(Agent a) {
		return new Content(new DivinationContentBuilder(a));
	}

	public Content guard(Agent a) {
		return new Content(new GuardCandidateContentBuilder(a));
	}

	public Content request(Agent a, Content c) {
		return new Content(new RequestContentBuilder(a, c));
	}

	public Content vote(Agent a) {
		return new Content(new VoteContentBuilder(a));
	}

	public Content skip() {
		return skip;
	}

	public Content over() {
		return over;
	}

}
