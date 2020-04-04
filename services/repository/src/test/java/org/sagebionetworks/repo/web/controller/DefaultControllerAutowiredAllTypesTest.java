package org.sagebionetworks.repo.web.controller;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.junit.Ignore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.manager.NodeManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.oauth.OIDCTokenHelper;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamMember;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.VersionableEntity;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.table.ColumnModelDAO;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.model.table.ViewType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is a an integration test for the default controller.
 * 
 */

public class DefaultControllerAutowiredAllTypesTest extends AbstractAutowiredControllerJunit5TestBase {

	// Used for cleanup
	@Autowired
	private EntityService entityController;
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private NodeManager nodeManager;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private FileHandleDao fileMetadataDao;
	
	@Autowired
	private ColumnModelDAO columnModelDao;
	
	@Autowired
	private TeamManager teamManager;

	@Autowired
	private IdGenerator idGenerator;

	@Autowired
	private OIDCTokenHelper oidcTokenHelper;

	private Long userId;
	private UserInfo testUser;
	private Team testTeam;

	private List<String> toDelete;
	S3FileHandle handleOne;
	ColumnModel columnModelOne;

	private String accessToken;

	@BeforeEach
	public void before() throws DatastoreException, NotFoundException {
		assertNotNull(entityController);
		toDelete = new ArrayList<String>();
		
		userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		
		accessToken = oidcTokenHelper.createTotalAccessToken(userId);

		// Map test objects to their urls
		// Make sure we have a valid user.
		testUser = userManager.getUserInfo(userId);
		UserInfo.validateUserInfo(testUser);
		Team team = new Team();
		team.setName("test team");
		try {
			testTeam = teamManager.create(testUser, team);
		} catch (NameConflictException e) {
			Map<Team, Collection<TeamMember>> allTeamsAndMembers = teamManager.listAllTeamsAndMembers();
			for (Team t : allTeamsAndMembers.keySet()) {
				if (t.getName().equals(team.getName())) {
					testTeam = t;
					break;
				}
			}
		}
		assertNotNull(testTeam);
		handleOne = TestUtils.createS3FileHandle(testUser.getId().toString(), idGenerator.generateNewId(IdType.FILE_IDS).toString());
		handleOne.setKey("EntityControllerTest.mainFileKey");
		handleOne = (S3FileHandle) fileMetadataDao.createFile(handleOne);
		// create a column model
		columnModelOne = new ColumnModel();
		columnModelOne.setName("one");
		columnModelOne.setColumnType(ColumnType.STRING);
		columnModelOne = columnModelDao.createColumnModel(columnModelOne);
	}

	@AfterEach
	public void after() throws Exception {
		if (entityController != null && toDelete != null) {
			UserInfo userInfo = userManager.getUserInfo(userId);
			for (String idToDelete : toDelete) {
				try {
					nodeManager.delete(userInfo, idToDelete);
				} catch (NotFoundException e) {
					// nothing to do here
				} catch (DatastoreException e) {
					// nothing to do here.
				}
			}
		}
		if(handleOne != null && handleOne.getId() != null){
			fileMetadataDao.delete(handleOne.getId());
		}
		if (testTeam != null) {
			teamManager.delete(testUser, testTeam.getId());
		}
	}

	@Test
	public void testAnonymousGet() throws Exception {
		Project project = new Project();
		project.setName("testAnonymousGet");
		project = servletTestHelper.createEntity(dispatchServlet, project, accessToken);
		String id = project.getId();
		assertNotNull(project);
		toDelete.add(id);
		// Grant this project public access
		AccessControlList acl = servletTestHelper.getEntityACL(dispatchServlet, id, accessToken);
		assertNotNull(acl);
		assertEquals(id, acl.getId());
		ResourceAccess ac = new ResourceAccess();
		UserGroup publicUserGroup = userGroupDAO.get(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId());
		assertNotNull(publicUserGroup);
		ac.setPrincipalId(Long.parseLong(publicUserGroup.getId()));
		ac.setAccessType(new HashSet<ACCESS_TYPE>());
		ac.getAccessType().add(ACCESS_TYPE.READ);
		acl.getResourceAccess().add(ac);
		servletTestHelper.updateEntityAcl(dispatchServlet, id, acl, accessToken);
		
		// Make sure the anonymous user can see this.
		String anonAccessToken=oidcTokenHelper.createAnonymousAccessToken();
		Project clone = servletTestHelper.getEntity(dispatchServlet, Project.class, project.getId(),
				anonAccessToken);
		assertNotNull(clone);
	}
	
