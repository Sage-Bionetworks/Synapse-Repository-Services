package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import java.util.Random;

import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;

public class DiscussionThreadTestUtil {
	private static Random random = new Random();

	/**
	 * 
	 * @return a valid CreateDiscussionThread
	 */
	public static CreateDiscussionThread createValidCreateDiscussionThread() {
		CreateDiscussionThread createThread = new CreateDiscussionThread();
		createThread.setForumId(""+random.nextLong());
		createThread.setTitle("title");
		createThread.setMessageMarkdown("markdown");
		return createThread;
	}
}
