package org.sagebionetworks.repo.model.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

public class IdAndVersionTest {
	
	@Test
	public void testCreate() {
		IdAndVersion one = new IdAndVersionBuilder().setId(123L).setVersion(456L).build();
		assertEquals(new Long(123), one.getId());
		assertEquals(new Long(456), one.getVersion().get());
	}
	
	@Test
	public void testCreateNullVersion() {
		IdAndVersion one = new IdAndVersionBuilder().setId(123L).setVersion(null).build();
		assertEquals(new Long(123), one.getId());
		assertFalse(one.getVersion().isPresent());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testNullId() {
		new IdAndVersionBuilder().setId(null).setVersion(456L).build();
	}
	
	@Test
	public void testEquals() {
		IdAndVersion one = new IdAndVersionBuilder().setId(123L).setVersion(456L).build();
		IdAndVersion two = new IdAndVersionBuilder().setId(123L).setVersion(456L).build();
		assertTrue(one.equals(two));
	}
	
	@Test
	public void testNotEquals() {
		IdAndVersion one = new IdAndVersionBuilder().setId(123L).setVersion(456L).build();
		IdAndVersion two = new IdAndVersionBuilder().setId(1233L).setVersion(456L).build();
		assertFalse(one.equals(two));
	}
	
	@Test
	public void testToString() {
		IdAndVersion one = new IdAndVersionBuilder().setId(123L).setVersion(456L).build();
		assertEquals("syn123.456", one.toString());
	}
	
	@Test
	public void testToStringNoVersion() {
		IdAndVersion one = new IdAndVersionBuilder().setId(123L).build();
		assertEquals("syn123", one.toString());
	}
	
	@Test
	public void testCompareOrdered() {
		List<IdAndVersion> unordered = Arrays.asList(
				IdAndVersion.parse("syn2.1"),
				IdAndVersion.parse("syn2"),
				IdAndVersion.parse("syn2.2"),
				IdAndVersion.parse("syn1.3"),
				IdAndVersion.parse("syn1"),
				IdAndVersion.parse("syn2.1"),
				IdAndVersion.parse("syn1")
		);
		// call under test
		List<IdAndVersion> ordered = unordered.stream().sorted().collect(Collectors.toList());
		List<IdAndVersion> expected = Arrays.asList(
				IdAndVersion.parse("syn1"),
				IdAndVersion.parse("syn1"),
				IdAndVersion.parse("syn1.3"),
				IdAndVersion.parse("syn2"),
				IdAndVersion.parse("syn2.1"),
				IdAndVersion.parse("syn2.1"),
				IdAndVersion.parse("syn2.2")
		);
		assertEquals(expected, ordered);
	}

}
