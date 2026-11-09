package org.sagebionetworks.repo.manager.file.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StringSerializedFieldRowMapperSupplierTest {

	@Mock
	private ResultSet mockResultSet;

	@Mock
	private Function<String, Set<Long>> mockExtractor;

	@Test
	public void testGetRowMapper() throws SQLException {
		when(mockResultSet.getLong("OWNER_ID")).thenReturn(123L);
		when(mockResultSet.getString("TEMPLATE_JSON")).thenReturn("{\"attachments\":[\"1\",\"2\"]}");
		when(mockExtractor.apply("{\"attachments\":[\"1\",\"2\"]}")).thenReturn(Set.of(1L, 2L));

		// call under test
		ScannedFileHandleAssociation result = new StringSerializedFieldRowMapperSupplier(mockExtractor)
				.getRowMapper("OWNER_ID", List.of("TEMPLATE_JSON")).mapRow(mockResultSet, 0);

		assertEquals(new ScannedFileHandleAssociation(123L).withFileHandleIds(Set.of(1L, 2L)), result);
		verify(mockResultSet).getLong("OWNER_ID");
		verify(mockResultSet).getString("TEMPLATE_JSON");
		verifyNoMoreInteractions(mockResultSet);
	}

	@Test
	public void testGetRowMapperWithNullField() throws SQLException {
		when(mockResultSet.getLong("OWNER_ID")).thenReturn(123L);
		when(mockResultSet.getString("TEMPLATE_JSON")).thenReturn(null);
		when(mockExtractor.apply(null)).thenReturn(Set.of());

		// call under test
		ScannedFileHandleAssociation result = new StringSerializedFieldRowMapperSupplier(mockExtractor)
				.getRowMapper("OWNER_ID", List.of("TEMPLATE_JSON")).mapRow(mockResultSet, 0);

		assertEquals(new ScannedFileHandleAssociation(123L).withFileHandleIds(Set.of()), result);
	}

	@Test
	public void testGetRowMapperWithMultipleColumns() {
		StringSerializedFieldRowMapperSupplier supplier = new StringSerializedFieldRowMapperSupplier(mockExtractor);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			supplier.getRowMapper("OWNER_ID", List.of("A", "B"));
		}).getMessage();

		assertEquals("Exactly one serialized field column is supported.", message);
	}

}
