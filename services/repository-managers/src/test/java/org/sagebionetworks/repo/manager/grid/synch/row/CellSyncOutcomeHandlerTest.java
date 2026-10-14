package org.sagebionetworks.repo.manager.grid.synch.row;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;

public class CellSyncOutcomeHandlerTest {

	private static CellCopyItem copyCell(String name, ConValue value, boolean changed) {
		return new CellCopyItem().setName(name).setValue(value).setWasChangedByUser(changed);
	}

	private static CellSourceItem sourceCell(String name, ConValue value) {
		return new CellSourceItem().setColumnName(name).setValue(value);
	}

	@Test
	public void testGetUserDeletedCells() {
		RowCopyItemImpl row = new RowCopyItemImpl().setCells(List.of(
				new CellCopyItem().setName("a").setValue(new ConValue(ConType.STRING, "one")).setWasChangedByUser(true),
				new CellCopyItem().setName("b").setValue(new ConValue(ConType.UNDEFINED, null))
						.setWasChangedByUser(true),
				new CellCopyItem().setName("c").setValue(new ConValue(ConType.UNDEFINED, null))
						.setWasChangedByUser(false),
				new CellCopyItem().setName("d").setValue(new ConValue(ConType.NULL, null)).setWasChangedByUser(true),
				new CellCopyItem().setName("e").setValue(new ConValue(ConType.NULL, null)).setWasChangedByUser(false),
				new CellCopyItem().setName("f").setValue(null).setWasChangedByUser(true)));

		// call under test
		Set<String> result = CellSyncOutcomeHandler.getUserDeletedCells(row);
		assertEquals(Set.of("b", "d"), result);
	}

	@Test
	public void testOnCopyAndSourceConflictUserWins() {
		ConValue userValue = new ConValue(ConType.STRING, "user");
		ConValue sourceValue = new ConValue(ConType.STRING, "source");
		CellCopyItem copy = copyCell("a", userValue, true);
		CellSyncOutcomeHandler handler = new CellSyncOutcomeHandler(List.of(copy));

		// call under test
		handler.onCopyAndSourceConflict(copy, sourceCell("a", sourceValue));

		assertEquals(Map.of("a", userValue), handler.getMergedCells());
		assertEquals(Map.of("a", userValue), handler.getUserChangedCells());
		assertEquals(Set.of("a"), handler.getUserWonCells());
	}

	@Test
	public void testOnCopyAndSourceConflictSourceWins() {
		ConValue copyValue = new ConValue(ConType.STRING, "copy");
		ConValue sourceValue = new ConValue(ConType.STRING, "source");
		CellCopyItem copy = copyCell("a", copyValue, false);
		CellSyncOutcomeHandler handler = new CellSyncOutcomeHandler(List.of(copy));

		// call under test — the user did not change the cell, so the source value wins.
		handler.onCopyAndSourceConflict(copy, sourceCell("a", sourceValue));

		assertEquals(Map.of("a", sourceValue), handler.getMergedCells());
		assertEquals(Map.of(), handler.getUserChangedCells());
		assertEquals(Set.of(), handler.getUserWonCells());
	}

	@Test
	public void testOnCopyAndSourceMatch() {
		ConValue value = new ConValue(ConType.STRING, "x");
		CellCopyItem copy = copyCell("a", value, false);
		CellSyncOutcomeHandler handler = new CellSyncOutcomeHandler(List.of(copy));

		// call under test — the merged cell already holds the copy value.
		handler.onCopyAndSourceMatch(copy, sourceCell("a", value));

		assertEquals(Map.of("a", value), handler.getMergedCells());
		assertEquals(Map.of(), handler.getUserChangedCells());
	}

	@Test
	public void testOnNewCopyItem() {
		ConValue value = new ConValue(ConType.STRING, "x");
		CellCopyItem copy = copyCell("a", value, true);
		CellSyncOutcomeHandler handler = new CellSyncOutcomeHandler(List.of(copy));

		// call under test — a user-added cell is pushed and stays in the merged result.
		handler.onNewCopyItem(copy, "a");

		assertEquals(Map.of("a", value), handler.getMergedCells());
		assertEquals(Map.of("a", value), handler.getUserChangedCells());
	}

	@Test
	public void testOnDeletedFromSource() {
		ConValue value = new ConValue(ConType.STRING, "x");
		CellCopyItem copy = copyCell("a", value, false);
		CellSyncOutcomeHandler handler = new CellSyncOutcomeHandler(List.of(copy));

		// call under test — a cell removed from the source is dropped from the merged result.
		handler.onDeletedFromSource(copy);

		assertEquals(Map.of(), handler.getMergedCells());
	}

	@Test
	public void testOnDeletedFromCopy() {
		CellSyncOutcomeHandler handler = new CellSyncOutcomeHandler(List.of());

		// call under test — the user cleared the cell, so a null is pushed to clear the source.
		handler.onDeletedFromCopy(sourceCell("a", new ConValue(ConType.STRING, "source")));

		assertEquals(Map.of(), handler.getMergedCells());
		java.util.Map<String, ConValue> expected = new java.util.HashMap<>();
		expected.put("a", null);
		assertEquals(expected, handler.getUserChangedCells());
	}

	@Test
	public void testOnNewSourceItem() {
		ConValue sourceValue = new ConValue(ConType.STRING, "source");
		CellSyncOutcomeHandler handler = new CellSyncOutcomeHandler(List.of());

		// call under test — a cell added in the source is pulled into the merged result.
		handler.onNewSourceItem(sourceCell("a", sourceValue));

		assertEquals(Map.of("a", sourceValue), handler.getMergedCells());
	}

}
