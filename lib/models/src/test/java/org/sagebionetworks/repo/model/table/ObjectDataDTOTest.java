package org.sagebionetworks.repo.model.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

public class ObjectDataDTOTest {
	
	@Test
	public void testCompare() {
		
		ObjectDataDTO one = new ObjectDataDTO().setId(1L).setVersion(0L).setName("1.0");
		ObjectDataDTO two = new ObjectDataDTO().setId(1L).setVersion(0L).setName("1.0");
		ObjectDataDTO three = new ObjectDataDTO().setId(1L).setVersion(1L).setName("1.1");
		ObjectDataDTO four = new ObjectDataDTO().setId(2L).setVersion(1L).setName("2.1");
		// call under test
		assertEquals(0, one.compareTo(one));
		assertEquals(0, one.compareTo(two));
		assertEquals(1, three.compareTo(one));
		assertEquals(-1, one.compareTo(three));
		assertEquals(1, four.compareTo(one));
		assertEquals(-1, one.compareTo(four));
	}
	
	@Test
	public void testGetIdVersion() {
		ObjectDataDTO one = new ObjectDataDTO().setId(1L).setVersion(0L).setName("1.0");
		assertEquals("1.0", one.getIdVersion());
	}
	
	@Test
	public void testDeduplicate() {
		
		ObjectDataDTO one = new ObjectDataDTO().setId(1L).setVersion(0L).setName("1.0");
		ObjectDataDTO two = new ObjectDataDTO().setId(1L).setVersion(0L).setName("Duplicate");
		ObjectDataDTO three = new ObjectDataDTO().setId(1L).setVersion(1L).setName("1.1");
		ObjectDataDTO four = new ObjectDataDTO().setId(2L).setVersion(1L).setName("2.1");
		List<ObjectDataDTO> input = Arrays.asList(one, three, two, four);
		// call under test
		Collection<ObjectDataDTO> result = ObjectDataDTO.deDuplicate(input);
		assertEquals(3, result.size());
		assertFalse(result.contains(one));
		assertTrue(result.contains(two));
		assertTrue(result.contains(three));
		assertTrue(result.contains(four));
	}

}
