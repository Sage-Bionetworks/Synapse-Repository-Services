package org.sagebionetworks.repo.manager.file;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.model.file.BatchFileHandleCopyRequest;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.FileHandleCopyRequest;
import org.sagebionetworks.repo.model.file.S3FileHandle;

public class FileHandleCopyUtilsTest {

	@Mock
	private IdGenerator mockIdGenerator;

	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetOriginalFilesWithNullBatch() {
		FileHandleCopyUtils.getOriginalFiles(null);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetOriginalFilesWithNullRequestList() {
		FileHandleCopyUtils.getOriginalFiles(new BatchFileHandleCopyRequest());
	}

	@Test
	public void testGetOriginalFilesWithEmptyRequestList() {
		BatchFileHandleCopyRequest batch = new BatchFileHandleCopyRequest();
		batch.setCopyRequests(new ArrayList<FileHandleCopyRequest>(0));
		assertEquals(new LinkedList<FileHandleAssociation>(),
				FileHandleCopyUtils.getOriginalFiles(batch));
	}

	@Test
	public void testGetOriginalFiles() {
		BatchFileHandleCopyRequest batch = new BatchFileHandleCopyRequest();
		List<FileHandleCopyRequest> requests = new ArrayList<FileHandleCopyRequest>(2);
		batch.setCopyRequests(requests);

		FileHandleAssociation fha1 = new FileHandleAssociation();
		fha1.setAssociateObjectId("1");
		fha1.setAssociateObjectType(FileHandleAssociateType.FileEntity);
		fha1.setFileHandleId("1");
		FileHandleAssociation fha2 = new FileHandleAssociation();
		fha2.setAssociateObjectId("2");
		fha2.setAssociateObjectType(FileHandleAssociateType.TableEntity);
		fha2.setFileHandleId("2");

		FileHandleCopyRequest request1 = new FileHandleCopyRequest();
		request1.setOriginalFile(fha1);
		FileHandleCopyRequest request2 = new FileHandleCopyRequest();
		request2.setOriginalFile(fha2);
		requests.add(request1);
		requests.add(request2);

		LinkedList<FileHandleAssociation> expected = new LinkedList<FileHandleAssociation>();
		expected.add(fha1);
		expected.add(fha2);
		assertEquals(expected, FileHandleCopyUtils.getOriginalFiles(batch));
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetFileHandleOverrideDataWithNullBatch() {
		FileHandleCopyUtils.getFileHandleOverwriteData(null);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetFileHandleOverrideDataWithNullRequestList() {
		FileHandleCopyUtils.getFileHandleOverwriteData(new BatchFileHandleCopyRequest());
	}

	@Test
	public void testGetFileHandleOverrideDataWithEmptyRequestList() {
		BatchFileHandleCopyRequest batch = new BatchFileHandleCopyRequest();
		batch.setCopyRequests(new ArrayList<FileHandleCopyRequest>(0));
		assertEquals(new HashMap<String, FileHandleOverwriteData>(),
				FileHandleCopyUtils.getFileHandleOverwriteData(batch));
	}

	@Test
	public void testGetFileHandleOverrideData() {
		BatchFileHandleCopyRequest batch = new BatchFileHandleCopyRequest();
		List<FileHandleCopyRequest> requests = new ArrayList<FileHandleCopyRequest>(2);
		batch.setCopyRequests(requests);

		FileHandleAssociation fha1 = new FileHandleAssociation();
		fha1.setAssociateObjectId("1");
		fha1.setAssociateObjectType(FileHandleAssociateType.FileEntity);
		fha1.setFileHandleId("1");
		FileHandleAssociation fha2 = new FileHandleAssociation();
		fha2.setAssociateObjectId("2");
		fha2.setAssociateObjectType(FileHandleAssociateType.TableEntity);
		fha2.setFileHandleId("2");
		FileHandleAssociation fha3 = new FileHandleAssociation();
		fha3.setAssociateObjectId("3");
		fha3.setAssociateObjectType(FileHandleAssociateType.WikiAttachment);
		fha3.setFileHandleId("3");

		FileHandleCopyRequest request1 = new FileHandleCopyRequest();
		request1.setOriginalFile(fha1);
		String newFileName1 = "newFileName1";
		request1.setNewFileName(newFileName1);
		FileHandleCopyRequest request2 = new FileHandleCopyRequest();
		request2.setOriginalFile(fha2);
		String newContentType2 = "newContentType2";
		request2.setNewContentType(newContentType2);
		FileHandleCopyRequest request3 = new FileHandleCopyRequest();
		request3.setOriginalFile(fha3);

		requests.add(request1);
		requests.add(request2);
		requests.add(request3);

		FileHandleOverwriteData data1 = new FileHandleOverwriteData();
		data1.setNewFileName(newFileName1);
		FileHandleOverwriteData data2 = new FileHandleOverwriteData();
		data2.setNewContentType(newContentType2);

		Map<String, FileHandleOverwriteData> expected = new HashMap<String, FileHandleOverwriteData>();
		expected.put("1", data1);
		expected.put("2", data2);
		expected.put("3", new FileHandleOverwriteData());
		assertEquals(expected, FileHandleCopyUtils.getFileHandleOverwriteData(batch));
	}

	@Test (expected=IllegalArgumentException.class)
	public void testCreateCopyWithNullUserId() {
		FileHandleCopyUtils.createCopy(null, new S3FileHandle(), new FileHandleOverwriteData(), mockIdGenerator);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testCreateCopyWithNullFileHandle() {
		FileHandleCopyUtils.createCopy("1", null, new FileHandleOverwriteData(), mockIdGenerator);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testCreateCopyWithNullFileHandleOverwriteData() {
		FileHandleCopyUtils.createCopy("1", new S3FileHandle(), null, mockIdGenerator);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testCreateCopyWithNullIdGenerator() {
		FileHandleCopyUtils.createCopy("1", new S3FileHandle(), new FileHandleOverwriteData(), null);
	}

	@Test
	public void testCreateCopy() {
		when(mockIdGenerator.generateNewId(TYPE.FILE_IDS)).thenReturn(2L);
		S3FileHandle fileHandle = new S3FileHandle();
		String oldId = "1";
		fileHandle.setId(oldId);
		String originalOwner = "999";
		fileHandle.setCreatedBy(originalOwner);
		Date oldCreationDate = new Date();
		fileHandle.setCreatedOn(oldCreationDate);
		String oldEtag = UUID.randomUUID().toString();
		fileHandle.setEtag(oldEtag);
		String oldFileName = "oldFileName";
		fileHandle.setFileName(oldFileName);
		String oldContentType = "oldContentType";
		fileHandle.setContentType(oldContentType);

		FileHandleOverwriteData overwriteData = new FileHandleOverwriteData();
		String newFileName = "newFileName";
		overwriteData.setNewFileName(newFileName);
		String newOwner = "777";

		FileHandle newFileHandle = FileHandleCopyUtils.createCopy(newOwner, fileHandle, overwriteData, mockIdGenerator);
		assertEquals("2", newFileHandle.getId());
		assertNotNull(newFileHandle.getEtag());
		assertFalse(newFileHandle.getEtag().equals(oldEtag));
		assertNotNull(newFileHandle.getCreatedOn());
		assertFalse(newFileHandle.getCreatedOn().equals(oldCreationDate));
		assertEquals(newOwner, newFileHandle.getCreatedBy());
		assertEquals(newFileName, newFileHandle.getFileName());
		assertEquals(oldContentType, newFileHandle.getContentType());
	}

	@Test (expected=IllegalArgumentException.class)
	public void testHasDuplicatesWithNullList() {
		FileHandleCopyUtils.hasDuplicates(null);
	}

	@Test
	public void testHasDuplicatesWithEmptyList() {
		assertFalse(FileHandleCopyUtils.hasDuplicates(new ArrayList<FileHandleAssociation>(0)));
	}

	@Test
	public void testHasDuplicatesWithOneElementList() {
		assertFalse(FileHandleCopyUtils.hasDuplicates(Arrays.asList(new FileHandleAssociation())));
	}

	@Test
	public void testHasDuplicatesCaseNoDuplicates() {
		FileHandleAssociation fha1 = new FileHandleAssociation();
		fha1.setAssociateObjectId("1");
		fha1.setAssociateObjectType(FileHandleAssociateType.FileEntity);
		fha1.setFileHandleId("1");
		FileHandleAssociation fha2 = new FileHandleAssociation();
		fha2.setAssociateObjectId("2");
		fha2.setAssociateObjectType(FileHandleAssociateType.TableEntity);
		fha2.setFileHandleId("2");
		FileHandleAssociation fha3 = new FileHandleAssociation();
		fha3.setAssociateObjectId("3");
		fha3.setAssociateObjectType(FileHandleAssociateType.WikiAttachment);
		fha3.setFileHandleId("3");
		assertFalse(FileHandleCopyUtils.hasDuplicates(Arrays.asList(fha1, fha2, fha3)));
	}

	@Test
	public void testHasDuplicatesCaseWithDuplicates() {
		FileHandleAssociation fha1 = new FileHandleAssociation();
		fha1.setAssociateObjectId("1");
		fha1.setAssociateObjectType(FileHandleAssociateType.FileEntity);
		fha1.setFileHandleId("1");
		FileHandleAssociation fha2 = new FileHandleAssociation();
		fha2.setAssociateObjectId("2");
		fha2.setAssociateObjectType(FileHandleAssociateType.TableEntity);
		fha2.setFileHandleId("2");
		FileHandleAssociation fha3 = new FileHandleAssociation();
		fha3.setAssociateObjectId("3");
		fha3.setAssociateObjectType(FileHandleAssociateType.WikiAttachment);
		fha3.setFileHandleId("3");
		assertTrue(FileHandleCopyUtils.hasDuplicates(Arrays.asList(fha1, fha2, fha3, fha2)));
	}
}
