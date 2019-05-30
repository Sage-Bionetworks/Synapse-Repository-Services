package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.util.FileProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(MockitoJUnitRunner.class)
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
	File mockFile;
	@Mock
	OutputStream mockOutputStream;
	@Mock
	WriterCallback mockCallback;
	
	String s3Bucket;
	
	@InjectMocks
	TableRowTruthDAOImpl dao;
	
	@Before
	public void before() throws IOException {
		when(mockFileProvider.createTempFile(anyString(), anyString())).thenReturn(mockFile);
		when(mockFileProvider.createFileOutputStream(any(File.class))).thenReturn(mockOutputStream);
		s3Bucket = "a.bucket";
		ReflectionTestUtils.setField(dao, "s3Bucket",s3Bucket);
	}
	
	@Test
	public void testSaveToS3() throws IOException {
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
		when(mockFileProvider.createFileOutputStream(any(File.class))).thenThrow(exception);
		try {
			// call under test
			dao.saveToS3(mockCallback);
			fail();
		} catch(RuntimeException e) {
			assertEquals(e.getCause(), exception);
		}
		// temp should still be deleted.
		verify(mockFile).delete();
	}
	
}
