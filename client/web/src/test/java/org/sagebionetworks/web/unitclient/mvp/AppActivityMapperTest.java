package org.sagebionetworks.web.unitclient.mvp;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.sagebionetworks.web.client.ProtalGinInjector;
import org.sagebionetworks.web.client.mvp.AppActivityMapper;
import org.sagebionetworks.web.client.place.Dataset;
import org.sagebionetworks.web.client.place.DatasetsHome;
import org.sagebionetworks.web.client.place.Home;
import org.sagebionetworks.web.client.presenter.DatasetPresenter;
import org.sagebionetworks.web.client.presenter.DatasetsHomePresenter;
import org.sagebionetworks.web.client.presenter.HomePresenter;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.place.shared.Place;

/**
 * Test the activity mapper.
 * 
 * @author jmhill
 *
 */
public class AppActivityMapperTest {
	
	@Test
	public void testDatasetsHome(){
		// Mock the views
		ProtalGinInjector mockInjector = createMock(ProtalGinInjector.class);
		// Mock the v
//		AllDatasetPresenter allDatasetsPresenter = new AllDatasetPresenter(view, datasetService);
		DatasetsHomePresenter mockHome = createMock(DatasetsHomePresenter.class);
		expect(mockInjector.getDatasetsHomePresenter()).andReturn(mockHome);
		DatasetsHome allDatestsPlace = new DatasetsHome(null);
		// The place must be set on the presenter
		mockHome.setPlace(allDatestsPlace);
		replay(mockHome);
		replay(mockInjector);
		// Create the mapper
		AppActivityMapper mapper = new AppActivityMapper(mockInjector);

		Activity object = mapper.getActivity(allDatestsPlace);
		assertNotNull(object);
		assertTrue(object instanceof DatasetsHomePresenter);
		// Validate that the place was set.
		verify(mockHome);
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
		HomePresenter mockAll = createMock(HomePresenter.class);
		expect(mockInjector.getHomePresenter()).andReturn(mockAll);
		// This is the place we will pass in
		Place unknownPlace = new Place(){}; 
		// This is the place we expect to be used.
		Home allDatestsPlace = new Home(null);
		// The place must be set on the presenter
		mockAll.setPlace((Home) anyObject());
		replay(mockAll);
		replay(mockInjector);
		// Create the mapper
		AppActivityMapper mapper = new AppActivityMapper(mockInjector);

		Activity object = mapper.getActivity(unknownPlace);
		assertNotNull(object);
		assertTrue(object instanceof HomePresenter);
		// Validate that the place was set.
		verify(mockAll);
	}

}
