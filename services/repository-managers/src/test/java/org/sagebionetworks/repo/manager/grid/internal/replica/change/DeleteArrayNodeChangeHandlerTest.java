package org.sagebionetworks.repo.manager.grid.internal.replica.change;

import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.repo.model.grid.patch.Timespan;
import org.sagebionetworks.repo.model.grid.patch.operation.builder.Operations;

@ExtendWith(MockitoExtension.class)
public class DeleteArrayNodeChangeHandlerTest {

	@Mock
	private PatchBuilder mockPatchBuilder;

	private DeleteArrayNodeChangeHandler handler;

	private DeleteArrayNodeChange change;

	@BeforeEach
	public void before() {
		handler = new DeleteArrayNodeChangeHandler();

		LogicalTimestamp arrId = new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(1L);
		LogicalTimestamp nodeId = new LogicalTimestamp().setReplicaId(123L).setSequenceNumber(45L);

		change = new DeleteArrayNodeChange(arrId, nodeId);
	}

	@Test
	public void testHandleChange() {
		Long repId = 5L;

		Timespan timespan = new Timespan(change.getRgaNodeId(), 1L);

		when(mockPatchBuilder
				.addOperationBuilder(Operations.delete().setNodeId(change.getArrId()).setTimespans(List.of(timespan))))
				.thenReturn(new LogicalTimestamp().setReplicaId(repId).setSequenceNumber(60L));

		// call under test
		handler.handleChange(mockPatchBuilder, change);

		verifyNoMoreInteractions(mockPatchBuilder);
	}

}
