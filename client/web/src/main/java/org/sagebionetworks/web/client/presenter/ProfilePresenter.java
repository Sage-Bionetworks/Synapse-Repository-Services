package org.sagebionetworks.web.client.presenter;

import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.UserAccountServiceAsync;
import org.sagebionetworks.web.client.place.LoginPlace;
import org.sagebionetworks.web.client.place.Profile;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.view.ProfileView;
import org.sagebionetworks.web.shared.users.UserData;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;

public class ProfilePresenter extends AbstractActivity implements ProfileView.Presenter {
		
	private Profile place;
	private ProfileView view;
	private PlaceController placeController;
	private PlaceChanger placeChanger;
	private AuthenticationController authenticationController;
	private UserAccountServiceAsync userService;

	
	@Inject
	public ProfilePresenter(ProfileView view, AuthenticationController authenticationController, UserAccountServiceAsync userService) {
		this.view = view;
		this.authenticationController = authenticationController;
		this.userService = userService;
	}

	@Override
	public void start(AcceptsOneWidget panel, EventBus eventBus) {
		this.placeController = DisplayUtils.placeController;
		this.placeChanger = new PlaceChanger() {			
			@Override
			public void goTo(Place place) {
				placeController.goTo(place);
			}
		};
		// Set the presenter on the view
		this.view.setPresenter(this);
		this.view.render();
		
		// Install the view
		panel.setWidget(view);
	}

	public void setPlace(Profile place) {
		this.place = place;
		this.view.render();
	}

	@Override
	public PlaceChanger getPlaceChanger() {
		return placeChanger;
	}

	@Override
	public void resetPassword(final String existingPassword, final String newPassword) {
		// 1. Authenticate user with existing password
		final UserData currentUser = authenticationController.getLoggedInUser();
		if(currentUser != null) {
			authenticationController.loginUser(currentUser.getEmail(), existingPassword, new AsyncCallback<UserData>() {				
				@Override
				public void onSuccess(UserData result) {
					// 2. set password
					userService.setPassword(currentUser.getEmail(), newPassword, new AsyncCallback<Void>() {
						@Override
						public void onSuccess(Void result) {
							view.showPasswordChangeSuccess();
						}

						@Override
						public void onFailure(Throwable caught) {						
							view.passwordChangeFailed();
							view.showErrorMessage("Password Change failed. Please try again.");
						}
					});
				}
				
				@Override
				public void onFailure(Throwable caught) {
					view.passwordChangeFailed();
					view.showErrorMessage("Incorrect password. Please enter your existing Synapse password.<br/><br/>If you have not setup a Synapse password, please see your Profile page to do so.");
				}
			});
		} else {
			view.passwordChangeFailed();
			view.showInfo("Reset Password failed. Please Login Again.");
			placeChanger.goTo(new LoginPlace(LoginPlace.LOGIN_TOKEN));
		}
	}

	@Override
	public void createSynapsePassword() {
		final UserData currentUser = authenticationController.getLoggedInUser();
		if(currentUser != null) {
			userService.sendPasswordResetEmail(currentUser.getEmail(), new AsyncCallback<Void>() {
				@Override
				public void onSuccess(Void result) {
					view.showRequestPasswordEmailSent();
					view.showInfo("You have been sent an email. Please check your inbox.");
				}
				
				@Override
				public void onFailure(Throwable caught) {
					view.requestPasswordEmailFailed();
					view.showErrorMessage("An error occured. Please try reloading the page.");					
				}
			});
		} else {	
			view.requestPasswordEmailFailed();
			view.showInfo("Please Login Again.");
			placeChanger.goTo(new LoginPlace(LoginPlace.LOGIN_TOKEN));
		}		
	}

}

