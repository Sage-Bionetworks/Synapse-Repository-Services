package org.sagebionetworks.web.client;

import org.sagebionetworks.web.shared.users.UserData;
import org.sagebionetworks.web.shared.users.UserRegistration;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface UserAccountServiceAsync {

	void sendPasswordResetEmail(String emailAddress, AsyncCallback<Void> callback);

	void initiateSession(String username, String password, AsyncCallback<UserData> callback);

	void createUser(UserRegistration userInfo, AsyncCallback<Void> callback);

	void terminateSession(String sessionToken, AsyncCallback<Void> callback);

	// Not needed, provided by crowd
//	void resetPassword(String securityToken, String newPassword, AsyncCallback<String> callback);
//	void isActivePasswordResetToken(String securityToken, AsyncCallback<Boolean> callback);

	
}
