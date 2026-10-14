package org.sagebionetworks.repo.manager.grid.synch.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.SynapseRow;
import org.sagebionetworks.repo.manager.grid.synch.row.CellCopyItem;
import org.sagebionetworks.repo.manager.grid.synch.row.RowCopyItem;
import org.sagebionetworks.repo.manager.grid.synch.row.RowCopyItemImpl;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.FileProvider;
import org.sagebionetworks.util.FileProviderImpl;

@ExtendWith(MockitoExtension.class)
public class CopyRowSnapshotTest {

	@Mock
	private FileProvider mockFileProvider;

	private final FileProvider fileProvider = new FileProviderImpl();

	@Test
	public void testCaptureAndReadRoundTrip() {
		ConValue a = new ConValue(ConType.STRING, "alpha");
		ConValue b = new ConValue(ConType.LONG, 42L);

		RowCopyItemImpl row1 = new RowCopyItemImpl()
				.setRgaNodeId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(10L))
				.setVectorNodeId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(11L))
				.setMetadataNodeId(new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(12L))
				.setSynapseRow(new SynapseRow().setRowId(99L).setVersionNumber(0L).setEtag("etag1"))
				.setCells(List.of(
						new CellCopyItem().setName("a").setValue(a).setWasChangedByUser(true),
						new CellCopyItem().setName("b").setValue(b).setWasChangedByUser(false)));

		// second row exercises null metadata, null synapseRow
		RowCopyItemImpl row2 = new RowCopyItemImpl()
				.setRgaNodeId(new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(20L))
				.setVectorNodeId(new LogicalTimestamp().setReplicaId(2L).setSequenceNumber(21L))
				.setCells(List.of(new CellCopyItem().setName("a").setValue(a).setWasChangedByUser(false)));

		List<RowCopyItem> result;
		try (CopyRowSnapshot snapshot = CopyRowSnapshot.capture(List.<RowCopyItem>of(row1, row2).iterator(), fileProvider)) {
			// read twice to prove the snapshot is re-readable
			assertEquals(2, count(snapshot));
			result = drain(snapshot);
		}

		assertEquals(2, result.size());

		RowCopyItem r1 = result.get(0);
		assertEquals("1.10", r1.getRgaNodeId().toCompact());
		assertEquals("1.11", r1.getVectorNodeId().toCompact());
		assertEquals("1.12", r1.getMetadataNodeId().toCompact());
		assertTrue(r1.getSynapseRow().isPresent());
		assertEquals(99L, r1.getSynapseRow().get().getRowId());
		assertEquals(2, r1.getCells().size());
		assertEquals("a", r1.getCells().get(0).getName());
		assertEquals(a, r1.getCells().get(0).getValue());
		assertTrue(r1.getCells().get(0).wasChangedByUser());
		assertEquals("b", r1.getCells().get(1).getName());
		assertEquals(b, r1.getCells().get(1).getValue());
		assertFalse(r1.getCells().get(1).wasChangedByUser());

		RowCopyItem r2 = result.get(1);
		assertNull(r2.getMetadataNodeId());
		assertFalse(r2.getSynapseRow().isPresent());
		assertEquals(1, r2.getCells().size());
		assertEquals(a, r2.getCells().get(0).getValue());
	}

	@Test
	public void testCaptureEmpty() {
		try (CopyRowSnapshot snapshot = CopyRowSnapshot.capture(List.<RowCopyItem>of().iterator(), fileProvider)) {
			assertEquals(0, count(snapshot));
		}
	}

	@Test
	public void captureWrapsIoException() throws IOException {
		IOException cause = new IOException("disk full");
		when(mockFileProvider.createTempFile(anyString(), anyString())).thenThrow(cause);

		RuntimeException result = assertThrows(RuntimeException.class,
				// call under test
				() -> CopyRowSnapshot.capture(List.<RowCopyItem>of().iterator(), mockFileProvider));

		assertEquals(cause, result.getCause());
	}

	private int count(CopyRowSnapshot snapshot) {
		int n = 0;
		var it = snapshot.read();
		while (it.hasNext()) {
			it.next();
			n++;
		}
		return n;
	}

	private List<RowCopyItem> drain(CopyRowSnapshot snapshot) {
		List<RowCopyItem> list = new java.util.ArrayList<>();
		var it = snapshot.read();
		while (it.hasNext()) {
			list.add(it.next());
		}
		return list;
	}
}
