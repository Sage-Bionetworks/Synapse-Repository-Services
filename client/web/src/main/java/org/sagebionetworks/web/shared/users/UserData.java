package org.sagebionetworks.web.shared.users;

import java.util.ArrayList;
import java.util.List;

import org.sagebionetworks.web.client.cookie.CookieUtils;

import com.google.gwt.user.client.rpc.IsSerializable;

public class UserData implements IsSerializable {
	
	// The internal user id.
	private String userId;
	// The display name for this user.
	private String userName;
	// The user's token
	private String token;
	
	/*
	 * Default Constructor is required
	 */
	public UserData() {		
	}
	
	public UserData(String cookieString) {
		List<String> cookieList = CookieUtils.createListFromString(cookieString);
		if(cookieList.size() == 3) {
			validateFields(cookieList.get(0), cookieList.get(1), cookieList.get(2));
			this.userId = cookieList.get(0);
			this.userName = cookieList.get(1);
			this.token = cookieList.get(2);
		} else {
			throw new IllegalArgumentException("Session cookie contains the wrong number of elements.");
		}
	}
	
	public UserData(String userId, String userName, String token) {
		validateFields(userId, userName, token);
		this.userId = userId;
		this.userName = userName;
		this.token = token;
	}
	
	
	public String getCookieString() {
		// Add the fileds to a list
		List<String> fieldList = new ArrayList<String>();
		fieldList.add(userId);
		fieldList.add(userName);
		fieldList.add(token);
		return CookieUtils.createStringFromList(fieldList);
	}
	public static UserData createFromCookieString(String cookie) {
		List<String> fieldList = CookieUtils.createListFromString(cookie);
		if(fieldList.size() != 3) throw new IllegalArgumentException("There should be three fields in this object");
		return new UserData(fieldList.get(0), fieldList.get(1), fieldList.get(2));
	}	

	public String getUserId() {
		return userId;
	}

	public String getUserName() {
		return userName;
	}

	public String getToken() {
		return token;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((token == null) ? 0 : token.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
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
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
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
	private void validateFields(String userId, String userName, String token) {
		if(userId == null) throw new IllegalArgumentException("UserId cannot be null");
		if(userName == null) throw new IllegalArgumentException("UserName cannot be null");
		if(token == null) throw new IllegalArgumentException("Token cannot be null");
	}
}
