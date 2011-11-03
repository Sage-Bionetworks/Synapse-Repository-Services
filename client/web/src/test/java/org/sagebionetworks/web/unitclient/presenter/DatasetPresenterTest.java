package org.sagebionetworks.web.unitclient.presenter;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.place.Dataset;
import org.sagebionetworks.web.client.presenter.DatasetPresenter;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.view.DatasetView;
import org.sagebionetworks.web.client.widget.licenseddownloader.LicenceServiceAsync;
import org.sagebionetworks.web.shared.DownloadLocation;
import org.sagebionetworks.web.shared.EULA;
import org.sagebionetworks.web.shared.FileDownload;
import org.sagebionetworks.web.shared.LicenseAgreement;
import org.sagebionetworks.web.shared.NodeType;
import org.sagebionetworks.web.shared.PagedResults;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;
import org.sagebionetworks.web.shared.users.UserData;
import org.sagebionetworks.web.test.helper.AsyncMockStubber;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

public class DatasetPresenterTest {
	
	DatasetPresenter datasetPresenter;
	DatasetView mockView;
	NodeServiceAsync mockNodeService;
	LicenceServiceAsync mockLicenseService;
	NodeModelCreator mockNodeModelCreator;
	AuthenticationController mockAuthenticationController;
	GlobalApplicationState mockGlobalApplicationState;
	String datasetId = "1";
	Dataset place = new Dataset("Dataset:"+ datasetId);
	org.sagebionetworks.repo.model.Dataset datasetModel1;
	UserData user1;
	EULA eula1;
	PagedResults emptyPagedResults;
	PagedResults pagedResults;
	DownloadLocation downloadLocation;
	FileDownload fileDownload;
	
	@Before
	public void setup(){
		mockView = mock(DatasetView.class);
		mockNodeService = mock(NodeServiceAsync.class);
		mockLicenseService = mock(LicenceServiceAsync.class);
		mockNodeModelCreator = mock(NodeModelCreator.class);
		mockAuthenticationController = mock(AuthenticationController.class);
		mockGlobalApplicationState = mock(GlobalApplicationState.class);
		datasetPresenter = new DatasetPresenter(mockView, mockNodeService, mockLicenseService, mockNodeModelCreator, mockAuthenticationController, mockGlobalApplicationState);
		
		// UserData
		user1 = new UserData("email@email.com", "Username", "token", false);

		// Dataset 
		datasetModel1 = new org.sagebionetworks.repo.model.Dataset();
		datasetModel1.setId(datasetId);
		datasetModel1.setName("test dataset");	

		// eula
		eula1 = new EULA();
		eula1.setId("3");
		eula1.setAgreement("Agreement");
		eula1.setName("Agreement 1");
		datasetModel1.setEulaId(eula1.getId());

		// PagedResult with no elements
		emptyPagedResults = new PagedResults();
		List<String> emptyResults = new ArrayList<String>();
		emptyPagedResults.setResults(emptyResults);
		
		// PagedResult with one element
		pagedResults = new PagedResults();
		List<String> results = new ArrayList<String>();
		results.add("{downloadLocationJSON}");
		pagedResults.setResults(results);

		// DownloadLocation
		downloadLocation = new DownloadLocation();
		String name = "name";
		String path = "path";
		String md5sum = "md5sum";
		String contentType = "application/jpg";
		downloadLocation.setName(name);
		downloadLocation.setPath(path);
		downloadLocation.setMd5sum(md5sum);
		downloadLocation.setContentType(contentType);
		
		// FileDownload
		fileDownload = new FileDownload(downloadLocation.getPath(), "Download " + datasetModel1.getName(), downloadLocation.getMd5sum(), downloadLocation.getContentType());		

		verify(mockView).setPresenter(datasetPresenter);
	}	
	
	@Test
	public void testSetPlace() {
		resetMocks();
		Dataset place = Mockito.mock(Dataset.class);
		datasetPresenter.setPlace(place);
		
		verify(mockView).setPresenter(datasetPresenter);

	}	
	
	@Test
	public void testStart() {
		resetMocks();
		datasetPresenter = new DatasetPresenter(mockView, mockNodeService, mockLicenseService, mockNodeModelCreator, mockAuthenticationController, mockGlobalApplicationState);		
		datasetPresenter.setPlace(place);		
		AcceptsOneWidget panel = mock(AcceptsOneWidget.class);
		EventBus eventBus = mock(EventBus.class);		
		
		datasetPresenter.start(panel, eventBus);		
		verify(panel).setWidget(mockView);		
	}
	