	/**
	 * This is a test helper method that will create at least on of each type of entity.
	 * @return
	 * @throws IOException 
	 * @throws ServletException 
	 * @throws InvalidModelException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws UnauthorizedException 
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	private List<Entity> createEntitesOfEachType(int countPerType) throws Exception {
		// For now put each object in a project so their parent id is not null;
		// Create a project
		Project project = new Project();
		project.setName("createAtLeastOneOfEachType");
		project = servletTestHelper.createEntity(dispatchServlet, project, accessToken);
		assertNotNull(project);
		toDelete.add(project.getId());
		// Now get the path of the layer
		List<EntityHeader> path = entityController.getEntityPath(testUser, project.getId());
		
		// This is the list of entities that will be created.
		List<Entity> newChildren = new ArrayList<Entity>();
		// Create one of each type
		EntityType[] types = EntityType.values();
		for(int i=0; i<countPerType; i++){
			int index = i;
			for(EntityType type: types){
				String name = type.name()+index;
				// use the correct parent type.
				String parentId = findCompatableParentId(path, type);
				Entity object = ObjectTypeFactory.createObjectForTest(name, type, parentId);
				if(object instanceof FileEntity){
					FileEntity file = (FileEntity) object;
					file.setDataFileHandleId(handleOne.getId());
				}
				if(object instanceof TableEntity){
					TableEntity table = (TableEntity) object;
					List<String> idList = new LinkedList<String>();
					idList.add(columnModelOne.getId());
					table.setColumnIds(idList);
				}
				if(object instanceof EntityView){
					EntityView view = (EntityView) object;
					List<String> idList = new LinkedList<String>();
					idList.add(columnModelOne.getId());
					view.setColumnIds(idList);
					view.setType(ViewType.file);
				}
				if(object instanceof DockerRepository){
					DockerRepository dockerRepository = (DockerRepository)object;
					dockerRepository.setIsManaged(false);
					dockerRepository.setRepositoryName("foo/bar");
				}
				Entity clone = servletTestHelper.createEntity(dispatchServlet, object, accessToken);
				assertNotNull(clone);
				assertNotNull(clone.getId());
				assertNotNull(clone.getEtag());
				
				// Mark entities for deletion after the current test completes
				toDelete.add(clone.getId());

				// Add this to the list of entities created
				newChildren.add(clone);
				index++;
			}
		}
		return newChildren;
	}
	
	/**
	 * Find the first compatible parent id for a given object type. 
	 * @param path
	 * @param type
	 * @return
	 */
	private String findCompatableParentId(List<EntityHeader> path, EntityType type){
		// First try null
		if(EntityTypeUtils.isValidParentType(type, null)) return null;
		// Try each entry in the list
		for(EntityHeader header: path) {
			if ("syn4489".equals(header.getId()) ) continue;// the root node has misleading type 'Folder' and we've already checked the null parent case above
			EntityType parentType = EntityTypeUtils.getEntityTypeForClassName(header.getType());
			if(EntityTypeUtils.isValidParentType(type, parentType)) { 
				return header.getId();
			}
		}
		// No match found
		throw new IllegalArgumentException("Cannot find a compatible parent for "+type);
	}

	@Test
	public void testCreateAllTypes() throws Exception {
		// All we need to do is create at least one of each type
		List<Entity> created = createEntitesOfEachType(1);
		assertNotNull(created);
		assertTrue(created.size() >= EntityType.values().length);
	}



