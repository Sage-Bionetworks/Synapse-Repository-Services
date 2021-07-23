package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.util.FileProvider;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
public class TableRowTruthDAOImplUnitTest {

	@Mock
	DBOBasicDao mockBasicDao;
	@Mock
	JdbcTemplate mockJdbcTemplate;
	@Mock
	SynapseS3Client mockS3Client;
	@Mock
	FileProvider mockFileProvider;
	@Mock
	IdGenerator mockIdGenerator;
	@Mock
	StackConfiguration mockConfig;
	@Mock
	File mockFile;
	@Mock
	OutputStream mockOutputStream;
	@Mock
	WriterCallback mockCallback;
	
	String s3Bucket;
	
	@InjectMocks
	TableRowTruthDAOImpl dao;
	
	@BeforeEach
	public void before() throws IOException {
		s3Bucket = "a.bucket";
		when(mockConfig.getTableRowChangeBucketName()).thenReturn(s3Bucket);
		dao.configure(mockConfig);
	}
	
	@Test
	public void testSaveToS3() throws IOException {
		when(mockFileProvider.createTempFile(anyString(), anyString())).thenReturn(mockFile);
		when(mockFileProvider.createFileOutputStream(any(File.class))).thenReturn(mockOutputStream);
		// Call under test
		dao.saveToS3(mockCallback);
		verify(mockCallback).write(mockOutputStream);
		verify(mockOutputStream).flush();
		verify(mockOutputStream, times(2)).close();
		verify(mockS3Client).putObject(eq(s3Bucket),anyString(), eq(mockFile));
		verify(mockFile).delete();
	}
	
	@Test
	public void testSaveToS3DeleteOnError() throws IOException {
		FileNotFoundException exception = new FileNotFoundException();
		when(mockFileProvider.createTempFile(anyString(), anyString())).thenReturn(mockFile);
		when(mockFileProvider.createFileOutputStream(any(File.class))).thenThrow(exception);
		
		RuntimeException result = assertThrows(RuntimeException.class, () -> {
			// call under test
			dao.saveToS3(mockCallback);
		});

		assertEquals(exception, result.getCause());
		
		// temp should still be deleted.
		verify(mockFile).delete();
	}
	
}
