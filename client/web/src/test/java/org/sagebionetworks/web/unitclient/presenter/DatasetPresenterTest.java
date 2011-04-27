package org.sagebionetworks.web.unitclient.presenter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.DatasetServiceAsync;
import org.sagebionetworks.web.client.place.Dataset;
import org.sagebionetworks.web.client.presenter.DatasetPresenter;
import org.sagebionetworks.web.client.view.DatasetView;
import org.sagebionetworks.web.client.widget.licenseddownloader.LicenceServiceAsync;

public class DatasetPresenterTest {
	
	DatasetPresenter datasetPresenter;
	DatasetView mockView;
	DatasetServiceAsync mockDatasetService;
	LicenceServiceAsync mockLicenseService;
	
	@Before
	public void setup(){
		mockView = mock(DatasetView.class);
		mockDatasetService = mock(DatasetServiceAsync.class);
		mockLicenseService = mock(LicenceServiceAsync.class);
		datasetPresenter = new DatasetPresenter(mockView, mockDatasetService, mockLicenseService);
		
		verify(mockView).setPresenter(datasetPresenter);
	}	
	
	@Test
	public void testSetPlace() {
		Dataset place = Mockito.mock(Dataset.class);
		datasetPresenter.setPlace(place);
	}	
	
	
	
}
