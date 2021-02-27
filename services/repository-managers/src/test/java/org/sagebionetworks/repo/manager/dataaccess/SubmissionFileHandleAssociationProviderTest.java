package org.sagebionetworks.repo.manager.dataaccess;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.SubmissionDAO;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@ExtendWith(MockitoExtension.class)
public class SubmissionFileHandleAssociationProviderTest {

	@Mock
	private SubmissionDAO mockSubmissionDao;
	
	@Mock
	private Submission mockSubmission;
	
	@Mock
	private JdbcTemplate mockJdbcTemplate;
	
	@Mock
	private NamedParameterJdbcTemplate mockNamedJdbcTemplate;
	
	@InjectMocks
	private SubmissionFileHandleAssociationProvider provider;
	
	@Test
	public void testGetFileHandleIdsAssociatedWithObjectForACTAccessRequirement() {
		String SubmissionId = "1";
		String duc = "2";
		String irb = "3";
		String other = "4";
		when(mockSubmission.getDucFileHandleId()).thenReturn(duc);
		when(mockSubmission.getIrbFileHandleId()).thenReturn(irb);
		when(mockSubmission.getAttachments()).thenReturn(Arrays.asList(other));
		when(mockSubmissionDao.getSubmission(SubmissionId)).thenReturn(mockSubmission);
		Set<String> associated = provider.getFileHandleIdsDirectlyAssociatedWithObject(
				Arrays.asList(duc, other, "5"), SubmissionId);
		assertTrue(associated.contains(duc));
		assertFalse(associated.contains(irb));
		assertTrue(associated.contains(other));
		assertFalse(associated.contains("5"));
	}

	@Test
	public void testGetFileHandleIdsAssociatedWithObjectForACTAccessRequirementNullAttachment() {
		String SubmissionId = "1";
		String duc = "2";
		String irb = "3";
		when(mockSubmission.getDucFileHandleId()).thenReturn(duc);
		when(mockSubmission.getIrbFileHandleId()).thenReturn(irb);
		when(mockSubmissionDao.getSubmission(SubmissionId)).thenReturn(mockSubmission);
		Set<String> associated = provider.getFileHandleIdsDirectlyAssociatedWithObject(
				Arrays.asList(duc, "5"), SubmissionId);
		assertTrue(associated.contains(duc));
		assertFalse(associated.contains(irb));
		assertFalse(associated.contains("5"));
	}

	@Test
	public void testGetObjectTypeForAssociatedType() {
		assertEquals(ObjectType.DATA_ACCESS_SUBMISSION, provider.getAuthorizationObjectTypeForAssociatedObjectType());
	}
	
	@Test
	public void testGetAssociateType() {
		assertEquals(FileHandleAssociateType.DataAccessSubmissionAttachment, provider.getAssociateType());
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
		
		verify(mockJdbcTemplate).queryForObject("SELECT MIN(`ID`), MAX(`ID`) FROM DATA_ACCESS_SUBMISSION", null, BasicFileHandleAssociationScanner.ID_RANGE_MAPPER);
		
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
		
		verify(mockNamedJdbcTemplate, times(2)).query(eq("SELECT `ID`, `SUBMISSION_SERIALIZED` FROM DATA_ACCESS_SUBMISSION WHERE `ID` BETWEEN :BMINID AND :BMAXID AND SUBMISSION_SERIALIZED IS NOT NULL ORDER BY `ID` LIMIT :KEY_LIMIT OFFSET :KEY_OFFSET"), anyMap(),  any(RowMapper.class));
	}
	
}
