package org.sagebionetworks.web.client.widget.licenseddownloader;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface LicenceServiceAsync {

	void hasAccepted(String username, String objectUri, AsyncCallback<Boolean> callback);

	void acceptLicenseAgreement(String username, String objectUri, AsyncCallback<Boolean> callback);

	void logUserDownload(String username, String objectUri, String fileUri, AsyncCallback<Void> callback);
	
}
