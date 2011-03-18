package org.sagebionetworks.web.server.servlet;

import java.util.logging.Logger;

import org.sagebionetworks.web.client.widget.licensebox.LicenceService;
import org.sagebionetworks.web.server.RestTemplateProvider;
import org.sagebionetworks.web.shared.LicenseAgreement;
import org.sagebionetworks.web.shared.QueryConstants.ObjectType;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;

public class LicenseServiceImpl extends RemoteServiceServlet implements LicenceService {

	private static Logger logger = Logger.getLogger(SearchServiceImpl.class.getName());

	/**
	 * The template is injected with Gin
	 */
	private RestTemplateProvider templateProvider;

	@Override
	public boolean hasAccepted(String username, String objectUri) {
		// TODO Auto-generated method stub
		return false; // require acceptance
	}

	@Override
	public boolean acceptLicenseAgreement(String username, String objectUri) {
		// TODO : !!!! actually connect to a service layer impl !!!!
		return true;  // license acceptance persisted by service
	}

	@Override
	public void logUserDownload(String username, String objectUri, String fileUri) {
		// TODO : !!!! actually connect to a service layer impl !!!!
	}

}


