package org.sagebionetworks.repo.manager.file.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
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
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.IdRange;
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
		
		List<ScannedFileHandleAssociation> expected = Arrays.asList(
			// First node has 3 revisions
			new ScannedFileHandleAssociation(KeyFactory.stringToKey(node1.getId()), Long.valueOf(node1.getFileHandleId())),
			new ScannedFileHandleAssociation(KeyFactory.stringToKey(node1.getId()), Long.valueOf(addRevision(node1).getFileHandleId())),
			new ScannedFileHandleAssociation(KeyFactory.stringToKey(node1.getId()), Long.valueOf(addRevision(node1).getFileHandleId())),
			
			// Second node has only 1 revisions
			new ScannedFileHandleAssociation(KeyFactory.stringToKey(node2.getId()), Long.valueOf(node2.getFileHandleId())),
			
			// Third node has 2 revisions
			new ScannedFileHandleAssociation(KeyFactory.stringToKey(node3.getId()), Long.valueOf(node3.getFileHandleId())),
			new ScannedFileHandleAssociation(KeyFactory.stringToKey(node3.getId()), Long.valueOf(addRevision(node3).getFileHandleId()))
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
	
	private Node addRevision(Node node) {
		node.setFileHandleId(utils.generateFileHandle(user));
		node.setVersionLabel("Revision_" + UUID.randomUUID().toString());
		Long revision = nodeDao.createNewVersion(node);
		return nodeDao.getNodeForVersion(node.getId(), revision);
	}

}
