package org.sagebionetworks.repo.model.query.entity;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class SynapseIdTransfromerTest {
	
	SynapseIdTransfromer transformer;
	
	@Before
	public void before(){
		transformer = new SynapseIdTransfromer();
	}

	@Test
	public void testTransformNull(){
		assertEquals(null, transformer.transform(null));
	}
	
	@Test
	public void testTransformString(){
		assertEquals(123L, transformer.transform("syn123"));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testTransformUnknown(){
		assertEquals(123L, transformer.transform(new Double(1.2)));
	}
	
	@Test
	public void testTransformLong(){
		assertEquals(123L, transformer.transform(new Long(123)));
	}
	
	@Test
	public void testTransformInteger(){
		assertEquals(123, transformer.transform(new Integer(123)));
	}
	
	@Test
	public void testTransformListStrings(){
		List<String> input = Lists.newArrayList("syn123","syn456");
		List<Long> expected = Lists.newArrayList(123L, 456L);
		assertEquals(expected, transformer.transform(input));
	}
	
	@Test
	public void testTransformListLongs(){
		List<Long> input = Lists.newArrayList(new Long(123), new Long(456));
		List<Long> expected = Lists.newArrayList(123L, 456L);
		assertEquals(expected, transformer.transform(input));
	}
	
	@Test
	public void testTransformListIntegers(){
		List<Integer> input = Lists.newArrayList(new Integer(123), new Integer(456));
		List<Integer> expected = Lists.newArrayList(123, 456);
		assertEquals(expected, transformer.transform(input));
	}
	
	@Test
	public void testTransformSetLong(){
		Set<Long> input = Sets.newHashSet(123L, 456L);
		assertEquals(input, transformer.transform(input));
	}
}