	@Test
	public void testRefreshFromServer() {
		resetMocks();
		datasetPresenter = new DatasetPresenter(mockView, mockNodeService, mockLicenseService, mockNodeModelCreator, mockAuthenticationController, mockGlobalApplicationState);		
		datasetPresenter.setPlace(place);		
		
		datasetPresenter.refreshFromServer();		
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testLoadDownloadLocations() throws RestServiceException {
		Dataset place = new Dataset(datasetId);		
		datasetPresenter.setPlace(place);						
				
		when(mockNodeModelCreator.createDataset(anyString())).thenReturn(datasetModel1);
		AsyncMockStubber.callSuccessWith("dataset json").when(mockNodeService).getNodeJSON(eq(NodeType.DATASET), eq(datasetModel1.getId()), any(AsyncCallback.class));		
		datasetPresenter.refreshFromServer();
		
		// Success Test: Download
		resetMocks();			
		when(mockNodeModelCreator.createPagedResults(Mockito.anyString())).thenReturn(pagedResults);
		when(mockNodeModelCreator.createDownloadLocation(Mockito.anyString())).thenReturn(downloadLocation);
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(user1);
		when(mockNodeModelCreator.createDataset(anyString())).thenReturn(datasetModel1);
		AsyncMockStubber.callSuccessWith("node location json").when(mockNodeService).getNodeLocations(eq(NodeType.DATASET), eq(datasetId), any(AsyncCallback.class));
		datasetPresenter.loadDownloadLocations();
		verify(mockNodeService).getNodeLocations(Mockito.eq(NodeType.DATASET), eq(datasetId), any(AsyncCallback.class));
		verify(mockView).setDatasetDownloads(Arrays.asList(new FileDownload[] { fileDownload }));

		// Success Test: No downloads
		resetMocks();			
		when(mockNodeModelCreator.createPagedResults(Mockito.anyString())).thenReturn(emptyPagedResults);
		AsyncMockStubber.callSuccessWith("node location json").when(mockNodeService).getNodeLocations(eq(NodeType.DATASET), eq(datasetId), any(AsyncCallback.class));
		datasetPresenter.loadDownloadLocations();
		verify(mockNodeService).getNodeLocations(Mockito.eq(NodeType.DATASET), eq(datasetId), any(AsyncCallback.class));
		verify(mockView).setDatasetDownloads(Arrays.asList(new FileDownload[] { }));
		
		// Failure Test		
		resetMocks();	
		when(mockNodeModelCreator.createPagedResults(Mockito.anyString())).thenReturn(pagedResults);
		when(mockNodeModelCreator.createDownloadLocation(Mockito.anyString())).thenReturn(downloadLocation);		
		AsyncMockStubber.callSuccessWith("dataset json").when(mockNodeService).getNodeJSON(eq(NodeType.DATASET), eq(datasetModel1.getId()), any(AsyncCallback.class));		
		Throwable error = new Throwable("Error Message");
		AsyncMockStubber.callFailureWith(error).when(mockNodeService).getNodeLocations(eq(NodeType.DATASET), eq(datasetId), any(AsyncCallback.class));
		datasetPresenter.loadDownloadLocations();
		verify(mockNodeService).getNodeLocations(Mockito.eq(NodeType.DATASET), eq(datasetId), any(AsyncCallback.class));
		verify(mockView).setDownloadUnavailable();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testLoadLicenseAgreement() throws RestServiceException {
		Dataset place = new Dataset(datasetId);		
		datasetPresenter.setPlace(place);						
		
		LicenseAgreement licenseAgreement = new LicenseAgreement(eula1.getAgreement(), null, eula1.getId());

		// Dataset has eula, license already accepted
		resetMocks();
		datasetModel1.setEulaId(eula1.getId());
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(user1);
		when(mockNodeModelCreator.createDataset(anyString())).thenReturn(datasetModel1);
		when(mockNodeModelCreator.createEULA(anyString())).thenReturn(eula1);
		AsyncMockStubber.callSuccessWith("dataset json").when(mockNodeService).getNodeJSON(eq(NodeType.DATASET), eq(datasetModel1.getId()), any(AsyncCallback.class));		
		AsyncMockStubber.callSuccessWith(true).when(mockLicenseService).hasAccepted(eq(user1.getEmail()), eq(eula1.getId()), eq(datasetModel1.getId()), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith("eula json").when(mockNodeService).getNodeJSON(eq(NodeType.EULA), eq(eula1.getId()), any(AsyncCallback.class));
		datasetPresenter.refreshFromServer();
		datasetPresenter.setLicenseAgreement();
		verify(mockView).requireLicenseAcceptance(false);		
		verify(mockView).setLicenseAgreement(licenseAgreement);

		// Dataset has eula, license not yet accepted
		resetMocks();
		datasetModel1.setEulaId(eula1.getId());
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(user1);
		when(mockNodeModelCreator.createDataset(anyString())).thenReturn(datasetModel1);
		when(mockNodeModelCreator.createEULA(anyString())).thenReturn(eula1);
		AsyncMockStubber.callSuccessWith("dataset json").when(mockNodeService).getNodeJSON(eq(NodeType.DATASET), eq(datasetModel1.getId()), any(AsyncCallback.class));		
		AsyncMockStubber.callSuccessWith(false).when(mockLicenseService).hasAccepted(eq(user1.getEmail()), eq(eula1.getId()), eq(datasetModel1.getId()), any(AsyncCallback.class));
		AsyncMockStubber.callSuccessWith("eula json").when(mockNodeService).getNodeJSON(eq(NodeType.EULA), eq(eula1.getId()), any(AsyncCallback.class));
		datasetPresenter.refreshFromServer();
		datasetPresenter.setLicenseAgreement();
		verify(mockView).requireLicenseAcceptance(true);
		verify(mockView).setLicenseAgreement(licenseAgreement);
		
		// Dataset has eula, hasAccepted failure
		resetMocks();
		datasetModel1.setEulaId(eula1.getId());
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(user1);
		when(mockNodeModelCreator.createDataset(anyString())).thenReturn(datasetModel1);
		when(mockNodeModelCreator.createEULA(anyString())).thenReturn(eula1);
		AsyncMockStubber.callSuccessWith("dataset json").when(mockNodeService).getNodeJSON(eq(NodeType.DATASET), eq(datasetModel1.getId()), any(AsyncCallback.class));		
		AsyncMockStubber.callFailureWith(new Throwable("error message")).when(mockLicenseService).hasAccepted(eq(user1.getEmail()), eq(eula1.getId()), eq(datasetModel1.getId()), any(AsyncCallback.class));
		datasetPresenter.refreshFromServer();
		datasetPresenter.setLicenseAgreement();
		verifyLicenceAgreementFailure();

		// Dataset has eula, get eula failure
		resetMocks();
		datasetModel1.setEulaId(eula1.getId());
		when(mockAuthenticationController.getLoggedInUser()).thenReturn(user1);
		when(mockNodeModelCreator.createDataset(anyString())).thenReturn(datasetModel1);
		when(mockNodeModelCreator.createEULA(anyString())).thenReturn(eula1);
		AsyncMockStubber.callSuccessWith("dataset json").when(mockNodeService).getNodeJSON(eq(NodeType.DATASET), eq(datasetModel1.getId()), any(AsyncCallback.class));		
		AsyncMockStubber.callSuccessWith(false).when(mockLicenseService).hasAccepted(eq(user1.getEmail()), eq(eula1.getId()), eq(datasetModel1.getId()), any(AsyncCallback.class));
		AsyncMockStubber.callFailureWith(new Throwable("error message")).when(mockNodeService).getNodeJSON(eq(NodeType.EULA), eq(eula1.getId()), any(AsyncCallback.class));
		datasetPresenter.refreshFromServer();
		datasetPresenter.setLicenseAgreement();
		verify(mockView).requireLicenseAcceptance(true);
		verifyLicenceAgreementFailure();		
	}

	private void verifyLicenceAgreementFailure() {
		verify(mockView).showErrorMessage(anyString());
		verify(mockView).disableLicensedDownloads(true);
	}

	private void resetMocks() {
		reset(mockView);
		reset(mockNodeService);
		reset(mockLicenseService);
		reset(mockNodeModelCreator);
		reset(mockAuthenticationController);
		reset(mockGlobalApplicationState);
	}
	
}
