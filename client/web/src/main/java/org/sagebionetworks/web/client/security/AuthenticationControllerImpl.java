package org.sagebionetworks.web.client.security;

import org.sagebionetworks.web.client.cookie.CookieKeys;
import org.sagebionetworks.web.client.cookie.CookieProvider;
import org.sagebionetworks.web.client.security.user.UserData;

import com.google.inject.Inject;

public class AuthenticationControllerImpl implements AuthenticationController {
	
	private static String AUTHENTICATION_MESSAGE = "Invalid usename or password.";
	
	private CookieProvider cookies;

	@Inject
	public AuthenticationControllerImpl(CookieProvider cookies){
		this.cookies = cookies;
	}

	@Override
	public boolean isLoggedIn() {
		String login = cookies.getCookie(CookieKeys.USER_LOGIN_DATA);
		return login != null;
	}



	@Override
	public UserData loginUser(String username, String password)	throws AuthenticationException {
		// This is hard-coded for the moment
		if(username == null || password == null) throw new AuthenticationException(AUTHENTICATION_MESSAGE);
		if(!"sage".equals(username))throw new AuthenticationException(AUTHENTICATION_MESSAGE);
		if(!"genetics12".equals(password)) throw new AuthenticationException(AUTHENTICATION_MESSAGE);
		UserData userData =  new UserData("Mike Kellen", "Mike Kellen", "someToken");
		// Store this in a cookie
		String cookie = userData.getCookieString();
		cookies.setCookie(CookieKeys.USER_LOGIN_DATA, cookie);
		return userData;
	}
	
	

}
