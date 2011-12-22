package org.sagebionetworks.web.client.presenter;

import java.util.ArrayList;

import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.UserAccountServiceAsync;
import org.sagebionetworks.web.client.cookie.CookieKeys;
import org.sagebionetworks.web.client.cookie.CookieProvider;
import org.sagebionetworks.web.client.place.PublicProfile;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.view.PublicProfileView;
import org.sagebionetworks.web.shared.users.UserData;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;


public class PublicProfilePresenter extends AbstractActivity implements PublicProfileView.Presenter {
	private PublicProfile place;
	private PublicProfileView view;
	private PlaceController placeController;
	private PlaceChanger placeChanger;
	private GlobalApplicationState globalApplicationState;
	private UserAccountServiceAsync userAccountService;
	// Temporarily in to enable getUser in UserAccountService. Remove with new user service
	// for arbitrarily grabbing users.
	private CookieProvider cookieProvider;

	@Inject
	public PublicProfilePresenter(PublicProfileView view, GlobalApplicationState globalApplicationState, UserAccountServiceAsync userAccountService, CookieProvider cookieProvider) {
		this.view = view;
		this.globalApplicationState = globalApplicationState;
		this.placeChanger = globalApplicationState.getPlaceChanger();
		this.userAccountService = userAccountService;
		this.cookieProvider = cookieProvider;

		view.setPresenter(this);
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		// Set the presenter on the view
		this.view.render();
		
		// Install the view
		panel.setWidget(view);
	}

	public void setPlace(PublicProfile place) {
		this.place = place;
		this.view.setPresenter(this);
		this.view.render();
	}

	@Override
	public PlaceChanger getPlaceChanger() {
		return placeChanger;
	}
	
	@Override
	public void getUserInfo() {		
		userAccountService.getUser(cookieProvider.getCookie(CookieKeys.USER_LOGIN_TOKEN), new AsyncCallback<UserData>() {
			@Override
			public void onSuccess(UserData result) {
				// Add whatever information (besides the user's name) can be extracted from the user data
				ArrayList<String> userInfo = new ArrayList<String>();
				userInfo.add("E-mail: " + result.getEmail());
				
				// Update the view
				view.updateWithUserInfo(result.getUserName(), userInfo);
			}
			
			@Override
			public void onFailure(Throwable caught) {
				view.showErrorMessage("An error occured retrieving this user's information.");
			}
		});
	}
}
