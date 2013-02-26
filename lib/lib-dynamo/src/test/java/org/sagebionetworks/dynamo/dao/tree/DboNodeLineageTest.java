package org.sagebionetworks.dynamo.dao.tree;

import java.util.Date;

import junit.framework.Assert;

import org.junit.Test;
import org.sagebionetworks.dynamo.KeyValueSplitter;

public class DboNodeLineageTest {

	@Test
	public void testCreateHashKey() {
		String hashKey = DboNodeLineage.createHashKey("M", LineageType.ANCESTOR);
		String hashKeyExpected = "M" + KeyValueSplitter.SEPARATOR + LineageType.ANCESTOR.getType();
		Assert.assertEquals(hashKeyExpected, hashKey);
		hashKey = DboNodeLineage.createHashKey("N", LineageType.DESCENDANT);
		hashKeyExpected = "N" + KeyValueSplitter.SEPARATOR + LineageType.DESCENDANT.getType();
		Assert.assertEquals(hashKeyExpected, hashKey);
	}

	@Test(expected=NullPointerException.class)
	public void testCreateHashKeyNullPointerException1() {
		DboNodeLineage.createHashKey(null, LineageType.ANCESTOR);
	}

	@Test(expected=NullPointerException.class)
	public void testCreateHashKeyNullPointerException2() {
		DboNodeLineage.createHashKey("N", null);
	}

	@Test
	public void testCreateRangeKey() {

		Assert.assertEquals(100, DboNodeLineage.MAX_DEPTH);

		String rangeKey = DboNodeLineage.createRangeKey(0, "id");
		String rangeKeyExpected = "00" + KeyValueSplitter.SEPARATOR + "id";
		Assert.assertEquals(rangeKeyExpected, rangeKey);

		rangeKey = DboNodeLineage.createRangeKey(10, "id");
		rangeKeyExpected = "10" + KeyValueSplitter.SEPARATOR + "id";
		Assert.assertEquals(rangeKeyExpected, rangeKey);
		
		rangeKey = DboNodeLineage.createRangeKey(99, "id");
		rangeKeyExpected = "99" + KeyValueSplitter.SEPARATOR + "id";
		Assert.assertEquals(rangeKeyExpected, rangeKey);

		// This is a valid hash key to query ancestor/descendant at a particular level.
		rangeKey = DboNodeLineage.createRangeKey(1, "");
		rangeKeyExpected = "01" + KeyValueSplitter.SEPARATOR + "";
		Assert.assertEquals(rangeKeyExpected, rangeKey);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateRangeKeyIllegalArgumentException() {
		DboNodeLineage.createRangeKey(-1, "id");
	}

	@Test(expected=NullPointerException.class)
	public void testCreateRangeKeyNullPointerException() {
		DboNodeLineage.createRangeKey(0, null);
	}

	@Test
	public void testGetSetHashKey() {
		DboNodeLineage dbo = new DboNodeLineage();
		Assert.assertNull(dbo.getHashKey());
		String hashKey = DboNodeLineage.createHashKey("M", LineageType.ANCESTOR);
		dbo.setHashKey(hashKey);
		Assert.assertEquals(hashKey, dbo.getHashKey());
	}

	@Test
	public void testGetSetRangeKey() {
		DboNodeLineage dbo = new DboNodeLineage();
		Assert.assertNull(dbo.getRangeKey());
		String rangeKey = DboNodeLineage.createRangeKey(3, "N");
		dbo.setRangeKey(rangeKey);
		Assert.assertEquals(rangeKey, dbo.getRangeKey());
	}

	@Test
	public void testGetSetVersion() {
		DboNodeLineage dbo = new DboNodeLineage();
		Assert.assertNull(dbo.getVersion());
		Long version = 2L;
		dbo.setVersion(version);
		Assert.assertEquals(version, dbo.getVersion());
	}

	@Test
	public void testGetSetTimestamp() {
		DboNodeLineage dbo = new DboNodeLineage();
		Assert.assertNull(dbo.getTimestamp());
		Date timestamp = new Date();
		dbo.setTimestamp(timestamp);
		Assert.assertEquals(timestamp, dbo.getTimestamp());
	}
}
