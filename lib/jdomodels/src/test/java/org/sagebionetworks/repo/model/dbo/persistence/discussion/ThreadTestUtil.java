package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.sagebionetworks.repo.model.discussion.DiscussionThread;

public class ThreadTestUtil {
	private static Random random = new Random();

	/**
	 * 
	 * @return a valid thread
	 */
	public static DiscussionThread createValidThread() {
		DiscussionThread dto = new DiscussionThread();
		dto.setId(""+random.nextLong());
		dto.setForumId(""+random.nextLong());
		dto.setTitle("title");
		dto.setCreatedOn(new Date());
		dto.setCreatedBy(""+random.nextLong());
		dto.setModifiedOn(new Date());
		dto.setMessageUrl(UUID.randomUUID().toString());
		dto.setIsEdited(true);
		dto.setIsDeleted(false);
		return dto;
	}

	/**
	 * 
	 * 
	 * @param numberOfThreads
	 * @param forumId
	 * @param userId
	 * @return
	 * @throws InterruptedException 
	 */
	public static List<DiscussionThread> createThreadList(int numberOfThreads,
			String forumId, String userId) throws InterruptedException {
		List<DiscussionThread> list = new ArrayList<DiscussionThread>();
		for (int i = 0; i < numberOfThreads; i++) {
			DiscussionThread dto = createValidThread();
			dto.setForumId(forumId);
			dto.setCreatedBy(userId);
			list.add(dto);
		}
		return list;
	}
}
