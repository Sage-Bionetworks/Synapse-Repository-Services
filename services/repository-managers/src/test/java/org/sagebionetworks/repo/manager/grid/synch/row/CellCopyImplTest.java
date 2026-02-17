package org.sagebionetworks.repo.manager.grid.synch.row;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;

public class CellCopyImplTest {

	@Test
	public void testGetUserDeletedCells() {

		RowCopyItemImpl row = new RowCopyItemImpl().setCells(List.of(
				// a
				new CellCopyItem().setName("a").setValue(new ConValue(ConType.STRING, "one")).setWasChangedByUser(true),
				// b
				new CellCopyItem().setName("b").setValue(new ConValue(ConType.UNDEFINED, null))
						.setWasChangedByUser(true),
				// c
				new CellCopyItem().setName("c").setValue(new ConValue(ConType.UNDEFINED, null))
						.setWasChangedByUser(false),
				// d
				new CellCopyItem().setName("d").setValue(new ConValue(ConType.NULL, null)).setWasChangedByUser(true),
				// e
				new CellCopyItem().setName("e").setValue(new ConValue(ConType.NULL, null)).setWasChangedByUser(false),
				// f
				new CellCopyItem().setName("f").setValue(null).setWasChangedByUser(true)

		));

		// call under test
		Set<String> result = CellCopyImpl.getUserDeletedCells(row);
		Set<String> expected = Set.of("b", "d");
		assertEquals(result, expected);
	}

}
