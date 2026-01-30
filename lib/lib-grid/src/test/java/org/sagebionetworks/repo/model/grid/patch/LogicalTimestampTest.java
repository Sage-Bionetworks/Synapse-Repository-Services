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

	@Test
	public void testParseValid() {
		LogicalTimestamp ts = LogicalTimestamp.parse("123.456");
		assertEquals(123L, ts.getReplicaId());
		assertEquals(456L, ts.getSequenceNumber());
	}

	@Test
	public void testParseTrim() {
		LogicalTimestamp ts = LogicalTimestamp.parse(" 7.8 ");
		assertEquals(7L, ts.getReplicaId());
		assertEquals(8L, ts.getSequenceNumber());
	}

	@Test
	public void testParseNull() {
		String message = assertThrowsExactly(IllegalArgumentException.class, () -> {
			LogicalTimestamp.parse(null);
		}).getMessage();
		assertEquals("value is required.", message);
	}

	@Test
	public void testParseInvalidFormatNoDot() {
		String message = assertThrowsExactly(IllegalArgumentException.class, () -> {
			LogicalTimestamp.parse("123456");
		}).getMessage();
		assertEquals("Expected format 'replicaId.sequenceNumber'", message);
	}

	@Test
	public void testParseInvalidFormatLeadingDot() {
		String message = assertThrowsExactly(IllegalArgumentException.class, () -> {
			LogicalTimestamp.parse(".1");
		}).getMessage();
		assertEquals("Expected format 'replicaId.sequenceNumber'", message);
	}

	@Test
	public void testParseInvalidFormatTrailingDot() {
		String message = assertThrowsExactly(IllegalArgumentException.class, () -> {
			LogicalTimestamp.parse("1.");
		}).getMessage();
		assertEquals("Expected format 'replicaId.sequenceNumber'", message);
	}

	@Test
	public void testParseInvalidNumberReplica() {
		String message = assertThrowsExactly(IllegalArgumentException.class, () -> {
			LogicalTimestamp.parse("x.1");
		}).getMessage();
		assertEquals("Invalid number in 'x.1'", message.substring(0, "Invalid number in 'x.1'".length()));
	}

	@Test
	public void testParseInvalidNumberSequence() {
		String message = assertThrowsExactly(IllegalArgumentException.class, () -> {
			LogicalTimestamp.parse("1.y");
		}).getMessage();
		assertEquals("Invalid number in '1.y'", message.substring(0, "Invalid number in '1.y'".length()));
	}


	@Test
	public void testToCompactValid() {
		LogicalTimestamp ts = new LogicalTimestamp().setReplicaId(5L).setSequenceNumber(9L);
		assertEquals("5.9", ts.toCompact());
	}

	@Test
	public void testToCompactMissingReplica() {
		String message = assertThrowsExactly(IllegalArgumentException.class, () -> {
			new LogicalTimestamp().setSequenceNumber(1L).setReplicaId(null).toCompact();
		}).getMessage();
		assertEquals("replicaId is required.", message);
	}

	@Test
	public void testToCompactMissingSequence() {
		String message = assertThrowsExactly(IllegalArgumentException.class, () -> {
			new LogicalTimestamp().setSequenceNumber(null).setReplicaId(1L).toCompact();
		}).getMessage();
		assertEquals("sequenceNumber is required.", message);
	}
}
