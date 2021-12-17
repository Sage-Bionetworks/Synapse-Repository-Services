package org.sagebionetworks.repo.manager.discussion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UploadContentToS3DAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.dao.discussion.DiscussionReplyDAO;
import org.sagebionetworks.repo.model.dbo.dao.discussion.DiscussionSearchIndexDao;
import org.sagebionetworks.repo.model.dbo.dao.discussion.DiscussionThreadDAO;
import org.sagebionetworks.repo.model.discussion.DiscussionFilter;
import org.sagebionetworks.repo.model.discussion.DiscussionReplyBundle;
import org.sagebionetworks.repo.model.discussion.DiscussionSearchRequest;
import org.sagebionetworks.repo.model.discussion.DiscussionSearchResponse;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadBundle;
import org.sagebionetworks.repo.model.discussion.Match;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonServiceException.ErrorType;
import com.amazonaws.services.s3.model.AmazonS3Exception;

@ExtendWith(MockitoExtension.class)
public class DiscussionSearchIndexManagerImplTest {

	@Mock
	private DiscussionSearchIndexDao mockDao;
	
	@Mock
	private DiscussionThreadDAO mockThreadDao;
	
	@Mock
	private DiscussionReplyDAO mockReplyDao;
	
	@Mock
	private UploadContentToS3DAO mockContentDao;
	
	@Mock
	private AuthorizationManager mockAuthManager;
	
	@InjectMocks
	private DiscussionSearchIndexManagerImpl manager;
	
	@Mock
	private UserInfo mockUser;
	
	@Mock
	private DiscussionThreadBundle mockThread;
	
	@Mock
	private DiscussionReplyBundle mockReply;
	
	@Test
	public void testSearch() {
		Long forumId = 123L;
		String searchString = "searchString";
		
		List<Match> matches = Arrays.asList(
			new Match().setForumId(forumId.toString()).setThreadId("456"),
			new Match().setForumId(forumId.toString()).setThreadId("456").setReplyId("789")
		);
		
		when(mockDao.search(any(), any(), anyLong(), anyLong())).thenReturn(matches);
		when(mockAuthManager.canAccess(any(), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		
		DiscussionSearchRequest searchRequest = new DiscussionSearchRequest()
				.setSearchString(searchString);
		
		DiscussionSearchResponse expected = new DiscussionSearchResponse()
			.setMatches(matches)
			.setNextPageToken(null);
		
		// Call under test
		DiscussionSearchResponse result = manager.search(mockUser, forumId, searchRequest);
		
		assertEquals(expected, result);
		
		verify(mockAuthManager).canAccess(mockUser, forumId.toString(), ObjectType.ENTITY, ACCESS_TYPE.READ);
		verify(mockDao).search(forumId, searchString, NextPageToken.DEFAULT_LIMIT + 1, NextPageToken.DEFAULT_OFFSET);
		
	}
	
	@Test
	public void testSearchWithAdditionalResults() {
		Long forumId = 123L;
		String searchString = "searchString";
		
		List<Match> matches = new ArrayList<>();
		
		for (int i=0; i< NextPageToken.DEFAULT_LIMIT + 1; i++) {
			matches.add(new Match().setForumId(forumId.toString()).setThreadId("456").setReplyId(String.valueOf(i)));
		}
		
		when(mockDao.search(any(), any(), anyLong(), anyLong())).thenReturn(matches);
		when(mockAuthManager.canAccess(any(), any(), any(), any())).thenReturn(AuthorizationStatus.authorized());
		
		DiscussionSearchRequest searchRequest = new DiscussionSearchRequest()
				.setSearchString(searchString);
		
		DiscussionSearchResponse expected = new DiscussionSearchResponse()
			.setMatches(matches.subList(0, matches.size() - 1))
			.setNextPageToken(new NextPageToken(NextPageToken.DEFAULT_LIMIT, NextPageToken.DEFAULT_LIMIT).toToken());
		
		// Call under test
		DiscussionSearchResponse result = manager.search(mockUser, forumId, searchRequest);
		
		assertEquals(expected, result);
		
		verify(mockAuthManager).canAccess(mockUser, forumId.toString(), ObjectType.ENTITY, ACCESS_TYPE.READ);
		verify(mockDao).search(forumId, searchString, NextPageToken.DEFAULT_LIMIT + 1, NextPageToken.DEFAULT_OFFSET);
		
	}
	
	@Test
	public void testSearchUnauthorized() {
		Long forumId = 123L;
		String searchString = "searchString";
		
		when(mockAuthManager.canAccess(any(), any(), any(), any())).thenReturn(AuthorizationStatus.accessDenied("denied"));
		
		DiscussionSearchRequest searchRequest = new DiscussionSearchRequest()
				.setSearchString(searchString);
		
		String message = assertThrows(UnauthorizedException.class, () -> {			
			// Call under test
			manager.search(mockUser, forumId, searchRequest);
		}).getMessage();
		
		assertEquals("denied", message);
		
		verify(mockAuthManager).canAccess(mockUser, forumId.toString(), ObjectType.ENTITY, ACCESS_TYPE.READ);
		verifyZeroInteractions(mockDao);
		
	}
	
	@Test
	public void testSearchWithNoForumId() {
		Long forumId = null;
		String searchString = "searchString";
				
		DiscussionSearchRequest searchRequest = new DiscussionSearchRequest()
				.setSearchString(searchString);
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.search(mockUser, forumId, searchRequest);
		}).getMessage();
		
		assertEquals("The forumId is required.", message);
		
		verifyZeroInteractions(mockAuthManager);
		verifyZeroInteractions(mockDao);	
	}
	
