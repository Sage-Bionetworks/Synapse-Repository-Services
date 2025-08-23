package org.sagebionetworks.repo.manager.grid.response;

import java.util.Objects;

import org.sagebionetworks.repo.model.grid.EventContext;
import org.springframework.context.ApplicationEventPublisher;

/**
 * An event that can be sent via {@link ApplicationEventPublisher}.
 */
public class InternalEvent {

	private EventContext context;
	private String body;

	public EventContext getContext() {
		return context;
	}

	public InternalEvent setContext(EventContext context) {
		this.context = context;
		return this;
	}

	public String getBody() {
		return body;
	}

	public InternalEvent setBody(String body) {
		this.body = body;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(body, context);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InternalEvent other = (InternalEvent) obj;
		return Objects.equals(body, other.body) && Objects.equals(context, other.context);
	}

	@Override
	public String toString() {
		return "InternalEvent [context=" + context + ", body=" + body + "]";
	}

}