	@Test
	public void testGetById() throws Exception {
		// First create one of each type
		List<Entity> created = createEntitesOfEachType(1);
		assertNotNull(created);
		assertTrue(created.size() >= EntityType.values().length);
		
		// Now make sure we can get each type
		for(Entity entity: created){
			// Can we get it?
			Entity fromGet = servletTestHelper.getEntity(dispatchServlet, entity.getClass(), entity.getId(), accessToken);
			assertNotNull(fromGet);
			// Should match the clone
			assertEquals(entity, fromGet);
		}
	}

	@Test
	public void testDelete() throws Exception {
		// First create one of each type
		List<Entity> created = createEntitesOfEachType(1);
		assertNotNull(created);
		assertTrue(created.size() >= EntityType.values().length);
		
		// Now delete each one
		for(Entity entity: created){
			servletTestHelper.deleteEntity(dispatchServlet, entity.getClass(), entity.getId(), userId);
			// This should throw an exception
			try {
				servletTestHelper.getEntity(dispatchServlet, entity.getClass(), entity.getId(), accessToken);
				fail("Entity ID " + entity.getId() + " should no longer exist. Expected an exception.");
			} catch (Exception e) {
				// expected
			}
		}
	}
	
	@Test
	public void testUpdateEntity() throws Exception{
		// First create one of each type
		List<Entity> created = createEntitesOfEachType(1);
		assertNotNull(created);
		assertTrue(created.size() >= EntityType.values().length);
		
		// Now update each
		int counter=0;
		for(Entity entity: created) {
			// Now change the name
			String newName ="my new name"+counter;
			entity.setName(newName);
			Entity updated = servletTestHelper.updateEntity(dispatchServlet, entity, accessToken);
			assertNotNull(updated);
			// Updating an entity should not create a new version
			if(updated instanceof VersionableEntity){
				VersionableEntity updatedVersionableEntity = (VersionableEntity) updated;
				assertEquals(new Long(1), updatedVersionableEntity.getVersionNumber());
			}
			// It should have a new etag
			assertNotNull(updated.getEtag());
			assertFalse(updated.getEtag().equals(entity.getEtag()));
			// Now get the object
			Entity fromGet = servletTestHelper.getEntity(dispatchServlet, entity.getClass(), entity.getId(), accessToken);
			assertEquals(updated, fromGet);
			assertEquals(newName, fromGet.getName());
			counter++;
		}
	}
	
	@Ignore
	@Test
	public void testGetPath() throws Exception{
		// First create one of each type
		List<Entity> created = createEntitesOfEachType(1);
		assertNotNull(created);
		assertTrue(created.size() >= EntityType.values().length);
		
		// Now update each
		for(Entity entity: created){
			// Make sure we can get the annotations for this entity.
			EntityPath entityPath = servletTestHelper.getEntityPath(dispatchServlet, entity.getId(), accessToken);
			List<EntityHeader> path = entityPath.getPath();
			assertNotNull(path);
			assertTrue(path.size() > 0);
			EntityType type = EntityTypeUtils.getEntityTypeForClass(entity.getClass());
			// The last element should match this entity
			EntityHeader myData = path.get(path.size()-1);
			assertNotNull(myData);
			assertEquals(entity.getId(), myData.getId());
			assertEquals(entity.getName(), myData.getName());
			assertEquals(EntityTypeUtils.getEntityTypeClassName(type), myData.getType());
		}
	}
	
	@Test
	public void testGetAnnotations() throws Exception{
		// First create one of each type
		List<Entity> created = createEntitesOfEachType(1);
		assertNotNull(created);
		assertTrue(created.size() >= EntityType.values().length);
		
		// Now update each
		for(Entity entity: created){
			// Make sure we can get the annotations for this entity.
			Annotations annos = servletTestHelper.getEntityAnnotations(dispatchServlet, entity.getClass(), entity.getId(), accessToken);
			assertNotNull(annos);
			// Annotations use the same etag as the entity
			assertEquals(entity.getEtag(), annos.getEtag());
			// Annotations use the same id as the entity
			assertEquals(entity.getId(), annos.getId());
		}
	}
	
