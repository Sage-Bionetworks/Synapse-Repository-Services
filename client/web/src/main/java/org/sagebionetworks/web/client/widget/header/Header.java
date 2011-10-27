package org.sagebionetworks.web.client.widget.header;

import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.place.Lookup;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.security.AuthenticationControllerImpl;
import org.sagebionetworks.web.shared.users.UserData;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class Header implements HeaderView.Presenter {

	
	public static enum MenuItems {
		DATASETS, TOOLS, NETWORKS, PEOPLE, PROJECTS, ANALYSES, STEPS
	}
	
	private HeaderView view;
	private AuthenticationController authenticationController;
	private GlobalApplicationState globalApplicationState;
	private PlaceChanger placeChanger;
	
	@Inject
	public Header(HeaderView view, AuthenticationController authenticationController, GlobalApplicationState globalApplicationState) {
		this.view = view;
		this.authenticationController = authenticationController;
		this.globalApplicationState = globalApplicationState;
		this.placeChanger = globalApplicationState.getPlaceChanger();
		view.setPresenter(this);
	}
	
    public void setPlaceChanger(PlaceChanger placeChanger) {
    	this.placeChanger = placeChanger;
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

	@Override
	public void lookupId(String synapseId) {
		if(placeChanger != null) {
			placeChanger.goTo(new Lookup(synapseId));
		}
	}
	
}
