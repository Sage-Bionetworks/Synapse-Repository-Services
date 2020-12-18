package org.sagebionetworks.repo.model.ar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACTAccessRequirement;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ApprovalState;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.LockAccessRequirement;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.helper.DoaObjectHelper;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class AccessRestrictionStatusDaoImplTest {

	@Autowired
	private UserGroupDAO userGroupDAO;

	@Autowired
	private AccessRestrictionStatusDao accessRestrictionStatusDao;

	@Autowired
	private AccessApprovalDAO accessApprovalDAO;

	@Autowired
	private AccessRequirementDAO accessRequirementDAO;

	@Autowired
	private NodeDAO nodeDao;

	@Autowired
	private DoaObjectHelper<Node> nodeDaoHelper;
	
	@Autowired
	private DoaObjectHelper<UserGroup> userGroupHelpler;
	
	@Autowired
	private DoaObjectHelper<TermsOfUseAccessRequirement> termsOfUseHelper;
	
	@Autowired
	private DoaObjectHelper<AccessApproval> accessApprovalHelper;
	
	Long userOneId;
	Long userTwoId;
	Long userThreeId;

	Node project;
	Node folder;
	Node file;
	Node folderTwo;
	Node fileTwo;

	@BeforeEach
	public void before() throws Exception {
		accessApprovalDAO.clear();
		accessRequirementDAO.clear();

		UserGroup ug = new UserGroup();
		ug.setIsIndividual(true);
		ug.setCreationDate(new Date());
		userOneId = Long.parseLong(userGroupHelpler.create(u->{}).getId());
		userTwoId = Long.parseLong(userGroupHelpler.create(u->{}).getId());
		userThreeId = Long.parseLong(userGroupHelpler.create(u->{}).getId());
	}

	@AfterEach
	public void after() {
		if (project != null) {
			nodeDao.delete(project.getId());
		}
		if (userOneId != null) {
			userGroupDAO.delete(userOneId.toString());
		}
		if (userTwoId != null) {
			userGroupDAO.delete(userTwoId.toString());
		}
		if (userThreeId != null) {
			userGroupDAO.delete(userThreeId.toString());
		}
		accessApprovalDAO.clear();
		accessRequirementDAO.clear();
	}

	@Test
	public void testGeEntityStatusWithNoRequirements() {
		setupNodeHierarchy(userTwoId);
		List<Long> subjectIds = KeyFactory.stringToKey(Arrays.asList(file.getId(), fileTwo.getId()));
		// call under test
		List<SubjectStatus> results = accessRestrictionStatusDao.getEntityStatus(subjectIds, userTwoId);
		validateBasicSubjectStatus(subjectIds, results, userTwoId);
		SubjectStatus result = results.get(0);
		assertFalse(result.hasUnmet());
		assertNotNull(result.getAccessRestrictions());
		assertTrue(result.getAccessRestrictions().isEmpty());

		result = results.get(1);
		assertFalse(result.hasUnmet());
		assertNotNull(result.getAccessRestrictions());
		assertTrue(result.getAccessRestrictions().isEmpty());
	}

	@Test
	public void testGeEntityStatusWithUnmetRestriction() {
		setupNodeHierarchy(userTwoId);
		
		TermsOfUseAccessRequirement projectToU = termsOfUseHelper.create(t->{
			t.setCreatedBy(userThreeId.toString());
			t.getSubjectIds().get(0).setId(project.getId());
		});
		List<Long> subjectIds = KeyFactory.stringToKey(Arrays.asList(file.getId(), fileTwo.getId()));
		// call under test
		List<SubjectStatus> results = accessRestrictionStatusDao.getEntityStatus(subjectIds, userOneId);
		validateBasicSubjectStatus(subjectIds, results, userOneId);

		List<UsersRequirementStatus> expected = Arrays
				.asList(new UsersRequirementStatus().withRequirementId(projectToU.getId())
						.withRequirementType(projectToU.getConcreteType()).withIsUnmet(true));

		SubjectStatus result = results.get(0);
		assertTrue(result.hasUnmet());
		assertEquals(expected, result.getAccessRestrictions());

		result = results.get(1);
		assertTrue(result.hasUnmet());
		assertEquals(expected, result.getAccessRestrictions());
	}

	@Test
	public void testGeEntityStatusWithUnmetRestrictionAsFileCreator() {
		setupNodeHierarchy(userTwoId);
		TermsOfUseAccessRequirement projectToU = termsOfUseHelper.create(t->{
			t.setCreatedBy(userThreeId.toString());
			t.getSubjectIds().get(0).setId(project.getId());
		});
		List<Long> subjectIds = KeyFactory.stringToKey(Arrays.asList(file.getId(), fileTwo.getId()));
		// call under test
		List<SubjectStatus> results = accessRestrictionStatusDao.getEntityStatus(subjectIds, userTwoId);
		validateBasicSubjectStatus(subjectIds, results, userTwoId);

		List<UsersRequirementStatus> expected = Arrays
				.asList(new UsersRequirementStatus().withRequirementId(projectToU.getId())
						.withRequirementType(projectToU.getConcreteType()).withIsUnmet(false));

		SubjectStatus result = results.get(0);
		assertFalse(result.hasUnmet());
		assertEquals(expected, result.getAccessRestrictions());

		result = results.get(1);
		assertFalse(result.hasUnmet());
		assertEquals(expected, result.getAccessRestrictions());
	}

	@Test
	public void testGeEntityStatusWithApprovedRestriction() {
		setupNodeHierarchy(userTwoId);
		TermsOfUseAccessRequirement projectToU = termsOfUseHelper.create(t->{
			t.setCreatedBy(userThreeId.toString());
			t.getSubjectIds().get(0).setId(project.getId());
		});
		accessApprovalHelper.create(a->{
			a.setCreatedBy(userThreeId.toString());
			a.setSubmitterId(userTwoId.toString());
			a.setRequirementId(projectToU.getId());
			a.setRequirementVersion(projectToU.getVersionNumber());
			a.setState(ApprovalState.APPROVED);
			a.setAccessorId(userOneId.toString());
		});

		List<Long> subjectIds = KeyFactory.stringToKey(Arrays.asList(file.getId()));
		// call under test
		List<SubjectStatus> results = accessRestrictionStatusDao.getEntityStatus(subjectIds, userOneId);
		validateBasicSubjectStatus(subjectIds, results, userOneId);

		List<UsersRequirementStatus> expected = Arrays
				.asList(new UsersRequirementStatus().withRequirementId(projectToU.getId())
						.withRequirementType(projectToU.getConcreteType()).withIsUnmet(false));

		SubjectStatus result = results.get(0);
		assertFalse(result.hasUnmet());
		assertEquals(expected, result.getAccessRestrictions());
	}
	
	@Test
	public void testGeEntityStatusWithRevokedRestriction() {
		setupNodeHierarchy(userTwoId);
		TermsOfUseAccessRequirement projectToU = termsOfUseHelper.create(t->{
			t.setCreatedBy(userThreeId.toString());
			t.getSubjectIds().get(0).setId(project.getId());
		});
		accessApprovalHelper.create(a->{
			a.setCreatedBy(userThreeId.toString());
			a.setSubmitterId(userTwoId.toString());
			a.setRequirementId(projectToU.getId());
			a.setRequirementVersion(projectToU.getVersionNumber());
			a.setState(ApprovalState.REVOKED);
			a.setAccessorId(userOneId.toString());
		});

		List<Long> subjectIds = KeyFactory.stringToKey(Arrays.asList(file.getId()));
		// call under test
		List<SubjectStatus> results = accessRestrictionStatusDao.getEntityStatus(subjectIds, userOneId);
		validateBasicSubjectStatus(subjectIds, results, userOneId);

		List<UsersRequirementStatus> expected = Arrays
				.asList(new UsersRequirementStatus().withRequirementId(projectToU.getId())
						.withRequirementType(projectToU.getConcreteType()).withIsUnmet(true));

		SubjectStatus result = results.get(0);
		assertTrue(result.hasUnmet());
		assertEquals(expected, result.getAccessRestrictions());
	}
	
	@Test
	public void testGeEntityStatusWithRevokeAndApproved() {
		setupNodeHierarchy(userTwoId);
		TermsOfUseAccessRequirement projectToUV1 = termsOfUseHelper.create(t->{
			t.setCreatedBy(userThreeId.toString());
			t.getSubjectIds().get(0).setId(project.getId());
		});
		
		List<Long> subjectIds = KeyFactory.stringToKey(Arrays.asList(file.getId()));
		
		// create a second version of this ToU
		TermsOfUseAccessRequirement toUpdate = (TermsOfUseAccessRequirement) accessRequirementDAO.get(projectToUV1.getId().toString());
		toUpdate.setVersionNumber(toUpdate.getVersionNumber()+1L);
		TermsOfUseAccessRequirement projectToUV2 = accessRequirementDAO.update(toUpdate);
		// revoke the first version
		accessApprovalHelper.create(a->{
			a.setCreatedBy(userThreeId.toString());
			a.setSubmitterId(userTwoId.toString());
			a.setRequirementId(projectToUV1.getId());
			a.setRequirementVersion(projectToUV1.getVersionNumber());
			a.setState(ApprovalState.REVOKED);
			a.setAccessorId(userOneId.toString());
		});
		
		// approve the second version
		accessApprovalHelper.create(a->{
			a.setCreatedBy(userThreeId.toString());
			a.setSubmitterId(userTwoId.toString());
			a.setRequirementId(projectToUV2.getId());
			a.setRequirementVersion(projectToUV2.getVersionNumber());
			a.setState(ApprovalState.APPROVED);
			a.setAccessorId(userOneId.toString());
		});

		// call under test
		List<SubjectStatus> results = accessRestrictionStatusDao.getEntityStatus(subjectIds, userOneId);
		validateBasicSubjectStatus(subjectIds, results, userOneId);

		List<UsersRequirementStatus> expected = Arrays
				.asList(new UsersRequirementStatus().withRequirementId(projectToUV2.getId())
						.withRequirementType(projectToUV2.getConcreteType()).withIsUnmet(false));

		SubjectStatus result = results.get(0);
		assertFalse(result.hasUnmet());
		assertEquals(expected, result.getAccessRestrictions());
	}
	
	@Test
	public void testGeEntityStatusWithMultipleRevoke() {
		setupNodeHierarchy(userTwoId);
		TermsOfUseAccessRequirement projectToUV1 = termsOfUseHelper.create(t->{
			t.setCreatedBy(userThreeId.toString());
			t.getSubjectIds().get(0).setId(project.getId());
		});
		
		List<Long> subjectIds = KeyFactory.stringToKey(Arrays.asList(file.getId()));
		
		// create a second version of this ToU
		TermsOfUseAccessRequirement toUpdate = (TermsOfUseAccessRequirement) accessRequirementDAO.get(projectToUV1.getId().toString());
		toUpdate.setVersionNumber(toUpdate.getVersionNumber()+1L);
		TermsOfUseAccessRequirement projectToUV2 = accessRequirementDAO.update(toUpdate);
		// revoke the first version
		accessApprovalHelper.create(a->{
			a.setCreatedBy(userThreeId.toString());
			a.setSubmitterId(userTwoId.toString());
			a.setRequirementId(projectToUV1.getId());
			a.setRequirementVersion(projectToUV1.getVersionNumber());
			a.setState(ApprovalState.REVOKED);
			a.setAccessorId(userOneId.toString());
		});
		
		// approve the second version
		accessApprovalHelper.create(a->{
			a.setCreatedBy(userThreeId.toString());
			a.setSubmitterId(userTwoId.toString());
			a.setRequirementId(projectToUV2.getId());
			a.setRequirementVersion(projectToUV2.getVersionNumber());
			a.setState(ApprovalState.REVOKED);
			a.setAccessorId(userOneId.toString());
		});

		// call under test
		List<SubjectStatus> results = accessRestrictionStatusDao.getEntityStatus(subjectIds, userOneId);
		validateBasicSubjectStatus(subjectIds, results, userOneId);

		List<UsersRequirementStatus> expected = Arrays
				.asList(new UsersRequirementStatus().withRequirementId(projectToUV2.getId())
						.withRequirementType(projectToUV2.getConcreteType()).withIsUnmet(true));

		SubjectStatus result = results.get(0);
		assertTrue(result.hasUnmet());
		assertEquals(expected, result.getAccessRestrictions());
	}
	
	/**
	 * If a user has been approved for one version of an access requirement, they 
	 * are still approved even if a new version of the requirement is created.
	 */
	@Test
	public void testGeEntityStatusWithWithOldApproval() {
		setupNodeHierarchy(userTwoId);
		TermsOfUseAccessRequirement projectToUV1 = termsOfUseHelper.create(t->{
			t.setCreatedBy(userThreeId.toString());
			t.getSubjectIds().get(0).setId(project.getId());
		});
		
		// approve the first version only
		accessApprovalHelper.create(a->{
			a.setCreatedBy(userThreeId.toString());
			a.setSubmitterId(userTwoId.toString());
			a.setRequirementId(projectToUV1.getId());
			a.setRequirementVersion(projectToUV1.getVersionNumber());
			a.setState(ApprovalState.APPROVED);
			a.setAccessorId(userOneId.toString());
		});

		// create a second version of this ToU that the user has not been approved for.
		TermsOfUseAccessRequirement toUpdate = (TermsOfUseAccessRequirement) accessRequirementDAO.get(projectToUV1.getId().toString());
		toUpdate.setVersionNumber(toUpdate.getVersionNumber()+1L);
		TermsOfUseAccessRequirement projectToUV2 = accessRequirementDAO.update(toUpdate);
		
		List<Long> subjectIds = KeyFactory.stringToKey(Arrays.asList(file.getId()));

		// call under test
		List<SubjectStatus> results = accessRestrictionStatusDao.getEntityStatus(subjectIds, userOneId);
		validateBasicSubjectStatus(subjectIds, results, userOneId);

		List<UsersRequirementStatus> expected = Arrays
				.asList(new UsersRequirementStatus().withRequirementId(projectToUV2.getId())
						.withRequirementType(projectToUV2.getConcreteType()).withIsUnmet(false));

		SubjectStatus result = results.get(0);
		assertFalse(result.hasUnmet());
		assertEquals(expected, result.getAccessRestrictions());
	}

	/**
	 * Basic validation of the SubjectStatus results against the input subject ids.
	 * 
	 * @param subjectIds
	 * @param results
	 * @param userId
	 */
	void validateBasicSubjectStatus(List<Long> subjectIds, List<SubjectStatus> results, Long userId) {
		assertNotNull(subjectIds);
		assertNotNull(results);
		assertEquals(subjectIds.size(), results.size());
		for (int i = 0; i < subjectIds.size(); i++) {
			Long subjectId = subjectIds.get(i);
			assertNotNull(subjectId);
			SubjectStatus status = results.get(i);
			assertNotNull(status);
			assertEquals(subjectId, status.getSubjectId());
			assertEquals(userId, status.getUserId());
		}
	}

	/**
	 * Helper to setup a node hierarchy with the provided user as the creator.
	 * 
	 * @param userId
	 */
	public void setupNodeHierarchy(Long userId) {
		
		project = nodeDaoHelper.create(n ->{
			n.setName("aProject");
			n.setCreatedByPrincipalId(userId);
		});
		folder = nodeDaoHelper.create(n -> {
			n.setName("aFolder");
			n.setCreatedByPrincipalId(userId);
			n.setParentId(project.getId());
			n.setNodeType(EntityType.folder);
		});
		file = nodeDaoHelper.create(n -> {
			n.setName("aFile");
			n.setCreatedByPrincipalId(userId);
			n.setParentId(folder.getId());
			n.setNodeType(EntityType.file);
		});
		folderTwo = nodeDaoHelper.create(n -> {
			n.setName("folderTwo");
			n.setCreatedByPrincipalId(userId);
			n.setParentId(project.getId());
			n.setNodeType(EntityType.folder);
		});
		fileTwo = nodeDaoHelper.create(n -> {
			n.setName("fileTwo");
			n.setCreatedByPrincipalId(userId);
			n.setParentId(folderTwo.getId());
			n.setNodeType(EntityType.file);
		});
	}
	
	
	/**
	 * Helper to create a new LockAccessRequirement.
	 * 
	 * @param userId
	 * @param subjectId
	 * @param subjectType
	 * @param jiraKey
	 * @return
	 * @throws DatastoreException
	 */
	public LockAccessRequirement createNewLockAccessRequirement(Long userId, Long subjectId,
			RestrictableObjectType subjectType, String jiraKey) throws DatastoreException {
		LockAccessRequirement ar = new LockAccessRequirement();
		ar.setCreatedBy(userId.toString());
		ar.setCreatedOn(new Date());
		ar.setModifiedBy(userId.toString());
		ar.setModifiedOn(new Date());
		ar.setEtag("10");
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setVersionNumber(1L);
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(subjectId.toString());
		rod.setType(subjectType);
		ar.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[] { rod }));
		ar.setJiraKey(jiraKey);
		return accessRequirementDAO.create(ar);
	}

	/**
	 * Helper to create a new ACTAccessRequirement
	 * 
	 * @param userId
	 * @param subjectId
	 * @param subjectType
	 * @param jiraKey
	 * @return
	 * @throws DatastoreException
	 */
	public ACTAccessRequirement createNewACTAccessRequirement(Long userId, Long subjectId,
			RestrictableObjectType subjectType) throws DatastoreException {
		ACTAccessRequirement ar = new ACTAccessRequirement();
		ar.setCreatedBy(userId.toString());
		ar.setCreatedOn(new Date());
		ar.setModifiedBy(userId.toString());
		ar.setModifiedOn(new Date());
		ar.setEtag("10");
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setVersionNumber(1L);
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(subjectId.toString());
		rod.setType(subjectType);
		ar.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[] { rod }));
		ar.setOpenJiraIssue(true);
		return accessRequirementDAO.create(ar);
	}
}
