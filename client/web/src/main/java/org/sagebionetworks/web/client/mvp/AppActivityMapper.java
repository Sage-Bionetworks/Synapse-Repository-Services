package org.sagebionetworks.web.client.mvp;


import java.util.logging.Level;
import java.util.logging.Logger;

import org.sagebionetworks.web.client.ProtalGinInjector;
import org.sagebionetworks.web.client.place.AllDatasets;
import org.sagebionetworks.web.client.place.Dataset;
import org.sagebionetworks.web.client.place.DynamicTest;
import org.sagebionetworks.web.client.presenter.AllDatasetPresenter;
import org.sagebionetworks.web.client.presenter.DatasetPresenter;
import org.sagebionetworks.web.client.presenter.DynamicTablePresenter;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;

public class AppActivityMapper implements ActivityMapper {
	
	private static Logger log = Logger.getLogger(AppActivityMapper.class.getName());
	private ProtalGinInjector ginjector;
	

	/**
	 * AppActivityMapper associates each Place with its corresponding
	 * {@link Activity}
	 * @param clientFactory
	 *            Factory to be passed to activities
	 */
	public AppActivityMapper(ProtalGinInjector ginjector) {
		super();
		this.ginjector = ginjector;
	}

	@Override
	public Activity getActivity(Place place) {
		// We use GIN to generate and inject all presenters with 
		// their dependencies.
		if(place instanceof AllDatasets){
			
//			DynamicTableTest presenter = ginjector.getDynamicTableTest();
//			// set this presenter's place
//			presenter.setPlace(null);
//			return presenter;
			
			AllDatasetPresenter presenter = ginjector.getAllDatasetsPresenter();
			// set this presenter's place
			presenter.setPlace((AllDatasets)place);
			return presenter;
		}else if(place instanceof Dataset){
			DatasetPresenter presenter = ginjector.getDatasetPresenter();
			// set this presenter's place
			presenter.setPlace((Dataset)place);
			return presenter;
		}else if(place instanceof DynamicTest){
			DynamicTablePresenter presenter = ginjector.getDynamicTableTest();
			// set this presenter's place
			presenter.setPlace((DynamicTest)place);
			return presenter;
		}else{
			// Log that we have an unknown place but send the user to the default
			log.log(Level.WARNING, "Unknown Place: "+place.getClass().getName());
			// Go to the default place
			return getActivity(getDefaultPlace());
		}
	}

	/**
	 * Get the default place
	 * @return
	 */
	public Place getDefaultPlace() {
		return new AllDatasets(null);
	}

}
