package org.sagebionetworks.dynamo.dao;

import java.util.Date;

import junit.framework.Assert;

import org.junit.Test;
import org.sagebionetworks.dynamo.KeyValueSplitter;

public class NodeLineageTest {

	@Test
	public void testConstructor() {
		String nodeId = "M";
		String ancestorId = "N";
		LineageType lineage = LineageType.ANCESTOR;
		int depth = 3;
		Date timestamp = new Date();
		NodeLineage pair = new NodeLineage(nodeId, ancestorId, lineage, depth, timestamp);
		Assert.assertEquals(nodeId, pair.getNodeId());
		Assert.assertEquals(ancestorId, pair.getAncestorOrDescendantId());
		Assert.assertEquals(lineage, pair.getLineageType());
		Assert.assertEquals(depth, pair.getDistance());
		Assert.assertEquals(timestamp, pair.getTimestamp());
	}

	@Test
	public void testConstructorRoot() {
		String nodeId = "M";
		String ancestorId = "M";
		LineageType lineage = LineageType.ANCESTOR;
		int depth = 0;
		Date timestamp = new Date();
		NodeLineage pair = new NodeLineage(nodeId, ancestorId, lineage, depth, timestamp);
		Assert.assertEquals(nodeId, pair.getNodeId());
		Assert.assertEquals(ancestorId, pair.getAncestorOrDescendantId());
		Assert.assertEquals(lineage, pair.getLineageType());
		Assert.assertEquals(depth, pair.getDistance());
		Assert.assertEquals(timestamp, pair.getTimestamp());
	}

	@Test(expected=NullPointerException.class)
	public void testConstructorNullPointerException1() {
		NodeLineage pair = new NodeLineage(null, "N",  LineageType.ANCESTOR, 0, new Date());
		Assert.assertNull(pair);
	}

	@Test(expected=NullPointerException.class)
	public void testConstructorNullPointerException2() {
		NodeLineage pair = new NodeLineage("M", null,  LineageType.ANCESTOR, 0, new Date());
		Assert.assertNull(pair);
	}

	@Test(expected=NullPointerException.class)
	public void testConstructorNullPointerException3() {
		NodeLineage pair = new NodeLineage("M", "N", null, 0, new Date());
		Assert.assertNull(pair);
	}

	@Test(expected=NullPointerException.class)
	public void testConstructorNullPointerException4() {
		NodeLineage pair = new NodeLineage("M", "N", LineageType.DESCENDANT, 0, null);
		Assert.assertNull(pair);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testConstructorIllegalArgumentException1() {
		NodeLineage pair = new NodeLineage("M", "N", LineageType.ANCESTOR, -1, new Date());
		Assert.assertNull(pair);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testConstructorIllegalArgumentException2() {
		NodeLineage pair = new NodeLineage("M", "M", LineageType.DESCENDANT, 0, new Date());
		Assert.assertNull(pair);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testConstructorIllegalArgumentException3() {
		NodeLineage pair = new NodeLineage("M", "M", LineageType.ANCESTOR, 1, new Date());
		Assert.assertNull(pair);
	}

	@Test
	public void testGetSetNodeIdLineageType() {
		NodeLineage pair = new NodeLineage();
		Assert.assertNull(pair.getHashKey());
		String nodeIdLineageType = "M" + KeyValueSplitter.SEPARATOR + LineageType.ANCESTOR;
		pair.setHashKey(nodeIdLineageType);
		Assert.assertEquals(nodeIdLineageType, pair.getHashKey());
	}

	@Test
	public void testGetSetDepthNodeId() {
		NodeLineage pair = new NodeLineage();
		Assert.assertNull(pair.getRangeKey());
		String depthNodeId = 0 + KeyValueSplitter.SEPARATOR + "N";
		pair.setRangeKey(depthNodeId);
		Assert.assertEquals(depthNodeId, pair.getRangeKey());
	}

	@Test
	public void testGetSetVersion() {
		NodeLineage pair = new NodeLineage();
		Assert.assertNull(pair.getVersion());
		Long version = 2L;
		pair.setVersion(version);
		Assert.assertEquals(version, pair.getVersion());
	}

	@Test
	public void testGetSetTimestamp() {
		NodeLineage pair = new NodeLineage();
		Assert.assertNull(pair.getTimestamp());
		Date timestamp = new Date();
		pair.setTimestamp(timestamp);
		Assert.assertEquals(timestamp, pair.getTimestamp());
	}
}
