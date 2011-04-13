package org.sagebionetworks.web.client.view;

import com.google.gwt.event.shared.HandlerRegistration;

/**
 * This is a proxy for a real a HandlerRegistration. It allows us to 
 * replace where a handler is registered {@link #replaceRegistration(HandlerRegistration)}, and still support
 * {@link #removeHandler()}.
 * 
 * @author jmhill
 *
 */
public class HandlerRegistrationProxy implements HandlerRegistration{
	
	// This is the actual handler.
	private HandlerRegistration wrapped;
	
	/**
	 * 
	 * @param toWrap
	 */
	public HandlerRegistrationProxy(HandlerRegistration toWrap){
		if(toWrap == null) throw new IllegalArgumentException("HandlerRegistration cannot be null");
		this.wrapped = toWrap;
	}
	
	/**
	 * Set this object as a proxy for a new HandlerRegistration.
	 * @param newReg
	 */
	public void replaceRegistration(HandlerRegistration newReg){
		if(newReg == null) throw new IllegalArgumentException("HandlerRegistration cannot be null");
		// First remove the old handler
		wrapped.removeHandler();
		// Assign the new
		wrapped = newReg;
	}

	/**
	 * Will forward the call to the real handler
	 */
	@Override
	public void removeHandler() {
		wrapped.removeHandler();
	}

	public HandlerRegistration getWrapped() {
		return wrapped;
	}
	
}
