package org.sagebionetworks.web.client;

import org.sagebionetworks.web.client.security.AuthenticationException;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;
import org.sagebionetworks.web.shared.users.UserData;
import org.sagebionetworks.web.shared.users.UserRegistration;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("users")
public interface UserAccountService extends RemoteService {	

	public void sendPasswordResetEmail(String emailAddress) throws RestServiceException;
	
	void sendSetApiPasswordEmail(String emailAddress) throws RestServiceException;
	
	public void setPassword(String email, String newPassword);

	public UserData initiateSession(String username, String password) throws AuthenticationException;
	
	public UserData getUser(String sessionToken) throws AuthenticationException;

	public void createUser(UserRegistration userInfo) throws RestServiceException;
	
	public void updateUser(String firstName, String lastName, String displayName) throws RestServiceException;
	
	public void terminateSession(String sessionToken) throws RestServiceException;
	
	public boolean ssoLogin(String sessionToken) throws RestServiceException;
		
	public String getPrivateAuthServiceUrl();
	
	public String getPublicAuthServiceUrl();
	
	public String getSynapseWebUrl();
	
}
