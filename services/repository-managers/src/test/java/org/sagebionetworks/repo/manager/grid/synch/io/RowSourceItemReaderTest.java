package org.sagebionetworks.repo.manager.grid.synch.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RowSourceItemReaderTest {

	@Mock
	private RandomAccessFile mockRaf;

	/**
	 * Helper: build a {@link DiskPointer} with a one-byte hash equal to the offset
	 * value, making each pointer uniquely identifiable in assertions.
	 */
	private static DiskPointer dp(String key, int offset, int length) {
		return new DiskPointer(key, new byte[] { (byte) offset }, offset, length);
	}

	// ---- Constructor ----------------------------------------------------------------

	@Test
	public void testConstructorWithNullRAFThrowsIllegalArgument() {
		// call under test
		assertThrows(IllegalArgumentException.class, () -> new RowSourceItemReader(List.of(), null));
	}

	@Test
	public void testConstructorWithNullDiskPointersThrowsIllegalArgument() {
		// call under test
		assertThrows(IllegalArgumentException.class, () -> new RowSourceItemReader(null, mockRaf));
	}

	@Test
	public void testConstructorWithEmptyDiskPointersProducesEmptyRemainingRows() throws IOException {
		try (RowSourceItemReader reader = new RowSourceItemReader(List.of(), mockRaf)) {
			// call under test
			assertFalse(reader.remainingRows().hasNext());
		}
	}

	// ---- consumeRow -----------------------------------------------------------------

	@Test
	public void testConsumeRowWithUnknownKeyReturnsEmpty() throws IOException {
		try (RowSourceItemReader reader = new RowSourceItemReader(List.of(dp("known", 0, 1)), mockRaf)) {
			// call under test
			Optional<RowSourceItemReference> result = reader.consumeRow("unknown");
			assertTrue(result.isEmpty());
		}
	}

	@Test
	public void testConsumeRowRemovesMapEntryWhenQueueEmpties() throws IOException {
		// Single entry: after consuming it the key must be gone from the internal map.
		try (RowSourceItemReader reader = new RowSourceItemReader(List.of(dp("K1", 0, 5)), mockRaf)) {
			// call under test
			Optional<RowSourceItemReference> consumed = reader.consumeRow("K1");
			assertTrue(consumed.isPresent());

			// key should now be absent — remaining is empty and a second consume returns empty
			assertFalse(reader.remainingRows().hasNext());
			assertTrue(reader.consumeRow("K1").isEmpty());
		}
	}

	@Test
	public void testConsumeRowRetainsKeyWhenDuplicatesRemainInQueue() throws IOException {
		// Two entries for "K1": consuming once must leave the second in the queue.
		DiskPointer first = new DiskPointer("K1", new byte[] { 1 }, 0, 1);
		DiskPointer second = new DiskPointer("K1", new byte[] { 2 }, 1, 1);
		try (RowSourceItemReader reader = new RowSourceItemReader(List.of(first, second), mockRaf)) {
			// call under test — first consume returns first arrival
			Optional<RowSourceItemReference> ref1 = reader.consumeRow("K1");
			assertTrue(ref1.isPresent());
			assertArrayEquals(first.getHash(), ref1.get().getHash());

			// call under test — second consume succeeds because key was retained
			Optional<RowSourceItemReference> ref2 = reader.consumeRow("K1");
			assertTrue(ref2.isPresent());
			assertArrayEquals(second.getHash(), ref2.get().getHash());

			// now queue is fully drained — key is removed
			assertTrue(reader.consumeRow("K1").isEmpty());
		}
	}

	@Test
	public void testConsumeRowReturnsFirstArrivalHashAndKey() throws IOException {
		byte[] hashFirst = { 10, 20 };
		byte[] hashSecond = { 30, 40 };
		DiskPointer dpFirst = new DiskPointer("myKey", hashFirst, 0, 2);
		DiskPointer dpSecond = new DiskPointer("myKey", hashSecond, 2, 2);
		try (RowSourceItemReader reader = new RowSourceItemReader(List.of(dpFirst, dpSecond), mockRaf)) {
			// call under test
			RowSourceItemReference ref = reader.consumeRow("myKey").orElseThrow();
			assertEquals("myKey", ref.getKey());
			assertArrayEquals(hashFirst, ref.getHash());
		}
	}

	// ---- remainingRows --------------------------------------------------------------

	@Test
	public void testRemainingRowsAfterAllConsumedReturnsEmptyIterator() throws IOException {
		try (RowSourceItemReader reader = new RowSourceItemReader(List.of(dp("K1", 0, 1)), mockRaf)) {
			reader.consumeRow("K1");
			// call under test
			assertFalse(reader.remainingRows().hasNext());
		}
	}

	@Test
	public void testRemainingRowsPreservesInsertionOrderAcrossKeys() throws IOException {
		// LinkedHashMap must preserve key insertion order.
		DiskPointer dpA = dp("A", 0, 1);
		DiskPointer dpB = dp("B", 1, 1);
		DiskPointer dpC = dp("C", 2, 1);
		try (RowSourceItemReader reader = new RowSourceItemReader(List.of(dpA, dpB, dpC), mockRaf)) {
			// call under test
			Iterator<RowSourceItemReference> remaining = reader.remainingRows();
			assertEquals("A", remaining.next().getKey());
			assertEquals("B", remaining.next().getKey());
			assertEquals("C", remaining.next().getKey());
			assertFalse(remaining.hasNext());
		}
	}

	@Test
	public void testRemainingRowsReturnsAllDuplicatesForSameKeyInArrivalOrder() throws IOException {
		// Three entries for the same key — flatMap must emit all in FIFO order.
		DiskPointer dp1 = new DiskPointer("K1", new byte[] { 1 }, 0, 1);
		DiskPointer dp2 = new DiskPointer("K1", new byte[] { 2 }, 1, 1);
		DiskPointer dp3 = new DiskPointer("K1", new byte[] { 3 }, 2, 1);
		try (RowSourceItemReader reader = new RowSourceItemReader(List.of(dp1, dp2, dp3), mockRaf)) {
			// call under test
			Iterator<RowSourceItemReference> remaining = reader.remainingRows();
			assertArrayEquals(new byte[] { 1 }, remaining.next().getHash());
			assertArrayEquals(new byte[] { 2 }, remaining.next().getHash());
			assertArrayEquals(new byte[] { 3 }, remaining.next().getHash());
			assertFalse(remaining.hasNext());
		}
	}

	@Test
	public void testRemainingRowsWithConsumedFirstDuplicateRetainsSecondDuplicateAndOtherKey()
			throws IOException {
		// K1 has two entries; K2 has one. After consuming K1 once, K1-second and K2
		// must both appear in remainingRows in insertion order.
		DiskPointer dpK1a = new DiskPointer("K1", new byte[] { 1 }, 0, 1);
		DiskPointer dpK1b = new DiskPointer("K1", new byte[] { 2 }, 1, 1);
		DiskPointer dpK2 = new DiskPointer("K2", new byte[] { 3 }, 2, 1);
		try (RowSourceItemReader reader = new RowSourceItemReader(List.of(dpK1a, dpK1b, dpK2), mockRaf)) {
			reader.consumeRow("K1");

			// call under test
			Iterator<RowSourceItemReference> remaining = reader.remainingRows();
			assertArrayEquals(new byte[] { 2 }, remaining.next().getHash());
			assertArrayEquals(new byte[] { 3 }, remaining.next().getHash());
			assertFalse(remaining.hasNext());
		}
	}

	// ---- fetchRow IOException wrapping ----------------------------------------------

	@Test
	public void testFetchRowIOExceptionWrappedAsRuntimeException() throws IOException {
		DiskPointer dp = new DiskPointer("K", new byte[] { 0 }, 0L, 5);
		doThrow(new IOException("disk error")).when(mockRaf).seek(anyLong());
		try (RowSourceItemReader reader = new RowSourceItemReader(List.of(dp), mockRaf)) {
			RowSourceItemReference ref = reader.consumeRow("K").orElseThrow();
			// call under test
			RuntimeException ex = assertThrows(RuntimeException.class, ref::fetchRow);
			assertInstanceOf(IOException.class, ex.getCause());
		}
	}

	// ---- close ----------------------------------------------------------------------

	@Test
	public void testCloseClosesRandomAccessFile() throws IOException {
		RowSourceItemReader reader = new RowSourceItemReader(List.of(), mockRaf);
		// call under test
		reader.close();
		verify(mockRaf).close();
	}
}
