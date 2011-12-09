package org.sagebionetworks.web.client.events;

import com.google.gwt.event.shared.GwtEvent;

public class EntityUpdatedEvent extends GwtEvent<EntityUpdatedHandler> {

	private static final Type TYPE = new Type<EntityUpdatedHandler>();
	
	public EntityUpdatedEvent() {
		
	}
	
	public static Type getType() {
		return TYPE;
	}
	
	@Override
	public com.google.gwt.event.shared.GwtEvent.Type<EntityUpdatedHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(EntityUpdatedHandler handler) {
		handler.onPersistSuccess(this);
	}

}
