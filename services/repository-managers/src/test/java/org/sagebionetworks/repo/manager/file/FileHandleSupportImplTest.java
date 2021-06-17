package org.sagebionetworks.repo.manager.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;
import static org.sagebionetworks.repo.model.file.FileHandleAssociateType.FileEntity;
import static org.sagebionetworks.repo.model.file.FileHandleAssociateType.TableEntity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.BulkFileDownloadRequest;
import org.sagebionetworks.repo.model.file.BulkFileDownloadResponse;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileDownloadCode;
import org.sagebionetworks.repo.model.file.FileDownloadStatus;
import org.sagebionetworks.repo.model.file.FileDownloadSummary;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.ZipFileFormat;
import org.sagebionetworks.repo.model.jdo.NameValidation;
import org.sagebionetworks.repo.web.NotFoundException;

import com.amazonaws.services.s3.model.GetObjectRequest;
import com.google.api.client.googleapis.media.MediaHttpDownloader.DownloadState;
import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class FileHandleSupportImplTest {

	@Mock
	private FileHandleDao mockFileHandleDao;
	@Mock
	private SynapseS3Client mockS3client;
	@Mock
	private FileHandleAuthorizationManager mockFileHandleAuthorizationManager;
	@Mock
	private FileHandleManager mockFileHandleManager;
	@Mock
	private File mockTempFile;
	@Mock
	private ZipOutputStream mockZipOut;
	@Captor
	private ArgumentCaptor<Set<String>> filesInZipCaptor;
	@Captor
	private ArgumentCaptor<ZipEntryNameProvider> zipEntryNameProviderCaptor;

	@Spy
	@InjectMocks
	private FileHandleSupportImpl fileHandleSupportSpy;

	private UserInfo userInfo;
	private List<FileHandleAssociation> associations;
	private BulkFileDownloadRequest request;
	private List<FileHandleAssociationAuthorizationStatus> authResults;
	private List<FileDownloadSummary> summaryResults;
	private S3FileHandle resultFileHandle;

	@BeforeEach
	public void before() {
		boolean isAdmin = false;
		long userId = 123L;
		userInfo = new UserInfo(isAdmin, userId);
		// @formatter:off
		associations = Arrays.asList(
				new FileHandleAssociation().setAssociateObjectId("syn456").setAssociateObjectType(FileEntity).setFileHandleId("11"),
				new FileHandleAssociation().setAssociateObjectId("syn780").setAssociateObjectType(TableEntity).setFileHandleId("22")
		);
		authResults = Arrays.asList(
				new FileHandleAssociationAuthorizationStatus(associations.get(0), AuthorizationStatus.authorized()),
				new FileHandleAssociationAuthorizationStatus(associations.get(1), AuthorizationStatus.accessDenied("no"))
		);
		summaryResults =  Arrays.asList(
				new FileDownloadSummary().setFileHandleId("11"),
				new FileDownloadSummary().setFileHandleId("22")
		);
		// @formatter:on
		resultFileHandle = new S3FileHandle().setBucketName("prod.bucket").setKey("some-key").setId("3333");

		request = new BulkFileDownloadRequest().setZipFileFormat(ZipFileFormat.Flat).setZipFileName("My.zip")
				.setRequestedFiles(associations);
	}

	@Test
	public void testZipRoundTrip() throws IOException {
		File one = null;
		File two = null;
		File zip = null;
		FileOutputStream oneOut = null;
		FileOutputStream twoOut = null;
		ZipOutputStream zipOut = null;
		ZipInputStream zipIn = null;
		try {
			String oneContents = "data for one";
			String twoContents = "data for two";
			// create one.
			one = fileHandleSupportSpy.createTempFile("One", ".txt");
			oneOut = new FileOutputStream(one);
			IOUtils.write(oneContents, oneOut);
			oneOut.close();
			// create two
			two = fileHandleSupportSpy.createTempFile("two", ".txt");
			twoOut = new FileOutputStream(two);
			IOUtils.write(twoContents, twoOut);
			// The output zip
			zip = fileHandleSupportSpy.createTempFile("Zip", ".zip");
			zipOut = fileHandleSupportSpy.createZipOutputStream(zip);

			// add the files to the zip.
			String entryNameOne = "p1/One.txt";
			fileHandleSupportSpy.addFileToZip(zipOut, one, entryNameOne);
			String entryNameTwo = "p2/Two.txt";
			fileHandleSupportSpy.addFileToZip(zipOut, two, entryNameTwo);
			zipOut.close();

			// unzip
			zipIn = new ZipInputStream(new FileInputStream(zip));
			ZipEntry entry = zipIn.getNextEntry();
			assertEquals(entryNameOne, entry.getName());
			assertEquals(oneContents, IOUtils.toString(zipIn));
			zipIn.closeEntry();
			entry = zipIn.getNextEntry();
			assertEquals(entryNameTwo, entry.getName());
			assertEquals(twoContents, IOUtils.toString(zipIn));
			zipIn.closeEntry();

		} finally {
			IOUtils.closeQuietly(oneOut);
			IOUtils.closeQuietly(twoOut);
			IOUtils.closeQuietly(zipOut);
			IOUtils.closeQuietly(zipIn);
			if (one != null) {
				one.delete();
			}
			if (two != null) {
				two.delete();
			}
			if (zip != null) {
				zip.delete();
			}
		}
	}

	@Test
	public void testGetS3FileHandle() {
		String fileHandleId = "123";
		S3FileHandle s3Handle = new S3FileHandle();
		s3Handle.setId(fileHandleId);

		when(mockFileHandleDao.get(fileHandleId)).thenReturn(s3Handle);
		S3FileHandle result = fileHandleSupportSpy.getS3FileHandle(fileHandleId);
		assertEquals(s3Handle, result);
	}

	@Test
	public void testGetS3FileHandleNotS3() {
		String fileHandleId = "123";
		ExternalFileHandle handle = new ExternalFileHandle();
		handle.setId(fileHandleId);
		when(mockFileHandleDao.get(fileHandleId)).thenReturn(handle);
		assertThrows(IllegalArgumentException.class, () -> {
			fileHandleSupportSpy.getS3FileHandle(fileHandleId);
		});
	}

	@Test
	public void testDownloadToTempFile() throws IOException {
		String fileHandleId = "123";
		S3FileHandle s3Handle = new S3FileHandle();
		s3Handle.setId(fileHandleId);
		s3Handle.setKey("someKey");
		s3Handle.setBucketName("someBucket");

		File result = null;
		try {
			result = fileHandleSupportSpy.downloadToTempFile(s3Handle);
			assertNotNull(result);
			verify(mockS3client).getObject(any(GetObjectRequest.class), any(File.class));
		} finally {
			if (result != null) {
				result.delete();
			}
		}
	}

	@Test
	public void testBuildZip() throws IOException {
		doReturn(mockTempFile).when(fileHandleSupportSpy).createTempFile(any(), any());
		doReturn(summaryResults).when(fileHandleSupportSpy).addFilesToZip(any(), any(), any());
		when(mockTempFile.length()).thenReturn(1L);
		when(mockFileHandleManager.uploadLocalFile(any())).thenReturn(resultFileHandle);

		// call under test
		BulkFileDownloadResponse response = fileHandleSupportSpy.buildZip(userInfo, request);

		BulkFileDownloadResponse expected = new BulkFileDownloadResponse().setUserId(userInfo.getId().toString())
				.setFileSummary(summaryResults).setResultZipFileHandleId(resultFileHandle.getId());
		assertEquals(expected, response);
		verify(fileHandleSupportSpy).createTempFile("Job", ".zip");
		verify(fileHandleSupportSpy).addFilesToZip(userInfo, request, mockTempFile);
		verify(mockFileHandleManager).uploadLocalFile(new LocalFileUploadRequest()
				.withFileName(request.getZipFileName()).withUserId(userInfo.getId().toString())
				.withFileToUpload(mockTempFile).withContentType(FileHandleSupportImpl.APPLICATION_ZIP));
		verify(mockTempFile).delete();
	}

	@Test
	public void testBuildZipWithEmptyFile() throws IOException {
		doReturn(mockTempFile).when(fileHandleSupportSpy).createTempFile(any(), any());
		doReturn(summaryResults).when(fileHandleSupportSpy).addFilesToZip(any(), any(), any());
		// no data in the file
		when(mockTempFile.length()).thenReturn(0L);

		// call under test
		BulkFileDownloadResponse response = fileHandleSupportSpy.buildZip(userInfo, request);

		BulkFileDownloadResponse expected = new BulkFileDownloadResponse().setUserId(userInfo.getId().toString())
				.setFileSummary(summaryResults).setResultZipFileHandleId(null);
		assertEquals(expected, response);
		verify(fileHandleSupportSpy).createTempFile("Job", ".zip");
		verify(fileHandleSupportSpy).addFilesToZip(userInfo, request, mockTempFile);
		verify(mockFileHandleManager, never()).uploadLocalFile(any());
		verify(fileHandleSupportSpy).collectDownloadStatistics(userInfo.getId(), summaryResults);
		verify(mockTempFile).delete();
	}

	@Test
	public void testBuildZipWithUnsupportedName() throws IOException {
		request.setZipFileName("ContainsNonÃ¢II.zip");
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			fileHandleSupportSpy.buildZip(userInfo, request);
		}).getMessage();
		assertEquals(NameValidation.createInvalidMessage(request.getZipFileName()), message);

		verify(fileHandleSupportSpy, never()).createTempFile(any(), any());
		verify(fileHandleSupportSpy, never()).addFilesToZip(any(), any(), any());
		verify(mockFileHandleManager, never()).uploadLocalFile(any());
	}

	@Test
	public void testBuildZipWithNullName() throws IOException {
		request.setZipFileName(null);

		doReturn(mockTempFile).when(fileHandleSupportSpy).createTempFile(any(), any());
		doReturn(summaryResults).when(fileHandleSupportSpy).addFilesToZip(any(), any(), any());
		when(mockTempFile.length()).thenReturn(1L);
		when(mockFileHandleManager.uploadLocalFile(any())).thenReturn(resultFileHandle);

		// call under test
		BulkFileDownloadResponse response = fileHandleSupportSpy.buildZip(userInfo, request);

		BulkFileDownloadResponse expected = new BulkFileDownloadResponse().setUserId(userInfo.getId().toString())
				.setFileSummary(summaryResults).setResultZipFileHandleId(resultFileHandle.getId());
		assertEquals(expected, response);
		verify(fileHandleSupportSpy).createTempFile("Job", ".zip");
		verify(fileHandleSupportSpy).addFilesToZip(userInfo, request, mockTempFile);
		verify(mockFileHandleManager).uploadLocalFile(new LocalFileUploadRequest()
				.withFileName(request.getZipFileName()).withUserId(userInfo.getId().toString())
				.withFileToUpload(mockTempFile).withContentType(FileHandleSupportImpl.APPLICATION_ZIP));
		verify(mockTempFile).delete();
	}

	@Test
	public void testBuildZipWithExceptionDeleteFile() throws IOException {
		doReturn(mockTempFile).when(fileHandleSupportSpy).createTempFile(any(), any());
		IllegalArgumentException exception = new IllegalArgumentException("not working");
		doThrow(exception).when(fileHandleSupportSpy).addFilesToZip(any(), any(), any());
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			fileHandleSupportSpy.buildZip(userInfo, request);
		}).getMessage();
		assertEquals(exception.getMessage(), message);

		verify(fileHandleSupportSpy).createTempFile("Job", ".zip");
		verify(fileHandleSupportSpy).addFilesToZip(userInfo, request, mockTempFile);
		verify(mockFileHandleManager, never()).uploadLocalFile(any());
		// the temp file still must be deleted.
		verify(mockTempFile).delete();
	}

	@Test
	public void testAddFilesToZip() throws IOException {
		doReturn(mockZipOut).when(fileHandleSupportSpy).createZipOutputStream(any());
		when(mockFileHandleAuthorizationManager.canDownLoadFile(any(), any())).thenReturn(authResults);
		doReturn("one.txt", "two.txt").when(fileHandleSupportSpy).writeOneFileToZip(any(), anyLong(), any(), any(),
				any());
		when(mockTempFile.length()).thenReturn(25L,125L);

		// call under test
		List<FileDownloadSummary> summary = fileHandleSupportSpy.addFilesToZip(userInfo, request, mockTempFile);
		// @formatter:off
		List<FileDownloadSummary> expected = Arrays.asList(
				createSummary(associations.get(0)).setZipEntryName("one.txt").setStatus(FileDownloadStatus.SUCCESS),
				createSummary(associations.get(1)).setZipEntryName("two.txt").setStatus(FileDownloadStatus.SUCCESS)
		);
		// @formatter:on
		assertEquals(expected, summary);

		verify(fileHandleSupportSpy).createZipOutputStream(mockTempFile);
		verify(fileHandleSupportSpy).writeOneFileToZip(eq(mockZipOut), eq(25L), eq(authResults.get(0)), filesInZipCaptor.capture(), zipEntryNameProviderCaptor.capture());
		verify(fileHandleSupportSpy).writeOneFileToZip(eq(mockZipOut), eq(125L), eq(authResults.get(1)), filesInZipCaptor.capture(), zipEntryNameProviderCaptor.capture());
		assertEquals(Sets.newHashSet("11","22"),  filesInZipCaptor.getValue());
		assertTrue(zipEntryNameProviderCaptor.getValue() instanceof FlatZipEntryNameProvider);
		verify(mockZipOut).close();
	}
	
	@Test
	public void testAddFilesToZipWithBulkFileException() throws IOException {
		doReturn(mockZipOut).when(fileHandleSupportSpy).createZipOutputStream(any());
		authResults = Arrays.asList(
				new FileHandleAssociationAuthorizationStatus(associations.get(0), AuthorizationStatus.authorized()));
		when(mockFileHandleAuthorizationManager.canDownLoadFile(any(), any())).thenReturn(authResults);

		BulkFileException exception = new BulkFileException("not found", FileDownloadCode.NOT_FOUND);
		doThrow(exception).when(fileHandleSupportSpy).writeOneFileToZip(any(), anyLong(), any(), any(), any());
		when(mockTempFile.length()).thenReturn(25L, 125L);

		// call under test
		List<FileDownloadSummary> summary = fileHandleSupportSpy.addFilesToZip(userInfo, request, mockTempFile);

		List<FileDownloadSummary> expected = Arrays
				.asList(createSummary(associations.get(0)).setStatus(FileDownloadStatus.FAILURE)
						.setFailureMessage(exception.getMessage()).setFailureCode(FileDownloadCode.NOT_FOUND));
		assertEquals(expected, summary);

		verify(fileHandleSupportSpy).createZipOutputStream(mockTempFile);
		verify(fileHandleSupportSpy).writeOneFileToZip(eq(mockZipOut), eq(25L), eq(authResults.get(0)),
				filesInZipCaptor.capture(), zipEntryNameProviderCaptor.capture());
		assertEquals(Collections.emptySet(), filesInZipCaptor.getValue());
		assertTrue(zipEntryNameProviderCaptor.getValue() instanceof FlatZipEntryNameProvider);
		verify(mockZipOut).close();
	}
	
	@Test
	public void testAddFilesToZipWithNotFoundException() throws IOException {
		doReturn(mockZipOut).when(fileHandleSupportSpy).createZipOutputStream(any());
		authResults = Arrays.asList(
				new FileHandleAssociationAuthorizationStatus(associations.get(0), AuthorizationStatus.authorized()));
		when(mockFileHandleAuthorizationManager.canDownLoadFile(any(), any())).thenReturn(authResults);

		NotFoundException exception = new NotFoundException("not found");
		doThrow(exception).when(fileHandleSupportSpy).writeOneFileToZip(any(), anyLong(), any(), any(), any());
		when(mockTempFile.length()).thenReturn(25L, 125L);

		// call under test
		List<FileDownloadSummary> summary = fileHandleSupportSpy.addFilesToZip(userInfo, request, mockTempFile);

		List<FileDownloadSummary> expected = Arrays
				.asList(createSummary(associations.get(0)).setStatus(FileDownloadStatus.FAILURE)
						.setFailureMessage(exception.getMessage()).setFailureCode(FileDownloadCode.NOT_FOUND));
		assertEquals(expected, summary);

		verify(fileHandleSupportSpy).createZipOutputStream(mockTempFile);
		verify(fileHandleSupportSpy).writeOneFileToZip(eq(mockZipOut), eq(25L), eq(authResults.get(0)),
				filesInZipCaptor.capture(), zipEntryNameProviderCaptor.capture());
		assertEquals(Collections.emptySet(), filesInZipCaptor.getValue());
		assertTrue(zipEntryNameProviderCaptor.getValue() instanceof FlatZipEntryNameProvider);
		verify(mockZipOut).close();
	}
	
	@Test
	public void testAddFilesToZipWithException() throws IOException {
		doReturn(mockZipOut).when(fileHandleSupportSpy).createZipOutputStream(any());
		authResults = Arrays.asList(
				new FileHandleAssociationAuthorizationStatus(associations.get(0), AuthorizationStatus.authorized()));
		when(mockFileHandleAuthorizationManager.canDownLoadFile(any(), any())).thenReturn(authResults);

		RuntimeException exception = new RuntimeException("something else");
		doThrow(exception).when(fileHandleSupportSpy).writeOneFileToZip(any(), anyLong(), any(), any(), any());
		when(mockTempFile.length()).thenReturn(25L, 125L);

		// call under test
		List<FileDownloadSummary> summary = fileHandleSupportSpy.addFilesToZip(userInfo, request, mockTempFile);

		List<FileDownloadSummary> expected = Arrays
				.asList(createSummary(associations.get(0)).setStatus(FileDownloadStatus.FAILURE)
						.setFailureMessage(exception.getMessage()).setFailureCode(FileDownloadCode.UNKNOWN_ERROR));
		assertEquals(expected, summary);

		verify(fileHandleSupportSpy).createZipOutputStream(mockTempFile);
		verify(fileHandleSupportSpy).writeOneFileToZip(eq(mockZipOut), eq(25L), eq(authResults.get(0)),
				filesInZipCaptor.capture(), zipEntryNameProviderCaptor.capture());
		assertEquals(Collections.emptySet(), filesInZipCaptor.getValue());
		assertTrue(zipEntryNameProviderCaptor.getValue() instanceof FlatZipEntryNameProvider);
		verify(mockZipOut).close();
	}
	
	public static FileDownloadSummary createSummary(FileHandleAssociation association) {
		return new FileDownloadSummary().setAssociateObjectId(association.getAssociateObjectId())
				.setAssociateObjectType(association.getAssociateObjectType())
				.setFileHandleId(association.getFileHandleId());
	}
}