	@Test
	public void testUpdateAnnotations() throws Exception {
		// First create one of each type
		List<Entity> created = createEntitesOfEachType(1);
		assertNotNull(created);
		assertTrue(created.size() >= EntityType.values().length);
		
		// Now update each
		for(Entity entity: created){
			// Make sure we can get the annotations for this entity.
			Annotations annos = servletTestHelper.getEntityAnnotations(dispatchServlet, entity.getClass(), entity.getId(), accessToken);
			assertNotNull(annos);
			assertNotNull(annos.getEtag());
			annos.addAnnotation("someStringKey", "one");
			// Do the update
			Annotations updatedAnnos = servletTestHelper.updateEntityAnnotations(dispatchServlet, entity.getClass(), annos, accessToken);
			assertNotNull(updatedAnnos);
			assertNotNull(updatedAnnos.getEtag());
			assertFalse(updatedAnnos.getEtag().equals(annos.getEtag()));
			assertEquals("one", updatedAnnos.getSingleValue("someStringKey"));
		}

	}
	
	@Test
	public void testGetEntityAcl() throws Exception {
		// First create one of each type
		List<Entity> created = createEntitesOfEachType(1);
		assertNotNull(created);
		assertTrue(created.size() >= EntityType.values().length);
		
		// Now update each
		for(Entity entity: created){
			AccessControlList acl = null;
			try{
				acl = servletTestHelper.getEntityACL(dispatchServlet, entity.getId(), accessToken);
			}catch(ACLInheritanceException e){
				acl = servletTestHelper.getEntityACL(dispatchServlet, e.getBenefactorId(), accessToken);
			}
			assertNotNull(acl);
		}
	}
	
	@Test
	public void testUpdateEntityAcl() throws Exception {
		// First create one of each type
		List<Entity> created = createEntitesOfEachType(1);
		assertNotNull(created);
		assertTrue(created.size() >= EntityType.values().length);
		
		// Now update each
		for(Entity entity: created){
			AccessControlList acl = null;
			try{
				acl = servletTestHelper.getEntityACL(dispatchServlet, entity.getId(), accessToken);
			}catch(ACLInheritanceException e){
				acl = servletTestHelper.getEntityACL(dispatchServlet, e.getBenefactorId(), accessToken);
			}
			assertNotNull(acl);
			servletTestHelper.updateEntityAcl(dispatchServlet, acl.getId(), acl, accessToken);
		}

	}
	
	@Ignore
	@Test
	public void testCreateEntityAcl() throws Exception {
		// First create one of each type
		List<Entity> created = createEntitesOfEachType(1);
		assertNotNull(created);
		assertTrue(created.size() >= EntityType.values().length);
		
		// Now update each
		for(Entity entity: created){
			AccessControlList acl = null;
			try{
				acl = servletTestHelper.getEntityACL(dispatchServlet, entity.getId(), accessToken);
			}catch(ACLInheritanceException e){
				// occurs when the child inherits its permissions from a benefactor
				acl = servletTestHelper.getEntityACL(dispatchServlet, e.getBenefactorId(), accessToken);
			}
			assertNotNull(acl);
			// Get the full path of this entity.
			EntityPath entityPath = servletTestHelper.getEntityPath(dispatchServlet, entity.getId(), accessToken);
			List<EntityHeader> path = entityPath.getPath();
			assertNotNull(path);
			assertTrue(path.size() > 0);
			// The ACL should match the root of the node
			EntityHeader rootHeader = path.get(1);
			// the returned ACL should refer to the parent
			assertEquals(rootHeader.getId(), acl.getId());
			
			// We cannot add an ACL to a node that already has one
			if(acl.getId().equals(entity.getId())){
				continue;
			}
			
			// now switch to child
			acl.setId(null);
			// (Is this OK, or do we have to make new ResourceAccess objects inside?)
			// now POST to /dataset/{id}/acl with this acl as the body
			AccessControlList acl2 = servletTestHelper.createEntityACL(dispatchServlet, entity.getId(), acl, accessToken);
			// now retrieve the acl for the child. should get its own back
			AccessControlList acl3 = servletTestHelper.getEntityACL(dispatchServlet, entity.getId(), accessToken);
			assertEquals(entity.getId(), acl3.getId());
			
			
			// now delete the ACL (restore inheritance)
			servletTestHelper.deleteEntityACL(dispatchServlet, entity.getId(), userId);
			// try retrieving the ACL for the child
			
			// should get the parent's ACL
			AccessControlList acl4 = null;
			try{
				acl4 = servletTestHelper.getEntityACL(dispatchServlet, entity.getId(), accessToken);
			}catch(ACLInheritanceException e){
				acl4 = servletTestHelper.getEntityACL(dispatchServlet, e.getBenefactorId(), accessToken);
			}
			assertNotNull(acl4);
			// the returned ACL should refer to the parent
			assertEquals(rootHeader.getId(), acl4.getId());
		}
	}
	
