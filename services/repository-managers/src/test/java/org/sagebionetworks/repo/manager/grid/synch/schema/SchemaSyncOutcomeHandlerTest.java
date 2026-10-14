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
import org.sagebionetworks.repo.manager.grid.synch.handler.SourceWriter;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

@ExtendWith(MockitoExtension.class)
public class SchemaSyncOutcomeHandlerTest {

	@Mock
	private IntendedChangePublisher mockIntendedChangePublisher;
	@Mock
	private CopyHandler mockCopyHandler;
	@Mock
	private GridHeader mockHeader;
	@Mock
	private SourceWriter mockSourceWriter;

	private Long internalReplicaId;
	private Long userReplicaId;
	private List<Column> startingSchema;
	private LogicalTimestamp columnOrderArrId;
	private LogicalTimestamp columnNamesVecId;

	private SchemaSyncOutcomeHandler handler;

	@BeforeEach
	public void before() {
		internalReplicaId = 5555L;
		userReplicaId = 88888L;

		columnOrderArrId = new LogicalTimestamp().setReplicaId(internalReplicaId).setSequenceNumber(0L);
		columnNamesVecId = new LogicalTimestamp().setReplicaId(internalReplicaId).setSequenceNumber(99L);

		startingSchema = List.of(
				new Column().setName("one")
						.setColumnOrderNodeId(
								new LogicalTimestamp().setReplicaId(internalReplicaId).setSequenceNumber(1L))
						.setVectorIndex(2),
				new Column().setName("two")
						.setColumnOrderNodeId(new LogicalTimestamp().setReplicaId(userReplicaId).setSequenceNumber(2L))
						.setVectorIndex(1),
				new Column().setName("three")
						.setColumnOrderNodeId(
								new LogicalTimestamp().setReplicaId(internalReplicaId).setSequenceNumber(3L))
						.setVectorIndex(0));
	}

	private void setupHandler(List<Column> startingSchema) {
		when(mockCopyHandler.getHeader()).thenReturn(mockHeader);
		when(mockHeader.getOrderedColumns()).thenReturn(startingSchema);
		when(mockHeader.getColumnOrderArrId()).thenReturn(columnOrderArrId);
		when(mockHeader.getColumnNamesVecId()).thenReturn(columnNamesVecId);

		handler = new SchemaSyncOutcomeHandler(mockIntendedChangePublisher, mockCopyHandler, mockSourceWriter);
	}

	@Test
	public void testStreamCopyItems() {
		setupHandler(startingSchema);
		List<ColumnCopyItem> expected = List.of(
				new ColumnCopyItem().setColumnName("one").setWasChangedByUser(false).setColumnOrderNodeId(
						new LogicalTimestamp().setReplicaId(internalReplicaId).setSequenceNumber(1L)),
				new ColumnCopyItem().setColumnName("two").setWasChangedByUser(true)
						.setColumnOrderNodeId(new LogicalTimestamp().setReplicaId(userReplicaId).setSequenceNumber(2L)),
				new ColumnCopyItem().setColumnName("three").setWasChangedByUser(false).setColumnOrderNodeId(
						new LogicalTimestamp().setReplicaId(internalReplicaId).setSequenceNumber(3L)));
		// call under test
		List<ColumnCopyItem> result = handler.streamCopyItems().collect(Collectors.toList());
		assertEquals(expected, result);

		verifyNoMoreInteractions(mockIntendedChangePublisher, mockSourceWriter);
	}

	@Test
	public void testCloseNoChanges() throws Exception {
		setupHandler(startingSchema);

		assertEquals(new ArrayList<>(startingSchema), handler.getFinalSchema());

		// call under test
		handler.close();

		verifyNoMoreInteractions(mockIntendedChangePublisher, mockSourceWriter);
	}

	@Test
	public void testOnDeletedFromSource() throws Exception {
		setupHandler(startingSchema);
		LogicalTimestamp columnOrderNodeId = new LogicalTimestamp().setReplicaId(internalReplicaId)
				.setSequenceNumber(3L);
		ColumnCopyItem toRemove = new ColumnCopyItem().setColumnName("three").setColumnOrderNodeId(columnOrderNodeId);

		// call under test — a column deleted from the source is dropped from the grid.
		handler.onDeletedFromSource(toRemove);

		assertEquals(List.of(startingSchema.get(0), startingSchema.get(1)), handler.getFinalSchema());
		verify(mockIntendedChangePublisher).publish(new DeleteArrayNodeChange(columnOrderArrId, columnOrderNodeId));

		handler.close();
		verify(mockIntendedChangePublisher)
				.publish(new UpdateColumnNamesChange(columnNamesVecId, Map.of(2, "one", 1, "two")));

		verifyNoMoreInteractions(mockIntendedChangePublisher, mockSourceWriter);
	}

