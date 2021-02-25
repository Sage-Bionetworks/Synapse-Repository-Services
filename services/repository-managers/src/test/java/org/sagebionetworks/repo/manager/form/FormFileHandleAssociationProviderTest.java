package org.sagebionetworks.repo.manager.form;


import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.sagebionetworks.repo.model.dbo.form.FormDao;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class FormFileHandleAssociationProviderTest {
	
	@Mock
	private FormDao mockFormDao;

	@Mock
	private JdbcTemplate mockJdbcTemplate;
	
	@Mock
	private NamedParameterJdbcTemplate mockNamedJdbcTemplate;
	
	@InjectMocks
	private FormFileHandleAssociationProvider provider;
	
	@Test
	public void testgetAuthorizationObjectTypeForAssociatedObjectType() {
		// call under test
		assertEquals(ObjectType.FORM_DATA,  provider.getAuthorizationObjectTypeForAssociatedObjectType());
	}
	
	@Test
	public void testGetFileHandleIdsDirectlyAssociatedWithObject() {
		List<String> handles = Lists.newArrayList("123", "456");
		String formDataId = "888";
		when(mockFormDao.getFormDataFileHandleId(formDataId)).thenReturn("456");
		// call under test
		Set<String> results = provider.getFileHandleIdsDirectlyAssociatedWithObject(handles, formDataId);
		assertEquals(Sets.newHashSet("456"), results);
	}
	
	@Test
	public void testGetFileHandleIdsDirectlyAssociatedWithObjectNot() {
		List<String> handles = Lists.newArrayList("123", "456");
		String formDataId = "888";
		// no match.
		when(mockFormDao.getFormDataFileHandleId(formDataId)).thenReturn("333");
		// call under test
		Set<String> results = provider.getFileHandleIdsDirectlyAssociatedWithObject(handles, formDataId);
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}
	
	@Test
	public void testGetFileHandleIdsDirectlyAssociatedWithObjectNullList() {
		List<String> handles = null;
		String formDataId = "888";
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			provider.getFileHandleIdsDirectlyAssociatedWithObject(handles, formDataId);
		});
	}
	
	@Test
	public void testGetFileHandleIdsDirectlyAssociatedWithObjectNullId() {
		List<String> handles = Lists.newArrayList("123", "456");
		String formDataId = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			provider.getFileHandleIdsDirectlyAssociatedWithObject(handles, formDataId);
		});
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
		
		verify(mockJdbcTemplate).queryForObject("SELECT MIN(`ID`), MAX(`ID`) FROM FORM_DATA", null, BasicFileHandleAssociationScanner.ID_RANGE_MAPPER);
		
	}
	
	@Test
	public void testScannerScanRange() {
		
		IdRange range = new IdRange(1, 10);
		
		List<ScannedFileHandleAssociation> expected = Arrays.asList(new ScannedFileHandleAssociation("123", 123L));
		
		when(mockNamedJdbcTemplate.query(any(), anyMap(), any(RowMapper.class))).thenReturn(expected, Collections.emptyList());
				
		FileHandleAssociationScanner scanner = provider.getAssociationScanner();
		
		assertNotNull(scanner);
		
		// Call under test
		List<ScannedFileHandleAssociation> result = StreamSupport.stream(scanner.scanRange(range).spliterator(), false).collect(Collectors.toList());
		
		assertEquals(expected, result);
		
		verify(mockNamedJdbcTemplate, times(2)).query(eq("SELECT `ID`, `FILE_HANDLE_ID` FROM FORM_DATA WHERE `ID` BETWEEN :BMINID AND :BMAXID AND FILE_HANDLE_ID IS NOT NULL ORDER BY `ID` LIMIT :KEY_LIMIT OFFSET :KEY_OFFSET"), anyMap(), any(RowMapper.class));
	}

}
