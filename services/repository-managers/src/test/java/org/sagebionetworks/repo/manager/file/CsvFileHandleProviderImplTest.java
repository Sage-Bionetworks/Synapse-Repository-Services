package org.sagebionetworks.repo.manager.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;

import au.com.bytecode.opencsv.CSVReader;

@ExtendWith(MockitoExtension.class)
public class CsvFileHandleProviderImplTest {

	@Mock
	private FileHandleManager mockFileHandleManager;
	@Mock
	private BucketObjectReaderProvider mockFileReaderProvider;
	@Mock
	private BucketObjectReader mockBucketObjectReader;
	@InjectMocks
	private CsvFileHandleProviderImpl provider;

	private String fileHandleId;
	private UserInfo user;
	private CsvTableDescriptor descriptor;
	private CloudProviderFileHandleInterface fileHandle;
	private String csvContent;

	@BeforeEach
	public void before() {
		fileHandleId = "123";
		user = new UserInfo(false, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		descriptor = new CsvTableDescriptor();
		fileHandle = new S3FileHandle().setId(fileHandleId).setBucketName("bucket").setKey("key");
		csvContent = 
			"a,b,c,d" + System.lineSeparator() + 
			"1,2,3,4" + System.lineSeparator();
	}
	
	@Test
	void testGetCsvReader() throws IOException {
		when(mockFileReaderProvider.getBucketObjectReader(S3FileHandle.class)).thenReturn(mockBucketObjectReader);
		when(mockBucketObjectReader.openStream(fileHandle.getBucketName(), fileHandle.getKey())).thenReturn(
			new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8))
		);

		// Call under test
		CSVReader reader = provider.getCsvReader(fileHandle, descriptor);

		assertEquals(csvContent, readAll(reader));
	}
	
	@Test
	void testGetCsvReaderWithNoFileHandle() throws IOException {
		fileHandle = null;
		
		assertEquals("The fileHandle is required.", assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			provider.getCsvReader(fileHandle, descriptor);
		}).getMessage());
	}
	
	@Test
	void testGetCsvReaderWithNoDescriptor() throws IOException {
		descriptor = null;
		
		assertEquals("The csvDescriptor is required.", assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			provider.getCsvReader(fileHandle, descriptor);
		}).getMessage());
	}
	
	@Test
	void testGetCsvReaderWithWrongFileType() throws IOException {
		FileHandle fileHandle = Mockito.mock(FileHandle.class);
		
		assertEquals("Only S3 and Google Cloud Storage files that Synapse can access are supported.", assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			provider.getCsvReader(fileHandle, descriptor);
		}).getMessage());
	}
	
	@Test
	void testGetCsvReaderByFileHandleId() throws IOException {
		when(mockFileHandleManager.getRawFileHandle(user, fileHandleId)).thenReturn(fileHandle);

		provider = Mockito.spy(provider);
		
		CSVReader expected = Mockito.mock(CSVReader.class);

		doReturn(expected).when(provider).getCsvReader(fileHandle, descriptor);
		
		// Call under test
		assertEquals(expected, provider.getCsvReader(user, fileHandleId, descriptor));

	}
	
	@Test
	void testGetCsvReaderByFileHandleIdWithNoFileHandleId() throws IOException {
		
		fileHandleId = null;
		
		assertEquals("The fileHandleId is required.", assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			provider.getCsvReader(user, fileHandleId, descriptor);
		}).getMessage());
	}
	
	@Test
	void testGetCsvReaderByFileHandleIdWithNoUser() throws IOException {
		
		user = null;
		
		assertEquals("The user is required.", assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			provider.getCsvReader(user, fileHandleId, descriptor);
		}).getMessage());
	}

	private static String readAll(CSVReader reader) throws IOException {
		StringBuilder sb = new StringBuilder();
		String[] line;
		while ((line = reader.readNext()) != null) {
			sb.append(String.join(",", line)).append(System.lineSeparator());
		}
		return sb.toString();
	}
}