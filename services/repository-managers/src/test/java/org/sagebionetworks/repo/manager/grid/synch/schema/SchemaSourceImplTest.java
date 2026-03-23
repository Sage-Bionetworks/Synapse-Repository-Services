package org.sagebionetworks.repo.manager.grid.synch.schema;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.synch.handler.SourceHandler;

@ExtendWith(MockitoExtension.class)
public class SchemaSourceImplTest {

	@Mock
	private SourceHandler mockHandler;

	private SchemaSourceImpl source;

	private ColumnCopyItem copyItem;

	@BeforeEach
	public void before() {
		copyItem = new ColumnCopyItem().setColumnName("one");
	}

	@Test
	public void testGetKey() {
		setupSource(List.of());
		assertEquals("one", source.getKey(copyItem));
		verifyNoMoreInteractions(mockHandler);
	}

	@Test
	public void testConsume() {
		setupSource(List.of("a", "b", "c"));
		// call under test
		Optional<ColumnSourceItem> nameOp = source.consume("b");
		assertEquals(Optional.of(new ColumnSourceItem().setColumnName("b")), nameOp);

		// call under source.
		List<ColumnSourceItem> remaining = source.streamRemaining().collect(Collectors.toList());
		assertEquals(List.of(new ColumnSourceItem().setColumnName("a"), new ColumnSourceItem().setColumnName("c")),
				remaining);
		verifyNoMoreInteractions(mockHandler);
	}

	@Test
	public void testConsumeWithNotFound() {
		setupSource(List.of("a", "b", "c"));
		// call under test
		Optional<ColumnSourceItem> nameOp = source.consume("d");
		assertEquals(Optional.empty(), nameOp);

		// call under source.
		List<ColumnSourceItem> remaining = source.streamRemaining().collect(Collectors.toList());
		assertEquals(List.of(new ColumnSourceItem().setColumnName("a"), new ColumnSourceItem().setColumnName("b"),
				new ColumnSourceItem().setColumnName("c")), remaining);
		verifyNoMoreInteractions(mockHandler);
	}

	@Test
	public void testAddItem() {
		setupSource(List.of("a", "b", "c"));
		// call under test
		source.addItem(new ColumnCopyItem().setColumnName("d"));
		verify(mockHandler).addColumnToSource("d");
		verifyNoMoreInteractions(mockHandler);
	}

	@Test
	public void testRemoveItem() {
		setupSource(List.of("a", "b", "c"));
		// call under test
		source.removeItem(new ColumnSourceItem().setColumnName("d"));
		verify(mockHandler).removeColumn("d");
		verifyNoMoreInteractions(mockHandler);
	}

	@Test
	public void testMatches() {
		setupSource(List.of("a", "b", "c"));
		// call under test
		assertTrue(source.matches(new ColumnCopyItem().setColumnName("a"), new ColumnSourceItem().setColumnName("a")));
		assertFalse(source.matches(new ColumnCopyItem().setColumnName("a"), new ColumnSourceItem().setColumnName("b")));
	}

	@Test
	public void testIsItemAdditionSupported() {
		setupSource(List.of());
		when(mockHandler.canAddRemoveColumns()).thenReturn(false);

		// call under test
		assertFalse(source.isItemAdditionSupported());

		verify(mockHandler).canAddRemoveColumns();
		verifyNoMoreInteractions(mockHandler);
	}

	@Test
	public void testIsItemRemovalSupported() {
		setupSource(List.of());
		when(mockHandler.canAddRemoveColumns()).thenReturn(false);

		// call under test
		assertFalse(source.isItemRemovalSupported());

		verify(mockHandler).canAddRemoveColumns();
		verifyNoMoreInteractions(mockHandler);
	}

	void setupSource(List<String> schema) {
		when(mockHandler.getCurrentSourceSchema()).thenReturn(schema);
		source = new SchemaSourceImpl(mockHandler);
	}
}
