package org.sagebionetworks.repo.manager.discussion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.discussion.DiscussionThreadManagerImpl.MAX_LIMIT;
import static org.sagebionetworks.repo.manager.discussion.DiscussionThreadManagerImpl.MAX_TITLE_LENGTH;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedIds;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UploadContentToS3DAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionReplyDAO;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.dao.discussion.ForumDAO;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadEntityReference;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadOrder;
import org.sagebionetworks.repo.model.discussion.EntityThreadCounts;
import org.sagebionetworks.repo.model.discussion.Forum;
import org.sagebionetworks.repo.model.discussion.MessageURL;
import org.sagebionetworks.repo.model.discussion.ThreadCount;
import org.sagebionetworks.repo.model.discussion.UpdateThreadMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadTitle;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.MessageToSend;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
public class DiscussionThreadManagerImplTest {
	@Mock
	private DiscussionThreadDAO mockThreadDao;
	@Mock
	private DiscussionReplyDAO mockReplyDao;
	@Mock
	private ForumDAO mockForumDao;
	@Mock
	private UploadContentToS3DAO mockUploadDao;
	@Mock
	private SubscriptionDAO mockSubscriptionDao;
	@Mock
	private AuthorizationManager mockAuthorizationManager;
	@Mock
	private IdGenerator mockIdGenerator;
	@Mock
	private TransactionalMessenger mockTransactionalMessenger;
	@Mock
	private AccessControlListDAO mockAclDao;
	@Mock
	private EntityIdList mockEntityIdList;
	@Mock
	private List<String> mockList;
	@Mock
	private GroupMembersDAO mockGroupMembersDao;

	@InjectMocks
	private DiscussionThreadManagerImpl threadManager;
	
	private UserInfo userInfo = new UserInfo(false /*not admin*/);
	private CreateDiscussionThread createDto;
	private DiscussionThreadBundle dto;
	private Long forumId = 1L;
	private String projectId = "syn123";
	private Long userId = 2L;
	private Long threadId = 3L;
	private Forum forum;
	private String messageKey = "1/3/messageKey";
	private MessageURL messageUrl = new MessageURL();
	private UpdateThreadTitle newTitle = new UpdateThreadTitle();
	private UpdateThreadMessage newMessage = new UpdateThreadMessage();
	private List<DiscussionThreadEntityReference> entityRefs = new ArrayList<DiscussionThreadEntityReference>();
	private List<DiscussionThreadEntityReference> titleEntityRefs = new ArrayList<DiscussionThreadEntityReference>();

	@BeforeEach
	public void before() {
		createDto = new CreateDiscussionThread();
		createDto.setForumId(forumId.toString());
		createDto.setTitle("title with syn123");
		createDto.setMessageMarkdown("messageMarkdown with syn456");
		forum = new Forum();
		forum.setId(forumId.toString());
		forum.setProjectId(projectId);
		dto = new DiscussionThreadBundle();
		dto.setProjectId(projectId);
		dto.setMessageKey(messageKey);
		dto.setId(threadId.toString());
		dto.setEtag("etag");
		dto.setForumId(forumId.toString());
		dto.setIsDeleted(false);
		userInfo.setId(userId);

		newTitle.setTitle("newTitle with syn123");
		newMessage.setMessageMarkdown("newMessageMarkdown");


		DiscussionThreadEntityReference titleEntityRef = new DiscussionThreadEntityReference();
		titleEntityRef.setEntityId("123");
		titleEntityRef.setThreadId(""+threadId);
		titleEntityRefs.add(titleEntityRef);
		entityRefs.add(titleEntityRef);

		DiscussionThreadEntityReference markdownEntityRef = new DiscussionThreadEntityReference();
		markdownEntityRef.setEntityId("456");
		markdownEntityRef.setThreadId(""+threadId);
		entityRefs.add(markdownEntityRef);
		
		messageUrl.setMessageUrl("messageUrl");
	}

