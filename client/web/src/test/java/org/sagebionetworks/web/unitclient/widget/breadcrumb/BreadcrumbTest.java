package org.sagebionetworks.web.unitclient.widget.breadcrumb;

import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.sagebionetworks.web.client.EntityTypeProvider;
import org.sagebionetworks.web.client.GlobalApplicationState;
import org.sagebionetworks.web.client.SynapseClientAsync;
import org.sagebionetworks.web.client.security.AuthenticationController;
import org.sagebionetworks.web.client.transform.NodeModelCreator;
import org.sagebionetworks.web.client.widget.breadcrumb.Breadcrumb;
import org.sagebionetworks.web.client.widget.breadcrumb.BreadcrumbView;
import org.sagebionetworks.web.server.servlet.SynapseClientImpl;
import org.sagebionetworks.web.shared.EntityWrapper;
import org.sagebionetworks.web.test.helper.AsyncMockStubber;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class BreadcrumbTest {
		
	Breadcrumb breadcrumb;
	BreadcrumbView mockView;
	NodeModelCreator mockNodeModelCreator;
	AuthenticationController mockAuthenticationController;
	GlobalApplicationState mockGlobalApplicationState;
	SynapseClientAsync mockSynapseClient;
	EntityTypeProvider entityTypeProvider;
	
	@SuppressWarnings("unchecked")
	@Before
	public void setup(){		
		mockView = Mockito.mock(BreadcrumbView.class);
		mockNodeModelCreator = mock(NodeModelCreator.class);
		mockAuthenticationController = mock(AuthenticationController.class);
		mockGlobalApplicationState = mock(GlobalApplicationState.class);
		mockSynapseClient = mock(SynapseClientAsync.class);
		
		String registryJson = SynapseClientImpl.getEntityTypeRegistryJson();
		AsyncMockStubber.callSuccessWith(registryJson).when(mockSynapseClient).getEntityTypeRegistryJSON(any(AsyncCallback.class));
		entityTypeProvider = new EntityTypeProvider(mockSynapseClient, new JSONObjectAdapterImpl());		
		
		breadcrumb = new Breadcrumb(mockView, mockSynapseClient, mockGlobalApplicationState, mockAuthenticationController, mockNodeModelCreator, entityTypeProvider);
		
		
		verify(mockView).setPresenter(breadcrumb);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testAsWidgetEntity() throws Exception {
		Entity entity = new Dataset();
		entity.setId("3");
		entity.setUri("path/dataset/3");
		List<EntityHeader> pathHeaders = new ArrayList<EntityHeader>();
		
		EntityHeader rootHeader = new EntityHeader();
		rootHeader.setId("1");
		rootHeader.setName("root");
		pathHeaders.add(rootHeader);
		
		EntityHeader projHeader = new EntityHeader();
		projHeader.setId("2");
		projHeader.setName("project");
		pathHeaders.add(projHeader);
		
		EntityHeader dsHeader = new EntityHeader();
		dsHeader.setId("3");
		dsHeader.setName("ds");
		pathHeaders.add(dsHeader);
		
		EntityPath entityPath = new EntityPath();
		entityPath.setPath(pathHeaders);
		JSONObjectAdapter pathAdapter = new JSONObjectAdapterImpl();
		entityPath.writeToJSONObject(pathAdapter);
				
		EntityWrapper entityWrapper = new EntityWrapper();
		entityWrapper.setEntityJson(pathAdapter.toJSONString());		
		
		// Fail path service call
		reset(mockView);		
		AsyncMockStubber.callFailureWith(new Throwable("error message")).when(mockSynapseClient).getEntityPath(eq(entity.getId()), anyString(), any(AsyncCallback.class)); // fail for get Path
		when(mockNodeModelCreator.createEntityPath(any(EntityWrapper.class))).thenReturn(entityPath);
		breadcrumb.asWidget(entity);
		verify(mockView).showErrorMessage(anyString());
		
		// fail model creation
		reset(mockView);			
		AsyncMockStubber.callSuccessWith(entityWrapper).when(mockSynapseClient).getEntityPath(eq(entity.getId()), anyString(), any(AsyncCallback.class));
		when(mockNodeModelCreator.createEntityPath(any(EntityWrapper.class))).thenReturn(null); // null model return
		breadcrumb.asWidget(entity);
		verify(mockView).setLinksList(any(List.class), (String)isNull());
		
		// success test
		reset(mockView);			
		AsyncMockStubber.callSuccessWith(entityWrapper).when(mockSynapseClient).getEntityPath(eq(entity.getId()), anyString(), any(AsyncCallback.class));
		when(mockNodeModelCreator.createEntityPath(entityWrapper)).thenReturn(entityPath);
		breadcrumb.asWidget(entity);
		verify(mockView).setLinksList(any(List.class), (String)isNotNull());				
	}
	
	@Test
	public void testAsWidget(){
		assertNull(breadcrumb.asWidget());
	}
	
	
}
