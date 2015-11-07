package org.sagebionetworks.repo.model.dbo.dao.discussion;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOForumDAOImplTest {
	@Autowired
	DBOForumDAOImpl dao;
	private long forumId = 123L;
	private long projectId = 987L;

	@Before
	public void before() {
		dao.truncateAll();
	}

	@Test (expected = NotFoundException.class)
	public void testNotFoundWithForumId() {
		dao.getForum(forumId);
	}

	@Test (expected = NotFoundException.class)
	public void testNotFoundWithProjectId() {
		dao.getForumByProjectId(projectId);
	}
}
