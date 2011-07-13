package org.sagebionetworks.web.shared.users;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.web.client.cookie.CookieUtils;

import com.google.gwt.user.client.rpc.IsSerializable;

public class UserData implements IsSerializable {
	
	// The internal user id.
	private String email;
	// The display name for this user.
	private String userName;
	// The user's token
	private String token;
	private boolean isSSO = false;
	
	/*
	 * Default Constructor is required
	 */
	public UserData() {		
	}
	
	public UserData(String cookieString) {
		List<String> cookieList = CookieUtils.createListFromString(cookieString);
		if(cookieList.size() == 3) {
			validateFields(cookieList.get(0), cookieList.get(1), cookieList.get(2));
			this.email = cookieList.get(0);
			this.userName = cookieList.get(1);
			this.token = cookieList.get(2);
		} else {
			throw new IllegalArgumentException("Session cookie contains the wrong number of elements.");
		}
	}
	
	public UserData(String email, String userName, String token) {
		validateFields(email, userName, token);
		this.email = email;
		this.userName = userName;
		this.token = token;
	}
	
	
	public String getCookieString() {
		// Add the fileds to a list
		List<String> fieldList = new ArrayList<String>();
		fieldList.add(email);
		fieldList.add(userName);
		fieldList.add(token);
		return CookieUtils.createStringFromList(fieldList);
	}
	public static UserData createFromCookieString(String cookie) {
		List<String> fieldList = CookieUtils.createListFromString(cookie);
		if(fieldList.size() != 3) throw new IllegalArgumentException("There should be three fields in this object");
		return new UserData(fieldList.get(0), fieldList.get(1), fieldList.get(2));
	}	

	public String getEmail() {
		return email;
	}
	
	public String getUserName() {
		return userName;
	}

	public String getToken() {
		return token;
	}

	public boolean isSSO() {
		return isSSO;
	}

	public void setSSO(boolean isSSO) {
		this.isSSO = isSSO;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((token == null) ? 0 : token.hashCode());
		result = prime * result + ((email == null) ? 0 : email.hashCode());
		result = prime * result
				+ ((userName == null) ? 0 : userName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		UserData other = (UserData) obj;
		if (token == null) {
			if (other.token != null)
				return false;
		} else if (!token.equals(other.token))
			return false;
		if (email == null) {
			if (other.email != null)
				return false;
		} else if (!email.equals(other.email))
			return false;
		if (userName == null) {
			if (other.userName != null)
				return false;
		} else if (!userName.equals(other.userName))
			return false;
		return true;
	}

	/*
	 * Private Methods
	 */
	private void validateFields(String email, String userName, String token) {
		if(email == null) throw new IllegalArgumentException("email cannot be null");
		if(userName == null) throw new IllegalArgumentException("UserName cannot be null");
		if(token == null) throw new IllegalArgumentException("Token cannot be null");
	}
}
