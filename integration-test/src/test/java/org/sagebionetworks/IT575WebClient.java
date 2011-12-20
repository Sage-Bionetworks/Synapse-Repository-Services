package org.sagebionetworks;

import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.widget.licenseddownloader.LicenceServiceAsync;
import org.sagebionetworks.web.client.widget.licenseddownloader.LicensedDownloader;
import org.sagebionetworks.web.client.widget.licenseddownloader.LicensedDownloaderView;

/**
 * This integration test works at the web portal presenter layer
 * 
 * @author deflaux
 * 
 */
public class IT575WebClient {

	/**
	 * 
	 */
	@Before
	public void setUp() {
	}

	/**
	 * 
	 */
	@After
	public void tearDown() {
	}

	/**
	 * @throws Exception
	 */
	@Ignore
	@Test
	public void testLicensedDownloader() throws Exception {
		LicensedDownloaderView mockView;
		NodeServiceAsync mockNodeService;
		LicenceServiceAsync mockLicenseService;
		NodeModelCreator mockNodeModelCreator;
		AuthenticationController mockAuthenticationController;
		GlobalApplicationState mockGlobalApplicationState;
		SynapseClientAsync mockSynapseClient;
		PlaceChanger mockPlaceChanger;
		JSONObjectAdapter jsonObjectAdapterProvider;		
		
		mockView = Mockito.mock(LicensedDownloaderView.class);		
		mockNodeService = mock(NodeServiceAsync.class);
		mockLicenseService = mock(LicenceServiceAsync.class);
		mockNodeModelCreator = mock(NodeModelCreator.class);
		mockAuthenticationController = mock(AuthenticationController.class);
		mockGlobalApplicationState = mock(GlobalApplicationState.class);
		mockSynapseClient = mock(SynapseClientAsync.class);
		mockPlaceChanger = mock(PlaceChanger.class);
		
		LicensedDownloader downloader = null;
	
	}
}
