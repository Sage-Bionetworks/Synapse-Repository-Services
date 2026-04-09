package org.sagebionetworks.repo.manager.grid.synch.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.AddColumnChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.DeleteArrayNodeChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.IntendedChangePublisher;
import org.sagebionetworks.repo.manager.grid.internal.replica.change.UpdateColumnNamesChange;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.Column;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.GridHeader;
import org.sagebionetworks.repo.manager.grid.synch.handler.CopyHandler;
import org.sagebionetworks.repo.model.grid.GridConnectionInfo;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

@ExtendWith(MockitoExtension.class)
public class SchemaCopyImplTest {

	@Mock
	private IntendedChangePublisher mockIntendedChangePublisher;
	@Mock
	private CopyHandler mockCopyHandler;
	@Mock
	private GridHeader mockHeader;
	@Mock
	private GridConnectionInfo mockConnection;

	private Long internalReplicaId;
	private Long userReplicaId;
	private List<Column> startingSchema;
	private LogicalTimestamp columnOrderArrId;
	private LogicalTimestamp columnNamesVecId;

	private SchemaCopyImpl copy;

	@BeforeEach
	public void before() {
		internalReplicaId = 5555L;
		userReplicaId = 88888L;

		columnOrderArrId = new LogicalTimestamp().setReplicaId(internalReplicaId).setSequenceNumber(0L);
		columnNamesVecId = new LogicalTimestamp().setReplicaId(internalReplicaId).setSequenceNumber(99L);

		startingSchema = List.of(
				// one
				new Column().setName("one")
						.setColumnOrderNodeId(
								new LogicalTimestamp().setReplicaId(internalReplicaId).setSequenceNumber(1L))
						.setVectorIndex(2),
				// two
				new Column().setName("two")
						.setColumnOrderNodeId(new LogicalTimestamp().setReplicaId(userReplicaId).setSequenceNumber(2L))
						.setVectorIndex(1),
				// two
				new Column().setName("three")
						.setColumnOrderNodeId(
								new LogicalTimestamp().setReplicaId(internalReplicaId).setSequenceNumber(3L))
						.setVectorIndex(0));
	}

	private void setupCopy(List<Column> startingSchema) {
		when(mockCopyHandler.getHeader()).thenReturn(mockHeader);
		when(mockHeader.getOrderedColumns()).thenReturn(startingSchema);
		when(mockHeader.getColumnOrderArrId()).thenReturn(columnOrderArrId);
		when(mockHeader.getColumnNamesVecId()).thenReturn(columnNamesVecId);

		copy = new SchemaCopyImpl(mockIntendedChangePublisher, mockCopyHandler);
	}

	@Test
	public void testStreamItems() {
		setupCopy(startingSchema);
		List<ColumnCopyItem> expected = List.of(
				// one
				new ColumnCopyItem().setColumnName("one").setWasChangedByUser(false).setColumnOrderNodeId(
						new LogicalTimestamp().setReplicaId(internalReplicaId).setSequenceNumber(1L)),
				// two
				new ColumnCopyItem().setColumnName("two").setWasChangedByUser(true)
						.setColumnOrderNodeId(new LogicalTimestamp().setReplicaId(userReplicaId).setSequenceNumber(2L)),
				// three
				new ColumnCopyItem().setColumnName("three").setWasChangedByUser(false).setColumnOrderNodeId(
						new LogicalTimestamp().setReplicaId(internalReplicaId).setSequenceNumber(3L)));
		// call under test
		List<ColumnCopyItem> result = copy.streamItems().collect(Collectors.toList());
		assertEquals(expected, result);

		verifyNoMoreInteractions(mockConnection, mockCopyHandler, mockCopyHandler, mockIntendedChangePublisher);
	}

