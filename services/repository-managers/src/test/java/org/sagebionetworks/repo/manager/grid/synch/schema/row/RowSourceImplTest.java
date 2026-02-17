package org.sagebionetworks.repo.manager.grid.synch.schema.row;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.SynapseRow;
import org.sagebionetworks.repo.manager.grid.synch.handler.SourceHandler;
import org.sagebionetworks.repo.manager.grid.synch.io.RowHeader;
import org.sagebionetworks.repo.manager.grid.synch.io.RowReader;
import org.sagebionetworks.repo.manager.grid.synch.io.SynchRow;
import org.sagebionetworks.repo.manager.grid.synch.row.CellCopyItem;
import org.sagebionetworks.repo.manager.grid.synch.row.RowCopyItemImpl;
import org.sagebionetworks.repo.manager.grid.synch.row.RowSourceImpl;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;

@ExtendWith(MockitoExtension.class)
public class RowSourceImplTest {

	@Mock
	private SourceHandler mockSourceHandler;
	@Mock
	private RowReader mockRowReader;
	@Mock
	private RowHeader mockRowHeader;

	@InjectMocks
	private RowSourceImpl source;

	@Test
	public void testAddItem() {

		ConValue c1 = new ConValue(ConType.STRING, "one");
		ConValue c2 = new ConValue(ConType.BOOLEAN, true);
		RowCopyItemImpl copyItem = new RowCopyItemImpl().setCells(
				List.of(new CellCopyItem().setName("a").setValue(c1), new CellCopyItem().setName("b").setValue(c2)));
		when(mockSourceHandler.getRowKey(copyItem)).thenReturn("theKey");
		// call under test
		source.addItem(copyItem);

		verify(mockSourceHandler).addNewRowToSource(new SynchRow(new TreeMap<>(Map.of("a", c1, "b", c2)), "theKey"));

		verifyNoMoreInteractionsWithAllMocks();
	}

	private void verifyNoMoreInteractionsWithAllMocks() {
		verifyNoMoreInteractions(mockRowHeader, mockSourceHandler, mockRowHeader);
	}

	@Test
	public void testAddItemWithSynapseRow() {

		ConValue c1 = new ConValue(ConType.STRING, "one");
		ConValue c2 = new ConValue(ConType.BOOLEAN, true);
		SynapseRow synRow = new SynapseRow().setRowId(123L).setVersionNumber(0L).setEtag("e1");
		RowCopyItemImpl copyItem = new RowCopyItemImpl().setCells(
				List.of(new CellCopyItem().setName("a").setValue(c1), new CellCopyItem().setName("b").setValue(c2)))
				.setSynapseRow(synRow);
		when(mockSourceHandler.getRowKey(copyItem)).thenReturn("theKey");
		// call under test
		source.addItem(copyItem);

		verify(mockSourceHandler)
				.addNewRowToSource(new SynchRow(new TreeMap<>(Map.of("a", c1, "b", c2)), "theKey", synRow));

		verifyNoMoreInteractionsWithAllMocks();
	}

	@Test
	public void testRemoveItem() {
		ConValue c1 = new ConValue(ConType.STRING, "one");
		ConValue c2 = new ConValue(ConType.BOOLEAN, true);
		SynapseRow synRow = new SynapseRow().setRowId(123L).setVersionNumber(0L).setEtag("e1");
		SynchRow synch = new SynchRow(new TreeMap<>(Map.of("a", c1, "b", c2)), "theKey", synRow);
		when(mockRowHeader.fetchRow()).thenReturn(synch);
		// call under test
		source.removeItem(mockRowHeader);

		verify(mockSourceHandler).removeRow(synch);

		verifyNoMoreInteractionsWithAllMocks();
	}

	@Test
	public void testRemoveItemWithNullSynapseRow() {
		ConValue c1 = new ConValue(ConType.STRING, "one");
		ConValue c2 = new ConValue(ConType.BOOLEAN, true);
		SynapseRow synRow = null;
		SynchRow synch = new SynchRow(new TreeMap<>(Map.of("a", c1, "b", c2)), "theKey", synRow);
		when(mockRowHeader.fetchRow()).thenReturn(synch);
		// call under test
		source.removeItem(mockRowHeader);

		verify(mockSourceHandler).removeRow(synch);

		verifyNoMoreInteractionsWithAllMocks();
	}

	@Test
	public void testMatches() {
		String key = "123";
		ConValue c1 = new ConValue(ConType.STRING, "one");
		ConValue c2 = new ConValue(ConType.BOOLEAN, true);
		SynapseRow synRow = new SynapseRow().setRowId(123L).setVersionNumber(0L).setEtag("e1");
		SynchRow synch = new SynchRow(new TreeMap<>(Map.of("a", c1, "b", c2)), key, synRow);

		RowCopyItemImpl copyItem = new RowCopyItemImpl().setCells(
				List.of(new CellCopyItem().setName("a").setValue(c1), new CellCopyItem().setName("b").setValue(c2)))
				.setSynapseRow(synRow);

		when(mockRowHeader.getKey()).thenReturn(key);
		when(mockRowHeader.getHash()).thenReturn(synch.getHash());

		// call under test
		assertTrue(source.matches(copyItem, mockRowHeader));

		verifyNoMoreInteractionsWithAllMocks();
	}

	@Test
	public void testMatchesWithNewEtag() {
		String key = "123";
		ConValue c1 = new ConValue(ConType.STRING, "one");
		ConValue c2 = new ConValue(ConType.BOOLEAN, true);
		SynapseRow synRow = new SynapseRow().setRowId(123L).setVersionNumber(0L).setEtag("e1");
		SynchRow synch = new SynchRow(new TreeMap<>(Map.of("a", c1, "b", c2)), key, synRow);

		RowCopyItemImpl copyItem = new RowCopyItemImpl()
				.setCells(List.of(new CellCopyItem().setName("a").setValue(c1),
						new CellCopyItem().setName("b").setValue(c2)))
				.setSynapseRow(new SynapseRow().setRowId(123L).setVersionNumber(0L).setEtag("e2"));

		when(mockRowHeader.getKey()).thenReturn(key);
		when(mockRowHeader.getHash()).thenReturn(synch.getHash());

		// call under test
		assertFalse(source.matches(copyItem, mockRowHeader));

		verifyNoMoreInteractionsWithAllMocks();
	}
}
