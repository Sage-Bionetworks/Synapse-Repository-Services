package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.sagebionetworks.repo.manager.AccessRequirementManager;
import org.sagebionetworks.repo.manager.AccessRequirementManagerImpl;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityWithAnnotations;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class EntityManagerImplAutowireTest {
	
	@Autowired
	private EntityManager entityManager;	
	
	@Autowired
	public UserManager userManager;	
	
	@Autowired 
	private ActivityManager activityManager;
	
	@Autowired
	private FileHandleManager fileHandleManager;
	
	@Autowired
	private AccessRequirementManager accessRequirementManager;
	
	// We use a mock auth DAO for this test.
	private AuthorizationManager mockAuth;

	private List<String> toDelete;
	private List<String> activitiesToDelete;
	private List<String> fileHandlesToDelete;
	
	private UserInfo adminUserInfo;
	private UserInfo userInfo;
	private Long userId;
	
	private AccessRequirement arToDelete;
	
	@Before
	public void before() throws Exception{
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		NewUser nu = new NewUser();
		nu.setUserName("test");
		nu.setEmail("just.a.test@sagebase.org");
		userId = userManager.createUser(nu);
		userInfo = userManager.getUserInfo(userId);
		userInfo.getGroups().add(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());

		
		toDelete = new ArrayList<String>();
		activitiesToDelete = new ArrayList<String>();
		fileHandlesToDelete = new ArrayList<String>();
		mockAuth = Mockito.mock(AuthorizationManager.class);
		when(mockAuth.canAccess((UserInfo)any(), anyString(), any(ObjectType.class), any(ACCESS_TYPE.class))).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		when(mockAuth.canCreate((UserInfo)any(), (String)any(), (EntityType)any())).thenReturn(AuthorizationManagerUtil.AUTHORIZED);

	}
	
	@After
	public void after() throws Exception {
		if(entityManager != null && toDelete != null){
			for(String id: toDelete){
				try{
					entityManager.deleteEntity(adminUserInfo, id);
				}catch(Exception e){}
			}
		}
		if(activityManager != null && activitiesToDelete != null){
			for(String id: activitiesToDelete){
				try{
					activityManager.deleteActivity(adminUserInfo, id);
				}catch(Exception e){}
			}
		}
		if(fileHandleManager != null && fileHandlesToDelete != null){
			for(String id: fileHandlesToDelete){
				try{
					fileHandleManager.deleteFileHandle(adminUserInfo, id);
				}catch(Exception e){}
			}
		}
		
		if (accessRequirementManager!=null && arToDelete!=null) {
			accessRequirementManager.deleteAccessRequirement(adminUserInfo, ""+arToDelete.getId());
		}
		
		if (userId!=null) {
			userManager.deletePrincipal(adminUserInfo, userId);
		}
	}
	
	@Test
	public void testMoveRestrictedEntity() throws Exception {
		// create a project with a child
		Project source = new Project();
		source.setName("orig parent");
		String sourceId = entityManager.createEntity(userInfo, source, null);
		toDelete.add(sourceId);
		// add a restriction to the project
		AccessRequirement ar = AccessRequirementManagerImpl.newLockAccessRequirement(adminUserInfo, sourceId, "jiraKey");
		arToDelete = accessRequirementManager.createAccessRequirement(adminUserInfo, ar);
		Folder child = new Folder();
		child.setName("child");
		child.setParentId(sourceId);
		String childId = entityManager.createEntity(userInfo, child, null);
		toDelete.add(childId);
		Project dest = new Project();
		dest.setName("new parent");
		String destinationId = entityManager.createEntity(userInfo, dest, null);
		toDelete.add(destinationId);
		child = entityManager.getEntity(userInfo, childId, Folder.class);
		// try to move the child (should fail)
		child.setParentId(destinationId);
		try {
			entityManager.updateEntity(userInfo, child, false, null);
			fail("Expected UnauthorizedException");
		} catch (UnauthorizedException ue) {
			// as expected
		}
		// however it *should* work if the new parent is under the same restriction
		RestrictableObjectDescriptor rod = new RestrictableObjectDescriptor();
		rod.setId(destinationId);
		rod.setType(RestrictableObjectType.ENTITY);
		arToDelete.getSubjectIds().add(rod);
		accessRequirementManager.updateAccessRequirement(adminUserInfo, arToDelete.getId().toString(), arToDelete);
		// now this should work!
		entityManager.updateEntity(userInfo, child, false, null);
	}
	
	@Test
	public void testAllInOne() throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException, ConflictingUpdateException{
		// Create a datset		
		Folder ds = createDataset();
		String id = entityManager.createEntity(adminUserInfo, ds, null);
		assertNotNull(id);
		toDelete.add(id);
		// Get another copy
		EntityWithAnnotations<Folder> ewa = entityManager.getEntityWithAnnotations(adminUserInfo, id, Folder.class);
		assertNotNull(ewa);
		assertNotNull(ewa.getAnnotations());
		assertNotNull(ewa.getEntity());
		Folder fetched = entityManager.getEntity(adminUserInfo, id, Folder.class);
		assertNotNull(fetched);
		assertEquals(ewa.getEntity(), fetched);
		System.out.println("Original: "+ds.toString());
		System.out.println("Fetched: "+fetched.toString());
		assertEquals(ds.getName(), fetched.getName());
		// Now get the Annotations
		Annotations annos = entityManager.getAnnotations(adminUserInfo, id);
		assertNotNull(annos);
		assertEquals(ewa.getAnnotations(), annos);
		annos.addAnnotation("someNewTestAnnotation", "someStringValue");
		// Update
		entityManager.updateAnnotations(adminUserInfo,id, annos);
		// Now make sure it changed
		annos = entityManager.getAnnotations(adminUserInfo, id);
		assertNotNull(annos);
		assertEquals("someStringValue", annos.getSingleValue("someNewTestAnnotation"));
		// Now update the dataset
		fetched = entityManager.getEntity(adminUserInfo, id, Folder.class);
		fetched.setName("myNewName");
		entityManager.updateEntity(adminUserInfo, fetched, false, null);
		fetched = entityManager.getEntity(adminUserInfo, id, Folder.class);
		assertNotNull(fetched);
		assertEquals("myNewName", fetched.getName());
	}
	
	@Test
	public void testAggregateUpdate() throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException{
		Folder ds = createDataset();
		String parentId = entityManager.createEntity(adminUserInfo, ds, null);
		assertNotNull(parentId);
		toDelete.add(parentId);
		List<Folder> layerList = new ArrayList<Folder>();
		int layers = 3;
		for(int i=0; i<layers; i++){
			Folder layer = createLayerForTest(i);
			layerList.add(layer);
		}
		List<String> childrenIds = entityManager.aggregateEntityUpdate(adminUserInfo, parentId, layerList);
		assertNotNull(childrenIds);
		assertEquals(layers, childrenIds.size());
		
		List<Folder> children = new LinkedList<Folder>();
		for(String childId: childrenIds){
			children.add(entityManager.getEntity(adminUserInfo, childId, Folder.class));
		}
		assertEquals(layers, children.size());
		Folder toUpdate = children.get(0);
		String udpatedId = toUpdate.getId();
		assertNotNull(udpatedId);
		toUpdate.setName("updatedName");
		// Do it again
		entityManager.aggregateEntityUpdate(adminUserInfo, parentId, children);
		children = new LinkedList<Folder>();
		for(String childId: childrenIds){
			children.add(entityManager.getEntity(adminUserInfo, childId, Folder.class));
		}
		assertNotNull(children);
		assertEquals(layers, children.size());
		// find the one with the updated name
		Folder updatedLayer = entityManager.getEntity(adminUserInfo, udpatedId, Folder.class);
		assertNotNull(updatedLayer);
		assertEquals("updatedName", updatedLayer.getName());
	}
	
	@Test
	public void testPLFM_1283() throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException{
		Folder study = new Folder();
		study.setName("test PLFM-1283");
		String id = entityManager.createEntity(adminUserInfo, study, null);
		assertNotNull(id);
		toDelete.add(id);
		try{
			entityManager.getEntityWithAnnotations(adminUserInfo, id, Project.class);
			fail("The requested entity type does not match the actaul entity type so this should fail.");
		}catch(IllegalArgumentException e){
			// This is expected.
			System.out.println(e.getMessage());
			assertTrue(e.getMessage().indexOf(id) > 0);
			assertTrue(e.getMessage().indexOf(Folder.class.getName()) > 0);
			assertTrue(e.getMessage().indexOf(Project.class.getName()) > 0);
		}
		
	}

	private Folder createLayerForTest(int i){
		Folder layer = new Folder();
		layer.setName("layerName"+i);
		layer.setDescription("layerDesc"+i);
		layer.setCreatedOn(new Date(1001));
		return layer;
	}

	@Test
	public void testCreateNewVersionOfEntityWithoutInheritingProvenance_PLFM_1869() throws Exception {
		Activity act = new Activity();
		String actId = activityManager.createActivity(adminUserInfo, act);
		assertNotNull(actId);
		activitiesToDelete.add(actId);
		
		FileEntity file = new FileEntity();
		ExternalFileHandle external1 = new ExternalFileHandle();
		external1.setExternalURL("http://www.google.com");
		external1.setFileName("file.txt");
		external1 = fileHandleManager.createExternalFileHandle(adminUserInfo, external1);
		fileHandlesToDelete.add(external1.getId());
		
		file.setDataFileHandleId(external1.getId());
		file.setName("testCreateNewVersionOfEntityWithoutProvenance");
		String id = entityManager.createEntity(adminUserInfo, file, actId);
		assertNotNull(id);
		toDelete.add(id);
		file = entityManager.getEntity(adminUserInfo, id, FileEntity.class);
		Activity v1Act = entityManager.getActivityForEntity(adminUserInfo, file.getId(), file.getVersionNumber());
		assertEquals(actId, v1Act.getId());
				
		ExternalFileHandle external2 = new ExternalFileHandle();
		external2.setExternalURL("http://www.yahoo.com");
		external2.setFileName("file.txt");
		external2 = fileHandleManager.createExternalFileHandle(adminUserInfo, external2);
		fileHandlesToDelete.add(external2.getId());
		
		file.setDataFileHandleId(external2.getId());
		file.setVersionLabel("2");		
		entityManager.updateEntity(adminUserInfo, file, false, null); // not necessarily a new version, like how the EntityController works
		FileEntity updated = entityManager.getEntity(adminUserInfo, file.getId(), FileEntity.class);
		
		try{			
			entityManager.getActivityForEntity(adminUserInfo, updated.getId(), updated.getVersionNumber());
			fail("activity should not have v1's activity id");
		} catch (NotFoundException e) {
			// expected
		}
	}
	
	
	/**
	 * Create a dataset with all of its fields filled in.
	 * @return
	 */
	public Folder createDataset(){
		// First we create a dataset with all fields filled in.
		Folder ds = new Folder();
		ds.setName("someName");
		ds.setDescription("someDesc");
		ds.setCreatedBy("magic");
		ds.setCreatedOn(new Date(1001));
		ds.setAnnotations("someAnnoUrl");
		ds.setEtag("110");
		ds.setId("12");
		ds.setUri("someUri");
		return ds;
	}


	@Test
	public void testTableViewCreateAndGet(){
		
		// create a project with a child
		Project project = new Project();
		project.setName("project");
		String pid = entityManager.createEntity(userInfo, project, null);
		toDelete.add(pid);
		
		EntityView fileView = new EntityView();
		fileView.setName("fileView");
		fileView.setColumnIds(Lists.newArrayList("1","2"));
		fileView.setScopeIds(Lists.newArrayList("syn4","5"));
		fileView.setParentId(pid);
		
		String fileViewId = entityManager.createEntity(userInfo, fileView, null);
		toDelete.add(fileViewId);
		
		EntityView viewGet = entityManager.getEntity(userInfo, fileViewId, EntityView.class);
		assertNotNull(viewGet);
		assertEquals(fileView.getColumnIds(), viewGet.getColumnIds());
		assertEquals(Lists.newArrayList("4","5"), viewGet.getScopeIds());
	}
}
