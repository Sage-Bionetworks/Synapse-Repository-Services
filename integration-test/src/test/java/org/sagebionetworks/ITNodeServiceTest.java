package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.web.server.RestTemplateProviderImpl;
import org.sagebionetworks.web.server.servlet.NodeServiceImpl;
import org.sagebionetworks.web.server.servlet.ServiceUrlProvider;
import org.sagebionetworks.web.server.servlet.TokenProvider;
import org.sagebionetworks.web.shared.NodeType;

public class ITNodeServiceTest {
	
	private static Synapse synapse;
	private static NodeServiceImpl nodeService;
	JSONObject project;
	JSONObject dataset;
	
	List<String> toDelete;
	
	@BeforeClass
	public static void beforeClass() throws Exception{
		// Use the synapse client to do some of the work for us.
		synapse = new Synapse();
		synapse.setAuthEndpoint(StackConfiguration
				.getAuthenticationServicePrivateEndpoint());
		synapse.setRepositoryEndpoint(StackConfiguration
				.getRepositoryServiceEndpoint());
		synapse.login(StackConfiguration.getIntegrationTestUserOneName(),
				StackConfiguration.getIntegrationTestUserOnePassword());
		
		// Create a template provider
		RestTemplateProviderImpl tempalteProvider = new RestTemplateProviderImpl(1000, 1);
		ServiceUrlProvider urlProvider = new ServiceUrlProvider();

		// Capture the session token
		final String sessionToken = synapse.getCurrentSessionToken();
		// The search service.		
		nodeService = new NodeServiceImpl();
		nodeService.setRestTemplate(tempalteProvider);
		nodeService.setServiceUrlProvider(urlProvider);
		// We want to user the same session token.
		nodeService.setTokenProvider(new TokenProvider() {
			@Override
			public String getSessionToken() {
				return sessionToken;
			}
		});

	}
	
	@Before
	public void before() throws Exception{
		this.toDelete = new ArrayList<String>();
		// Create a project
		project = synapse.createEntity("/project", new JSONObject());
		toDelete.add(project.getString("uri"));
		// Create a datasets
		dataset = new JSONObject();
		dataset.put("parentId", project.getString("id"));
		dataset = synapse.createEntity("/dataset", dataset);
	}
	
	@After
	public void after(){
		// Delete all entities
		if(this.toDelete != null && synapse != null){
			for(String uri: toDelete){
				try{
					synapse.deleteEntity(uri);
				}catch(Throwable e){}
			}
		}
	}
	
	@Test
	public void getNodeAclJSON() throws Exception{
		// We should be able to get the project ACL
		String result = nodeService.getNodeAclJSON(NodeType.PROJECT, project.getString("id"));
		assertNotNull(result);
		JSONObject projectAcl = new JSONObject(result);
		assertEquals(project.getString("id"), projectAcl.getString("id"));
		// Now try to get the ACL for the dataset that inherits from the project.
		// The node service should automatically resolve the 404.
		result = nodeService.getNodeAclJSON(NodeType.STUDY, dataset.getString("id"));
		assertNotNull("Failed to get the ACL for a dataset that inherits its permissions from its parent project",result);
		projectAcl = new JSONObject(result);
		assertEquals(project.getString("id"), projectAcl.getString("id"));
	}

}