	@Test
	public void testCreateNewVersion() throws Exception {
		// First create one of each type
		List<Entity> created = createEntitesOfEachType(1);
		assertNotNull(created);
		assertTrue(created.size() >= EntityType.values().length);
		// Now update each
		for(Entity entity: created){
			// Cannot directly create version of tables or views
			if(entity instanceof TableEntity || entity instanceof EntityView) {
				continue;
			}
			// We can only create new versions for versionable entities.
			if(entity instanceof VersionableEntity){
				VersionableEntity versionableEntity = (VersionableEntity) entity;
				// Before we start, make sure there is only one version so far
				assertEquals(new Long(1), versionableEntity.getVersionNumber());
				assertNotNull(versionableEntity.getVersionLabel());
				// Now create a new version
				// We must give it a new version label or it will fail
				versionableEntity.setVersionLabel("1.1.99");
				versionableEntity.setVersionComment("Testing the DefaultController.createNewVersion()");
				VersionableEntity newVersion = servletTestHelper.createNewVersion(dispatchServlet, versionableEntity, accessToken);
				assertNotNull(newVersion);
				// Make sure we have a new version number.
				assertEquals(new Long(2), newVersion.getVersionNumber());
				assertEquals(versionableEntity.getVersionLabel(), newVersion.getVersionLabel());
				assertEquals(versionableEntity.getVersionComment(), newVersion.getVersionComment());
			}
		}
	}
	
	@Test
	public void testGetEntityForVersion() throws Exception {
		// First create one of each type
		List<Entity> created = createEntitesOfEachType(1);
		assertNotNull(created);
		assertTrue(created.size() >= EntityType.values().length);
		// Now update each
		for(Entity entity: created){
			// Cannot directly create version of tables or views
			if(entity instanceof TableEntity || entity instanceof EntityView) {
				continue;
			}
			// We can only create new versions for versionable entities.
			if(entity instanceof VersionableEntity){
				VersionableEntity versionableEntity = (VersionableEntity) entity;

				// Now create a new version
				// We must give it a new version label or it will fail
				versionableEntity.setVersionLabel("1.1.99");
				versionableEntity.setVersionComment("Testing the DefaultController.testGetVersion()");
				VersionableEntity newVersion = servletTestHelper.createNewVersion(dispatchServlet, versionableEntity, accessToken);
				assertNotNull(newVersion);
				// Make sure we have a new version number.
				assertEquals(new Long(2), newVersion.getVersionNumber());
				assertEquals(versionableEntity.getVersionLabel(), newVersion.getVersionLabel());
				assertEquals(versionableEntity.getVersionComment(), newVersion.getVersionComment());
				
				// Get the first version
				VersionableEntity v1 = servletTestHelper.getEntityForVersion(dispatchServlet, versionableEntity.getClass(),
						versionableEntity.getId(), new Long(1), accessToken);
				assertNotNull(v1);
				assertEquals(new Long(1), v1.getVersionNumber());
				// now get the second version
				VersionableEntity v2 = servletTestHelper.getEntityForVersion(dispatchServlet, versionableEntity.getClass(),
						versionableEntity.getId(), new Long(2), accessToken);
				assertNotNull(v2);
				assertEquals(new Long(2), v2.getVersionNumber());
			}
		}
	}
	
