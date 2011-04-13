package org.sagebionetworks.web.client.widget.footer;

import com.google.gwt.user.client.ui.IsWidget;

public interface FooterView extends IsWidget {

	/**
	 * Set the presenter.
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);
	
	
	/**
	 * Presenter interface
	 */
	public interface Presenter {
		
	}
}
