package org.sagebionetworks.repo.manager.grid.synch.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.synch.handler.SourceHandler;

@ExtendWith(MockitoExtension.class)
public class SchemaSourceReaderTest {

	@Mock
	private SourceHandler mockHandler;

	private SchemaSourceReader reader;

	@Test
	public void testConsume() {
		setupReader(List.of("a", "b", "c"));
		// call under test
		Optional<ColumnSourceItem> nameOp = reader.consume("b");
		assertEquals(Optional.of(new ColumnSourceItem().setColumnName("b")), nameOp);

		List<ColumnSourceItem> remaining = reader.streamRemaining().collect(Collectors.toList());
		assertEquals(List.of(new ColumnSourceItem().setColumnName("a"), new ColumnSourceItem().setColumnName("c")),
				remaining);
	}

	@Test
	public void testConsumeWithNotFound() {
		setupReader(List.of("a", "b", "c"));
		// call under test
		Optional<ColumnSourceItem> nameOp = reader.consume("d");
		assertEquals(Optional.empty(), nameOp);

		List<ColumnSourceItem> remaining = reader.streamRemaining().collect(Collectors.toList());
		assertEquals(List.of(new ColumnSourceItem().setColumnName("a"), new ColumnSourceItem().setColumnName("b"),
				new ColumnSourceItem().setColumnName("c")), remaining);
	}

	void setupReader(List<String> schema) {
		when(mockHandler.getCurrentSourceSchema()).thenReturn(schema);
		reader = new SchemaSourceReader(mockHandler);
	}
}
