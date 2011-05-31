package org.sagebionetworks.web.client.security;

import org.sagebionetworks.web.client.UserAccountServiceAsync;
import org.sagebionetworks.web.client.cookie.CookieKeys;
import org.sagebionetworks.web.client.cookie.CookieProvider;
import org.sagebionetworks.web.shared.users.UserData;

import com.extjs.gxt.ui.client.widget.Info;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.inject.Inject;

public class AuthenticationControllerImpl implements AuthenticationController {
	
	private static String AUTHENTICATION_MESSAGE = "Invalid usename or password.";
	private static UserData currentUser;
	
	private CookieProvider cookies;
	private UserAccountServiceAsync userAccountService;

	@Inject
	public AuthenticationControllerImpl(CookieProvider cookies, UserAccountServiceAsync userAccountService){
		this.cookies = cookies;
		this.userAccountService = userAccountService;
	}

	@Override
	public boolean isLoggedIn() {
		String loginCookieString = cookies.getCookie(CookieKeys.USER_LOGIN_DATA);
		if(loginCookieString != null) {
			currentUser = new UserData(loginCookieString);
			return true;
		} 
		return false;
	}

	@Override
	public void loginUser(final String username, String password, final AsyncCallback<UserData> callback) {
		if(username == null || password == null) callback.onFailure(new AuthenticationException(AUTHENTICATION_MESSAGE));		
		userAccountService.initiateSession(username, password, new AsyncCallback<UserData>() {		
			@Override
			public void onSuccess(UserData userData) {				
				String cookie = userData.getCookieString();
				cookies.setCookie(CookieKeys.USER_LOGIN_DATA, cookie);
				cookies.setCookie(CookieKeys.USER_LOGIN_TOKEN, userData.getToken());
				
				AuthenticationControllerImpl.currentUser = userData;
				callback.onSuccess(userData);
			}
			
			@Override
			public void onFailure(Throwable caught) {
				callback.onFailure(new AuthenticationException(AUTHENTICATION_MESSAGE));
			}
		});
	}

	@Override
	public UserData getLoggedInUser() {
		return currentUser;
	}

	@Override
	public void logoutUser() {
		if(currentUser != null) {
			userAccountService.terminateSession(currentUser.getToken(), new AsyncCallback<Void>() {	
				@Override
				public void onSuccess(Void result) {					
					Info.display("Message", "You have been logged out.");
					cookies.removeCookie(CookieKeys.USER_LOGIN_DATA);
					cookies.removeCookie(CookieKeys.USER_LOGIN_TOKEN);
				}

				@Override
				public void onFailure(Throwable caught) {
					MessageBox.alert("Message", "An error occured while logging you out. Please try again.", null);
				}
			});
		}
	}

}
