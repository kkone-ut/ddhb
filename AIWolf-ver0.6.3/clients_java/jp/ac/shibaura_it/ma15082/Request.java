package jp.ac.shibaura_it.ma15082;

import org.aiwolf.client.lib.Topic;
import org.aiwolf.common.data.Agent;

public class Request {
	private Agent from;
	private Agent subject;
	private Topic verb;
	private Agent object;

	public Request(Agent f, Agent s, Topic v, Agent o) {
		from = f;
		subject = s;
		verb = v;
		object = o;
	}

	public Agent getFrom() {
		return from;
	}

	public Agent getSubject() {
		return subject;
	}

	public Agent getObject() {
		return object;
	}

	public Topic getVerb() {
		return verb;
	}

}
