package org.sagebionetworks.web.unitclient.presenter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.place.Analysis;
import org.sagebionetworks.web.client.presenter.AnalysisPresenter;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.view.AnalysisView;

public class AnalysisPresenterTest {

	AnalysisPresenter analysisPresenter;
	AnalysisView mockView;
	GlobalApplicationState mockGlobalApplicationState;
	NodeServiceAsync mockNodeService;
	NodeModelCreator mockNodeModelCreator;
	AuthenticationController mockAuthenticationController;

	String projectId = "1";
	String analysisId = "2";
	org.sagebionetworks.repo.model.Analysis analysisModel1;
	
	@Before
	public void setup(){
		mockView = mock(AnalysisView.class);
		mockGlobalApplicationState = mock(GlobalApplicationState.class);
		mockNodeService = mock(NodeServiceAsync.class);
		mockNodeModelCreator = mock(NodeModelCreator.class);
		mockAuthenticationController = mock(AuthenticationController.class);
		analysisPresenter = new AnalysisPresenter(mockView, mockNodeService, mockNodeModelCreator, mockAuthenticationController, mockGlobalApplicationState);
		
		// Analysis object
		analysisModel1 = new org.sagebionetworks.repo.model.Analysis();
		analysisModel1.setId(analysisId);
		analysisModel1.setName("test analysis");
		analysisModel1.setParentId(projectId);
		analysisModel1.setDescription("test description");

		verify(mockView).setPresenter(analysisPresenter);
	}	
	
	@Test
	public void testSetPlace() {
		Analysis place = new Analysis(analysisId);
		analysisPresenter.setPlace(place);
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
