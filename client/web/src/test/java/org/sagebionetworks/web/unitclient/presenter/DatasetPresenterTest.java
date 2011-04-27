package org.sagebionetworks.web.unitclient.presenter;

import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.DatasetServiceAsync;
import org.sagebionetworks.web.client.place.DatasetPlace;
import org.sagebionetworks.web.client.presenter.DatasetPresenter;
import org.sagebionetworks.web.client.view.DatasetView;
import org.sagebionetworks.web.client.widget.licenseddownloader.LicenceServiceAsync;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

public class DatasetPresenterTest {
	
	DatasetPresenter datasetPresenter;
	DatasetView mockView;
	DatasetServiceAsync mockDatasetService;
	LicenceServiceAsync mockLicenseService;
	int datasetId = 1;
	DatasetPlace place = new DatasetPlace("Dataset:"+ datasetId);
	
	@Before
	public void setup(){
		mockView = mock(DatasetView.class);
		mockDatasetService = mock(DatasetServiceAsync.class);
		mockLicenseService = mock(LicenceServiceAsync.class);
		datasetPresenter = new DatasetPresenter(mockView, mockDatasetService, mockLicenseService);		
		datasetPresenter.setPlace(place);
		
		verify(mockView).setPresenter(datasetPresenter);
	}	
	
	@Test
	public void testSetPlace() {
		DatasetPlace place = Mockito.mock(DatasetPlace.class);
		datasetPresenter.setPlace(place);
	}	
	
	@Test
	public void testStart() {
		reset(mockView);
		reset(mockDatasetService);
		reset(mockLicenseService);
		datasetPresenter = new DatasetPresenter(mockView, mockDatasetService, mockLicenseService);		
		datasetPresenter.setPlace(place);		
		AcceptsOneWidget panel = mock(AcceptsOneWidget.class);
		EventBus eventBus = mock(EventBus.class);		
		
		datasetPresenter.start(panel, eventBus);		
		verify(panel).setWidget(mockView);		
	}
	
	@Test
	public void testRefreshFromServer() {
		reset(mockView);
		reset(mockDatasetService);
		reset(mockLicenseService);
		datasetPresenter = new DatasetPresenter(mockView, mockDatasetService, mockLicenseService);		
		datasetPresenter.setPlace(place);		
		
		datasetPresenter.refreshFromServer();
		
		//verify(mockDatasetService).getDataset(datasetId, new AsyncCallback<Dataset>)
	}
	
}
