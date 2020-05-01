package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.docker.DockerCommit;
import org.sagebionetworks.repo.model.docker.DockerCommitSortBy;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.repo.web.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;

public class DockerCommitControllerAutowiredTest extends AbstractAutowiredControllerTestBase {

	private Long adminUserId;
	
	private Project project = null;
	private DockerRepository unmanagedRepository = null;
	
	private static final String DIGEST = "digest";
	private static final String TAG = "tag";
	
	@Autowired
	private EntityService entityService;

	@Before
	public void before() throws Exception {
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		project = new Project();
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		when(request.getServletPath()).thenReturn("/repo/v1/dockerTag");
		project = entityService.createEntity(adminUserId, project, null);
		unmanagedRepository = new DockerRepository();
		unmanagedRepository.setParentId(project.getId());
		unmanagedRepository.setRepositoryName("uname/reponame");
		unmanagedRepository = entityService.createEntity(adminUserId, unmanagedRepository, null);
		assertFalse(unmanagedRepository.getIsManaged());
	}
	
	@After
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
