package org.sagebionetworks.web.unitclient.presenter;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.place.Layer;
import org.sagebionetworks.web.client.presenter.LayerPresenter;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.view.LayerView;
import org.sagebionetworks.web.client.widget.licenseddownloader.LicenceServiceAsync;
import org.sagebionetworks.web.shared.Dataset;
import org.sagebionetworks.web.shared.DownloadLocation;
import org.sagebionetworks.web.shared.EULA;
import org.sagebionetworks.web.shared.FileDownload;
import org.sagebionetworks.web.shared.LayerPreview;
import org.sagebionetworks.web.shared.LicenseAgreement;
import org.sagebionetworks.web.shared.NodeType;
import org.sagebionetworks.web.shared.PagedResults;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;
import org.sagebionetworks.web.shared.users.AclAccessType;
import org.sagebionetworks.web.shared.users.UserData;
import org.sagebionetworks.web.test.helper.AsyncMockStubber;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

public class LayerPresenterTest {
	
	LayerPresenter layerPresenter;
	LayerView mockView;
	NodeServiceAsync mockNodeService;
	LicenceServiceAsync mockLicenseService;
	NodeModelCreator mockNodeModelCreator;
	AuthenticationController mockAuthenticationController;
	GlobalApplicationState mockGlobalApplicationState;
	String datasetId = "1";
	String layerId = "2";
	Dataset datasetModel1;
	org.sagebionetworks.web.shared.Layer layerModel1;
	UserData user1;
	EULA eula1;
	
	@Before
	public void setup(){
		mockView = mock(LayerView.class);
		mockNodeService = mock(NodeServiceAsync.class);
		mockLicenseService = mock(LicenceServiceAsync.class);
		mockNodeModelCreator = mock(NodeModelCreator.class);
		mockAuthenticationController = mock(AuthenticationController.class);
		mockGlobalApplicationState = mock(GlobalApplicationState.class);
		layerPresenter = new LayerPresenter(mockView, mockNodeService, mockLicenseService, mockNodeModelCreator, mockAuthenticationController, mockGlobalApplicationState);

		// Dataset object
		datasetModel1 = new Dataset();
		datasetModel1.setId(datasetId);
		datasetModel1.setName("test dataset");		

		// Layer object
		layerModel1 = new org.sagebionetworks.web.shared.Layer();
		layerModel1.setType("C");
		layerModel1.setId(layerId);
		layerModel1.setName("test layer");
		layerModel1.setParentId(datasetModel1.getId());
		// UserData
		user1 = new UserData("email@email.com", "Username", "token", false);
		// eula
		eula1 = new EULA();
		eula1.setId("3");
		eula1.setAgreement("Agreement");
		eula1.setName("Agreement 1");
		datasetModel1.setEulaId(eula1.getId());
		
		
		verify(mockView).setPresenter(layerPresenter);
	}	
	
	@Test
	public void testSetPlace() {
		resetMocks();

		Layer place = new Layer(layerId, datasetId, false);		
		layerPresenter.setPlace(place);		
		
		verify(mockView).setPresenter(layerPresenter);
		verify(mockView).clear();
		
		// make sure that the set place causes the model to be reloaded. 
		// This is more throughly tested in testRefresh()		
		verify(mockNodeService).getNodeJSON(Mockito.eq(NodeType.LAYER), eq(layerId), (AsyncCallback<String>)any());
	}
	
