package org.sagebionetworks.repo.model.dbo.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.schema.ObjectType;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.model.schema.ValidationSummaryStatistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.Sets;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class EntitySchemaValidationResultDaoImplTest {

	@Autowired
	NodeDAO nodeDao;
	@Autowired
	SchemaValidationResultDao schemaValidationResultDao;
	@Autowired
	EntitySchemaValidationResultDao entitySchemaValidationResultDao;

	private Long adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();

	String projectId;

	@AfterEach
	public void after() {
		if (projectId != null) {
			nodeDao.delete(projectId);
		}
		schemaValidationResultDao.clearAll();
	}

	@Test
	public void testGetEntityValidationStatistics() {
		Node project = createNode("a project", null, EntityType.project);
		projectId = project.getId();
		Node validFolder = createNode("valid folder", projectId, EntityType.folder);
		createValidationResults(validFolder, true);
		Node invalidFolderOne = createNode("invalid folder one", projectId, EntityType.folder);
		createValidationResults(invalidFolderOne, false);
		Node invalidFolderTwo = createNode("invalid folder two", projectId, EntityType.folder);
		createValidationResults(invalidFolderTwo, false);
		createNode("unknown folder one", projectId, EntityType.folder);
		createNode("unknown folder two", projectId, EntityType.folder);
		createNode("unknown folder three", projectId, EntityType.folder);
		Set<Long> childIdsToExclude = new HashSet<Long>(0);
		// Call under test
		ValidationSummaryStatistics stats = entitySchemaValidationResultDao.getEntityValidationStatistics(projectId,
				childIdsToExclude);
		assertNotNull(stats);
		assertEquals(projectId, stats.getContainerId());
		assertNotNull(stats.getGeneratedOn());
		assertEquals(new Long(1), stats.getNumberOfValidChildren());
		assertEquals(new Long(2), stats.getNumberOfInvalidChildren());
		assertEquals(new Long(3), stats.getNumberOfUnknownChildren());
		assertEquals(new Long(6), stats.getTotalNumberOfChildren());
	}

	@Test
	public void testGetEntityValidationStatisticsWithNoUnknown() {
		Node project = createNode("a project", null, EntityType.project);
		projectId = project.getId();
		Node validFolder = createNode("valid folder", projectId, EntityType.folder);
		createValidationResults(validFolder, true);
		Node invalidFolderOne = createNode("invalid folder one", projectId, EntityType.folder);
		createValidationResults(invalidFolderOne, false);
		Node invalidFolderTwo = createNode("invalid folder two", projectId, EntityType.folder);
		createValidationResults(invalidFolderTwo, false);
		Set<Long> childIdsToExclude = new HashSet<Long>(0);
		// Call under test
		ValidationSummaryStatistics stats = entitySchemaValidationResultDao.getEntityValidationStatistics(projectId,
				childIdsToExclude);
		assertNotNull(stats);
		assertEquals(projectId, stats.getContainerId());
		assertNotNull(stats.getGeneratedOn());
		assertEquals(new Long(1), stats.getNumberOfValidChildren());
		assertEquals(new Long(2), stats.getNumberOfInvalidChildren());
		assertEquals(new Long(0), stats.getNumberOfUnknownChildren());
		assertEquals(new Long(3), stats.getTotalNumberOfChildren());
	}

	@Test
	public void testGetEntityValidationStatisticsWithNoValid() {
		Node project = createNode("a project", null, EntityType.project);
		projectId = project.getId();
		Node invalidFolderOne = createNode("invalid folder one", projectId, EntityType.folder);
		createValidationResults(invalidFolderOne, false);
		Node invalidFolderTwo = createNode("invalid folder two", projectId, EntityType.folder);
		createValidationResults(invalidFolderTwo, false);
		createNode("unknown folder one", projectId, EntityType.folder);
		createNode("unknown folder two", projectId, EntityType.folder);
		createNode("unknown folder three", projectId, EntityType.folder);
		Set<Long> childIdsToExclude = new HashSet<Long>(0);
		// Call under test
		ValidationSummaryStatistics stats = entitySchemaValidationResultDao.getEntityValidationStatistics(projectId,
				childIdsToExclude);
		assertNotNull(stats);
		assertEquals(projectId, stats.getContainerId());
		assertNotNull(stats.getGeneratedOn());
		assertEquals(new Long(0), stats.getNumberOfValidChildren());
		assertEquals(new Long(2), stats.getNumberOfInvalidChildren());
		assertEquals(new Long(3), stats.getNumberOfUnknownChildren());
		assertEquals(new Long(5), stats.getTotalNumberOfChildren());
	}

	@Test
	public void testGetEntityValidationStatisticsWithNoInvalid() {
		Node project = createNode("a project", null, EntityType.project);
		projectId = project.getId();
		Node validFolder = createNode("valid folder", projectId, EntityType.folder);
		createValidationResults(validFolder, true);
		createNode("unknown folder one", projectId, EntityType.folder);
		createNode("unknown folder two", projectId, EntityType.folder);
		createNode("unknown folder three", projectId, EntityType.folder);
		Set<Long> childIdsToExclude = new HashSet<Long>(0);
		// Call under test
		ValidationSummaryStatistics stats = entitySchemaValidationResultDao.getEntityValidationStatistics(projectId,
				childIdsToExclude);
		assertNotNull(stats);
		assertEquals(projectId, stats.getContainerId());
		assertNotNull(stats.getGeneratedOn());
		assertEquals(new Long(1), stats.getNumberOfValidChildren());
		assertEquals(new Long(0), stats.getNumberOfInvalidChildren());
		assertEquals(new Long(3), stats.getNumberOfUnknownChildren());
		assertEquals(new Long(4), stats.getTotalNumberOfChildren());
	}

	@Test
	public void testGetEntityValidationStatisticsWithChildrenToExclude() {
		Node project = createNode("a project", null, EntityType.project);
		projectId = project.getId();
		Node validFolder = createNode("valid folder", projectId, EntityType.folder);
		createValidationResults(validFolder, true);
		Node invalidFolderOne = createNode("invalid folder one", projectId, EntityType.folder);
		createValidationResults(invalidFolderOne, false);
		Node invalidFolderTwo = createNode("invalid folder two", projectId, EntityType.folder);
		createValidationResults(invalidFolderTwo, false);
		createNode("unknown folder one", projectId, EntityType.folder);
		createNode("unknown folder two", projectId, EntityType.folder);
		Node unknownThree = createNode("unknown folder three", projectId, EntityType.folder);
		// Filter out the valid folder and the third unknown.
		Set<Long> childIdsToExclude = Sets.newHashSet(KeyFactory.stringToKey(validFolder.getId()),
				KeyFactory.stringToKey(unknownThree.getId()));
		// Call under test
		ValidationSummaryStatistics stats = entitySchemaValidationResultDao.getEntityValidationStatistics(projectId,
				childIdsToExclude);
		assertNotNull(stats);
		assertEquals(projectId, stats.getContainerId());
		assertNotNull(stats.getGeneratedOn());
		assertEquals(new Long(0), stats.getNumberOfValidChildren());
		assertEquals(new Long(2), stats.getNumberOfInvalidChildren());
		assertEquals(new Long(2), stats.getNumberOfUnknownChildren());
		assertEquals(new Long(4), stats.getTotalNumberOfChildren());
	}

	@Test
	public void testGetEntityValidationStatisticsWithExistingContainerWithNoChildren() {
		Node project = createNode("a project", null, EntityType.project);
		projectId = project.getId();
		Set<Long> childIdsToExclude = new HashSet<Long>(0);
		// Call under test
		ValidationSummaryStatistics stats = entitySchemaValidationResultDao.getEntityValidationStatistics(projectId,
				childIdsToExclude);
		assertNotNull(stats);
		assertEquals(projectId, stats.getContainerId());
		assertNotNull(stats.getGeneratedOn());
		assertEquals(new Long(0), stats.getNumberOfValidChildren());
		assertEquals(new Long(0), stats.getNumberOfInvalidChildren());
		assertEquals(new Long(0), stats.getNumberOfUnknownChildren());
		assertEquals(new Long(0), stats.getTotalNumberOfChildren());
	}

	@Test
	public void testGetEntityValidationStatisticsWithContainerDoesNotExist() {
		String containerId = "syn123";
		Set<Long> childIdsToExclude = new HashSet<Long>(0);
		// Call under test
		ValidationSummaryStatistics stats = entitySchemaValidationResultDao.getEntityValidationStatistics(containerId,
				childIdsToExclude);
		assertNotNull(stats);
		assertEquals(containerId, stats.getContainerId());
		assertNotNull(stats.getGeneratedOn());
		assertEquals(new Long(0), stats.getNumberOfValidChildren());
		assertEquals(new Long(0), stats.getNumberOfInvalidChildren());
		assertEquals(new Long(0), stats.getNumberOfUnknownChildren());
		assertEquals(new Long(0), stats.getTotalNumberOfChildren());
	}

	@Test
	public void testGetEntityValidationStatisticsWithNullContainerId() {
		String containerId = null;
		Set<Long> childIdsToExclude = new HashSet<Long>(0);
		assertThrows(IllegalArgumentException.class, () -> {
			entitySchemaValidationResultDao.getEntityValidationStatistics(containerId, childIdsToExclude);
		});
	}

	@Test
	public void testGetEntityValidationStatisticsWithNullSet() {
		String containerId = "syn123";
		Set<Long> childIdsToExclude = null;
		assertThrows(IllegalArgumentException.class, () -> {
			entitySchemaValidationResultDao.getEntityValidationStatistics(containerId, childIdsToExclude);
		});
	}

	@Test
	public void testGetInvalidEntitySchemaValidationPage() {
		Node project = createNode("a project", null, EntityType.project);
		projectId = project.getId();
		Node validFolder = createNode("valid folder", projectId, EntityType.folder);
		createValidationResults(validFolder, true);
		Node invalidFolderOne = createNode("invalid folder one", projectId, EntityType.folder);
		ValidationResults inValidResultOne = createValidationResults(invalidFolderOne, false);
		Node invalidFolderTwo = createNode("invalid folder two", projectId, EntityType.folder);
		ValidationResults inValidResultTwo = createValidationResults(invalidFolderTwo, false);
		createNode("unknown folder one", projectId, EntityType.folder);

		Set<Long> childIdsToExclude = new HashSet<Long>(0);
		long limit = 100;
		long offset = 0;

		// Call under test
		List<ValidationResults> page = entitySchemaValidationResultDao.getInvalidEntitySchemaValidationPage(projectId,
				childIdsToExclude, limit, offset);
		assertNotNull(page);
		assertEquals(2, page.size());
		assertEquals(inValidResultOne, page.get(0));
		assertEquals(inValidResultTwo, page.get(1));
	}
	
	@Test
	public void testGetInvalidEntitySchemaValidationPageWithChildrenToExclude() {
		Node project = createNode("a project", null, EntityType.project);
		projectId = project.getId();
		Node validFolder = createNode("valid folder", projectId, EntityType.folder);
		createValidationResults(validFolder, true);
		Node invalidFolderOne = createNode("invalid folder one", projectId, EntityType.folder);
		ValidationResults inValidResultOne = createValidationResults(invalidFolderOne, false);
		Node invalidFolderTwo = createNode("invalid folder two", projectId, EntityType.folder);
		createValidationResults(invalidFolderTwo, false);
		createNode("unknown folder one", projectId, EntityType.folder);

		Set<Long> childIdsToExclude = Sets.newHashSet(KeyFactory.stringToKey(validFolder.getId()), KeyFactory.stringToKey(invalidFolderTwo.getId()));
		long limit = 100;
		long offset = 0;

		// Call under test
		List<ValidationResults> page = entitySchemaValidationResultDao.getInvalidEntitySchemaValidationPage(projectId,
				childIdsToExclude, limit, offset);
		assertNotNull(page);
		assertEquals(1, page.size());
		assertEquals(inValidResultOne, page.get(0));
	}

	
	@Test
	public void testGetInvalidEntitySchemaValidationPageWithLimit() {
		Node project = createNode("a project", null, EntityType.project);
		projectId = project.getId();
		Node validFolder = createNode("valid folder", projectId, EntityType.folder);
		createValidationResults(validFolder, true);
		Node invalidFolderOne = createNode("invalid folder one", projectId, EntityType.folder);
		ValidationResults inValidResultOne = createValidationResults(invalidFolderOne, false);
		Node invalidFolderTwo = createNode("invalid folder two", projectId, EntityType.folder);
		createValidationResults(invalidFolderTwo, false);
		createNode("unknown folder one", projectId, EntityType.folder);

		Set<Long> childIdsToExclude = new HashSet<Long>(0);
		long limit = 1;
		long offset = 0;

		// Call under test
		List<ValidationResults> page = entitySchemaValidationResultDao.getInvalidEntitySchemaValidationPage(projectId,
				childIdsToExclude, limit, offset);
		assertNotNull(page);
		assertEquals(1, page.size());
		assertEquals(inValidResultOne, page.get(0));
	}
	
	@Test
	public void testGetInvalidEntitySchemaValidationPageWithOffset() {
		Node project = createNode("a project", null, EntityType.project);
		projectId = project.getId();
		Node validFolder = createNode("valid folder", projectId, EntityType.folder);
		createValidationResults(validFolder, true);
		Node invalidFolderOne = createNode("invalid folder one", projectId, EntityType.folder);
		createValidationResults(invalidFolderOne, false);
		Node invalidFolderTwo = createNode("invalid folder two", projectId, EntityType.folder);
		ValidationResults inValidResultTwo = createValidationResults(invalidFolderTwo, false);
		createNode("unknown folder one", projectId, EntityType.folder);

		Set<Long> childIdsToExclude = new HashSet<Long>(0);
		long limit = 10;
		long offset = 1;

		// Call under test
		List<ValidationResults> page = entitySchemaValidationResultDao.getInvalidEntitySchemaValidationPage(projectId,
				childIdsToExclude, limit, offset);
		assertNotNull(page);
		assertEquals(1, page.size());
		assertEquals(inValidResultTwo, page.get(0));
	}
	
	@Test
	public void testGetInvalidEntitySchemaValidationPageWithNoResults() {
		Node project = createNode("a project", null, EntityType.project);
		projectId = project.getId();
		Set<Long> childIdsToExclude = new HashSet<Long>(0);
		long limit = 10;
		long offset = 0;

		// Call under test
		List<ValidationResults> page = entitySchemaValidationResultDao.getInvalidEntitySchemaValidationPage(projectId,
				childIdsToExclude, limit, offset);
		assertNotNull(page);
		assertEquals(0, page.size());
	}
	
	@Test
	public void testGetInvalidEntitySchemaValidationPageWithDoesNotEixst() {
		String containerId = "syn123";
		Set<Long> childIdsToExclude = new HashSet<Long>(0);
		long limit = 10;
		long offset = 0;

		// Call under test
		List<ValidationResults> page = entitySchemaValidationResultDao.getInvalidEntitySchemaValidationPage(containerId,
				childIdsToExclude, limit, offset);
		assertNotNull(page);
		assertEquals(0, page.size());
	}
	
	@Test
	public void testGetInvalidEntitySchemaValidationPageWithNullContainerId() {
		String containerId = null;
		Set<Long> childIdsToExclude = new HashSet<Long>(0);
		long limit = 10;
		long offset = 0;
		assertThrows(IllegalArgumentException.class, ()->{
			entitySchemaValidationResultDao.getInvalidEntitySchemaValidationPage(containerId,
					childIdsToExclude, limit, offset);
		});
	}
	
	@Test
	public void testGetInvalidEntitySchemaValidationPageWithNullToExclude() {
		String containerId = "syn123";
		Set<Long> childIdsToExclude = null;
		long limit = 10;
		long offset = 0;
		assertThrows(IllegalArgumentException.class, ()->{
			entitySchemaValidationResultDao.getInvalidEntitySchemaValidationPage(containerId,
					childIdsToExclude, limit, offset);
		});
	}
	
	Node createNode(String name, String parentId, EntityType type) {
		Node node = new Node();
		node.setParentId(parentId);
		node.setNodeType(type);
		node.setName(name);
		node.setCreatedByPrincipalId(adminUserId);
		node.setCreatedOn(new Date());
		node.setModifiedByPrincipalId(adminUserId);
		node.setModifiedOn(new Date());
		return nodeDao.createNewNode(node);
	}

	ValidationResults createValidationResults(Node node, boolean isValid) {
		ValidationResults vr = new ValidationResults();
		vr.setIsValid(isValid);
		vr.setObjectId(node.getId());
		vr.setObjectType(ObjectType.entity);
		vr.setObjectEtag(node.getETag());
		vr.setSchema$id("my.org-fake");
		vr.setValidatedOn(new Date());
		schemaValidationResultDao.createOrUpdateResults(vr);
		return vr;
	}

}
