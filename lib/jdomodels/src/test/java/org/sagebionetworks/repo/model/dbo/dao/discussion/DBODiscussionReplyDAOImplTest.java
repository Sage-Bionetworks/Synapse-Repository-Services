package org.sagebionetworks.repo.model.dbo.dao.discussion;

import static org.junit.Assert.*;

import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionReplyDAO;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.dao.discussion.ForumDAO;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBODiscussionReplyDAOImplTest {

	@Autowired
	private ForumDAO forumDao;
	@Autowired
	private UserGroupDAO userGroupDAO;
	@Autowired
	private NodeDAO nodeDao;
	@Autowired
	private DiscussionThreadDAO threadDao;
	@Autowired
	private DiscussionReplyDAO replyDao;
	@Autowired
	private IdGenerator idGenerator;

	private Long userId = null;
	private String projectId = null;
	private String forumId;
	private String threadId;
	private Long replyId;

	@Before
	public void before() {
		// create a user to create a project
		UserGroup user = new UserGroup();
		user.setIsIndividual(true);
		userId = userGroupDAO.create(user);
		// create a project
		Node project = NodeTestUtils.createNew("projectName" + "-" + new Random().nextInt(), userId);
		project.setParentId(StackConfiguration.getRootFolderEntityIdStatic());
		projectId = nodeDao.createNew(project);
		// create a forum
		Forum dto = forumDao.createForum(projectId);
		forumId = dto.getId();
		// create a thread
		threadId = idGenerator.generateNewId(TYPE.DISCUSSION_THREAD_ID).toString();
		threadDao.createThread(forumId, threadId, "title", "messageKey", userId);
	}

	@After
	public void after() {
		if (projectId != null) nodeDao.delete(projectId);
		if (userId != null) userGroupDAO.delete(userId.toString());
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateReplyWithNullThreadId() {
		replyDao.createReply(null, "messageKey", userId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateReplyWithNullMessageKey() {
		replyDao.createReply(threadId, null, userId);
	}

	@Test (expected = IllegalArgumentException.class)
	public void testCreateReplyWithNullUserId() {
		replyDao.createReply(threadId, "messageKey", null);
	}

	@Test
	public void testCreate() {
		String messageKey = "messageKey";
		DiscussionReplyBundle dto = replyDao.createReply(threadId, messageKey, userId);
		assertNotNull(dto);
		assertEquals(threadId, dto.getThreadId());
		assertEquals(messageKey, dto.getMessageKey());
		assertEquals(userId.toString(), dto.getCreatedBy());
		assertFalse(dto.getIsEdited());
		assertFalse(dto.getIsDeleted());
		assertNotNull(dto.getId());
		assertNotNull(dto.getEtag());
		Long replyId = Long.parseLong(dto.getId());
		assertEquals(dto, replyDao.getReply(replyId));
	}
}