	@Test
	public void testStart() {
		resetMocks();
		AcceptsOneWidget panel = mock(AcceptsOneWidget.class);
		EventBus eventBus = mock(EventBus.class);		
		
		layerPresenter.start(panel, eventBus);		
		verify(panel).setWidget(mockView);		
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testRefreshShallow() throws RestServiceException {
		Layer place = new Layer(layerId, datasetId, false);		
		layerPresenter.setPlace(place);		
		resetMocks();				

		// TODO : use the toJson() when model objects use agnostic JSON interface
		//String layerModelJSON = layerModel.toJson(); 
		String layerModelJSON = "{ layerModel1 given by the nodeModelCreator }";		 
		when(mockNodeModelCreator.createLayer(layerModelJSON)).thenReturn(layerModel1);
		AsyncMockStubber.callSuccessWith(layerModelJSON).when(mockNodeService).getNodeJSON(eq(NodeType.LAYER), eq(layerId), any(AsyncCallback.class));
		layerPresenter.refresh();
		// verify
		verify(mockNodeService).getNodeJSON(Mockito.eq(NodeType.LAYER), eq(layerId), any(AsyncCallback.class));

		verify(mockView).clear();		
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testLoadLayerPreview() throws RestServiceException {
		Layer place = new Layer(layerId, datasetId, false);		
		layerPresenter.setPlace(place);						

		// create a LayerPreview model for this test
		LayerPreview layerPreview = new LayerPreview();
		List<Map<String,String>> rows = new ArrayList<Map<String,String>>();
		Map<String,String> row1 = new HashMap<String,String>();
		row1.put("col1", "value1");		
		row1.put("col2", "value2");
		rows.add(row1);
		layerPreview.setRows(rows);
		List<String> headers = Arrays.asList(new String [] {"col1", "col2"});
		layerPreview.setHeaders(headers);
		
		// make paged result
		PagedResults pagedResults = new PagedResults();
		List<String> results = new ArrayList<String>();
		//String layerPreviewJSON = layerPreview.toJson();
		String layerPreviewJSON = "{ layerPreview (provided by NMC) }"; 
		results.add(layerPreviewJSON);
		pagedResults.setResults(results);
		//String pagedResultsJSON = pagedResults.toJson();

		// Failure Test		
		resetMocks();	
		when(mockNodeModelCreator.createPagedResults(Mockito.anyString())).thenReturn(pagedResults);
		when(mockNodeModelCreator.createLayerPreview(Mockito.anyString())).thenReturn(layerPreview);		
		Throwable error = new Throwable("Error Message");
		AsyncMockStubber.callFailureWith(error).when(mockNodeService).getNodePreview(eq(NodeType.LAYER), eq(layerId), any(AsyncCallback.class));
		layerPresenter.loadLayerPreview();
		verify(mockNodeService).getNodePreview(Mockito.eq(NodeType.LAYER), eq(layerId), any(AsyncCallback.class));
		verify(mockView).showLayerPreviewUnavailable();		
				
		// Success Test
		resetMocks();			
		when(mockNodeModelCreator.createPagedResults(Mockito.anyString())).thenReturn(pagedResults);
		when(mockNodeModelCreator.createLayerPreview(Mockito.anyString())).thenReturn(layerPreview);
		AsyncMockStubber.callSuccessWith(layerPreviewJSON).when(mockNodeService).getNodePreview(eq(NodeType.LAYER), eq(layerId), any(AsyncCallback.class));
		layerPresenter.loadLayerPreview();
		verify(mockNodeService).getNodePreview(Mockito.eq(NodeType.LAYER), eq(layerId), any(AsyncCallback.class));
		verify(mockView).setLayerPreviewTable(eq(rows), eq(headers), any(Map.class), any(Map.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testLoadDownloadLocations() throws RestServiceException {
		Layer place = new Layer(layerId, datasetId, false);		
		layerPresenter.setPlace(place);						
		
		// create a DownloadLocation model for this test
		DownloadLocation downloadLocation = new DownloadLocation();
		String name = "name";
		String path = "path";
		String md5sum = "md5sum";
		String contentType = "application/jpg";
		downloadLocation.setName(name);
		downloadLocation.setPath(path);
		downloadLocation.setMd5sum(md5sum);
		downloadLocation.setContentType(contentType);
		
		FileDownload dl = new FileDownload(downloadLocation.getPath(), "Download " + layerModel1.getName(), downloadLocation.getMd5sum(), downloadLocation.getContentType());		
		
		// make paged result
		PagedResults pagedResults = new PagedResults();
		List<String> results = new ArrayList<String>();
		//String layerPreviewJSON = layerPreview.toJson();
		String downloadLocationJSON = "{ downloadLocation (provided by NMC) }"; 
		results.add(downloadLocationJSON);
		pagedResults.setResults(results);
		//String pagedResultsJSON = pagedResults.toJson();

		// null model
		resetMocks();
		layerPresenter.loadDownloadLocations(null, null);
		
		// Failure Test		
		resetMocks();	
		when(mockNodeModelCreator.createPagedResults(Mockito.anyString())).thenReturn(pagedResults);
		when(mockNodeModelCreator.createDownloadLocation(Mockito.anyString())).thenReturn(downloadLocation);		
		Throwable error = new Throwable("Error Message");
		AsyncMockStubber.callFailureWith(error).when(mockNodeService).getNodeLocations(eq(NodeType.LAYER), eq(layerId), any(AsyncCallback.class));
		layerPresenter.loadDownloadLocations(layerModel1, false);
		verify(mockView).setDownloadUnavailable();
		verify(mockNodeService).getNodeLocations(Mockito.eq(NodeType.LAYER), eq(layerId), any(AsyncCallback.class));
				
				
		// Success Test: No download
		resetMocks();			
		when(mockNodeModelCreator.createPagedResults(Mockito.anyString())).thenReturn(pagedResults);
		when(mockNodeModelCreator.createDownloadLocation(Mockito.anyString())).thenReturn(downloadLocation);
		AsyncMockStubber.callSuccessWith(downloadLocationJSON).when(mockNodeService).getNodeLocations(eq(NodeType.LAYER), eq(layerId), any(AsyncCallback.class));
		layerPresenter.loadDownloadLocations(layerModel1, false);
		verify(mockView).showDownloadsLoading();
		verify(mockNodeService).getNodeLocations(Mockito.eq(NodeType.LAYER), eq(layerId), any(AsyncCallback.class));
		verify(mockView).setLicensedDownloads(Arrays.asList(new FileDownload[] { dl }));

		// Success Test: Download
		resetMocks();			
		when(mockNodeModelCreator.createPagedResults(Mockito.anyString())).thenReturn(pagedResults);
		when(mockNodeModelCreator.createDownloadLocation(Mockito.anyString())).thenReturn(downloadLocation);
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(user1);
		AsyncMockStubber.callSuccessWith(downloadLocationJSON).when(mockNodeService).getNodeLocations(eq(NodeType.LAYER), eq(layerId), any(AsyncCallback.class));
		layerPresenter.loadDownloadLocations(layerModel1, true);
		verify(mockView).showDownloadsLoading();
		verify(mockView).showDownload();
		verify(mockNodeService).getNodeLocations(Mockito.eq(NodeType.LAYER), eq(layerId), any(AsyncCallback.class));
		verify(mockView).setLicensedDownloads(Arrays.asList(new FileDownload[] { dl }));

	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testLoadPermissionLevel() {
		Layer place = new Layer(layerId, datasetId, false);		
		layerPresenter.setPlace(place);						
		
		// null model
		resetMocks();		
		layerPresenter.loadPermissionLevel(null);		
		
		// Public Page view (no user)
		resetMocks();		
		layerPresenter.loadPermissionLevel(layerModel1);
		verify(mockView).setLayerDetails(anyString(), anyString(), anyString(),
				anyString(), anyString(), anyString(), anyString(),
				any(Date.class), anyString(), anyInt(), anyInt(), anyString(),
				anyString(), anyString(), eq(false), eq(false), anyString());
		
		
		// Failure Test		
		resetMocks();
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(user1);
		Throwable error = new Throwable("Error Message");
		AsyncMockStubber.callFailureWith(error).when(mockNodeService).hasAccess(eq(NodeType.LAYER), eq(layerId), eq(AclAccessType.UPDATE), any(AsyncCallback.class));		
		layerPresenter.loadPermissionLevel(layerModel1);
		verify(mockView).showErrorMessage(anyString());
		verify(mockView).setLayerDetails(anyString(), anyString(), anyString(),
				anyString(), anyString(), anyString(), anyString(),
				any(Date.class), anyString(), anyInt(), anyInt(), anyString(),
				anyString(), anyString(), eq(false), eq(false), anyString());		
				
				
		// Success Tests
		// No UPDATE, No CHANGE_PERMISSIONS
		resetMocks();			
		AsyncMockStubber.callSuccessWith(false).when(mockNodeService).hasAccess(eq(NodeType.LAYER), eq(layerId), eq(AclAccessType.UPDATE), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(false).when(mockNodeService).hasAccess(eq(NodeType.LAYER), eq(layerId), eq(AclAccessType.CHANGE_PERMISSIONS), any(AsyncCallback.class));
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(user1);
		layerPresenter.loadPermissionLevel(layerModel1);
		verify(mockView).setLayerDetails(anyString(), anyString(), anyString(),
				anyString(), anyString(), anyString(), anyString(),
				any(Date.class), anyString(), anyInt(), anyInt(), anyString(),
				anyString(), anyString(), eq(false), eq(false), anyString());		
				
		// Yes UPDATE, No CHANGE_PERMISSIONS
		resetMocks();			
		AsyncMockStubber.callSuccessWith(true).when(mockNodeService).hasAccess(eq(NodeType.LAYER), eq(layerId), eq(AclAccessType.UPDATE), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(false).when(mockNodeService).hasAccess(eq(NodeType.LAYER), eq(layerId), eq(AclAccessType.CHANGE_PERMISSIONS), any(AsyncCallback.class));
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(user1);
		layerPresenter.loadPermissionLevel(layerModel1);
		verify(mockView).setLayerDetails(anyString(), anyString(), anyString(),
				anyString(), anyString(), anyString(), anyString(),
				any(Date.class), anyString(), anyInt(), anyInt(), anyString(),
				anyString(), anyString(), eq(false), eq(true), anyString());		
				
		// Yes UPDATE, Yes CHANGE_PERMISSIONS
		resetMocks();			
		AsyncMockStubber.callSuccessWith(true).when(mockNodeService).hasAccess(eq(NodeType.LAYER), eq(layerId), eq(AclAccessType.UPDATE), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(true).when(mockNodeService).hasAccess(eq(NodeType.LAYER), eq(layerId), eq(AclAccessType.CHANGE_PERMISSIONS), any(AsyncCallback.class));
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(user1);
		layerPresenter.loadPermissionLevel(layerModel1);
		verify(mockView).setLayerDetails(anyString(), anyString(), anyString(),
				anyString(), anyString(), anyString(), anyString(),
				any(Date.class), anyString(), anyInt(), anyInt(), anyString(),
				anyString(), anyString(), eq(true), eq(true), anyString());		
				

	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testLoadLicenseAgreement() throws RestServiceException {
		Layer place = new Layer(layerId, datasetId, false);		
		layerPresenter.setPlace(place);						
		
		LicenseAgreement licenseAgreement = new LicenseAgreement(eula1.getAgreement(), null, eula1.getId());
		
		// null model
		resetMocks();
		layerPresenter.loadLicenseAgreement(null, false);
		
		// Failure of Dataset
		resetMocks();
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(user1);
		when(mockNodeModelCreator.createDataset(anyString())).thenReturn(datasetModel1);
		when(mockNodeModelCreator.createEULA(anyString())).thenReturn(eula1);
		AsyncMockStubber.callFailureWith(new Throwable("error message")).when(mockNodeService).getNodeJSON(eq(NodeType.DATASET), eq(datasetModel1.getId()), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith("eula json").when(mockNodeService).getNodeJSON(eq(NodeType.EULA), eq(eula1.getId()), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(true).when(mockLicenseService).hasAccepted(eq(user1.getEmail()), eq(eula1.getId()), eq(datasetModel1.getId()), any(AsyncCallback.class));
		layerPresenter.loadLicenseAgreement(layerModel1, false);
		verifyShowDownloadFailure();

		// Success of Dataset with null
		resetMocks();
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(user1);
		when(mockNodeModelCreator.createDataset(anyString())).thenReturn(null);
		when(mockNodeModelCreator.createEULA(anyString())).thenReturn(eula1);
		AsyncMockStubber.callSuccessWith("dataset json").when(mockNodeService).getNodeJSON(eq(NodeType.DATASET), eq(datasetModel1.getId()), any(AsyncCallback.class));		
		AsyncMockStubber.callSuccessWith("eula json").when(mockNodeService).getNodeJSON(eq(NodeType.EULA), eq(eula1.getId()), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(true).when(mockLicenseService).hasAccepted(eq(user1.getEmail()), eq(eula1.getId()), eq(datasetModel1.getId()), any(AsyncCallback.class));
		layerPresenter.loadLicenseAgreement(layerModel1, false);
		verifyShowDownloadFailure();

		// Dataset with null EULA 
		resetMocks();
		datasetModel1.setEulaId(null);
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(user1);
		when(mockNodeModelCreator.createDataset(anyString())).thenReturn(datasetModel1);
		when(mockNodeModelCreator.createEULA(anyString())).thenReturn(eula1);
		AsyncMockStubber.callSuccessWith("dataset json").when(mockNodeService).getNodeJSON(eq(NodeType.DATASET), eq(datasetModel1.getId()), any(AsyncCallback.class));		
		AsyncMockStubber.callSuccessWith("eula json").when(mockNodeService).getNodeJSON(eq(NodeType.EULA), eq(eula1.getId()), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(true).when(mockLicenseService).hasAccepted(eq(user1.getEmail()), eq(eula1.getId()), eq(datasetModel1.getId()), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith("").when(mockNodeService).getNodeJSON(eq(NodeType.DATASET), eq(datasetModel1.getId()), any(AsyncCallback.class));
		layerPresenter.loadLicenseAgreement(layerModel1, false);
		verify(mockView).requireLicenseAcceptance(false);		
		verify(mockView).setLicenseAgreement(null);						

		
		// License Acceptance failure
		resetMocks();
		datasetModel1.setEulaId(eula1.getId());
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(user1);
		when(mockNodeModelCreator.createDataset(anyString())).thenReturn(datasetModel1);
		when(mockNodeModelCreator.createEULA(anyString())).thenReturn(eula1);
		AsyncMockStubber.callSuccessWith("dataset json").when(mockNodeService).getNodeJSON(eq(NodeType.DATASET), eq(datasetModel1.getId()), any(AsyncCallback.class));		
		AsyncMockStubber.callSuccessWith("eula json").when(mockNodeService).getNodeJSON(eq(NodeType.EULA), eq(eula1.getId()), any(AsyncCallback.class));
		AsyncMockStubber.callFailureWith(new Throwable("error message")).when(mockLicenseService).hasAccepted(eq(user1.getEmail()), eq(eula1.getId()), eq(datasetModel1.getId()), any(AsyncCallback.class));
		layerPresenter.loadLicenseAgreement(layerModel1, false);
		verifyShowDownloadFailure();

		// Check all okay but with user not logged in and trying to show download
		resetMocks();
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(null);
		when(mockNodeModelCreator.createDataset(anyString())).thenReturn(datasetModel1);
		when(mockNodeModelCreator.createEULA(anyString())).thenReturn(eula1);
		AsyncMockStubber.callSuccessWith("dataset json").when(mockNodeService).getNodeJSON(eq(NodeType.DATASET), eq(datasetModel1.getId()), any(AsyncCallback.class));		
		AsyncMockStubber.callSuccessWith("eula json").when(mockNodeService).getNodeJSON(eq(NodeType.EULA), eq(eula1.getId()), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(false).when(mockLicenseService).hasAccepted(eq(user1.getEmail()), eq(eula1.getId()), eq(datasetModel1.getId()), any(AsyncCallback.class));
		layerPresenter.loadLicenseAgreement(layerModel1, true);
		verify(mockView).showInfo(anyString(), anyString());		
		// TODO : check for placeChanger call
		
		// License Accepted == False
		resetMocks();
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(user1);
		when(mockNodeModelCreator.createDataset(anyString())).thenReturn(datasetModel1);
		when(mockNodeModelCreator.createEULA(anyString())).thenReturn(eula1);
		AsyncMockStubber.callSuccessWith("dataset json").when(mockNodeService).getNodeJSON(eq(NodeType.DATASET), eq(datasetModel1.getId()), any(AsyncCallback.class));		
		AsyncMockStubber.callSuccessWith("eula json").when(mockNodeService).getNodeJSON(eq(NodeType.EULA), eq(eula1.getId()), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(false).when(mockLicenseService).hasAccepted(eq(user1.getEmail()), eq(eula1.getId()), eq(datasetModel1.getId()), any(AsyncCallback.class));
		layerPresenter.loadLicenseAgreement(layerModel1, false);
		verify(mockView).requireLicenseAcceptance(true);
		verify(mockView).setLicenseAgreement(licenseAgreement);

		
		// License Accepted == True
		resetMocks();
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(user1);
		when(mockNodeModelCreator.createDataset(anyString())).thenReturn(datasetModel1);
		when(mockNodeModelCreator.createEULA(anyString())).thenReturn(eula1);
		AsyncMockStubber.callSuccessWith("dataset json").when(mockNodeService).getNodeJSON(eq(NodeType.DATASET), eq(datasetModel1.getId()), any(AsyncCallback.class));		
		AsyncMockStubber.callSuccessWith("eula json").when(mockNodeService).getNodeJSON(eq(NodeType.EULA), eq(eula1.getId()), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(true).when(mockLicenseService).hasAccepted(eq(user1.getEmail()), eq(eula1.getId()), eq(datasetModel1.getId()), any(AsyncCallback.class));
		layerPresenter.loadLicenseAgreement(layerModel1, false);
		verify(mockView).requireLicenseAcceptance(false);
		verify(mockView).setLicenseAgreement(licenseAgreement);
		

		// EULA is null
		resetMocks();
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(user1);
		when(mockNodeModelCreator.createDataset(anyString())).thenReturn(datasetModel1);
		when(mockNodeModelCreator.createEULA(anyString())).thenReturn(null);
		AsyncMockStubber.callSuccessWith("dataset json").when(mockNodeService).getNodeJSON(eq(NodeType.DATASET), eq(datasetModel1.getId()), any(AsyncCallback.class));		
		AsyncMockStubber.callSuccessWith("eula json").when(mockNodeService).getNodeJSON(eq(NodeType.EULA), eq(eula1.getId()), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith(true).when(mockLicenseService).hasAccepted(eq(user1.getEmail()), eq(eula1.getId()), eq(datasetModel1.getId()), any(AsyncCallback.class));
		layerPresenter.loadLicenseAgreement(layerModel1, false);
		verifyShowDownloadFailure();		
	}

	private void verifyShowDownloadFailure() {
		verify(mockView).showErrorMessage(anyString());
		verify(mockView).setDownloadUnavailable();
		verify(mockView).disableLicensedDownloads(true);
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
	}	

}

