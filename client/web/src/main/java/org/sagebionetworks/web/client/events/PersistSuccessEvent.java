package org.sagebionetworks.web.client.events;

import com.google.gwt.event.shared.GwtEvent;

public class PersistSuccessEvent extends GwtEvent<PersistSuccessHandler> {

	private static final Type TYPE = new Type<PersistSuccessHandler>();
	
	public PersistSuccessEvent() {
		
	}
	
	public static Type getType() {
		return TYPE;
	}
	
	@Override
	public com.google.gwt.event.shared.GwtEvent.Type<PersistSuccessHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(PersistSuccessHandler handler) {
		handler.onPersistSuccess(this);
	}

}
