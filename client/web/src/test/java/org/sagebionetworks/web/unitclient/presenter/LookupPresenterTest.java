package org.sagebionetworks.web.unitclient.presenter;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.place.Lookup;
import org.sagebionetworks.web.client.presenter.LookupPresenter;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.services.NodeServiceAsync;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.view.LookupView;
import org.sagebionetworks.web.shared.EntityTypeResponse;
import org.sagebionetworks.web.shared.NodeType;
import org.sagebionetworks.web.shared.exceptions.RestServiceException;
import org.sagebionetworks.web.test.helper.AsyncMockStubber;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

public class LookupPresenterTest {

	LookupPresenter lookupPresenter;
	LookupView mockView;
	GlobalApplicationState mockGlobalApplicationState;
	NodeServiceAsync mockNodeService;
	NodeModelCreator mockNodeModelCreator;
	AuthenticationController mockAuthenticationController;
	
	@Before
	public void setup(){
		mockView = mock(LookupView.class);
		mockGlobalApplicationState = mock(GlobalApplicationState.class);
		mockNodeService = mock(NodeServiceAsync.class);
		mockNodeModelCreator = mock(NodeModelCreator.class);
		mockAuthenticationController = mock(AuthenticationController.class);

		lookupPresenter = new LookupPresenter(mockView, mockGlobalApplicationState, mockNodeService, mockNodeModelCreator, mockAuthenticationController);
		
		verify(mockView).setPresenter(lookupPresenter);
	}	
	
	@Test
	public void testSetPlace() {
		Lookup place = Mockito.mock(Lookup.class);
		lookupPresenter.setPlace(place);
	}	
	
	@SuppressWarnings("unchecked")
	@Test 
	public void testDoLookupEntity() throws RestServiceException {
		String entityId = "555";			
		EntityTypeResponse etr = new EntityTypeResponse();
		etr.setId(entityId);
		etr.setName("name");
		etr.setType("/dataset");
		String etrJson = "{ any string }";
		
		// service failure
		resetMocks();
		when(mockNodeModelCreator.createEntityTypeResponse(Mockito.anyString())).thenReturn(etr);
		AsyncMockStubber.callFailureWith(new Throwable("error")).when(mockNodeService).getNodeType(eq(entityId), any(AsyncCallback.class));		
		lookupPresenter.doLookupEntity(entityId);
		verify(mockView).showLooking(entityId);
		verify(mockView).showLookupFailed(entityId);
		
		// failure: unknown entity type
		String unknownType = "unknowntype";
		etr.setType("/" + unknownType);
		resetMocks();
		when(mockNodeModelCreator.createEntityTypeResponse(Mockito.anyString())).thenReturn(etr);
		AsyncMockStubber.callSuccessWith(etrJson).when(mockNodeService).getNodeType(eq(entityId), any(AsyncCallback.class));		
		lookupPresenter.doLookupEntity(entityId);		
		verify(mockView).showLooking(entityId);
		verify(mockView).showUnknownType(unknownType, entityId);
		
		// success: entities that have places
		for(NodeType nodeType : NodeType.values()) {
			if (nodeType == NodeType.EULA || nodeType == NodeType.AGREEMENT
					|| nodeType == NodeType.ENTITY) {
				// skip entities that do not have pages
				continue;
			}			
			etr.setType("/" + nodeType.toString().toLowerCase());
			resetMocks();
			when(mockNodeModelCreator.createEntityTypeResponse(Mockito.anyString())).thenReturn(etr);
			AsyncMockStubber.callSuccessWith(etrJson).when(mockNodeService).getNodeType(eq(entityId), any(AsyncCallback.class));		
			lookupPresenter.doLookupEntity(entityId);		
			verify(mockView).showLooking(entityId);
			verify(mockView).showForwarding();		
			verify(mockView).doneLooking();
			
		}
			
	}
	
	private void resetMocks() {
		reset(mockView);
		reset(mockGlobalApplicationState);
		reset(mockNodeService);
		reset(mockNodeModelCreator);
		reset(mockAuthenticationController);
	}
}
