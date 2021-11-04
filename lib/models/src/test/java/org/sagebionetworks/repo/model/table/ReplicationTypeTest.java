package org.sagebionetworks.repo.model.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.ObjectType;

public class ReplicationTypeTest {
	
	@Test
	public void testMatchTypeWithEntity() {
		Optional<ReplicationType> optional = ReplicationType.matchType(ObjectType.ENTITY);
		assertNotNull(optional);
		assertTrue(optional.isPresent());
		assertEquals(ReplicationType.ENTITY, optional.get());
	}
	
	@Test
	public void testMatchTypeWithSubmission() {
		Optional<ReplicationType> optional = ReplicationType.matchType(ObjectType.SUBMISSION);
		assertNotNull(optional);
		assertTrue(optional.isPresent());
		assertEquals(ReplicationType.SUBMISSION, optional.get());
	}
	
	@Test
	public void testMatchTypeWithOther() {
		Optional<ReplicationType> optional = ReplicationType.matchType(ObjectType.ACCESS_APPROVAL);
		assertNotNull(optional);
		assertFalse(optional.isPresent());
	}

}
