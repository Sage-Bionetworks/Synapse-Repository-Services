package org.sagebionetworks.web.unitclient.widget.licenseddownloader;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Eula;
import org.sagebionetworks.repo.model.Layer;
import org.sagebionetworks.repo.model.LocationData;
import org.sagebionetworks.repo.model.Locationable;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.PlaceChanger;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.place.LoginPlace;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.widget.licenseddownloader.LicenceServiceAsync;
import org.sagebionetworks.web.client.widget.licenseddownloader.LicensedDownloader;
import org.sagebionetworks.web.client.widget.licenseddownloader.LicensedDownloaderView;
import org.sagebionetworks.web.shared.EntityWrapper;
import org.sagebionetworks.web.shared.LicenseAgreement;
import org.sagebionetworks.web.shared.NodeType;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;
import org.sagebionetworks.web.shared.users.UserData;
import org.sagebionetworks.web.test.helper.AsyncMockStubber;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;

public class LicensedDownloaderTest {
		
	LicensedDownloader licensedDownloader;
	LicensedDownloaderView mockView;
	NodeServiceAsync mockNodeService;
	LicenceServiceAsync mockLicenseService;
	NodeModelCreator mockNodeModelCreator;
	AuthenticationController mockAuthenticationController;
	GlobalApplicationState mockGlobalApplicationState;
	SynapseClientAsync mockSynapseClient;
	PlaceChanger mockPlaceChanger;
	
	JSONObjectAdapter jsonObjectAdapterProvider;
	Entity entity;
	Entity parentEntity;
	Eula eula1;
	UserData user1;
	LicenseAgreement licenseAgreement;
	List<LocationData> locations;
	
