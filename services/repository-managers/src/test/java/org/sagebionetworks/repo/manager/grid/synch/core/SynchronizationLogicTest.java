package org.sagebionetworks.repo.manager.grid.synch.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SynchronizationLogicTest {

	@Mock
	private SourceReader<TestSourceItem> mockSource;
	@Mock
	private SyncRules<TestCopyItem, TestSourceItem> mockRules;
	@Mock
	private SyncOutcomeHandler<TestCopyItem, TestSourceItem> mockHandler;

	private SynchronizationLogic logic = new SynchronizationLogic();

	@Test
	public void testSynchronizeWithEmptyCopyAndSource() {
		List<TestCopyItem> copyItems = List.of();
		List<TestSourceItem> sourceItems = List.of();
		when(mockSource.streamRemaining()).thenReturn(sourceItems.stream());

		// call under test
		logic.synchronize(copyItems.stream(), mockSource, mockRules, mockHandler);

		verifyNoMoreInteractions(mockHandler);
	}

	@Test
	public void testSynchronizeWithNoChanges() {
		List<TestCopyItem> copyItems = List.of(new TestCopyItem().setValue("a").setId("one").setWasChangedByUser(false),
				new TestCopyItem().setValue("b").setId("two").setWasChangedByUser(false));
		List<TestSourceItem> sourceItems = List.of(new TestSourceItem().setValue("a").setKey("one"),
				new TestSourceItem().setValue("b").setKey("two"));
		when(mockRules.getKey(copyItems.get(0))).thenReturn("one");
		when(mockRules.getKey(copyItems.get(1))).thenReturn("two");
		when(mockRules.isExcludedFromMatching(any(), any())).thenReturn(false);
		when(mockSource.consume("one")).thenReturn(Optional.of(sourceItems.get(0)));
		when(mockSource.consume("two")).thenReturn(Optional.of(sourceItems.get(1)));
		when(mockRules.matches(copyItems.get(0), sourceItems.get(0))).thenReturn(true);
		when(mockRules.matches(copyItems.get(1), sourceItems.get(1))).thenReturn(true);
		when(mockSource.streamRemaining()).thenReturn(List.<TestSourceItem>of().stream());

		// call under test
		logic.synchronize(copyItems.stream(), mockSource, mockRules, mockHandler);

		verify(mockHandler).onCopyAndSourceMatch(copyItems.get(0), sourceItems.get(0));
		verify(mockHandler).onCopyAndSourceMatch(copyItems.get(1), sourceItems.get(1));
		verifyNoMoreInteractions(mockHandler);
	}

	@Test
	public void testSynchronizeWithConflict() {
		List<TestCopyItem> copyItems = List.of(new TestCopyItem().setValue("a").setId("one").setWasChangedByUser(false),
				new TestCopyItem().setValue("b").setId("two").setWasChangedByUser(false));
		List<TestSourceItem> sourceItems = List.of(new TestSourceItem().setValue("a").setKey("one"),
				new TestSourceItem().setValue("c").setKey("two"));
		when(mockRules.getKey(copyItems.get(0))).thenReturn("one");
		when(mockRules.getKey(copyItems.get(1))).thenReturn("two");
		when(mockRules.isExcludedFromMatching(any(), any())).thenReturn(false);
		when(mockSource.consume("one")).thenReturn(Optional.of(sourceItems.get(0)));
		when(mockSource.consume("two")).thenReturn(Optional.of(sourceItems.get(1)));
		when(mockRules.matches(copyItems.get(0), sourceItems.get(0))).thenReturn(true);
		when(mockRules.matches(copyItems.get(1), sourceItems.get(1))).thenReturn(false);
		when(mockSource.streamRemaining()).thenReturn(List.<TestSourceItem>of().stream());

		// call under test
		logic.synchronize(copyItems.stream(), mockSource, mockRules, mockHandler);

		verify(mockHandler).onCopyAndSourceConflict(copyItems.get(1), sourceItems.get(1));
		verify(mockHandler).onCopyAndSourceMatch(copyItems.get(0), sourceItems.get(0));
		verifyNoMoreInteractions(mockHandler);
	}

	@Test
	public void testSynchronizeWithCopyItemExcludedFromMatching() {
		List<TestCopyItem> copyItems = List.of(new TestCopyItem().setValue("a").setId("one").setWasChangedByUser(false),
				new TestCopyItem().setValue("b").setId("two").setWasChangedByUser(false));
		List<TestSourceItem> sourceItems = List.of(new TestSourceItem().setValue("b").setKey("two"));
		when(mockRules.getKey(copyItems.get(0))).thenReturn("one");
		when(mockRules.getKey(copyItems.get(1))).thenReturn("two");
		when(mockRules.isExcludedFromMatching(copyItems.get(0), "one")).thenReturn(true);
		when(mockRules.isExcludedFromMatching(copyItems.get(1), "two")).thenReturn(false);
		when(mockSource.consume("two")).thenReturn(Optional.of(sourceItems.get(0)));
		when(mockRules.matches(copyItems.get(1), sourceItems.get(0))).thenReturn(true);
		when(mockSource.streamRemaining()).thenReturn(List.<TestSourceItem>of().stream());

		// call under test
		logic.synchronize(copyItems.stream(), mockSource, mockRules, mockHandler);

		// the excluded item's key is still computed and passed to isExcludedFromMatching,
		// but it is never consumed from the source
		// `onCopyOnlyItemAddedByUser` is still invoked
		verify(mockRules).getKey(copyItems.get(0));
		verify(mockRules).isExcludedFromMatching(copyItems.get(0), "one");
		verify(mockSource, never()).consume("one");
		verify(mockHandler).onNewCopyItem(copyItems.get(0), "one");
		verify(mockHandler).onCopyAndSourceMatch(copyItems.get(1), sourceItems.get(0));
		verifyNoMoreInteractions(mockHandler);
	}

	@Test
	public void testSynchronizeWithCopyOnlyUserAdded() {
		List<TestCopyItem> copyItems = List.of(new TestCopyItem().setValue("a").setId("one").setWasChangedByUser(true),
				new TestCopyItem().setValue("b").setId("two").setWasChangedByUser(false));
		List<TestSourceItem> sourceItems = List.of(new TestSourceItem().setValue("b").setKey("two"));
		when(mockRules.getKey(copyItems.get(0))).thenReturn("one");
		when(mockRules.getKey(copyItems.get(1))).thenReturn("two");
		when(mockRules.isExcludedFromMatching(any(), any())).thenReturn(false);
		when(mockSource.consume("one")).thenReturn(Optional.empty());
		when(mockSource.consume("two")).thenReturn(Optional.of(sourceItems.get(0)));
		when(mockRules.matches(copyItems.get(1), sourceItems.get(0))).thenReturn(true);
		when(mockSource.streamRemaining()).thenReturn(List.<TestSourceItem>of().stream());

		// call under test
		logic.synchronize(copyItems.stream(), mockSource, mockRules, mockHandler);

		verify(mockHandler).onNewCopyItem(copyItems.get(0), "one");
		verify(mockHandler).onCopyAndSourceMatch(copyItems.get(1), sourceItems.get(0));
		verifyNoMoreInteractions(mockHandler);
	}

	@Test
	public void testSynchronizeWithCopyOnlyMissingFromSource() {
		List<TestCopyItem> copyItems = List.of(new TestCopyItem().setValue("a").setId("one").setWasChangedByUser(false),
				new TestCopyItem().setValue("b").setId("two").setWasChangedByUser(false));
		List<TestSourceItem> sourceItems = List.of(new TestSourceItem().setValue("b").setKey("two"));
		when(mockRules.getKey(copyItems.get(0))).thenReturn("one");
		when(mockRules.getKey(copyItems.get(1))).thenReturn("two");
		when(mockRules.isExcludedFromMatching(any(), any())).thenReturn(false);
		when(mockSource.consume("one")).thenReturn(Optional.empty());
		when(mockSource.consume("two")).thenReturn(Optional.of(sourceItems.get(0)));
		when(mockRules.matches(copyItems.get(1), sourceItems.get(0))).thenReturn(true);
		when(mockSource.streamRemaining()).thenReturn(List.<TestSourceItem>of().stream());

		// call under test
		logic.synchronize(copyItems.stream(), mockSource, mockRules, mockHandler);

		verify(mockHandler).onDeletedFromSource(copyItems.get(0));
		verify(mockHandler).onCopyAndSourceMatch(copyItems.get(1), sourceItems.get(0));
		verifyNoMoreInteractions(mockHandler);
	}

	@Test
	public void testSynchronizeWithSourceOnlyAdded() {
		List<TestCopyItem> copyItems = List.of(new TestCopyItem().setValue("a").setId("one").setWasChangedByUser(false));
		List<TestSourceItem> sourceItems = List.of(new TestSourceItem().setValue("a").setKey("one"),
				new TestSourceItem().setValue("b").setKey("two"));
		when(mockRules.getKey(copyItems.get(0))).thenReturn("one");
		when(mockRules.isExcludedFromMatching(any(), any())).thenReturn(false);
		when(mockSource.consume("one")).thenReturn(Optional.of(sourceItems.get(0)));
		when(mockRules.matches(copyItems.get(0), sourceItems.get(0))).thenReturn(true);
		when(mockSource.streamRemaining()).thenReturn(List.of(sourceItems.get(1)).stream());
		when(mockRules.wasDeletedByUser(sourceItems.get(1))).thenReturn(false);

		// call under test
		logic.synchronize(copyItems.stream(), mockSource, mockRules, mockHandler);

		verify(mockHandler).onNewSourceItem(sourceItems.get(1));
		verify(mockHandler).onCopyAndSourceMatch(copyItems.get(0), sourceItems.get(0));
		verifyNoMoreInteractions(mockHandler);
	}

	@Test
	public void testSynchronizeWithSourceOnlyDeletedByUser() {
		List<TestCopyItem> copyItems = List.of(new TestCopyItem().setValue("a").setId("one").setWasChangedByUser(false));
		List<TestSourceItem> sourceItems = List.of(new TestSourceItem().setValue("a").setKey("one"),
				new TestSourceItem().setValue("b").setKey("two"));
		when(mockRules.getKey(copyItems.get(0))).thenReturn("one");
		when(mockRules.isExcludedFromMatching(any(), any())).thenReturn(false);
		when(mockSource.consume("one")).thenReturn(Optional.of(sourceItems.get(0)));
		when(mockRules.matches(copyItems.get(0), sourceItems.get(0))).thenReturn(true);
		when(mockSource.streamRemaining()).thenReturn(List.of(sourceItems.get(1)).stream());
		when(mockRules.wasDeletedByUser(sourceItems.get(1))).thenReturn(true);

		// call under test
		logic.synchronize(copyItems.stream(), mockSource, mockRules, mockHandler);

		verify(mockHandler).onDeletedFromCopy(sourceItems.get(1));
		verify(mockHandler).onCopyAndSourceMatch(copyItems.get(0), sourceItems.get(0));
		verifyNoMoreInteractions(mockHandler);
	}

	@Test
	public void testSynchronizeWithNullCopyItems() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			logic.synchronize(null, mockSource, mockRules, mockHandler);
		}).getMessage();
		assertEquals("copyItems is required.", message);
		verifyNoMoreInteractions(mockHandler);
	}

	@Test
	public void testSynchronizeWithNullSource() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			logic.synchronize(List.<TestCopyItem>of().stream(), null, mockRules, mockHandler);
		}).getMessage();
		assertEquals("source is required.", message);
		verifyNoMoreInteractions(mockHandler);
	}

	@Test
	public void testSynchronizeWithNullRules() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			logic.synchronize(List.<TestCopyItem>of().stream(), mockSource, null, mockHandler);
		}).getMessage();
		assertEquals("rules is required.", message);
		verifyNoMoreInteractions(mockHandler);
	}

	@Test
	public void testSynchronizeWithNullHandler() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			logic.synchronize(List.<TestCopyItem>of().stream(), mockSource, mockRules, null);
		}).getMessage();
		assertEquals("handler is required.", message);
		verifyNoMoreInteractions(mockHandler);
	}

	@Test
	public void testSynchronizeWithNullKey() {
		List<TestCopyItem> copyItems = List.of(new TestCopyItem().setValue("a").setId("one").setWasChangedByUser(false));
		when(mockRules.getKey(copyItems.get(0))).thenReturn(null);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			logic.synchronize(copyItems.stream(), mockSource, mockRules, mockHandler);
		}).getMessage();
		assertEquals("key is required.", message);
		verify(mockRules, never()).isExcludedFromMatching(any(), any());
		verifyNoMoreInteractions(mockHandler);
	}

}