	@Test
	public void testCloseNoChanges() throws Exception {
		setupCopy(startingSchema);

		List<Column> expectedFinal = new ArrayList<>(startingSchema);
		List<Column> finalSchema = copy.getFinalSchema();
		assertEquals(expectedFinal, finalSchema);

		// call under test
		copy.close();

		verifyNoMoreInteractions(mockConnection, mockCopyHandler, mockCopyHandler, mockIntendedChangePublisher);
	}

	@Test
	public void testRemoveItem() throws Exception {
		setupCopy(startingSchema);
		LogicalTimestamp columnOrderNodeId = new LogicalTimestamp().setReplicaId(internalReplicaId)
				.setSequenceNumber(3L);
		ColumnCopyItem toRemove = new ColumnCopyItem().setColumnName("three").setColumnOrderNodeId(columnOrderNodeId);

		// call under test
		copy.removeItem(toRemove);

		List<Column> expectedFinal = List.of(startingSchema.get(0), startingSchema.get(1));
		// call under test
		List<Column> finalSchema = copy.getFinalSchema();
		assertEquals(expectedFinal, finalSchema);

		verify(mockIntendedChangePublisher).publish(new DeleteArrayNodeChange(columnOrderArrId, columnOrderNodeId));

		copy.close();
		verify(mockIntendedChangePublisher)
				.publish(new UpdateColumnNamesChange(columnNamesVecId, Map.of(2, "one", 1, "two")));

		verifyNoMoreInteractions(mockConnection, mockCopyHandler, mockCopyHandler, mockIntendedChangePublisher);

	}

	@Test
	public void testAddItem() throws Exception {
		setupCopy(startingSchema);

		// call under test
		copy.addItem(new ColumnSourceItem().setColumnName("four"));
		copy.addItem(new ColumnSourceItem().setColumnName("five"));

		List<Column> expectedFinal = new ArrayList<>(startingSchema);
		// index is set to be after the last index of 2
		expectedFinal.add(new Column().setName("four").setVectorIndex(3));
		expectedFinal.add(new Column().setName("five").setVectorIndex(4));
		// call under test
		List<Column> finalSchema = copy.getFinalSchema();
		assertEquals(expectedFinal, finalSchema);

		verify(mockIntendedChangePublisher).publish(new AddColumnChange(columnOrderArrId, columnOrderArrId, 3L));
		verify(mockIntendedChangePublisher).publish(new AddColumnChange(columnOrderArrId, columnOrderArrId, 4L));

		copy.close();
		verify(mockIntendedChangePublisher).publish(new UpdateColumnNamesChange(columnNamesVecId,
				Map.of(2, "one", 1, "two", 0, "three", 3, "four", 4, "five")));

		verifyNoMoreInteractions(mockConnection, mockCopyHandler, mockCopyHandler, mockIntendedChangePublisher);

	}

	@Test
	public void testAddItemWithEmptyStart() throws Exception {
		setupCopy(Collections.emptyList());

		// call under test
		copy.addItem(new ColumnSourceItem().setColumnName("four"));
		copy.addItem(new ColumnSourceItem().setColumnName("five"));

		List<Column> expectedFinal = new ArrayList<>();
		// index is set to be after the last index of 2
		expectedFinal.add(new Column().setName("four").setVectorIndex(0));
		expectedFinal.add(new Column().setName("five").setVectorIndex(1));
		// call under test
		List<Column> finalSchema = copy.getFinalSchema();
		assertEquals(expectedFinal, finalSchema);

		verify(mockIntendedChangePublisher).publish(new AddColumnChange(columnOrderArrId, columnOrderArrId, 0L));
		verify(mockIntendedChangePublisher).publish(new AddColumnChange(columnOrderArrId, columnOrderArrId, 1L));

		copy.close();
		// second close should not send a second change.
		copy.close();
		verify(mockIntendedChangePublisher)
				.publish(new UpdateColumnNamesChange(columnNamesVecId, Map.of(0, "four", 1, "five")));

		verifyNoMoreInteractions(mockConnection, mockCopyHandler, mockCopyHandler, mockIntendedChangePublisher);

	}

}
