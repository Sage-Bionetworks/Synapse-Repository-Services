package org.sagebionetworks.web.client.presenter;

import org.sagebionetworks.web.client.place.LoginPlace;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.security.AuthenticationControllerImpl;
import org.sagebionetworks.web.client.view.LoginView;
import org.sagebionetworks.web.shared.users.UserData;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

public class LoginPresenter extends AbstractActivity implements LoginView.Presenter {

	private LoginPlace loginPlace;
	private LoginView view;
	private EventBus bus;
	private PlaceController placeController;
	private AuthenticationController authenticationController;
	
	@Inject
	public LoginPresenter(LoginView view, AuthenticationController authenticationController){
		this.view = view;
		this.view.setPresenter(this);
		this.authenticationController = authenticationController;
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		panel.setWidget(this.view.asWidget());
		this.bus = eventBus;
		this.placeController = new PlaceController(eventBus);
	}

	public void setPlace(LoginPlace place) {
		this.loginPlace = place;
		view.clear();
		if(LoginPlace.LOGOUT_TOKEN.equals(place.toToken())) {
			authenticationController.logoutUser();
		}
	}

	@Override
	public void setNewUser(UserData newUser) {	
		// Allow the user to proceed.
		bus.fireEvent( new PlaceChangeEvent(loginPlace.getForwardPlace()));
		
	}

	@Override
	public void goTo(Place place) {
		this.placeController.goTo(place);
	}

}
