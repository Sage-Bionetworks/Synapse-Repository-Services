package org.sagebionetworks.repo.web.controller;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.manager.oauth.OpenIDConnectManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.docker.DockerCommit;
import org.sagebionetworks.repo.model.docker.DockerCommitSortBy;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.repo.web.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;

public class DockerCommitControllerAutowiredTest extends AbstractAutowiredControllerJunit5TestBase {

	private Long adminUserId;
	
	private Project project = null;
	private DockerRepository unmanagedRepository = null;
	
	private static final String DIGEST = "digest";
	private static final String TAG = "tag";
	
	@Autowired
	private EntityService entityService;

	@Autowired
	private OIDCTokenHelper oidcTokenHelper;
	
	@Autowired
	private OpenIDConnectManager oidcManager;
	
	@BeforeEach
	public void before() throws Exception {
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		String accessToken = oidcTokenHelper.createTotalAccessToken(adminUserId);
		project = new Project();
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		when(request.getServletPath()).thenReturn("/repo/v1/dockerTag");
		UserInfo userInfo = oidcManager.getUserAuthorization(accessToken);
		project = entityService.createEntity(userInfo, project, null);
		unmanagedRepository = new DockerRepository();
		unmanagedRepository.setParentId(project.getId());
		unmanagedRepository.setRepositoryName("uname/reponame");
		unmanagedRepository = entityService.createEntity(userInfo, unmanagedRepository, null);
		assertFalse(unmanagedRepository.getIsManaged());
	}
	
	@AfterEach
	public void after() throws Exception {
		if (project!=null && project.getId()!=null) {
			entityService.deleteEntity(adminUserId, project.getId(), Project.class);
		}
	}

	@Test
	public void testAddAndListCommit() throws Exception {
		DockerCommit commit = new DockerCommit();
		commit.setDigest(DIGEST);
		commit.setTag(TAG);
		servletTestHelper.createDockerCommit(dispatchServlet, adminUserId, unmanagedRepository.getId(), commit);

		// list the commits (should get back the added one)
		PaginatedResults<DockerCommit> result = servletTestHelper.listDockerTaggedCommits(
				adminUserId, unmanagedRepository.getId(), DockerCommitSortBy.CREATED_ON, /*ascending*/true, 10L, 0L);
		assertEquals(1, result.getTotalNumberOfResults());
		assertEquals(1, result.getResults().size());
		DockerCommit retrieved = result.getResults().get(0);
		assertNotNull(retrieved.getCreatedOn());
		assertEquals(DIGEST, retrieved.getDigest());
		assertEquals(TAG, retrieved.getTag());
		
		// make sure optional param's are optional
		PaginatedResults<DockerCommit> result2 = servletTestHelper.listDockerTaggedCommits(
				adminUserId, unmanagedRepository.getId(), null, null, null, null);
		assertEquals(result, result2);
	}

}
