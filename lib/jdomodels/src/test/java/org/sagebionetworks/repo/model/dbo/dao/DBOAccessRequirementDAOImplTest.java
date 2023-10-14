package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AccessRequirementDAO;
import org.sagebionetworks.repo.model.AccessRequirementStats;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.LockAccessRequirement;
import org.sagebionetworks.repo.model.ManagedACTAccessRequirement;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.SelfSignAccessRequirement;
import org.sagebionetworks.repo.model.TermsOfUseAccessRequirement;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementSearchSort;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementSortField;
import org.sagebionetworks.repo.model.dataaccess.Request;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dataaccess.SortDirection;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.RequestTestUtils;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.ResearchProjectDAO;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.ResearchProjectTestUtils;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.IllegalTransactionStateException;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOAccessRequirementDAOImplTest {

	@Autowired
	private UserGroupDAO userGroupDAO;

	@Autowired
	private AccessRequirementDAO accessRequirementDAO;

	@Autowired
	private NodeDAO nodeDao;

	@Autowired
	private RequestDAO requestDao;

	@Autowired
	private ResearchProjectDAO researchProjectDao;
	
	@Autowired
	private DaoObjectHelper<Node> nodeDaoHelper;
	
	@Autowired
	private AccessControlListDAO aclDao;
	
	private UserGroup individualGroup = null;
	private Node node = null;
	private Node node2 = null;
	private TermsOfUseAccessRequirement accessRequirement = null;
	private TermsOfUseAccessRequirement accessRequirement2 = null;

	
	@BeforeEach
	public void setUp() throws Exception {
		aclDao.truncateAll();
		requestDao.truncateAll();
		researchProjectDao.truncateAll();
		accessRequirementDAO.truncateAll();
		nodeDao.truncateAll();

		individualGroup = new UserGroup();
		individualGroup.setIsIndividual(true);
		individualGroup.setCreationDate(new Date());
		individualGroup.setId(userGroupDAO.create(individualGroup).toString());
		
		// note: we set up multiple nodes and multiple evaluations to ensure that filtering works
		if (node==null) {
			node = NodeTestUtils.createNew("foo", Long.parseLong(individualGroup.getId()));
			node.setId( nodeDao.createNew(node) );
		};
		if (node2==null) {
			node2 = NodeTestUtils.createNew("bar", Long.parseLong(individualGroup.getId()));
			node2.setId( nodeDao.createNew(node2) );
		};
	}
	
	@AfterEach
	public void tearDown() throws Exception{
		aclDao.truncateAll();
		requestDao.truncateAll();
		researchProjectDao.truncateAll();
		accessRequirementDAO.truncateAll();
		nodeDao.truncateAll();
		if (individualGroup != null) {
			userGroupDAO.delete(individualGroup.getId());
		}
	}
	
	public static TermsOfUseAccessRequirement newEntityAccessRequirement(UserGroup principal, Node node, String text) throws DatastoreException {
		TermsOfUseAccessRequirement accessRequirement = new TermsOfUseAccessRequirement();
		accessRequirement.setCreatedBy(principal.getId());
		accessRequirement.setCreatedOn(new Date());
		accessRequirement.setModifiedBy(principal.getId());
		accessRequirement.setModifiedOn(new Date());
		accessRequirement.setEtag("10");
		accessRequirement.setAccessType(ACCESS_TYPE.DOWNLOAD);
		accessRequirement.setVersionNumber(1L);
		RestrictableObjectDescriptor rod = AccessRequirementUtilsTest.createRestrictableObjectDescriptor(node.getId());
		accessRequirement.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod, rod})); // test that repeated IDs doesn't break anything
		accessRequirement.setTermsOfUse(text);
		return accessRequirement;
	}

	// for PLFM-1730 test that if we create a number of access requirements, they come back in the creation order
	@Test
	public void testRetrievalOrder() throws Exception{
		List<AccessRequirement> ars = new ArrayList<>();
		for (int i=0; i<10; i++) {
			TermsOfUseAccessRequirement accessRequirement = newEntityAccessRequirement(individualGroup, node, "foo_"+i);
			ars.add(accessRequirementDAO.create(accessRequirement));
		}
		List<AccessRequirement> ars2 = accessRequirementDAO.getAccessRequirementsForSubject(Collections.singletonList(KeyFactory.stringToKey(node.getId())), RestrictableObjectType.ENTITY, 10L, 0L);
		assertEquals(ars, ars2);
	}
	
	/**
	 * PLFM-4415 When subjectIds is null return an empty list.
	 * 
	 */
	@Test
	public void testPLFM_4415(){
		// Create a new object
		accessRequirement = newEntityAccessRequirement(individualGroup, node, "foo");
		accessRequirement.setSubjectIds(null);
		// Create it
		accessRequirement = accessRequirementDAO.create(accessRequirement);
		assertNotNull(accessRequirement.getSubjectIds());
		assertTrue(accessRequirement.getSubjectIds().isEmpty());
	}

	@Test
	public void testEntityAccessRequirementCRUD() throws Exception{
		// Create a new object
		accessRequirement = newEntityAccessRequirement(individualGroup, node, "foo");
		RestrictableObjectDescriptor subjectId = 
			AccessRequirementUtilsTest.createRestrictableObjectDescriptor(node.getId(), RestrictableObjectType.ENTITY);

		// Create it
		accessRequirement = accessRequirementDAO.create(accessRequirement);
		assertNotNull(accessRequirement.getId());
		assertEquals(accessRequirement.getSubjectIds(), accessRequirementDAO.getSubjects(accessRequirement.getId(), 10L, 0L));

		// Fetch it
		// PLFM-1477, we have to check that retrieval works when there is another access requirement
		accessRequirement2 = newEntityAccessRequirement(individualGroup, node2, "bar");
		accessRequirement2 = accessRequirementDAO.create(accessRequirement2);		
		AccessRequirement clone = accessRequirementDAO.get(accessRequirement.getId().toString());
		assertNotNull(clone);
		assertEquals(accessRequirement, clone);

		// Get by Node Id
		Collection<AccessRequirement> ars = accessRequirementDAO.getAccessRequirementsForSubject(Collections.singletonList(KeyFactory.stringToKey(subjectId.getId())), subjectId.getType(), 10L, 0L);
		assertEquals(1, ars.size());
		assertEquals(accessRequirement, ars.iterator().next());
		
		// update it
		clone = ars.iterator().next();
		clone.setAccessType(ACCESS_TYPE.DOWNLOAD);
		clone.setVersionNumber(accessRequirement.getVersionNumber()+1);
		AccessRequirement updatedAR = accessRequirementDAO.update(clone);
		assertEquals(clone.getAccessType(), updatedAR.getAccessType());

		assertTrue(!clone.getEtag().equals(updatedAR.getEtag()), "etags should be different after an update");

		assertEquals(accessRequirement.getClass().getName(),
				accessRequirementDAO.getConcreteType(accessRequirement.getId().toString()));

		// Delete the access requirements
		accessRequirementDAO.delete(accessRequirement.getId().toString());
		accessRequirementDAO.delete(accessRequirement2.getId().toString());
	}
		
	@Test
	public void testMultipleNodes() throws Exception {
		// Create a new object
		accessRequirement = newEntityAccessRequirement(individualGroup, node, "foo");
		
		// Create it
		accessRequirement = accessRequirementDAO.create(accessRequirement);
		assertNotNull(accessRequirement.getId());
		
		accessRequirement2 = newEntityAccessRequirement(individualGroup, node2, "bar");
		accessRequirement2 = accessRequirementDAO.create(accessRequirement2);		
		AccessRequirement clone = accessRequirementDAO.get(accessRequirement.getId().toString());
		assertNotNull(clone);
		assertEquals(accessRequirement, clone);
		
		// Delete the access requirements
		accessRequirementDAO.delete(accessRequirement.getId().toString());
		accessRequirementDAO.delete(accessRequirement2.getId().toString());
	}
	
	@Test
	public void testGetDoesNotExist(){
		assertThrows(NotFoundException.class, () -> {			
			accessRequirementDAO.get("-1");
		});
	}

	@Test
	public void testGetConcreteTypeNotFound() {
		assertThrows(NotFoundException.class, () -> {
			accessRequirementDAO.getConcreteType("-11");
		});
	}

	@Test
	public void testGetAccessRequirementStatsWithNullSubjectIds() {
		assertThrows(IllegalArgumentException.class, () -> {
			accessRequirementDAO.getAccessRequirementStats(null, RestrictableObjectType.ENTITY);
		});
	}

	@Test
	public void testGetAccessRequirementStatsWithEmptySubjectIds() {
		assertThrows(IllegalArgumentException.class, () -> {
			accessRequirementDAO.getAccessRequirementStats(new LinkedList<Long>(), RestrictableObjectType.ENTITY);
		});
	}

	@Test
	public void testGetAccessRequirementStatsWithNullRestrictableObjectType() {
		assertThrows(IllegalArgumentException.class, () -> {
			accessRequirementDAO.getAccessRequirementStats(Arrays.asList(KeyFactory.stringToKey(node.getId())), null);
		});
	}

	@Test
	public void testGetAccessRequirementStats() {
		AccessRequirementStats stats = accessRequirementDAO.getAccessRequirementStats(Arrays.asList(KeyFactory.stringToKey(node.getId())), RestrictableObjectType.ENTITY);
		assertNotNull(stats);
		assertFalse(stats.getHasToU());
		assertFalse(stats.getHasACT());
		assertTrue(stats.getRequirementIdSet().isEmpty());

		/*
		 * PLFM-4501
		 */
		RestrictableObjectDescriptor rod = AccessRequirementUtilsTest.createRestrictableObjectDescriptor(node.getId());
		SelfSignAccessRequirement selfsignAR = new SelfSignAccessRequirement();
		selfsignAR.setCreatedBy(individualGroup.getId());
		selfsignAR.setCreatedOn(new Date());
		selfsignAR.setModifiedBy(individualGroup.getId());
		selfsignAR.setModifiedOn(new Date());
		selfsignAR.setEtag("etag");
		selfsignAR.setAccessType(ACCESS_TYPE.DOWNLOAD);
		selfsignAR.setSubjectIds(Arrays.asList(rod));
		selfsignAR.setVersionNumber(1L);
		selfsignAR = accessRequirementDAO.create(selfsignAR);

		stats = accessRequirementDAO.getAccessRequirementStats(Arrays.asList(KeyFactory.stringToKey(node.getId())), RestrictableObjectType.ENTITY);
		assertNotNull(stats);
		assertTrue(stats.getHasToU());
		assertFalse(stats.getHasACT());
		assertFalse(stats.getHasLock());
		assertTrue(stats.getRequirementIdSet().contains(selfsignAR.getId().toString()));

		accessRequirementDAO.delete(selfsignAR.getId().toString());

		accessRequirement = new TermsOfUseAccessRequirement();
		accessRequirement.setCreatedBy(individualGroup.getId());
		accessRequirement.setCreatedOn(new Date());
		accessRequirement.setModifiedBy(individualGroup.getId());
		accessRequirement.setModifiedOn(new Date());
		accessRequirement.setEtag("etag");
		accessRequirement.setAccessType(ACCESS_TYPE.DOWNLOAD);
		accessRequirement.setSubjectIds(Arrays.asList(rod));
		accessRequirement.setVersionNumber(1L);
		accessRequirement = accessRequirementDAO.create(accessRequirement);

		stats = accessRequirementDAO.getAccessRequirementStats(Arrays.asList(KeyFactory.stringToKey(node.getId())), RestrictableObjectType.ENTITY);
		assertNotNull(stats);
		assertTrue(stats.getHasToU());
		assertFalse(stats.getHasACT());
		assertFalse(stats.getHasLock());
		assertTrue(stats.getRequirementIdSet().contains(accessRequirement.getId().toString()));

		ManagedACTAccessRequirement accessRequirement2 = new ManagedACTAccessRequirement();
		accessRequirement2.setCreatedBy(individualGroup.getId());
		accessRequirement2.setCreatedOn(new Date());
		accessRequirement2.setModifiedBy(individualGroup.getId());
		accessRequirement2.setModifiedOn(new Date());
		accessRequirement2.setEtag("etag");
		accessRequirement2.setAccessType(ACCESS_TYPE.DOWNLOAD);
		accessRequirement2.setSubjectIds(Arrays.asList(rod));
		accessRequirement2.setIsTwoFaRequired(false);
		accessRequirement2 = accessRequirementDAO.create(accessRequirement2);

		stats = accessRequirementDAO.getAccessRequirementStats(Arrays.asList(KeyFactory.stringToKey(node.getId())), RestrictableObjectType.ENTITY);
		assertNotNull(stats);
		assertTrue(stats.getHasToU());
		assertTrue(stats.getHasACT());
		assertTrue(stats.getRequirementIdSet().contains(accessRequirement.getId().toString()));
		assertTrue(stats.getRequirementIdSet().contains(accessRequirement2.getId().toString()));

		accessRequirementDAO.delete(accessRequirement2.getId().toString());
	}

	/*
	 * PLFM-3300
	 */
	@Test
	public void testAROnEntityAndItsParent() {
		node2.setParentId(node.getId());
		nodeDao.updateNode(node2);

		TermsOfUseAccessRequirement accessRequirement = new TermsOfUseAccessRequirement();
		accessRequirement.setCreatedBy(individualGroup.getId());
		accessRequirement.setCreatedOn(new Date());
		accessRequirement.setModifiedBy(individualGroup.getId());
		accessRequirement.setModifiedOn(new Date());
		accessRequirement.setAccessType(ACCESS_TYPE.DOWNLOAD);
		accessRequirement.setVersionNumber(1L);
		RestrictableObjectDescriptor rod1 = AccessRequirementUtilsTest.createRestrictableObjectDescriptor(node.getId());
		RestrictableObjectDescriptor rod2 = AccessRequirementUtilsTest.createRestrictableObjectDescriptor(node2.getId());
		accessRequirement.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod1, rod2}));
		accessRequirement.setTermsOfUse("text");

		accessRequirement = accessRequirementDAO.create(accessRequirement);
		List<AccessRequirement> list = accessRequirementDAO.getAccessRequirementsForSubject(Arrays.asList(KeyFactory.stringToKey(node.getId()), KeyFactory.stringToKey(node2.getId())), RestrictableObjectType.ENTITY, 10L, 0L);
		assertNotNull(list);
		assertEquals(1, list.size());
		assertEquals(accessRequirement, list.get(0));

		accessRequirementDAO.delete(accessRequirement.getId().toString());
	}

	@Test
	public void testGetForUpdateWithoutTransaction() {
		accessRequirement = newEntityAccessRequirement(individualGroup, node, "foo");
		accessRequirement = accessRequirementDAO.create(accessRequirement);
		assertThrows(IllegalTransactionStateException.class, () -> {
			accessRequirementDAO.getForUpdate(accessRequirement.getId().toString());
		});
	}

	@Test
	public void testGetAccessRequirementForUpdateWithoutTransaction() {
		accessRequirement = newEntityAccessRequirement(individualGroup, node, "foo");
		accessRequirement = accessRequirementDAO.create(accessRequirement);
		assertThrows(IllegalTransactionStateException.class, () -> {
			accessRequirementDAO.getAccessRequirementForUpdate(accessRequirement.getId().toString());
		});
	}

	@Test
	public void testGetAccessRequirementDiffForNullSource() {
		List<Long> destSubjects = Arrays.asList(KeyFactory.stringToKey(node.getId()));
		assertThrows(IllegalArgumentException.class, () -> {
			accessRequirementDAO.getAccessRequirementDiff(null, destSubjects , RestrictableObjectType.ENTITY);
		});
	}

	@Test
	public void testGetAccessRequirementDiffForNullDest() {
		List<Long> sourceSubjects = Arrays.asList(KeyFactory.stringToKey(node.getId()));
		assertThrows(IllegalArgumentException.class, () -> {
			accessRequirementDAO.getAccessRequirementDiff(sourceSubjects, null , RestrictableObjectType.ENTITY);
		});
	}

	@Test
	public void testGetAccessRequirementDiffForEmptySource() {
		List<Long> sourceSubjects = new LinkedList<Long>();
		List<Long> destSubjects = Arrays.asList(KeyFactory.stringToKey(node.getId()));
		assertThrows(IllegalArgumentException.class, () -> {
			accessRequirementDAO.getAccessRequirementDiff(sourceSubjects, destSubjects , RestrictableObjectType.ENTITY);
		});
	}

	@Test
	public void testGetAccessRequirementDiffForEmptyDest() {
		List<Long> sourceSubjects = Arrays.asList(KeyFactory.stringToKey(node.getId()));
		List<Long> destSubjects = new LinkedList<Long>();
		assertThrows(IllegalArgumentException.class, () -> {
			accessRequirementDAO.getAccessRequirementDiff(sourceSubjects, destSubjects , RestrictableObjectType.ENTITY);
		});
	}

	@Test
	public void testGetAccessRequirementDiffWithDestinationMoreRestricted() {
		accessRequirement = newEntityAccessRequirement(individualGroup, node, "foo");
		accessRequirement = accessRequirementDAO.create(accessRequirement);
		List<Long> sourceSubjects = Arrays.asList(KeyFactory.stringToKey(node2.getId()));
		List<Long> destSubjects = Arrays.asList(KeyFactory.stringToKey(node.getId()));
		assertEquals(new LinkedList<String>(),
				accessRequirementDAO.getAccessRequirementDiff(sourceSubjects, destSubjects , RestrictableObjectType.ENTITY));
	}

	@Test
	public void testGetAccessRequirementDiffWithMatchingAR() {
		accessRequirement = newEntityAccessRequirement(individualGroup, node, "foo");
		accessRequirement = accessRequirementDAO.create(accessRequirement);
		List<Long> sourceSubjects = Arrays.asList(KeyFactory.stringToKey(node.getId()));
		List<Long> destSubjects = Arrays.asList(KeyFactory.stringToKey(node.getId()));
		assertEquals(new LinkedList<String>(),
				accessRequirementDAO.getAccessRequirementDiff(sourceSubjects, destSubjects , RestrictableObjectType.ENTITY));
	}

	@Test
	public void testGetAccessRequirementDiffWithSourceMoreRestricted() {
		accessRequirement = newEntityAccessRequirement(individualGroup, node, "foo");
		accessRequirement = accessRequirementDAO.create(accessRequirement);
		List<Long> sourceSubjects = Arrays.asList(KeyFactory.stringToKey(node.getId()));
		List<Long> destSubjects = Arrays.asList(KeyFactory.stringToKey(node2.getId()));
		assertEquals(Arrays.asList(accessRequirement.getId().toString()),
				accessRequirementDAO.getAccessRequirementDiff(sourceSubjects, destSubjects , RestrictableObjectType.ENTITY));
	}

	@Test
	public void testDeleteAccessRequirement() {
		accessRequirement = newEntityAccessRequirement(individualGroup, node, "foo");
		accessRequirement = accessRequirementDAO.create(accessRequirement);
		String accessRequirementId = String.valueOf(accessRequirement.getId());
		String expectedMessage = "An access requirement with id "+ accessRequirementId + " cannot be found.";
		// Call under test
		accessRequirementDAO.delete(accessRequirementId);
		NotFoundException exception = assertThrows(NotFoundException.class, () -> {
			accessRequirementDAO.get(accessRequirementId);
		});
		assertEquals(expectedMessage, exception.getMessage());
	}

	@Test
	public void testDeleteAccessRequirementForeignKeyConstraint() throws Exception {
		accessRequirement = newEntityAccessRequirement(individualGroup, node, "foo");
		accessRequirement = accessRequirementDAO.create(accessRequirement);
		String accessRequirementId = String.valueOf(accessRequirement.getId());
		ResearchProject researchProject = ResearchProjectTestUtils.createNewDto();
		researchProject.setAccessRequirementId(accessRequirementId);
		researchProject = researchProjectDao.create(researchProject);
		Request dto = RequestTestUtils.createNewRequest();
		dto.setAccessRequirementId(accessRequirementId);
		dto.setResearchProjectId(researchProject.getId());
		dto = requestDao.create(dto);
		String expectedMessage = "The access requirement with id " + accessRequirementId +
				" cannot be deleted as it is referenced by another object.";
		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			accessRequirementDAO.delete(accessRequirementId);
		});
		assertEquals(expectedMessage, exception.getMessage());
	}
	
	@Test
	public void testTranslateExceptionWithUnknownMessage() {
		IllegalArgumentException e = new IllegalArgumentException("Some random exception");
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			DBOAccessRequirementDAOImpl.translateException(e, "some name");
		}).getMessage();
		assertEquals("Some random exception", message);
	}
	
	@Test
	public void testTranslateExceptionWithDuplicateKey() {
		IllegalArgumentException e = new IllegalArgumentException("Tagged with 'AR_NAME'");
		String message = assertThrows(NameConflictException.class, ()->{
			// call under test
			DBOAccessRequirementDAOImpl.translateException(e, "some name");
		}).getMessage();
		assertEquals("An AccessRequirement with the name: 'some name' already exists", message);
	}
	
	// create name
	
	@Test
	public void testCreateAccessRequirmentWithNullDescriptionAndNullName() {
		TermsOfUseAccessRequirement ar = newEntityAccessRequirement(individualGroup, node, "foo");
		ar.setDescription(null);
		ar.setName(null);
		// call under test
		ar = accessRequirementDAO.create(ar);
		assertEquals(ar.getId().toString(), ar.getName());
	}
	
	@Test
	public void testCreateAccessRequirmentWithNullDescriptionAndNonNullName() {
		TermsOfUseAccessRequirement ar = newEntityAccessRequirement(individualGroup, node, "foo");
		ar.setDescription(null);
		ar.setName("some name");
		// call under test
		ar = accessRequirementDAO.create(ar);
		assertEquals("some name", ar.getName());
	}
	
	@Test
	public void testCreateAccessRequirmentWithNonNullDescriptionAndNullName() {
		TermsOfUseAccessRequirement ar = newEntityAccessRequirement(individualGroup, node, "foo");
		ar.setDescription("some description");
		ar.setName(null);
		// call under test
		ar = accessRequirementDAO.create(ar);
		assertEquals("some description", ar.getName());
	}
	
	@Test
	public void testCreateAccessRequirmentWithEmptyDescriptionAndNullName() {
		TermsOfUseAccessRequirement ar = newEntityAccessRequirement(individualGroup, node, "foo");
		ar.setDescription("");
		ar.setName(null);
		// call under test
		ar = accessRequirementDAO.create(ar);
		assertEquals(ar.getId().toString(), ar.getName());
	}
	
	@Test
	public void testCreateAccessRequirmentWithNonNullDescriptionAndNonNullName() {
		TermsOfUseAccessRequirement ar = newEntityAccessRequirement(individualGroup, node, "foo");
		ar.setDescription("some description");
		ar.setName("some name");
		// call under test
		ar = accessRequirementDAO.create(ar);
		assertEquals("some description", ar.getName());
	}
	
	@Test
	public void testCreateAccessRequirmentWithDuplicateName() {
		TermsOfUseAccessRequirement ar = newEntityAccessRequirement(individualGroup, node, "foo");
		ar.setName("Not Unique");
		ar = accessRequirementDAO.create(ar);
		assertEquals("Not Unique", ar.getName());
		
		TermsOfUseAccessRequirement ardup = newEntityAccessRequirement(individualGroup, node, "foo");
		ardup.setName("not unique");
		String message = assertThrows(NameConflictException.class, ()->{
			accessRequirementDAO.create(ardup);
		}).getMessage();
		assertEquals("An AccessRequirement with the name: 'not unique' already exists", message);
	}
	
	// update name
	
	@Test
	public void testUpdateAccessRequirmentWithNullDescriptionAndNullName() {
		TermsOfUseAccessRequirement ar = newEntityAccessRequirement(individualGroup, node, "foo");
		ar.setName("change me");
		ar = accessRequirementDAO.create(ar);
		ar.setDescription(null);
		ar.setName(null);
		ar.setVersionNumber(ar.getVersionNumber()+1);
		// call under test
		ar = accessRequirementDAO.update(ar);
		assertEquals(ar.getId().toString(), ar.getName());
	}
	
	@Test
	public void testUpdateAccessRequirmentWithEmptyDescriptionAndNullName() {
		TermsOfUseAccessRequirement ar = newEntityAccessRequirement(individualGroup, node, "foo");
		ar.setName("change me");
		ar = accessRequirementDAO.create(ar);
		ar.setDescription("");
		ar.setName(null);
		ar.setVersionNumber(ar.getVersionNumber()+1);
		// call under test
		ar = accessRequirementDAO.update(ar);
		assertEquals(ar.getId().toString(), ar.getName());
	}
	
	@Test
	public void testUpdateAccessRequirmentWithNullDescriptionAndNonNullName() {
		TermsOfUseAccessRequirement ar = newEntityAccessRequirement(individualGroup, node, "foo");
		ar.setName("change me");
		ar = accessRequirementDAO.create(ar);
		ar.setDescription(null);
		ar.setName("some name");
		ar.setVersionNumber(ar.getVersionNumber()+1);
		// call under test
		ar = accessRequirementDAO.update(ar);
		assertEquals("some name", ar.getName());
	}
	
	@Test
	public void testUpdateAccessRequirmentWithNonNullDescriptionAndNullName() {
		TermsOfUseAccessRequirement ar = newEntityAccessRequirement(individualGroup, node, "foo");
		ar.setName("change me");
		ar = accessRequirementDAO.create(ar);
		ar.setDescription("some description");
		ar.setName(null);
		ar.setVersionNumber(ar.getVersionNumber()+1);
		// call under test
		ar = accessRequirementDAO.update(ar);
		assertEquals("some description", ar.getName());
	}
	
	@Test
	public void testUpdateAccessRequirmentWithNonNullDescriptionAndNonNullName() {
		TermsOfUseAccessRequirement ar = newEntityAccessRequirement(individualGroup, node, "foo");
		ar.setName("change me");
		ar = accessRequirementDAO.create(ar);
		ar.setDescription("some description");
		ar.setName("some name");
		ar.setVersionNumber(ar.getVersionNumber()+1);
		// call under test
		ar = accessRequirementDAO.update(ar);
		assertEquals("some description", ar.getName());
	}
	
	@Test
	public void testUpdateAccessRequirmentWithDuplicateName() {
		TermsOfUseAccessRequirement ar = newEntityAccessRequirement(individualGroup, node, "foo");
		ar.setName("Not Unique");
		ar = accessRequirementDAO.create(ar);
		assertEquals("Not Unique", ar.getName());
		
		ar = newEntityAccessRequirement(individualGroup, node, "foo");
		ar.setName("change me");
		TermsOfUseAccessRequirement arDup = accessRequirementDAO.create(ar);
		arDup.setName("not unique");
		arDup.setVersionNumber(arDup.getVersionNumber()+1);
		String message = assertThrows(NameConflictException.class, ()->{
			// call under test
			accessRequirementDAO.update(arDup);
		}).getMessage();
		assertEquals("An AccessRequirement with the name: 'not unique' already exists", message);
	}
	
	@Test
	public void testGetAccessRequirementNames() {
		AccessRequirement ar1 = accessRequirementDAO.create(newEntityAccessRequirement(individualGroup, node, "foo"));
		AccessRequirement ar2 = accessRequirementDAO.create(newEntityAccessRequirement(individualGroup, node, "foo").setName("name"));
		AccessRequirement ar3 = accessRequirementDAO.create(newEntityAccessRequirement(individualGroup, node, "foo").setName("name2"));
		
		Map<Long, String> expected = Map.of(
			ar1.getId(), ar1.getName(),
			ar2.getId(), ar2.getName(),
			ar3.getId(), ar3.getName()
		);
		
		Map<Long, String> result = accessRequirementDAO.getAccessRequirementNames(Set.of(ar1.getId(), ar2.getId(), ar3.getId(), 0L));
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetAccessRequirementNamesWithEmptyList() {
		
		Map<Long, String> expected = Collections.emptyMap();
		
		Map<Long, String> result = accessRequirementDAO.getAccessRequirementNames(Collections.emptySet());
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetAccessRequirementNamesWithNullList() {
		
		Map<Long, String> expected = Collections.emptyMap();
		
		Map<Long, String> result = accessRequirementDAO.getAccessRequirementNames(null);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testMapAccessRequirmentsToProject() {
		
		Node projectOne = nodeDaoHelper.create((n)->{
			n.setNodeType(EntityType.project);
		});
		Long projectOneId = KeyFactory.stringToKey(projectOne.getId());
		
		Node projectTwo = nodeDaoHelper.create((n)->{
			n.setNodeType(EntityType.project);
		});
		Long projectTwoId = KeyFactory.stringToKey(projectTwo.getId());
		
		TermsOfUseAccessRequirement arOne = accessRequirementDAO.create(newEntityAccessRequirement(individualGroup, node, "foo"));
		TermsOfUseAccessRequirement arTwo = accessRequirementDAO.create(newEntityAccessRequirement(individualGroup, node, "foo"));
		TermsOfUseAccessRequirement arThree = accessRequirementDAO.create(newEntityAccessRequirement(individualGroup, node, "foo"));

		
		// call under test
		accessRequirementDAO.mapAccessRequirmentsToProject(new Long[] {arOne.getId(), arTwo.getId()}, projectOneId);
		accessRequirementDAO.mapAccessRequirmentsToProject(new Long[] {arTwo.getId(), arThree.getId()}, projectTwoId);
		
		assertEquals(Arrays.asList(projectOneId), accessRequirementDAO.getAccessRequirementProjectsMap(Set.of(arOne.getId())).get(arOne.getId()));
		assertEquals(Arrays.asList(projectOneId, projectTwoId), accessRequirementDAO.getAccessRequirementProjectsMap(Set.of(arTwo.getId())).get(arTwo.getId()));
		assertEquals(Arrays.asList(projectTwoId), accessRequirementDAO.getAccessRequirementProjectsMap(Set.of(arThree.getId())).get(arThree.getId()));
	}
	
	@Test
	public void testMapAccessRequirmentsToProjectWithDuplicate() {
		
		Node projectOne = nodeDaoHelper.create((n)->{
			n.setNodeType(EntityType.project);
		});
		Long projectOneId = KeyFactory.stringToKey(projectOne.getId());
		
		TermsOfUseAccessRequirement arOne = accessRequirementDAO.create(newEntityAccessRequirement(individualGroup, node, "foo"));

		// call under test
		accessRequirementDAO.mapAccessRequirmentsToProject(new Long[] {arOne.getId()}, projectOneId);
		accessRequirementDAO.mapAccessRequirmentsToProject(new Long[] {arOne.getId()}, projectOneId);
		
		assertEquals(Arrays.asList(projectOneId), accessRequirementDAO.getAccessRequirementProjectsMap(Set.of(arOne.getId())).get(arOne.getId()));
	}
	
	@Test
	public void testGetAccessRequirementProjectsMap() {
		Node projectOne = nodeDaoHelper.create((n)->{
			n.setNodeType(EntityType.project);
		});
		
		Long projectOneId = KeyFactory.stringToKey(projectOne.getId());
		
		Node projectTwo = nodeDaoHelper.create((n)->{
			n.setNodeType(EntityType.project);
		});
		
		Long projectTwoId = KeyFactory.stringToKey(projectTwo.getId());
		
		TermsOfUseAccessRequirement arOne = accessRequirementDAO.create(newEntityAccessRequirement(individualGroup, node, "foo"));
		TermsOfUseAccessRequirement arTwo = accessRequirementDAO.create(newEntityAccessRequirement(individualGroup, node, "foo"));
		
		accessRequirementDAO.mapAccessRequirmentsToProject(new Long[] {arOne.getId(), arTwo.getId()}, projectOneId);
		accessRequirementDAO.mapAccessRequirmentsToProject(new Long[] {arTwo.getId()}, projectTwoId);
		
		Map<Long, List<Long>> expected = Map.of(
			arOne.getId(), List.of(projectOneId),
			arTwo.getId(), List.of(projectOneId, projectTwoId)
		);
		
		Map<Long, List<Long>> result = accessRequirementDAO.getAccessRequirementProjectsMap(Set.of(arOne.getId(), arTwo.getId(), -1L));
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetAccessRequirementProjectsMapWithNullSet() {
		Map<Long, List<Long>> expected = Collections.emptyMap();
		
		Map<Long, List<Long>> result = accessRequirementDAO.getAccessRequirementProjectsMap(null);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetAccessRequirementProjectsMapWithEmptySet() {
		Map<Long, List<Long>> expected = Collections.emptyMap();
		
		Map<Long, List<Long>> result = accessRequirementDAO.getAccessRequirementProjectsMap(Collections.emptySet());
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testSearchAccessRequirements() {
		TermsOfUseAccessRequirement arOne = accessRequirementDAO.create(newEntityAccessRequirement(individualGroup, node, "foo1"));
		TermsOfUseAccessRequirement arTwo = accessRequirementDAO.create(newEntityAccessRequirement(individualGroup, node, "foo2"));
		
		List<AccessRequirementSearchSort> sort = List.of(new AccessRequirementSearchSort().setField(AccessRequirementSortField.NAME));
		
		String nameSubs = null;
		String reviewerId = null;
		Long projectId = null;
		ACCESS_TYPE accessType = null;
		
		long limit = 10;
		long offset = 0;
		
		List<AccessRequirement> expected = List.of(arOne, arTwo);
		
		List<AccessRequirement> result = accessRequirementDAO.searchAccessRequirements(sort, nameSubs, reviewerId, projectId, accessType, limit, offset);
		result = result.stream().filter(a -> !AccessRequirementDAO.INVALID_ANNOTATIONS_LOCK_ID.equals(a.getId()))
				.collect(Collectors.toList());
		assertEquals(expected, result);
	}
	
	@Test
	public void testSearchAccessRequirementsWithMultiSort() {
		TermsOfUseAccessRequirement arOne = accessRequirementDAO.create(newEntityAccessRequirement(individualGroup, node, "foo1"));
		TermsOfUseAccessRequirement arTwo = accessRequirementDAO.create(newEntityAccessRequirement(individualGroup, node, "foo2").setCreatedOn(arOne.getCreatedOn()));
		
		List<AccessRequirementSearchSort> sort = List.of(
			new AccessRequirementSearchSort().setField(AccessRequirementSortField.CREATED_ON),
			new AccessRequirementSearchSort().setField(AccessRequirementSortField.NAME).setDirection(SortDirection.DESC)
		);
		
		String nameSubs = null;
		String reviewerId = null;
		Long projectId = null;
		ACCESS_TYPE accessType = null;
		
		long limit = 10;
		long offset = 0;
		
		List<AccessRequirement> expected = List.of(arTwo, arOne);
		
		List<AccessRequirement> result = accessRequirementDAO.searchAccessRequirements(sort, nameSubs, reviewerId, projectId, accessType, limit, offset);
		result = result.stream().filter(a -> !AccessRequirementDAO.INVALID_ANNOTATIONS_LOCK_ID.equals(a.getId()))
				.collect(Collectors.toList());
		assertEquals(expected, result);
	}
	
	@Test
	public void testSearchAccessRequirementsWithLimitoffset() {
		TermsOfUseAccessRequirement arOne = accessRequirementDAO.create(newEntityAccessRequirement(individualGroup, node, "foo1"));
		TermsOfUseAccessRequirement arTwo = accessRequirementDAO.create(newEntityAccessRequirement(individualGroup, node, "foo2"));
		
		List<AccessRequirementSearchSort> sort = List.of(new AccessRequirementSearchSort().setField(AccessRequirementSortField.NAME));
		
		String nameSubs = null;
		String reviewerId = null;
		Long projectId = null;
		ACCESS_TYPE accessType = null;
		
		long limit = 1;
		long offset = 0;
		
		List<AccessRequirement> expected = List.of(arOne);
		
		List<AccessRequirement> result = accessRequirementDAO.searchAccessRequirements(sort, nameSubs, reviewerId, projectId, accessType, limit, offset);
		
		assertEquals(expected, result);
		
		limit = 1;
		offset = 1;
		
		expected = List.of(arTwo);
		
		result = accessRequirementDAO.searchAccessRequirements(sort, nameSubs, reviewerId, projectId, accessType, limit, offset);
		result = result.stream().filter(a -> !AccessRequirementDAO.INVALID_ANNOTATIONS_LOCK_ID.equals(a.getId()))
				.collect(Collectors.toList());
		assertEquals(expected, result);
	}
	
	@Test
	public void testSearchAccessRequirementsWithNameContains() {
		TermsOfUseAccessRequirement arOne = accessRequirementDAO.create(newEntityAccessRequirement(individualGroup, node, "foo").setName("name one"));
		TermsOfUseAccessRequirement arTwo = accessRequirementDAO.create(newEntityAccessRequirement(individualGroup, node, "foo").setName("name two"));
		
		List<AccessRequirementSearchSort> sort = List.of(new AccessRequirementSearchSort().setField(AccessRequirementSortField.NAME));
		
		String nameSubs = "one";
		String reviewerId = null;
		Long projectId = null;
		ACCESS_TYPE accessType = null;
		
		long limit = 10;
		long offset = 0;
		
		List<AccessRequirement> expected = List.of(arOne);
		
		List<AccessRequirement> result = accessRequirementDAO.searchAccessRequirements(sort, nameSubs, reviewerId, projectId, accessType, limit, offset);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testSearchAccessRequirementsWithAccessType() {
		TermsOfUseAccessRequirement arOne = accessRequirementDAO.create(newEntityAccessRequirement(individualGroup, node, "foo").setAccessType(ACCESS_TYPE.PARTICIPATE));
		TermsOfUseAccessRequirement arTwo = accessRequirementDAO.create(newEntityAccessRequirement(individualGroup, node, "foo"));
		
		List<AccessRequirementSearchSort> sort = List.of(new AccessRequirementSearchSort().setField(AccessRequirementSortField.NAME));
		
		String nameSubs = null;
		String reviewerId = null;
		Long projectId = null;
		ACCESS_TYPE accessType = ACCESS_TYPE.DOWNLOAD;
		
		long limit = 10;
		long offset = 0;
		
		List<AccessRequirement> expected = List.of(arTwo);
		
		List<AccessRequirement> result = accessRequirementDAO.searchAccessRequirements(sort, nameSubs, reviewerId, projectId, accessType, limit, offset);
		result = result.stream().filter(a -> !AccessRequirementDAO.INVALID_ANNOTATIONS_LOCK_ID.equals(a.getId()))
				.collect(Collectors.toList());
		assertEquals(expected, result);
	}
	
	@Test
	public void testSearchAccessRequirementsWithReviewerId() {
		TermsOfUseAccessRequirement arOne = accessRequirementDAO.create(newEntityAccessRequirement(individualGroup, node, "foo"));
		TermsOfUseAccessRequirement arTwo = accessRequirementDAO.create(newEntityAccessRequirement(individualGroup, node, "foo"));
		
		addReviewers(arTwo.getId(), List.of(individualGroup.getId()));
		
		List<AccessRequirementSearchSort> sort = List.of(new AccessRequirementSearchSort().setField(AccessRequirementSortField.NAME));
		
		String nameSubs = null;
		String reviewerId = individualGroup.getId();
		Long projectId = null;
		ACCESS_TYPE accessType = null;
		
		long limit = 10;
		long offset = 0;
		
		List<AccessRequirement> expected = List.of(arTwo);
		
		List<AccessRequirement> result = accessRequirementDAO.searchAccessRequirements(sort, nameSubs, reviewerId, projectId, accessType, limit, offset);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testSearchAccessRequirementsWithProjectId() {
		Node projectOne = nodeDaoHelper.create((n)->{
			n.setNodeType(EntityType.project);
		});
		
		Long projectOneId = KeyFactory.stringToKey(projectOne.getId());
		
		Node projectTwo = nodeDaoHelper.create((n)->{
			n.setNodeType(EntityType.project);
		});
		
		Long projectTwoId = KeyFactory.stringToKey(projectTwo.getId());
		
		TermsOfUseAccessRequirement arOne = accessRequirementDAO.create(newEntityAccessRequirement(individualGroup, node, "foo"));
		TermsOfUseAccessRequirement arTwo = accessRequirementDAO.create(newEntityAccessRequirement(individualGroup, node, "foo"));
		
		accessRequirementDAO.mapAccessRequirmentsToProject(new Long[] {arOne.getId(), arTwo.getId()}, projectOneId);
		accessRequirementDAO.mapAccessRequirmentsToProject(new Long[] {arTwo.getId()}, projectTwoId);
				
		List<AccessRequirementSearchSort> sort = List.of(new AccessRequirementSearchSort().setField(AccessRequirementSortField.NAME));
		
		String nameSubs = null;
		String reviewerId = null;
		Long projectId = projectOneId;
		ACCESS_TYPE accessType = null;
		
		long limit = 10;
		long offset = 0;
		
		List<AccessRequirement> expected = List.of(arOne, arTwo);
		
		List<AccessRequirement> result = accessRequirementDAO.searchAccessRequirements(sort, nameSubs, reviewerId, projectId, accessType, limit, offset);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testSearchAccessRequirementsWithProjectIdAndReviewerId() {
		Node projectOne = nodeDaoHelper.create((n)->{
			n.setNodeType(EntityType.project);
		});
		
		Long projectOneId = KeyFactory.stringToKey(projectOne.getId());
		
		Node projectTwo = nodeDaoHelper.create((n)->{
			n.setNodeType(EntityType.project);
		});
		
		Long projectTwoId = KeyFactory.stringToKey(projectTwo.getId());
		
		TermsOfUseAccessRequirement arOne = accessRequirementDAO.create(newEntityAccessRequirement(individualGroup, node, "foo"));
		TermsOfUseAccessRequirement arTwo = accessRequirementDAO.create(newEntityAccessRequirement(individualGroup, node, "foo"));
		
		addReviewers(arTwo.getId(), List.of(individualGroup.getId()));
		
		accessRequirementDAO.mapAccessRequirmentsToProject(new Long[] {arOne.getId(), arTwo.getId()}, projectOneId);
		accessRequirementDAO.mapAccessRequirmentsToProject(new Long[] {arTwo.getId()}, projectTwoId);
				
		List<AccessRequirementSearchSort> sort = List.of(new AccessRequirementSearchSort().setField(AccessRequirementSortField.NAME));
		
		String nameSubs = null;
		String reviewerId = individualGroup.getId();
		Long projectId = projectTwoId;
		ACCESS_TYPE accessType = null;
		
		long limit = 10;
		long offset = 0;
		
		List<AccessRequirement> expected = List.of(arTwo);
		
		List<AccessRequirement> result = accessRequirementDAO.searchAccessRequirements(sort, nameSubs, reviewerId, projectId, accessType, limit, offset);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testCreateyAccessRequirementWithSubjectsDefinedByAnnotations() throws Exception{
		accessRequirement = newEntityAccessRequirement(individualGroup, node, "foo");
		accessRequirement.setSubjectIds(null);
		accessRequirement.setSubjectsDefinedByAnnotations(true);

		// call under test
		accessRequirement = accessRequirementDAO.create(accessRequirement);
		
		assertNotNull(accessRequirement.getId());
		assertEquals(accessRequirement.getSubjectIds(), Collections.emptyList());
		assertEquals(Collections.emptyList(), accessRequirementDAO.getSubjects(accessRequirement.getId(), 1000L, 0L));
	}
	
	@Test
	public void testaddDynamicallyBoundAccessRequirmentsToSubjectWithDoesNotExist() {

		RestrictableObjectDescriptor subject = new RestrictableObjectDescriptor().setId("syn123")
				.setType(RestrictableObjectType.ENTITY);

		String message = assertThrows(NotFoundException.class, () -> {
			// call under test
			accessRequirementDAO.addDynamicallyBoundAccessRequirmentsToSubject(subject, List.of(-222L));
		}).getMessage();
		assertEquals(
				"Cannot bind access requirements to: 'syn123' because one or more of the provided"
				+ " access requirement IDs does not exist: '[-222]'",
				message);
	}
	
	@Test
	public void testUpdateAccessRequirementWithSubjectsDefinedByAnnotations() throws Exception{
		accessRequirement = newEntityAccessRequirement(individualGroup, node, "foo");
		accessRequirement.setSubjectIds(null);
		accessRequirement.setSubjectsDefinedByAnnotations(true);

		accessRequirement = accessRequirementDAO.create(accessRequirement);
		
		assertNotNull(accessRequirement.getId());
		assertEquals(accessRequirement.getSubjectIds(), Collections.emptyList());
		assertEquals(Collections.emptyList(), accessRequirementDAO.getSubjects(accessRequirement.getId(), 1000L, 0L));
		
		accessRequirement.setVersionNumber(2L);
		accessRequirement.setName("new name");
		
		// call under test
		accessRequirement = accessRequirementDAO.update(accessRequirement);
		assertEquals(accessRequirement.getSubjectIds(), Collections.emptyList());
		assertEquals(Collections.emptyList(), accessRequirementDAO.getSubjects(accessRequirement.getId(), 1000L, 0L));
	}
	
	@Test
	public void testUpdateAccessRequirementWithSubjectsDefinedByAnnotationsAndBoundSubjects() throws Exception {
		accessRequirement = newEntityAccessRequirement(individualGroup, node, "foo");
		accessRequirement.setSubjectIds(null);
		accessRequirement.setSubjectsDefinedByAnnotations(true);

		accessRequirement = accessRequirementDAO.create(accessRequirement);

		assertNotNull(accessRequirement.getId());
		assertEquals(accessRequirement.getSubjectIds(), Collections.emptyList());
		assertEquals(Collections.emptyList(), accessRequirementDAO.getSubjects(accessRequirement.getId(), 1000L, 0L));

		// one
		RestrictableObjectDescriptor subjectOne = new RestrictableObjectDescriptor().setId("syn123")
				.setType(RestrictableObjectType.ENTITY);

		// bind a subject to this AR
		accessRequirementDAO.addDynamicallyBoundAccessRequirmentsToSubject(subjectOne,
				List.of(accessRequirement.getId()));

		accessRequirement.setVersionNumber(2L);
		accessRequirement.setName("new name");

		// call under test
		accessRequirement = accessRequirementDAO.update(accessRequirement);
		assertEquals(accessRequirement.getSubjectIds(), Collections.emptyList());
		assertEquals(List.of(subjectOne),
				accessRequirementDAO.getSubjects(accessRequirement.getId(), 1000L, 0L));

		// the update should not remove dynamically bound subjects.
		assertEquals(List.of(accessRequirement.getId()),
				accessRequirementDAO.getDynamicallyBoundAccessRequirementIdsForSubject(subjectOne));
	}
	
	@Test
	public void testValidateSubject() {
		RestrictableObjectDescriptor subject = new RestrictableObjectDescriptor().setId("syn123")
				.setType(RestrictableObjectType.ENTITY);
		// call under test
		Long subjectId = DBOAccessRequirementDAOImpl.validateSubject(subject);
		assertEquals(Long.valueOf(123L), subjectId);
	}
	
	@Test
	public void testValidateSubjectWithNullSubject() {
		RestrictableObjectDescriptor subject = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			DBOAccessRequirementDAOImpl.validateSubject(subject);
		}).getMessage();
		assertEquals("subject is required.", message);
	}
	
	@Test
	public void testValidateSubjectWithNullSubjectId() {
		RestrictableObjectDescriptor subject = new RestrictableObjectDescriptor().setId(null)
				.setType(RestrictableObjectType.ENTITY);
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			DBOAccessRequirementDAOImpl.validateSubject(subject);
		}).getMessage();
		assertEquals("subject.id is required.", message);
	}
	
	@Test
	public void testValidateSubjectWithNullSubjectType() {
		RestrictableObjectDescriptor subject = new RestrictableObjectDescriptor().setId("syn123")
				.setType(null);
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			DBOAccessRequirementDAOImpl.validateSubject(subject);
		}).getMessage();
		assertEquals("subject.type is required.", message);
	}
	
	@Test
	public void testAddDynamicallyBoundAccessRequirmentsToSubject() {
		List<AccessRequirement> ars = createArsWithSubjectsDefinedByAnnotations(3);

		Map<Long, String> startingEtags = ars.stream()
				.collect(Collectors.toMap(AccessRequirement::getId, AccessRequirement::getEtag));

		// one
		RestrictableObjectDescriptor subjectOne = new RestrictableObjectDescriptor().setId("syn123")
				.setType(RestrictableObjectType.ENTITY);
		List<Long> bindToOne = List.of(ars.get(0).getId(), ars.get(2).getId());
		
		// call under test
		accessRequirementDAO.addDynamicallyBoundAccessRequirmentsToSubject(subjectOne, bindToOne);
		
		assertFalse(doesAccessRequirmentEtagMatch(ars.get(0).getId(), startingEtags));
		assertTrue(doesAccessRequirmentEtagMatch(ars.get(1).getId(), startingEtags));
		assertFalse(doesAccessRequirmentEtagMatch(ars.get(2).getId(), startingEtags));
		
		assertEquals(bindToOne, accessRequirementDAO.getDynamicallyBoundAccessRequirementIdsForSubject(subjectOne));
	}
	
	@Test
	public void testAddDynamicallyBoundAccessRequirmentsToSubjectWithDuplicate() {
		List<AccessRequirement> ars = createArsWithSubjectsDefinedByAnnotations(3);

		Map<Long, String> startingEtags = ars.stream()
				.collect(Collectors.toMap(AccessRequirement::getId, AccessRequirement::getEtag));

		// one
		RestrictableObjectDescriptor subjectOne = new RestrictableObjectDescriptor().setId("syn123")
				.setType(RestrictableObjectType.ENTITY);
		List<Long> bindToOne = List.of(ars.get(0).getId(), ars.get(2).getId());
		
		accessRequirementDAO.addDynamicallyBoundAccessRequirmentsToSubject(subjectOne, bindToOne);
		
		assertFalse(doesAccessRequirmentEtagMatch(ars.get(0).getId(), startingEtags));
		assertTrue(doesAccessRequirmentEtagMatch(ars.get(1).getId(), startingEtags));
		assertFalse(doesAccessRequirmentEtagMatch(ars.get(2).getId(), startingEtags));
		
		List<Long> overlap = List.of(ars.get(0).getId(), ars.get(1).getId());
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			accessRequirementDAO.addDynamicallyBoundAccessRequirmentsToSubject(subjectOne, overlap);
		}).getMessage();
		assertEquals("One or more access requirement is already dynamically bound to this subject.", message);
		
		// the etag should not change since it was part of a duplicate batch.
		assertTrue(doesAccessRequirmentEtagMatch(ars.get(1).getId(), startingEtags));
		
		assertEquals(bindToOne, accessRequirementDAO.getDynamicallyBoundAccessRequirementIdsForSubject(subjectOne));
	}
	
	@Test
	public void testAddDynamicallyBoundAccessRequirmentsToSubjectWithMultipleSubjects() {
		List<AccessRequirement> ars = createArsWithSubjectsDefinedByAnnotations(4);

		Map<Long, String> startingEtags = ars.stream()
				.collect(Collectors.toMap(AccessRequirement::getId, AccessRequirement::getEtag));

		// one
		RestrictableObjectDescriptor subjectOne = new RestrictableObjectDescriptor().setId("syn123")
				.setType(RestrictableObjectType.ENTITY);
		List<Long> bindToOne = List.of(ars.get(0).getId(), ars.get(2).getId());
		
		// two
		RestrictableObjectDescriptor subjectTwo = new RestrictableObjectDescriptor().setId("syn456")
				.setType(RestrictableObjectType.ENTITY);
		List<Long> bindToTwo = List.of(ars.get(2).getId(), ars.get(3).getId());
		
		// call under test
		accessRequirementDAO.addDynamicallyBoundAccessRequirmentsToSubject(subjectOne, bindToOne);
		accessRequirementDAO.addDynamicallyBoundAccessRequirmentsToSubject(subjectTwo, bindToTwo);
		
		assertFalse(doesAccessRequirmentEtagMatch(ars.get(0).getId(), startingEtags));
		assertTrue(doesAccessRequirmentEtagMatch(ars.get(1).getId(), startingEtags));
		assertFalse(doesAccessRequirmentEtagMatch(ars.get(2).getId(), startingEtags));
		assertFalse(doesAccessRequirmentEtagMatch(ars.get(3).getId(), startingEtags));

		assertEquals(bindToOne, accessRequirementDAO.getDynamicallyBoundAccessRequirementIdsForSubject(subjectOne));
		assertEquals(bindToTwo, accessRequirementDAO.getDynamicallyBoundAccessRequirementIdsForSubject(subjectTwo));
	}
	
	@Test
	public void testRemovedDynamicallyBoundAccessRequirmentsToSubject() {
		List<AccessRequirement> ars = createArsWithSubjectsDefinedByAnnotations(3);

		// one
		RestrictableObjectDescriptor subjectOne = new RestrictableObjectDescriptor().setId("syn123")
				.setType(RestrictableObjectType.ENTITY);
		// add all 
		List<Long> bindToOne = ars.stream().map(AccessRequirement::getId).collect(Collectors.toList());
		// start with all bound
		accessRequirementDAO.addDynamicallyBoundAccessRequirmentsToSubject(subjectOne, bindToOne);
		
		Map<Long, String> startingEtags = createMapOfCurrentEtags(bindToOne);
		
		List<Long> toRemove = List.of(ars.get(0).getId(), ars.get(2).getId());
		
		// call under test
		accessRequirementDAO.removeDynamicallyBoundAccessRequirementsFromSubject(subjectOne, toRemove);
		
		assertEquals(List.of(ars.get(1).getId()), accessRequirementDAO.getDynamicallyBoundAccessRequirementIdsForSubject(subjectOne));
		
		assertFalse(doesAccessRequirmentEtagMatch(ars.get(0).getId(), startingEtags));
		assertTrue(doesAccessRequirmentEtagMatch(ars.get(1).getId(), startingEtags));
		assertFalse(doesAccessRequirmentEtagMatch(ars.get(2).getId(), startingEtags));
	}
	
	
	@Test
	public void testRemoveDynamicallyBoundAccessRequirmentsToSubjectWithMultipleSubjects() {
		List<AccessRequirement> ars = createArsWithSubjectsDefinedByAnnotations(4);

		// one
		RestrictableObjectDescriptor subjectOne = new RestrictableObjectDescriptor().setId("syn123")
				.setType(RestrictableObjectType.ENTITY);
		List<Long> bindToOne = List.of(ars.get(0).getId(), ars.get(2).getId());

		// two
		RestrictableObjectDescriptor subjectTwo = new RestrictableObjectDescriptor().setId("syn456")
				.setType(RestrictableObjectType.ENTITY);
		List<Long> bindToTwo = List.of(ars.get(2).getId(), ars.get(3).getId());

		accessRequirementDAO.addDynamicallyBoundAccessRequirmentsToSubject(subjectOne, bindToOne);
		accessRequirementDAO.addDynamicallyBoundAccessRequirmentsToSubject(subjectTwo, bindToTwo);

		Map<Long, String> startingEtags = createMapOfCurrentEtags(
				ars.stream().map(AccessRequirement::getId).collect(Collectors.toList()));

		List<Long> toRemoveFromOne = List.of(ars.get(0).getId());
		List<Long> toRemoveFromTwo = List.of(ars.get(2).getId());

		// call under test
		accessRequirementDAO.removeDynamicallyBoundAccessRequirementsFromSubject(subjectOne, toRemoveFromOne);
		accessRequirementDAO.removeDynamicallyBoundAccessRequirementsFromSubject(subjectTwo, toRemoveFromTwo);

		assertEquals(List.of(ars.get(2).getId()),
				accessRequirementDAO.getDynamicallyBoundAccessRequirementIdsForSubject(subjectOne));
		assertEquals(List.of(ars.get(3).getId()),
				accessRequirementDAO.getDynamicallyBoundAccessRequirementIdsForSubject(subjectTwo));

		assertFalse(doesAccessRequirmentEtagMatch(ars.get(0).getId(), startingEtags));
		assertTrue(doesAccessRequirmentEtagMatch(ars.get(1).getId(), startingEtags));
		assertFalse(doesAccessRequirmentEtagMatch(ars.get(2).getId(), startingEtags));
		assertTrue(doesAccessRequirmentEtagMatch(ars.get(3).getId(), startingEtags));
	}
	
	@Test
	public void testBootstrapLockAccessRequirement() {
		// call under test
		AccessRequirement ar = accessRequirementDAO.get(AccessRequirementDAO.INVALID_ANNOTATIONS_LOCK_ID.toString());
		assertTrue(ar instanceof LockAccessRequirement);
		assertEquals(AccessRequirementDAO.INVALID_ANNOTATIONS_LOCK_ID, ar.getId());
	}
	
	@Test
	public void testGetVersion() {
		// Call under test
		assertTrue(accessRequirementDAO.getVersion("123", -1L).isEmpty());
		
		accessRequirement = accessRequirementDAO.create(newEntityAccessRequirement(individualGroup, node, "foo"));
		
		Long versionNumber = accessRequirement.getVersionNumber();
		
		// Call under test
		assertEquals(accessRequirement, accessRequirementDAO.getVersion(accessRequirement.getId().toString(), accessRequirement.getVersionNumber()).get());
		
		accessRequirement.setName("Updated");
		accessRequirement.setVersionNumber(versionNumber + 1);
		
		accessRequirement = accessRequirementDAO.update(accessRequirement);
		
		assertNotEquals(versionNumber, accessRequirement.getVersionNumber());
		
		assertEquals(accessRequirement, accessRequirementDAO.getVersion(accessRequirement.getId().toString(), accessRequirement.getVersionNumber()).get());
		
		// Delete the access requirements
		accessRequirementDAO.delete(accessRequirement.getId().toString());
	}
		
	/**
	 * Does the provide etag match the current etag of the given access requirement.
	 * @param arId
	 * @param etag
	 * @return
	 */
	boolean doesAccessRequirmentEtagMatch(Long arId, String etag) {
		return etag.equals(accessRequirementDAO.get(arId.toString()).getEtag());
	}
	
	/**
	 * Does the current etag of the provided access requirement ID match the mapped etag for this ar.
	 * @param arId
	 * @param etagMap
	 * @return
	 */
	boolean doesAccessRequirmentEtagMatch(Long arId, Map<Long,String> etagMap) {
		return doesAccessRequirmentEtagMatch(arId, etagMap.get(arId));
	}
	
	/**
	 * Helper to get a map of each access requirements current etag
	 * @param arIds
	 * @return
	 */
	Map<Long,String> createMapOfCurrentEtags(List<Long> arIds) {
		Map<Long,String> etags = new HashMap<>(arIds.size());
		for(Long id: arIds) {
			etags.put(id, accessRequirementDAO.get(id.toString()).getEtag());
		}
		return etags;
	}
	
	/**
	 * Helper to create n access requirements with subjectsDefinedByAnnotations = true.
	 * @param count
	 * @return
	 */
	List<AccessRequirement> createArsWithSubjectsDefinedByAnnotations(int count){
		List<AccessRequirement> ars = new ArrayList<>(count);
		for(int i=0; i<count; i++) {
			AccessRequirement ar = newEntityAccessRequirement(individualGroup, node, "foo");
			ar.setSubjectIds(null);
			ar.setSubjectsDefinedByAnnotations(true);
			ar = accessRequirementDAO.create(ar);
			ars.add(ar);
		}
		return ars;
	}
	
	private void addReviewers(Long arId, List<String> reviewerIds) {
		AccessControlList acl = new AccessControlList()
			.setId(arId.toString())
			.setCreationDate(new Date())
			.setCreatedBy(individualGroup.getId())
			.setModifiedBy(individualGroup.getId())
			.setModifiedOn(new Date())
			.setResourceAccess(reviewerIds.stream().map(reviewerId -> 
				new ResourceAccess().setAccessType(Set.of(ACCESS_TYPE.REVIEW_SUBMISSIONS)).setPrincipalId(Long.valueOf(reviewerId))
			).collect(Collectors.toSet()));
		
		aclDao.create(acl, ObjectType.ACCESS_REQUIREMENT);
	}

}
