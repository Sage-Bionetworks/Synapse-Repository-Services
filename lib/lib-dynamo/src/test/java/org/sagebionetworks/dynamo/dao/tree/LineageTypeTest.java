package org.sagebionetworks.dynamo.dao.tree;

import org.junit.Assert;
import org.junit.Test;

public class LineageTypeTest {

	@Test
	public void testFromString() {
		Assert.assertEquals(LineageType.ANCESTOR, LineageType.fromString(LineageType.ANCESTOR.getType()));
		Assert.assertEquals(LineageType.DESCENDANT, LineageType.fromString(LineageType.DESCENDANT.getType()));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testFromStringIllegalArgumentException() {
		LineageType.fromString("some invalid lineage type");
	}
}
