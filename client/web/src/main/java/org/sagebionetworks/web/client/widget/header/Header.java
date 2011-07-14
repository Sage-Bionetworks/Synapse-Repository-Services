package org.sagebionetworks.web.client.widget.header;

import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.security.AuthenticationControllerImpl;
import org.sagebionetworks.web.shared.users.UserData;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class Header implements HeaderView.Presenter {

	
	public static enum MenuItems {
		DATASETS, TOOLS, NETWORKS, PEOPLE, PROJECTS
	}
	
	private HeaderView view;
	private AuthenticationController authenticationController;
	
	@Inject
	public Header(HeaderView view, AuthenticationController authenticationController) {
		this.view = view;
		this.authenticationController = authenticationController;
		view.setPresenter(this);
	}
	
	public void setMenuItemActive(MenuItems menuItem) {
		view.setMenuItemActive(menuItem);
	}

	public void removeMenuItemActive(MenuItems menuItem) {
		view.removeMenuItemActive(menuItem);
	}

	public Widget asWidget() {
		view.setPresenter(this);
		return view.asWidget();
	}
	
	public void refresh() {
		view.refresh();
	}

	@Override
	public UserData getUser() {		
		return authenticationController.getLoggedInUser();
	}
	
}
