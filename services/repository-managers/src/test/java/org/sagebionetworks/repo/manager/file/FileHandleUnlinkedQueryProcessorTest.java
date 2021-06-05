package org.sagebionetworks.repo.manager.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.athena.RowMapper;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandleStatus;

import com.amazonaws.services.athena.model.Datum;
import com.amazonaws.services.athena.model.Row;

@ExtendWith(MockitoExtension.class)
public class FileHandleUnlinkedQueryProcessorTest {

	@Mock
	private FileHandleDao mockFileHandleDao;
	
	@InjectMocks
	private FileHandleUnlinkedQueryProcessor processor;

	@Test
	public void testGetQueryName() {
		assertEquals("UnlinkedFileHandles", processor.getQueryName());
	}
	
	@Test
	public void testGetRowMapper() {
		RowMapper<Long> rowMapper = processor.getRowMapper();
		
		Long result = rowMapper.mapRow(new Row().withData(new Datum().withVarCharValue("1")));
		
		assertEquals(1L, result);
	}
	
	@Test
	public void testProcessResultPage() {
		
		List<Long> resultsPage = Arrays.asList(1L, 2L, 3L);
		
		processor.processQueryResultsPage(resultsPage);
		
		verify(mockFileHandleDao).updateBatchStatus(resultsPage, FileHandleStatus.UNLINKED, FileHandleStatus.AVAILABLE, FileHandleUnlinkedQueryProcessor.UPDATED_ON_DAYS_LIMIT);
	}

}
