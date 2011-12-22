package org.sagebionetworks.web.client;

import org.sagebionetworks.web.client.presenter.AnalysesHomePresenter;
import org.sagebionetworks.web.client.presenter.AnalysisPresenter;
import org.sagebionetworks.web.client.presenter.ComingSoonPresenter;
import org.sagebionetworks.web.client.presenter.DatasetPresenter;
import org.sagebionetworks.web.client.presenter.DatasetsHomePresenter;
import org.sagebionetworks.web.client.presenter.EntityPresenter;
import org.sagebionetworks.web.client.presenter.HomePresenter;
import org.sagebionetworks.web.client.presenter.LayerPresenter;
import org.sagebionetworks.web.client.presenter.LoginPresenter;
import org.sagebionetworks.web.client.presenter.LookupPresenter;
import org.sagebionetworks.web.client.presenter.PhenoEditPresenter;
import org.sagebionetworks.web.client.presenter.ProfilePresenter;
import org.sagebionetworks.web.client.presenter.ProjectPresenter;
import org.sagebionetworks.web.client.presenter.ProjectsHomePresenter;
import org.sagebionetworks.web.client.presenter.PublicProfilePresenter;
import org.sagebionetworks.web.client.presenter.StepPresenter;
import org.sagebionetworks.web.client.presenter.StepsHomePresenter;
import org.sagebionetworks.web.client.presenter.users.PasswordResetPresenter;
import org.sagebionetworks.web.client.presenter.users.RegisterAccountPresenter;
import org.sagebionetworks.web.client.security.AuthenticationController;

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;

/**
 * The root portal dependency injection root.
 * 
 * @author jmhill
 *
 */
@GinModules(PortalGinModule.class)
public interface PortalGinInjector extends Ginjector {

	public GlobalApplicationState getGlobalApplicationState();
	
	public HomePresenter getHomePresenter();
	
	public DatasetsHomePresenter getDatasetsHomePresenter();
	
	public EntityPresenter getEntityPresenter();
	
	public DatasetPresenter getDatasetPresenter();
		
	public LayerPresenter getLayerPresenter();
	
	public ProjectsHomePresenter getProjectsHomePresenter();
	
	public ProjectPresenter getProjectPresenter();
	
	public AnalysesHomePresenter getAnalysesHomePresenter();
	
	public AnalysisPresenter getAnalysisPresenter();

	public StepsHomePresenter getStepsHomePresenter();
	
	public StepPresenter getStepPresenter();

	public LoginPresenter getLoginPresenter();
	
	public AuthenticationController getAuthenticationController();
	
	public PasswordResetPresenter getPasswordResetPresenter();
	
	public RegisterAccountPresenter getRegisterAccountPresenter();

	public ProfilePresenter getProfilePresenter();

	public ComingSoonPresenter getComingSoonPresenter();
	
	public PhenoEditPresenter getPhenoEditPresenter();
	
	public LookupPresenter getLookupPresenter();
	
	public PublicProfilePresenter getPublicProfilePresenter();
}
