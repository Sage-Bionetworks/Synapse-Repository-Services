package org.sagebionetworks.repo.manager.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.file.BatchFileHandleCopyRequest;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.FileHandleCopyRequest;
import org.sagebionetworks.repo.model.file.S3FileHandle;

public class FileHandleCopyUtilsTest {

	@Test
	public void testGetOriginalFilesWithNullBatch() {
		assertThrows(IllegalArgumentException.class, () -> {			
				FileHandleCopyUtils.getOriginalFiles(null);
		});
	}

	@Test
	public void testGetOriginalFilesWithNullRequestList() {
		assertThrows(IllegalArgumentException.class, () -> {
			FileHandleCopyUtils.getOriginalFiles(new BatchFileHandleCopyRequest());
		});
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

	@Test
	public void testGetRequestMapWithNullBatch() {
		assertThrows(IllegalArgumentException.class, () -> {			 
			FileHandleCopyUtils.getRequestMap(null);
		});
	}

	@Test
	public void testGetRequestMapWithNullRequestList() {
		assertThrows(IllegalArgumentException.class, () -> {
			FileHandleCopyUtils.getRequestMap(new BatchFileHandleCopyRequest());
		});
	}

	@Test
	public void testGetRequestMapWithEmptyRequestList() {
		BatchFileHandleCopyRequest batch = new BatchFileHandleCopyRequest();
		batch.setCopyRequests(new ArrayList<FileHandleCopyRequest>(0));
		assertEquals(new HashMap<String, FileHandleCopyRequest>(),
				FileHandleCopyUtils.getRequestMap(batch));
	}

	@Test
	public void testGetRequestMap() {
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
		String newFileName1 = "newFileName1";
		request1.setNewFileName(newFileName1);
		FileHandleCopyRequest request2 = new FileHandleCopyRequest();
		request2.setOriginalFile(fha2);
		String newContentType2 = "newContentType2";
		request2.setNewContentType(newContentType2);

		requests.add(request1);
		requests.add(request2);

		Map<String, FileHandleCopyRequest> expected = new HashMap<String, FileHandleCopyRequest>();
		expected.put("1", request1);
		expected.put("2", request2);
		assertEquals(expected, FileHandleCopyUtils.getRequestMap(batch));
	}

	@Test
	public void testCreateCopyWithNullUserId() {
		assertThrows(IllegalArgumentException.class, () -> {
			FileHandleCopyUtils.createCopy(null, new S3FileHandle(), new FileHandleCopyRequest(), "2");
		});
	}

	@Test
	public void testCreateCopyWithNullFileHandle() {
		assertThrows(IllegalArgumentException.class, () -> {
			FileHandleCopyUtils.createCopy("1", null, new FileHandleCopyRequest(), "2");
		});
	}

	@Test
	public void testCreateCopyWithNullFileHandleOverwriteData() {
		assertThrows(IllegalArgumentException.class, () -> {
			FileHandleCopyUtils.createCopy("1", new S3FileHandle(), null, "2");
		});
	}

	@Test
	public void testCreateCopyWithNullIdGenerator() {
		assertThrows(IllegalArgumentException.class, () -> {
			FileHandleCopyUtils.createCopy("1", new S3FileHandle(), new FileHandleCopyRequest(), null);
		});
	}

	@Test
	public void testCreateCopy() throws Exception{
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
		fileHandle.setPreviewId("333");

		FileHandleCopyRequest overwriteData = new FileHandleCopyRequest();
		String newFileName = "newFileName";
		overwriteData.setNewFileName(newFileName);
		String newOwner = "777";

		Thread.sleep(1000);

		FileHandle newFileHandle = FileHandleCopyUtils.createCopy(newOwner, fileHandle, overwriteData, "2");
		assertEquals("2", newFileHandle.getId());
		assertNotNull(newFileHandle.getEtag());
		assertFalse(newFileHandle.getEtag().equals(oldEtag));
		assertNotNull(newFileHandle.getCreatedOn());
		assertFalse(newFileHandle.getCreatedOn().equals(oldCreationDate));
		assertEquals(newOwner, newFileHandle.getCreatedBy());
		assertEquals(newFileName, newFileHandle.getFileName());
		assertEquals(oldContentType, newFileHandle.getContentType());
		assertNull(((CloudProviderFileHandleInterface)newFileHandle).getPreviewId());
	}

	@Test
	public void testHasDuplicatesWithNullList() {
		assertThrows(IllegalArgumentException.class, () -> {
			FileHandleCopyUtils.hasDuplicates(null);
		});
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
		fha3.setFileHandleId("2");
		assertTrue(FileHandleCopyUtils.hasDuplicates(Arrays.asList(fha1, fha2, fha3)));
	}
}