	@Test
	public void testGetAllVersions() throws Exception {
		// First create one of each type
		List<Entity> created = createEntitesOfEachType(1);
		assertNotNull(created);
		assertTrue(created.size() >= EntityType.values().length);
		// Now update each
		int numberVersion = 4;
		for(Entity entity: created){
			// We can only create new versions for versionable entities.
			if(entity instanceof VersionableEntity){
				// Cannot directly create version of tables or views
				if(entity instanceof TableEntity || entity instanceof EntityView) {
					continue;
				}
				VersionableEntity versionableEntity = (VersionableEntity) entity;
				// Create multiple versions for each.
				for(int i=0; i<numberVersion; i++){
					// Create a comment and label for each
					versionableEntity = servletTestHelper.getEntity(dispatchServlet, versionableEntity.getClass(), entity.getId(), accessToken);
					versionableEntity.setVersionLabel("1.1."+i);
					versionableEntity.setVersionComment("Comment: "+i);
					servletTestHelper.createNewVersion(dispatchServlet, versionableEntity, accessToken);
				}
				long currentVersion = numberVersion+1;
				long previousVersion = currentVersion-1;
				long firstVersion = 1;
				// Now get all entities
				PaginatedResults<VersionInfo> results = servletTestHelper.getAllVersionsOfEntity(dispatchServlet, entity.getId(), 0, 100,
						userId);
				assertNotNull(results);
				assertEquals(currentVersion, results.getTotalNumberOfResults());
				assertNotNull(results.getResults());
				assertEquals(currentVersion, results.getResults().size());
				// The first should be the current version
				assertNotNull(results.getResults().get(0));
				assertEquals(new Long(currentVersion), results.getResults().get(0).getVersionNumber());
				// The last should be the first version
				assertEquals(new Long(firstVersion), results.getResults().get(results.getResults().size()-1).getVersionNumber());
				
				// Query again but this time get a sub-set
				results = servletTestHelper.getAllVersionsOfEntity(dispatchServlet, entity.getId(), 1, 3, userId);
				assertNotNull(results);
				assertEquals(currentVersion, results.getTotalNumberOfResults());
				assertNotNull(results.getResults());
				assertEquals(3, results.getResults().size());
				// The first should be the previous version
				assertNotNull(results.getResults().get(0));
				assertEquals(new Long(previousVersion), results.getResults().get(0).getVersionNumber());
				// The last should be the previous version - 2;
				assertEquals(new Long(previousVersion-2), results.getResults().get(results.getResults().size()-1).getVersionNumber());
			}
		}
	}
	
	@Test
	public void testGetEntityAnnotationsForVersion() throws Exception {
		// First create one of each type
		List<Entity> created = createEntitesOfEachType(1);
		assertNotNull(created);
		assertTrue(created.size() >= EntityType.values().length);
		// Now update each
		for(Entity entity: created){
			// Cannot directly create version of tables or views
			if(entity instanceof TableEntity || entity instanceof EntityView) {
				continue;
			}
			// We can only create new versions for versionable entities.
			if(entity instanceof VersionableEntity){
				VersionableEntity versionableEntity = (VersionableEntity) entity;
				
				// Before we create a new version make sure the current version has some annotations
				Annotations v1Annos = servletTestHelper.getEntityAnnotations(dispatchServlet, versionableEntity.getClass(), entity.getId(),
						accessToken);
				assertNotNull(v1Annos);
				String v1Value = "I am on the first version, whooo hooo!...";
				v1Annos.addAnnotation("stringKey", v1Value);
				v1Annos = servletTestHelper.updateEntityAnnotations(dispatchServlet, versionableEntity.getClass(), v1Annos, accessToken);

				// Now create a new version
				// We must give it a new version label or it will fail
				versionableEntity = servletTestHelper.getEntity(dispatchServlet, versionableEntity.getClass(), entity.getId(), accessToken);
				versionableEntity.setVersionLabel("1.1.80");
				versionableEntity.setVersionComment("Testing the DefaultController.EntityAnnotationsForVersion()");
				VersionableEntity newVersion = servletTestHelper.createNewVersion(dispatchServlet, versionableEntity, accessToken);
				assertNotNull(newVersion);
				
				// Make sure the new version has the annotations
				Annotations v2Annos = servletTestHelper.getEntityAnnotations(dispatchServlet, versionableEntity.getClass(), entity.getId(),
						accessToken);
				assertNotNull(v2Annos);
				assertEquals(v1Value, v2Annos.getSingleValue("stringKey"));
				// Now update the v2 annotations
				v2Annos.getStringAnnotations().clear();
				String v2Value = "I am on the second version, booo hooo!...";
				v2Annos.addAnnotation("stringKey", v2Value);
				v2Annos = servletTestHelper.updateEntityAnnotations(dispatchServlet, versionableEntity.getClass(), v2Annos, accessToken);
				
				// Now make sure we can get both v1 and v2 annotations and each has the correct values
				//v1
				v1Annos = servletTestHelper.getEntityAnnotationsForVersion(dispatchServlet, versionableEntity.getClass(), entity.getId(), 1l,
						accessToken);
				assertNotNull(v1Annos);
				assertEquals(v1Value, v1Annos.getSingleValue("stringKey"));
				//v2
				v2Annos = servletTestHelper.getEntityAnnotationsForVersion(dispatchServlet, versionableEntity.getClass(), entity.getId(), 2l,
						accessToken);
				assertNotNull(v2Annos);
				assertEquals(v2Value, v2Annos.getSingleValue("stringKey"));
			}
		}
	}
	
