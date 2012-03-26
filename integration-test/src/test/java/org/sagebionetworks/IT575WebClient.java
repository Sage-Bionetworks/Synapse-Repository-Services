package org.sagebionetworks;


import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.List;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.repo.model.Data;
import org.sagebionetworks.repo.model.LayerTypeNames;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.AdapterFactoryImpl;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.web.client.EntityTypeProvider;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.cookie.CookieKeys;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.widget.licenseddownloader.LicenceServiceAsync;
import org.sagebionetworks.web.client.widget.licenseddownloader.LicensedDownloaderView;
import org.sagebionetworks.web.shared.EntityWrapper;
import org.sagebionetworks.web.shared.users.UserData;

import com.gdevelop.gwt.syncrpc.SyncProxy;

/**
 * This integration test works at the web portal presenter layer
 * 
 * @author deflaux
 * 
 */
public class IT575WebClient {

	private static File testUploadfile = null;
	private static Synapse synapse = null;
	private static Project project = null;
	private static Study dataset = null;
	private static Data layer = null;

	
	List<String> toDelete;
	/**
	 * @throws Exception
	 * 
	 */
	@BeforeClass
	public static void beforeClass() throws Exception {
		// create a file for the location
		testUploadfile = new File("IT575WebClient_location.txt");
		FileWriter fstream = new FileWriter(testUploadfile);
		BufferedWriter out = new BufferedWriter(fstream);
		out.write("IT575WebClient_location in S3");
		out.close();

		
		synapse = new Synapse();
		synapse.setAuthEndpoint(StackConfiguration
				.getAuthenticationServicePrivateEndpoint());
		synapse.setRepositoryEndpoint(StackConfiguration
				.getRepositoryServiceEndpoint());
		synapse.login(StackConfiguration.getIntegrationTestUserOneName(),
				StackConfiguration.getIntegrationTestUserOnePassword());
		
		
		project = synapse.createEntity(new Project());
		dataset = new Study();
		dataset.setParentId(project.getId());
		dataset = synapse.createEntity(dataset);
		layer = new Data();
		layer.setParentId(dataset.getId());
		layer.setType(LayerTypeNames.G);			
		layer = synapse.createEntity(layer);		

		project.setUri("blahblah/project/id");
		dataset.setUri("blahblah/dataset/id");
		layer.setUri("blahblah/layer/id");
		
	} 

		
	@After
	public void after() throws Exception{
		if(null != project) 
			synapse.deleteEntity(project);
		// do not try to delete the EULA object
	}


	/**
	 * This test should be refactored with Bruce's new code for workflows
	 * @throws Exception
	 */
	@Ignore
	@SuppressWarnings("unused")
	@Test
	public void testLicensedDownloader() throws Exception {		
		// upload file to S3
		synapse.uploadLocationableToSynapse(layer, testUploadfile);		
		layer = synapse.getEntity(layer);		

		String portalEndpoint = StackConfiguration.getPortalEndpoint() + "/";
		final String sessionToken = synapse.getCurrentSessionToken();	
		// setup user data & cookies
		UserData userData = new UserData(StackConfiguration.getIntegrationTestUserOneName(), StackConfiguration.getIntegrationTestUserOneName(), sessionToken, false);
		CookieManager empSession = new CookieManager();
		CookieStore cookieStore = empSession.getCookieStore();		
		cookieStore.add(new URI(portalEndpoint), new HttpCookie(CookieKeys.USER_LOGIN_TOKEN, sessionToken));
		cookieStore.add(new URI(portalEndpoint), new HttpCookie(CookieKeys.USER_LOGIN_DATA, userData.getCookieString()));
		empSession = new CookieManager(cookieStore, CookiePolicy.ACCEPT_ALL);
		
		// Sync'd services		
		NodeServiceAsync nodeService = (NodeServiceAsync) SyncProxy.newProxyInstance(NodeServiceAsync.class, portalEndpoint, "node", empSession); 
		LicenceServiceAsync licenseService = (LicenceServiceAsync) SyncProxy.newProxyInstance(LicenceServiceAsync.class, portalEndpoint, "license", empSession);
		SynapseClientAsync synapseClient = (SynapseClientAsync) SyncProxy.newProxyInstance(SynapseClientAsync.class, portalEndpoint, "synapse", empSession);
				
		// mocks
		LicensedDownloaderView mockView = Mockito.mock(LicensedDownloaderView.class);		
		NodeModelCreator mockNodeModelCreator = Mockito.mock(NodeModelCreator.class);
		AuthenticationController mockAuthenticationController = Mockito.mock(AuthenticationController.class);
		GlobalApplicationState mockGlobalApplicationState = Mockito.mock(GlobalApplicationState.class);		
		PlaceChanger mockPlaceChanger = Mockito.mock(PlaceChanger.class);		
		JSONObjectAdapter jsonObjectAdapterProvider = new JSONObjectAdapterImpl();		
		
		// provide for mocks
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(userData);
		JSONObjectAdapter datasetObj = jsonObjectAdapterProvider.createNew();
		dataset.writeToJSONObject(datasetObj);
		EntityWrapper datasetWrapper = new EntityWrapper(datasetObj.toJSONString(), Study.class.getName(), null);
		when(mockNodeModelCreator.createEntity(datasetWrapper)).thenReturn(dataset);
		
		// create entity type provider
//		EntityTypeProvider entityTypeProvider = new EntityTypeProvider(new RegisterConstantsStub(), new AdapterFactoryImpl());		
		
//		// create, run and verify
//		LicensedDownloader downloader = new LicensedDownloader(mockView,
//				nodeService, licenseService, mockNodeModelCreator,
//				mockAuthenticationController, mockGlobalApplicationState,
//				synapseClient, jsonObjectAdapterProvider, entityTypeProvider);
//		reset(mockView);				
//		downloader.configureHeadless(layer, false);		
		
		// test 
		verify(mockView).setDownloadLocations(layer.getLocations(), layer.getMd5());
	}
	
}

