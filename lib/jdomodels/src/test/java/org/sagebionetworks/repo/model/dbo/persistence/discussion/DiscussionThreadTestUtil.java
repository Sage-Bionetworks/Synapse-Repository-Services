package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import java.util.Date;
import java.util.Random;
import java.util.UUID;

import org.sagebionetworks.repo.model.discussion.CreateDiscussionThread;

public class DiscussionThreadTestUtil {
	private static Random random = new Random();

	/**
	 * 
	 * @return a valid dbo
	 */
	public static DBODiscussionThread createValidDBO() {
		DBODiscussionThread dbo = new DBODiscussionThread();
		dbo.setId(random.nextLong());
		dbo.setForumId(random.nextLong());
		dbo.setTitle("title".getBytes(DiscussionThreadUtils.UTF8));
		dbo.setCreatedOn(new Date().getTime());
		dbo.setCreatedBy(random.nextLong());
		dbo.setModifiedOn(new Date().getTime());
		dbo.setMessageUrl(UUID.randomUUID().toString());
		dbo.setIsEdited(true);
		dbo.setIsDeleted(false);
		return dbo;
	}

	/**
	 * 
	 * @return a valid CreateThread
	 */
	public static CreateDiscussionThread createValidCreateThread() {
		CreateDiscussionThread createThread = new CreateDiscussionThread();
		createThread.setForumId(""+random.nextLong());
		createThread.setTitle("title");
		createThread.setMessageMarkdown("markdown");
		return createThread;
	}
}