	@Test
	public void testDeleteVersion() throws Exception {
		// First create one of each type
		List<Entity> created = createEntitesOfEachType(1);
		assertNotNull(created);
		assertTrue(created.size() >= EntityType.values().length);
		// Now update each
		for(Entity entity: created){
			// Cannot directly create version of tables or views
			if(entity instanceof TableEntity || entity instanceof EntityView) {
				continue;
			}
			// We can only create new versions for versionable entities.
			if(entity instanceof VersionableEntity){
				VersionableEntity versionableEntity = (VersionableEntity) entity;
				
				// Now create a new version
				// We must give it a new version label or it will fail
				versionableEntity = servletTestHelper.getEntity(dispatchServlet, versionableEntity.getClass(), entity.getId(), accessToken);
				versionableEntity.setVersionLabel("1.1.80");
				versionableEntity.setVersionComment("Testing the DefaultController.testDeleteVersion()");
				VersionableEntity newVersion = servletTestHelper.createNewVersion(dispatchServlet, versionableEntity, accessToken);
				assertNotNull(newVersion);
				
				// There should be two versions
				PaginatedResults<VersionInfo> paging = servletTestHelper.getAllVersionsOfEntity(dispatchServlet, entity.getId(), 1, 100,
						userId);
				assertNotNull(paging);
				assertEquals(2, paging.getTotalNumberOfResults());
				
				// Now delete the new version
				servletTestHelper.deleteEntityVersion(dispatchServlet, versionableEntity.getClass(), entity.getId(), 2l, userId);
				// We should be down to one version
				paging = servletTestHelper.getAllVersionsOfEntity(dispatchServlet, entity.getId(), 1, 100, userId);
				assertNotNull(paging);
				assertEquals(1, paging.getTotalNumberOfResults());
				
			}
		}
	}
	
	@Test
	public void testGetUserEntityPermissions() throws Exception {
		// First create one of each type
		List<Entity> created = createEntitesOfEachType(1);
		assertNotNull(created);
		assertTrue(created.size() >= EntityType.values().length);
		
		// Now update each
		for(Entity entity: created){
			// Make sure we can get the annotations for this entity.
			UserEntityPermissions uep = servletTestHelper.getUserEntityPermissions(dispatchServlet, entity.getId(), accessToken);
			assertNotNull(uep);
			assertEquals(true, uep.getCanDownload());
			assertEquals(true, uep.getCanUpload());
			assertEquals(true, uep.getCanEdit());
			assertEquals(true, uep.getCanChangePermissions());
			assertEquals(true, uep.getCanChangeSettings());
			assertEquals(true, uep.getCanDelete());
			assertEquals(true, uep.getCanView());
			assertEquals(true, uep.getCanAddChild());
		}
	}
	
}
