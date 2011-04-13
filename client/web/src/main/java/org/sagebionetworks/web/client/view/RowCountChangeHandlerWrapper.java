package org.sagebionetworks.web.client.view;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.view.client.HasRows;
import com.google.gwt.view.client.RowCountChangeEvent.Handler;

/**
 * A HandlerWrapper for RowCountChangeEvent.Handler.
 * @author jmhill
 *
 */
public class RowCountChangeHandlerWrapper extends AbstactHandlerWrapper<Handler>{

	public RowCountChangeHandlerWrapper(HasRows hasRow, Handler handler) {
		super(hasRow, handler);
	}

	@Override
	public HandlerRegistration addHandlerToListner(HasRows hasRow, Handler handler) {
		// This is a RowCountChangeHandler
		return hasRow.addRowCountChangeHandler(handler);
	}

}
