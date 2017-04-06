package org.sagebionetworks.repo.manager.discussion;

import static org.sagebionetworks.repo.manager.AuthorizationManagerImpl.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedIds;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UploadContentToS3DAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.dao.discussion.ForumDAO;
import org.sagebionetworks.repo.model.dao.subscription.SubscriptionDAO;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadOrder;
import org.sagebionetworks.repo.model.discussion.EntityThreadCounts;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadEntityReference;
import org.sagebionetworks.repo.model.discussion.MessageURL;
import org.sagebionetworks.repo.model.discussion.ThreadCount;
import org.sagebionetworks.repo.model.discussion.UpdateThreadMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadTitle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.subscription.SubscriptionObjectType;
import org.sagebionetworks.repo.transactions.WriteTransactionReadCommitted;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.upload.discussion.MessageKeyUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class DiscussionThreadManagerImpl implements DiscussionThreadManager {

	private static final long DEFAULT_OFFSET = 0L;
	private static final DiscussionFilter DEFAULT_FILTER = DiscussionFilter.NO_FILTER;
	public static final int MAX_TITLE_LENGTH = 140;
	public static final long MAX_LIMIT = 20L;
	@Autowired
	private DiscussionThreadDAO threadDao;
	@Autowired
	private ForumDAO forumDao;
	@Autowired
	private UploadContentToS3DAO uploadDao;
	@Autowired
	private SubscriptionDAO subscriptionDao;
	@Autowired
	private AuthorizationManager authorizationManager;
	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private TransactionalMessenger transactionalMessenger;
	@Autowired
	private AccessControlListDAO aclDao;
	@Autowired
	private GroupMembersDAO groupMembersDao;

	@WriteTransactionReadCommitted
	@Override
	public DiscussionThreadBundle createThread(UserInfo userInfo, CreateDiscussionThread createThread) throws IOException {
		ValidateArgument.required(createThread, "createThread");
		ValidateArgument.required(createThread.getForumId(), "CreateDiscussionThread.forumId");
		ValidateArgument.required(createThread.getTitle(), "CreateDiscussionThread.title");
		ValidateArgument.required(createThread.getMessageMarkdown(), "CreateDiscussionThread.messageMarkdown");
		ValidateArgument.requirement(createThread.getTitle().length() <= MAX_TITLE_LENGTH, "Title cannot exceed "+MAX_TITLE_LENGTH+" characters.");
		UserInfo.validateUserInfo(userInfo);
		String projectId = forumDao.getForum(Long.parseLong(createThread.getForumId())).getProjectId();
		if (authorizationManager.isAnonymousUser(userInfo)){
			throw new UnauthorizedException(ANONYMOUS_ACCESS_DENIED_REASON);
		}
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ));
		Long id = idGenerator.generateNewId(IdType.DISCUSSION_THREAD_ID);
		String messageKey = uploadDao.uploadThreadMessage(createThread.getMessageMarkdown(), createThread.getForumId(), id.toString());
		DiscussionThreadBundle thread = threadDao.createThread(createThread.getForumId(), id.toString(), createThread.getTitle(), messageKey, userInfo.getId());
		transactionalMessenger.sendMessageAfterCommit(""+id, ObjectType.THREAD, thread.getEtag(), ChangeType.CREATE, userInfo.getId());
		subscriptionDao.create(userInfo.getId().toString(), id.toString(), SubscriptionObjectType.THREAD);
		List<DiscussionThreadEntityReference> entityRefs = DiscussionUtils.getEntityReferences(createThread.getMessageMarkdown(), thread.getId());
		entityRefs.addAll(DiscussionUtils.getEntityReferences(createThread.getTitle(), thread.getId()));
		threadDao.insertEntityReference(entityRefs);
		return thread;
	}

	@WriteTransactionReadCommitted
	@Override
	public DiscussionThreadBundle getThread(UserInfo userInfo, String threadId) {
		ValidateArgument.required(threadId, "threadId");
		UserInfo.validateUserInfo(userInfo);
		Long threadIdLong = Long.parseLong(threadId);
		DiscussionThreadBundle thread = threadDao.getThread(threadIdLong, DEFAULT_FILTER);
		if (thread.getIsDeleted()) {
			try {
				AuthorizationManagerUtil.checkAuthorizationAndThrowException(
						authorizationManager.canAccess(userInfo, thread.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.MODERATE));
			} catch (UnauthorizedException e) {
				throw new NotFoundException();
			}
		} else {
			AuthorizationManagerUtil.checkAuthorizationAndThrowException(
					authorizationManager.canAccess(userInfo, thread.getProjectId(), ObjectType.ENTITY, ACCESS_TYPE.READ));
		}
		threadDao.updateThreadView(threadIdLong, userInfo.getId());
		transactionalMessenger.sendMessageAfterCommit(threadId, ObjectType.THREAD, thread.getEtag(),  ChangeType.UPDATE, userInfo.getId());
		return thread;
	}

	@Override
	public void checkPermission(UserInfo userInfo, String threadId, ACCESS_TYPE accessType) {
		ValidateArgument.required(threadId, "threadId");
		ValidateArgument.required(accessType, "accessType");
		UserInfo.validateUserInfo(userInfo);
		String projectId = threadDao.getProjectId(threadId);
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, accessType));
	}

	@WriteTransactionReadCommitted
	@Override
	public DiscussionThreadBundle updateTitle(UserInfo userInfo, String threadId, UpdateThreadTitle newTitle) {
		ValidateArgument.required(threadId, "threadId");
		ValidateArgument.required(newTitle, "newTitle");
		ValidateArgument.required(newTitle.getTitle(), "UpdateThreadTitle.title");
		UserInfo.validateUserInfo(userInfo);
		Long threadIdLong = Long.parseLong(threadId);
		String author = threadDao.getAuthorForUpdate(threadId);
		if (authorizationManager.isUserCreatorOrAdmin(userInfo, author)) {
			DiscussionThreadBundle thread = threadDao.updateTitle(threadIdLong, newTitle.getTitle());
			threadDao.insertEntityReference(DiscussionUtils.getEntityReferences(newTitle.getTitle(), thread.getId()));
			return thread;
		} else {
			throw new UnauthorizedException("Only the user that created the thread can modify it.");
		}
	}

	@WriteTransactionReadCommitted
	@Override
	public DiscussionThreadBundle updateMessage(UserInfo userInfo, String threadId,
			UpdateThreadMessage newMessage) throws IOException {
		ValidateArgument.required(threadId, "threadId");
		ValidateArgument.required(newMessage, "newMessage");
		ValidateArgument.required(newMessage.getMessageMarkdown(), "UpdateThreadMessage.messageMarkdown");
		UserInfo.validateUserInfo(userInfo);
		Long threadIdLong = Long.parseLong(threadId);
		DiscussionThreadBundle thread = threadDao.getThread(threadIdLong, DiscussionFilter.EXCLUDE_DELETED);
		if (authorizationManager.isUserCreatorOrAdmin(userInfo, thread.getCreatedBy())) {
			String messageKey = uploadDao.uploadThreadMessage(newMessage.getMessageMarkdown(), thread.getForumId(), thread.getId());
			thread = threadDao.updateMessageKey(threadIdLong, messageKey);
			threadDao.insertEntityReference(DiscussionUtils.getEntityReferences(newMessage.getMessageMarkdown(), thread.getId()));
			return thread;
		} else {
			throw new UnauthorizedException("Only the user that created the thread can modify it.");
		}
	}

	@WriteTransactionReadCommitted
	@Override
	public void markThreadAsDeleted(UserInfo userInfo, String threadId) {
		checkPermission(userInfo, threadId, ACCESS_TYPE.MODERATE);
		threadDao.markThreadAsDeleted(Long.parseLong(threadId));
	}

	@WriteTransactionReadCommitted
	@Override
	public void pinThread(UserInfo userInfo, String threadId) {
		if (threadDao.isThreadDeleted(threadId)) {
			throw new NotFoundException();
		}
		checkPermission(userInfo, threadId, ACCESS_TYPE.MODERATE);
		threadDao.pinThread(Long.parseLong(threadId));
	}

	@WriteTransactionReadCommitted
	@Override
	public void unpinThread(UserInfo userInfo, String threadId) {
		if (threadDao.isThreadDeleted(threadId)) {
			throw new NotFoundException();
		}
		checkPermission(userInfo, threadId, ACCESS_TYPE.MODERATE);
		threadDao.unpinThread(Long.parseLong(threadId));
	}

	@Override
	public PaginatedResults<DiscussionThreadBundle> getThreadsForForum(
			UserInfo userInfo, String forumId, Long limit, Long offset,
			DiscussionThreadOrder order, Boolean ascending, DiscussionFilter filter) {
		ValidateArgument.required(forumId, "forumId");
		ValidateArgument.required(filter, "filter");
		UserInfo.validateUserInfo(userInfo);
		if (limit == null) {
			limit = MAX_LIMIT;
		}
		if (offset == null) {
			offset = DEFAULT_OFFSET;
		}
		ValidateArgument.requirement(limit >= 0 && offset >= 0 && limit <= MAX_LIMIT,
				"Limit and offset must be greater than 0, and limit must be smaller than or equal to "+MAX_LIMIT);
		String projectId = forumDao.getForum(Long.parseLong(forumId)).getProjectId();
		if (filter.equals(DiscussionFilter.EXCLUDE_DELETED)) {
			AuthorizationManagerUtil.checkAuthorizationAndThrowException(
					authorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ));
		} else {
			AuthorizationManagerUtil.checkAuthorizationAndThrowException(
					authorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE));
		}
		List<DiscussionThreadBundle> results = threadDao.getThreadsForForum(Long.parseLong(forumId), limit, offset, order, ascending, filter);
		return PaginatedResults.createWithLimitAndOffset(results, limit, offset);
	}

	@Override
	public MessageURL getMessageUrl(UserInfo userInfo, String messageKey) {
		ValidateArgument.required(messageKey, "messageKey");
		String threadId = MessageKeyUtils.getThreadId(messageKey);
		checkPermission(userInfo, threadId, ACCESS_TYPE.READ);
		threadDao.updateThreadView(Long.parseLong(threadId), userInfo.getId());
		return uploadDao.getThreadUrl(messageKey);
	}

	@Override
	public ThreadCount getThreadCountForForum(UserInfo userInfo, String forumId, DiscussionFilter filter) {
		ValidateArgument.required(forumId, "forumId");
		ValidateArgument.required(filter, "filter");
		UserInfo.validateUserInfo(userInfo);
		String projectId = forumDao.getForum(Long.parseLong(forumId)).getProjectId();
		AuthorizationManagerUtil.checkAuthorizationAndThrowException(
				authorizationManager.canAccess(userInfo, projectId, ObjectType.ENTITY, ACCESS_TYPE.READ));
		ThreadCount count = new ThreadCount();
		count.setCount(threadDao.getThreadCountForForum(Long.parseLong(forumId), filter));
		return count;
	}

	@Override
	public PaginatedResults<DiscussionThreadBundle> getThreadsForEntity(UserInfo userInfo, String entityId, Long limit,
			Long offset, DiscussionThreadOrder order, Boolean ascending) {
		ValidateArgument.required(entityId, "entityId");
		UserInfo.validateUserInfo(userInfo);
		if (limit == null) {
			limit = MAX_LIMIT;
		}
		if (offset == null) {
			offset = DEFAULT_OFFSET;
		}
		ValidateArgument.requirement(limit >= 0 && offset >= 0 && limit <= MAX_LIMIT,
				"Limit and offset must be greater than 0, and limit must be smaller than or equal to "+MAX_LIMIT);
		Long entityIdLong = KeyFactory.stringToKey(entityId);
		Set<Long> projectIds = threadDao.getDistinctProjectIdsOfThreadsReferencesEntityIds(Arrays.asList(entityIdLong));
		projectIds = aclDao.getAccessibleBenefactors(userInfo.getGroups(), projectIds, ObjectType.ENTITY, ACCESS_TYPE.READ);
		List<DiscussionThreadBundle> results = threadDao.getThreadsForEntity(entityIdLong, limit, offset, order, ascending, DiscussionFilter.EXCLUDE_DELETED, projectIds);
		return PaginatedResults.createWithLimitAndOffset(results, limit, offset);
	}

	@Override
	public EntityThreadCounts getEntityThreadCounts(UserInfo userInfo, EntityIdList entityIdList) {
		UserInfo.validateUserInfo(userInfo);
		ValidateArgument.required(entityIdList, "entityIdList");
		ValidateArgument.required(entityIdList.getIdList(), "EntityIdList.list");
		ValidateArgument.requirement(entityIdList.getIdList().size() <= MAX_LIMIT, "The size of entityIdList cannot exceed "+MAX_LIMIT);
		List<Long> entityIds = KeyFactory.stringToKey(entityIdList.getIdList());
		Set<Long> projectIds = threadDao.getDistinctProjectIdsOfThreadsReferencesEntityIds(entityIds);
		projectIds = aclDao.getAccessibleBenefactors(userInfo.getGroups(), projectIds, ObjectType.ENTITY, ACCESS_TYPE.READ);
		return threadDao.getThreadCounts(entityIds, projectIds);
	}

	@Override
	public void markThreadAsNotDeleted(UserInfo userInfo, String threadId) {
		checkPermission(userInfo, threadId, ACCESS_TYPE.MODERATE);
		threadDao.markThreadAsNotDeleted(Long.parseLong(threadId));
	}

	@Override
	public PaginatedIds getModerators(UserInfo userInfo, String forumId, Long limit, Long offset) {
		ValidateArgument.required(forumId, "forumId");
		UserInfo.validateUserInfo(userInfo);
		if (limit == null) {
			limit = MAX_LIMIT;
		}
		if (offset == null) {
			offset = DEFAULT_OFFSET;
		}
		ValidateArgument.requirement(limit >= 0 && offset >= 0 && limit <= MAX_LIMIT,
				"Limit and offset must be greater than 0, and limit must be smaller than or equal to "+MAX_LIMIT);

		PaginatedIds results = new PaginatedIds();
		List<String> userIds = new ArrayList<String>();
		results.setResults(userIds);
		String projectId = forumDao.getForum(Long.parseLong(forumId)).getProjectId();
		Set<String> principalIds = aclDao.getPrincipalIds(projectId, ObjectType.ENTITY, ACCESS_TYPE.MODERATE);
		if (principalIds.isEmpty()) {
			results.setTotalNumberOfResults(0L);
			return results;
		}
		userIds.addAll(groupMembersDao.getIndividuals(principalIds, limit, offset));
		results.setTotalNumberOfResults(groupMembersDao.getIndividualCount(principalIds));
		return results;
	}
}
