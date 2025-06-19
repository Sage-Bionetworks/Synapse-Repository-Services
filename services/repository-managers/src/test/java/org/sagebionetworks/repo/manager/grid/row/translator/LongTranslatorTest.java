package org.sagebionetworks.repo.manager.grid.row.translator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.grid.patch.ConType;
import org.sagebionetworks.repo.model.grid.patch.ConValue;

public class LongTranslatorTest {

	@Test
	public void testTranslate() {
		// call under test
		ConValue con = new LongTranslator().translate("-123");
		assertEquals(new ConValue(ConType.LONG, -123L), con);
	}

}
