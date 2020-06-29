package org.sagebionetworks.repo.model.dbo.form;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.form.FormData;
import org.sagebionetworks.repo.model.form.FormGroup;
import org.sagebionetworks.repo.model.form.ListRequest;
import org.sagebionetworks.repo.model.form.StateEnum;
import org.sagebionetworks.repo.model.form.SubmissionStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.Sets;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class FormDaoImplTest {

	@Autowired
	FormDao formDao;
	@Autowired
	FileHandleDao fileDao;
	@Autowired
	IdGenerator idGenerator;
	@Autowired
	UserGroupDAO userGroupDao;

	List<S3FileHandle> files;
	Long adminUserId;
	List<Long> userIds;
	String groupName;
	String formName;

	@BeforeEach
	public void before() {
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		files = new ArrayList<S3FileHandle>();
		groupName = "some group";
		formName = "some form";
		userIds = new ArrayList<>(2);
		// Create multiple users
		for (int i = 0; i < 2; i++) {
			UserGroup ug = new UserGroup();
			ug.setIsIndividual(true);
			userIds.add(userGroupDao.create(ug));
		}
	}

	@AfterEach
	public void afterEach() {
		formDao.truncateAll();
		for (S3FileHandle file : files) {
			fileDao.delete(file.getId());
		}
		for (Long userId : userIds) {
			userGroupDao.delete(userId.toString());
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
	public void testGetFormGroup() {
		FormGroup group = createSampleGroup();
		// call under test
		FormGroup fetched = formDao.getFormGroup(group.getGroupId());
		assertEquals(group, fetched);
	}
	
	@Test
	public void testGetFormGroupWithNotFound() {
		String id = "-1";
		String message = assertThrows(NotFoundException.class, ()->{
			// call under test
			formDao.getFormGroup(id);
		}).getMessage();
		assertEquals("FormGroup does not exist for id: -1", message);
	}
	
	@Test
	public void testGetFormGroupWithNullId() {
		String id = null;
		assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			formDao.getFormGroup(id);
		});
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

	@Test
	public void testDeleteFormData() {
		FormData toDelete = createFormData();
		// call under test
		assertTrue(formDao.deleteFormData(toDelete.getFormDataId()));
	}

	@Test
	public void testDeleteFormDataDoesNotExist() {
		// call under test
		assertFalse(formDao.deleteFormData("-1"));
	}

	@Test
	public void testUpdateStatus() {
		FormData toUpdate = createFormData();
		FormData unchanged = createFormData();

		long now = System.currentTimeMillis();
		SubmissionStatus status = new SubmissionStatus();
		status.setSubmittedOn(new Date(now - 1010));
		status.setRejectionMessage("a new rejection message");
		status.setReviewedBy(adminUserId.toString());
		status.setReviewedOn(new Date(now));
		status.setState(StateEnum.REJECTED);
		// call under test
		FormData updated = formDao.updateStatus(toUpdate.getFormDataId(), status);
		// etag should change
		assertFalse(toUpdate.getEtag().equals(updated.getEtag()));
		assertEquals(status, updated.getSubmissionStatus());

		// should not have changed the other formdata
		FormData shouldNotChange = formDao.getFormData(unchanged.getFormDataId());
		assertEquals(unchanged, shouldNotChange);
	}

	@Test
	public void testListFormDataByCreator() {
		List<FormData> allData = createMultipleForms();
		long limit = 10;
		long offset = 0;
		// Should be able to find each form using its data
		for (FormData form : allData) {
			ListRequest request = new ListRequest();
			request.setGroupId(form.getGroupId());
			request.setFilterByState(Sets.newHashSet(form.getSubmissionStatus().getState()));
			// call under test
			List<FormData> list = formDao.listFormDataByCreator(Long.parseLong(form.getCreatedBy()), request, limit,
					offset);
			assertNotNull(list);
			assertEquals(1, list.size());
			assertEquals(form, list.get(0));
		}
	}

	@Test
	public void testListFormDataByCreatorLimitOffsetOrder() {
		List<FormData> allData = createMultipleForms();
		String firsTUserId = userIds.get(0).toString();
		String firstGroupId = allData.get(0).getGroupId();
		// filter by the first user
		List<FormData> firsUsersFirstGroupForms = allData.stream().filter((FormData t) -> {
			return t.getCreatedBy().equals(firsTUserId) && t.getGroupId().equals(firstGroupId);
		}).collect(Collectors.toList());
		// should have one value for each state.
		assertEquals(StateEnum.values().length, firsUsersFirstGroupForms.size());

		long limit = 2;
		long offset = 1;
		ListRequest request = new ListRequest();
		request.setGroupId(firstGroupId);
		request.setFilterByState(Sets.newHashSet(StateEnum.values()));
		// call under test
		List<FormData> results = formDao.listFormDataByCreator(Long.parseLong(firsTUserId), request, limit, offset);
		assertNotNull(results);
		assertEquals((int) limit, results.size());
		// Results will be in reverse order as last created is first.
		Collections.reverse(firsUsersFirstGroupForms);
		List<FormData> expected = firsUsersFirstGroupForms.subList(1, 3);
		assertEquals(expected, results);
	}

	@Test
	public void testListFormDataForReviewer() {
		List<FormData> allData = createMultipleForms();
		String firstGroupId = allData.get(0).getGroupId();
		List<FormData> firstGroupAccepted = allData.stream().filter((FormData t) -> {
			return t.getGroupId().equals(firstGroupId) && t.getSubmissionStatus().getState().equals(StateEnum.ACCEPTED);
		}).collect(Collectors.toList());

		long limit = 100;
		long offset = 0;
		ListRequest request = new ListRequest();
		request.setGroupId(firstGroupId);
		request.setFilterByState(Sets.newHashSet(StateEnum.ACCEPTED));
		// call under test
		List<FormData> results = formDao.listFormDataForReviewer(request, limit, offset);
		// results will be in reverse order of creation
		Collections.reverse(firstGroupAccepted);
		assertEquals(firstGroupAccepted, results);
	}

	@Test
	public void testListFormDataForReviewerLimitOffset() {
		List<FormData> allData = createMultipleForms();
		String firstGroupId = allData.get(0).getGroupId();
		List<FormData> firstGroupAcceptedOrRejected = allData.stream().filter((FormData t) -> {
			return t.getGroupId().equals(firstGroupId) && (t.getSubmissionStatus().getState().equals(StateEnum.ACCEPTED)
					|| t.getSubmissionStatus().getState().equals(StateEnum.REJECTED));
		}).collect(Collectors.toList());

		long limit = 2;
		long offset = 1;
		ListRequest request = new ListRequest();
		request.setGroupId(firstGroupId);
		request.setFilterByState(Sets.newHashSet(StateEnum.ACCEPTED, StateEnum.REJECTED));
		// call under test
		List<FormData> results = formDao.listFormDataForReviewer(request, limit, offset);
		// results will be in reverse order of creation
		Collections.reverse(firstGroupAcceptedOrRejected);
		List<FormData> expected = firstGroupAcceptedOrRejected.subList(1, 3);
		assertEquals(expected, results);
	}
	
	@Test
	public void testGetFormDataFileHandleId() {
		FormData data = createFormData();
		// call under test
		String fileHandleId = formDao.getFormDataFileHandleId(data.getFormDataId());
		assertEquals(data.getDataFileHandleId(), fileHandleId);
	}
	
	@Test
	public void testGetFormDataFileHandleIdDoesNotExist() {
		assertThrows(NotFoundException.class, () -> {
			// call under test
			formDao.getFormDataFileHandleId("-1");
		});
	}
	
	@Test
	public void testGetFormDataFileHandleIdNullId() {
		String formId = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			formDao.getFormDataFileHandleId(formId);
		});
	}

	/**
	 * Helper to create a FormGroup.
	 * 
	 * @return
	 */
	FormGroup createSampleGroup() {
		return createSampleGroup(groupName);
	}

	/**
	 * Create a FormGroup for a given name
	 * 
	 * @param groupName
	 * @return
	 */
	FormGroup createSampleGroup(String groupName) {
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
	 * Helper to create Form data using two FormGroups and two Users. Will create a
	 * FormData for each group, user, state combination.
	 * 
	 * @return
	 */
	List<FormData> createMultipleForms() {
		List<FormGroup> groups = new LinkedList<>();
		groups.add(createSampleGroup("groupOne"));
		groups.add(createSampleGroup("groupTwo"));
		List<FormData> allData = new LinkedList<>();
		// Create data for each group, user, and state combination.
		int counter = 0;
		for (FormGroup group : groups) {
			for (Long userId : userIds) {
				for (StateEnum state : StateEnum.values()) {
					S3FileHandle sampleFile = createFileHandle();
					FormData form = formDao.createFormData(userId, group.getGroupId(), "form-" + counter++,
							sampleFile.getId());
					SubmissionStatus status = new SubmissionStatus();
					status.setState(state);
					form = formDao.updateStatus(form.getFormDataId(), status);
					allData.add(form);
				}
			}
		}
		return allData;
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
