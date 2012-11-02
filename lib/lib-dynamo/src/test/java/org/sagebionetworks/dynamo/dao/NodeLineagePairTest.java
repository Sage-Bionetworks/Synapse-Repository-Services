package org.sagebionetworks.dynamo.dao;

import java.util.Date;

import junit.framework.Assert;

import org.junit.Test;

public class NodeLineagePairTest {

	@Test
	public void testRoot() {
		NodeLineagePair pair = new NodeLineagePair("root", new Date(), "root", new Date(), 0, 0);
		Assert.assertNotNull(pair);
		Assert.assertEquals(0, pair.getAncestorDepth());
		Assert.assertEquals(0, pair.getAncestorDepth());
		Assert.assertNotNull(pair.getDescendant2Ancestor());
		Assert.assertNull(pair.getAncestor2Descendant());
	}
}
