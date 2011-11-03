package org.sagebionetworks.web.unitclient.presenter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.place.Step;
import org.sagebionetworks.web.client.presenter.StepPresenter;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.view.StepView;

public class StepPresenterTest {

	StepPresenter stepPresenter;
	StepView mockView;
	GlobalApplicationState mockGlobalApplicationState;
	NodeServiceAsync mockNodeService;
	NodeModelCreator mockNodeModelCreator;
	AuthenticationController mockAuthenticationController;

	String analysisId = "1";
	String stepId = "2";
	org.sagebionetworks.repo.model.Step stepModel1;
	
	@Before
	public void setup(){
		mockView = mock(StepView.class);
		mockGlobalApplicationState = mock(GlobalApplicationState.class);
		mockNodeService = mock(NodeServiceAsync.class);
		mockNodeModelCreator = mock(NodeModelCreator.class);
		mockAuthenticationController = mock(AuthenticationController.class);
		stepPresenter = new StepPresenter(mockView, mockNodeService, mockNodeModelCreator, mockAuthenticationController, mockGlobalApplicationState);
		
		// Step object
		stepModel1 = new org.sagebionetworks.repo.model.Step();
		stepModel1.setId(stepId);
		stepModel1.setName("test step");
		stepModel1.setParentId(analysisId);
		stepModel1.setCommandLine("test commandLine");

		verify(mockView).setPresenter(stepPresenter);
	}	
	
	@Test
	public void testSetPlace() {
		Step place = new Step(stepId);
		stepPresenter.setPlace(place);
	}	
	
	/*
	 * Private methods
	 */
	private void resetMocks() {
		reset(mockView);
		reset(mockNodeService);
		reset(mockNodeModelCreator);
		reset(mockAuthenticationController);
		reset(mockGlobalApplicationState);
	}	
}