	@Before
	public void setup(){		
		mockView = Mockito.mock(LicensedDownloaderView.class);		
		mockNodeService = mock(NodeServiceAsync.class);
		mockLicenseService = mock(LicenceServiceAsync.class);
		mockNodeModelCreator = mock(NodeModelCreator.class);
		mockAuthenticationController = mock(AuthenticationController.class);
		mockGlobalApplicationState = mock(GlobalApplicationState.class);
		mockSynapseClient = mock(SynapseClientAsync.class);
		mockPlaceChanger = mock(PlaceChanger.class);
		jsonObjectAdapterProvider = new JSONObjectAdapterImpl();
		
		licensedDownloader = new LicensedDownloader(mockView, mockNodeService,
				mockLicenseService, mockNodeModelCreator,
				mockAuthenticationController, mockGlobalApplicationState,
				mockSynapseClient, jsonObjectAdapterProvider);
		
		
		licensedDownloader.setPlaceChanger(mockPlaceChanger);
		verify(mockView).setPresenter(licensedDownloader);
		
		// Eula	
		eula1 = new Eula();
		eula1.setId("eulaId");
		eula1.setAgreement("Agreement");
		eula1.setName("Agreement 1");
		
		// Parent Entity
		parentEntity = new Dataset();
		parentEntity.setId("datasetId");
		((Dataset)parentEntity).setEulaId(eula1.getId());

		// Entity
		entity = new Layer();
		entity.setId("layerId");
		entity.setName("layer");
		entity.setParentId(parentEntity.getId());

		// User
		user1 = new UserData("email@email.com", "Username", "token", false);
		
		licenseAgreement = new LicenseAgreement();		
		licenseAgreement.setLicenseHtml(eula1.getAgreement());

		// create a DownloadLocation model for this test
		LocationData downloadLocation = new LocationData();
		String name = "name";
		String path = "path";
		String md5sum = "md5sum";
		String contentType = "application/jpg";		
		downloadLocation.setPath(path);
		downloadLocation.setMd5(md5sum);
		downloadLocation.setContentType(contentType);
		locations = new ArrayList<LocationData>();
		locations.add(downloadLocation);

	}
	
	
	@SuppressWarnings("unchecked")
	@Test
	public void testLoadLicenseAgreement() throws RestServiceException {
		LicenseAgreement licenseAgreement = new LicenseAgreement(eula1.getAgreement(), null, eula1.getId());
		
		// null model
		resetMocks();
		licensedDownloader.loadLicenseAgreement(null);		
		
		// Failure to get parent via synapse client		
		resetMocks();
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(user1);
		when(mockNodeModelCreator.createEntity(any(EntityWrapper.class))).thenReturn(parentEntity);
		when(mockNodeModelCreator.createEULA(anyString())).thenReturn(eula1);
		AsyncMockStubber.callFailureWith(new Throwable("error message")).when(mockSynapseClient).getEntity(eq(parentEntity.getId()), any(AsyncCallback.class)); // fail for get parent 
		AsyncMockStubber.callSuccessWith("eula json").when(mockNodeService).getNodeJSON(eq(NodeType.EULA), eq(eula1.getId()), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(true).when(mockLicenseService).hasAccepted(eq(user1.getEmail()), eq(eula1.getId()), eq(parentEntity.getId()), any(AsyncCallback.class));
		licensedDownloader.loadLicenseAgreement(entity);
		verify(mockView).showDownloadFailure();

		// Success of parental get but failure in Entity creation 
		resetMocks();
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(user1);
		when(mockNodeModelCreator.createEntity(any(EntityWrapper.class))).thenReturn(null); // failed entity creation
		when(mockNodeModelCreator.createEULA(anyString())).thenReturn(eula1);
		AsyncMockStubber.callSuccessWith(new EntityWrapper()).when(mockSynapseClient).getEntity(eq(parentEntity.getId()), any(AsyncCallback.class)); 		
		AsyncMockStubber.callSuccessWith("eula json").when(mockNodeService).getNodeJSON(eq(NodeType.EULA), eq(eula1.getId()), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(true).when(mockLicenseService).hasAccepted(eq(user1.getEmail()), eq(eula1.getId()), eq(parentEntity.getId()), any(AsyncCallback.class));
		licensedDownloader.loadLicenseAgreement(entity);
		verify(mockView).showDownloadFailure();

		// Dataset with null EULA 
		resetMocks();
		((Dataset)parentEntity).setEulaId(null); // null EULA
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(user1);
		when(mockNodeModelCreator.createEntity(any(EntityWrapper.class))).thenReturn(parentEntity);
		when(mockNodeModelCreator.createEULA(anyString())).thenReturn(eula1);
		AsyncMockStubber.callSuccessWith(new EntityWrapper()).when(mockSynapseClient).getEntity(eq(parentEntity.getId()), any(AsyncCallback.class));		
		AsyncMockStubber.callSuccessWith("eula json").when(mockNodeService).getNodeJSON(eq(NodeType.EULA), eq(eula1.getId()), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(true).when(mockLicenseService).hasAccepted(eq(user1.getEmail()), eq(eula1.getId()), eq(parentEntity.getId()), any(AsyncCallback.class));
		licensedDownloader.loadLicenseAgreement(entity);
		verify(mockView).setLicenceAcceptanceRequired(false);		
		
		// model creation of EULA fails
		resetMocks();
		((Dataset)parentEntity).setEulaId(eula1.getId()); 
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(user1);
		when(mockNodeModelCreator.createEntity(any(EntityWrapper.class))).thenReturn(parentEntity);
		when(mockNodeModelCreator.createEULA(anyString())).thenReturn(null); // null EULA object
		AsyncMockStubber.callSuccessWith(new EntityWrapper()).when(mockSynapseClient).getEntity(eq(parentEntity.getId()), any(AsyncCallback.class));		
		AsyncMockStubber.callSuccessWith("eula json").when(mockNodeService).getNodeJSON(eq(NodeType.EULA), eq(eula1.getId()), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(true).when(mockLicenseService).hasAccepted(eq(user1.getEmail()), eq(eula1.getId()), eq(parentEntity.getId()), any(AsyncCallback.class));
		licensedDownloader.loadLicenseAgreement(entity);
		verify(mockView).showDownloadFailure();
		

		// License Acceptance failure
		resetMocks();
		((Dataset)parentEntity).setEulaId(eula1.getId());
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(user1);
		when(mockNodeModelCreator.createEntity(any(EntityWrapper.class))).thenReturn(parentEntity);
		when(mockNodeModelCreator.createEULA(anyString())).thenReturn(eula1);
		AsyncMockStubber.callSuccessWith(new EntityWrapper()).when(mockSynapseClient).getEntity(eq(parentEntity.getId()), any(AsyncCallback.class));		
		AsyncMockStubber.callSuccessWith("eula json").when(mockNodeService).getNodeJSON(eq(NodeType.EULA), eq(eula1.getId()), any(AsyncCallback.class));
		AsyncMockStubber.callFailureWith(new Throwable("error message")).when(mockLicenseService).hasAccepted(eq(user1.getEmail()), eq(eula1.getId()), eq(parentEntity.getId()), any(AsyncCallback.class)); // has not accepted
		licensedDownloader.loadLicenseAgreement(entity);
		verify(mockView).showDownloadFailure();

		// Check all okay but with user not logged in and trying to show download
		resetMocks();
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(null); // null user
		when(mockNodeModelCreator.createEntity(any(EntityWrapper.class))).thenReturn(parentEntity);
		when(mockNodeModelCreator.createEULA(anyString())).thenReturn(eula1);
		AsyncMockStubber.callSuccessWith(new EntityWrapper()).when(mockSynapseClient).getEntity(eq(parentEntity.getId()), any(AsyncCallback.class));		
		AsyncMockStubber.callSuccessWith("eula json").when(mockNodeService).getNodeJSON(eq(NodeType.EULA), eq(eula1.getId()), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(true).when(mockLicenseService).hasAccepted(eq(user1.getEmail()), eq(eula1.getId()), eq(parentEntity.getId()), any(AsyncCallback.class));
		licensedDownloader.loadLicenseAgreement(entity);
		verify(mockView).setUnauthorizedDownloads();		
		
		// License Accepted == False
		resetMocks();
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(user1);
		when(mockNodeModelCreator.createEntity(any(EntityWrapper.class))).thenReturn(parentEntity);
		when(mockNodeModelCreator.createEULA(anyString())).thenReturn(eula1);
		AsyncMockStubber.callSuccessWith(new EntityWrapper()).when(mockSynapseClient).getEntity(eq(parentEntity.getId()), any(AsyncCallback.class));		
		AsyncMockStubber.callSuccessWith("eula json").when(mockNodeService).getNodeJSON(eq(NodeType.EULA), eq(eula1.getId()), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(false).when(mockLicenseService).hasAccepted(eq(user1.getEmail()), eq(eula1.getId()), eq(parentEntity.getId()), any(AsyncCallback.class)); // accepted == false
		licensedDownloader.loadLicenseAgreement(entity);
		verify(mockView).setLicenceAcceptanceRequired(true);
		verify(mockView).setLicenseHtml(licenseAgreement.getLicenseHtml());

		
		// License Accepted == True
		resetMocks();
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(user1);
		when(mockNodeModelCreator.createEntity(any(EntityWrapper.class))).thenReturn(parentEntity);
		when(mockNodeModelCreator.createEULA(anyString())).thenReturn(eula1);
		AsyncMockStubber.callSuccessWith(new EntityWrapper()).when(mockSynapseClient).getEntity(eq(parentEntity.getId()), any(AsyncCallback.class));		
		AsyncMockStubber.callSuccessWith("eula json").when(mockNodeService).getNodeJSON(eq(NodeType.EULA), eq(eula1.getId()), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(true).when(mockLicenseService).hasAccepted(eq(user1.getEmail()), eq(eula1.getId()), eq(parentEntity.getId()), any(AsyncCallback.class)); // accepted == true
		licensedDownloader.loadLicenseAgreement(entity);
		verify(mockView).setLicenceAcceptanceRequired(false);
		verify(mockView).setLicenseHtml(licenseAgreement.getLicenseHtml());
		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testLoadDownloadLocations() throws RestServiceException {
				
		// null model
		resetMocks();
		licensedDownloader.loadDownloadLocations(null, null);
		
		// Null locations
		resetMocks();			
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(user1);
		((Locationable)entity).setLocations(null);		
		licensedDownloader.loadDownloadLocations(entity, false);
		verify(mockView).showDownloadsLoading();
		verify(mockView).setNoDownloads();	
								
		// Success Test: No download
		resetMocks();
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(user1);
		((Locationable)entity).setLocations(locations);		
		licensedDownloader.loadDownloadLocations(entity, false);
		verify(mockView).showDownloadsLoading();		
		verify(mockView).setDownloadLocations(locations);

		// Not Logged in Test: Download
		resetMocks();			
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(null); // not logged in
		((Locationable)entity).setLocations(locations);
		licensedDownloader.loadDownloadLocations(entity, true);
		verify(mockView).showDownloadsLoading();		
		verify(mockView).setDownloadLocations(locations);
		verify(mockView).showInfo(anyString(), anyString());
		verify(mockPlaceChanger).goTo(any(LoginPlace.class));

		// Success Test: Download
		resetMocks();			
		((Locationable)entity).setLocations(locations);
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(user1);
		licensedDownloader.loadDownloadLocations(entity, true);
		verify(mockView).showDownloadsLoading();		
		verify(mockView).setDownloadLocations(locations);
		verify(mockView).showWindow();
	}

	@Test
	public void testAsWidgetParameterized() throws RestServiceException {

		// License Accepted == True
		resetMocks();
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(user1);
		when(mockNodeModelCreator.createEntity(any(EntityWrapper.class))).thenReturn(parentEntity);
		when(mockNodeModelCreator.createEULA(anyString())).thenReturn(eula1);
		AsyncMockStubber.callSuccessWith(new EntityWrapper()).when(mockSynapseClient).getEntity(eq(parentEntity.getId()), any(AsyncCallback.class));		
		AsyncMockStubber.callSuccessWith("eula json").when(mockNodeService).getNodeJSON(eq(NodeType.EULA), eq(eula1.getId()), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(true).when(mockLicenseService).hasAccepted(eq(user1.getEmail()), eq(eula1.getId()), eq(parentEntity.getId()), any(AsyncCallback.class)); // accepted == true		

		// Success Test: No download
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(user1);
		((Locationable)entity).setLocations(locations);		
		
		licensedDownloader.asWidget(entity, false);		

		verify(mockView).showDownloadsLoading();		
		verify(mockView).setDownloadLocations(locations);
		verify(mockView).setLicenceAcceptanceRequired(false);
		verify(mockView).setLicenseHtml(licenseAgreement.getLicenseHtml());

	}
	
	@Test
	public void testAsWidget(){
		// make sure this version of asWidget can not be used		
		Widget widget = licensedDownloader.asWidget();
		assertNull(widget);		
	}
	
	@Test 
	public void testSetLicenseAgreement() {		
		// test license only
		licensedDownloader.setLicenseAgreement(licenseAgreement);				
		verify(mockView).setLicenseHtml(licenseAgreement.getLicenseHtml());
		
		reset(mockView);
		
		// test license and citation
		String citationHtml = "citation";
		licenseAgreement.setCitationHtml(citationHtml);
		licensedDownloader.setLicenseAgreement(licenseAgreement);
		verify(mockView).setCitationHtml(citationHtml);
		verify(mockView).setLicenseHtml(licenseAgreement.getLicenseHtml());		
	}
	
	@Test
	public void testSetLicenseAccepted() {
		@SuppressWarnings("unchecked")
		AsyncCallback<Void> callback = mock(AsyncCallback.class);
		licensedDownloader.setLicenseAcceptedCallback(callback);
		
		licensedDownloader.setLicenseAccepted();		
		verify(callback).onSuccess(null);
		verify(mockView).setLicenceAcceptanceRequired(false);
	}
	
	
	/*
	 * Private methods
	 */
	private void resetMocks() {
		reset(mockView);
		reset(mockNodeService);
		reset(mockLicenseService);
		reset(mockNodeModelCreator);
		reset(mockAuthenticationController);
		reset(mockGlobalApplicationState);
		reset(mockSynapseClient);
		reset(mockPlaceChanger);
		jsonObjectAdapterProvider = new JSONObjectAdapterImpl();
	}	

	
}
