package org.sagebionetworks.web.client;

import org.sagebionetworks.web.shared.LinkedInInfo;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface LinkedInServiceAsync {

	void returnAuthUrl(AsyncCallback<LinkedInInfo> callback);
	
	void getCurrentUserInfo(String requestToken, String secret, String verifier,
			AsyncCallback<String> callback);

}