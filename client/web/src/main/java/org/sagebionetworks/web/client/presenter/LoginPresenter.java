package org.sagebionetworks.web.client.presenter;

import org.sagebionetworks.web.client.place.Home;
import org.sagebionetworks.web.client.place.LoginPlace;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.view.LoginView;
import org.sagebionetworks.web.shared.users.UserData;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceChangeEvent;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
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
		String token = place.toToken();
		if(LoginPlace.LOGOUT_TOKEN.equals(token)) {
			authenticationController.logoutUser();			
		} else if(!"0".equals(token)) {
			// Single Sign on token. try refreshing the token to see if it is valid. if so, log user in
			// parse token
			if(token != null) {
				String[] parts = token.split(":");
				if(parts != null && parts.length == 2) {
					String sessionToken = parts[0];
					String displayName = parts[1];
					authenticationController.setSSOUser(displayName, sessionToken, new AsyncCallback<UserData>() {	
						@Override
						public void onSuccess(UserData result) {
							// user is logged in. forward to home page
							bus.fireEvent( new PlaceChangeEvent(new Home("0")));
						}
						@Override
						public void onFailure(Throwable caught) {
							view.showErrorMessage("An error occured. Please try logging in again.");
						}
					});
				} else {
					view.showErrorMessage("An error occured. Please try logging in again.");
				}
			}
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
