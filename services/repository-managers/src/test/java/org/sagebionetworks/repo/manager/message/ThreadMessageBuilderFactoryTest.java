package org.sagebionetworks.repo.manager.message;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.markdown.MarkdownDao;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UploadContentToS3DAO;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.springframework.test.util.ReflectionTestUtils;

public class ThreadMessageBuilderFactoryTest {

	@Mock
	private DiscussionThreadDAO mockThreadDao;
	@Mock
	private NodeDAO mockNodeDao;
	@Mock
	private PrincipalAliasDAO mockPrincipalAliasDAO;
	@Mock
	private UploadContentToS3DAO mockUploadDao;
	@Mock
	private MarkdownDao mockMarkdownDao;

	DiscussionThreadBundle threadBundle;
	EntityHeader projectHeader;
	String message;
	String key;
	Long actorUserId;
	String actorUsername;
	
	ThreadMessageBuilderFactory factory;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);

		factory = new ThreadMessageBuilderFactory();
		ReflectionTestUtils.setField(factory, "threadDao", mockThreadDao);
		ReflectionTestUtils.setField(factory, "nodeDao", mockNodeDao);
		ReflectionTestUtils.setField(factory, "principalAliasDAO", mockPrincipalAliasDAO);
		ReflectionTestUtils.setField(factory, "uploadDao", mockUploadDao);
		ReflectionTestUtils.setField(factory, "markdownDao", mockMarkdownDao);

		key = "key";
		message = "message";
		threadBundle = new DiscussionThreadBundle();
		threadBundle.setId("333");
		threadBundle.setProjectId("444");
		threadBundle.setTitle("title");
		threadBundle.setCreatedBy("987");
		threadBundle.setMessageKey(key);
		when(mockThreadDao.getThread(anyLong(), any(DiscussionFilter.class))).thenReturn(threadBundle);

		projectHeader = new EntityHeader();
		projectHeader.setName("project name");
		when(mockNodeDao.getEntityHeader(anyString(),  anyLong())).thenReturn(projectHeader);

		when(mockUploadDao.getMessage(key)).thenReturn(message);

		actorUserId = 456L;
		actorUsername = "someone";
		when(mockPrincipalAliasDAO.getUserName(actorUserId)).thenReturn(actorUsername);
	}
	
	@Test
	public void testBuild(){
		String objectId = "123";
		ChangeType type = ChangeType.CREATE;
		BroadcastMessageBuilder bulider = factory.createMessageBuilder(objectId, type, actorUserId);
		assertNotNull(bulider);
		verify(mockNodeDao).getEntityHeader("444", null);
		verify(mockUploadDao).getMessage(key);
	}
	
}
