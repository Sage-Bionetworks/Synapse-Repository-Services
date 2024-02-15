package org.sagebionetworks.repo.manager.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.sagebionetworks.repo.model.util.AccessControlListUtil.createResourceAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityRef;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.helper.AccessControlListObjectHelper;
import org.sagebionetworks.repo.model.helper.FileHandleObjectHelper;
import org.sagebionetworks.repo.model.helper.NodeDaoObjectHelper;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.Dataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DatasetBackfillAutowiredTest {

	@Autowired
	private EntityManager entityManager;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private DatasetChecksumBackfill datasetBackfill;
	
	@Autowired
	private AccessControlListObjectHelper aclHelper;
	
	@Autowired
	private FileHandleObjectHelper fileHandleHelper;
	
	@Autowired
	private NodeDaoObjectHelper nodeHelper;
		
	@Autowired
	private NodeDAO nodeDao;
	
	private UserInfo adminUser;
	
	private String datasetId;
		
	@BeforeEach
	public void before() {
		fileHandleHelper.truncateAll();
		entityManager.truncateAll();
		
		adminUser = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		String projectId = nodeHelper.create( p -> {
			p.setName("Project");
			p.setNodeType(EntityType.project);
		}).getId();
		
		aclHelper.create((a) -> {
			a.setId(projectId);
			a.getResourceAccess().add(createResourceAccess(adminUser.getId(), ACCESS_TYPE.READ));
		});
		
		List<EntityRef> datasetRefs = new ArrayList<>();
		
		IntStream.range(0, 10).forEach( i-> {
			String fileId = fileHandleHelper.create(f -> {
				f.setFileName("handle_" + i);
				f.setContentMd5(DigestUtils.md2Hex(f.getFileName()));
			}).getId();
			
			Node fileNode = nodeHelper.create( n-> {
				n.setParentId(projectId);
				n.setName("file_" + i);
				n.setNodeType(EntityType.file);
				n.setFileHandleId(fileId);
			});
						
			datasetRefs.add(new EntityRef().setEntityId(fileNode.getId()).setVersionNumber(fileNode.getVersionNumber()));
		});
		
		Node datasetNode = nodeHelper.create( n -> {
			n.setParentId(projectId);
			n.setName("dataset");
			n.setNodeType(EntityType.dataset);
			n.setItems(datasetRefs);
		});
		
		Annotations props = new Annotations();
		
		props.addAnnotation("checksum", "oldWrongMd5");
		
		nodeDao.updateEntityPropertyAnnotations(datasetNode.getId(), props);
		
		datasetNode.setVersionLabel("New Version");
		datasetNode.setItems(datasetNode.getItems().subList(0, 5));
		
		nodeHelper.createNewVersion(datasetNode);
		
		props.replaceAnnotation("checksum", "newWrongMd5");
		
		nodeDao.updateEntityPropertyAnnotations(datasetNode.getId(), props);
		
		datasetId = KeyFactory.stringToKey(datasetNode.getId()).toString();
	}
	
	@Test
	public void testBackfill() {
		
		Dataset dataset = entityManager.getEntity(adminUser, datasetId, Dataset.class);
		
		String currentEtag = dataset.getEtag();
		
		assertEquals("newWrongMd5", dataset.getChecksum());
		assertEquals(2L, dataset.getVersionNumber());
		
		Dataset datasetOldVersion = entityManager.getEntityForVersion(adminUser, datasetId, 1L, Dataset.class);
		
		assertEquals("oldWrongMd5", datasetOldVersion.getChecksum());
		assertEquals(1L, datasetOldVersion.getVersionNumber());
		
		datasetBackfill.backfillChecksum(adminUser);
		
		Dataset updatedDataset = entityManager.getEntity(adminUser, datasetId, Dataset.class);
		
		assertNotEquals(currentEtag, updatedDataset.getEtag());
		assertNotEquals(dataset.getChecksum(), updatedDataset.getChecksum());
		
		Dataset updatedDatasetOldVersion = entityManager.getEntityForVersion(adminUser, datasetId, 1L, Dataset.class);
		
		assertNotEquals(datasetOldVersion.getChecksum(), updatedDatasetOldVersion.getChecksum());
		
		assertNotEquals(updatedDataset.getChecksum(), updatedDatasetOldVersion.getChecksum());
	}

}
