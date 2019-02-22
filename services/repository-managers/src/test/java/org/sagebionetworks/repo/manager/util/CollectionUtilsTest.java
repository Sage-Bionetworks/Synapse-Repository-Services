package org.sagebionetworks.repo.manager.util;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.sagebionetworks.manager.util.CollectionUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class CollectionUtilsTest {

	@Test
	public void testConvertLongToString(){
		List<Long> in = Lists.newArrayList(3L,4L, null);
		Set<String> out = Sets.newHashSet();
		CollectionUtils.convertLongToString(in, out);
		Set<String> expected = Sets.newHashSet("3","4");
		assertEquals(expected, out);
	}
	
	@Test
	public void testConvertStringToLong(){
		List<String> in = Lists.newArrayList("2","4", null);
		Set<Long> out = Sets.newHashSet();
		CollectionUtils.convertStringToLong(in, out);
		Set<Long> expected = Sets.newHashSet(2L, 4L);
		assertEquals(expected, out);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testConvertStringToLongNotANumber(){
		List<String> in = Lists.newArrayList("2","not a number");
		Set<Long> out = Sets.newHashSet();
		// should fail.
		CollectionUtils.convertStringToLong(in, out);
	}
}
