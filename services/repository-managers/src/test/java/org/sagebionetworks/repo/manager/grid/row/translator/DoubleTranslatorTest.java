package org.sagebionetworks.repo.manager.grid.row.translator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;

public class DoubleTranslatorTest {

	@Test
	public void testTranslate() {
		// call under test
		ConValue con = new DoubleTranslator().translate("1.1");
		assertEquals(new ConValue(ConType.DOUBLE, 1.1), con);
	}
}