	@Test
	public void testCreateWithNullDTO() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {			
			threadManager.createThread(userInfo, null);
		});
	}

	@Test
	public void testCreateWithNullForumId() throws Exception {
		createDto.setForumId(null);
		assertThrows(IllegalArgumentException.class, () ->{		
			threadManager.createThread(userInfo, createDto);
		});
	}

	@Test
	public void testCreateWithNullTitle() throws Exception {
		createDto.setTitle(null);
		assertThrows(IllegalArgumentException.class, () ->{		
			threadManager.createThread(userInfo, createDto);
		});
	}

	@Test
	public void testCreateWithNullMessage() throws Exception {
		createDto.setMessageMarkdown(null);
		assertThrows(IllegalArgumentException.class, () ->{		
			threadManager.createThread(userInfo, createDto);
		});
	}

	@Test
	public void testCreateWithNulluserInfo() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			threadManager.createThread(null, new CreateDiscussionThread());
		});
	}

	@Test
	public void testCreateWithLongTitle() throws Exception {
		CreateDiscussionThread createThread = new CreateDiscussionThread();
		createThread.setTitle(RandomStringUtils.randomAlphanumeric(MAX_TITLE_LENGTH+1));
		assertThrows(IllegalArgumentException.class, () ->{		
			threadManager.createThread(null, createThread);
		});
	}

	@Test
	public void testCreateAccessDenied() throws Exception {
		when(mockForumDao.getForum(Long.parseLong(createDto.getForumId()))).thenReturn(forum);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.accessDenied(""));
		assertThrows(UnauthorizedException.class, () ->{		
			threadManager.createThread(userInfo, createDto);
		});
	}

	@Test
	public void testCreateByAnonymous() throws Exception {
		when(mockForumDao.getForum(Long.parseLong(createDto.getForumId()))).thenReturn(forum);
		when(mockAuthorizationManager.isAnonymousUser(userInfo)).thenReturn(true);
		assertThrows(UnauthorizedException.class, () ->{		
			threadManager.createThread(userInfo, createDto);
		});
	}

	@Test
	public void testCreateAuthorized() throws Exception {
		when(mockForumDao.getForum(Long.parseLong(createDto.getForumId()))).thenReturn(forum);
		when(mockIdGenerator.generateNewId(IdType.DISCUSSION_THREAD_ID)).thenReturn(threadId);
		when(mockAuthorizationManager.isAnonymousUser(userInfo)).thenReturn(false);
		
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());
		when(mockUploadDao.uploadThreadMessage(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
				.thenReturn(messageKey);
		when(mockThreadDao.createThread(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyLong()))
				.thenReturn(dto);
		DiscussionThreadBundle createdThread = threadManager.createThread(userInfo, createDto);
		assertNotNull(createdThread);
		assertEquals(createdThread, dto);
		
		MessageToSend expectedMessage = new MessageToSend()
				.withUserId(userInfo.getId())
				.withObjectType(ObjectType.THREAD)
				.withObjectId(createdThread.getId())
				.withChangeType(ChangeType.CREATE);
		
		verify(mockTransactionalMessenger).sendMessageAfterCommit(expectedMessage);
		
		verify(mockSubscriptionDao).create(eq(userId.toString()), eq(dto.getId()), eq(SubscriptionObjectType.THREAD));
		ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
		verify(mockThreadDao).insertEntityReference(captor.capture());
		List<DiscussionThreadEntityReference> list = captor.getValue();
		assertTrue(list.contains(entityRefs.get(0)));
		assertTrue(list.contains(entityRefs.get(1)));
	}

	@Test
	public void testGetThreadWithNullThreadId() {
		assertThrows(IllegalArgumentException.class, () -> {		
			threadManager.getThread(userInfo, null);
		});
	}

	@Test
	public void testGetThreadUnauthorized() {
		when(mockThreadDao.getThread(threadId, DiscussionFilter.NO_FILTER)).thenReturn(dto);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.accessDenied(""));
		
		assertThrows(UnauthorizedException.class, () -> {
			threadManager.getThread(userInfo, threadId.toString());
		});
	}

	@Test
	public void testGetThreadAuthorized() {
		when(mockThreadDao.getThread(threadId, DiscussionFilter.NO_FILTER)).thenReturn(dto);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());
		assertEquals(dto, threadManager.getThread(userInfo, threadId.toString()));
		verify(mockThreadDao).updateThreadView(Mockito.anyLong(), Mockito.anyLong());
		
		MessageToSend expectedMessage = new MessageToSend()
				.withUserId(userInfo.getId())
				.withObjectType(ObjectType.THREAD)
				.withObjectId(threadId.toString())
				.withChangeType(ChangeType.UPDATE);
		
		verify(mockTransactionalMessenger).sendMessageAfterCommit(expectedMessage);
	}

	@Test
	public void testGetDeletedThreadAuthorized() {
		when(mockThreadDao.getThread(threadId, DiscussionFilter.NO_FILTER)).thenReturn(dto);
		dto.setIsDeleted(true);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationStatus.authorized());
		assertEquals(dto, threadManager.getThread(userInfo, threadId.toString()));
		verify(mockThreadDao).updateThreadView(Mockito.anyLong(), Mockito.anyLong());
	}

	@Test
	public void testGetDeletedThreadUnauthorized() {
		when(mockThreadDao.getThread(threadId, DiscussionFilter.NO_FILTER)).thenThrow(new NotFoundException());
		assertThrows(NotFoundException.class, () -> {
			threadManager.getThread(userInfo, threadId.toString());
		});
	}

	@Test
	public void testCheckPermissionWithNullUserInfo() {
		assertThrows(IllegalArgumentException.class, () -> {
			threadManager.checkPermission(null, threadId.toString(), ACCESS_TYPE.READ);
		});
		
	}

	@Test
	public void testCheckPermissionWithNullThreadId() {
		assertThrows(IllegalArgumentException.class, () -> {
			threadManager.checkPermission(userInfo, null, ACCESS_TYPE.READ);
		});
	}

	@Test
	public void testCheckPermissionUnauthorized() {
		when(mockThreadDao.getProjectId(threadId.toString())).thenReturn(projectId);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.accessDenied(""));
		assertThrows(UnauthorizedException.class, () -> {
			threadManager.checkPermission(userInfo, threadId.toString(), ACCESS_TYPE.READ);
		});
	}

	@Test
	public void testCheckPermissionAuthorized() {
		when(mockThreadDao.getProjectId(threadId.toString())).thenReturn(projectId);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());
		threadManager.checkPermission(userInfo, threadId.toString(), ACCESS_TYPE.READ);
		verify(mockThreadDao, Mockito.never()).updateThreadView(Mockito.anyLong(), Mockito.anyLong());
	}

	@Test
	public void testUpdateTitleWithNullTitle() {
		assertThrows(IllegalArgumentException.class, () -> {
			threadManager.updateTitle(userInfo, threadId.toString(), null);
		});
	}

	@Test
	public void testUpdateTitleUnauthorized() {
		when(mockAuthorizationManager.isUserCreatorOrAdmin(eq(userInfo), any()))
				.thenReturn(false);
		assertThrows(UnauthorizedException.class, () -> {
			threadManager.updateTitle(userInfo, threadId.toString(), newTitle);
		});
	}

	@Test
	public void testUpdateTitleDeletedThread() {
		when(mockThreadDao.getAuthorForUpdate(threadId.toString())).thenThrow(new NotFoundException());
		assertThrows(NotFoundException.class, () -> {
			threadManager.updateTitle(userInfo, threadId.toString(), newTitle);
		});
	}

	@Test
	public void testUpdateTitleAuthorized() {
		when(mockAuthorizationManager.isUserCreatorOrAdmin(Mockito.eq(userInfo), any()))
				.thenReturn(true);
		when(mockThreadDao.updateTitle(anyLong(), any())).thenReturn(dto);

		assertEquals(dto, threadManager.updateTitle(userInfo, threadId.toString(), newTitle));
		verify(mockThreadDao).insertEntityReference(titleEntityRefs);
	}

	@Test
	public void testUpdateMessageWithNullMessage() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			threadManager.updateMessage(userInfo, threadId.toString(), null);
		});
	}

	@Test
	public void testUpdateMessageUnauthorized() throws Exception {
		when(mockThreadDao.getThread(threadId, DiscussionFilter.EXCLUDE_DELETED)).thenReturn(dto);
		when(mockAuthorizationManager.isUserCreatorOrAdmin(eq(userInfo), any())).thenReturn(false);
		assertThrows(UnauthorizedException.class, () -> {
			threadManager.updateMessage(userInfo, threadId.toString(), newMessage );
		});
	}

	@Test
	public void testUpdateMessageForDeletedThread() throws Exception {
		when(mockThreadDao.getThread(threadId, DiscussionFilter.EXCLUDE_DELETED)).thenThrow(new NotFoundException());
		assertThrows(NotFoundException.class, () -> {
			threadManager.updateMessage(userInfo, threadId.toString(), newMessage );
		});
	}

	@Test
	public void testUpdateMessageAuthorized() throws Exception {
		when(mockThreadDao.getThread(threadId, DiscussionFilter.EXCLUDE_DELETED)).thenReturn(dto);
		when(mockAuthorizationManager.isUserCreatorOrAdmin(Mockito.eq(userInfo),any()))
				.thenReturn(true);
		when(mockUploadDao.uploadThreadMessage(any(), any(), any()))
				.thenReturn("newMessage");
		when(mockThreadDao.updateMessageKey(Mockito.anyLong(), Mockito.anyString())).thenReturn(dto);
		assertEquals(dto, threadManager.updateMessage(userInfo, threadId.toString(), newMessage));
		verify(mockThreadDao).insertEntityReference(any(List.class));
	}

	@Test
	public void testDeleteUnauthorized() {
		when(mockThreadDao.getProjectId(threadId.toString())).thenReturn(projectId);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationStatus.accessDenied(""));
		assertThrows(UnauthorizedException.class, () -> {
			threadManager.markThreadAsDeleted(userInfo, threadId.toString());
		});
	}

	@Test
	public void testDeleteAuthorized() {
		when(mockThreadDao.getProjectId(threadId.toString())).thenReturn(projectId);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationStatus.authorized());
		threadManager.markThreadAsDeleted(userInfo, threadId.toString());
		verify(mockThreadDao).markThreadAsDeleted(threadId);
	}


	@Test
	public void testRestoreUnauthorized() {
		when(mockThreadDao.getProjectId(threadId.toString())).thenReturn(projectId);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationStatus.accessDenied(""));
		assertThrows(UnauthorizedException.class, () -> {
			threadManager.markThreadAsNotDeleted(userInfo, threadId.toString());
		});
	}

	@Test
	public void testRestoreAuthorized() {
		when(mockThreadDao.getProjectId(threadId.toString())).thenReturn(projectId);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationStatus.authorized());
		threadManager.markThreadAsNotDeleted(userInfo, threadId.toString());
		verify(mockThreadDao).markThreadAsNotDeleted(threadId);
	}

	@Test
	public void testPinThreadUnauthorized() {
		when(mockThreadDao.getProjectId(threadId.toString())).thenReturn(projectId);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationStatus.accessDenied(""));
		assertThrows(UnauthorizedException.class, () -> {
			threadManager.pinThread(userInfo, threadId.toString());
		});
	}

	@Test
	public void testPinNotExistingThread() {
		when(mockThreadDao.isThreadDeleted(threadId.toString())).thenThrow(new NotFoundException());
		assertThrows(NotFoundException.class, () -> {
			threadManager.pinThread(userInfo, threadId.toString());
		});
	}

	@Test
	public void testPinDeletedThread() {
		when(mockThreadDao.isThreadDeleted(threadId.toString())).thenReturn(true);
		assertThrows(NotFoundException.class, () -> {
			threadManager.pinThread(userInfo, threadId.toString());
		});
	}

	@Test
	public void testPinThreadAuthorized() {
		when(mockThreadDao.getProjectId(threadId.toString())).thenReturn(projectId);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationStatus.authorized());
		threadManager.pinThread(userInfo, threadId.toString());
		verify(mockThreadDao).pinThread(threadId);
	}

	@Test
	public void testUnpinThreadUnauthorized() {
		when(mockThreadDao.getProjectId(threadId.toString())).thenReturn(projectId);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationStatus.accessDenied(""));
		assertThrows(UnauthorizedException.class, () -> {
			threadManager.unpinThread(userInfo, threadId.toString());
		});
	}

	@Test
	public void testUnpinNonExistingThread() {
		when(mockThreadDao.isThreadDeleted(threadId.toString())).thenThrow(new NotFoundException());
		assertThrows(NotFoundException.class, () -> {
			threadManager.unpinThread(userInfo, threadId.toString());
		});
	}

	@Test
	public void testUnpinDeletedThread() {
		when(mockThreadDao.isThreadDeleted(threadId.toString())).thenReturn(true);
		assertThrows(NotFoundException.class, () -> {
			threadManager.unpinThread(userInfo, threadId.toString());
		});
	}

	@Test
	public void testUnpinThreadAuthorized() {
		when(mockThreadDao.getProjectId(threadId.toString())).thenReturn(projectId);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationStatus.authorized());
		threadManager.unpinThread(userInfo, threadId.toString());
		verify(mockThreadDao).unpinThread(threadId);
	}

	@Test
	public void testGetThreadsForForumWithNullForumId() {
		assertThrows(IllegalArgumentException.class, () -> {
			threadManager.getThreadsForForum(userInfo, null, 2L, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, null, DiscussionFilter.NO_FILTER);
		});
	}

	@Test
	public void testGetThreadsForForumExceedLimit() {
		assertThrows(IllegalArgumentException.class, () -> {
			threadManager.getThreadsForForum(userInfo, forumId.toString(), MAX_LIMIT+1, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.EXCLUDE_DELETED);
		});
	}

	@Test
	public void testNegativeOffset() {
		assertThrows(IllegalArgumentException.class, () -> {
			threadManager.getThreadsForForum(userInfo, forumId.toString(), MAX_LIMIT, -2L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.EXCLUDE_DELETED);
		});
	}

	@Test
	public void testNegativeLimit() {
		assertThrows(IllegalArgumentException.class, () -> {
			threadManager.getThreadsForForum(userInfo, forumId.toString(), -1L, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.EXCLUDE_DELETED);
		});
	}

	@Test
	public void testGetThreadsForForumUnauthorizedWithNoFilter() {
		when(mockForumDao.getForum(Long.parseLong(createDto.getForumId()))).thenReturn(forum);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationStatus.accessDenied(""));
		assertThrows(UnauthorizedException.class, () -> {
			threadManager.getThreadsForForum(userInfo, forumId.toString(), 2L, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.NO_FILTER);
		});
	}

	@Test
	public void testGetThreadsForForumUnauthorized() {
		when(mockForumDao.getForum(Long.parseLong(createDto.getForumId()))).thenReturn(forum);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.accessDenied(""));
		assertThrows(UnauthorizedException.class, () -> {
			threadManager.getThreadsForForum(userInfo, forumId.toString(), 2L, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.EXCLUDE_DELETED);
		});
	}

	@Test
	public void testGetThreadsForForumWithNoThreads() {
		List<DiscussionThreadBundle> list = new ArrayList<DiscussionThreadBundle>();
		PaginatedResults<DiscussionThreadBundle> threads = PaginatedResults.createWithLimitAndOffset(list, 100L, 0L);
		when(mockForumDao.getForum(Long.parseLong(createDto.getForumId()))).thenReturn(forum);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationStatus.authorized());
		assertEquals(threads, threadManager.getThreadsForForum(userInfo, forumId.toString(), 2L, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.NO_FILTER));
		verify(mockThreadDao).getThreadsForForum(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), (DiscussionThreadOrder) Mockito.any(), Mockito.anyBoolean(), Mockito.any(DiscussionFilter.class));
	}

	@Test
	public void testGetThreadsForForum() {
		when(mockForumDao.getForum(Long.parseLong(createDto.getForumId()))).thenReturn(forum);
		List<DiscussionThreadBundle> list = Arrays.asList(dto);
		PaginatedResults<DiscussionThreadBundle> threads = PaginatedResults.createWithLimitAndOffset(list, 100L, 0L);
		when(mockThreadDao.getThreadsForForum(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), (DiscussionThreadOrder) Mockito.any(), Mockito.anyBoolean(), Mockito.any(DiscussionFilter.class)))
				.thenReturn(list);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE))
				.thenReturn(AuthorizationStatus.authorized());
		assertEquals(threads, threadManager.getThreadsForForum(userInfo, forumId.toString(), 2L, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.NO_FILTER));
		verify(mockThreadDao).getThreadsForForum(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyLong(), (DiscussionThreadOrder) Mockito.any(), Mockito.anyBoolean(), Mockito.any(DiscussionFilter.class));
	}

	@Test
	public void testGetThreadURLUnauthorized() {
		when(mockThreadDao.getProjectId(threadId.toString())).thenReturn(projectId);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.accessDenied(""));
		assertThrows(UnauthorizedException.class, () -> {
			threadManager.getMessageUrl(userInfo, messageKey);
		});
	}

	@Test
	public void testGetThreadURLAuthorized() {
		when(mockUploadDao.getThreadUrl(messageKey)).thenReturn(messageUrl);
		when(mockThreadDao.getProjectId(threadId.toString())).thenReturn(projectId);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());
		MessageURL url = threadManager.getMessageUrl(userInfo, messageKey);
		assertNotNull(url);
		assertNotNull(url.getMessageUrl());
		verify(mockThreadDao).updateThreadView(threadId, userId);
	}

	@Test
	public void testGetThreadCountForForumWithNullForumId() {
		assertThrows(IllegalArgumentException.class, () -> {
			threadManager.getThreadCountForForum(userInfo, null, DiscussionFilter.NO_FILTER);
		});
	}

	@Test
	public void testGetThreadCountForForumUnauthorizedWithNoFilter() {
		when(mockForumDao.getForum(Long.parseLong(createDto.getForumId()))).thenReturn(forum);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.accessDenied(""));
		assertThrows(UnauthorizedException.class, () -> {
			threadManager.getThreadCountForForum(userInfo, forumId.toString(), DiscussionFilter.NO_FILTER);
		});
	}

	@Test
	public void testGetThreadCountForForumUnauthorized() {
		when(mockForumDao.getForum(Long.parseLong(createDto.getForumId()))).thenReturn(forum);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.accessDenied(""));
		assertThrows(UnauthorizedException.class, () -> {
			threadManager.getThreadCountForForum(userInfo, forumId.toString(), DiscussionFilter.EXCLUDE_DELETED);
		});
	}

	@Test
	public void testGetThreadCountForForum() {
		when(mockForumDao.getForum(Long.parseLong(createDto.getForumId()))).thenReturn(forum);
		Long count = 3L;
		when(mockThreadDao.getThreadCountForForum(Mockito.anyLong(), Mockito.any(DiscussionFilter.class)))
				.thenReturn(count);
		when(mockAuthorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ))
				.thenReturn(AuthorizationStatus.authorized());
		ThreadCount tc = threadManager.getThreadCountForForum(userInfo, forumId.toString(), DiscussionFilter.NO_FILTER);
		assertNotNull(tc);
		assertEquals(count, tc.getCount());
	}

	@Test
	public void testGetEntityThreadCountNullEntityIdList() {
		assertThrows(IllegalArgumentException.class, () -> {
			threadManager.getEntityThreadCounts(userInfo, null);
		});
	}

	@Test
	public void testGetEntityThreadCountNullIdList() {
		assertThrows(IllegalArgumentException.class, () -> {
			threadManager.getEntityThreadCounts(userInfo, new EntityIdList());
		});
	}

	@Test
	public void testGetEntityThreadCountExceedLimit() {
		when(mockEntityIdList.getIdList()).thenReturn(mockList);
		when(mockList.size()).thenReturn((int) (MAX_LIMIT+1));
		assertThrows(IllegalArgumentException.class, () -> {
			threadManager.getEntityThreadCounts(userInfo, mockEntityIdList);
		});
	}

	@Test
	public void testGetEntityThreadCountEmptyIdList() {
		HashSet<Long> projectIds = new HashSet<Long>();
		EntityIdList entityIdList = new EntityIdList();
		entityIdList.setIdList(new ArrayList<String>());
		ArrayList<Long> entityIds = new ArrayList<Long>();
		EntityThreadCounts entityThreadCounts = new EntityThreadCounts();
		
		when(mockThreadDao.getDistinctProjectIdsOfThreadsReferencesEntityIds(anyList())).thenReturn(projectIds);
		when(mockAclDao.getAccessibleBenefactors(any(), eq(projectIds), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.READ))).thenReturn(projectIds);
		when(mockThreadDao.getThreadCounts(entityIds, projectIds)).thenReturn(entityThreadCounts);
		
		assertEquals(entityThreadCounts, threadManager.getEntityThreadCounts(userInfo, entityIdList));
		
		verify(mockThreadDao).getDistinctProjectIdsOfThreadsReferencesEntityIds(eq(entityIds));
		verify(mockAclDao).getAccessibleBenefactors(any(), eq(projectIds), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.READ));
		verify(mockThreadDao).getThreadCounts(eq(entityIds), eq(projectIds));
	}

	@Test
	public void testGetEntityThreadCount() {
		HashSet<Long> projectIds = new HashSet<Long>();
		projectIds.add(1L);
		projectIds.add(2L);
		HashSet<Long> projectIdsCanRead = new HashSet<Long>();
		projectIdsCanRead.add(1L);

		ArrayList<String> idList = new ArrayList<String>();
		idList.add("syn3");
		EntityIdList entityIdList = new EntityIdList();
		entityIdList.setIdList(idList);
		ArrayList<Long> entityIds = new ArrayList<Long>();
		entityIds.add(3L);

		EntityThreadCounts entityThreadCounts = new EntityThreadCounts();

		when(mockThreadDao.getDistinctProjectIdsOfThreadsReferencesEntityIds(entityIds)).thenReturn(projectIds);
		when(mockAclDao.getAccessibleBenefactors(any(), eq(projectIds), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.READ)))
				.thenReturn(projectIdsCanRead);
		when(mockThreadDao.getThreadCounts(entityIds, projectIdsCanRead)).thenReturn(entityThreadCounts);
		assertEquals(entityThreadCounts, threadManager.getEntityThreadCounts(userInfo, entityIdList));
		verify(mockThreadDao).getDistinctProjectIdsOfThreadsReferencesEntityIds(eq(entityIds));
		verify(mockAclDao).getAccessibleBenefactors(any(), eq(projectIds), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.READ));
		verify(mockThreadDao).getThreadCounts(eq(entityIds), eq(projectIdsCanRead));
	}

	@Test
	public void testGetThreadsForEntityWithNullEntityId(){
		assertThrows(IllegalArgumentException.class, () -> {
			threadManager.getThreadsForEntity(userInfo, null, 2L, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true);
		});
	}

	@Test
	public void testGetThreadsForEntityExceedLimit(){
		assertThrows(IllegalArgumentException.class, () -> {
			threadManager.getThreadsForEntity(userInfo, "syn3", MAX_LIMIT+1, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true);
		});
	}

	@Test
	public void testGetThreadsForEntityNegativeOffset() {
		assertThrows(IllegalArgumentException.class, () -> {
			threadManager.getThreadsForEntity(userInfo, "syn3", MAX_LIMIT, -3L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true);
		});
	}

	@Test
	public void testGetThreadsForEntityNegativeLimit() {
		assertThrows(IllegalArgumentException.class, () -> {
			threadManager.getThreadsForEntity(userInfo, "syn3", -2L, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true);
		});
	}

	@Test
	public void testGetThreadsForEntityWithoutReferences(){
		HashSet<Long> projectIds = new HashSet<Long>();
		projectIds.add(1L);
		projectIds.add(2L);
		HashSet<Long> projectIdsCanRead = new HashSet<Long>();
		projectIdsCanRead.add(1L);

		List<Long> entityIds = Arrays.asList(3L);

		List<DiscussionThreadBundle> list = new ArrayList<DiscussionThreadBundle>();
		PaginatedResults<DiscussionThreadBundle> result = PaginatedResults.createWithLimitAndOffset(list, 100L, 0L);
		
		when(mockThreadDao.getDistinctProjectIdsOfThreadsReferencesEntityIds(entityIds)).thenReturn(projectIds);
		when(mockAclDao.getAccessibleBenefactors(any(), eq(projectIds), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.READ)))
				.thenReturn(projectIdsCanRead);

		assertEquals(result, threadManager.getThreadsForEntity(userInfo, "syn3", 2L, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true));
		verify(mockThreadDao).getDistinctProjectIdsOfThreadsReferencesEntityIds(eq(entityIds));
		verify(mockAclDao).getAccessibleBenefactors(any(), eq(projectIds), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.READ));
		verify(mockThreadDao).getThreadsForEntity(anyLong(), any(), any(), (DiscussionThreadOrder) anyObject(), anyBoolean(), (DiscussionFilter) anyObject(), (Set) anyObject());
	}

	@Test
	public void testGetThreadsForEntity(){
		HashSet<Long> projectIds = new HashSet<Long>();
		projectIds.add(1L);
		projectIds.add(2L);
		HashSet<Long> projectIdsCanRead = new HashSet<Long>();
		projectIdsCanRead.add(1L);

		List<Long> entityIds = Arrays.asList(3L);

		List<DiscussionThreadBundle> list = new ArrayList<DiscussionThreadBundle>();
		PaginatedResults<DiscussionThreadBundle> result = PaginatedResults.createWithLimitAndOffset(list, 100L, 0L);

		when(mockThreadDao.getDistinctProjectIdsOfThreadsReferencesEntityIds(entityIds)).thenReturn(projectIds);
		when(mockAclDao.getAccessibleBenefactors(any(), eq(projectIds), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.READ)))
				.thenReturn(projectIdsCanRead);
		when(mockThreadDao.getThreadsForEntity(3L, 2L, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.EXCLUDE_DELETED, projectIdsCanRead)).thenReturn(list);

		assertEquals(result, threadManager.getThreadsForEntity(userInfo, "syn3", 2L, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true));
		verify(mockThreadDao).getDistinctProjectIdsOfThreadsReferencesEntityIds(eq(entityIds));
		verify(mockAclDao).getAccessibleBenefactors(any(), eq(projectIds), eq(ObjectType.ENTITY), eq(ACCESS_TYPE.READ));
		verify(mockThreadDao).getThreadsForEntity(3L, 2L, 0L, DiscussionThreadOrder.PINNED_AND_LAST_ACTIVITY, true, DiscussionFilter.EXCLUDE_DELETED, projectIdsCanRead);
	}

	@Test
	public void testGetModeratorsWithNullUserInfo(){
		assertThrows(IllegalArgumentException.class, () -> {
			threadManager.getModerators(null, forum.getId(), 10L, 0L);
		});
	}

	@Test
	public void testGetModeratorsWithNullForumId(){
		assertThrows(IllegalArgumentException.class, () -> {
			threadManager.getModerators(userInfo, null, 10L, 0L);
		});
	}

	@Test
	public void testGetModeratorsWithNegativeLimit(){
		assertThrows(IllegalArgumentException.class, () -> {
			threadManager.getModerators(userInfo, forum.getId(), -1L, 0L);
		});
	}

	@Test
	public void testGetModeratorsWithOverLimit(){
		assertThrows(IllegalArgumentException.class, () -> {
			threadManager.getModerators(userInfo, forum.getId(), MAX_LIMIT+1, 0L);
		});
	}

	@Test
	public void testGetModeratorsWithNegativeOffset(){
		assertThrows(IllegalArgumentException.class, () -> {
			threadManager.getModerators(userInfo, forum.getId(), 10L, -1L);
		});
	}

	@Test
	public void testGetModeratorsWithNotFoundForum() {
		when(mockForumDao.getForum(Long.parseLong(forum.getId()))).thenThrow(new NotFoundException());
		assertThrows(NotFoundException.class, () -> {
			threadManager.getModerators(userInfo, forum.getId(), 10L, 0L);
		});
	}

	@Test
	public void testGetModeratorsWithNotFoundModerators() {
		when(mockForumDao.getForum(Long.parseLong(createDto.getForumId()))).thenReturn(forum);
		when(mockAclDao.getPrincipalIds(projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE)).thenReturn(new HashSet<String>());
		PaginatedIds actual = threadManager.getModerators(userInfo, forum.getId(), 10L, 0L);
		assertNotNull(actual);
		assertEquals((Long)0L, actual.getTotalNumberOfResults());
		assertTrue(actual.getResults().isEmpty());
	}

	@Test
	public void testGetModerators() {
		HashSet<String> userGroups = new HashSet<String>();
		userGroups.addAll(Arrays.asList("1", "2"));
		when(mockForumDao.getForum(Long.parseLong(createDto.getForumId()))).thenReturn(forum);
		when(mockAclDao.getPrincipalIds(projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE)).thenReturn(userGroups);
		Set<String> individuals = new HashSet<String>();
		individuals.addAll(Arrays.asList("2", "3", "4"));
		when(mockGroupMembersDao.getIndividuals(userGroups, 10L, 0L)).thenReturn(individuals);
		when(mockGroupMembersDao.getIndividualCount(userGroups)).thenReturn(3L);
		PaginatedIds actual = threadManager.getModerators(userInfo, forum.getId(), 10L, 0L);
		assertNotNull(actual);
		assertEquals((Long)3L, actual.getTotalNumberOfResults());
		assertTrue(individuals.containsAll(actual.getResults()));
		assertTrue(actual.getResults().containsAll(individuals));
	}
}
