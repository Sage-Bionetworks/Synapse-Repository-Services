package org.sagebionetworks.repo.manager.file.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.file.scanner.SerializedFieldRowMapperSupplier.Deserializer;
import org.sagebionetworks.repo.manager.file.scanner.SerializedFieldRowMapperSupplier.FileHandleExtractor;
import org.springframework.jdbc.core.RowMapper;

import com.google.common.collect.ImmutableSet;

@ExtendWith(MockitoExtension.class)
public class SerializedFieldRowMapperSupplierTest {

	private static class TestClass {}

	@Mock
	private ResultSet mockResultSet;
	
	@Mock
	private Blob mockBlob;
	
	@Mock
	private Deserializer<TestClass>  mockDeserializer;
	
	@Mock
	private FileHandleExtractor<TestClass> mockExtractor;
	
	@InjectMocks
	private SerializedFieldRowMapperSupplier<TestClass> supplier;
	
	private String idColumnName = "id";
	private String serializedFieldColumnName = "serializedField";
	private RowMapper<ScannedFileHandleAssociation> rowMapper;
	
	@BeforeEach
	public void before() {
		 rowMapper = supplier.getRowMapper(idColumnName, serializedFieldColumnName);
	}
	
	@Test
	public void testRowMapper() throws SQLException {
		Long objectId = 123L;
		TestClass deserialized = new TestClass();
		Set<String> fileHandleIds = ImmutableSet.of("1", "2", "3");
		byte[] serialized = new byte[] {1, 2, 3};
		
		when(mockResultSet.getLong(anyString())).thenReturn(objectId);
		when(mockResultSet.getBlob(anyString())).thenReturn(mockBlob);
		when(mockBlob.length()).thenReturn(Long.valueOf(serialized.length));
		when(mockBlob.getBytes(anyLong(), anyInt())).thenReturn(serialized);
		when(mockDeserializer.deserialize(any())).thenReturn(deserialized);
		when(mockExtractor.extractAll(any())).thenReturn(fileHandleIds);
		
		ScannedFileHandleAssociation expected = new ScannedFileHandleAssociation(objectId).withFileHandleIds(ImmutableSet.of(1L, 2L, 3L));
		
		// Call under test
		ScannedFileHandleAssociation result = rowMapper.mapRow(mockResultSet, 0);
		
		assertEquals(expected, result);
		
		verify(mockResultSet).getLong(idColumnName);
		verify(mockResultSet).getBlob(serializedFieldColumnName);
		verify(mockBlob).getBytes(1, serialized.length);
		verify(mockDeserializer).deserialize(serialized);
		verify(mockExtractor).extractAll(deserialized);
		
	}
	
	@Test
	public void testRowMapperWithMalformedFileHandle() throws SQLException {
		Long objectId = 123L;
		TestClass deserialized = new TestClass();
		Set<String> fileHandleIds = ImmutableSet.of("1", "2", "malformed");
		byte[] serialized = new byte[] {1, 2, 3};
		
		when(mockResultSet.getLong(anyString())).thenReturn(objectId);
		when(mockResultSet.getBlob(anyString())).thenReturn(mockBlob);
		when(mockBlob.length()).thenReturn(Long.valueOf(serialized.length));
		when(mockBlob.getBytes(anyLong(), anyInt())).thenReturn(serialized);
		when(mockDeserializer.deserialize(any())).thenReturn(deserialized);
		when(mockExtractor.extractAll(any())).thenReturn(fileHandleIds);
		
		ScannedFileHandleAssociation expected = new ScannedFileHandleAssociation(objectId).withFileHandleIds(ImmutableSet.of(1L, 2L));
		
		// Call under test
		ScannedFileHandleAssociation result = rowMapper.mapRow(mockResultSet, 0);
		
		assertEquals(expected, result);
		
		verify(mockResultSet).getLong(idColumnName);
		verify(mockResultSet).getBlob(serializedFieldColumnName);
		verify(mockBlob).getBytes(1, serialized.length);
		verify(mockDeserializer).deserialize(serialized);
		verify(mockExtractor).extractAll(deserialized);
		
	}
	
	@Test
	public void testRowMapperWithEmptyBlob() throws SQLException {
		Long objectId = 123L;
		
		when(mockResultSet.getLong(anyString())).thenReturn(objectId);
		when(mockResultSet.getBlob(anyString())).thenReturn(null);
		
		ScannedFileHandleAssociation expected = new ScannedFileHandleAssociation(objectId);
		
		// Call under test
		ScannedFileHandleAssociation result = rowMapper.mapRow(mockResultSet, 0);
		
		assertEquals(expected, result);
		
		verify(mockResultSet).getLong(idColumnName);
		verify(mockResultSet).getBlob(serializedFieldColumnName);

		verifyZeroInteractions(mockBlob, mockDeserializer, mockExtractor);
		
	}
	
	@Test
	public void testRowMapperWithNoFileHandles() throws SQLException {
		Long objectId = 123L;
		TestClass deserialized = new TestClass();
		Set<String> fileHandleIds = Collections.emptySet();
		byte[] serialized = new byte[] {1, 2, 3};
		
		when(mockResultSet.getLong(anyString())).thenReturn(objectId);
		when(mockResultSet.getBlob(anyString())).thenReturn(mockBlob);
		when(mockBlob.length()).thenReturn(Long.valueOf(serialized.length));
		when(mockBlob.getBytes(anyLong(), anyInt())).thenReturn(serialized);
		when(mockDeserializer.deserialize(any())).thenReturn(deserialized);
		when(mockExtractor.extractAll(any())).thenReturn(fileHandleIds);
		
		ScannedFileHandleAssociation expected = new ScannedFileHandleAssociation(objectId);
		
		// Call under test
		ScannedFileHandleAssociation result = rowMapper.mapRow(mockResultSet, 0);
		
		assertEquals(expected, result);
		
		verify(mockResultSet).getLong(idColumnName);
		verify(mockResultSet).getBlob(serializedFieldColumnName);
		verify(mockBlob).getBytes(1, serialized.length);
		verify(mockDeserializer).deserialize(serialized);
		verify(mockExtractor).extractAll(deserialized);
		
	}
	
	@Test
	public void testRowMapperWithNullFileHandles() throws SQLException {
		Long objectId = 123L;
		TestClass deserialized = new TestClass();
		Set<String> fileHandleIds = null;
		byte[] serialized = new byte[] {1, 2, 3};
		
		when(mockResultSet.getLong(anyString())).thenReturn(objectId);
		when(mockResultSet.getBlob(anyString())).thenReturn(mockBlob);
		when(mockBlob.length()).thenReturn(Long.valueOf(serialized.length));
		when(mockBlob.getBytes(anyLong(), anyInt())).thenReturn(serialized);
		when(mockDeserializer.deserialize(any())).thenReturn(deserialized);
		when(mockExtractor.extractAll(any())).thenReturn(fileHandleIds);
		
		ScannedFileHandleAssociation expected = new ScannedFileHandleAssociation(objectId);
		
		// Call under test
		ScannedFileHandleAssociation result = rowMapper.mapRow(mockResultSet, 0);
		
		assertEquals(expected, result);
		
		verify(mockResultSet).getLong(idColumnName);
		verify(mockResultSet).getBlob(serializedFieldColumnName);
		verify(mockBlob).getBytes(1, serialized.length);
		verify(mockDeserializer).deserialize(serialized);
		verify(mockExtractor).extractAll(deserialized);
		
	}
	
	
}
