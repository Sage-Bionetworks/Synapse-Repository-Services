package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.docker.DockerAuthorizationToken;
import org.springframework.beans.factory.annotation.Autowired;

public class DockerAuthorizationControllerAutowiredTest extends AbstractAutowiredControllerTestBase {

	private Long adminUserId;
	private String service;
	
	@Autowired
	UserManager userManager;
	
	@Autowired
	NodeManager nodeManager;
	
	List<String> cleanupIdList;
	

	@Before
	public void before() throws Exception {
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		service = "docker.synapse.org";
		cleanupIdList = new ArrayList<String>();
	}
	
	@After
	public void after(){
		//cleanup
		UserInfo adminUserInfo = userManager.getUserInfo(adminUserId);
		for(String id : cleanupIdList){
			nodeManager.delete(adminUserInfo, id);
		}
	}

	@Test
	public void testLogin() throws Exception {
		DockerAuthorizationToken dockerAuthorizationToken = servletTestHelper.authorizeDockerAccess(dispatchServlet, adminUserId, service, null);
		assertNotNull(dockerAuthorizationToken.getToken());
	}
	
	@Test
	public void testMultipleScopes() throws Exception{
		//Setup: create a project
		Project p = new Project();
		p.setName("Create without entity type");
		Project clone = (Project) entityServletHelper.createEntity(p, adminUserId, null);
		String id = clone.getId();
		cleanupIdList.add(id);
		
		//get the auth token
		DockerAuthorizationToken dockerAuthorizationToken= servletTestHelper.authorizeDockerAccess(dispatchServlet, adminUserId, service, new String[] {"repository:"+id+"/example:push,pull", "repository:"+id+"/example2:push" });
		String token = dockerAuthorizationToken.getToken();
		assertNotNull(token);
		
		String[] splitTokens = token.split("\\.");
		assertEquals(3, splitTokens.length);
		
		//verifying claimSet is giving push and pull permissions.
		String claimSetBase64 = splitTokens[1];
		String claimSetJsonString = new String(Base64.decodeBase64(claimSetBase64), StandardCharsets.UTF_8);
		JSONObject claimSetJson= new JSONObject(claimSetJsonString);
		JSONArray accessJsonArray = claimSetJson.getJSONArray("access");
		for(int i = 0; i < accessJsonArray.length(); i++)
		{
		    JSONObject accessJsonItem = accessJsonArray.getJSONObject(i);
		    String actionsJsonString =  accessJsonItem.getJSONArray("actions").toString();
		    assertTrue(actionsJsonString.contains("\"pull\""));
		    assertTrue(actionsJsonString.contains("\"push\""));

		}
		
		//not testing header(index 0 of split) because it is already covered by unit test
		//not testing signature (index 2 of split) because it changes every time
		
	}

}