	@Test
	public void testSearchWithNoUser() {
		Long forumId = 123L;
		String searchString = "searchString";
				
		DiscussionSearchRequest searchRequest = new DiscussionSearchRequest()
				.setSearchString(searchString);
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.search(null, forumId, searchRequest);
		}).getMessage();
		
		assertEquals("The user is required.", message);
		
		verifyZeroInteractions(mockAuthManager);
		verifyZeroInteractions(mockDao);
	}
	
	@Test
	public void testSearchWithNoRequest() {
		Long forumId = 123L;
				
		DiscussionSearchRequest searchRequest = null;
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.search(mockUser, forumId, searchRequest);
		}).getMessage();
		
		assertEquals("The search request is required.", message);
		
		verifyZeroInteractions(mockAuthManager);
		verifyZeroInteractions(mockDao);
		
	}
	
	@Test
	public void testSearchWithEmtpySearchString() {
		Long forumId = 123L;
		String searchString = " ";
				
		DiscussionSearchRequest searchRequest = new DiscussionSearchRequest()
				.setSearchString(searchString);
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.search(mockUser, forumId, searchRequest);
		}).getMessage();
		
		assertEquals("The request.searchString is required and must not be a blank string.", message);
		
		verifyZeroInteractions(mockAuthManager);
		verifyZeroInteractions(mockDao);
	}
	
	@Test
	public void testSearchWithNullSearchString() {
		Long forumId = 123L;
		String searchString = null;
				
		DiscussionSearchRequest searchRequest = new DiscussionSearchRequest()
				.setSearchString(searchString);
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.search(mockUser, forumId, searchRequest);
		}).getMessage();
		
		assertEquals("The request.searchString is required and must not be the empty string.", message);
		
		verifyZeroInteractions(mockAuthManager);
		verifyZeroInteractions(mockDao);
	}

	@Test
	public void testSearchWithTooShortSearchString() {
		Long forumId = 123L;
		String searchString = "  ab   ";
				
		DiscussionSearchRequest searchRequest = new DiscussionSearchRequest()
				.setSearchString(searchString);
		
		String message = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.search(mockUser, forumId, searchRequest);
		}).getMessage();
		
		assertEquals("The search string should be at least 3 characters.", message);
		
		verifyZeroInteractions(mockAuthManager);
		verifyZeroInteractions(mockDao);
	}
	
	@Test
	public void testProcessThreadChange() {
		Long forumId = 456L;
		Long threadId = 123L;
		boolean isThreadDeleted = false;
		String messageKey = "key";
		String content = "some content";
		
		when(mockThreadDao.getThread(anyLong(), any())).thenReturn(mockThread);
		when(mockThread.getForumId()).thenReturn(forumId.toString());
		when(mockThread.getIsDeleted()).thenReturn(isThreadDeleted);
		when(mockThread.getMessageKey()).thenReturn(messageKey);
		when(mockContentDao.getMessage(any())).thenReturn(content);
		
		// Call under test
		manager.processThreadChange(threadId);
		
		verify(mockThreadDao).getThread(threadId, DiscussionFilter.NO_FILTER);
		verify(mockContentDao).getMessage(messageKey);
		verify(mockDao).createOrUpdateRecordForThread(forumId, threadId, content);
		verify(mockDao).markThreadAsNotDeleted(threadId);
		
		verifyNoMoreInteractions(mockThreadDao);
		verifyNoMoreInteractions(mockContentDao);
		verifyNoMoreInteractions(mockDao);
	}
	
	@Test
	public void testProcessThreadChangeWithDelete() {
		Long threadId = 123L;
		boolean isThreadDeleted = true;
		
		when(mockThreadDao.getThread(anyLong(), any())).thenReturn(mockThread);
		when(mockThread.getIsDeleted()).thenReturn(isThreadDeleted);
		
		// Call under test
		manager.processThreadChange(threadId);
		
		verify(mockThreadDao).getThread(threadId, DiscussionFilter.NO_FILTER);
		verify(mockDao).markThreadAsDeleted(threadId);
		
		verifyNoMoreInteractions(mockThreadDao);
		verifyNoMoreInteractions(mockContentDao);
		verifyNoMoreInteractions(mockDao);
	}
	
	@Test
	public void testProcessThreadChangeWithNotFoundException() {
		Long threadId = 123L;
		
		when(mockThreadDao.getThread(anyLong(), any())).thenThrow(NotFoundException.class);
		
		// Call under test
		manager.processThreadChange(threadId);
		
		verify(mockThreadDao).getThread(threadId, DiscussionFilter.NO_FILTER);
		verify(mockDao).markThreadAsDeleted(threadId);
		
		verifyNoMoreInteractions(mockThreadDao);
		verifyNoMoreInteractions(mockContentDao);
		verifyNoMoreInteractions(mockDao);
	}
	
	@Test
	public void testProcessThreadChangeWithAmazonNotFoundException() {
		Long forumId = 456L;
		Long threadId = 123L;
		boolean isThreadDeleted = false;
		String messageKey = "key";
		
		when(mockThreadDao.getThread(anyLong(), any())).thenReturn(mockThread);
		when(mockThread.getForumId()).thenReturn(forumId.toString());
		when(mockThread.getIsDeleted()).thenReturn(isThreadDeleted);
		when(mockThread.getMessageKey()).thenReturn(messageKey);
		
		AmazonServiceException ex = new AmazonS3Exception("not found");
		ex.setStatusCode(HttpStatus.SC_NOT_FOUND);
		
		when(mockContentDao.getMessage(any())).thenThrow(ex);
		
		// Call under test
		manager.processThreadChange(threadId);
		
		verify(mockThreadDao).getThread(threadId, DiscussionFilter.NO_FILTER);
		verify(mockContentDao).getMessage(messageKey);
		verify(mockDao).createOrUpdateRecordForThread(forumId, threadId, "");
		verify(mockDao).markThreadAsNotDeleted(threadId);
		
		verifyNoMoreInteractions(mockThreadDao);
		verifyNoMoreInteractions(mockContentDao);
		verifyNoMoreInteractions(mockDao);
	}
	
	@Test
	public void testProcessThreadChangeWithAmazonServiceException() {
		Long threadId = 123L;
		boolean isThreadDeleted = false;
		String messageKey = "key";
		
		when(mockThreadDao.getThread(anyLong(), any())).thenReturn(mockThread);
		when(mockThread.getIsDeleted()).thenReturn(isThreadDeleted);
		when(mockThread.getMessageKey()).thenReturn(messageKey);
		
		AmazonServiceException ex = new AmazonS3Exception("something");
		ex.setErrorType(ErrorType.Service);
		
		when(mockContentDao.getMessage(any())).thenThrow(ex);
		
		RecoverableMessageException result = assertThrows(RecoverableMessageException.class, () -> {			
			// Call under test
			manager.processThreadChange(threadId);
		});
		
		assertEquals(ex, result.getCause());
		
		verify(mockThreadDao).getThread(threadId, DiscussionFilter.NO_FILTER);
		verify(mockContentDao).getMessage(messageKey);
		
		verifyNoMoreInteractions(mockThreadDao);
		verifyNoMoreInteractions(mockContentDao);
		verifyNoMoreInteractions(mockDao);
	}
	
	@Test
	public void testProcessThreadChangeWithAmazonOtherException() {
		Long threadId = 123L;
		boolean isThreadDeleted = false;
		String messageKey = "key";
		
		when(mockThreadDao.getThread(anyLong(), any())).thenReturn(mockThread);
		when(mockThread.getIsDeleted()).thenReturn(isThreadDeleted);
		when(mockThread.getMessageKey()).thenReturn(messageKey);
		
		AmazonServiceException ex = new AmazonS3Exception("something else");
		
		when(mockContentDao.getMessage(any())).thenThrow(ex);
		
		AmazonServiceException result = assertThrows(AmazonServiceException.class, () -> {			
			// Call under test
			manager.processThreadChange(threadId);
		});
		
		assertEquals(ex, result);
		
		verify(mockThreadDao).getThread(threadId, DiscussionFilter.NO_FILTER);
		verify(mockContentDao).getMessage(messageKey);
		
		verifyNoMoreInteractions(mockThreadDao);
		verifyNoMoreInteractions(mockContentDao);
		verifyNoMoreInteractions(mockDao);
	}
	
	@Test
	public void testProcessReplyChange() {
		Long forumId = 456L;
		Long threadId = 123L;
		Long replyId = 789L;
		boolean isThreadDeleted = false;
		boolean isReplyDeleted = false;
		String messageKey = "key";
		String content = "some content";
		
		when(mockReplyDao.getReply(anyLong(), any())).thenReturn(mockReply);
		when(mockReply.getThreadId()).thenReturn(threadId.toString());
		when(mockReply.getForumId()).thenReturn(forumId.toString());
		when(mockReply.getIsDeleted()).thenReturn(isReplyDeleted);
		when(mockThreadDao.getThread(anyLong(), any())).thenReturn(mockThread);
		when(mockThread.getIsDeleted()).thenReturn(isThreadDeleted);
		when(mockReply.getMessageKey()).thenReturn(messageKey);
		when(mockContentDao.getMessage(any())).thenReturn(content);
		
		// Call under test
		manager.processReplyChange(replyId);
		
		verify(mockReplyDao).getReply(replyId, DiscussionFilter.NO_FILTER);
		verify(mockThreadDao).getThread(threadId, DiscussionFilter.NO_FILTER);
		verify(mockContentDao).getMessage(messageKey);
		verify(mockDao).createOrUpdateRecordForReply(forumId, threadId, replyId, content);
		verify(mockDao).markReplyAsNotDeleted(replyId);
		verify(mockDao).markThreadAsNotDeleted(threadId);
		
		verifyNoMoreInteractions(mockThreadDao);
		verifyNoMoreInteractions(mockContentDao);
		verifyNoMoreInteractions(mockDao);
	}
	
	@Test
	public void testProcessReplyChangeWithThreadDeleted() {
		Long forumId = 456L;
		Long threadId = 123L;
		Long replyId = 789L;
		boolean isThreadDeleted = true;
		boolean isReplyDeleted = false;
		String messageKey = "key";
		String content = "some content";
		
		when(mockReplyDao.getReply(anyLong(), any())).thenReturn(mockReply);
		when(mockReply.getThreadId()).thenReturn(threadId.toString());
		when(mockReply.getForumId()).thenReturn(forumId.toString());
		when(mockReply.getIsDeleted()).thenReturn(isReplyDeleted);
		when(mockThreadDao.getThread(anyLong(), any())).thenReturn(mockThread);
		when(mockThread.getIsDeleted()).thenReturn(isThreadDeleted);
		when(mockReply.getMessageKey()).thenReturn(messageKey);
		when(mockContentDao.getMessage(any())).thenReturn(content);
		
		// Call under test
		manager.processReplyChange(replyId);
		
		verify(mockReplyDao).getReply(replyId, DiscussionFilter.NO_FILTER);
		verify(mockThreadDao).getThread(threadId, DiscussionFilter.NO_FILTER);
		verify(mockContentDao).getMessage(messageKey);
		verify(mockDao).createOrUpdateRecordForReply(forumId, threadId, replyId, content);
		verify(mockDao).markReplyAsNotDeleted(replyId);
		verify(mockDao).markThreadAsDeleted(threadId);
		
		verifyNoMoreInteractions(mockThreadDao);
		verifyNoMoreInteractions(mockContentDao);
		verifyNoMoreInteractions(mockDao);
	}
	
	@Test
	public void testProcessReplyChangeWithReplyDeleted() {
		Long threadId = 123L;
		Long replyId = 789L;
		boolean isThreadDeleted = false;
		boolean isReplyDeleted = true;
		
		when(mockReplyDao.getReply(anyLong(), any())).thenReturn(mockReply);
		when(mockReply.getThreadId()).thenReturn(threadId.toString());
		when(mockReply.getIsDeleted()).thenReturn(isReplyDeleted);
		when(mockThreadDao.getThread(anyLong(), any())).thenReturn(mockThread);
		when(mockThread.getIsDeleted()).thenReturn(isThreadDeleted);
		
		// Call under test
		manager.processReplyChange(replyId);
		
		verify(mockReplyDao).getReply(replyId, DiscussionFilter.NO_FILTER);
		verify(mockThreadDao).getThread(threadId, DiscussionFilter.NO_FILTER);
		verify(mockDao).markReplyAsDeleted(replyId);
		verify(mockDao).markThreadAsNotDeleted(threadId);
		
		verifyNoMoreInteractions(mockThreadDao);
		verifyNoMoreInteractions(mockContentDao);
		verifyNoMoreInteractions(mockDao);
	}
	
	@Test
	public void testProcessReplyChangeWithReplyAndThreadDeleted() {
		Long threadId = 123L;
		Long replyId = 789L;
		boolean isThreadDeleted = true;
		boolean isReplyDeleted = true;
		
		when(mockReplyDao.getReply(anyLong(), any())).thenReturn(mockReply);
		when(mockReply.getThreadId()).thenReturn(threadId.toString());
		when(mockReply.getIsDeleted()).thenReturn(isReplyDeleted);
		when(mockThreadDao.getThread(anyLong(), any())).thenReturn(mockThread);
		when(mockThread.getIsDeleted()).thenReturn(isThreadDeleted);
		
		// Call under test
		manager.processReplyChange(replyId);
		
		verify(mockReplyDao).getReply(replyId, DiscussionFilter.NO_FILTER);
		verify(mockThreadDao).getThread(threadId, DiscussionFilter.NO_FILTER);
		verify(mockDao).markReplyAsDeleted(replyId);
		verify(mockDao).markThreadAsDeleted(threadId);
		
		verifyNoMoreInteractions(mockThreadDao);
		verifyNoMoreInteractions(mockContentDao);
		verifyNoMoreInteractions(mockDao);
	}
	
	@Test
	public void testProcessReplyChangeWithReplyNotFound() {
		Long replyId = 789L;
		
		when(mockReplyDao.getReply(anyLong(), any())).thenThrow(NotFoundException.class);
		
		// Call under test
		manager.processReplyChange(replyId);
		
		verify(mockReplyDao).getReply(replyId, DiscussionFilter.NO_FILTER);
		verify(mockDao).markReplyAsDeleted(replyId);
		
		verifyNoMoreInteractions(mockThreadDao);
		verifyNoMoreInteractions(mockContentDao);
		verifyNoMoreInteractions(mockDao);
	}
	
	@Test
	public void testProcessReplyChangeWithThreadNotFound() {
		Long forumId = 456L;
		Long threadId = 123L;
		Long replyId = 789L;
		boolean isReplyDeleted = false;
		String messageKey = "key";
		String content = "some content";
		
		when(mockReplyDao.getReply(anyLong(), any())).thenReturn(mockReply);
		when(mockReply.getThreadId()).thenReturn(threadId.toString());
		when(mockReply.getForumId()).thenReturn(forumId.toString());
		when(mockReply.getIsDeleted()).thenReturn(isReplyDeleted);
		when(mockThreadDao.getThread(anyLong(), any())).thenThrow(NotFoundException.class);
		when(mockReply.getMessageKey()).thenReturn(messageKey);
		when(mockContentDao.getMessage(any())).thenReturn(content);
		
		// Call under test
		manager.processReplyChange(replyId);
		
		verify(mockReplyDao).getReply(replyId, DiscussionFilter.NO_FILTER);
		verify(mockThreadDao).getThread(threadId, DiscussionFilter.NO_FILTER);
		verify(mockContentDao).getMessage(messageKey);
		verify(mockDao).createOrUpdateRecordForReply(forumId, threadId, replyId, content);
		verify(mockDao).markReplyAsNotDeleted(replyId);
		verify(mockDao).markThreadAsDeleted(threadId);
		
		verifyNoMoreInteractions(mockThreadDao);
		verifyNoMoreInteractions(mockContentDao);
		verifyNoMoreInteractions(mockDao);
	}
	
	@Test
	public void testProcessReplyChangeWithReplyDeletedAndThreadNotFound() {
		Long threadId = 123L;
		Long replyId = 789L;
		boolean isReplyDeleted = true;
		
		when(mockReplyDao.getReply(anyLong(), any())).thenReturn(mockReply);
		when(mockReply.getThreadId()).thenReturn(threadId.toString());
		when(mockReply.getIsDeleted()).thenReturn(isReplyDeleted);
		when(mockThreadDao.getThread(anyLong(), any())).thenThrow(NotFoundException.class);
		
		// Call under test
		manager.processReplyChange(replyId);
		
		verify(mockReplyDao).getReply(replyId, DiscussionFilter.NO_FILTER);
		verify(mockThreadDao).getThread(threadId, DiscussionFilter.NO_FILTER);
		verify(mockDao).markReplyAsDeleted(replyId);
		verify(mockDao).markThreadAsDeleted(threadId);
		
		verifyNoMoreInteractions(mockThreadDao);
		verifyNoMoreInteractions(mockContentDao);
		verifyNoMoreInteractions(mockDao);
	}
	
	@Test
	public void testBuildSearchContent() {
		String[] contentArray = new String[] {"a", "b"};
		
		String expected = "a b";
		
		String result = DiscussionSearchIndexManagerImpl.buildSearchContent(contentArray);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testBuildSearchContentWithEmpty() {
		String[] contentArray = new String[] {"a", " "};
		
		String expected = "a";
		
		String result = DiscussionSearchIndexManagerImpl.buildSearchContent(contentArray);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testBuildSearchContentWithAllEmpty() {
		String[] contentArray = new String[] {null, "", " "};
		
		String expected = "";
		
		String result = DiscussionSearchIndexManagerImpl.buildSearchContent(contentArray);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testBuildSearchContentWithNull() {
		String[] contentArray = new String[] {"a", null};
		
		String expected = "a";
		
		String result = DiscussionSearchIndexManagerImpl.buildSearchContent(contentArray);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testBuildSearchContentWithNone() {
		String[] contentArray = new String[] { };
		
		String expected = "";
		
		String result = DiscussionSearchIndexManagerImpl.buildSearchContent(contentArray);
		
		assertEquals(expected, result);
	}

}
