package org.sagebionetworks.repo.manager.grid.synch.row;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItemReader;
import org.sagebionetworks.repo.manager.grid.synch.io.RowSourceItemReference;

@ExtendWith(MockitoExtension.class)
public class RowSourceReaderTest {

	@Mock
	private RowSourceItemReader mockRowReader;
	@Mock
	private RowSourceItemReference mockRowHeader;

	@InjectMocks
	private RowSourceReader reader;

	@Test
	public void testConsume() {
		when(mockRowReader.consumeRow("a")).thenReturn(Optional.of(mockRowHeader));
		when(mockRowReader.consumeRow("b")).thenReturn(Optional.empty());

		// call under test
		assertEquals(Optional.of(mockRowHeader), reader.consume("a"));
		assertEquals(Optional.empty(), reader.consume("b"));
	}

	@Test
	public void testStreamRemaining() {
		List<RowSourceItemReference> input = List.of(Mockito.mock(RowSourceItemReference.class),
				Mockito.mock(RowSourceItemReference.class));
		when(mockRowReader.remainingRows()).thenReturn(input.iterator());

		// call under test
		List<RowSourceItemReference> refs = reader.streamRemaining().collect(Collectors.toList());
		assertEquals(input, refs);
	}

}
