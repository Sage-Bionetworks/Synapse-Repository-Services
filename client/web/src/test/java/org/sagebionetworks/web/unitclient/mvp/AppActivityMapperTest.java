package org.sagebionetworks.web.unitclient.mvp;

import org.junit.Test;
import org.sagebionetworks.web.client.ProtalGinInjector;
import org.sagebionetworks.web.client.mvp.AppActivityMapper;
import org.sagebionetworks.web.client.place.AllDatasets;
import org.sagebionetworks.web.client.place.Dataset;
import org.sagebionetworks.web.client.presenter.AllDatasetPresenter;
import org.sagebionetworks.web.client.presenter.DatasetPresenter;


import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.Place;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

/**
 * Test the activity mapper.
 * 
 * @author jmhill
 *
 */
public class AppActivityMapperTest {
	
	@Test
	public void testAllDatasets(){
		// Mock the views
		ProtalGinInjector mockInjector = createMock(ProtalGinInjector.class);
		// Mock the v
//		AllDatasetPresenter allDatasetsPresenter = new AllDatasetPresenter(view, datasetService);
		AllDatasetPresenter mockAll = createMock(AllDatasetPresenter.class);
		expect(mockInjector.getAllDatasetsPresenter()).andReturn(mockAll);
		AllDatasets allDatestsPlace = new AllDatasets(null);
		// The place must be set on the presenter
		mockAll.setPlace(allDatestsPlace);
		replay(mockAll);
		replay(mockInjector);
		// Create the mapper
		AppActivityMapper mapper = new AppActivityMapper(mockInjector);

		Activity object = mapper.getActivity(allDatestsPlace);
		assertNotNull(object);
		assertTrue(object instanceof AllDatasetPresenter);
		// Validate that the place was set.
		verify(mockAll);
	}
	
	@Test
	public void testDatasets(){
		// Mock the views
		ProtalGinInjector mockInjector = createMock(ProtalGinInjector.class);
		// Mock the v
		DatasetPresenter mockPresenter = createMock(DatasetPresenter.class);
		expect(mockInjector.getDatasetPresenter()).andReturn(mockPresenter);
		Dataset datasetPlace = new Dataset("ID");
		// The place must be set on the presenter
		mockPresenter.setPlace(datasetPlace);
		replay(mockPresenter);
		replay(mockInjector);
		// Create the mapper
		AppActivityMapper mapper = new AppActivityMapper(mockInjector);

		Activity object = mapper.getActivity(datasetPlace);
		assertNotNull(object);
		assertTrue(object instanceof DatasetPresenter);
//		AllDatasetPresenter resultPresenter =  (AllDatasetPresenter) object;
		// Validate that the place was set.
		verify(mockPresenter);
	}
	
	
	@Test
	public void testUnknown(){
		// Mock the views
		ProtalGinInjector mockInjector = createMock(ProtalGinInjector.class);
		// Mock the presenter
		AllDatasetPresenter mockAll = createMock(AllDatasetPresenter.class);
		expect(mockInjector.getAllDatasetsPresenter()).andReturn(mockAll);
		// This is the place we will pass in
		Place unknownPlace = new Place(){}; 
		// This is the place we expect to be used.
		AllDatasets allDatestsPlace = new AllDatasets(null);
		// The place must be set on the presenter
		mockAll.setPlace((AllDatasets) anyObject());
		replay(mockAll);
		replay(mockInjector);
		// Create the mapper
		AppActivityMapper mapper = new AppActivityMapper(mockInjector);

		Activity object = mapper.getActivity(unknownPlace);
		assertNotNull(object);
		assertTrue(object instanceof AllDatasetPresenter);
		// Validate that the place was set.
		verify(mockAll);
	}

}
