package org.sagebionetworks.web.client;

import java.util.List;

import org.sagebionetworks.web.shared.NodeType;
import org.sagebionetworks.web.shared.users.AclAccessType;
import org.sagebionetworks.web.shared.users.AclPrincipal;
import org.sagebionetworks.web.shared.users.UserData;
import org.sagebionetworks.web.shared.users.UserRegistration;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface UserAccountServiceAsync {

	void sendPasswordResetEmail(String emailAddress, AsyncCallback<Void> callback);

	void setPassword(String email, String newPassword, AsyncCallback<Void> callback);

	void initiateSession(String username, String password, AsyncCallback<UserData> callback);

	void getUser(String sessionToken, AsyncCallback<UserData> callback);	

	void createUser(UserRegistration userInfo, AsyncCallback<Void> callback);

	void terminateSession(String sessionToken, AsyncCallback<Void> callback);

	void ssoLogin(String sessionToken, AsyncCallback<Boolean> callback);

	void getAllUsers(AsyncCallback<List<AclPrincipal>> callback);

	void getAllGroups(AsyncCallback<List<AclPrincipal>> callback);

	void getAllUsersAndGroups(AsyncCallback<List<AclPrincipal>> callback);

	void getAuthServiceUrl(AsyncCallback<String> callback);

	void getSynapseWebUrl(AsyncCallback<String> callback);

}
