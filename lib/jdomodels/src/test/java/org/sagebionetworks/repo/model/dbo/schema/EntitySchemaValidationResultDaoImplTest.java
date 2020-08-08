package org.sagebionetworks.repo.model.dbo.schema;



import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.schema.ObjectType;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.model.schema.ValidationSummaryStatistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

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
		if(projectId != null) {
			nodeDao.delete(projectId);
		}
		schemaValidationResultDao.clearAll();
	}
	
	@Test
	public void testGetEntityValidationStatistics() {
		Node project = createNode("a project", null, EntityType.project);
		projectId = project.getId();
		// Add a folder
		Node validFolder = createNode("valid folder", projectId, EntityType.folder);
		ValidationResults validFolderResults = createValidationResults(validFolder, true);
		Node invalidFolder = createNode("invalid folder", projectId, EntityType.folder);
		ValidationResults invalidFolderResults = createValidationResults(invalidFolder, false);
		Node unknownFolder = createNode("unknown folder", projectId, EntityType.folder);
		Set<Long> childIdsToExclude = new HashSet<Long>(0);
		// Call under test
		ValidationSummaryStatistics stats = entitySchemaValidationResultDao.getEntityValidationStatistics(projectId, childIdsToExclude);
		assertNotNull(stats);
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
