package org.sagebionetworks.dynamo.dao.tree;

import java.util.Date;

import junit.framework.Assert;

import org.junit.Test;
import org.sagebionetworks.dynamo.dao.tree.DboNodeLineage;
import org.sagebionetworks.dynamo.dao.tree.LineageType;
import org.sagebionetworks.dynamo.dao.tree.NodeLineage;
import org.sagebionetworks.dynamo.dao.tree.NodeLineagePair;

public class NodeLineagePairTest {

	@Test
	public void testFromDbo() {

		String descId = "48920";
		LineageType lineageType = LineageType.ANCESTOR;
		int distance = 3;
		String ancId = "10238";
		Date timestamp = new Date();
		Long version = 5L;
		NodeLineage lineage = new NodeLineage(descId, lineageType, distance, ancId, timestamp, version);
		DboNodeLineage dbo = lineage.createDbo();
		int depth = 1;
		NodeLineagePair pair = new NodeLineagePair(dbo, depth);
		Assert.assertEquals(ancId, pair.getAncestorId());
		Assert.assertEquals(descId, pair.getDescendantId());
		Assert.assertEquals(depth, pair.getAncestorDepth());
		Assert.assertEquals(distance, pair.getDistance());

		Assert.assertNotNull(pair.getDescendant2Ancestor());
		DboNodeLineage d2aDbo = pair.getDescendant2Ancestor();
		Assert.assertEquals(DboNodeLineage.createHashKey(descId, LineageType.ANCESTOR), d2aDbo.getHashKey());
		Assert.assertEquals(DboNodeLineage.createRangeKey(distance, ancId), d2aDbo.getRangeKey());
		Assert.assertEquals(timestamp, d2aDbo.getTimestamp());
		Assert.assertEquals(version, d2aDbo.getVersion());
		Assert.assertNotNull(pair.getAncestor2Descendant());
		DboNodeLineage a2dDbo = pair.getAncestor2Descendant();
		Assert.assertEquals(DboNodeLineage.createHashKey(ancId, LineageType.DESCENDANT), a2dDbo.getHashKey());
		Assert.assertEquals(DboNodeLineage.createRangeKey(distance, descId), a2dDbo.getRangeKey());
		Assert.assertEquals(timestamp, a2dDbo.getTimestamp());
		Assert.assertEquals(version, a2dDbo.getVersion());
		Assert.assertEquals(ancId, pair.getAncestorId());
		Assert.assertEquals(descId, pair.getDescendantId());

		ancId = "48920";
		lineageType = LineageType.DESCENDANT;
		distance = 5;
		descId = "10238";
		timestamp = new Date();
		version = 2L;
		lineage = new NodeLineage(ancId, lineageType, distance, descId, timestamp, version);
		dbo = lineage.createDbo();
		depth = 9;
		pair = new NodeLineagePair(dbo, depth);
		Assert.assertEquals(ancId, pair.getAncestorId());
		Assert.assertEquals(descId, pair.getDescendantId());
		Assert.assertEquals(depth, pair.getAncestorDepth());
		Assert.assertEquals(distance, pair.getDistance());

		Assert.assertNotNull(pair.getDescendant2Ancestor());
		d2aDbo = pair.getDescendant2Ancestor();
		Assert.assertEquals(DboNodeLineage.createHashKey(descId, LineageType.ANCESTOR), d2aDbo.getHashKey());
		Assert.assertEquals(DboNodeLineage.createRangeKey(distance, ancId), d2aDbo.getRangeKey());
		Assert.assertEquals(timestamp, d2aDbo.getTimestamp());
		Assert.assertEquals(version, d2aDbo.getVersion());
		Assert.assertNotNull(pair.getAncestor2Descendant());
		a2dDbo = pair.getAncestor2Descendant();
		Assert.assertEquals(DboNodeLineage.createHashKey(ancId, LineageType.DESCENDANT), a2dDbo.getHashKey());
		Assert.assertEquals(DboNodeLineage.createRangeKey(distance, descId), a2dDbo.getRangeKey());
		Assert.assertEquals(timestamp, a2dDbo.getTimestamp());
		Assert.assertEquals(version, a2dDbo.getVersion());
		Assert.assertEquals(ancId, pair.getAncestorId());
		Assert.assertEquals(descId, pair.getDescendantId());
	}

	@Test
	public void testConstructor() {
	
		String ancId = "123";
		String descId = "321";
		Date timestamp = new Date();
		int depth = 3;
		int distance = 96;
		NodeLineagePair pair = new NodeLineagePair(ancId, descId, depth, distance, timestamp);
		Assert.assertNotNull(pair);
		Assert.assertEquals(depth, pair.getAncestorDepth());
		Assert.assertEquals(distance, pair.getDistance());

		Assert.assertNotNull(pair.getDescendant2Ancestor());
		DboNodeLineage d2aDbo = pair.getDescendant2Ancestor();
		Assert.assertEquals(DboNodeLineage.createHashKey(descId, LineageType.ANCESTOR), d2aDbo.getHashKey());
		Assert.assertEquals(DboNodeLineage.createRangeKey(distance, ancId), d2aDbo.getRangeKey());
		Assert.assertEquals(timestamp, d2aDbo.getTimestamp());
		Assert.assertNull(d2aDbo.getVersion());

		Assert.assertNotNull(pair.getAncestor2Descendant());
		DboNodeLineage a2dDbo = pair.getAncestor2Descendant();
		Assert.assertEquals(DboNodeLineage.createHashKey(ancId, LineageType.DESCENDANT), a2dDbo.getHashKey());
		Assert.assertEquals(DboNodeLineage.createRangeKey(distance, descId), a2dDbo.getRangeKey());
		Assert.assertEquals(timestamp, a2dDbo.getTimestamp());
		Assert.assertNull(a2dDbo.getVersion());
		Assert.assertEquals(ancId, pair.getAncestorId());
		Assert.assertEquals(descId, pair.getDescendantId());
	}

	@Test
	public void testRoot() {
		NodeLineagePair pair = new NodeLineagePair(DboNodeLineage.ROOT, "123", 0, 1, new Date());
		Assert.assertNotNull(pair);
		Assert.assertEquals(0, pair.getAncestorDepth());
		Assert.assertEquals(1, pair.getDistance());
		Assert.assertNotNull(pair.getDescendant2Ancestor());
		Assert.assertNotNull(pair.getAncestor2Descendant());
		Assert.assertEquals(DboNodeLineage.ROOT, pair.getAncestorId());
		Assert.assertEquals("123", pair.getDescendantId());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testRootIllegalArgumentException() {
		NodeLineagePair pair = new NodeLineagePair("123", "123", 0, 1, new Date());
		Assert.assertNull(pair);
	}
}
