package org.sagebionetworks.web.client.events;

import com.google.gwt.event.shared.GwtEvent;

public class CancelEvent extends GwtEvent<CancelHandler> {

	private static final Type TYPE = new Type<CancelHandler>();
	
	public CancelEvent() {
		
	}
	
	public static Type getType() {
		return TYPE;
	}
	
	@Override
	public com.google.gwt.event.shared.GwtEvent.Type<CancelHandler> getAssociatedType() {
		return TYPE;
	}

	@Override
	protected void dispatch(CancelHandler handler) {
		handler.onCancel(this);
	}

}
