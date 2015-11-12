package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.discussion.Forum;

public class DiscussionControllerAutowiredTest extends AbstractAutowiredControllerTestBase{

	private Entity project;
	private Long adminUserId;

	@Before
	public void before() throws Exception {
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		project = new Project();
		project.setName(UUID.randomUUID().toString());
		project = servletTestHelper.createEntity(dispatchServlet, project, adminUserId);
	}

	@After
	public void cleanup() {
		try {
			servletTestHelper.deleteEntity(dispatchServlet, null, project.getId(), adminUserId,
					Collections.singletonMap("skipTrashCan", "false"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testGetForumMetadata() throws Exception {
		Forum dto = servletTestHelper.getForumMetadata(dispatchServlet, project.getId(), adminUserId);
		assertNotNull(dto);
		assertNotNull(dto.getId());
		assertEquals(dto.getProjectId(), project.getId());
	}

}
