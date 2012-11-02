package org.sagebionetworks.dynamo;

import org.junit.Assert;
import org.junit.Test;

public class KeyValueSplitterTest {

	@Test
	public void testSplitNull() {
		String[] splits =KeyValueSplitter.split(null);
		Assert.assertNotNull(splits);
		Assert.assertEquals(0, splits.length);
	}

	@Test
	public void testSplitEmpty() {
		String[] splits =KeyValueSplitter.split("");
		Assert.assertNotNull(splits);
		Assert.assertEquals(0, splits.length);
	}

	@Test
	public void testSplitSeparator() {
		String[] splits = KeyValueSplitter.split(KeyValueSplitter.SEPARATOR);
		Assert.assertNotNull(splits);
		Assert.assertEquals(0, splits.length);
		String separators = KeyValueSplitter.SEPARATOR +
				KeyValueSplitter.SEPARATOR +
				KeyValueSplitter.SEPARATOR;
		splits = KeyValueSplitter.split(separators);
		Assert.assertNotNull(splits);
		Assert.assertEquals(0, splits.length);
	}

	@Test
	public void test() {
		String input = "A" + KeyValueSplitter.SEPARATOR;
		String[] splits = KeyValueSplitter.split(input);
		Assert.assertNotNull(splits);
		Assert.assertEquals(1, splits.length);
		Assert.assertEquals("A", splits[0]);
		input = KeyValueSplitter.SEPARATOR + "A";
		splits = KeyValueSplitter.split(input);
		Assert.assertNotNull(splits);
		Assert.assertEquals(2, splits.length);
		Assert.assertEquals("", splits[0]);
		Assert.assertEquals("A", splits[1]);
		input = "A" + KeyValueSplitter.SEPARATOR + "B";
		splits = KeyValueSplitter.split(input);
		Assert.assertNotNull(splits);
		Assert.assertEquals(2, splits.length);
		Assert.assertEquals("A", splits[0]);
		Assert.assertEquals("B", splits[1]);
		input = "A" + KeyValueSplitter.SEPARATOR + KeyValueSplitter.SEPARATOR + "B";
		splits = KeyValueSplitter.split(input);
		Assert.assertNotNull(splits);
		Assert.assertEquals(3, splits.length);
		Assert.assertEquals("A", splits[0]);
		Assert.assertEquals("", splits[1]);
		Assert.assertEquals("B", splits[2]);
	}
}
