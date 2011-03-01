package org.sagebionetworks.web.client.view;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.view.client.HasRows;

/**
 * This object keeps track of a handler's registration and allows the handler
 * to be applied to a new listener.
 * 
 * @author jmhill
 * @param <T>
 *
 */
abstract public class AbstactHandlerWrapper<T> {
	
	T handler;
	HandlerRegistrationProxy registration;
	
	/**
	 * Will register the passed handler with the passed listener.
	 * This object will track the handler, and wrap the HandlerRegistration.
	 * 
	 * To apply the handler to a new listener use {@link #removeFromOldAndAddToNewListner(HasRows)} 
	 * @param hasRow
	 * @param handler
	 */
	public AbstactHandlerWrapper(HasRows hasRow , T handler){
		if(hasRow == null) throw new IllegalArgumentException("The listner (HasRow) cannot be null");
		if(handler == null) throw new IllegalArgumentException("The handler cannot be null");
		// First add the hanlder to the listner
		HandlerRegistration realRgeistration = addHandlerToListner(hasRow, handler);
		if(realRgeistration == null) throw new IllegalArgumentException("addHandlerToListner() returned null");
		registration = new HandlerRegistrationProxy(realRgeistration);
		this.handler = handler;
	}

	public T getHandler() {
		return handler;
	}

	public HandlerRegistrationProxy getRegistration() {
		return registration;
	}
	
	/**
	 * Will remove the wrapped handler from the old listener and add it to the new.
	 * @param newHasRow
	 */
	public void removeFromOldAndAddToNewListner(HasRows listener){
		// First add this to the listener
		HandlerRegistration newRegistration = addHandlerToListner(listener, handler);
		// Now replace the
		registration.replaceRegistration(newRegistration);
	}
	
	/**
	 * Override to add the passed handler to the appropriate method of the listener (HasRow).
	 * @param hasRow
	 * @param handler
	 * @return
	 */
	public abstract HandlerRegistration addHandlerToListner(HasRows hasRow, T handler);

}
