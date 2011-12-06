package org.sagebionetworks.web.unitclient.presenter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.place.Synapse;
import org.sagebionetworks.web.client.presenter.EntityPresenter;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.view.EntityView;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

public class EntityPresenterTest {
	
	EntityPresenter entityPresenter;
	EntityView mockView;
	GlobalApplicationState mockGlobalApplicationState;
	AuthenticationController mockAuthenticationController;
	SynapseClientAsync mockSynapseClient;
	NodeServiceAsync mockNodeService;
	NodeModelCreator mockNodeModelCreator;
	String EntityId = "1";
	Synapse place = new Synapse("Synapse:"+ EntityId);
	Entity EntityModel1;

	
	@Before
	public void setup(){
		mockView = mock(EntityView.class);
		mockGlobalApplicationState = mock(GlobalApplicationState.class);
		mockAuthenticationController = mock(AuthenticationController.class);
		mockNodeService = mock(NodeServiceAsync.class);
		mockSynapseClient = mock(SynapseClientAsync.class);
		mockNodeModelCreator = mock(NodeModelCreator.class);
		entityPresenter = new EntityPresenter(mockView, mockGlobalApplicationState, mockAuthenticationController, mockNodeService, mockSynapseClient, mockNodeModelCreator);

		verify(mockView).setPresenter(entityPresenter);
	}	
	
	@Test
	public void testSetPlace() {
		resetMocks();
		Synapse place = Mockito.mock(Synapse.class);
		entityPresenter.setPlace(place);
		
		verify(mockView).setPresenter(entityPresenter);
	}	
	
	@Test
	public void testStart() {
		resetMocks();
		entityPresenter = new EntityPresenter(mockView, mockGlobalApplicationState, mockAuthenticationController, mockNodeService, mockSynapseClient, mockNodeModelCreator);
		entityPresenter.setPlace(place);		
		AcceptsOneWidget panel = mock(AcceptsOneWidget.class);
		EventBus eventBus = mock(EventBus.class);		
		
		entityPresenter.start(panel, eventBus);		
		verify(panel).setWidget(mockView);		
	}
	
	private void resetMocks() {
		reset(mockView);
		reset(mockGlobalApplicationState);
	}
	
}
