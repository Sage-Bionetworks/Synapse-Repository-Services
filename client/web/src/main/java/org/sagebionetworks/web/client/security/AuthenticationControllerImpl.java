package org.sagebionetworks.web.client.security;

import org.sagebionetworks.web.client.UserAccountServiceAsync;
import org.sagebionetworks.web.client.cookie.CookieKeys;
import org.sagebionetworks.web.client.cookie.CookieProvider;
import org.sagebionetworks.web.shared.users.UserData;

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
		String login = cookies.getCookie(CookieKeys.USER_LOGIN_DATA);
		return login != null;
	}

	@Override
	public void loginUser(final String username, String password, final AsyncCallback<UserData> callback) {
		if(username == null || password == null) callback.onFailure(new AuthenticationException(AUTHENTICATION_MESSAGE));		
		userAccountService.authenticateUser(username, password, new AsyncCallback<UserData>() {		
			@Override
			public void onSuccess(UserData userData) {				
				String cookie = userData.getCookieString();
				cookies.setCookie(CookieKeys.USER_LOGIN_DATA, cookie);
				AuthenticationControllerImpl.currentUser = userData;
				callback.onSuccess(userData);
			}
			
			@Override
			public void onFailure(Throwable caught) {
				callback.onFailure(new AuthenticationException(AUTHENTICATION_MESSAGE));
			}
		});

		// This is hard-coded for the moment
//		if(!"sage".equals(username))throw new AuthenticationException(AUTHENTICATION_MESSAGE);
//		if(!"genetics12".equals(password)) throw new AuthenticationException(AUTHENTICATION_MESSAGE);
//		UserData userData =  new UserData("Mike Kellen", "Mike Kellen", "someToken");
//		// Store this in a cookie
//		String cookie = userData.getCookieString();
//		cookies.setCookie(CookieKeys.USER_LOGIN_DATA, cookie);
//		return userData;

	}

	@Override
	public UserData getLoggedInUser() {
		return currentUser;
	}

}
