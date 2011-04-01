package org.sagebionetworks.web.client.mvp;


import java.util.logging.Level;
import java.util.logging.Logger;

import org.sagebionetworks.web.client.ProtalGinInjector;
import org.sagebionetworks.web.client.place.Dataset;
import org.sagebionetworks.web.client.place.DatasetsHome;
import org.sagebionetworks.web.client.place.Home;
import org.sagebionetworks.web.client.place.Layer;
import org.sagebionetworks.web.client.presenter.DatasetPresenter;
import org.sagebionetworks.web.client.presenter.DatasetsHomePresenter;
import org.sagebionetworks.web.client.presenter.HomePresenter;
import org.sagebionetworks.web.client.presenter.LayerPresenter;

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
		if(place instanceof Home) {
			HomePresenter presenter = ginjector.getHomePresenter();
			presenter.setPlace((Home)place);
			return presenter;
		} else if(place instanceof DatasetsHome){
			// The home page for all datasets
			DatasetsHomePresenter presenter = ginjector.getDatasetsHomePresenter();
			// set this presenter's place
			presenter.setPlace((DatasetsHome)place);
			return presenter;
		}else if(place instanceof Dataset){
			DatasetPresenter presenter = ginjector.getDatasetPresenter();
			// set this presenter's place
			presenter.setPlace((Dataset)place);
			return presenter;
		}else if (place instanceof Layer) {
			// The layer detail view
			LayerPresenter presenter = ginjector.getLayerPresenter();
			presenter.setPlace((Layer)place);
			return presenter;
		} else {
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
		return new Home(null);
	}

}
