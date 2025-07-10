package org.sagebionetworks.repo.model.grid.patch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import org.junit.jupiter.api.Test;

public class LogicalTimestampTest {

	@Test
	public void testCompareTo() {
		// call under test
		assertEquals(0, new LogicalTimestamp().setSequenceNumber(1L).setReplicaId(11L)
				.compareTo(new LogicalTimestamp().setSequenceNumber(1L).setReplicaId(11L)));
		// call under test
		assertEquals(1, new LogicalTimestamp().setSequenceNumber(2L).setReplicaId(11L)
				.compareTo(new LogicalTimestamp().setSequenceNumber(1L).setReplicaId(11L)));
		// call under test
		assertEquals(-1, new LogicalTimestamp().setSequenceNumber(1L).setReplicaId(11L)
				.compareTo(new LogicalTimestamp().setSequenceNumber(2L).setReplicaId(11L)));
		// call under test
		assertEquals(1, new LogicalTimestamp().setSequenceNumber(1L).setReplicaId(12L)
				.compareTo(new LogicalTimestamp().setSequenceNumber(1L).setReplicaId(11L)));
		// call under test
		assertEquals(-1, new LogicalTimestamp().setSequenceNumber(1L).setReplicaId(11L)
				.compareTo(new LogicalTimestamp().setSequenceNumber(1L).setReplicaId(12L)));
	}

	@Test
	public void testCompareNullOther() {
		String message = assertThrowsExactly(IllegalArgumentException.class, () -> {
			// call under test
			new LogicalTimestamp().setReplicaId(1L).setSequenceNumber(11L).compareTo(null);
		}).getMessage();
		assertEquals("other is required.", message);
	}

	@Test
	public void testCompareToNullOtherRep() {
		String message = assertThrowsExactly(IllegalArgumentException.class, () -> {
			// call under test
			new LogicalTimestamp().setSequenceNumber(1L).setReplicaId(11L)
					.compareTo(new LogicalTimestamp().setSequenceNumber(1L).setReplicaId(null));
		}).getMessage();
		assertEquals("other.replicaId is required.", message);
	}
	
	@Test
	public void testCompareToNullOtherSeq() {
		String message = assertThrowsExactly(IllegalArgumentException.class, () -> {
			// call under test
			new LogicalTimestamp().setSequenceNumber(1L).setReplicaId(11L)
					.compareTo(new LogicalTimestamp().setSequenceNumber(null).setReplicaId(11L));
		}).getMessage();
		assertEquals("other.sequenceNumber is required.", message);
	}
	
	@Test
	public void testCompareToNullThisSeq() {
		String message = assertThrowsExactly(IllegalArgumentException.class, () -> {
			// call under test
			new LogicalTimestamp().setSequenceNumber(null).setReplicaId(10L)
					.compareTo(new LogicalTimestamp().setSequenceNumber(2L).setReplicaId(11L));
		}).getMessage();
		assertEquals("this.sequenceNumber is required.", message);
	}
	
	@Test
	public void testCompareToNullThisRep() {
		String message = assertThrowsExactly(IllegalArgumentException.class, () -> {
			// call under test
			new LogicalTimestamp().setSequenceNumber(1L).setReplicaId(null)
					.compareTo(new LogicalTimestamp().setSequenceNumber(2L).setReplicaId(11L));
		}).getMessage();
		assertEquals("this.replicaId is required.", message);
	}

}
