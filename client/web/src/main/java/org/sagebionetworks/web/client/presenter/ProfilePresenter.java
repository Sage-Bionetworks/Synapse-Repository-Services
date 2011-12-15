package org.sagebionetworks.web.client.presenter;

import java.util.Date;

import org.sagebionetworks.web.client.DisplayUtils;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.LinkedInServiceAsync;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.UserAccountServiceAsync;
import org.sagebionetworks.web.client.cookie.CookieKeys;
import org.sagebionetworks.web.client.cookie.CookieProvider;
import org.sagebionetworks.web.client.place.LoginPlace;
import org.sagebionetworks.web.client.place.Profile;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.view.ProfileView;
import org.sagebionetworks.web.shared.LinkedInInfo;
import org.sagebionetworks.web.shared.users.UserData;

import com.gargoylesoftware.htmlunit.util.Cookie;
import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.XMLParser;
import com.google.inject.Inject;

public class ProfilePresenter extends AbstractActivity implements ProfileView.Presenter {
		
	private Profile place;
	private ProfileView view;
	private PlaceController placeController;
	private PlaceChanger placeChanger;
	private AuthenticationController authenticationController;
	private UserAccountServiceAsync userService;
	private LinkedInServiceAsync linkedInService;
	private GlobalApplicationState globalApplicationState;
	private CookieProvider cookieProvider;
	
	@Inject
	public ProfilePresenter(ProfileView view, AuthenticationController authenticationController, UserAccountServiceAsync userService, LinkedInServiceAsync linkedInService, GlobalApplicationState globalApplicationState, CookieProvider cookieProvider) {
		this.view = view;
		this.authenticationController = authenticationController;
		this.userService = userService;
		this.linkedInService = linkedInService;
		this.globalApplicationState = globalApplicationState;
		this.placeChanger = globalApplicationState.getPlaceChanger();
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

	public void setPlace(Profile place) {
		this.place = place;
		this.view.setPresenter(this);
		this.view.clear();
		showView(place);
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
			view.showInfo("Error","Reset Password failed. Please Login Again.");
			placeChanger.goTo(new LoginPlace(LoginPlace.LOGIN_TOKEN));
		}
	}

	@Override
	public void createSynapsePassword() {
		final UserData currentUser = authenticationController.getLoggedInUser();
		if(currentUser != null) {
			userService.sendSetApiPasswordEmail(currentUser.getEmail(), new AsyncCallback<Void>() {
				@Override
				public void onSuccess(Void result) {
					view.showRequestPasswordEmailSent();
					view.showInfo("Email Sent","You have been sent an email. Please check your inbox.");
				}
				
				@Override
				public void onFailure(Throwable caught) {
					view.requestPasswordEmailFailed();
					view.showErrorMessage("An error occured. Please try reloading the page.");					
				}
			});
		} else {	
			view.requestPasswordEmailFailed();
			view.showInfo("Error", "Please Login Again.");
			placeChanger.goTo(new LoginPlace(LoginPlace.LOGIN_TOKEN));
		}		
	}

	@Override
	public void updateProfile(String firstName, String lastName) {
		final UserData currentUser = authenticationController.getLoggedInUser();
		
		if(currentUser != null) {
			userService.updateUser(firstName, lastName, firstName + " " + lastName, new AsyncCallback<Void>() {
				@Override
				public void onSuccess(Void result) {
					view.showUserUpdateSuccess();
					view.showInfo("Success", "Your profile has been updated.");
					
					AsyncCallback<UserData> callback = new AsyncCallback<UserData>() {
						@Override
						public void onFailure(Throwable caught) { }

						@Override
						public void onSuccess(UserData result) {
							view.refreshHeader();
						}
					};
					
					if(currentUser.isSSO()) {
						authenticationController.loginUserSSO(currentUser.getToken(), callback);
					} else {
						authenticationController.loginUser(currentUser.getToken(), callback);
					}
				}
				
				@Override
				public void onFailure(Throwable caught) {
					view.userUpdateFailed();
					view.showErrorMessage("An error occured. Please try reloading the page.");
				}
			});
		}
	}
	
	@Override
    public String mayStop() {
        view.clear();
        return null;
    }
	
	@Override
	public void redirectToLinkedIn() {
		linkedInService.returnAuthUrl(new AsyncCallback<LinkedInInfo>() {
			@Override
			public void onSuccess(LinkedInInfo result) {
				// Store the requestToken secret in a cookie, set to expire in five minutes
				Date date = new Date(System.currentTimeMillis() + 300000);
				cookieProvider.setCookie(CookieKeys.LINKEDIN, result.getRequestSecret(), date);
				// Open the LinkedIn authentication window in the same tab
				Window.open(result.getAuthUrl(), "_self", "");
			}
			
			@Override
			public void onFailure(Throwable caught) {
				view.showErrorMessage("An error occured. Please try reloading the page.");					
			}
		});
	}

	@Override
	public void goTo(Place place) {
		placeChanger.goTo(place);
	}

	/**
	 * This method will update the current user's profile using LinkedIn
	 */
	@Override
	public void updateProfileWithLinkedIn(String requestToken, String verifier) {
		// Grab the requestToken secret from the cookie. If it's expired, show an error message.
		// If not, grab the user's info for an update.
		String secret = cookieProvider.getCookie(CookieKeys.LINKEDIN);
		if(secret == null || secret.equals("")) {
			view.showErrorMessage("You request has timed out. Please reload the page and try again.");
		} else {
			linkedInService.getCurrentUserInfo(requestToken, secret, verifier, new AsyncCallback<String>() {
				@Override
				public void onSuccess(String result) {
					Document linkedInProfile = XMLParser.parse(result);
				    String firstName = linkedInProfile.getElementsByTagName("first-name").item(0).getFirstChild().getNodeValue();
				    String lastName = linkedInProfile.getElementsByTagName("last-name").item(0).getFirstChild().getNodeValue();
				    updateProfile(firstName, lastName);
				}
				
				@Override
				public void onFailure(Throwable caught) {
					view.showErrorMessage("An error occured. Please try reloading the page.");									
				}
			});
		}
	}

	
	private void showView(Profile place) {
		String token = place.toToken();
		
		if(!DisplayUtils.DEFAULT_PLACE_TOKEN.equals(token)
				&& !"".equals(token) && token != null) {
			// User just logged in to LinkedIn. Get the request token and their info to update
			// their profile with.
			
			String requestToken = "";
			String verifier = "";
			String[] oAuthTokens = token.split("&");
			for(String s : oAuthTokens) {
				String[] tokenParts = s.split("=");
				if(tokenParts[0].equals("oauth_token")) {
					requestToken = tokenParts[1];
				} else if(tokenParts[0].equals("oauth_verifier")) {
					verifier = tokenParts[1];
				}
			}
			
			if(!requestToken.equals("") && !verifier.equals("")) {
				updateProfileWithLinkedIn(requestToken, verifier);
			} else {
				view.showErrorMessage("An error occured. Please try reloading the page.");
			}
			
		}
	}
}

