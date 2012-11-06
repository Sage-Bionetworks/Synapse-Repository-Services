package org.sagebionetworks.dynamo.dao;

import java.util.Date;

import junit.framework.Assert;

import org.junit.Test;

public class NodeLineagePairTest {

	@Test
	public void testRoot() {
		NodeLineagePair pair = new NodeLineagePair("123", new Date(), "123", new Date(), 0, 0);
		Assert.assertNotNull(pair);
		Assert.assertEquals(0, pair.getAncestorDepth());
		Assert.assertEquals(0, pair.getAncestorDepth());
		Assert.assertNotNull(pair.getDescendant2Ancestor());
		Assert.assertNotNull(pair.getAncestor2Descendant());
		Assert.assertEquals(NodeLineage.ROOT_ID, pair.getAncestorId());
		Assert.assertEquals("123", pair.getDescendantId());
	}
}
