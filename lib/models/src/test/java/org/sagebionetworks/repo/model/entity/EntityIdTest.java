package org.sagebionetworks.repo.model.entity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EntityIdTest {
	
	@Test
	public void testCreate() {
		EntityId one = new EntityIdBuilder().setId(123L).setVersion(456L).build();
		assertEquals(new Long(123), one.getId());
		assertEquals(new Long(456), one.getVersion());
	}
	
	@Test
	public void testCreateNullVersion() {
		EntityId one = new EntityIdBuilder().setId(123L).setVersion(null).build();
		assertEquals(new Long(123), one.getId());
		assertEquals(null, one.getVersion());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testNullId() {
		new EntityIdBuilder().setId(null).setVersion(456L).build();
	}
	
	@Test
	public void testEquals() {
		EntityId one = new EntityIdBuilder().setId(123L).setVersion(456L).build();
		EntityId two = new EntityIdBuilder().setId(123L).setVersion(456L).build();
		assertTrue(one.equals(two));
	}
	
	@Test
	public void testNotEquals() {
		EntityId one = new EntityIdBuilder().setId(123L).setVersion(456L).build();
		EntityId two = new EntityIdBuilder().setId(1233L).setVersion(456L).build();
		assertFalse(one.equals(two));
	}
	
	@Test
	public void testToString() {
		EntityId one = new EntityIdBuilder().setId(123L).setVersion(456L).build();
		assertEquals("syn123.456", one.toString());
	}
	
	@Test
	public void testToStringNoVersion() {
		EntityId one = new EntityIdBuilder().setId(123L).build();
		assertEquals("syn123", one.toString());
	}

}
