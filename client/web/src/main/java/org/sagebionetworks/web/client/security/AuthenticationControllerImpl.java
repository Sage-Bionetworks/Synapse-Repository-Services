package org.sagebionetworks.web.client.security;

import org.sagebionetworks.web.client.DisplayConstants;
import org.sagebionetworks.web.client.UserAccountServiceAsync;
import org.sagebionetworks.web.client.cookie.CookieKeys;
import org.sagebionetworks.web.client.cookie.CookieProvider;
import org.sagebionetworks.web.shared.users.UserData;

import com.extjs.gxt.ui.client.widget.Info;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.google.gwt.place.shared.PlaceChangeEvent;
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
	public void setSSOUser(final String displayName, final String token, final AsyncCallback<UserData> callback) {
		if(displayName == null || token == null) callback.onFailure(new AuthenticationException(AUTHENTICATION_MESSAGE));
		userAccountService.ssoLogin(token, new AsyncCallback<Boolean>() {
			@Override
			public void onSuccess(Boolean result) {
				if(result) {
					UserData userData = new UserData(DisplayConstants.SINGLE_SIGN_ON_USERID, displayName, token);
					String cookie = userData.getCookieString();
					cookies.setCookie(CookieKeys.USER_LOGIN_DATA, cookie);
					cookies.setCookie(CookieKeys.USER_LOGIN_TOKEN, userData.getToken());
					
					AuthenticationControllerImpl.currentUser = userData;
					callback.onSuccess(userData);
				} else {
					callback.onFailure(new AuthenticationException(AUTHENTICATION_MESSAGE));
				}
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
			// don't actually terminate session, just remove the cookies			
			cookies.removeCookie(CookieKeys.USER_LOGIN_DATA);
			cookies.removeCookie(CookieKeys.USER_LOGIN_TOKEN);			
		}
	}


}
