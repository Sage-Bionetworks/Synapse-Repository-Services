package org.sagebionetworks.discussion.workers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.discussion.DiscussionReplyManager;
import org.sagebionetworks.repo.manager.discussion.DiscussionSearchIndexManager;
import org.sagebionetworks.repo.manager.discussion.DiscussionThreadManager;
import org.sagebionetworks.repo.manager.discussion.ForumManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionReply;
import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;
import org.sagebionetworks.repo.model.discussion.DiscussionSearchRequest;
import org.sagebionetworks.repo.model.discussion.Match;
import org.sagebionetworks.repo.model.discussion.UpdateReplyMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadMessage;
import org.sagebionetworks.repo.model.discussion.UpdateThreadTitle;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DiscussionSearchIndexWorkerIntegrationTest {
	
	private static final int TIMEOUT = 30 * 1000;

	@Autowired
	private UserManager userManager;
	
	@Autowired
	private EntityManager entityManager;
	
	@Autowired
	private ForumManager forumManager;
	
	@Autowired
	private DiscussionThreadManager threadManager;
	
	@Autowired
	private DiscussionReplyManager replyManager;
	
	@Autowired
	private DiscussionSearchIndexManager searchManager;
	
	private UserInfo user;

	private Long forumId;
	
	@BeforeEach
	public void before() {
		entityManager.truncateAll();
		
		user = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		String projectId = entityManager.createEntity(user, new Project().setName(UUID.randomUUID().toString()), null);

		forumId = Long.valueOf(forumManager.createForum(user, projectId).getId());
	}
	
	@AfterEach
	public void after() {
		entityManager.truncateAll();		
	}
	
	@Test
	public void testThreadIndexCRUD() throws Exception {
		List<Match> expected = Collections.emptyList();
		
		assertResults("threadcontent", expected);
		
		// Call under test
		String threadId = threadManager.createThread(user, new CreateDiscussionThread()
			.setForumId(forumId.toString())
			.setTitle("threadtitle")
			.setMessageMarkdown("threadcontent")).getId();
		
		expected = Arrays.asList(
			match(threadId, null)
		);
		
		assertResults("threadcontent", expected);
		
		// Call under test
		threadManager.updateTitle(user, threadId, new UpdateThreadTitle().setTitle("newtitle"));
		
		// Searching with the old title should not work
		assertResults("threadtitle", Collections.emptyList());
		assertResults("newtitle", expected);
		
		// Call under test
		threadManager.updateMessage(user, threadId, new UpdateThreadMessage().setMessageMarkdown("newthreadcontent"));
		
		// Searching with the old content should not work
		assertResults("threadcontent", Collections.emptyList());
		assertResults("newthreadcontent", expected);
		
		// Call under test, if we mark it as delete it should disappear
		threadManager.markThreadAsDeleted(user, threadId);
		
		assertResults("newthreadcontent", Collections.emptyList());
		
		// Call under test, restoring the thread should make it appear in the results
		threadManager.markThreadAsNotDeleted(user, threadId);
		
		assertResults("newthreadcontent", expected);
		
	}	
	
	@Test
	public void testReplyIndexCRUD() throws Exception {
		// Call under test
		String threadId = threadManager.createThread(user, new CreateDiscussionThread()
			.setForumId(forumId.toString())
			.setTitle("threadtitle")
			.setMessageMarkdown("threadcontent")).getId();
				
		List<Match> expected = Collections.emptyList();
		
		assertResults("replycontent", expected);
		
		// Call under test
		String replyId = replyManager.createReply(user, new CreateDiscussionReply().setThreadId(threadId).setMessageMarkdown("replycontent")).getId();
		
		expected = Arrays.asList(
			match(threadId, replyId)
		);
		
		assertResults("replycontent", expected);
		
		// Call under test
		replyManager.updateReplyMessage(user, replyId, new UpdateReplyMessage().setMessageMarkdown("newreplycontent"));
		
		// Searching with the old content should not work
		assertResults("replycontent", Collections.emptyList());
		assertResults("newreplycontent", expected);
		
		// Call under test, if we delete the thread the reply should not appear
		threadManager.markThreadAsDeleted(user, threadId);
		
		assertResults("newreplycontent", Collections.emptyList());
		
		// Call under test, restoring the thread should make the reply appear as well
		threadManager.markThreadAsNotDeleted(user, threadId);
		
		assertResults("newreplycontent", expected);
		
		// Call under test, deleted the reply should make it disappear
		replyManager.markReplyAsDeleted(user, replyId);
		
		assertResults("newreplycontent", Collections.emptyList());
				
	}
	
	List<Match> assertResults(String query, List<Match> expected) throws Exception {
		return TimeUtils.waitFor(TIMEOUT, 1000, () -> {
			List<Match> matches = searchManager.search(user, forumId, new DiscussionSearchRequest().setSearchString(query)).getMatches();
			
			boolean result = matches.equals(expected);
			
			return Pair.create(result, matches);
		});
	}
	
	Match match(String threadId, String replyId) {
		return new Match().setForumId(forumId.toString()).setThreadId(threadId).setReplyId(replyId);
	}
	
}
