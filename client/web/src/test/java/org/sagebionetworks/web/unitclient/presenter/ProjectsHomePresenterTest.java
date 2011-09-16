package org.sagebionetworks.web.unitclient.presenter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.cookie.CookieProvider;
import org.sagebionetworks.web.client.place.Home;
import org.sagebionetworks.web.client.place.ProjectsHome;
import org.sagebionetworks.web.client.presenter.HomePresenter;
import org.sagebionetworks.web.client.presenter.ProjectsHomePresenter;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.view.HomeView;
import org.sagebionetworks.web.client.view.ProjectsHomeView;

public class ProjectsHomePresenterTest {

	ProjectsHomePresenter projectsHomePresenter;
	ProjectsHomeView mockView;
	GlobalApplicationState mockGlobalApplicationState;
	
	@Before
	public void setup(){
		mockView = mock(ProjectsHomeView.class);
		mockGlobalApplicationState = mock(GlobalApplicationState.class);
		projectsHomePresenter = new ProjectsHomePresenter(mockView, mockGlobalApplicationState);
		
		verify(mockView).setPresenter(projectsHomePresenter);
	}	
	
	@Test
	public void testSetPlace() {
		ProjectsHome place = Mockito.mock(ProjectsHome.class);
		projectsHomePresenter.setPlace(place);
	}	
}
