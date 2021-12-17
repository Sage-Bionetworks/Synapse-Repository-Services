package org.sagebionetworks.repo.model.dbo.dao.discussion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.dbo.persistence.discussion.DBODiscussionSearchIndexRecord;
import org.sagebionetworks.repo.model.discussion.Match;
import org.sagebionetworks.repo.model.helper.NodeDaoObjectHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DiscussionSearchIndexDaoImplTest {

	@Autowired
	private NodeDaoObjectHelper nodeHelper;
		
	@Autowired
	private ForumDAO forumDao;
	
	@Autowired
	private DiscussionThreadDAO threadDao;
	
	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private DiscussionSearchIndexDaoImpl dao;
	
	private Long forumId;
	private Long threadId;

	@BeforeEach
	public void before() {
		nodeHelper.truncateAll();
		
		String projectId = nodeHelper.create(node -> {
			node.setNodeType(EntityType.project);
		}).getId();
		
		forumId = Long.valueOf(forumDao.createForum(projectId).getId());
		threadId = Long.valueOf(threadDao.createThread(forumId.toString(), idGenerator.generateNewId(IdType.DISCUSSION_THREAD_ID).toString(), "title", "some_key", BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId()).getId());
	}
	
	@AfterEach
	public void after() {
		nodeHelper.truncateAll();
	}
	
	@Test
	public void testCreateOrUpdateRecordForThread() {
				
		DBODiscussionSearchIndexRecord expected = createRecord(DBODiscussionSearchIndexRecord.NO_REPLY_ID, "search content");
		
		// Call under test
		dao.createOrUpdateRecordForThread(forumId, threadId, expected.getSearchContent());
		
		assertEquals(Arrays.asList(expected), dao.listRecords(forumId));
		
	}
	
	@Test
	public void testCreateOrUpdateRecordForThreadExisting() {
		
		dao.createOrUpdateRecordForThread(forumId, threadId, "search content");
		
		DBODiscussionSearchIndexRecord expected = createRecord(DBODiscussionSearchIndexRecord.NO_REPLY_ID, "search content updated");
		
		// Call under test
		dao.createOrUpdateRecordForThread(forumId, threadId, expected.getSearchContent());
		
		assertEquals(Arrays.asList(expected), dao.listRecords(forumId));
		
	}
	
	@Test
	public void testCreateOrUpdateRecordForThreadWithNoForum() {

		String searchContent = "search content";
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			dao.createOrUpdateRecordForThread(null, threadId, searchContent);
		}).getMessage();
		
		assertEquals("The forumId is required.", message);
		
	}
	
	@Test
	public void testCreateOrUpdateRecordForThreadWithNoThread() {

		String searchContent = "search content";
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			dao.createOrUpdateRecordForThread(forumId, null, searchContent);
		}).getMessage();
		
		assertEquals("The threadId is required.", message);
		
	}
	
	@Test
	public void testCreateOrUpdateRecordForThreadWithNoSearchContent() {

		String searchContent = null;
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			dao.createOrUpdateRecordForThread(forumId, threadId, searchContent);
		}).getMessage();
		
		assertEquals("The searchContent is required.", message);
		
	}
	
	@Test
	public void testCreateOrUpdateRecordForReply() {
		
		Long replyId = 123L;
				
		DBODiscussionSearchIndexRecord expected = createRecord(replyId, "search content");
		
		// Call under test
		dao.createOrUpdateRecordForReply(forumId, threadId, replyId, expected.getSearchContent());
		
		assertEquals(Arrays.asList(expected), dao.listRecords(forumId));
		
	}
	
	@Test
	public void testCreateOrUpdateRecordForReplyExisting() {
		
		Long replyId = 123L;
		
		dao.createOrUpdateRecordForReply(forumId, threadId, replyId, "search content");
		
		DBODiscussionSearchIndexRecord expected = createRecord(replyId, "search content updated");
		
		// Call under test
		dao.createOrUpdateRecordForReply(forumId, threadId, replyId, expected.getSearchContent());
		
		assertEquals(Arrays.asList(expected), dao.listRecords(forumId));
		
	}
	
	@Test
	public void testCreateOrUpdateRecordForReplyWithNoForum() {
		
		Long replyId = 123L;
				
		String searchContent = "search content";
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			dao.createOrUpdateRecordForReply(null, threadId, replyId, searchContent);
		}).getMessage();
		
		assertEquals("The forumId is required.", message);
	}
	
	@Test
	public void testCreateOrUpdateRecordForReplyWithNoThread() {
		
		Long replyId = 123L;
				
		String searchContent = "search content";
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			dao.createOrUpdateRecordForReply(forumId, null, replyId, searchContent);
		}).getMessage();
		
		assertEquals("The threadId is required.", message);
	}
	
	@Test
	public void testCreateOrUpdateRecordForReplyWithNoReply() {
		
		Long replyId = null;
				
		String searchContent = "search content";
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			dao.createOrUpdateRecordForReply(forumId, threadId, replyId, searchContent);
		}).getMessage();
		
		assertEquals("The replyId is required.", message);
	}
	
	@Test
	public void testCreateOrUpdateRecordForReplyWithUnexpectedReply() {
		
		Long replyId = DBODiscussionSearchIndexRecord.NO_REPLY_ID;
				
		String searchContent = "search content";
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			dao.createOrUpdateRecordForReply(forumId, threadId, replyId, searchContent);
		}).getMessage();
		
		assertEquals("Unexpected replyId: " + replyId, message);
	}	
	
	@Test
	public void testCreateOrUpdateRecordForReplyWithNoSearchContent() {
		
		Long replyId = 123L;
				
		String searchContent = null;
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			dao.createOrUpdateRecordForReply(forumId, threadId, replyId, searchContent);
		}).getMessage();
		
		assertEquals("The searchContent is required.", message);
	}
	
	@Test
	public void testMarkThreadAsDeleted() {
		String searchContent = "search content";
		Long replyId = 123L;
		
		dao.createOrUpdateRecordForThread(forumId, threadId, searchContent);
		dao.createOrUpdateRecordForReply(forumId, threadId, replyId, searchContent);
		
		List<DBODiscussionSearchIndexRecord> expected = Arrays.asList(
			createRecord(DBODiscussionSearchIndexRecord.NO_REPLY_ID, searchContent, true, false),
			createRecord(replyId, searchContent, true, false)
		);
		
		// Call under test
		dao.markThreadAsDeleted(threadId);
				
		assertEquals(expected, dao.listRecords(forumId));
	}
	
	@Test
	public void testMarkThreadAsNotDeleted() {
		String searchContent = "search content";
		Long replyId = 123L;
		
		dao.createOrUpdateRecordForThread(forumId, threadId, searchContent);
		dao.createOrUpdateRecordForReply(forumId, threadId, replyId, searchContent);
		
		dao.markThreadAsDeleted(threadId);
		
		List<DBODiscussionSearchIndexRecord> expected = Arrays.asList(
			createRecord(DBODiscussionSearchIndexRecord.NO_REPLY_ID, searchContent),
			createRecord(replyId, searchContent)
		);
		
		// Call under test
		dao.markThreadAsNotDeleted(threadId);
		
		assertEquals(expected, dao.listRecords(forumId));
	}
	
	@Test
	public void testMarkReplyAsDeleted() {
		String searchContent = "search content";
		Long replyId = 123L;
		
		dao.createOrUpdateRecordForThread(forumId, threadId, searchContent);
		dao.createOrUpdateRecordForReply(forumId, threadId, replyId, searchContent);
		
		List<DBODiscussionSearchIndexRecord> expected = Arrays.asList(
			createRecord(DBODiscussionSearchIndexRecord.NO_REPLY_ID, searchContent, false, false),
			createRecord(replyId, searchContent, false, true)
		);
		
		// Call under test
		dao.markReplyAsDeleted(replyId);
				
		assertEquals(expected, dao.listRecords(forumId));
	}
	
	@Test
	public void testMarkReplyAsNotDeleted() {
		String searchContent = "search content";
		Long replyId = 123L;
		
		dao.createOrUpdateRecordForThread(forumId, threadId, searchContent);
		dao.createOrUpdateRecordForReply(forumId, threadId, replyId, searchContent);
		
		dao.markReplyAsDeleted(replyId);
		
		List<DBODiscussionSearchIndexRecord> expected = Arrays.asList(
			createRecord(DBODiscussionSearchIndexRecord.NO_REPLY_ID, searchContent),
			createRecord(replyId, searchContent)
		);
		
		// Call under test
		dao.markReplyAsNotDeleted(replyId);
		
		assertEquals(expected, dao.listRecords(forumId));
	}
				
	@Test
	public void testSearch() {
		String searchString = "search content";
		
		Long replyId = 123L;
		
		dao.createOrUpdateRecordForThread(forumId, threadId, "search content content thread");
		dao.createOrUpdateRecordForReply(forumId, threadId, replyId + 1, "search content reply");
		dao.createOrUpdateRecordForReply(forumId, threadId, replyId, "content reply");
		dao.createOrUpdateRecordForReply(forumId, threadId, replyId + 2, "non-matching reply");
		
		List<Match> expected = Arrays.asList(
			new Match().setForumId(forumId.toString()).setThreadId(threadId.toString()).setReplyId(null),
			new Match().setForumId(forumId.toString()).setThreadId(threadId.toString()).setReplyId(String.valueOf(replyId + 1)),
			new Match().setForumId(forumId.toString()).setThreadId(threadId.toString()).setReplyId(String.valueOf(replyId))
		);
		
		// Call under test
		List<Match> result = dao.search(forumId, searchString, 10, 0);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testSearchWithDeletedThread() {
		String searchString = "search content";
		
		Long replyId = 123L;
		
		dao.createOrUpdateRecordForThread(forumId, threadId, "search content thread");
		dao.createOrUpdateRecordForReply(forumId, threadId, replyId, "content reply");
		dao.createOrUpdateRecordForReply(forumId, threadId, replyId + 1, "non-matching reply");
		
		dao.markThreadAsDeleted(threadId);
		
		List<Match> expected = Collections.emptyList();
		
		// Call under test
		List<Match> result = dao.search(forumId, searchString, 10, 0);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testSearchWithDeletedReply() {
		String searchString = "search content";
		
		Long replyId = 123L;
		
		dao.createOrUpdateRecordForThread(forumId, threadId, "search content thread");
		dao.createOrUpdateRecordForReply(forumId, threadId, replyId + 1, "deleted content reply");
		dao.createOrUpdateRecordForReply(forumId, threadId, replyId, "content reply");
		dao.createOrUpdateRecordForReply(forumId, threadId, replyId + 2, "non-matching reply");
		
		dao.markReplyAsDeleted(replyId + 1);
		
		List<Match> expected = Arrays.asList(
			new Match().setForumId(forumId.toString()).setThreadId(threadId.toString()).setReplyId(null),
			new Match().setForumId(forumId.toString()).setThreadId(threadId.toString()).setReplyId(String.valueOf(replyId))
		);
		
		// Call under test
		List<Match> result = dao.search(forumId, searchString, 10, 0);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testSearchMultiplePages() {
		
		long limit = 2;
		long offset = 0;
		
		String searchString = "search content";
		
		Long replyId = 456L;
		Long anotherReplyId = 123L;
		
		dao.createOrUpdateRecordForThread(forumId, threadId, "search content content thread");
		dao.createOrUpdateRecordForReply(forumId, threadId, replyId, "content content reply");
		dao.createOrUpdateRecordForReply(forumId, threadId, anotherReplyId, "content reply");
		
		List<Match> expectedFirstPage = Arrays.asList(
			new Match().setForumId(forumId.toString()).setThreadId(threadId.toString()).setReplyId(null),
			new Match().setForumId(forumId.toString()).setThreadId(threadId.toString()).setReplyId(replyId.toString())
		);
		
		// Call under test
		List<Match> result = dao.search(forumId, searchString, limit, offset);
		
		assertEquals(expectedFirstPage, result);
		
		List<Match> expectedSecondPage = Arrays.asList(
			new Match().setForumId(forumId.toString()).setThreadId(threadId.toString()).setReplyId(anotherReplyId.toString())
		);
		
		// Call under test
		result = dao.search(forumId, searchString, limit, offset + limit);
		
		assertEquals(expectedSecondPage, result);
	}
	
	@Test
	public void testSearchNoForum() {
		String searchString = "search string";
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			dao.search(null, searchString, 10, 0);
		}).getMessage();
		
		assertEquals("The forumId is required.", message);
		
	}
	
	@Test
	public void testSearchNoSearchString() {
		String searchString = null;
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			dao.search(forumId, searchString, 10, 0);
		}).getMessage();
		
		assertEquals("The searchString is required.", message);
		
	}
	
	private DBODiscussionSearchIndexRecord createRecord(Long replyId, String searchContent) {
		return createRecord(replyId, searchContent, false, false);
	}
	
	private DBODiscussionSearchIndexRecord createRecord(Long replyId, String searchContent, boolean threadDeleted, boolean replyDeleted) {
		DBODiscussionSearchIndexRecord record = new DBODiscussionSearchIndexRecord();
		record.setForumId(forumId);
		record.setThreadId(threadId);
		record.setThreadDeleted(threadDeleted);
		record.setReplyId(replyId);
		record.setReplyDeleted(replyDeleted);
		record.setSearchContent(searchContent);
		return record;
	}

}
