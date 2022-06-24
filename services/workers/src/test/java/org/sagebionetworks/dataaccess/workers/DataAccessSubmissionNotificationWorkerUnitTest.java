package org.sagebionetworks.dataaccess.workers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.dataaccess.DataAccessSubmissionNotificationManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionEvent;

@ExtendWith(MockitoExtension.class)
public class DataAccessSubmissionNotificationWorkerUnitTest {

	@Mock
	private DataAccessSubmissionNotificationManager mockManager;

	@Mock
	private ProgressCallback mockCallback;

	@InjectMocks
	private DataAccessSubmissionNotificationWorker worker;

	@Test
	public void testRunWithEvent() throws Exception {
		Date timeStamp = new Date(System.currentTimeMillis());
		DataAccessSubmissionEvent event = new DataAccessSubmissionEvent()
				.setObjectType(ObjectType.DATA_ACCESS_SUBMISSION_EVENT).setObjectId("123").setTimestamp(timeStamp);
		// call under test
		worker.run(mockCallback, null, event);
		verify(mockManager).sendNotificationToReviewers("123");
	}

	@Test
	public void testRunWithWithAlmostExpired() throws Exception {
		Date timeStamp = new Date(Instant.now().plus(59, ChronoUnit.MINUTES).toEpochMilli());
		DataAccessSubmissionEvent event = new DataAccessSubmissionEvent()
				.setObjectType(ObjectType.DATA_ACCESS_SUBMISSION_EVENT).setObjectId("123").setTimestamp(timeStamp);
		// call under test
		worker.run(mockCallback, null, event);
		verify(mockManager).sendNotificationToReviewers("123");
	}

	@Test
	public void testRunWithWithExpired() throws Exception {
		Date timeStamp = new Date(Instant.now().plus(61, ChronoUnit.MINUTES).toEpochMilli());
		DataAccessSubmissionEvent event = new DataAccessSubmissionEvent()
				.setObjectType(ObjectType.DATA_ACCESS_SUBMISSION_EVENT).setObjectId("123").setTimestamp(timeStamp);
		// call under test
		worker.run(mockCallback, null, event);
		verify(mockManager, never()).sendNotificationToReviewers(any());
	}

	@Test
	public void testRunWithWrongObjectType() throws Exception {
		Date timeStamp = new Date(System.currentTimeMillis());
		DataAccessSubmissionEvent event = new DataAccessSubmissionEvent()
				.setObjectType(ObjectType.ENTITY).setObjectId("123").setTimestamp(timeStamp);
		// call under test
		worker.run(mockCallback, null, event);
		verify(mockManager, never()).sendNotificationToReviewers(any());
	}

}
