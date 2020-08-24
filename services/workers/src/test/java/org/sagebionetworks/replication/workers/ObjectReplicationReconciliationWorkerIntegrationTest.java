package org.sagebionetworks.replication.workers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.table.TableViewManager;
import org.sagebionetworks.repo.manager.table.metadata.DefaultColumnModelMapper;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ObjectField;
import org.sagebionetworks.repo.model.table.ViewEntityType;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewScope;
import org.sagebionetworks.table.cluster.ConnectionFactory;
import org.sagebionetworks.table.cluster.TableIndexDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.Lists;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class ObjectReplicationReconciliationWorkerIntegrationTest {
	
	private static final int MAX_WAIT_MS = 2* 60 *1000;
	
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private ConnectionFactory tableConnectionFactory;
	@Autowired
	UserManager userManager;
	@Autowired
	private TableViewManager viewManager;
	@Autowired
	DefaultColumnModelMapper modelMapper;

	@Mock
	ProgressCallback mockProgressCallback;
	
	TableIndexDAO indexDao;
	
	UserInfo adminUserInfo;
	Project project;
	
	String projectId;
	Long projectIdLong;
	
	ViewObjectType viewObjectType;
	
	@BeforeEach
	public void before() throws Exception {
		
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		project = new Project();
		project.setName(UUID.randomUUID().toString());
		projectId = entityManager.createEntity(adminUserInfo, project, null);
		project = entityManager.getEntity(adminUserInfo, projectId, Project.class);
		projectIdLong = KeyFactory.stringToKey(projectId);
		indexDao = tableConnectionFactory.getAllConnections().get(0);
		// ensure a sycn can occur.
		indexDao.truncateReplicationSyncExpiration();
		
		viewObjectType = ViewObjectType.ENTITY;
	}
	
	@AfterEach
	public void after(){
		if(project != null){
			entityManager.deleteEntity(adminUserInfo, project.getId());
		}
	}
	
	@Test
	public void testReconciliation() throws Exception{
		// Add a folder to the project
		Folder folder = addHierarchyToProject();
		// wait for the folder to replicated
		ObjectDataDTO dto = waitForEntityDto(folder.getId());
		assertNotNull(dto);
		
		// Simulate out-of-synch by deleting the project's replication data
		indexDao.deleteObjectData(viewObjectType, Lists.newArrayList(KeyFactory.stringToKey(folder.getId())));
		
		// ensure a sycn can occur.
		indexDao.truncateReplicationSyncExpiration();
		// by creating a view we trigger a reconcile.
		List<String> scope = Lists.newArrayList(project.getId());
		long viewTypeMask = 0x08;
		createView(scope, viewTypeMask);
		
		// wait for reconciliation to restore the deleted data.
		waitForEntityDto(folder.getId());
	}
	
	/**
	 * With PLFM_5352, there are cases where the benefactor can be out-of-date in the entity replication table.
	 * The reconciliation process should detect and repair these cases.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testPLFM_5352() throws InterruptedException {
		// Add a folder to the project
		Folder folder = addHierarchyToProject();
		// wait for the folder to replicated
		ObjectDataDTO dto = waitForEntityDto(folder.getId());
		assertNotNull(dto);
		
		// simulate a stale benefactor on the folder
		indexDao.deleteObjectData(viewObjectType, Lists.newArrayList(KeyFactory.stringToKey(folder.getId())));
		dto.setBenefactorId(KeyFactory.stringToKey(folder.getId()));
		indexDao.addObjectData(viewObjectType, Lists.newArrayList(dto));
		
		// ensure a sycn can occur.
		indexDao.truncateReplicationSyncExpiration();
		// by creating a view we trigger a reconcile.
		List<String> scope = Lists.newArrayList(project.getId());
		long viewTypeMask = 0x08;
		EntityView view = createView(scope, viewTypeMask);
		IdAndVersion.parse(view.getId());
	
		// Wait for the benefactor to be fixed
		Long expectedBenefactor = projectIdLong;
		waitForEntityDto(folder.getId(), expectedBenefactor);
	}
	
	EntityView createView(List<String> scopeIds, long viewTypeMask) {
		EntityView view = new EntityView();
		view.setName(UUID.randomUUID().toString());
		view.setScopeIds(scopeIds);
		view.setViewTypeMask(viewTypeMask);
		view.setParentId(projectId);
		List<ColumnModel> cm = modelMapper.getColumnModels(viewObjectType, ObjectField.name);
		view.setColumnIds(Lists.newArrayList(cm.get(0).getId()));
		String activityId = null;
		String viewId = entityManager.createEntity(adminUserInfo, view, activityId);
		view = entityManager.getEntity(adminUserInfo, viewId, EntityView.class);
		ViewScope scope = new ViewScope();
		scope.setViewEntityType(ViewEntityType.entityview);
		scope.setScope(view.getScopeIds());
		scope.setViewType(view.getType());
		scope.setViewTypeMask(view.getViewTypeMask());
		viewManager.setViewSchemaAndScope(adminUserInfo, view.getColumnIds(), scope,  view.getId());
		return view;
	}
	
	/**
	 * Add a folder and a file to the project.
	 * hierarchy 
	 * @return
	 */
	public Folder addHierarchyToProject() {
		// Add a folder to the project
		Folder folder = new Folder();
		folder.setName(UUID.randomUUID().toString());
		folder.setParentId(projectId);
		String folderIdString = entityManager.createEntity(adminUserInfo, folder, null);
		folder = entityManager.getEntity(adminUserInfo, folderIdString, Folder.class);
		
		// add a child folder
		Folder child = new Folder();
		child.setName(UUID.randomUUID().toString());
		child.setParentId(folder.getId());
		String childId = entityManager.createEntity(adminUserInfo, child, null);
		return entityManager.getEntity(adminUserInfo, childId, Folder.class);
	}
	
	/**
	 * Helper to wait for an entity's replication data to appear.
	 * 
	 * @param entityId
	 * @return
	 * @throws InterruptedException
	 */
	public ObjectDataDTO waitForEntityDto(String entityId) throws InterruptedException{
		Long expectedBenefactor = null;
		return waitForEntityDto(entityId, expectedBenefactor);
	}
	
	/**
	 * Helper to wait for an entity's replication data to appear.
	 * 
	 * @param entityId
	 * @param When not null, will wait for the given entity benefactor to match the provided value.
	 * @return
	 * @throws InterruptedException
	 */
	public ObjectDataDTO waitForEntityDto(String entityId, Long expectedBenefactor) throws InterruptedException{
		long startTimeMS = System.currentTimeMillis();
		while(true){
			ObjectDataDTO entityDto = indexDao.getObjectData(viewObjectType, KeyFactory.stringToKey(entityId));
			if(entityDto != null){
				if(expectedBenefactor == null || expectedBenefactor.equals(entityDto.getBenefactorId())) {
					return entityDto;
				}
			}
			System.out.println("Waiting for entity data to be replicated for id: "+entityId);
			Thread.sleep(2000);
			assertTrue(System.currentTimeMillis()-startTimeMS < MAX_WAIT_MS,"Timed-out waiting for entity data to be replicated.");
		}
	}
}
