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
import org.sagebionetworks.repo.manager.dataaccess.DataAccessRequestNotificationManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

@ExtendWith(MockitoExtension.class)
public class DataAccessRequestNotificationWorkerUnitTest {

	@Mock
	private DataAccessRequestNotificationManager mockManager;
	
	@Mock
	private ProgressCallback mockCallback;

	@InjectMocks
	private DataAccessRequestNotificationWorker worker;

	@Test
	public void testRunWithCreate() throws Exception {
		Date timeStamp = new Date(System.currentTimeMillis());
		ChangeMessage message = new ChangeMessage().setChangeType(ChangeType.CREATE)
				.setObjectType(ObjectType.DATA_ACCESS_REQUEST).setObjectId("123").setTimestamp(timeStamp);
		// call under test
		worker.run(mockCallback, message);
		verify(mockManager).sendNotificationToReviewers("123");
	}
	
	@Test
	public void testRunWithUpdate() throws Exception {
		Date timeStamp = new Date(System.currentTimeMillis());
		ChangeMessage message = new ChangeMessage().setChangeType(ChangeType.UPDATE)
				.setObjectType(ObjectType.DATA_ACCESS_REQUEST).setObjectId("123").setTimestamp(timeStamp);
		// call under test
		worker.run(mockCallback, message);
		verify(mockManager).sendNotificationToReviewers("123");
	}
	
	@Test
	public void testRunWithDelete() throws Exception {
		Date timeStamp = new Date(System.currentTimeMillis());
		ChangeMessage message = new ChangeMessage().setChangeType(ChangeType.DELETE)
				.setObjectType(ObjectType.DATA_ACCESS_REQUEST).setObjectId("123").setTimestamp(timeStamp);
		// call under test
		worker.run(mockCallback, message);
		verify(mockManager, never()).sendNotificationToReviewers(any());
	}
	
	@Test
	public void testRunWithWithAlmostExpired() throws Exception {
		Date timeStamp = new Date(Instant.now().plus(59, ChronoUnit.MINUTES).toEpochMilli());
		ChangeMessage message = new ChangeMessage().setChangeType(ChangeType.CREATE)
				.setObjectType(ObjectType.DATA_ACCESS_REQUEST).setObjectId("123").setTimestamp(timeStamp);
		// call under test
		worker.run(mockCallback, message);
		verify(mockManager).sendNotificationToReviewers("123");
	}
	
	@Test
	public void testRunWithWithExpired() throws Exception {
		Date timeStamp = new Date(Instant.now().plus(61, ChronoUnit.MINUTES).toEpochMilli());
		ChangeMessage message = new ChangeMessage().setChangeType(ChangeType.CREATE)
				.setObjectType(ObjectType.DATA_ACCESS_REQUEST).setObjectId("123").setTimestamp(timeStamp);
		// call under test
		worker.run(mockCallback, message);
		verify(mockManager, never()).sendNotificationToReviewers(any());
	}
	
	@Test
	public void testRunWithWrongObjectType() throws Exception {
		Date timeStamp = new Date(System.currentTimeMillis());
		ChangeMessage message = new ChangeMessage().setChangeType(ChangeType.CREATE)
				.setObjectType(ObjectType.ACTIVITY).setObjectId("123").setTimestamp(timeStamp);
		// call under test
		worker.run(mockCallback, message);
		verify(mockManager, never()).sendNotificationToReviewers(any());
		
		System.out.println(EntityFactory.createJSONStringForEntity(new ChangeMessage().setChangeType(ChangeType.UPDATE).setObjectId("3451154").setObjectType(ObjectType.PRINCIPAL)));
		
	}
	

}
