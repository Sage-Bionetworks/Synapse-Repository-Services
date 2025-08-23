package org.sagebionetworks.grid.workers.message;

import java.util.Objects;

import org.sagebionetworks.repo.model.grid.EventContext;

public abstract class AbstractJsonRxMessage implements JsonRxMessageBase {
	
	private final EventContext context;
	private final Integer id;
	private final Object body;
	
	public AbstractJsonRxMessage(EventContext context, Integer id, Object body) {
		super();
		this.context = context;
		this.id = id;
		this.body = body;
	}

	public EventContext getContext() {
		return context;
	}

	public Integer getId() {
		return id;
	}

	public Object getBody() {
		return body;
	}

	@Override
	public int hashCode() {
		return Objects.hash(body, context, id);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractJsonRxMessage other = (AbstractJsonRxMessage) obj;
		return Objects.equals(body, other.body) && Objects.equals(context, other.context)
				&& Objects.equals(id, other.id);
	}

	@Override
	public String toString() {
		return "AbstractJsonRxMessage [context=" + context + ", id=" + id + ", body=" + body + "]";
	}
	
}
