package org.sagebionetworks.web.client.widget.sharing;

import org.sagebionetworks.web.client.widget.sharing.AccessMenuButton.AccessLevel;

import com.google.gwt.user.client.ui.IsWidget;

public interface AccessMenuButtonView extends IsWidget {

	/**
	 * Set the presenter.
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);
	
	/**
	 * Sets the access level string
	 * @param level
	 */
	public void setAccessLevel(AccessLevel level);
	
	/**
	 * Presenter interface
	 */
	public interface Presenter {
		
	}
}
