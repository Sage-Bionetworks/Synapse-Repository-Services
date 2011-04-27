package org.sagebionetworks.web.client.mvp;


import java.util.logging.Level;
import java.util.logging.Logger;

import org.sagebionetworks.web.client.ProtalGinInjector;
import org.sagebionetworks.web.client.place.DatasetPlace;
import org.sagebionetworks.web.client.place.DatasetsHomePlace;
import org.sagebionetworks.web.client.place.HomePlace;
import org.sagebionetworks.web.client.place.LayerPlace;
import org.sagebionetworks.web.client.place.LoginPlace;
import org.sagebionetworks.web.client.presenter.DatasetPresenter;
import org.sagebionetworks.web.client.presenter.DatasetsHomePresenter;
import org.sagebionetworks.web.client.presenter.HomePresenter;
import org.sagebionetworks.web.client.presenter.LayerPresenter;
import org.sagebionetworks.web.client.presenter.LoginPresenter;

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
		// If the user is not logged in then we redirect them to the login screen
		if(!(place  instanceof LoginPlace)){
			if(!this.ginjector.getAuthenticationController().isLoggedIn()){
				// Redirect them to the login screen
				LoginPlace loginPlace = new LoginPlace(place);
				return getActivity(loginPlace);
			}
		}
		
		// We use GIN to generate and inject all presenters with 
		// their dependencies.
		if(place instanceof HomePlace) {
			HomePresenter presenter = ginjector.getHomePresenter();
			presenter.setPlace((HomePlace)place);
			return presenter;
		} else if(place instanceof DatasetsHomePlace){
			// The home page for all datasets
			DatasetsHomePresenter presenter = ginjector.getDatasetsHomePresenter();
			// set this presenter's place
			presenter.setPlace((DatasetsHomePlace)place);
			return presenter;
		}else if(place instanceof DatasetPlace){
			DatasetPresenter presenter = ginjector.getDatasetPresenter();
			// set this presenter's place
			presenter.setPlace((DatasetPlace)place);
			return presenter;
		}else if (place instanceof LayerPlace) {
			// The layer detail view
			LayerPresenter presenter = ginjector.getLayerPresenter();
			presenter.setPlace((LayerPlace)place);
			return presenter;
		}else if (place instanceof LoginPlace) {
			// The layer detail view
			LoginPresenter presenter = ginjector.getLoginPresenter();
			presenter.setPlace((LoginPlace)place);
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
		return new HomePlace(null);
	}

}
