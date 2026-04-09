package org.sagebionetworks.repo.manager.grid.synch.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SynchronizationLogicTest {

	@Mock
	private Copy<TestCopyItem, TestSourceItem> mockCopy;
	@Mock
	private Source<TestCopyItem, TestSourceItem> mockSource;
	@Mock
	private Merge<TestCopyItem, TestSourceItem> mockMerge;

	private SynchronizationLogic logic = new SynchronizationLogic();

	@Test
	public void testSynchronizeWithEmptyCopyAndSource() {
		List<TestCopyItem> copyItems = Collections.emptyList();
		List<TestSourceItem> sourceItems = Collections.emptyList();
		when(mockCopy.streamItems()).thenReturn(copyItems.stream());
		when(mockSource.streamRemaining()).thenReturn(sourceItems.stream());

		// call under test
		logic.synchronize(mockCopy, mockSource, mockMerge);

		verifyNoMoreInteractions(mockCopy, mockSource, mockMerge);
	}

	@Test
	public void testSynchronizeWithNoChanges() {
		List<TestCopyItem> copyItems = List.of(
				// one
				new TestCopyItem().setValue("a").setId("one").setWasChangedByUser(false),
				// two
				new TestCopyItem().setValue("b").setId("two").setWasChangedByUser(false));
		List<TestSourceItem> sourceItems = List.of(
				// one
				new TestSourceItem().setValue("a").setKey("one"),
				// two
				new TestSourceItem().setValue("b").setKey("two"));
		when(mockCopy.streamItems()).thenReturn(copyItems.stream());
		when(mockSource.getKey(copyItems.get(0))).thenReturn("one");
		when(mockSource.getKey(copyItems.get(1))).thenReturn("two");
		when(mockSource.consume("one")).thenReturn(Optional.of(sourceItems.get(0)));
		when(mockSource.consume("two")).thenReturn(Optional.of(sourceItems.get(1)));

		when(mockSource.matches(copyItems.get(0), sourceItems.get(0))).thenReturn(true);
		when(mockSource.matches(copyItems.get(1), sourceItems.get(1))).thenReturn(true);
		List<TestSourceItem> emptyList = List.of();
		when(mockSource.streamRemaining()).thenReturn(emptyList.stream());

		// call under test
		logic.synchronize(mockCopy, mockSource, mockMerge);

		verifyNoMoreInteractions(mockCopy, mockSource, mockMerge);
	}

	@Test
	public void testSynchronizeWithTwoDoesNotMatch() {
		List<TestCopyItem> copyItems = List.of(
				// one
				new TestCopyItem().setValue("a").setId("one").setWasChangedByUser(false),
				// two
				new TestCopyItem().setValue("b").setId("two").setWasChangedByUser(false));
		List<TestSourceItem> sourceItems = List.of(
				// one
				new TestSourceItem().setValue("a").setKey("one"),
				// two
				new TestSourceItem().setValue("c").setKey("two"));
		when(mockCopy.streamItems()).thenReturn(copyItems.stream());
		when(mockSource.getKey(copyItems.get(0))).thenReturn("one");
		when(mockSource.getKey(copyItems.get(1))).thenReturn("two");
		when(mockSource.consume("one")).thenReturn(Optional.of(sourceItems.get(0)));
		when(mockSource.consume("two")).thenReturn(Optional.of(sourceItems.get(1)));

		when(mockSource.matches(copyItems.get(0), sourceItems.get(0))).thenReturn(true);
		when(mockSource.matches(copyItems.get(1), sourceItems.get(1))).thenReturn(false);
		List<TestSourceItem> emptyList = List.of();
		when(mockSource.streamRemaining()).thenReturn(emptyList.stream());

		// call under test
		logic.synchronize(mockCopy, mockSource, mockMerge);

		verify(mockMerge).merge("two", copyItems.get(1), sourceItems.get(1));

		verifyNoMoreInteractions(mockCopy, mockSource, mockMerge);
	}

	@Test
	public void testSynchronizeWithAddedToCopy() {
		List<TestCopyItem> copyItems = List.of(
				// one
				new TestCopyItem().setValue("a").setId("one").setWasChangedByUser(true),
				// two
				new TestCopyItem().setValue("b").setId("two").setWasChangedByUser(false));
		List<TestSourceItem> sourceItems = List.of(
				// two
				new TestSourceItem().setValue("b").setKey("two"));
		when(mockCopy.streamItems()).thenReturn(copyItems.stream());
		when(mockSource.getKey(copyItems.get(0))).thenReturn("one");
		when(mockSource.getKey(copyItems.get(1))).thenReturn("two");
		when(mockSource.consume("one")).thenReturn(Optional.empty());
		when(mockSource.consume("two")).thenReturn(Optional.of(sourceItems.get(0)));
		when(mockSource.isItemAdditionSupported()).thenReturn(true);

		when(mockSource.matches(copyItems.get(1), sourceItems.get(0))).thenReturn(true);
		List<TestSourceItem> emptyList = List.of();
		when(mockSource.streamRemaining()).thenReturn(emptyList.stream());

		// call under test
		logic.synchronize(mockCopy, mockSource, mockMerge);

		verify(mockSource).addItem(copyItems.get(0));

		verifyNoMoreInteractions(mockCopy, mockSource, mockMerge);
	}

	@Test
	public void testSynchronizeWithChangedInCopyButItemAdditionNotSupported() {
		// When a row exists in the copy with user changes, but the source does not
		// support row addition (e.g. entity views), the row should be removed from the
		// copy rather than pushed to the source.
		List<TestCopyItem> copyItems = List.of(
				// one - changed by user but source no longer has it
				new TestCopyItem().setValue("a").setId("one").setWasChangedByUser(true),
				// two
				new TestCopyItem().setValue("b").setId("two").setWasChangedByUser(false));
		List<TestSourceItem> sourceItems = List.of(
				// two
				new TestSourceItem().setValue("b").setKey("two"));
		when(mockCopy.streamItems()).thenReturn(copyItems.stream());
		when(mockSource.getKey(copyItems.get(0))).thenReturn("one");
		when(mockSource.getKey(copyItems.get(1))).thenReturn("two");
		when(mockSource.consume("one")).thenReturn(Optional.empty());
		when(mockSource.consume("two")).thenReturn(Optional.of(sourceItems.get(0)));
		when(mockSource.isItemAdditionSupported()).thenReturn(false);

		when(mockSource.matches(copyItems.get(1), sourceItems.get(0))).thenReturn(true);
		List<TestSourceItem> emptyList = List.of();
		when(mockSource.streamRemaining()).thenReturn(emptyList.stream());

		// call under test
		logic.synchronize(mockCopy, mockSource, mockMerge);

		verify(mockCopy).removeItem(copyItems.get(0));

		verifyNoMoreInteractions(mockCopy, mockSource, mockMerge);
	}

	@Test
	public void testSynchronizeWithDeletedFromSource() {
		List<TestCopyItem> copyItems = List.of(
				// one
				new TestCopyItem().setValue("a").setId("one").setWasChangedByUser(false),
				// two
				new TestCopyItem().setValue("b").setId("two").setWasChangedByUser(false));
		List<TestSourceItem> sourceItems = List.of(
				// two
				new TestSourceItem().setValue("b").setKey("two"));
		when(mockCopy.streamItems()).thenReturn(copyItems.stream());
		when(mockSource.getKey(copyItems.get(0))).thenReturn("one");
		when(mockSource.getKey(copyItems.get(1))).thenReturn("two");
		when(mockSource.consume("one")).thenReturn(Optional.empty());
		when(mockSource.consume("two")).thenReturn(Optional.of(sourceItems.get(0)));

		when(mockSource.matches(copyItems.get(1), sourceItems.get(0))).thenReturn(true);
		List<TestSourceItem> emptyList = List.of();
		when(mockSource.streamRemaining()).thenReturn(emptyList.stream());

		// call under test
		logic.synchronize(mockCopy, mockSource, mockMerge);

		verify(mockCopy).removeItem(copyItems.get(0));

		verifyNoMoreInteractions(mockCopy, mockSource, mockMerge);
	}

	@Test
	public void testSynchronizeWithAddedToSource() {
		List<TestCopyItem> copyItems = List.of(
				// one
				new TestCopyItem().setValue("a").setId("one").setWasChangedByUser(false));
		List<TestSourceItem> sourceItems = List.of(
				// one
				new TestSourceItem().setValue("a").setKey("one"),
				// two
				new TestSourceItem().setValue("b").setKey("two"));
		when(mockCopy.streamItems()).thenReturn(copyItems.stream());
		when(mockSource.getKey(copyItems.get(0))).thenReturn("one");
		when(mockSource.consume("one")).thenReturn(Optional.of(sourceItems.get(0)));

		when(mockSource.matches(copyItems.get(0), sourceItems.get(0))).thenReturn(true);
		when(mockSource.streamRemaining()).thenReturn(List.of(sourceItems.get(1)).stream());
		when(mockCopy.wasDeletedByUser("two")).thenReturn(false);

		// call under test
		logic.synchronize(mockCopy, mockSource, mockMerge);

		verify(mockCopy).addItem(sourceItems.get(1));

		verifyNoMoreInteractions(mockCopy, mockSource, mockMerge);
	}

	@Test
	public void testSynchronizeWithUserDeletedFromCopy() {
		List<TestCopyItem> copyItems = List.of(
				// one
				new TestCopyItem().setValue("a").setId("one").setWasChangedByUser(false));
		List<TestSourceItem> sourceItems = List.of(
				// one
				new TestSourceItem().setValue("a").setKey("one"),
				// two
				new TestSourceItem().setValue("b").setKey("two"));
		when(mockCopy.streamItems()).thenReturn(copyItems.stream());
		when(mockSource.getKey(copyItems.get(0))).thenReturn("one");
		when(mockSource.consume("one")).thenReturn(Optional.of(sourceItems.get(0)));

		when(mockSource.matches(copyItems.get(0), sourceItems.get(0))).thenReturn(true);
		when(mockSource.streamRemaining()).thenReturn(List.of(sourceItems.get(1)).stream());
		when(mockCopy.wasDeletedByUser("two")).thenReturn(true);
		when(mockSource.isItemRemovalSupported()).thenReturn(true);

		// call under test
		logic.synchronize(mockCopy, mockSource, mockMerge);

		verify(mockSource).removeItem(sourceItems.get(1));

		verifyNoMoreInteractions(mockCopy, mockSource, mockMerge);
	}

	@Test
	public void testSynchronizeWithUserDeletedFromCopyButItemRemovalNotSupported() {
		// When a row exists in the source but the user deleted it from the copy,
		// and the source does not support item removal (e.g. entity views), the
		// row should be pulled back into the copy rather than removed from the source.
		List<TestCopyItem> copyItems = List.of(
				// one
				new TestCopyItem().setValue("a").setId("one").setWasChangedByUser(false));
		List<TestSourceItem> sourceItems = List.of(
				// one
				new TestSourceItem().setValue("a").setKey("one"),
				// two - user deleted from copy, but source does not support removal
				new TestSourceItem().setValue("b").setKey("two"));
		when(mockCopy.streamItems()).thenReturn(copyItems.stream());
		when(mockSource.getKey(copyItems.get(0))).thenReturn("one");
		when(mockSource.consume("one")).thenReturn(Optional.of(sourceItems.get(0)));

		when(mockSource.matches(copyItems.get(0), sourceItems.get(0))).thenReturn(true);
		when(mockSource.streamRemaining()).thenReturn(List.of(sourceItems.get(1)).stream());
		when(mockCopy.wasDeletedByUser("two")).thenReturn(true);
		when(mockSource.isItemRemovalSupported()).thenReturn(false);

		// call under test
		logic.synchronize(mockCopy, mockSource, mockMerge);

		verify(mockCopy).addItem(sourceItems.get(1));

		verifyNoMoreInteractions(mockCopy, mockSource, mockMerge);
	}

	@Test
	public void testSynchronizeWithNullCopy() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			logic.synchronize(null, mockSource, mockMerge);
		}).getMessage();
		assertEquals("copy is required.", message);
		verifyNoMoreInteractions(mockCopy, mockSource, mockMerge);
	}

	@Test
	public void testSynchronizeWithNullSource() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			logic.synchronize(mockCopy, null, mockMerge);
		}).getMessage();
		assertEquals("source is required.", message);
		verifyNoMoreInteractions(mockCopy, mockSource, mockMerge);
	}

	@Test
	public void testSynchronizeWithNullMerge() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			logic.synchronize(mockCopy, mockSource, null);
		}).getMessage();
		assertEquals("merge is required.", message);
		verifyNoMoreInteractions(mockCopy, mockSource, mockMerge);
	}

	@Test
	public void testSynchronizeWithNullKey() {
		List<TestCopyItem> copyItems = List.of(
				// one
				new TestCopyItem().setValue("a").setId("one").setWasChangedByUser(false));
		when(mockCopy.streamItems()).thenReturn(copyItems.stream());
		when(mockSource.getKey(copyItems.get(0))).thenReturn(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			logic.synchronize(mockCopy, mockSource, mockMerge);
		}).getMessage();
		assertEquals("key is required.", message);
		verifyNoMoreInteractions(mockCopy, mockSource, mockMerge);
	}

}
