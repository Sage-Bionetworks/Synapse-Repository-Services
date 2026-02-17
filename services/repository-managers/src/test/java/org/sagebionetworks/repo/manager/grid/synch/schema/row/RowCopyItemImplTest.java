package org.sagebionetworks.repo.manager.grid.synch.schema.row;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.SynapseRow;
import org.sagebionetworks.repo.manager.grid.synch.row.CellCopyItem;
import org.sagebionetworks.repo.manager.grid.synch.row.RowCopyItemImpl;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

public class RowCopyItemImplTest {

	private Long internalReplicaId;

	@BeforeEach
	public void before() {
		internalReplicaId = 55L;
	}

	@Test
	public void testWasChangedByUser() {
		RowCopyItemImpl row = new RowCopyItemImpl().setSynapseRow(new SynapseRow().setRowId(111L))
				.setRgaNodeId(new LogicalTimestamp().setReplicaId(internalReplicaId).setSequenceNumber(6L))
				.setVectorNodeId(new LogicalTimestamp().setReplicaId(internalReplicaId).setSequenceNumber(7L))
				.setCells(List.of(
						new CellCopyItem().setName("a").setValue(new ConValue(ConType.STRING, "foo"))
								.setWasChangedByUser(false),
						new CellCopyItem().setName("b").setValue(new ConValue(ConType.STRING, "foo"))
								.setWasChangedByUser(false)));

		assertFalse(row.wasChangedByUser());
	}

	@Test
	public void testWasChangedByUserWithOneChanged() {
		RowCopyItemImpl row = new RowCopyItemImpl().setSynapseRow(new SynapseRow().setRowId(111L))
				.setRgaNodeId(new LogicalTimestamp().setReplicaId(internalReplicaId).setSequenceNumber(6L))
				.setVectorNodeId(new LogicalTimestamp().setReplicaId(internalReplicaId).setSequenceNumber(7L))
				.setCells(List.of(
						new CellCopyItem().setName("a").setValue(new ConValue(ConType.STRING, "foo"))
								.setWasChangedByUser(false),
						new CellCopyItem().setName("b").setValue(new ConValue(ConType.STRING, "foo"))
								.setWasChangedByUser(true)));

		assertTrue(row.wasChangedByUser());
	}

	@Test
	public void testWasChangedByUserWithEmpty() {
		RowCopyItemImpl row = new RowCopyItemImpl().setSynapseRow(new SynapseRow().setRowId(111L))
				.setRgaNodeId(new LogicalTimestamp().setReplicaId(internalReplicaId).setSequenceNumber(6L))
				.setVectorNodeId(new LogicalTimestamp().setReplicaId(internalReplicaId).setSequenceNumber(7L))
				.setCells(List.of());

		assertFalse(row.wasChangedByUser());
	}
}
