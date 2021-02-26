package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_ID;
import static org.sagebionetworks.repo.model.query.jdo.SqlConstants.COL_DATA_ACCESS_REQUEST_REQUEST_SERIALIZED;

import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.file.scanner.BasicFileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.FileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.IdRange;
import org.sagebionetworks.repo.manager.file.scanner.ScannedFileHandleAssociation;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
public class RequestFileHandleAssociationProviderTest {

	@Mock
	private RequestDAO mockRequestDao;
	
	@Mock
	private RequestInterface mockRequest;
	
	@Mock
	private JdbcTemplate mockJdbcTemplate;
	
	@Mock
	private NamedParameterJdbcTemplate mockNamedJdbcTemplate;
	
	@InjectMocks
	private RequestFileHandleAssociationProvider provider;
	
	@Mock
	private ResultSet mockResultSet;
	
	@Mock
	private Blob mockBlob;
	
	private RowMapper<ScannedFileHandleAssociation> scannedMapper = RequestFileHandleAssociationProvider.SCANNED_MAPPER;

	@Test
	public void testGetFileHandleIdsAssociatedWithObjectForACTAccessRequirement() {
		String requestId = "1";
		String duc = "2";
		String irb = "3";
		String other = "4";
		when(mockRequest.getDucFileHandleId()).thenReturn(duc);
		when(mockRequest.getIrbFileHandleId()).thenReturn(irb);
		when(mockRequest.getAttachments()).thenReturn(Arrays.asList(other));
		when(mockRequestDao.get(requestId)).thenReturn(mockRequest);
		Set<String> associated = provider.getFileHandleIdsDirectlyAssociatedWithObject(
				Arrays.asList(duc, other, "5"), requestId);
		assertTrue(associated.contains(duc));
		assertFalse(associated.contains(irb));
		assertTrue(associated.contains(other));
		assertFalse(associated.contains("5"));
	}

	@Test
	public void testGetFileHandleIdsAssociatedWithObjectForACTAccessRequirementNullAttachment() {
		String requestId = "1";
		String duc = "2";
		String irb = "3";
		when(mockRequest.getDucFileHandleId()).thenReturn(duc);
		when(mockRequest.getIrbFileHandleId()).thenReturn(irb);
		when(mockRequestDao.get(requestId)).thenReturn(mockRequest);
		Set<String> associated = provider.getFileHandleIdsDirectlyAssociatedWithObject(
				Arrays.asList(duc, "5"), requestId);
		assertTrue(associated.contains(duc));
		assertFalse(associated.contains(irb));
		assertFalse(associated.contains("5"));
	}

	@Test
	public void testGetObjectTypeForAssociatedType() {
		assertEquals(ObjectType.DATA_ACCESS_REQUEST, provider.getAuthorizationObjectTypeForAssociatedObjectType());
	}
	
	@Test
	public void testScannerIdRange() {
		
		IdRange expected = new IdRange(1, 10);
		
		when(mockNamedJdbcTemplate.getJdbcTemplate()).thenReturn(mockJdbcTemplate);
		when(mockJdbcTemplate.queryForObject(anyString(), any(), eq(BasicFileHandleAssociationScanner.ID_RANGE_MAPPER))).thenReturn(expected);
		
		FileHandleAssociationScanner scanner = provider.getAssociationScanner();
		
		assertNotNull(scanner);
		
		// Call under test
		IdRange range = scanner.getIdRange();
		
		assertEquals(expected, range);
		
		verify(mockJdbcTemplate).queryForObject("SELECT MIN(`ID`), MAX(`ID`) FROM DATA_ACCESS_REQUEST", null, BasicFileHandleAssociationScanner.ID_RANGE_MAPPER);
		
	}
	
	@Test
	public void testScannerScanRange() {
		
		IdRange range = new IdRange(1, 10);
		
		List<ScannedFileHandleAssociation> expected = Arrays.asList(new ScannedFileHandleAssociation("123", 123L));
		
		when(mockNamedJdbcTemplate.query(any(), anyMap(), eq(scannedMapper))).thenReturn(expected, Collections.emptyList());
				
		FileHandleAssociationScanner scanner = provider.getAssociationScanner();
		
		assertNotNull(scanner);
		
		// Call under test
		List<ScannedFileHandleAssociation> result = StreamSupport.stream(scanner.scanRange(range).spliterator(), false).collect(Collectors.toList());
		
		assertEquals(expected, result);
		
		verify(mockNamedJdbcTemplate, times(2)).query(eq("SELECT `ID`, `REQUEST_SERIALIZED` FROM DATA_ACCESS_REQUEST WHERE `ID` BETWEEN :BMINID AND :BMAXID AND REQUEST_SERIALIZED IS NOT NULL ORDER BY `ID` LIMIT :KEY_LIMIT OFFSET :KEY_OFFSET"), anyMap(), eq(scannedMapper));
	}
	