	@Test
	public void testOnNewCopyItemWhenSourceAcceptsColumns() {
		setupHandler(startingSchema);
		when(mockSourceWriter.canAddRemoveColumns()).thenReturn(true);
		ColumnCopyItem userAdded = new ColumnCopyItem().setColumnName("two")
				.setColumnOrderNodeId(new LogicalTimestamp().setReplicaId(userReplicaId).setSequenceNumber(2L));

		// call under test — push the addition to the source; it stays in the grid.
		handler.onNewCopyItem(userAdded, "two");

		assertEquals(new ArrayList<>(startingSchema), handler.getFinalSchema());
		verify(mockSourceWriter).canAddRemoveColumns();
		verify(mockSourceWriter).addColumnToSource("two");
		verifyNoMoreInteractions(mockIntendedChangePublisher, mockSourceWriter);
	}

	@Test
	public void testOnNewCopyItemWhenSourceCannotAddColumns() {
		setupHandler(startingSchema);
		when(mockSourceWriter.canAddRemoveColumns()).thenReturn(false);
		LogicalTimestamp columnOrderNodeId = new LogicalTimestamp().setReplicaId(userReplicaId).setSequenceNumber(2L);
		ColumnCopyItem userAdded = new ColumnCopyItem().setColumnName("two")
				.setColumnOrderNodeId(columnOrderNodeId);

		// call under test — source cannot accept the column, so drop it from the grid.
		handler.onNewCopyItem(userAdded, "two");

		assertEquals(List.of(startingSchema.get(0), startingSchema.get(2)), handler.getFinalSchema());
		verify(mockSourceWriter).canAddRemoveColumns();
		verify(mockIntendedChangePublisher).publish(new DeleteArrayNodeChange(columnOrderArrId, columnOrderNodeId));
		verifyNoMoreInteractions(mockIntendedChangePublisher, mockSourceWriter);
	}

	@Test
	public void testOnDeletedFromCopy() {
		setupHandler(startingSchema);
		when(mockSourceWriter.canAddRemoveColumns()).thenReturn(true);

		// call under test — push the user's column deletion; do not re-import it.
		handler.onDeletedFromCopy(new ColumnSourceItem().setColumnName("gone"));

		assertEquals(new ArrayList<>(startingSchema), handler.getFinalSchema());
		verify(mockSourceWriter).canAddRemoveColumns();
		verify(mockSourceWriter).removeColumn("gone");
		verifyNoMoreInteractions(mockIntendedChangePublisher, mockSourceWriter);
	}

	@Test
	public void testOnNewSourceItem() throws Exception {
		setupHandler(startingSchema);

		// call under test — columns added to the source are pulled into the grid.
		handler.onNewSourceItem(new ColumnSourceItem().setColumnName("four"));
		handler.onNewSourceItem(new ColumnSourceItem().setColumnName("five"));

		List<Column> expectedFinal = new ArrayList<>(startingSchema);
		expectedFinal.add(new Column().setName("four").setVectorIndex(3));
		expectedFinal.add(new Column().setName("five").setVectorIndex(4));
		assertEquals(expectedFinal, handler.getFinalSchema());

		verify(mockIntendedChangePublisher).publish(new AddColumnChange(columnOrderArrId, columnOrderArrId, 3L));
		verify(mockIntendedChangePublisher).publish(new AddColumnChange(columnOrderArrId, columnOrderArrId, 4L));

		handler.close();
		verify(mockIntendedChangePublisher).publish(new UpdateColumnNamesChange(columnNamesVecId,
				Map.of(2, "one", 1, "two", 0, "three", 3, "four", 4, "five")));

		verifyNoMoreInteractions(mockIntendedChangePublisher, mockSourceWriter);
	}

	@Test
	public void testOnNewSourceItemWithEmptyStart() throws Exception {
		setupHandler(Collections.emptyList());

		// call under test
		handler.onNewSourceItem(new ColumnSourceItem().setColumnName("four"));
		handler.onNewSourceItem(new ColumnSourceItem().setColumnName("five"));

		List<Column> expectedFinal = new ArrayList<>();
		expectedFinal.add(new Column().setName("four").setVectorIndex(0));
		expectedFinal.add(new Column().setName("five").setVectorIndex(1));
		assertEquals(expectedFinal, handler.getFinalSchema());

		verify(mockIntendedChangePublisher).publish(new AddColumnChange(columnOrderArrId, columnOrderArrId, 0L));
		verify(mockIntendedChangePublisher).publish(new AddColumnChange(columnOrderArrId, columnOrderArrId, 1L));

		handler.close();
		// second close should not send a second change.
		handler.close();
		verify(mockIntendedChangePublisher)
				.publish(new UpdateColumnNamesChange(columnNamesVecId, Map.of(0, "four", 1, "five")));

		verifyNoMoreInteractions(mockIntendedChangePublisher, mockSourceWriter);
	}

}
