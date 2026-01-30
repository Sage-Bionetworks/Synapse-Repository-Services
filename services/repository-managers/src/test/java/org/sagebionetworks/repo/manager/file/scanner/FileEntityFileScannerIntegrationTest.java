package org.sagebionetworks.repo.manager.file.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.IdRange;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.helper.DaoObjectHelper;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class FileEntityFileScannerIntegrationTest {

	@Autowired
	private NodeDAO nodeDao;
	
	@Autowired
	private FileHandleDao fileHandleDao;
			
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private DaoObjectHelper<Node> nodeDaoHelper;
	
	@Autowired
	private FileHandleAssociationScannerTestUtils utils;
	
	@Autowired
	private FileHandleAssociationManager manager;
	
	private FileHandleAssociateType associationType = FileHandleAssociateType.FileEntity;
	
	private UserInfo user;
	
	@BeforeEach
	public void before() {
		nodeDao.truncateAll();
		fileHandleDao.truncateTable();
		
		user = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
	}
	
	@AfterEach
	public void after() {
		nodeDao.truncateAll();
		fileHandleDao.truncateTable();
	}
	
	@Test
	public void testScanner() {
		
		String projectId = nodeDaoHelper.create(n -> {
			n.setName("Project");
			n.setCreatedByPrincipalId(user.getId());
		}).getId();
		
		String folderId = nodeDaoHelper.create(n -> {
			n.setNodeType(EntityType.folder);
			n.setName("Folder");
			n.setCreatedByPrincipalId(user.getId());
			n.setParentId(projectId);
		}).getId();
		
		Node node1 = createFileNode(folderId);
		Node node2 = createFileNode(folderId);
		Node node3 = createFileNode(folderId);
		
		List<ScannedFileHandleAssociation> expected = List.of(
			// First node has 3 revisions
			getFileAssociation(node1),
			getFileAssociation(addRevision(node1, false)),
			getFileAssociation(addRevision(node1, false)),
			
			// Second node has only 1 revisions
			getFileAssociation(node2),
			
			// Third node has 2 revisions and a validation file handle on the second revision
			getFileAssociation(node3),
			getFileAssociation(addRevision(node3, true))
		);
		
		IdRange range = manager.getIdRange(associationType);
		
		// Call under test
		List<ScannedFileHandleAssociation> result = StreamSupport.stream(manager.scanRange(associationType, range).spliterator(), false).collect(Collectors.toList());
		
		assertEquals(expected, result);
		
	}
	
	private Node createFileNode(String parentId) {
		return nodeDaoHelper.create(n -> {
			n.setName("File-" + UUID.randomUUID().toString());
			n.setCreatedByPrincipalId(user.getId());
			n.setParentId(parentId);
			n.setNodeType(EntityType.file);
			n.setFileHandleId(utils.generateFileHandle(user));
			
		});
	}
	
	private Node addRevision(Node node, boolean withValidationFile) {
		node.setFileHandleId(utils.generateFileHandle(user));
		node.setVersionLabel("Revision_" + UUID.randomUUID().toString());
		if (withValidationFile) {
			node.setValidationResultFileHandleId(utils.generateFileHandle(user));
		}
		Long revision = nodeDao.createNewVersion(node);
		return nodeDao.getNodeForVersion(node.getId(), revision);
	}
	
	ScannedFileHandleAssociation getFileAssociation(Node node) {
		Set<Long> fileHandleIds = new HashSet<>();
		fileHandleIds.add(Long.valueOf(node.getFileHandleId()));
		if (node.getValidationResultFileHandleId() != null) {
			fileHandleIds.add(Long.valueOf(node.getValidationResultFileHandleId()));
		}
		return new ScannedFileHandleAssociation(KeyFactory.stringToKey(node.getId())).withFileHandleIds(fileHandleIds);
	}

}
