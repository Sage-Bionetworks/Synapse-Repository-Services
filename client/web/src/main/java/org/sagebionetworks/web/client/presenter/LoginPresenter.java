package org.sagebionetworks.web.client.presenter;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.UserAccountServiceAsync;
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
	private UserAccountServiceAsync userService;
	private String openIdActionUrl;
	private String openIdReturnUrl;
	
	@Inject
	public LoginPresenter(LoginView view, AuthenticationController authenticationController, UserAccountServiceAsync userService){
		this.view = view;
		this.view.setPresenter(this);
		this.authenticationController = authenticationController;
		this.userService = userService;
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		panel.setWidget(this.view.asWidget());
		this.bus = eventBus;
		this.placeController = DisplayUtils.placeController;
	}

	public void setPlace(final LoginPlace place) {
		this.loginPlace = place;
		view.clear();
		if(openIdActionUrl != null && openIdReturnUrl != null) {		
			showView(place);
		} else {
			// load Open ID urls
			// retrieve endpoints for SSO
			userService.getAuthServiceUrl(new AsyncCallback<String>() {
				@Override
				public void onSuccess(String result) {
					openIdActionUrl = result + "/openid";
					
					userService.getSynapseWebUrl(new AsyncCallback<String>() {
						@Override
						public void onSuccess(String result) {
							openIdReturnUrl = result + "/#LoginPlace";
							
							// now show the view
							showView(place);
						}
						@Override
						public void onFailure(Throwable caught) {
						}
					});					
				}
				@Override
				public void onFailure(Throwable caught) {
					view.showErrorMessage("An Error occured. Please try reloading the page");
				}
			});
		}
	}

	private void showView(LoginPlace place) {
		String token = place.toToken();
		if(LoginPlace.LOGOUT_TOKEN.equals(token)) {
			UserData currentUser = authenticationController.getLoggedInUser();
			boolean isSso = false;
			if(currentUser != null)
				isSso = currentUser.isSSO();
			authenticationController.logoutUser();
			view.showLogout(isSso);
		} else if(!DisplayUtils.DEFAULT_PLACE_TOKEN.equals(token) && !"".equals(token) && token != null) {			
			// Single Sign on token. try refreshing the token to see if it is valid. if so, log user in
			// parse token
			view.showLoggingInLoader();
			if(token != null) {
				String[] parts = token.split(":");
				if(parts != null) {
					String sessionToken = parts[0];					
					authenticationController.loginUserSSO(sessionToken, new AsyncCallback<UserData>() {	
						@Override
						public void onSuccess(UserData result) {
							view.hideLoggingInLoader();
							// user is logged in. forward to home page
							bus.fireEvent( new PlaceChangeEvent(new Home(DisplayUtils.DEFAULT_PLACE_TOKEN)));
						}
						@Override
						public void onFailure(Throwable caught) {
							view.showErrorMessage("An error occured. Please try logging in again.");
							view.showLogin(openIdActionUrl, openIdReturnUrl);
						}
					});
				} else {
					view.showErrorMessage("An error occured. Please try logging in again.");
					view.showLogin(openIdActionUrl, openIdReturnUrl);
				}
			}
		} else {
			// standard view
			view.showLogin(openIdActionUrl, openIdReturnUrl);
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
