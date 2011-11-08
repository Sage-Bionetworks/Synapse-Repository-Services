package org.sagebionetworks.web.unitclient.presenter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.place.StepsHome;
import org.sagebionetworks.web.client.presenter.StepsHomePresenter;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.view.StepsHomeView;

public class StepsHomePresenterTest {

	StepsHomePresenter stepsHomePresenter;
	StepsHomeView mockView;
	AuthenticationController mockAuthenticationController;
	GlobalApplicationState mockGlobalApplicationState;
	
	@Before
	public void setup(){
		mockView = mock(StepsHomeView.class);
		mockAuthenticationController = mock(AuthenticationController.class);
		mockGlobalApplicationState = mock(GlobalApplicationState.class);
		stepsHomePresenter = new StepsHomePresenter(mockView, mockAuthenticationController, mockGlobalApplicationState);
		
		verify(mockView).setPresenter(stepsHomePresenter);
	}	
	
	@Test
	public void testSetPlace() {
		StepsHome place = Mockito.mock(StepsHome.class);
		stepsHomePresenter.setPlace(place);
	}	
}

