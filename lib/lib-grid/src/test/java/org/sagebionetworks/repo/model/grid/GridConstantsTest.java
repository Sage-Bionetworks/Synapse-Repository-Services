package org.sagebionetworks.repo.model.grid;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class GridConstantsTest {

	@Test
	public void testIsUserReplica() {
		assertTrue(GridConstants.isUserReplica(GridConstants.START_REPLICA_ID_CLIENT));
		assertTrue(GridConstants.isUserReplica(GridConstants.START_REPLICA_ID_CLIENT + 1L));
		assertFalse(GridConstants.isUserReplica(GridConstants.START_REPLICA_ID_SERVICE));
		assertFalse(GridConstants.isUserReplica(GridConstants.START_REPLICA_ID_SERVICE - 1));
	}

	@Test
	public void testIsUserReplciaWithNullId() {

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			GridConstants.isUserReplica(null);

		}).getMessage();
		assertEquals("replicaId is required.", message);
	}

}
