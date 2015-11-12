package org.sagebionetworks.repo.model.dbo.persistence.discussion;

import java.util.Date;
import java.util.Random;

import org.sagebionetworks.repo.model.discussion.Thread;

public class ThreadTestUtil {
	private static Random random = new Random();

	public static DBOThread createValidatedThread() {
		Thread dto = new Thread();
		dto.setId(""+random.nextLong());
		dto.setForumId(""+random.nextLong());
		dto.setTitle("title");
		dto.setCreatedOn(new Date());
		dto.setCreatedBy(""+random.nextLong());
		dto.setModifiedOn(new Date());
		dto.setMessageUrl("messageUrl");
		dto.setIsEdited(true);
		dto.setIsDeleted(false);

		DBOThread dbo = ThreadUtils.createDBOFromDTO(dto);
		dbo.setEtag("etag");
		return dbo;
	}
}
