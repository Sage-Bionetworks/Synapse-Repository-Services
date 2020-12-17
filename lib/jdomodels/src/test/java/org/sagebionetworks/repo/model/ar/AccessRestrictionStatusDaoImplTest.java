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
import org.sagebionetworks.repo.model.AccessApprovalDAO;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
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

	Long userOneId;
	Long userTwoId;

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
		userOneId = userGroupDAO.create(ug);
		userTwoId = userGroupDAO.create(ug);
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
		TermsOfUseAccessRequirement projectToU = createNewTermsOfUse(userTwoId, project.getId(),
				RestrictableObjectType.ENTITY);
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
		project = createNewNode("aProject", userId, null, EntityType.project);
		folder = createNewNode("aFolder", userId, project.getId(), EntityType.folder);
		file = createNewNode("aFile", userId, folder.getId(), EntityType.file);
		folderTwo = createNewNode("folderTwo", userTwoId, project.getId(), EntityType.folder);
		fileTwo = createNewNode("fileTwo", userTwoId, folderTwo.getId(), EntityType.file);
	}

	/**
	 * Helper to create a new node.
	 * 
	 * @param name
	 * @param userId
	 * @param parentId
	 * @param type
	 * @return
	 */
	public Node createNewNode(String name, Long userId, String parentId, EntityType type) {
		Node node = new Node();
		node.setName(name);
		node.setCreatedByPrincipalId(userId);
		node.setModifiedByPrincipalId(userId);
		node.setCreatedOn(new Date());
		node.setModifiedOn(node.getCreatedOn());
		node.setNodeType(type);
		node.setParentId(parentId);
		return nodeDao.createNewNode(node);
	}

	/**
	 * Helper to create a TermsOfUseAccessRequirement.
	 * 
	 * @param userId
	 * @param subjectId
	 * @param subjectType
	 * @param text
	 * @return
	 * @throws DatastoreException
	 */
	public TermsOfUseAccessRequirement createNewTermsOfUse(Long userId, String subjectId,
			RestrictableObjectType subjectType) throws DatastoreException {
		TermsOfUseAccessRequirement ar = new TermsOfUseAccessRequirement();
		ar.setCreatedBy(userId.toString());
		ar.setCreatedOn(new Date());
		ar.setModifiedBy(userId.toString());
		ar.setModifiedOn(new Date());
		ar.setEtag("10");
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		ar.setVersionNumber(1L);
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(subjectId);
		rod.setType(subjectType);
		ar.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[] { rod }));
		ar.setTermsOfUse("Must agree");
		return accessRequirementDAO.create(ar);
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
