package org.sagebionetworks.web.client.mvp;


import java.util.logging.Level;
import java.util.logging.Logger;

import org.sagebionetworks.web.client.PortalGinInjector;
import org.sagebionetworks.web.client.place.Dataset;
import org.sagebionetworks.web.client.place.DatasetsHome;
import org.sagebionetworks.web.client.place.Home;
import org.sagebionetworks.web.client.place.Layer;
import org.sagebionetworks.web.client.place.LoginPlace;
import org.sagebionetworks.web.client.place.Project;
import org.sagebionetworks.web.client.place.ProjectsHome;
import org.sagebionetworks.web.client.place.users.PasswordReset;
import org.sagebionetworks.web.client.place.users.RegisterAccount;
import org.sagebionetworks.web.client.presenter.DatasetPresenter;
import org.sagebionetworks.web.client.presenter.DatasetsHomePresenter;
import org.sagebionetworks.web.client.presenter.HomePresenter;
import org.sagebionetworks.web.client.presenter.LayerPresenter;
import org.sagebionetworks.web.client.presenter.LoginPresenter;
import org.sagebionetworks.web.client.presenter.ProjectPresenter;
import org.sagebionetworks.web.client.presenter.ProjectsHomePresenter;
import org.sagebionetworks.web.client.presenter.users.PasswordResetPresenter;
import org.sagebionetworks.web.client.presenter.users.RegisterAccountPresenter;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;

public class AppActivityMapper implements ActivityMapper {
	
	private static Logger log = Logger.getLogger(AppActivityMapper.class.getName());
	private PortalGinInjector ginjector;
	

	/**
	 * AppActivityMapper associates each Place with its corresponding
	 * {@link Activity}
	 * @param clientFactory
	 *            Factory to be passed to activities
	 */
	public AppActivityMapper(PortalGinInjector ginjector) {
		super();
		this.ginjector = ginjector;
	}

	@Override
	public Activity getActivity(Place place) {
		// If the user is not logged in then we redirect them to the login screen
		// except for the fully public pages: LoginPlace, PasswordReset and RegisterAccount
		if(!(place  instanceof LoginPlace) && !(place instanceof PasswordReset) && !(place instanceof RegisterAccount)){
			if(!this.ginjector.getAuthenticationController().isLoggedIn()){
				// Redirect them to the login screen
				LoginPlace loginPlace = new LoginPlace(place);
				return getActivity(loginPlace);
			}
		}
		
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
		}else if (place instanceof ProjectsHome) {
			// Projects Home 
			ProjectsHomePresenter presenter = ginjector.getProjectsHomePresenter();
			presenter.setPlace((ProjectsHome)place);
			return presenter;
		}else if (place instanceof Project) {
			// Projects Home 
			ProjectPresenter presenter = ginjector.getProjectPresenter();
			presenter.setPlace((Project)place);
			return presenter;
		}else if (place instanceof LoginPlace) {
			// login view
			LoginPresenter presenter = ginjector.getLoginPresenter();
			presenter.setPlace((LoginPlace)place);
			return presenter;
		} else if (place instanceof PasswordReset) {
			// reset passwords
			PasswordResetPresenter presenter = ginjector.getPasswordResetPresenter();
			presenter.setPlace((PasswordReset)place);
			return presenter;
		} else if (place instanceof RegisterAccount) {
			// register for a new account
			RegisterAccountPresenter presenter = ginjector.getRegisterAccountPresenter();
			presenter.setPlace((RegisterAccount)place);
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
