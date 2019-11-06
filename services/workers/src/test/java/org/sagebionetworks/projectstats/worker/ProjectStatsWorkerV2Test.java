package org.sagebionetworks.projectstats.worker;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.ProjectStatsManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.util.TimeoutUtils;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.test.util.ReflectionTestUtils;

public class ProjectStatsWorkerV2Test {

	@Mock
	TimeoutUtils mockTimeoutUtils;
	@Mock
	ProjectStatsManager mockProjectStatsManager;
	@Mock
	ProgressCallback mockProgressCallback;
	
	ProjectStatsWorkerV2 worker;
	
	ChangeMessage message;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);

		worker = new ProjectStatsWorkerV2();
		ReflectionTestUtils.setField(worker, "timeoutUtils", mockTimeoutUtils);
		ReflectionTestUtils.setField(worker, "projectStatsManager", mockProjectStatsManager);
		
		message = new ChangeMessage();
		message.setChangeNumber(123L);
		message.setChangeType(ChangeType.CREATE);
		message.setObjectId("syn456");
		message.setObjectType(ObjectType.ENTITY);
		message.setTimestamp(new Date(2));
		message.setUserId(777L);
		
		// not expired by default.
		when(mockTimeoutUtils.hasExpired(anyLong(), anyLong())).thenReturn(false);
		
	}
	
	@Test
	public void testRunNotExpired() throws Exception{
		worker.run(mockProgressCallback, message);
		verify(mockProjectStatsManager).updateProjectStats(message.getUserId(), message.getObjectId(), message.getObjectType(), message.getTimestamp());
	}
	
	@Test
	public void testRunExpired() throws Exception{
		// setup expired
		when(mockTimeoutUtils.hasExpired(anyLong(), anyLong())).thenReturn(true);
		worker.run(mockProgressCallback, message);
		verify(mockProjectStatsManager, never()).updateProjectStats(anyLong(), anyString(), any(ObjectType.class), any(Date.class));
	}
	
	@Test
	public void testIgnoreMissingUserId() throws RecoverableMessageException, Exception{
		message.setUserId(null);
		worker.run(mockProgressCallback, message);
		verify(mockProjectStatsManager, never()).updateProjectStats(anyLong(), anyString(), any(ObjectType.class), any(Date.class));
	}
}
