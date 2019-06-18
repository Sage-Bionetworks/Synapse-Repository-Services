package org.sagebionetworks.repo.model.dbo.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.file.CloudFileHandleInterface;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadState;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.upload.PartRange;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
class MultipartUploadComposerDAOImplTest {

	@Autowired
	private MultipartUploadComposerDAO multipartUploadComposerDAO;

	@Autowired
	private MultipartUploadDAO multipartUploadDAO;

	@Autowired
	private FileHandleDao fileHandleDao;

	@Autowired
	private IdGenerator idGenerator;

	private CloudFileHandleInterface file;
	private Long userId = AuthorizationConstants.BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
	private String uploadId;

	@BeforeEach
	public void before() throws JSONObjectAdapterException {
		multipartUploadComposerDAO.truncateAll();

		// We need an active multipart request to create parts (foreign key restriction)
		String hash = "someHash";
		Long storageLocationId = null;
		MultipartUploadRequest request = new MultipartUploadRequest();
		request.setFileName("foo.txt");
		request.setFileSizeBytes(123L);
		request.setContentMD5Hex("someMD5Hex");
		request.setContentType("plain/text");
		request.setPartSizeBytes(5L);
		request.setStorageLocationId(storageLocationId);
		String uploadToken = "";
		UploadType uploadType = UploadType.GOOGLECLOUDSTORAGE;
		String bucket = "someBucket";
		String key = "someKey";
		int numberOfParts = 11;
		String requestJSON = EntityFactory.createJSONStringForEntity(request);
		CreateMultipartRequest createRequest = new CreateMultipartRequest(userId, hash, requestJSON, uploadToken, uploadType, bucket, key, numberOfParts);

		uploadId =  multipartUploadDAO.createUploadStatus(createRequest).getMultipartUploadStatus().getUploadId();
	}

	@After
	public void after() {
		multipartUploadComposerDAO.truncateAll();
		multipartUploadDAO.truncateAll();
		if(file != null){
			fileHandleDao.delete(file.getId());;
		}
	}


	@Test
	void addAndGetParts() {
		int lowerBound = 5;
		int upperBound = 24;
		String oldEtag = multipartUploadDAO.getUploadStatus(uploadId).getEtag();

		// Calls under test
		multipartUploadComposerDAO.addPartToUpload(uploadId, lowerBound, upperBound);
		List<DBOMultipartUploadComposerPartState> results = multipartUploadComposerDAO.getAddedParts(Long.valueOf(uploadId));

		// Verify we get back the part we added
		assertFalse(results.isEmpty());
		assertEquals(1, results.size());
		DBOMultipartUploadComposerPartState result = results.get(0);
		assertEquals(Long.valueOf(uploadId), result.getUploadId());
		assertEquals(lowerBound, result.getPartRangeLowerBound());
		assertEquals(upperBound, result.getPartRangeUpperBound());

		// Verify the eTag on the upload status changed
		assertNotEquals(oldEtag, multipartUploadDAO.getUploadStatus(uploadId).getEtag());
	}

	@Test
	void deletePartsInRangeWrongUploadId() {
		int lowerBound = 5;
		int upperBound = 24;
		multipartUploadComposerDAO.addPartToUpload(uploadId, lowerBound, upperBound);

		// Call under test
		multipartUploadComposerDAO.deletePartsInRange("83573295", lowerBound, upperBound);

		List<DBOMultipartUploadComposerPartState> results = multipartUploadComposerDAO.getAddedParts(Long.valueOf(uploadId));
		assertEquals(1, results.size());
		DBOMultipartUploadComposerPartState result = results.get(0);
		assertEquals(Long.valueOf(uploadId), result.getUploadId());
		assertEquals(lowerBound, result.getPartRangeLowerBound());
		assertEquals(upperBound, result.getPartRangeUpperBound());
	}

	@Test
	void deletePartsInRangeOutOfRange() {
		int lowerBound = 5;
		int upperBound = 24;
		multipartUploadComposerDAO.addPartToUpload(uploadId, lowerBound, upperBound);

		// Call under test
		multipartUploadComposerDAO.deletePartsInRange(uploadId, 28, 30);

		List<DBOMultipartUploadComposerPartState> results = multipartUploadComposerDAO.getAddedParts(Long.valueOf(uploadId));
		assertEquals(1, results.size());
	}

	@Test
	void deletePartsInRangePartiallyOverlapping() {
		int lowerBound = 5;
		int upperBound = 24;
		multipartUploadComposerDAO.addPartToUpload(uploadId, lowerBound, upperBound);

		// Call under test
		multipartUploadComposerDAO.deletePartsInRange(uploadId, 3, 10);

		List<DBOMultipartUploadComposerPartState> results = multipartUploadComposerDAO.getAddedParts(Long.valueOf(uploadId));
		assertEquals(1, results.size());
	}

	@Test
	void deletePartsInRangePerfectMatch() {
		int lowerBound = 5;
		int upperBound = 24;
		multipartUploadComposerDAO.addPartToUpload(uploadId, lowerBound, upperBound);

		// Call under test
		multipartUploadComposerDAO.deletePartsInRange(uploadId, lowerBound, upperBound);

		List<DBOMultipartUploadComposerPartState> results = multipartUploadComposerDAO.getAddedParts(Long.valueOf(uploadId));
		assertTrue(results.isEmpty());
	}

	@Test
	void deletePartsInRangeOutsideRange() {
		int lowerBound = 5;
		int upperBound = 24;
		multipartUploadComposerDAO.addPartToUpload(uploadId, lowerBound, upperBound);

		// Call under test
		multipartUploadComposerDAO.deletePartsInRange(uploadId, lowerBound - 1, upperBound + 1);

		List<DBOMultipartUploadComposerPartState> results = multipartUploadComposerDAO.getAddedParts(Long.valueOf(uploadId));
		assertTrue(results.isEmpty());
	}

	@Test
	void getAddedPartRangesForUpdate() {
		multipartUploadComposerDAO.addPartToUpload(uploadId, 5, 7);
		multipartUploadComposerDAO.addPartToUpload(uploadId, 8, 9);
		multipartUploadComposerDAO.addPartToUpload(uploadId, 10, 20);
		multipartUploadComposerDAO.addPartToUpload(uploadId, 21, 26);

		List<PartRange> expected = new ArrayList<>();
		expected.add(new PartRange(8, 9));
		expected.add(new PartRange(10, 20));

		// Call under test
		List<PartRange> actual = multipartUploadComposerDAO.getAddedPartRangesForUpdate(Long.valueOf(uploadId), 8L, 20L);

		assertEquals(expected, actual);
	}

	@Test
	void setUploadComplete() {
		file = (S3FileHandle) fileHandleDao.createFile(TestUtils.createS3FileHandle(userId.toString(), idGenerator.generateNewId(IdType.FILE_IDS).toString()));
		multipartUploadComposerDAO.addPartToUpload(uploadId, 2, 5);

		String oldEtag = multipartUploadDAO.getUploadStatus(uploadId).getEtag();

		// Call under test
		CompositeMultipartUploadStatus status = multipartUploadComposerDAO.setUploadComplete(uploadId, file.getId());

		// The upload state should be complete.
		assertEquals(MultipartUploadState.COMPLETED, status.getMultipartUploadStatus().getState());
		// All parts should have been removed from the composer table
		assertTrue(multipartUploadComposerDAO.getAddedParts(Long.valueOf(uploadId)).isEmpty());
		assertNotEquals(oldEtag, status.getEtag());
	}
}