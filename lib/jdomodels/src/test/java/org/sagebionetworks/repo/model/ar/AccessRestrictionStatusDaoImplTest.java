package org.sagebionetworks.repo.model.ar;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.ApprovalState;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.LockAccessRequirement;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.RestrictionLevel;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.helper.AccessControlListObjectHelper;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
	private DaoObjectHelper<Node> nodeDaoHelper;

	@Autowired
	private DaoObjectHelper<UserGroup> userGroupHelpler;

	@Autowired
	private DaoObjectHelper<TermsOfUseAccessRequirement> termsOfUseHelper;

	@Autowired
	private DaoObjectHelper<LockAccessRequirement> lockHelper;

	@Autowired
	private DaoObjectHelper<ManagedACTAccessRequirement> managedHelper;

	@Autowired
	private DaoObjectHelper<AccessApproval> accessApprovalHelper;

	@Autowired
	private AccessControlListObjectHelper aclHelper;

	@Autowired
	private GroupMembersDAO groupMembersDAO;

	Long userOneId;
	Long userTwoId;
	Long userThreeId;

	Long teamOneId;
	Long teamTwoId;

	Node project;
	Node folder;
	Node file;
	Node folderTwo;
	Node fileTwo;

	@BeforeEach
	public void before() throws Exception {
		accessApprovalDAO.clear();
		accessRequirementDAO.truncateAll();

		UserGroup ug = new UserGroup();
		ug.setIsIndividual(true);
		ug.setCreationDate(new Date());
		userOneId = Long.parseLong(userGroupHelpler.create(u -> {
		}).getId());
		userTwoId = Long.parseLong(userGroupHelpler.create(u -> {
		}).getId());
		userThreeId = Long.parseLong(userGroupHelpler.create(u -> {
		}).getId());
		teamOneId = Long.parseLong(userGroupHelpler.create(u -> {
			u.setIsIndividual(false);
		}).getId());
		teamTwoId = Long.parseLong(userGroupHelpler.create(u -> {
			u.setIsIndividual(false);
		}).getId());
	}

	@AfterEach
	public void after() {
		if (project != null) {
			nodeDao.delete(project.getId());
		}
		aclHelper.truncateAll();
		accessApprovalDAO.clear();
		accessRequirementDAO.truncateAll();
		if (userOneId != null) {
			userGroupDAO.delete(userOneId.toString());
		}
		if (userTwoId != null) {
			userGroupDAO.delete(userTwoId.toString());
		}
		if (userThreeId != null) {
			userGroupDAO.delete(userThreeId.toString());
		}
		if (teamOneId != null) {
			userGroupDAO.delete(teamOneId.toString());
		}
		if (teamTwoId != null) {
			userGroupDAO.delete(teamTwoId.toString());
		}
	}

	@Test
	public void testGetEntityStatusWithNullSubjects() {
		List<Long> subjectIds = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			accessRestrictionStatusDao.getEntityStatusAsMap(subjectIds, userTwoId, Set.of(userTwoId));
		}).getMessage();
		assertEquals("entityIds is required.", message);
	}

	@Test
	public void testGetEntityStatusWithNullUserIdForEntity() {
		List<Long> subjectIds = Arrays.asList(123L);
		Long userId = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			accessRestrictionStatusDao.getEntityStatusAsMap(subjectIds, userId, Collections.emptySet());
		}).getMessage();
		assertEquals("userId is required.", message);
	}

	@Test
	public void testGetEntityStatusWithNullGroupId() {
		List<Long> subjectIds = Arrays.asList(123L);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			accessRestrictionStatusDao.getEntityStatusAsMap(subjectIds, userTwoId, null);
		}).getMessage();
		assertEquals("userGroups is required.", message);
	}

	@Test
	public void testGetEntityStatusWithEmptySetOfGroupId() {
		List<Long> subjectIds = Arrays.asList(123L);
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			accessRestrictionStatusDao.getEntityStatusAsMap(subjectIds, userTwoId, Collections.emptySet());
		}).getMessage();
		assertEquals("User's groups cannot be empty.", message);
	}

	@Test
	public void testGetEntityStatusWithNoEmptySubjects() {
		List<Long> subjectIds = Collections.emptyList();
		// call under test
		List<UsersRestrictionStatus> results = new ArrayList<>(
				accessRestrictionStatusDao.getEntityStatusAsMap(subjectIds, userTwoId, Set.of(userTwoId)).values());
		assertNotNull(results);
		assertTrue(results.isEmpty());
		
	}

	@Test
	public void testGeEntityStatusWithNoRequirementsAsCreator() {
		setupNodeHierarchy(userTwoId);
		List<Long> subjectIds = KeyFactory.stringToKey(Arrays.asList(file.getId(), fileTwo.getId()));
		// call under test
		List<UsersRestrictionStatus> results = new ArrayList<>(
				accessRestrictionStatusDao.getEntityStatusAsMap(subjectIds, userTwoId, Set.of(userTwoId)).values());
		validateBasicSubjectStatus(subjectIds, results, userTwoId);
		UsersRestrictionStatus result = results.get(0);
		assertNotNull(result.getAccessRestrictions());
		assertTrue(result.getAccessRestrictions().isEmpty());

		result = results.get(1);
		assertNotNull(result.getAccessRestrictions());
		assertTrue(result.getAccessRestrictions().isEmpty());
		assertEquals(RestrictionLevel.OPEN, result.getMostRestrictiveLevel());
	}
	
	@Test
	public void testGetEntityStatusWithNoRequirementsNotCreator() {
		setupNodeHierarchy(userTwoId);
		List<Long> subjectIds = KeyFactory.stringToKey(Arrays.asList(file.getId(), fileTwo.getId()));
		// call under test
		List<UsersRestrictionStatus> results = new ArrayList<>(
				accessRestrictionStatusDao.getEntityStatusAsMap(subjectIds, userOneId, Set.of(userOneId)).values());
		validateBasicSubjectStatus(subjectIds, results, userOneId);
		UsersRestrictionStatus result = results.get(0);
		assertNotNull(result.getAccessRestrictions());
		assertTrue(result.getAccessRestrictions().isEmpty());

		result = results.get(1);
		assertNotNull(result.getAccessRestrictions());
		assertTrue(result.getAccessRestrictions().isEmpty());
		assertEquals(RestrictionLevel.OPEN, result.getMostRestrictiveLevel());
	}

	@Test
	public void testGeEntityStatusWithSingleUnmetRestrictionOnProject() {
		setupNodeHierarchy(userTwoId);

		TermsOfUseAccessRequirement projectToU = termsOfUseHelper.create(t -> {
			t.setCreatedBy(userThreeId.toString());
			t.getSubjectIds().get(0).setId(project.getId());
		});
		List<Long> subjectIds = KeyFactory.stringToKey(Arrays.asList(file.getId(), fileTwo.getId()));
		// call under test
		List<UsersRestrictionStatus> results = new ArrayList<>(
				accessRestrictionStatusDao.getEntityStatusAsMap(subjectIds, userOneId, Set.of(userOneId)).values());
		validateBasicSubjectStatus(subjectIds, results, userOneId);

		List<UsersRequirementStatus> expected = Arrays
				.asList(new UsersRequirementStatus().withRequirementId(projectToU.getId())
						.withRequirementType(AccessRequirementType.TOU).withIsUnmet(true).withIsExemptionEligible(false));

		UsersRestrictionStatus result = results.get(0);
		assertEquals(expected, result.getAccessRestrictions());

		result = results.get(1);
		assertEquals(expected, result.getAccessRestrictions());
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, result.getMostRestrictiveLevel());
	}

	@Test
	public void testGeEntityStatusWithUnmetRestrictionHierarchy() {
		setupNodeHierarchy(userTwoId);

		TermsOfUseAccessRequirement projectToU = termsOfUseHelper.create(t -> {
			t.setCreatedBy(userThreeId.toString());
			t.getSubjectIds().get(0).setId(project.getId());
		});
		LockAccessRequirement lockFolderOne = lockHelper.create(a -> {
			a.setCreatedBy(userThreeId.toString());
			a.getSubjectIds().get(0).setId(folder.getId());
		});
		ManagedACTAccessRequirement managedFolderTwo = managedHelper.create(a -> {
			a.setCreatedBy(userThreeId.toString());
			a.getSubjectIds().get(0).setId(folderTwo.getId());
		});

		List<Long> subjectIds = KeyFactory.stringToKey(Arrays.asList(file.getId(), fileTwo.getId()));
		// call under test
		List<UsersRestrictionStatus> results = new ArrayList<>(
				accessRestrictionStatusDao.getEntityStatusAsMap(subjectIds, userOneId, Set.of(userOneId)).values());
		validateBasicSubjectStatus(subjectIds, results, userOneId);

		List<UsersRequirementStatus> expectedOne = Arrays.asList(
				new UsersRequirementStatus().withRequirementId(projectToU.getId())
						.withRequirementType(AccessRequirementType.TOU).withIsUnmet(true).withIsExemptionEligible(false),
				new UsersRequirementStatus().withRequirementId(lockFolderOne.getId())
						.withRequirementType(AccessRequirementType.LOCK).withIsUnmet(true).withIsExemptionEligible(false));

		UsersRestrictionStatus result = results.get(0);
		assertEquals(expectedOne, result.getAccessRestrictions());

		List<UsersRequirementStatus> expectedTwo = Arrays.asList(
				new UsersRequirementStatus().withRequirementId(projectToU.getId())
						.withRequirementType(AccessRequirementType.TOU).withIsUnmet(true).withIsExemptionEligible(false),
				new UsersRequirementStatus().withRequirementId(managedFolderTwo.getId())
						.withRequirementType(AccessRequirementType.MANAGED_ATC).withIsUnmet(true).withIsExemptionEligible(false));

		result = results.get(1);
		assertEquals(expectedTwo, result.getAccessRestrictions());
		assertEquals(RestrictionLevel.CONTROLLED_BY_ACT, result.getMostRestrictiveLevel());
	}

	@Test
	public void testGeEntityStatusWithSingleUnmetRestrictionOnMultipleFiles() {
		setupNodeHierarchy(userTwoId);

		TermsOfUseAccessRequirement tou = termsOfUseHelper.create(t -> {
			t.setCreatedBy(userThreeId.toString());
			RestrictableObjectDescriptor rod1 = new RestrictableObjectDescriptor();
			rod1.setId(file.getId());
			rod1.setType(RestrictableObjectType.ENTITY);
			RestrictableObjectDescriptor rod2 = new RestrictableObjectDescriptor();
			rod2.setId(fileTwo.getId());
			rod2.setType(RestrictableObjectType.ENTITY);
			t.setSubjectIds(Arrays.asList(rod1, rod2));
		});
		List<Long> subjectIds = KeyFactory.stringToKey(Arrays.asList(file.getId(), fileTwo.getId()));
		// call under test
		List<UsersRestrictionStatus> results = new ArrayList<>(
				accessRestrictionStatusDao.getEntityStatusAsMap(subjectIds, userOneId, Set.of(userOneId)).values());
		validateBasicSubjectStatus(subjectIds, results, userOneId);

		List<UsersRequirementStatus> expected = Arrays.asList(new UsersRequirementStatus()
				.withRequirementId(tou.getId()).withRequirementType(AccessRequirementType.TOU)
				.withIsUnmet(true).withIsExemptionEligible(false));

		UsersRestrictionStatus result = results.get(0);
		assertEquals(expected, result.getAccessRestrictions());
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, result.getMostRestrictiveLevel());

		result = results.get(1);
		assertEquals(expected, result.getAccessRestrictions());
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, result.getMostRestrictiveLevel());
	}

	@Test
	public void testGeEntityStatusWithMultipleUnmetRestrictionOnfile() {
		setupNodeHierarchy(userTwoId);

		TermsOfUseAccessRequirement tou = termsOfUseHelper.create(a -> {
			a.setCreatedBy(userThreeId.toString());
			a.getSubjectIds().get(0).setId(file.getId());
		});
		LockAccessRequirement lock = lockHelper.create(a -> {
			a.setCreatedBy(userThreeId.toString());
			a.getSubjectIds().get(0).setId(file.getId());
		});
		ManagedACTAccessRequirement managed = managedHelper.create(a -> {
			a.setCreatedBy(userThreeId.toString());
			a.getSubjectIds().get(0).setId(file.getId());
		});

		List<Long> subjectIds = KeyFactory.stringToKey(Arrays.asList(file.getId()));
		// call under test
		List<UsersRestrictionStatus> results = new ArrayList<>(
				accessRestrictionStatusDao.getEntityStatusAsMap(subjectIds, userOneId, Set.of(userOneId)).values());
		validateBasicSubjectStatus(subjectIds, results, userOneId);

		List<UsersRequirementStatus> expected = Arrays.asList(
				new UsersRequirementStatus().withRequirementId(tou.getId()).withRequirementType(AccessRequirementType.TOU)
						.withIsUnmet(true).withIsExemptionEligible(false),
				new UsersRequirementStatus().withRequirementId(lock.getId()).withRequirementType(AccessRequirementType.LOCK)
						.withIsUnmet(true).withIsExemptionEligible(false),
				new UsersRequirementStatus().withRequirementId(managed.getId())
						.withRequirementType(AccessRequirementType.MANAGED_ATC).withIsUnmet(true).withIsExemptionEligible(false));

		UsersRestrictionStatus result = results.get(0);
		assertEquals(expected, result.getAccessRestrictions());
		assertEquals(RestrictionLevel.CONTROLLED_BY_ACT, result.getMostRestrictiveLevel());
	}

	@Test
	public void testGeEntityStatusWithUnmetRestrictionAsFileCreator() {
		setupNodeHierarchy(userTwoId);
		TermsOfUseAccessRequirement projectToU = termsOfUseHelper.create(t -> {
			t.setCreatedBy(userThreeId.toString());
			t.getSubjectIds().get(0).setId(project.getId());
		});
		List<Long> subjectIds = KeyFactory.stringToKey(Arrays.asList(file.getId(), fileTwo.getId()));
		// call under test
		List<UsersRestrictionStatus> results = new ArrayList<>(
				accessRestrictionStatusDao.getEntityStatusAsMap(subjectIds, userTwoId, Set.of(userTwoId)).values());
		validateBasicSubjectStatus(subjectIds, results, userTwoId);

		List<UsersRequirementStatus> expected = Arrays
				.asList(new UsersRequirementStatus().withRequirementId(projectToU.getId())
						.withRequirementType(AccessRequirementType.TOU).withIsUnmet(false).withIsExemptionEligible(false));

		UsersRestrictionStatus result = results.get(0);
		assertEquals(expected, result.getAccessRestrictions());

		result = results.get(1);
		assertEquals(expected, result.getAccessRestrictions());
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, result.getMostRestrictiveLevel());
	}

	@Test
	public void testGeEntityStatusWithApprovedRestriction() {
		setupNodeHierarchy(userTwoId);
		TermsOfUseAccessRequirement projectToU = termsOfUseHelper.create(t -> {
			t.setCreatedBy(userThreeId.toString());
			t.getSubjectIds().get(0).setId(project.getId());
		});
		accessApprovalHelper.create(a -> {
			a.setCreatedBy(userThreeId.toString());
			a.setSubmitterId(userTwoId.toString());
			a.setRequirementId(projectToU.getId());
			a.setRequirementVersion(projectToU.getVersionNumber());
			a.setState(ApprovalState.APPROVED);
			a.setAccessorId(userOneId.toString());
		});

		List<Long> subjectIds = KeyFactory.stringToKey(Arrays.asList(file.getId()));
		// call under test
		List<UsersRestrictionStatus> results = new ArrayList<>(
				accessRestrictionStatusDao.getEntityStatusAsMap(subjectIds, userOneId, Set.of(userOneId)).values());
		validateBasicSubjectStatus(subjectIds, results, userOneId);

		List<UsersRequirementStatus> expected = Arrays
				.asList(new UsersRequirementStatus().withRequirementId(projectToU.getId())
						.withRequirementType(AccessRequirementType.TOU).withIsUnmet(false).withIsExemptionEligible(false));

		UsersRestrictionStatus result = results.get(0);
		assertEquals(expected, result.getAccessRestrictions());
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, result.getMostRestrictiveLevel());
	}

	@Test
	public void testGeEntityStatusWithRevokedRestriction() {
		setupNodeHierarchy(userTwoId);
		TermsOfUseAccessRequirement projectToU = termsOfUseHelper.create(t -> {
			t.setCreatedBy(userThreeId.toString());
			t.getSubjectIds().get(0).setId(project.getId());
		});
		accessApprovalHelper.create(a -> {
			a.setCreatedBy(userThreeId.toString());
			a.setSubmitterId(userTwoId.toString());
			a.setRequirementId(projectToU.getId());
			a.setRequirementVersion(projectToU.getVersionNumber());
			a.setState(ApprovalState.REVOKED);
			a.setAccessorId(userOneId.toString());
		});

		List<Long> subjectIds = KeyFactory.stringToKey(Arrays.asList(file.getId()));
		// call under test
		List<UsersRestrictionStatus> results = new ArrayList<>(
				accessRestrictionStatusDao.getEntityStatusAsMap(subjectIds, userOneId, Set.of(userOneId)).values());
		validateBasicSubjectStatus(subjectIds, results, userOneId);

		List<UsersRequirementStatus> expected = Arrays
				.asList(new UsersRequirementStatus().withRequirementId(projectToU.getId())
						.withRequirementType(AccessRequirementType.TOU).withIsUnmet(true).withIsExemptionEligible(false));

		UsersRestrictionStatus result = results.get(0);
		assertEquals(expected, result.getAccessRestrictions());
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, result.getMostRestrictiveLevel());
	}

	@Test
	public void testGeEntityStatusWithRevokeAndApproved() {
		setupNodeHierarchy(userTwoId);
		TermsOfUseAccessRequirement projectToUV1 = termsOfUseHelper.create(t -> {
			t.setCreatedBy(userThreeId.toString());
			t.getSubjectIds().get(0).setId(project.getId());
		});

		List<Long> subjectIds = KeyFactory.stringToKey(Arrays.asList(file.getId()));

		// create a second version of this ToU
		TermsOfUseAccessRequirement toUpdate = (TermsOfUseAccessRequirement) accessRequirementDAO
				.get(projectToUV1.getId().toString());
		toUpdate.setVersionNumber(toUpdate.getVersionNumber() + 1L);
		TermsOfUseAccessRequirement projectToUV2 = accessRequirementDAO.update(toUpdate);
		// revoke the first version
		accessApprovalHelper.create(a -> {
			a.setCreatedBy(userThreeId.toString());
			a.setSubmitterId(userTwoId.toString());
			a.setRequirementId(projectToUV1.getId());
			a.setRequirementVersion(projectToUV1.getVersionNumber());
			a.setState(ApprovalState.REVOKED);
			a.setAccessorId(userOneId.toString());
		});

		// approve the second version
		accessApprovalHelper.create(a -> {
			a.setCreatedBy(userThreeId.toString());
			a.setSubmitterId(userTwoId.toString());
			a.setRequirementId(projectToUV2.getId());
			a.setRequirementVersion(projectToUV2.getVersionNumber());
			a.setState(ApprovalState.APPROVED);
			a.setAccessorId(userOneId.toString());
		});

		// call under test
		List<UsersRestrictionStatus> results = new ArrayList<>(
				accessRestrictionStatusDao.getEntityStatusAsMap(subjectIds, userOneId, Set.of(userOneId)).values());
		validateBasicSubjectStatus(subjectIds, results, userOneId);

		List<UsersRequirementStatus> expected = Arrays
				.asList(new UsersRequirementStatus().withRequirementId(projectToUV2.getId())
						.withRequirementType(AccessRequirementType.TOU).withIsUnmet(false).withIsExemptionEligible(false));

		UsersRestrictionStatus result = results.get(0);
		assertEquals(expected, result.getAccessRestrictions());
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, result.getMostRestrictiveLevel());
	}

	@Test
	public void testGeEntityStatusWithMultipleRevoke() {
		setupNodeHierarchy(userTwoId);
		TermsOfUseAccessRequirement projectToUV1 = termsOfUseHelper.create(t -> {
			t.setCreatedBy(userThreeId.toString());
			t.getSubjectIds().get(0).setId(project.getId());
		});

		List<Long> subjectIds = KeyFactory.stringToKey(Arrays.asList(file.getId()));

		// create a second version of this ToU
		TermsOfUseAccessRequirement toUpdate = (TermsOfUseAccessRequirement) accessRequirementDAO
				.get(projectToUV1.getId().toString());
		toUpdate.setVersionNumber(toUpdate.getVersionNumber() + 1L);
		TermsOfUseAccessRequirement projectToUV2 = accessRequirementDAO.update(toUpdate);
		// revoke the first version
		accessApprovalHelper.create(a -> {
			a.setCreatedBy(userThreeId.toString());
			a.setSubmitterId(userTwoId.toString());
			a.setRequirementId(projectToUV1.getId());
			a.setRequirementVersion(projectToUV1.getVersionNumber());
			a.setState(ApprovalState.REVOKED);
			a.setAccessorId(userOneId.toString());
		});

		// approve the second version
		accessApprovalHelper.create(a -> {
			a.setCreatedBy(userThreeId.toString());
			a.setSubmitterId(userTwoId.toString());
			a.setRequirementId(projectToUV2.getId());
			a.setRequirementVersion(projectToUV2.getVersionNumber());
			a.setState(ApprovalState.REVOKED);
			a.setAccessorId(userOneId.toString());
		});

		// call under test
		List<UsersRestrictionStatus> results = new ArrayList<>(
				accessRestrictionStatusDao.getEntityStatusAsMap(subjectIds, userOneId, Set.of(userOneId)).values());
		validateBasicSubjectStatus(subjectIds, results, userOneId);

		List<UsersRequirementStatus> expected = Arrays
				.asList(new UsersRequirementStatus().withRequirementId(projectToUV2.getId())
						.withRequirementType(AccessRequirementType.TOU).withIsUnmet(true).withIsExemptionEligible(false));

		UsersRestrictionStatus result = results.get(0);
		assertEquals(expected, result.getAccessRestrictions());
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, result.getMostRestrictiveLevel());
	}

	/**
	 * If a user has been approved for one version of an access requirement, they
	 * are still approved even if a new version of the requirement is created.
	 */
	@Test
	public void testGeEntityStatusWithWithOldApproval() {
		setupNodeHierarchy(userTwoId);
		TermsOfUseAccessRequirement projectToUV1 = termsOfUseHelper.create(t -> {
			t.setCreatedBy(userThreeId.toString());
			t.getSubjectIds().get(0).setId(project.getId());
		});

		// approve the first version only
		accessApprovalHelper.create(a -> {
			a.setCreatedBy(userThreeId.toString());
			a.setSubmitterId(userTwoId.toString());
			a.setRequirementId(projectToUV1.getId());
			a.setRequirementVersion(projectToUV1.getVersionNumber());
			a.setState(ApprovalState.APPROVED);
			a.setAccessorId(userOneId.toString());
		});

		// create a second version of this ToU that the user has not been approved for.
		TermsOfUseAccessRequirement toUpdate = (TermsOfUseAccessRequirement) accessRequirementDAO
				.get(projectToUV1.getId().toString());
		toUpdate.setVersionNumber(toUpdate.getVersionNumber() + 1L);
		TermsOfUseAccessRequirement projectToUV2 = accessRequirementDAO.update(toUpdate);

		List<Long> subjectIds = KeyFactory.stringToKey(Arrays.asList(file.getId()));

		// call under test
		List<UsersRestrictionStatus> results = new ArrayList<>(
				accessRestrictionStatusDao.getEntityStatusAsMap(subjectIds, userOneId, Set.of(userOneId)).values());
		validateBasicSubjectStatus(subjectIds, results, userOneId);

		List<UsersRequirementStatus> expected = Arrays
				.asList(new UsersRequirementStatus().withRequirementId(projectToUV2.getId())
						.withRequirementType(AccessRequirementType.TOU).withIsUnmet(false).withIsExemptionEligible(false));

		UsersRestrictionStatus result = results.get(0);
		assertEquals(expected, result.getAccessRestrictions());
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, result.getMostRestrictiveLevel());
	}

	@Test
	public void testGetEntityStatusWithApprovedRestrictionAnd2FaRequired() {
		setupNodeHierarchy(userTwoId);

		ManagedACTAccessRequirement managedAr = managedHelper.create(ar -> {
			ar.setCreatedBy(userThreeId.toString());
			ar.getSubjectIds().get(0).setId(project.getId());
			ar.setIsTwoFaRequired(true);
		});

		accessApprovalHelper.create(a -> {
			a.setCreatedBy(userThreeId.toString());
			a.setSubmitterId(userTwoId.toString());
			a.setRequirementId(managedAr.getId());
			a.setRequirementVersion(managedAr.getVersionNumber());
			a.setState(ApprovalState.APPROVED);
			a.setAccessorId(userOneId.toString());
		});

		Long subjectId = KeyFactory.stringToKey(project.getId());

		List<UsersRestrictionStatus> results = new ArrayList<>(
				accessRestrictionStatusDao.getEntityStatusAsMap(List.of(subjectId), userOneId, Set.of(userOneId)).values());

		assertEquals(List.of(
				new UsersRestrictionStatus()
						.withSubjectId(subjectId)
						.withUserId(userOneId)
						.withRestrictionStatus(List.of(
								new UsersRequirementStatus()
										.withRequirementId(managedAr.getId())
										.withRequirementType(AccessRequirementType.MANAGED_ATC)
										.withIsUnmet(false)
										.withIsTwoFaRequired(true)
										.withIsExemptionEligible(false)))
		), results);
	}

	/**
	 * Test eligible exemption for user.
	 */
	@Test
	public void testGetEntityStatusWithIsExemptionEligible() {
		setupNodeHierarchy(userTwoId);

		ManagedACTAccessRequirement managedAr = managedHelper.create(ar -> {
			ar.setCreatedBy(userThreeId.toString());
			ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
			ar.setSubjectIds(List.of(new RestrictableObjectDescriptor()
					.setId(project.getId()).setType(RestrictableObjectType.ENTITY)));
		});

		AccessControlList aclOnAR = aclHelper.create(al->{
					al.setId(managedAr.getId().toString());
					al.setEtag("testetag");
					al.setCreationDate(new Date());
					al.setResourceAccess(Set.of(
							new ResourceAccess().setPrincipalId(Long.valueOf(userTwoId))
									.setAccessType(Collections.singleton(ACCESS_TYPE.EXEMPTION_ELIGIBLE))
					));
		} , ObjectType.ACCESS_REQUIREMENT);

		Long subjectId = KeyFactory.stringToKey(project.getId());

		// call under test the for user has exemption
		List<UsersRestrictionStatus> results = new ArrayList<>(
				accessRestrictionStatusDao.getEntityStatusAsMap(List.of(subjectId), userTwoId, Set.of(userTwoId)).values());

		assertEquals(List.of(
				new UsersRestrictionStatus()
						.withSubjectId(subjectId)
						.withUserId(userTwoId)
						.withRestrictionStatus(List.of(
								new UsersRequirementStatus()
										.withRequirementId(managedAr.getId())
										.withRequirementType(AccessRequirementType.MANAGED_ATC)
										.withIsUnmet(true)
										.withIsTwoFaRequired(false)
										.withIsExemptionEligible(true)
						))
		), results);

		// call under test for the user does not have exemption
		List<UsersRestrictionStatus> resultTwo = new ArrayList<>(
				accessRestrictionStatusDao.getEntityStatusAsMap(List.of(subjectId), userOneId, Set.of(userOneId)).values());

		assertEquals(List.of(
				new UsersRestrictionStatus()
						.withSubjectId(subjectId)
						.withUserId(userOneId)
						.withRestrictionStatus(List.of(
								new UsersRequirementStatus()
										.withRequirementId(managedAr.getId())
										.withRequirementType(AccessRequirementType.MANAGED_ATC)
										.withIsUnmet(true)
										.withIsTwoFaRequired(false)
										.withIsExemptionEligible(false)
						))
		), resultTwo);
	}

	/**
	 * Test for the ACL with exemption and ACL without exemption for the same user. The user is
	 * eligible of exemption for one ACL on AR which has exemption eligible.
	 */
	@Test
	public void testIsExemptionEligibleWhenMultipleACLsONAR() {
		setupNodeHierarchy(userTwoId);
		Long subjectId = KeyFactory.stringToKey(file.getId());

		ManagedACTAccessRequirement managedAr = managedHelper.create(ar -> {
			ar.setCreatedBy(userThreeId.toString());
			ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
			ar.setSubjectIds(List.of(new RestrictableObjectDescriptor()
					.setId(file.getId()).setType(RestrictableObjectType.ENTITY)));
		});

		ManagedACTAccessRequirement managedArTwo = managedHelper.create(ar -> {
			ar.setCreatedBy(userThreeId.toString());
			ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
			ar.setSubjectIds(List.of(new RestrictableObjectDescriptor()
							.setId(file.getId()).setType(RestrictableObjectType.ENTITY)));
		});

		//ACL has exemption eligible
		AccessControlList aclOnAR = aclHelper.create(al->{
			al.setId(managedAr.getId().toString());
			al.setEtag("testetag");
			al.setCreationDate(new Date());
			al.setResourceAccess(Set.of(
					new ResourceAccess().setPrincipalId(Long.valueOf(userTwoId))
							.setAccessType(Collections.singleton(ACCESS_TYPE.EXEMPTION_ELIGIBLE))
			));
		} , ObjectType.ACCESS_REQUIREMENT);

		//ACL does not has exemption eligible
		AccessControlList aclOnARTwo = aclHelper.create(al->{
			al.setId(managedArTwo.getId().toString());
			al.setEtag("testetag");
			al.setCreationDate(new Date());
			al.setResourceAccess(Set.of(
					new ResourceAccess().setPrincipalId(Long.valueOf(userTwoId))
							.setAccessType(Collections.singleton(ACCESS_TYPE.REVIEW_SUBMISSIONS))
			));
		} , ObjectType.ACCESS_REQUIREMENT);


		UsersRestrictionStatus expectedStatusOne = new UsersRestrictionStatus()
				.withSubjectId(subjectId)
				.withUserId(userTwoId)
				.withRestrictionStatus(List.of(
						new UsersRequirementStatus()
								.withRequirementId(managedAr.getId())
								.withRequirementType(AccessRequirementType.MANAGED_ATC)
								.withIsUnmet(false)
								.withIsTwoFaRequired(false)
								.withIsExemptionEligible(true),
						new UsersRequirementStatus()
								.withRequirementId(managedArTwo.getId())
								.withRequirementType(AccessRequirementType.MANAGED_ATC)
								.withIsUnmet(false)
								.withIsTwoFaRequired(false)
								.withIsExemptionEligible(false)));

		// call under test
		List<UsersRestrictionStatus> results = new ArrayList<>(
				accessRestrictionStatusDao.getEntityStatusAsMap(List.of(subjectId), userTwoId, Set.of(userTwoId)).values());

		assertEquals(List.of(expectedStatusOne), results);
	}


	/**
	 * Test for exemption eligible when user is part of multiple user groups have exemption.
	 */
	@Test
	public void testIsExemptionEligibleIsGrantedToUserByMultiplePrincipals() {
		groupMembersDAO.addMembers(teamOneId.toString(),List.of(userTwoId.toString()));
		groupMembersDAO.addMembers(teamTwoId.toString(), List.of(userTwoId.toString()));

		setupNodeHierarchy(userTwoId);
		Long subjectId = KeyFactory.stringToKey(file.getId());

		ManagedACTAccessRequirement managedAr = managedHelper.create(ar -> {
			ar.setCreatedBy(userThreeId.toString());
			ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
			ar.setSubjectIds(List.of(new RestrictableObjectDescriptor()
							.setId(file.getId()).setType(RestrictableObjectType.ENTITY)));
		});

		AccessControlList aclOnAR = aclHelper.create(al->{
			al.setId(managedAr.getId().toString());
			al.setEtag("testetag");
			al.setCreationDate(new Date());
			al.setResourceAccess(Set.of(
					new ResourceAccess().setPrincipalId(Long.valueOf(teamOneId))
							.setAccessType(Collections.singleton(ACCESS_TYPE.EXEMPTION_ELIGIBLE)),
					new ResourceAccess().setPrincipalId(Long.valueOf(teamTwoId))
							.setAccessType(Collections.singleton(ACCESS_TYPE.EXEMPTION_ELIGIBLE))
			));
		} , ObjectType.ACCESS_REQUIREMENT);

		// call under test for user has groups which are exempted
		List<UsersRestrictionStatus> results = new ArrayList<>(
				accessRestrictionStatusDao.getEntityStatusAsMap(List.of(subjectId), userTwoId, Set.of(teamOneId, teamTwoId, userTwoId)).values());

		assertEquals(List.of(
						new UsersRestrictionStatus()
								.withSubjectId(subjectId)
								.withUserId(userTwoId)
								.withRestrictionStatus(List.of(
										new UsersRequirementStatus()
												.withRequirementId(managedAr.getId())
												.withRequirementType(AccessRequirementType.MANAGED_ATC)
												.withIsUnmet(false)
												.withIsTwoFaRequired(false)
												.withIsExemptionEligible(true)))
			), results);

		// call under test for user itself
		List<UsersRestrictionStatus> resultsTwo = new ArrayList<>(
				accessRestrictionStatusDao.getEntityStatusAsMap(List.of(subjectId), userTwoId, Set.of(userTwoId)).values());

		assertEquals(List.of(
						new UsersRestrictionStatus()
								.withSubjectId(subjectId)
								.withUserId(userTwoId)
								.withRestrictionStatus(List.of(
										new UsersRequirementStatus()
												.withRequirementId(managedAr.getId())
												.withRequirementType(AccessRequirementType.MANAGED_ATC)
												.withIsUnmet(false)
												.withIsTwoFaRequired(false)
												.withIsExemptionEligible(false)))
		), resultsTwo);
	}

	/**
	 * Test for exemption eligible when multiple acl of different types are created on Same AR.
	 */
	@Test
	public void testIsExemptionEligibleWhenMultipleACLsOfDifferentTypeShareSameAR() {
		groupMembersDAO.addMembers(teamOneId.toString(),List.of(userTwoId.toString()));

		setupNodeHierarchy(userTwoId);
		Long subjectId = KeyFactory.stringToKey(file.getId());

		ManagedACTAccessRequirement managedAr = managedHelper.create(ar -> {
			ar.setCreatedBy(userThreeId.toString());
			ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
			ar.setSubjectIds(List.of(new RestrictableObjectDescriptor()
							.setId(file.getId()).setType(RestrictableObjectType.ENTITY)));
		});

		AccessControlList aclOnAR = aclHelper.create(al->{
			al.setId(managedAr.getId().toString());
			al.setEtag("testetag");
			al.setCreationDate(new Date());
			al.setResourceAccess(Set.of(
					new ResourceAccess().setPrincipalId(Long.valueOf(teamOneId))
							.setAccessType(Collections.singleton(ACCESS_TYPE.EXEMPTION_ELIGIBLE))
			));
		} , ObjectType.ACCESS_REQUIREMENT);

		AccessControlList aclOnEntity = aclHelper.create(al->{
			al.setId(managedAr.getId().toString());
			al.setEtag("testetag");
			al.setCreationDate(new Date());
			al.setResourceAccess(Set.of(
					new ResourceAccess().setPrincipalId(Long.valueOf(teamOneId))
							.setAccessType(Collections.singleton(ACCESS_TYPE.CREATE))
			));
		} , ObjectType.ENTITY);

		AccessControlList aclOnOrganization = aclHelper.create(al->{
			al.setId(managedAr.getId().toString());
			al.setEtag("testetag");
			al.setCreationDate(new Date());
			al.setResourceAccess(Set.of(
					new ResourceAccess().setPrincipalId(Long.valueOf(teamOneId))
							.setAccessType(Collections.singleton(ACCESS_TYPE.READ))
			));
		} , ObjectType.ORGANIZATION);

		// call under test for user group has exemption eligible
		List<UsersRestrictionStatus> results = new ArrayList<>(
				accessRestrictionStatusDao.getEntityStatusAsMap(List.of(subjectId), userTwoId, Set.of(teamOneId, userTwoId)).values());

		assertEquals(List.of(
						new UsersRestrictionStatus()
								.withSubjectId(subjectId)
								.withUserId(userTwoId)
								.withRestrictionStatus(List.of(
										new UsersRequirementStatus()
												.withRequirementId(managedAr.getId())
												.withRequirementType(AccessRequirementType.MANAGED_ATC)
												.withIsUnmet(false)
												.withIsTwoFaRequired(false)
												.withIsExemptionEligible(true)))
		), results);

		// call under test for user itself
		List<UsersRestrictionStatus> resultsTwo = new ArrayList<>(
				accessRestrictionStatusDao.getEntityStatusAsMap(List.of(subjectId), userTwoId, Set.of(userTwoId)).values());

		assertEquals(List.of(
						new UsersRestrictionStatus()
								.withSubjectId(subjectId)
								.withUserId(userTwoId)
								.withRestrictionStatus(List.of(
										new UsersRequirementStatus()
												.withRequirementId(managedAr.getId())
												.withRequirementType(AccessRequirementType.MANAGED_ATC)
												.withIsUnmet(false)
												.withIsTwoFaRequired(false)
												.withIsExemptionEligible(false)))
		), resultsTwo);
	}

	/**
	 * Test for ACL grants both exemption and non exemption access to the same user
	 */

	@Test
	public void testSameAClGrantsExemptionEligibleAndNonExemptionEligibleToSameUser() {
		groupMembersDAO.addMembers(teamOneId.toString(),List.of(userTwoId.toString(), userOneId.toString()));
		setupNodeHierarchy(userTwoId);
		Long subjectId = KeyFactory.stringToKey(file.getId());

		ManagedACTAccessRequirement managedAr = managedHelper.create(ar -> {
			ar.setCreatedBy(userThreeId.toString());
			ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
			ar.setSubjectIds(List.of(new RestrictableObjectDescriptor()
							.setId(file.getId()).setType(RestrictableObjectType.ENTITY)));
		});

		AccessControlList aclOnAR = aclHelper.create(al->{
			al.setId(managedAr.getId().toString());
			al.setEtag("testetag");
			al.setCreationDate(new Date());
			al.setResourceAccess(Set.of(
					new ResourceAccess().setPrincipalId(Long.valueOf(userTwoId))
							.setAccessType(Collections.singleton(ACCESS_TYPE.EXEMPTION_ELIGIBLE)),
					new ResourceAccess().setPrincipalId(Long.valueOf(userTwoId))
							.setAccessType(Collections.singleton(ACCESS_TYPE.REVIEW_SUBMISSIONS))
			));
		} , ObjectType.ACCESS_REQUIREMENT);

		// call under test for user has exemption as individual, User is also member of group has no exemption
		List<UsersRestrictionStatus> results = new ArrayList<>(accessRestrictionStatusDao.getEntityStatusAsMap(List.of(subjectId),
				userTwoId, Set.of(userTwoId, teamOneId)).values());

		assertEquals(List.of(
				new UsersRestrictionStatus()
						.withSubjectId(subjectId)
						.withUserId(userTwoId)
						.withRestrictionStatus(List.of(
								new UsersRequirementStatus()
										.withRequirementId(managedAr.getId())
										.withRequirementType(AccessRequirementType.MANAGED_ATC)
										.withIsUnmet(false)
										.withIsTwoFaRequired(false)
										.withIsExemptionEligible(true)))
		), results);

		// call under test for user and its groups have no exemption
		List<UsersRestrictionStatus> resultsTwo = new ArrayList<>(
				accessRestrictionStatusDao.getEntityStatusAsMap(List.of(subjectId), userOneId, Set.of(userOneId, teamOneId)).values());

		assertEquals(List.of(
						new UsersRestrictionStatus()
								.withSubjectId(subjectId)
								.withUserId(userOneId)
								.withRestrictionStatus(List.of(
										new UsersRequirementStatus()
												.withRequirementId(managedAr.getId())
												.withRequirementType(AccessRequirementType.MANAGED_ATC)
												.withIsUnmet(true)
												.withIsTwoFaRequired(false)
												.withIsExemptionEligible(false)))
		), resultsTwo);
	}


	/**
	 * Test for exemption eligible is granted on AR but not to the user
	 */
	@Test
	public void testIsExemptionEligibleOnARButNotForUser() {
		setupNodeHierarchy(userTwoId);
		Long subjectId = KeyFactory.stringToKey(file.getId());

		ManagedACTAccessRequirement managedAr = managedHelper.create(ar -> {
			ar.setCreatedBy(userThreeId.toString());
			ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
			ar.setSubjectIds(List.of(new RestrictableObjectDescriptor()
							.setId(file.getId()).setType(RestrictableObjectType.ENTITY)));
		});

		AccessControlList aclOnAR = aclHelper.create(al->{
			al.setId(managedAr.getId().toString());
			al.setEtag("testetag");
			al.setCreationDate(new Date());
			al.setResourceAccess(Set.of(
					new ResourceAccess().setPrincipalId(Long.valueOf(userOneId))
							.setAccessType(Collections.singleton(ACCESS_TYPE.EXEMPTION_ELIGIBLE))
			));
		} , ObjectType.ACCESS_REQUIREMENT);

		// call under test for user does not have exemption for AR
		List<UsersRestrictionStatus> results = new ArrayList<>(
				accessRestrictionStatusDao.getEntityStatusAsMap(List.of(subjectId), userTwoId, Set.of(userTwoId)).values());

		assertEquals(List.of(
				new UsersRestrictionStatus()
						.withSubjectId(subjectId)
						.withUserId(userTwoId)
						.withRestrictionStatus(List.of(
										new UsersRequirementStatus()
												.withRequirementId(managedAr.getId())
												.withRequirementType(AccessRequirementType.MANAGED_ATC)
												.withIsUnmet(false)
												.withIsTwoFaRequired(false)
												.withIsExemptionEligible(false)
						))
		), results);

		// call under test for has exemption for AR
		List<UsersRestrictionStatus> resultsTwo = new ArrayList<>(
				accessRestrictionStatusDao.getEntityStatusAsMap(List.of(subjectId), userOneId, Set.of(userOneId)).values());

		assertEquals(List.of(
				new UsersRestrictionStatus()
						.withSubjectId(subjectId)
						.withUserId(userOneId)
						.withRestrictionStatus(List.of(
								new UsersRequirementStatus()
										.withRequirementId(managedAr.getId())
										.withRequirementType(AccessRequirementType.MANAGED_ATC)
										.withIsUnmet(true)
										.withIsTwoFaRequired(false)
										.withIsExemptionEligible(true)
						))
		), resultsTwo);
	}

	@Test
	public void testGeEntityStatusWithUnmetRestrictionAnd2FaRequired() {
		setupNodeHierarchy(userTwoId);

		ManagedACTAccessRequirement managedAr = managedHelper.create(ar -> {
			ar.setCreatedBy(userThreeId.toString());
			ar.getSubjectIds().get(0).setId(project.getId());
			ar.setIsTwoFaRequired(true);
		});

		Long subjectId = KeyFactory.stringToKey(project.getId());

		// call under test
		List<UsersRestrictionStatus> results = new ArrayList<>(
				accessRestrictionStatusDao.getEntityStatusAsMap(List.of(subjectId), userOneId, Set.of(userOneId)).values());

		assertEquals(List.of(
				new UsersRestrictionStatus()
						.withSubjectId(subjectId)
						.withUserId(userOneId)
						.withRestrictionStatus(List.of(
								new UsersRequirementStatus()
										.withRequirementId(managedAr.getId())
										.withRequirementType(AccessRequirementType.MANAGED_ATC)
										.withIsUnmet(true)
										.withIsTwoFaRequired(true)
										.withIsExemptionEligible(false)
						))
		), results);
	}

	@Test
	public void testGetNonEntityStatusWithNullSubjectsForTeam() {
		List<Long> subjectIds = null;
		RestrictableObjectType subjectType = RestrictableObjectType.TEAM;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			accessRestrictionStatusDao.getNonEntityStatus(subjectIds, subjectType, userTwoId);
		}).getMessage();
		assertEquals("subjectIds is required.", message);
	}

	@Test
	public void testGetNonEntityStatusWithNullSubjectType() {
		List<Long> subjectIds = Arrays.asList(123L);
		RestrictableObjectType subjectType = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			accessRestrictionStatusDao.getNonEntityStatus(subjectIds, subjectType, userTwoId);
		}).getMessage();
		assertEquals("subjectType is required.", message);
	}

	@Test
	public void testGetNonEntityStatusWithEntitySubjectType() {
		List<Long> subjectIds = Arrays.asList(123L);
		RestrictableObjectType subjectType = RestrictableObjectType.ENTITY;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			accessRestrictionStatusDao.getNonEntityStatus(subjectIds, subjectType, userTwoId);
		}).getMessage();
		assertEquals("This method can only be used for non-entity subject types.", message);
	}

	@Test
	public void testGetNonEntityStatusWithNullUserIdForTeam() {
		List<Long> subjectIds = Arrays.asList(123L);
		RestrictableObjectType subjectType = RestrictableObjectType.TEAM;
		Long userId = null;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			accessRestrictionStatusDao.getNonEntityStatus(subjectIds, subjectType, userId);
		}).getMessage();
		assertEquals("userId is required.", message);
	}

	@Test
	public void testGetNonEntityStatusWithNoEmptySubjects() {
		List<Long> subjectIds = Collections.emptyList();
		RestrictableObjectType subjectType = RestrictableObjectType.TEAM;
		// call under test
		List<UsersRestrictionStatus> results = accessRestrictionStatusDao.getNonEntityStatus(subjectIds, subjectType, userTwoId);
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}

	@Test
	public void testGetNonEntityStatusWithNoRestrictions() {
		List<Long> subjectIds = Arrays.asList(teamOneId, teamTwoId);
		RestrictableObjectType subjectType = RestrictableObjectType.TEAM;
		// call under test
		List<UsersRestrictionStatus> results = accessRestrictionStatusDao.getNonEntityStatus(subjectIds, subjectType, userTwoId);
		validateBasicSubjectStatus(subjectIds, results, userTwoId);
		UsersRestrictionStatus result = results.get(0);
		assertNotNull(result.getAccessRestrictions());
		assertTrue(result.getAccessRestrictions().isEmpty());

		result = results.get(1);
		assertNotNull(result.getAccessRestrictions());
		assertTrue(result.getAccessRestrictions().isEmpty());
		assertEquals(RestrictionLevel.OPEN, result.getMostRestrictiveLevel());
	}

	@Test
	public void testGetNonEntityStatusWithSingleUnmetRestrictions() {
		TermsOfUseAccessRequirement tou = termsOfUseHelper.create(t -> {
			t.setCreatedBy(userThreeId.toString());
			RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
			rod.setId(teamOneId.toString());
			rod.setType(RestrictableObjectType.TEAM);
			t.setSubjectIds(Arrays.asList(rod));
		});

		List<Long> subjectIds = Arrays.asList(teamOneId, teamTwoId);
		RestrictableObjectType subjectType = RestrictableObjectType.TEAM;
		// call under test
		List<UsersRestrictionStatus> results = accessRestrictionStatusDao.getNonEntityStatus(subjectIds, subjectType, userTwoId);
		validateBasicSubjectStatus(subjectIds, results, userTwoId);

		List<UsersRequirementStatus> expected = Arrays.asList(new UsersRequirementStatus()
				.withRequirementId(tou.getId()).withRequirementType(AccessRequirementType.TOU)
				.withIsUnmet(true).withIsExemptionEligible(false));

		UsersRestrictionStatus result = results.get(0);
		assertEquals(expected, result.getAccessRestrictions());
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, result.getMostRestrictiveLevel());

		result = results.get(1);
		assertNotNull(result.getAccessRestrictions());
		assertTrue(result.getAccessRestrictions().isEmpty());
		assertEquals(RestrictionLevel.OPEN, result.getMostRestrictiveLevel());
	}

	@Test
	public void testGetNonEntityStatusWithSingletRestrictionApproved() {
		TermsOfUseAccessRequirement tou = termsOfUseHelper.create(t -> {
			t.setCreatedBy(userThreeId.toString());
			RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
			rod.setId(teamOneId.toString());
			rod.setType(RestrictableObjectType.TEAM);
			t.setSubjectIds(Arrays.asList(rod));
		});
		accessApprovalHelper.create(a -> {
			a.setCreatedBy(userThreeId.toString());
			a.setSubmitterId(userTwoId.toString());
			a.setRequirementId(tou.getId());
			a.setRequirementVersion(tou.getVersionNumber());
			a.setState(ApprovalState.APPROVED);
			a.setAccessorId(userOneId.toString());
		});
		List<Long> subjectIds = Arrays.asList(teamOneId);
		RestrictableObjectType subjectType = RestrictableObjectType.TEAM;
		// call under test
		List<UsersRestrictionStatus> results = accessRestrictionStatusDao.getNonEntityStatus(subjectIds, subjectType, userOneId);
		validateBasicSubjectStatus(subjectIds, results, userOneId);

		List<UsersRequirementStatus> expected = Arrays.asList(new UsersRequirementStatus()
				.withRequirementId(tou.getId()).withRequirementType(AccessRequirementType.TOU)
				.withIsUnmet(false).withIsExemptionEligible(false));

		UsersRestrictionStatus result = results.get(0);
		assertEquals(expected, result.getAccessRestrictions());
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, result.getMostRestrictiveLevel());
	}

	@Test
	public void testGetNonEntityStatusWithSingletRestrictionRevoked() {
		TermsOfUseAccessRequirement tou = termsOfUseHelper.create(t -> {
			t.setCreatedBy(userThreeId.toString());
			RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
			rod.setId(teamOneId.toString());
			rod.setType(RestrictableObjectType.TEAM);
			t.setSubjectIds(Arrays.asList(rod));
		});
		accessApprovalHelper.create(a -> {
			a.setCreatedBy(userThreeId.toString());
			a.setSubmitterId(userTwoId.toString());
			a.setRequirementId(tou.getId());
			a.setRequirementVersion(tou.getVersionNumber());
			a.setState(ApprovalState.REVOKED);
			a.setAccessorId(userOneId.toString());
		});
		List<Long> subjectIds = Arrays.asList(teamOneId);
		RestrictableObjectType subjectType = RestrictableObjectType.TEAM;
		// call under test
		List<UsersRestrictionStatus> results = accessRestrictionStatusDao.getNonEntityStatus(subjectIds, subjectType, userOneId);
		validateBasicSubjectStatus(subjectIds, results, userOneId);

		List<UsersRequirementStatus> expected = Arrays.asList(new UsersRequirementStatus()
				.withRequirementId(tou.getId()).withRequirementType(AccessRequirementType.TOU)
				.withIsUnmet(true).withIsExemptionEligible(false));

		UsersRestrictionStatus result = results.get(0);
		assertEquals(expected, result.getAccessRestrictions());
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, result.getMostRestrictiveLevel());
	}

	@Test
	public void testGetNonEntityStatusWithMultipleUnmetRestriction() {
		TermsOfUseAccessRequirement tou = termsOfUseHelper.create(t -> {
			t.setCreatedBy(userThreeId.toString());
			RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
			rod.setId(teamOneId.toString());
			rod.setType(RestrictableObjectType.TEAM);
			t.setSubjectIds(Arrays.asList(rod));
		});

		LockAccessRequirement lock = lockHelper.create(t -> {
			t.setCreatedBy(userThreeId.toString());
			RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
			rod.setId(teamOneId.toString());
			rod.setType(RestrictableObjectType.TEAM);
			t.setSubjectIds(Arrays.asList(rod));
		});

		List<Long> subjectIds = Arrays.asList(teamOneId);
		RestrictableObjectType subjectType = RestrictableObjectType.TEAM;
		// call under test
		List<UsersRestrictionStatus> results = accessRestrictionStatusDao.getNonEntityStatus(subjectIds, subjectType, userOneId);
		validateBasicSubjectStatus(subjectIds, results, userOneId);

		List<UsersRequirementStatus> expected = Arrays.asList(
				new UsersRequirementStatus().withRequirementId(tou.getId()).withRequirementType(AccessRequirementType.TOU)
						.withIsUnmet(true).withIsExemptionEligible(false),
				new UsersRequirementStatus().withRequirementId(lock.getId()).withRequirementType(AccessRequirementType.LOCK)
						.withIsUnmet(true).withIsExemptionEligible(false));

		UsersRestrictionStatus result = results.get(0);
		assertEquals(expected, result.getAccessRestrictions());
		assertEquals(RestrictionLevel.CONTROLLED_BY_ACT, result.getMostRestrictiveLevel());
	}

	@Test
	public void testGetNonEntityStatusWithMultipleRestrictionMixed() {
		TermsOfUseAccessRequirement tou = termsOfUseHelper.create(t -> {
			t.setCreatedBy(userThreeId.toString());
			RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
			rod.setId(teamOneId.toString());
			rod.setType(RestrictableObjectType.TEAM);
			t.setSubjectIds(Arrays.asList(rod));
		});

		LockAccessRequirement lock = lockHelper.create(t -> {
			t.setCreatedBy(userThreeId.toString());
			RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
			rod.setId(teamOneId.toString());
			rod.setType(RestrictableObjectType.TEAM);
			t.setSubjectIds(Arrays.asList(rod));
		});

		accessApprovalHelper.create(a -> {
			a.setCreatedBy(userThreeId.toString());
			a.setSubmitterId(userTwoId.toString());
			a.setRequirementId(lock.getId());
			a.setRequirementVersion(lock.getVersionNumber());
			a.setState(ApprovalState.APPROVED);
			a.setAccessorId(userOneId.toString());
		});

		List<Long> subjectIds = Arrays.asList(teamOneId);
		RestrictableObjectType subjectType = RestrictableObjectType.TEAM;
		// call under test
		List<UsersRestrictionStatus> results = accessRestrictionStatusDao.getNonEntityStatus(subjectIds, subjectType, userOneId);
		validateBasicSubjectStatus(subjectIds, results, userOneId);

		List<UsersRequirementStatus> expected = Arrays.asList(
				new UsersRequirementStatus().withRequirementId(tou.getId()).withRequirementType(AccessRequirementType.TOU)
						.withIsUnmet(true).withIsExemptionEligible(false),
				new UsersRequirementStatus().withRequirementId(lock.getId()).withRequirementType(AccessRequirementType.LOCK)
						.withIsUnmet(false).withIsExemptionEligible(false));

		UsersRestrictionStatus result = results.get(0);
		assertEquals(expected, result.getAccessRestrictions());
		assertEquals(RestrictionLevel.CONTROLLED_BY_ACT, result.getMostRestrictiveLevel());
	}

	@Test
	public void testGetNonEntityStatusWithMultipleRestrictionApproved() {
		TermsOfUseAccessRequirement tou = termsOfUseHelper.create(t -> {
			t.setCreatedBy(userThreeId.toString());
			RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
			rod.setId(teamOneId.toString());
			rod.setType(RestrictableObjectType.TEAM);
			t.setSubjectIds(Arrays.asList(rod));
		});

		accessApprovalHelper.create(a -> {
			a.setCreatedBy(userThreeId.toString());
			a.setSubmitterId(userTwoId.toString());
			a.setRequirementId(tou.getId());
			a.setRequirementVersion(tou.getVersionNumber());
			a.setState(ApprovalState.APPROVED);
			a.setAccessorId(userOneId.toString());
		});

		LockAccessRequirement lock = lockHelper.create(t -> {
			t.setCreatedBy(userThreeId.toString());
			RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
			rod.setId(teamOneId.toString());
			rod.setType(RestrictableObjectType.TEAM);
			t.setSubjectIds(Arrays.asList(rod));
		});

		accessApprovalHelper.create(a -> {
			a.setCreatedBy(userThreeId.toString());
			a.setSubmitterId(userTwoId.toString());
			a.setRequirementId(lock.getId());
			a.setRequirementVersion(lock.getVersionNumber());
			a.setState(ApprovalState.APPROVED);
			a.setAccessorId(userOneId.toString());
		});

		List<Long> subjectIds = Arrays.asList(teamOneId);
		RestrictableObjectType subjectType = RestrictableObjectType.TEAM;
		// call under test
		List<UsersRestrictionStatus> results = accessRestrictionStatusDao.getNonEntityStatus(subjectIds, subjectType, userOneId);
		validateBasicSubjectStatus(subjectIds, results, userOneId);

		List<UsersRequirementStatus> expected = Arrays.asList(
				new UsersRequirementStatus().withRequirementId(tou.getId()).withRequirementType(AccessRequirementType.TOU)
						.withIsUnmet(false).withIsExemptionEligible(false),
				new UsersRequirementStatus().withRequirementId(lock.getId()).withRequirementType(AccessRequirementType.LOCK)
						.withIsUnmet(false).withIsExemptionEligible(false));

		UsersRestrictionStatus result = results.get(0);
		assertEquals(expected, result.getAccessRestrictions());
		assertEquals(RestrictionLevel.CONTROLLED_BY_ACT, result.getMostRestrictiveLevel());
	}

	@Test
	public void testGetNonEntityStatusWithUnmetRestrictionWithMultipleSubjects() {
		TermsOfUseAccessRequirement tou = termsOfUseHelper.create(t -> {
			t.setCreatedBy(userThreeId.toString());
			RestrictableObjectDescriptor rod1 = new RestrictableObjectDescriptor();
			rod1.setId(teamOneId.toString());
			rod1.setType(RestrictableObjectType.TEAM);
			RestrictableObjectDescriptor rod2 = new RestrictableObjectDescriptor();
			rod2.setId(teamTwoId.toString());
			rod2.setType(RestrictableObjectType.TEAM);
			t.setSubjectIds(Arrays.asList(rod1, rod2));
		});

		List<Long> subjectIds = Arrays.asList(teamOneId, teamTwoId);
		RestrictableObjectType subjectType = RestrictableObjectType.TEAM;
		// call under test
		List<UsersRestrictionStatus> results = accessRestrictionStatusDao.getNonEntityStatus(subjectIds, subjectType, userTwoId);
		validateBasicSubjectStatus(subjectIds, results, userTwoId);

		List<UsersRequirementStatus> expected = Arrays.asList(new UsersRequirementStatus()
				.withRequirementId(tou.getId()).withRequirementType(AccessRequirementType.TOU)
				.withIsUnmet(true).withIsExemptionEligible(false));

		UsersRestrictionStatus result = results.get(0);
		assertEquals(expected, result.getAccessRestrictions());

		result = results.get(1);
		assertEquals(expected, result.getAccessRestrictions());
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, result.getMostRestrictiveLevel());
	}

	@Test
	public void testGeNonEntityStatusWithRevokeAndApproved() {

		TermsOfUseAccessRequirement touV1 = termsOfUseHelper.create(t -> {
			t.setCreatedBy(userThreeId.toString());
			RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
			rod.setId(teamOneId.toString());
			rod.setType(RestrictableObjectType.TEAM);
			t.setSubjectIds(Arrays.asList(rod));
		});

		// create a second version of this ToU
		TermsOfUseAccessRequirement toUpdate = (TermsOfUseAccessRequirement) accessRequirementDAO
				.get(touV1.getId().toString());
		toUpdate.setVersionNumber(toUpdate.getVersionNumber() + 1L);
		TermsOfUseAccessRequirement touV2 = accessRequirementDAO.update(toUpdate);
		// revoke the first version
		accessApprovalHelper.create(a -> {
			a.setCreatedBy(userThreeId.toString());
			a.setSubmitterId(userTwoId.toString());
			a.setRequirementId(touV1.getId());
			a.setRequirementVersion(touV1.getVersionNumber());
			a.setState(ApprovalState.REVOKED);
			a.setAccessorId(userOneId.toString());
		});

		// approve the second version
		accessApprovalHelper.create(a -> {
			a.setCreatedBy(userThreeId.toString());
			a.setSubmitterId(userTwoId.toString());
			a.setRequirementId(touV2.getId());
			a.setRequirementVersion(touV2.getVersionNumber());
			a.setState(ApprovalState.APPROVED);
			a.setAccessorId(userOneId.toString());
		});

		List<Long> subjectIds = Arrays.asList(teamOneId);
		// call under test
		List<UsersRestrictionStatus> results = accessRestrictionStatusDao.getNonEntityStatus(subjectIds,
				RestrictableObjectType.TEAM, userOneId);
		validateBasicSubjectStatus(subjectIds, results, userOneId);

		List<UsersRequirementStatus> expected = Arrays.asList(new UsersRequirementStatus()
				.withRequirementId(touV2.getId()).withRequirementType(AccessRequirementType.TOU)
				.withIsUnmet(false).withIsExemptionEligible(false));

		UsersRestrictionStatus result = results.get(0);
		assertEquals(expected, result.getAccessRestrictions());
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, result.getMostRestrictiveLevel());
	}

	@Test
	public void testGetSubjectStatusWithNullSubjectsForEntity() {
		List<Long> subjectIds = null;
		Long userId = userOneId;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			accessRestrictionStatusDao.getEntityStatusAsMap(subjectIds, userId, Set.of(userId));
		}).getMessage();
		assertEquals("entityIds is required.", message);
	}

	@Test
	public void testGetSubjectStatusWithNullSubjectType() {
		List<Long> subjectIds = Arrays.asList(teamOneId);
		RestrictableObjectType subjectType = null;
		Long userId = userOneId;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			accessRestrictionStatusDao.getNonEntityStatus(subjectIds, subjectType, userId);
		}).getMessage();
		assertEquals("subjectType is required.", message);
	}

	@Test
	public void testGetSubjectStatusWithNullUserIdForEntity() {
		List<Long> subjectIds = Arrays.asList(teamOneId);
		RestrictableObjectType subjectType = RestrictableObjectType.ENTITY;
		Long userId = userOneId = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			accessRestrictionStatusDao.getEntityStatusAsMap(subjectIds, userId, Collections.emptySet());
		}).getMessage();
		assertEquals("userId is required.", message);
	}

	@Test
	public void testGeSubjectStatusWithEnity() {
		setupNodeHierarchy(userTwoId);

		TermsOfUseAccessRequirement projectToU = termsOfUseHelper.create(t -> {
			t.setCreatedBy(userThreeId.toString());
			t.getSubjectIds().get(0).setId(project.getId());
		});
		List<Long> subjectIds = KeyFactory.stringToKey(Arrays.asList(file.getId(), fileTwo.getId()));
		// call under test
		List<UsersRestrictionStatus> results = new ArrayList<>(accessRestrictionStatusDao.getEntityStatusAsMap(subjectIds,
				userOneId, Set.of(userOneId)).values());
		validateBasicSubjectStatus(subjectIds, results, userOneId);

		List<UsersRequirementStatus> expected = Arrays
				.asList(new UsersRequirementStatus().withRequirementId(projectToU.getId())
						.withRequirementType(AccessRequirementType.TOU).withIsUnmet(true).withIsExemptionEligible(false));

		UsersRestrictionStatus result = results.get(0);
		assertEquals(expected, result.getAccessRestrictions());
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, result.getMostRestrictiveLevel());

		result = results.get(1);
		assertEquals(expected, result.getAccessRestrictions());
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, result.getMostRestrictiveLevel());
	}

	@Test
	public void testGetSubjectStatusWithNonEntity() {
		TermsOfUseAccessRequirement tou = termsOfUseHelper.create(t -> {
			t.setCreatedBy(userThreeId.toString());
			RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
			rod.setId(teamOneId.toString());
			rod.setType(RestrictableObjectType.TEAM);
			t.setSubjectIds(Arrays.asList(rod));
		});

		accessApprovalHelper.create(a -> {
			a.setCreatedBy(userThreeId.toString());
			a.setSubmitterId(userTwoId.toString());
			a.setRequirementId(tou.getId());
			a.setRequirementVersion(tou.getVersionNumber());
			a.setState(ApprovalState.APPROVED);
			a.setAccessorId(userOneId.toString());
		});

		List<Long> subjectIds = Arrays.asList(teamOneId);
		RestrictableObjectType subjectType = RestrictableObjectType.TEAM;
		// call under test
		List<UsersRestrictionStatus> results = accessRestrictionStatusDao.getNonEntityStatus(subjectIds, subjectType, userOneId);
		validateBasicSubjectStatus(subjectIds, results, userOneId);

		List<UsersRequirementStatus> expected = Arrays.asList(new UsersRequirementStatus()
				.withRequirementId(tou.getId()).withRequirementType(AccessRequirementType.TOU)
				.withIsUnmet(false).withIsExemptionEligible(false));

		UsersRestrictionStatus result = results.get(0);
		assertEquals(expected, result.getAccessRestrictions());
		assertEquals(RestrictionLevel.RESTRICTED_BY_TERMS_OF_USE, result.getMostRestrictiveLevel());
	}

	/**
	 * Basic validation of the SubjectStatus results against the input subject ids.
	 * 
	 * @param subjectIds
	 * @param results
	 * @param userId
	 */
	void validateBasicSubjectStatus(List<Long> subjectIds, List<UsersRestrictionStatus> results, Long userId) {
		assertNotNull(subjectIds);
		assertNotNull(results);
		assertEquals(subjectIds.size(), results.size());
		for (int i = 0; i < subjectIds.size(); i++) {
			Long subjectId = subjectIds.get(i);
			assertNotNull(subjectId);
			UsersRestrictionStatus status = results.get(i);
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

		project = nodeDaoHelper.create(n -> {
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

}
