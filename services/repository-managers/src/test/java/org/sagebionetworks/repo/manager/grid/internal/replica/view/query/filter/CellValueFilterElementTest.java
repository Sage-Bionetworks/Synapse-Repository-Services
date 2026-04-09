package org.sagebionetworks.repo.manager.grid.internal.replica.view.query.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.query.CellValueFilter;
import org.sagebionetworks.repo.model.grid.query.CellValueOperator;

public class CellValueFilterElementTest {

	/**
	 * This is case that failed for PLFM-9309.
	 */
	@Test
	public void testTranslateWithNullValue() {
		CellValueFilter toClone = new CellValueFilter().setColumnName("c1").setOperator(CellValueOperator.IS_NOT_NULL);
		// call under test
		CellValueFilterElement clone = new CellValueFilterElement(toClone);

		assertEquals(new CellValueFilterElement().setColumnName("c1").setOperator(CellValueOperatorElement.IS_NOT_NULL),
				clone);
	}

	@Test
	public void testTranslate() {
		CellValueFilter toClone = new CellValueFilter().setColumnName("c1").setOperator(CellValueOperator.EQUALS)
				.setValue("one");
		// call under test
		CellValueFilterElement clone = new CellValueFilterElement(toClone);

		assertEquals(new CellValueFilterElement().setColumnName("c1").setOperator(CellValueOperatorElement.EQUALS)
				.setValue("one"), clone);
	}

	@Test
	public void testTranslateWithNullFilter() {
		CellValueFilter toClone = null;

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new CellValueFilterElement(toClone);
		}).getMessage();
		assertEquals("filter is required.", message);

	}

	@Test
	public void testTranslateWithNullOperator() {
		CellValueFilter toClone = new CellValueFilter().setColumnName("c1").setOperator(null);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			new CellValueFilterElement(toClone);
		}).getMessage();
		assertEquals("filter.operator is required.", message);
	}

}
