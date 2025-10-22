package org.sagebionetworks.grid.workers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.GridReplicaPatchBuilderManager;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChangeSerializable;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChangeSet;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.UpdateMetadataChange;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class GridReplicaPatchBuilderWorkerTest {

	@Mock
	private GridReplicaPatchBuilderManager mockGridReplicaPatchBuilderManager;
	@Mock
	private ProgressCallback mockCallback;

	@InjectMocks
	private GridReplicaPatchBuilderWorker worker;

	private Message message;
	private IntendedChangeSet changeSet;

	@BeforeEach
	public void before() {
		changeSet = new IntendedChangeSet().setSessionId("s1").setReplicaId(22L).setConnectionId("con1")
				.setChanges(List.of(new UpdateMetadataChange()
						.setRowObjectId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(2L)))).setClockSequenceMaximum(321L);
		message = new Message().withBody(IntendedChangeSerializable.serialize(changeSet).toString());
	}

	@Test
	public void testRun() throws RecoverableMessageException, Exception {

		// call under test
		worker.run(mockCallback, message);
		verify(mockGridReplicaPatchBuilderManager).buildPatch(changeSet);
	}

	@Test
	public void testRunWithRecoverableException() throws RecoverableMessageException, Exception {
		RecoverableMessageException e = new RecoverableMessageException("not now");
		doThrow(e).when(mockGridReplicaPatchBuilderManager).buildPatch(changeSet);

		String m1 = assertThrows(RecoverableMessageException.class, () -> {
			// call under test
			worker.run(mockCallback, message);
		}).getMessage();
		assertEquals("not now", m1);

		verify(mockGridReplicaPatchBuilderManager).buildPatch(changeSet);
	}

	@Test
	public void testRunWithOtherException() throws RecoverableMessageException, Exception {
		IllegalArgumentException e = new IllegalArgumentException("other");
		doThrow(e).when(mockGridReplicaPatchBuilderManager).buildPatch(changeSet);

		// call under test
		worker.run(mockCallback, message);
		verify(mockGridReplicaPatchBuilderManager).buildPatch(changeSet);
	}

}
