package org.sagebionetworks.repo.model.ar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
	public void testGetEntityStatusWithHierarchy() {
		setupNodeHierarchy(userTwoId);
		Node folderTwo = createNewNode("folderTwo", userTwoId, project.getId(), EntityType.folder);
		Node fileTwo = createNewNode("fileTwo", userTwoId, folderTwo.getId(), EntityType.file);
		
		TermsOfUseAccessRequirement projectToU = createNewTermsOfUse(userTwoId, project.getId(), RestrictableObjectType.ENTITY);
		TermsOfUseAccessRequirement folderToU = createNewTermsOfUse(userTwoId, folder.getId(), RestrictableObjectType.ENTITY);
		TermsOfUseAccessRequirement fileToU = createNewTermsOfUse(userTwoId, file.getId(), RestrictableObjectType.ENTITY);
		TermsOfUseAccessRequirement folderTwoToU = createNewTermsOfUse(userTwoId, folderTwo.getId(), RestrictableObjectType.ENTITY);
		TermsOfUseAccessRequirement fileTwoToU = createNewTermsOfUse(userTwoId, fileTwo.getId(), RestrictableObjectType.ENTITY);
		
		// call under test
		List<SubjectStatus> results = accessRestrictionStatusDao
				.getEntityStatus(KeyFactory.stringToKey(Arrays.asList(file.getId(), fileTwo.getId())), userTwoId);
		assertNotNull(results);
		assertEquals(2, results.size());
	}

	@Test
	public void testGetEntityStatusWithRestrictionOnFileUserCreated() {
		setupNodeHierarchy(userTwoId);
		TermsOfUseAccessRequirement tou = createNewTermsOfUse(userTwoId, folder.getId(), RestrictableObjectType.ENTITY);
		// call under test
		List<SubjectStatus> results = accessRestrictionStatusDao
				.getEntityStatus(Arrays.asList(KeyFactory.stringToKey(folder.getId())), userTwoId);
		assertNotNull(results);
		assertEquals(1, results.size());
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
