package org.sagebionetworks.web.client.presenter;

import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.place.StepsHome;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.view.StepsHomeView;
import org.sagebionetworks.web.shared.users.UserData;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

public class StepsHomePresenter extends AbstractActivity implements StepsHomeView.Presenter {
		
	private StepsHome place;
	private StepsHomeView view;
	private PlaceController placeController;
	private PlaceChanger placeChanger;
	private AuthenticationController authenticationController;
	private GlobalApplicationState globalApplicationState;
	
	@Inject
	public StepsHomePresenter(StepsHomeView view, 
			AuthenticationController authenticationController, 
			GlobalApplicationState globalApplicationState){
		this.view = view;
		this.authenticationController = authenticationController;
		this.globalApplicationState = globalApplicationState;
		this.placeChanger = globalApplicationState.getPlaceChanger();
		
		view.setPresenter(this);
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		// Install the view
		panel.setWidget(view);
	}

	public void setPlace(StepsHome place) {
		this.place = place;
		
		setCurrentUserId(); 
		view.setPresenter(this);
	}

	@Override
	public PlaceChanger getPlaceChanger() {
		return placeChanger;
	}

	@Override
    public String mayStop() {
        view.clear();
        return null;
    }
	
	public void setCurrentUserId() {
		UserData currentUser = authenticationController.getLoggedInUser();
		if(currentUser != null) {
			view.setCurrentUserId(currentUser.getEmail());
		}
	}
	
}
