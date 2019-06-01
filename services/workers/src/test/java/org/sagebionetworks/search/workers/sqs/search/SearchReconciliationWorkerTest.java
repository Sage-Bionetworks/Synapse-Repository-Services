package org.sagebionetworks.search.workers.sqs.search;

import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.search.SearchManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.util.Callback;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;


@RunWith(MockitoJUnitRunner.class)
public class SearchReconciliationWorkerTest {

	@Mock
	WorkerLogger mockWorkerLogger;

	@Mock
	DBOChangeDAO mockChangeDao;

	@Mock
	SearchManager mockSearchManager;
	
	@Mock
	ProgressCallback mockProgressCallback;
	
	@InjectMocks
	SearchReconciliationWorker worker;
	
	@Test
	public void testPushChange() {
		ChangeMessage toPush = new ChangeMessage();
		toPush.setChangeNumber(123L);
		// call under test
		worker.pushChange(toPush);
		List<ChangeMessage> expected = Lists.newArrayList(toPush);
		verify(mockSearchManager).documentChangeMessages(expected);
		verifyZeroInteractions(mockWorkerLogger);
	}
	
	@Test
	public void testPushChangeException() {
		IllegalArgumentException error = new IllegalArgumentException("wrong");
		doThrow(error).when(mockSearchManager).documentChangeMessages(anyList());
		ChangeMessage toPush = new ChangeMessage();
		toPush.setChangeNumber(123L);
		// call under test
		worker.pushChange(toPush);
		verify(mockWorkerLogger).logWorkerFailure(SearchReconciliationWorker.class.getName(), error, false);;
	}
	
	@Test
	public void testRun() throws Exception {
		Set<ObjectType> expectedObjectTypes = Sets.newHashSet(ObjectType.ENTITY, ObjectType.WIKI);
		Set<ChangeType> expectedChangeTypes = Sets.newHashSet(ChangeType.CREATE, ChangeType.UPDATE);
		// call under test
		worker.run(mockProgressCallback);
		verify(mockChangeDao).streamOverChanges(eq(expectedObjectTypes), eq(expectedChangeTypes), any(Callback.class));
		verifyZeroInteractions(mockWorkerLogger);
	}
	
	@Test
	public void testRunError() throws Exception {
		IllegalArgumentException error = new IllegalArgumentException("wrong");
		doThrow(error).when(mockChangeDao).streamOverChanges(anySet(), anySet(), any(Callback.class));
		// call under test
		worker.run(mockProgressCallback);
		verify(mockWorkerLogger).logWorkerFailure(SearchReconciliationWorker.class.getName(), error, true);;
	}
}
