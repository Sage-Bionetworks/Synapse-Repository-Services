package org.sagebionetworks.web.client.widget.header;

import org.sagebionetworks.web.client.widget.header.Header.MenuItem;

import com.google.gwt.user.client.ui.IsWidget;

public interface HeaderView extends IsWidget {

	/**
	 * Set the presenter.
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);

	public void setMenuItemActive(MenuItem menuItem);

	public void removeMenuItemActive(MenuItem menuItem);

	
	/**
	 * Presenter interface
	 */
	public interface Presenter {
		
	}

}
