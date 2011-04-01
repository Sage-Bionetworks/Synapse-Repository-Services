package org.sagebionetworks.web.client.widget.modal;

import com.google.gwt.user.client.ui.IsWidget;

public interface ModalWindowView extends IsWidget {

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
