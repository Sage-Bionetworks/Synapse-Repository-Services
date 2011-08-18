package org.sagebionetworks.web.unitclient.presenter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

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

import com.google.gwt.event.shared.EventBus;
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
	
	@Before
	public void setup(){
		mockView = mock(DatasetView.class);
		mockNodeService = mock(NodeServiceAsync.class);
		mockLicenseService = mock(LicenceServiceAsync.class);
		mockNodeModelCreator = mock(NodeModelCreator.class);
		mockAuthenticationController = mock(AuthenticationController.class);
		mockGlobalApplicationState = mock(GlobalApplicationState.class);
		datasetPresenter = new DatasetPresenter(mockView, mockNodeService, mockLicenseService, mockNodeModelCreator, mockAuthenticationController, mockGlobalApplicationState);		
		datasetPresenter.setPlace(place);
		
		verify(mockView).setPresenter(datasetPresenter);
	}	
	
	@Test
	public void testSetPlace() {
		Dataset place = Mockito.mock(Dataset.class);
		datasetPresenter.setPlace(place);
	}	
	
	@Test
	public void testStart() {
		reset(mockView);
		reset(mockNodeService);
		reset(mockLicenseService);
		reset(mockNodeModelCreator);
		reset(mockAuthenticationController);
		reset(mockGlobalApplicationState);
		datasetPresenter = new DatasetPresenter(mockView, mockNodeService, mockLicenseService, mockNodeModelCreator, mockAuthenticationController, mockGlobalApplicationState);		
		datasetPresenter.setPlace(place);		
		AcceptsOneWidget panel = mock(AcceptsOneWidget.class);
		EventBus eventBus = mock(EventBus.class);		
		
		datasetPresenter.start(panel, eventBus);		
		verify(panel).setWidget(mockView);		
	}
	
	@Test
	public void testRefreshFromServer() {
		reset(mockView);
		reset(mockNodeService);
		reset(mockLicenseService);
		reset(mockNodeModelCreator);
		reset(mockAuthenticationController);
		reset(mockGlobalApplicationState);
		datasetPresenter = new DatasetPresenter(mockView, mockNodeService, mockLicenseService, mockNodeModelCreator, mockAuthenticationController, mockGlobalApplicationState);		
		datasetPresenter.setPlace(place);		
		
		datasetPresenter.refreshFromServer();		
	}
	
}
