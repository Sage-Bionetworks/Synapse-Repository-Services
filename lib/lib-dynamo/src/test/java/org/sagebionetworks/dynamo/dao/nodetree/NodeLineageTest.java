package org.sagebionetworks.dynamo.dao.nodetree;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

public class NodeLineageTest {

	@Test(expected=IllegalArgumentException.class)
	public void testConstructorDboIllegalArgumentException1() {
		DboNodeLineage dbo = new DboNodeLineage();
		dbo.setRangeKey("003#98931");
		NodeLineage lineage = new NodeLineage(dbo);
		Assert.assertNull(lineage);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testConstructorDboIllegalArgumentException2() {
		DboNodeLineage dbo = new DboNodeLineage();
		dbo.setHashKey("29201#D");
		NodeLineage lineage = new NodeLineage(dbo);
		Assert.assertNull(lineage);
	}

	@Test
	public void testConstructorDbo() {
		DboNodeLineage dbo = new DboNodeLineage();
		dbo.setHashKey("29201#D");
		dbo.setRangeKey("003#98931");
		Date date = new Date();
		dbo.setTimestamp(date);
		Long version = 5L;
		dbo.setVersion(version);
		NodeLineage lineage = new NodeLineage(dbo);
		Assert.assertNotNull(lineage);
		Assert.assertEquals("29201", lineage.getNodeId());
		Assert.assertEquals(LineageType.DESCENDANT, lineage.getLineageType());
		Assert.assertEquals(3, lineage.getDistance());
		Assert.assertEquals("98931", lineage.getAncestorOrDescendantId());
		Assert.assertEquals(date, lineage.getTimestamp());
		Assert.assertEquals(version, lineage.getVersion());
	}

	@Test
	public void testConstructor() {
		String nodeId = "M";
		LineageType lineageType = LineageType.ANCESTOR;
		int distance = 3;
		String ancestorId = "N";
		Date timestamp = new Date();
		Long version = 5L;
		NodeLineage lineage = new NodeLineage(nodeId, lineageType, distance, ancestorId, timestamp, version);
		Assert.assertEquals(nodeId, lineage.getNodeId());
		Assert.assertEquals(lineageType, lineage.getLineageType());
		Assert.assertEquals(distance, lineage.getDistance());
		Assert.assertEquals(ancestorId, lineage.getAncestorOrDescendantId());
		Assert.assertEquals(timestamp, lineage.getTimestamp());
		Assert.assertEquals(version, lineage.getVersion());
	}

	@Test
	public void testConstructorRoot() {
		String nodeId = "M";
		String ancestorId = DboNodeLineage.ROOT;
		LineageType lineageType = LineageType.ANCESTOR;
		int distance = 1;
		Date timestamp = new Date();
		NodeLineage lineage = new NodeLineage(nodeId, lineageType, distance, ancestorId, timestamp);
		Assert.assertEquals(nodeId, lineage.getNodeId());
		Assert.assertEquals(lineageType, lineage.getLineageType());
		Assert.assertEquals(distance, lineage.getDistance());
		Assert.assertEquals(ancestorId, lineage.getAncestorOrDescendantId());
		Assert.assertEquals(timestamp, lineage.getTimestamp());
		Assert.assertEquals(null, lineage.getVersion());
	}

	@Test(expected=NullPointerException.class)
	public void testConstructorNullPointerException1() {
		NodeLineage lineage = new NodeLineage(null, LineageType.ANCESTOR, 0, "N", null);
		Assert.assertNull(lineage);
	}

	@Test(expected=NullPointerException.class)
	public void testConstructorNullPointerException2() {
		NodeLineage lineage = new NodeLineage("M", LineageType.ANCESTOR, 0, null, null, null);
		Assert.assertNull(lineage);
	}

	@Test(expected=NullPointerException.class)
	public void testConstructorNullPointerException3() {
		NodeLineage lineage = new NodeLineage("M", null, 0, "N", null, null);
		Assert.assertNull(lineage);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testConstructorIllegalArgumentException1() {
		NodeLineage lineage = new NodeLineage("M", LineageType.ANCESTOR, -1, "N", new Date());
		Assert.assertNull(lineage);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testConstructorIllegalArgumentException2() {
		NodeLineage lineage = new NodeLineage("M", LineageType.DESCENDANT, 0, "N", new Date());
		Assert.assertNull(lineage);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testConstructorIllegalArgumentException3() {
		NodeLineage lineage = new NodeLineage("M", LineageType.ANCESTOR, 1, "M", new Date(), null);
		Assert.assertNull(lineage);
	}

	@Test
	public void testCreateDbo() {
		String nodeId = "M";
		LineageType lineageType = LineageType.ANCESTOR;
		int distance = 3;
		String ancestorId = "N";
		Date timestamp = new Date();
		Long version = 5L;
		NodeLineage lineage = new NodeLineage(nodeId, lineageType, distance, ancestorId, timestamp, version);
		DboNodeLineage dbo = lineage.createDbo();
		Assert.assertEquals(DboNodeLineage.createHashKey(nodeId, lineageType), dbo.getHashKey());
		Assert.assertEquals(DboNodeLineage.createRangeKey(distance, ancestorId), dbo.getRangeKey());
		Assert.assertEquals(timestamp, dbo.getTimestamp());
		Assert.assertEquals(version, dbo.getVersion());
	}
}
