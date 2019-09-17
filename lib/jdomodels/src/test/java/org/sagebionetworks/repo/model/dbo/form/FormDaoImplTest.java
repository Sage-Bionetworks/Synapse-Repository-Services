package org.sagebionetworks.repo.model.dbo.form;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.form.FormData;
import org.sagebionetworks.repo.model.form.FormGroup;
import org.sagebionetworks.repo.model.form.StateEnum;
import org.sagebionetworks.repo.model.form.SubmissionStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class FormDaoImplTest {

	@Autowired
	FormDao formDao;
	@Autowired
	FileHandleDao fileDao;
	@Autowired
	IdGenerator idGenerator;

	List<S3FileHandle> files;
	Long adminUserId;
	String groupName;
	String formName;

	@BeforeEach
	public void before() {
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		files = new ArrayList<S3FileHandle>();
		groupName = "some group";
		formName = "some form";
	}

	@AfterEach
	public void afterEach() {
		formDao.truncateAll();
		for (S3FileHandle file : files) {
			fileDao.delete(file.getId());
		}
	}

	@Test
	public void testCreateGroupDto() {
		DBOFormGroup dbo = new DBOFormGroup();
		dbo.setCreatedBy(123L);
		dbo.setCreatedOn(new Timestamp(1));
		dbo.setName("someName");
		dbo.setGroupId(456L);

		// call under test
		FormGroup dto = FormDaoImpl.createGroupDto(dbo);
		assertNotNull(dto);
		assertEquals(dbo.getCreatedBy().toString(), dto.getCreatedBy());
		assertEquals(dbo.getCreatedOn().getTime(), dto.getCreatedOn().getTime());
		assertEquals(dbo.getName(), dto.getName());
		assertEquals(dbo.getGroupId().toString(), dto.getGroupId());
	}

	@Test
	public void testCreate() {
		String name = UUID.randomUUID().toString();
		// call under test
		FormGroup group = formDao.createFormGroup(adminUserId, name);
		assertNotNull(group);
		assertEquals(name, group.getName());
		assertEquals(adminUserId.toString(), group.getCreatedBy());
		assertNotNull(group.getCreatedOn());
	}

	@Test
	public void testCreateDuplicateName() {
		String name = UUID.randomUUID().toString();
		FormGroup group = formDao.createFormGroup(adminUserId, name);
		assertNotNull(group);
		// try to create a group with the same name
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			formDao.createFormGroup(adminUserId, name);
		});
	}

	@Test
	public void testLookupGroupByName() {
		String name = UUID.randomUUID().toString();
		// call under test
		Optional<FormGroup> optional = formDao.lookupGroupByName(name);
		assertNotNull(optional);
		assertFalse(optional.isPresent());
		FormGroup group = formDao.createFormGroup(adminUserId, name);
		// lookup again
		optional = formDao.lookupGroupByName(name);
		assertNotNull(optional);
		assertTrue(optional.isPresent());
		assertEquals(group, optional.get());
	}

	@Test
	public void testDtoToDbo() {
		DBOFormData dbo = new DBOFormData();
		dbo.setId(111L);
		dbo.setEtag("an etag");
		dbo.setName("some name");
		dbo.setCreatedOn(new Timestamp(88888));
		dbo.setCreatedBy(222L);
		dbo.setModifiedOn(new Timestamp(99999));
		dbo.setGroupId(333L);
		dbo.setFileHandleId(444L);
		dbo.setSubmittedOn(new Timestamp(101010));
		dbo.setReviewedOn(new Timestamp(111111));
		dbo.setReviewedBy(555L);
		dbo.setState(StateEnum.REJECTED.name());
		dbo.setRejectionMessage("more data");

		// call under test
		FormData dto = FormDaoImpl.dtoToDbo(dbo);

		assertNotNull(dto);
		assertEquals(dbo.getId().toString(), dto.getFormDataId());
		assertEquals(dbo.getEtag(), dto.getEtag());
		assertEquals(dbo.getName(), dto.getName());
		assertEquals(dbo.getCreatedOn().getTime(), dto.getCreatedOn().getTime());
		assertEquals(dbo.getCreatedBy().toString(), dto.getCreatedBy());
		assertEquals(dbo.getModifiedOn().getTime(), dto.getModifiedOn().getTime());
		assertEquals(dbo.getGroupId().toString(), dto.getGroupId());
		assertEquals(dbo.getFileHandleId().toString(), dto.getDataFileHandleId());
		SubmissionStatus status = dto.getSubmissionStatus();
		assertNotNull(status);
		assertEquals(dbo.getSubmittedOn().getTime(), status.getSubmittedOn().getTime());
		assertEquals(dbo.getReviewedOn().getTime(), status.getReviewedOn().getTime());
		assertEquals(dbo.getReviewedBy().toString(), status.getReviewedBy());
		assertEquals(dbo.getState(), status.getState().name());
		assertEquals(dbo.getRejectionMessage(), status.getRejectionMessage());
	}

	@Test
	public void testDtoToDboWithOptionalNulls() {
		DBOFormData dbo = new DBOFormData();
		dbo.setId(111L);
		dbo.setEtag("an etag");
		dbo.setName("some name");
		dbo.setCreatedOn(new Timestamp(88888));
		dbo.setCreatedBy(222L);
		dbo.setModifiedOn(new Timestamp(99999));
		dbo.setGroupId(333L);
		dbo.setFileHandleId(444L);
		dbo.setSubmittedOn(null);
		dbo.setReviewedOn(null);
		dbo.setReviewedBy(null);
		dbo.setState(StateEnum.REJECTED.name());
		dbo.setRejectionMessage(null);

		// call under test
		FormData dto = FormDaoImpl.dtoToDbo(dbo);

		assertNotNull(dto);
		assertEquals(dbo.getId().toString(), dto.getFormDataId());
		assertEquals(dbo.getEtag(), dto.getEtag());
		assertEquals(dbo.getName(), dto.getName());
		assertEquals(dbo.getCreatedOn().getTime(), dto.getCreatedOn().getTime());
		assertEquals(dbo.getCreatedBy().toString(), dto.getCreatedBy());
		assertEquals(dbo.getModifiedOn().getTime(), dto.getModifiedOn().getTime());
		assertEquals(dbo.getGroupId().toString(), dto.getGroupId());
		assertEquals(dbo.getFileHandleId().toString(), dto.getDataFileHandleId());
		SubmissionStatus status = dto.getSubmissionStatus();
		assertNotNull(status);
		assertNull(status.getSubmittedOn());
		assertNull(status.getReviewedOn());
		assertNull(status.getReviewedBy());
		assertEquals(dbo.getState(), status.getState().name());
		assertEquals(dbo.getRejectionMessage(), status.getRejectionMessage());
	}

	@Test
	public void testCreateFormData() {
		FormGroup group = createSampleGroup();
		S3FileHandle sampleFile = createFileHandle();
		// call under test
		FormData data = formDao.createFormData(adminUserId, group.getGroupId(), formName, sampleFile.getId());
		assertNotNull(data);
		assertNotNull(data.getFormDataId());
		assertNotNull(data.getEtag());
		assertEquals(formName, data.getName());
		assertNotNull(data.getCreatedOn());
		assertEquals(adminUserId.toString(), data.getCreatedBy());
		assertEquals(data.getCreatedOn(), data.getModifiedOn());
		assertEquals(group.getGroupId(), data.getGroupId());
		assertEquals(sampleFile.getId(), data.getDataFileHandleId());
		SubmissionStatus status = data.getSubmissionStatus();
		assertNotNull(status);
		assertEquals(StateEnum.WAITING_FOR_SUBMISSION, status.getState());
		assertNull(status.getSubmittedOn());
		assertNull(status.getReviewedOn());
		assertNull(status.getReviewedBy());
		assertNull(status.getRejectionMessage());
	}

	@Test
	public void testGetFormDataCreator() {
		FormData data = createFormData();
		// call under test
		long createdBy = formDao.getFormDataCreator(data.getFormDataId());
		assertEquals(adminUserId, createdBy);
	}

	@Test
	public void testGetFormDataCreatorDoesNotExist() {
		String message = assertThrows(NotFoundException.class, () -> {
			// call under test
			formDao.getFormDataCreator("-1");
		}).getMessage();
		assertEquals("FormData does not exist for: -1", message);
	}

	@Test
	public void testGetFormDataGroupId() {
		FormData data = createFormData();
		// call under test
		String groupId = formDao.getFormDataGroupId(data.getFormDataId());
		assertEquals(data.getGroupId(), groupId);
	}

	@Test
	public void testGetFormDataGroupIdDoesNotExist() {
		String message = assertThrows(NotFoundException.class, () -> {
			// call under test
			formDao.getFormDataGroupId("-1");
		}).getMessage();
		assertEquals("FormData does not exist for: -1", message);
	}

	@Test
	public void testGetFormDataState() {
		FormData data = createFormData();
		// call under test
		StateEnum state = formDao.getFormDataState(data.getFormDataId());
		assertEquals(data.getSubmissionStatus().getState(), state);
	}

	@Test
	public void testUpdateNameAndFile() throws InterruptedException {
		FormData toUpdate = createFormData();
		FormData unchanged = createFormData();

		S3FileHandle newFile = createFileHandle();
		String newName = "this is the new name";
		// sleep to ensure modifiedOn changes.
		Thread.sleep(101);
		// call under test
		FormData updated = formDao.updateFormData(toUpdate.getFormDataId(), newName, newFile.getId());
		assertFalse(toUpdate.getEtag().equals(updated.getEtag()));
		assertEquals(newName, updated.getName());
		assertEquals(newFile.getId(), updated.getDataFileHandleId());
		assertFalse(toUpdate.getModifiedOn().equals(updated.getModifiedOn()));
		// everything else should be the same
		assertEquals(updated.getFormDataId(), toUpdate.getFormDataId());
		assertEquals(updated.getCreatedOn(), toUpdate.getCreatedOn());
		assertEquals(updated.getCreatedBy(), toUpdate.getCreatedBy());
		assertEquals(updated.getGroupId(), updated.getGroupId());
		SubmissionStatus status = updated.getSubmissionStatus();
		assertNotNull(status);
		assertEquals(StateEnum.WAITING_FOR_SUBMISSION, status.getState());
		assertNull(status.getSubmittedOn());
		assertNull(status.getReviewedOn());
		assertNull(status.getReviewedBy());
		assertNull(status.getRejectionMessage());

		// should not have changed the other formdata
		FormData shouldNotChange = formDao.getFormData(unchanged.getFormDataId());
		assertEquals(unchanged, shouldNotChange);
	}

	@Test
	public void testUpdateNameAndFileNullName() throws InterruptedException {
		FormData toUpdate = createFormData();

		S3FileHandle newFile = createFileHandle();
		String newName = null;

		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			formDao.updateFormData(toUpdate.getFormDataId(), newName, newFile.getId());
		});
	}

	@Test
	public void testUpdateNameAndFileNullId() throws InterruptedException {
		String formId = null;
		S3FileHandle newFile = createFileHandle();
		String newName = "this is the new name";

		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			formDao.updateFormData(formId, newName, newFile.getId());
		});
	}

	@Test
	public void testUpdateNameAndFileNullFileId() throws InterruptedException {
		FormData toUpdate = createFormData();
		String fileId = null;
		String newName = "this is the new name";

		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			formDao.updateFormData(toUpdate.getFormDataId(), newName, fileId);
		});
	}

	@Test
	public void testUpdateFile() throws InterruptedException {
		FormData toUpdate = createFormData();
		FormData unchanged = createFormData();

		S3FileHandle newFile = createFileHandle();
		// sleep to ensure modifiedOn changes.
		Thread.sleep(101);
		// call under test
		FormData updated = formDao.updateFormData(toUpdate.getFormDataId(), newFile.getId());
		assertFalse(toUpdate.getEtag().equals(updated.getEtag()));
		assertEquals(newFile.getId(), updated.getDataFileHandleId());
		assertFalse(toUpdate.getModifiedOn().equals(updated.getModifiedOn()));
		// everything else should be the same
		assertEquals(updated.getFormDataId(), toUpdate.getFormDataId());
		assertEquals(updated.getName(), toUpdate.getName());
		assertEquals(updated.getCreatedOn(), toUpdate.getCreatedOn());
		assertEquals(updated.getCreatedBy(), toUpdate.getCreatedBy());
		assertEquals(updated.getGroupId(), updated.getGroupId());
		SubmissionStatus status = updated.getSubmissionStatus();
		assertNotNull(status);
		assertEquals(StateEnum.WAITING_FOR_SUBMISSION, status.getState());
		assertNull(status.getSubmittedOn());
		assertNull(status.getReviewedOn());
		assertNull(status.getReviewedBy());
		assertNull(status.getRejectionMessage());

		// should not have changed the other formdata
		FormData shouldNotChange = formDao.getFormData(unchanged.getFormDataId());
		assertEquals(unchanged, shouldNotChange);
	}

	@Test
	public void testUpdateFileNullId() throws InterruptedException {
		String formId = null;
		S3FileHandle newFile = createFileHandle();

		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			formDao.updateFormData(formId, newFile.getId());
		});
	}

	@Test
	public void testUpdateFileNullFileId() throws InterruptedException {
		FormData toUpdate = createFormData();
		String fileId = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			formDao.updateFormData(toUpdate.getFormDataId(), fileId);
		});
	}

	@Test
	public void testGetFormDataStateDoesNotExist() {
		String message = assertThrows(NotFoundException.class, () -> {
			// call under test
			formDao.getFormDataState("-1");
		}).getMessage();
		assertEquals("FormData does not exist for: -1", message);
	}

	/**
	 * Helper to create a FormGroup.
	 * 
	 * @return
	 */
	FormGroup createSampleGroup() {
		Optional<FormGroup> optional = formDao.lookupGroupByName(groupName);
		if (optional.isPresent()) {
			return optional.get();
		}
		return formDao.createFormGroup(adminUserId, groupName);
	}

	/**
	 * Helper to create a new FormData object.
	 * 
	 * @return
	 */
	FormData createFormData() {
		FormGroup group = createSampleGroup();
		S3FileHandle sampleFile = createFileHandle();
		return formDao.createFormData(adminUserId, group.getGroupId(), formName, sampleFile.getId());
	}

	/**
	 * Helper to create a new FileHandle
	 * 
	 * @return
	 */
	public S3FileHandle createFileHandle() {
		S3FileHandle handle = TestUtils.createS3FileHandle(adminUserId.toString(),
				idGenerator.generateNewId(IdType.FILE_IDS).toString());
		S3FileHandle file = (S3FileHandle) fileDao.createFile(handle);
		files.add(file);
		return file;
	}
}
