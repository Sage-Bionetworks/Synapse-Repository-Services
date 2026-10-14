package org.sagebionetworks.repo.manager.grid.synch.row;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItem;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItemReference;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;

@ExtendWith(MockitoExtension.class)
public class RowSyncRulesTest {

	@Mock
	private SourceHandler mockSourceHandler;
	@Mock
	private RowSourceItemReference mockRowHeader;

	@InjectMocks
	private RowSyncRules rules;

	@Test
	public void testGetKey() {
		RowCopyItemImpl copyItem = new RowCopyItemImpl().setSynapseRow(new SynapseRow().setRowId(1L));
		when(mockSourceHandler.getRowKey(copyItem)).thenReturn("theKey");

		// call under test
		assertTrue("theKey".equals(rules.getKey(copyItem)));
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
		assertTrue(rules.matches(copyItem, mockRowHeader));
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
		assertFalse(rules.matches(copyItem, mockRowHeader));
	}

	@Test
	public void testIsExcludedFromMatching() {
		RowCopyItemImpl copyItem = new RowCopyItemImpl().setSynapseRow(new SynapseRow().setRowId(1L));
		when(mockSourceHandler.isUnmatchableCopyRow(copyItem, "theKey")).thenReturn(true);

		// call under test
		assertTrue(rules.isExcludedFromMatching(copyItem, "theKey"));
	}

	@Test
	public void testWasDeletedByUserWhenInBaselineAndUnchanged() {
		when(mockRowHeader.getKey()).thenReturn("k1");
		when(mockSourceHandler.wasInSyncedBaseline("k1")).thenReturn(true);
		when(mockSourceHandler.changedSinceBaseline("k1")).thenReturn(false);

		// call under test — a row absent from the grid was deleted by the user iff its
		// key was in the synced baseline AND the source row has not changed since then.
		assertTrue(rules.wasDeletedByUser(mockRowHeader));
	}

	@Test
	public void testWasDeletedByUserWhenNotInBaseline() {
		when(mockRowHeader.getKey()).thenReturn("k1");
		when(mockSourceHandler.wasInSyncedBaseline("k1")).thenReturn(false);

		// call under test — the key was never in the baseline, so its absence is a
		// source-side addition, not a user deletion.
		assertFalse(rules.wasDeletedByUser(mockRowHeader));
	}

	@Test
	public void testWasDeletedByUserWhenSourceChangedSinceBaseline() {
		when(mockRowHeader.getKey()).thenReturn("k1");
		when(mockSourceHandler.wasInSyncedBaseline("k1")).thenReturn(true);
		when(mockSourceHandler.changedSinceBaseline("k1")).thenReturn(true);

		// call under test — the user deleted this row, but the source row changed since
		// the synced revision, so it is re-imported rather than treated as a deletion.
		assertFalse(rules.wasDeletedByUser(mockRowHeader));
	}

}
