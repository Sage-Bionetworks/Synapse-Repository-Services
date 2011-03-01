package org.sagebionetworks.web.client.view;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.view.client.HasRows;
import com.google.gwt.view.client.RangeChangeEvent.Handler;
/**
 * A HandlerWrapper for RangeChangeEvent.Handler.
 * 
 * @author jmhill
 *
 */
public class RangeChangeHandlerWrapper extends AbstactHandlerWrapper<Handler> {

	public RangeChangeHandlerWrapper(HasRows hasRow, Handler handler) {
		super(hasRow, handler);
	}

	@Override
	public HandlerRegistration addHandlerToListner(HasRows hasRow, Handler handler) {
		// We are adding a RangeChangeHandler 
		return hasRow.addRangeChangeHandler(handler);
	}

}
