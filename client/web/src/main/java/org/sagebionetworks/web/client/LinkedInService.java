package org.sagebionetworks.web.client;

import org.sagebionetworks.web.shared.LinkedInInfo;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

@RemoteServiceRelativePath("linkedin")
public interface LinkedInService extends RemoteService {

	public LinkedInInfo returnAuthUrl();

	public String getCurrentUserInfo(String requestToken, String secret, String verifier);

}