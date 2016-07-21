package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertNotNull;

import java.util.UUID;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.docker.DockerAuthorizationToken;
import org.sagebionetworks.repo.model.docker.DockerRegistryEventList;
import org.sagebionetworks.repo.model.docker.RegistryEventAction;
import org.sagebionetworks.util.DockerRegistryEventUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DockerManagerImplAutowiredTest {

	private static final String SERVICE = "docker.synapse.org";
	private static final String TYPE = "repository";
	private String repositoryPath;
	private static final String TAG = "lastest";
	private static final String DIGEST = "sha256:10010101";

	
	@Autowired
	private DockerManager dockerManager;
	
	@Autowired
	private EntityManager entityManager;

	@Autowired
	private UserManager userManager;

	private UserInfo userInfo;
	private String projectId;
	
	@Before
	public void setUp() throws Exception {
		NewUser user = new NewUser();
		String username = UUID.randomUUID().toString();
		user.setEmail(username + "@test.com");
		user.setUserName(username);
		userInfo = userManager.getUserInfo(userManager.createUser(user));
		userInfo.getGroups().add(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());

		Project project = new Project();
		project.setName("project" + RandomStringUtils.randomAlphanumeric(10));
		projectId = entityManager.createEntity(userInfo, project, null);
		repositoryPath = projectId+"/path";
	}
	
	@After
	public void tearDown() throws Exception {
		UserInfo adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		entityManager.deleteEntity(adminUserInfo, projectId);
		userManager.deletePrincipal(adminUserInfo, Long.parseLong(userInfo.getId().toString()));
	}

	@Test
	public void testAuthorizeDockerAccess() {
		// test to see if we can push to the project.  Answer should be yes!
		String scope =TYPE+":"+repositoryPath+":push";
		DockerAuthorizationToken token = dockerManager.authorizeDockerAccess(userInfo, SERVICE, scope);
		assertNotNull(token.getToken());
	}
	
	@Test
	public void testDockerRegistryNotification() {
		DockerRegistryEventList events = 
				DockerRegistryEventUtil.createDockerRegistryEvent(
						RegistryEventAction.push, SERVICE, userInfo.getId(), repositoryPath, TAG, DIGEST);
		dockerManager.dockerRegistryNotification(events);
	}

}
