package org.sagebionetworks.web.client.widget.header;

import org.sagebionetworks.web.client.widget.header.Header.MenuItems;
import org.sagebionetworks.web.shared.users.UserData;

import com.google.gwt.user.client.ui.IsWidget;

public interface HeaderView extends IsWidget {

	/**
	 * Set the presenter.
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);

	public void setMenuItemActive(MenuItems menuItem);

	public void removeMenuItemActive(MenuItems menuItem);

	public void refresh();
	
	/**
	 * Presenter interface
	 */
	public interface Presenter {
		UserData getUser();

		void lookupId(String synapseId);
	}

}
