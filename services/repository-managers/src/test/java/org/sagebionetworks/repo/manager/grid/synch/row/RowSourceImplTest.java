package org.sagebionetworks.repo.manager.grid.synch.row;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockitoSession;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.internal.replica.model.SynapseRow;
import org.sagebionetworks.repo.manager.grid.synch.handler.SourceHandler;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItem;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItemReader;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItemReference;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;

@ExtendWith(MockitoExtension.class)
public class RowSourceImplTest {

	@Mock
	private SourceHandler mockSourceHandler;
	@Mock
	private RowSourceItemReader mockRowReader;
	@Mock
	private RowSourceItemReference mockRowHeader;

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

		verify(mockSourceHandler)
				.addNewRowToSource(new RowSourceItem(new TreeMap<>(Map.of("a", c1, "b", c2)), "theKey"));

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
				.addNewRowToSource(new RowSourceItem(new TreeMap<>(Map.of("a", c1, "b", c2)), "theKey", synRow));

		verifyNoMoreInteractionsWithAllMocks();
	}

	@Test
	public void testRemoveItem() {
		ConValue c1 = new ConValue(ConType.STRING, "one");
		ConValue c2 = new ConValue(ConType.BOOLEAN, true);
		SynapseRow synRow = new SynapseRow().setRowId(123L).setVersionNumber(0L).setEtag("e1");
		RowSourceItem synch = new RowSourceItem(new TreeMap<>(Map.of("a", c1, "b", c2)), "theKey", synRow);
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
		RowSourceItem synch = new RowSourceItem(new TreeMap<>(Map.of("a", c1, "b", c2)), "theKey", synRow);
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
		RowSourceItem synch = new RowSourceItem(new TreeMap<>(Map.of("a", c1, "b", c2)), key, synRow);

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
		RowSourceItem synch = new RowSourceItem(new TreeMap<>(Map.of("a", c1, "b", c2)), key, synRow);

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

	@Test
	public void testAddItemConsume() {

		when(mockRowReader.consumeRow("a")).thenReturn(Optional.of(mockRowHeader));
		when(mockRowReader.consumeRow("b")).thenReturn(Optional.empty());

		// call under test
		assertEquals(Optional.of(mockRowHeader), source.consume("a"));
		assertEquals(Optional.empty(), source.consume("b"));

	}

	@Test
	public void testStreamRemaining() {

		List<RowSourceItemReference> input = List.of(Mockito.mock(RowSourceItemReference.class),
				Mockito.mock(RowSourceItemReference.class));
		when(mockRowReader.remainingRows()).thenReturn(input.iterator());

		// call under test
		List<RowSourceItemReference> refs = source.streamRemaining().collect(Collectors.toList());
		assertEquals(input, refs);

	}

	@Test
	public void testIsItemAdditionSupported() {
		when(mockSourceHandler.canAddRemoveRows()).thenReturn(false);

		// call under test
		assertFalse(source.isItemAdditionSupported());

		verify(mockSourceHandler).canAddRemoveRows();
		verifyNoMoreInteractionsWithAllMocks();
	}

	@Test
	public void testIsItemRemovalSupported() {
		when(mockSourceHandler.canAddRemoveRows()).thenReturn(false);

		// call under test
		assertFalse(source.isItemRemovalSupported());

		verify(mockSourceHandler).canAddRemoveRows();
		verifyNoMoreInteractionsWithAllMocks();
	}
}