	@Test
	public void testScannerMapperWithNoSerializedField() throws SQLException {
		
		String objectId = "123";
		
		when(mockResultSet.getString(COL_DATA_ACCESS_REQUEST_ID)).thenReturn("123");
		when(mockResultSet.getBlob(COL_DATA_ACCESS_REQUEST_REQUEST_SERIALIZED)).thenReturn(null);
		
		ScannedFileHandleAssociation expected = new ScannedFileHandleAssociation(objectId);
		
		// Call under test
		ScannedFileHandleAssociation result = scannedMapper.mapRow(mockResultSet, 0);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testScannerMapperWithMalformedSerializedField() throws SQLException {
		
		String objectId = "123";
		
		when(mockResultSet.getString(COL_DATA_ACCESS_REQUEST_ID)).thenReturn("123");
		when(mockResultSet.getBlob(COL_DATA_ACCESS_REQUEST_REQUEST_SERIALIZED)).thenReturn(mockBlob);
		when(mockBlob.getBytes(anyLong(), anyInt())).thenReturn(new byte[] {});
		
		ScannedFileHandleAssociation expected = new ScannedFileHandleAssociation(objectId);
		
		// Call under test
		ScannedFileHandleAssociation result = scannedMapper.mapRow(mockResultSet, 0);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testScannerMapperWithNoFileHandleId() throws SQLException {
		
		RequestInterface request = new Request();
		
		String objectId = "123";
		
		when(mockResultSet.getString(COL_DATA_ACCESS_REQUEST_ID)).thenReturn("123");
		when(mockResultSet.getBlob(COL_DATA_ACCESS_REQUEST_REQUEST_SERIALIZED)).thenReturn(mockBlob);
		when(mockBlob.getBytes(anyLong(), anyInt())).thenReturn(RequestUtils.writeSerializedField(request));
		
		ScannedFileHandleAssociation expected = new ScannedFileHandleAssociation(objectId);
		
		// Call under test
		ScannedFileHandleAssociation result = scannedMapper.mapRow(mockResultSet, 0);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testScannerMapperWithMalformedFileHandleIds() throws SQLException {
		
		RequestInterface request = new Request();
		
		request.setDucFileHandleId("malformed");
		
		String objectId = "123";
		
		when(mockResultSet.getString(COL_DATA_ACCESS_REQUEST_ID)).thenReturn("123");
		when(mockResultSet.getBlob(COL_DATA_ACCESS_REQUEST_REQUEST_SERIALIZED)).thenReturn(mockBlob);
		when(mockBlob.getBytes(anyLong(), anyInt())).thenReturn(RequestUtils.writeSerializedField(request));
		
		ScannedFileHandleAssociation expected = new ScannedFileHandleAssociation(objectId).withFileHandleIds(Collections.emptyList());
		
		// Call under test
		ScannedFileHandleAssociation result = scannedMapper.mapRow(mockResultSet, 0);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testScannerMapperWithSingleFileHandleId() throws SQLException {

		RequestInterface request = new Request();
		
		request.setDucFileHandleId("456");
		
		String objectId = "123";
		
		when(mockResultSet.getString(COL_DATA_ACCESS_REQUEST_ID)).thenReturn("123");
		when(mockResultSet.getBlob(COL_DATA_ACCESS_REQUEST_REQUEST_SERIALIZED)).thenReturn(mockBlob);
		when(mockBlob.getBytes(anyLong(), anyInt())).thenReturn(RequestUtils.writeSerializedField(request));
		
		ScannedFileHandleAssociation expected = new ScannedFileHandleAssociation(objectId, 456L);
		
		// Call under test
		ScannedFileHandleAssociation result = scannedMapper.mapRow(mockResultSet, 0);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testScannerMapperWithMultipleFileHandleIds() throws SQLException {
		
		RequestInterface request = new Request();
		request.setDucFileHandleId("123");
		request.setIrbFileHandleId("456");
		request.setAttachments(Arrays.asList("789"));
		
		String objectId = "123";
		
		when(mockResultSet.getString(COL_DATA_ACCESS_REQUEST_ID)).thenReturn("123");
		when(mockResultSet.getBlob(COL_DATA_ACCESS_REQUEST_REQUEST_SERIALIZED)).thenReturn(mockBlob);
		when(mockBlob.getBytes(anyLong(), anyInt())).thenReturn(RequestUtils.writeSerializedField(request));
		
		ScannedFileHandleAssociation expected = new ScannedFileHandleAssociation(objectId).withFileHandleIds(Arrays.asList(123L, 456L, 789L));
		
		// Call under test
		ScannedFileHandleAssociation result = scannedMapper.mapRow(mockResultSet, 0);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testScannerMapperWithMultipleFileHandleIdsAndMalformed() throws SQLException {
		
		RequestInterface request = new Request();
		request.setDucFileHandleId("123");
		request.setIrbFileHandleId("malformed");
		request.setAttachments(Arrays.asList("789"));
		
		String objectId = "123";
		
		when(mockResultSet.getString(COL_DATA_ACCESS_REQUEST_ID)).thenReturn("123");
		when(mockResultSet.getBlob(COL_DATA_ACCESS_REQUEST_REQUEST_SERIALIZED)).thenReturn(mockBlob);
		when(mockBlob.getBytes(anyLong(), anyInt())).thenReturn(RequestUtils.writeSerializedField(request));
		
		ScannedFileHandleAssociation expected = new ScannedFileHandleAssociation(objectId).withFileHandleIds(Arrays.asList(123L, 789L));
		
		// Call under test
		ScannedFileHandleAssociation result = scannedMapper.mapRow(mockResultSet, 0);
		
		assertEquals(expected, result);
	}
}
