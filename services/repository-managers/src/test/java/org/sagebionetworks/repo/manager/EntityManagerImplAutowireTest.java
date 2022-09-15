package org.sagebionetworks.repo.manager;

import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.dataaccess.AccessRequirementManager;
import org.sagebionetworks.repo.manager.dataaccess.AccessRequirementManagerImpl;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RestrictableObjectDescriptor;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Utils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValue;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsValueType;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.schema.DerivedAnnotationDao;
import org.sagebionetworks.repo.model.entity.BindSchemaToEntityRequest;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.schema.CreateOrganizationRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaRequest;
import org.sagebionetworks.repo.model.schema.CreateSchemaResponse;
import org.sagebionetworks.repo.model.schema.JsonSchema;
import org.sagebionetworks.repo.model.schema.Organization;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.SubmissionView;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.FORMAT;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JsonDateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(SpringExtension.class)
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

	@Autowired
	private DerivedAnnotationDao derivedAnnotationsDao;
	@Autowired
	JsonSchemaManager jsonSchemaManager;

	private List<String> toDelete;
	private List<String> activitiesToDelete;
	private List<String> fileHandlesToDelete;

	private UserInfo adminUserInfo;
	private UserInfo userInfo;
	private Long userId;

	private AccessRequirement arToDelete;

	private Organization organization = null;

	private JsonSchema schema = null;

	@BeforeEach
	public void before() throws Exception{
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		NewUser nu = new NewUser();
		nu.setUserName("test");
		nu.setEmail("just.a.test@sagebase.org");
		userId = userManager.createUser(nu);
		userInfo = userManager.getUserInfo(userId);
		userInfo.getGroups().add(BOOTSTRAP_PRINCIPAL.CERTIFIED_USERS.getPrincipalId());
		userInfo.setAcceptsTermsOfUse(true);

		toDelete = new ArrayList<String>();
		activitiesToDelete = new ArrayList<String>();
		fileHandlesToDelete = new ArrayList<String>();
		derivedAnnotationsDao.clearAll();
	}

	@AfterEach
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

		jsonSchemaManager.truncateAll();

		derivedAnnotationsDao.clearAll();
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
		Folder entity = entityManager.getEntity(adminUserInfo, id, Folder.class);
		assertNotNull(entity);
		Folder fetched = entityManager.getEntity(adminUserInfo, id, Folder.class);
		assertNotNull(fetched);
		assertEquals(entity, fetched);
		System.out.println("Original: "+ds.toString());
		System.out.println("Fetched: "+fetched.toString());
		assertEquals(ds.getName(), fetched.getName());
		// Now get the Annotations
		Annotations annos = entityManager.getAnnotations(adminUserInfo, id);
		assertNotNull(annos);
		AnnotationsV2TestUtils.putAnnotations(annos, "someNewTestAnnotation", "someStringValue", AnnotationsValueType.STRING);
		// Update
		entityManager.updateAnnotations(adminUserInfo,id, annos);
		// Now make sure it changed
		annos = entityManager.getAnnotations(adminUserInfo, id);
		assertNotNull(annos);
		AnnotationsValue annoValue = annos.getAnnotations().get("someNewTestAnnotation");
		assertEquals("someStringValue", AnnotationsV2Utils.getSingleValue(annoValue));
		assertEquals(AnnotationsValueType.STRING, annoValue.getType());
		// Now update the dataset
		fetched = entityManager.getEntity(adminUserInfo, id, Folder.class);
		fetched.setName("myNewName");
		entityManager.updateEntity(adminUserInfo, fetched, false, null);
		fetched = entityManager.getEntity(adminUserInfo, id, Folder.class);
		assertNotNull(fetched);
		assertEquals("myNewName", fetched.getName());
	}

	@Test
	public void testPLFM_1283() throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException{
		Folder study = new Folder();
		study.setName("test PLFM-1283");
		String id = entityManager.createEntity(adminUserInfo, study, null);
		assertNotNull(id);
		toDelete.add(id);
		try{
			entityManager.getEntity(adminUserInfo, id, Project.class);
			fail("The requested entity type does not match the actaul entity type so this should fail.");
		}catch(IllegalArgumentException e){
			// This is expected.
			System.out.println(e.getMessage());
			assertTrue(e.getMessage().indexOf(id) > 0);
			assertTrue(e.getMessage().indexOf(Folder.class.getName()) > 0);
			assertTrue(e.getMessage().indexOf(Project.class.getName()) > 0);
		}

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
		ds.setEtag("110");
		ds.setId("12");
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

	/**
	 * Test added for PLFM-5188
	 *
	 */
	@Test
	public void testCreateWithID() {
		String maxId = KeyFactory.keyToString(Long.MAX_VALUE);
		Project project = new Project();
		project.setName(null);
		project.setId(maxId);
		String pid = entityManager.createEntity(userInfo, project, null);
		toDelete.add(pid);
		// the provided ID must not be used.
		assertFalse(maxId.equals(pid));
		project = entityManager.getEntity(userInfo, pid, Project.class);
		// the name should match the newly issued ID.
		assertEquals(pid, project.getName());
	}

	/**
	 * Test for PLFM-5702
	 */
	@Test
	public void testUpdateEntityNewVersionTable() {
		// update a table with newVersion=true;
		TableEntity table = new TableEntity();
		table.setName("Table");
		String id = entityManager.createEntity(userInfo, table, null);
		table = entityManager.getEntity(adminUserInfo, id, TableEntity.class);
		toDelete.add(id);
		boolean newVersion = true;
		String activityId = null;
		// call under test
		boolean wasNewVersionCreated = entityManager.updateEntity(adminUserInfo, table, newVersion, activityId);
		// should not create a new version.
		assertFalse(wasNewVersionCreated);
	}

	/**
	 * Test for PLFM-5702
	 */
	@Test
	public void testUpdateEntityNewVersionEntityView() {
		// update a table with newVersion=true;
		EntityView view = new EntityView();
		view.setName("Table");
		String id = entityManager.createEntity(userInfo, view, null);
		view = entityManager.getEntity(adminUserInfo, id, EntityView.class);
		toDelete.add(id);
		boolean newVersion = true;
		String activityId = null;
		// call under test
		boolean wasNewVersionCreated = entityManager.updateEntity(adminUserInfo, view, newVersion, activityId);
		// should not create a new version.
		assertFalse(wasNewVersionCreated);
	}

	/**
	 * Test for PLFM-6362
	 */
	@Test
	public void testUpdateEntityNewVersionSubmissionView() {
		// update a table with newVersion=true;
		SubmissionView view = new SubmissionView();
		view.setName("Table");
		String id = entityManager.createEntity(userInfo, view, null);
		view = entityManager.getEntity(adminUserInfo, id, SubmissionView.class);
		toDelete.add(id);
		boolean newVersion = true;
		String activityId = null;
		// call under test
		boolean wasNewVersionCreated = entityManager.updateEntity(adminUserInfo, view, newVersion, activityId);
		// should not create a new version.
		assertFalse(wasNewVersionCreated);
	}

	@Test
	public void testGetEntityJson() {
		Project project = new Project();
		project.setName("some kind of test project");
		String pid = entityManager.createEntity(userInfo, project, null);
		toDelete.add(pid);
		project = entityManager.getEntity(userInfo, pid, Project.class);
		Annotations annotations = entityManager.getAnnotations(userInfo, pid);
		AnnotationsV2TestUtils.putAnnotations(annotations, "singleString", "one", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(annotations, "listOfDoubles", Arrays.asList("1.2", "2.3"),
				AnnotationsValueType.DOUBLE);
		AnnotationsV2TestUtils.putAnnotations(annotations, "parentId", "overrideMe!", AnnotationsValueType.STRING);
		entityManager.updateAnnotations(userInfo, pid, annotations);
		project = entityManager.getEntity(userInfo, pid, Project.class);

		// Call under test
		JSONObject result = entityManager.getEntityJson(pid, false);
		assertNotNull(result);
		assertEquals(project.getId(), result.getString("id"));
		assertEquals(project.getEtag(), result.getString("etag"));
		assertEquals(project.getName(), result.getString("name"));
		assertEquals(JsonDateUtils.convertDateToString(FORMAT.DATE_TIME, project.getCreatedOn()),
				result.getString("createdOn"));
		assertEquals(JsonDateUtils.convertDateToString(FORMAT.DATE_TIME, project.getModifiedOn()),
				result.getString("modifiedOn"));
		assertEquals(project.getModifiedBy(), result.getString("modifiedBy"));
		assertEquals(project.getCreatedBy(), result.getString("createdBy"));
		assertEquals(Project.class.getName(), result.getString("concreteType"));
		// the 'parentId' annotation value should not override the real parentId.
		assertEquals(project.getParentId(), result.getString("parentId"));

		// the annotations:
		assertEquals("one", result.getJSONArray("singleString").getString(0));
		JSONArray doubleArray = result.getJSONArray("listOfDoubles");
		assertNotNull(doubleArray);
		assertEquals(2, doubleArray.length());
		assertEquals(new Double(1.2), doubleArray.getDouble(0));
		assertEquals(new Double(2.3), doubleArray.getDouble(1));
	}


	@Test
	public void testGetEntityJsonForVersion() {
		FileEntity file = new FileEntity();
		file.setName("some kind of test project");
		String pid = entityManager.createEntity(userInfo, file, null);
		toDelete.add(pid);
		file = entityManager.getEntity(userInfo, pid, FileEntity.class);
		Annotations annotations = entityManager.getAnnotations(userInfo, pid);
		AnnotationsV2TestUtils.putAnnotations(annotations, "testKey", "overrideMe!", AnnotationsValueType.STRING);
		entityManager.updateAnnotations(userInfo, pid, annotations);
		file = entityManager.getEntity(userInfo, pid, FileEntity.class);

		file.setVersionLabel("Updated version");

		entityManager.updateEntity(userInfo, file, true, null);

		annotations = entityManager.getAnnotations(userInfo, pid);
		AnnotationsV2TestUtils.putAnnotations(annotations, "testKey", "overriden!", AnnotationsValueType.STRING);
		entityManager.updateAnnotations(userInfo, pid, annotations);

		// Call under test
		JSONObject result = entityManager.getEntityJsonForVersion(userInfo, pid, 1L);

		assertEquals("overrideMe!", result.getJSONArray("testKey").get(0));

		result = entityManager.getEntityJsonForVersion(userInfo, pid, 2L);

		assertEquals("overriden!", result.getJSONArray("testKey").get(0));

	}

	@Test
	public void testGetEntityJsonWithDerivedAnnotations() {
		Project project = new Project();
		project.setName("some kind of test project");
		String pid = entityManager.createEntity(userInfo, project, null);
		toDelete.add(pid);
		project = entityManager.getEntity(userInfo, pid, Project.class);

		Annotations annotations = entityManager.getAnnotations(userInfo, pid);

		AnnotationsV2TestUtils.putAnnotations(annotations, "a", Arrays.asList("1"), AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(annotations, "b", Arrays.asList("1.2"), AnnotationsValueType.DOUBLE);

		entityManager.updateAnnotations(userInfo, pid, annotations);
		project = entityManager.getEntity(userInfo, pid, Project.class);

		Annotations derivedAnnotations = AnnotationsV2Utils.emptyAnnotations();

		AnnotationsV2TestUtils.putAnnotations(derivedAnnotations, "a", "should not override", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(derivedAnnotations, "c", "value", AnnotationsValueType.STRING);

		derivedAnnotationsDao.saveDerivedAnnotations(pid, derivedAnnotations);

		// Call under test
		JSONObject result = entityManager.getEntityJson(pid, true);

		assertNotNull(result);
		assertEquals(project.getId(), result.getString("id"));
		assertEquals(project.getEtag(), result.getString("etag"));
		assertEquals(project.getName(), result.getString("name"));
		assertEquals(JsonDateUtils.convertDateToString(FORMAT.DATE_TIME, project.getCreatedOn()), result.getString("createdOn"));
		assertEquals(JsonDateUtils.convertDateToString(FORMAT.DATE_TIME, project.getModifiedOn()), result.getString("modifiedOn"));
		assertEquals(project.getModifiedBy(), result.getString("modifiedBy"));
		assertEquals(project.getCreatedBy(), result.getString("createdBy"));
		assertEquals(Project.class.getName(), result.getString("concreteType"));
		assertEquals(project.getParentId(), result.getString("parentId"));

		// the annotations:
		assertEquals(1, result.getJSONArray("a").length());
		assertEquals("1", result.getJSONArray("a").getString(0));
		assertEquals(1, result.getJSONArray("b").length());
		assertEquals(1.2, result.getJSONArray("b").getDouble(0));
		assertEquals(1, result.getJSONArray("c").length());
		assertEquals("value", result.getJSONArray("c").getString(0));
	}

	@Test
	public void testUpdateEntityJson() {
		Project project = new Project();
		project.setName("some kind of test project");
		String pid = entityManager.createEntity(userInfo, project, null);
		toDelete.add(pid);
		project = entityManager.getEntity(userInfo, pid, Project.class);
		Annotations annotations = entityManager.getAnnotations(userInfo, pid);
		AnnotationsV2TestUtils.putAnnotations(annotations, "singleString", "one", AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(annotations, "listOfDoubles", Arrays.asList("1.2", "2.3"),
				AnnotationsValueType.DOUBLE);
		AnnotationsV2TestUtils.putAnnotations(annotations, "parentId", "overrideMe!", AnnotationsValueType.STRING);
		entityManager.updateAnnotations(userInfo, pid, annotations);
		project = entityManager.getEntity(userInfo, pid, Project.class);

		JSONObject toUpdate = entityManager.getEntityJson(pid, false);
		toUpdate.put("singleString", "two");
		JSONArray doubleArray = new JSONArray();
		doubleArray.put(new Double(4.5));
		doubleArray.put(new Double(6.7));
		toUpdate.put("listOfDoubles", doubleArray);
		toUpdate.put("parentId", "ignoreMe");

		// Call under test
		JSONObject result = entityManager.updateEntityJson(userInfo, pid, toUpdate);

		assertNotNull(result);
		assertEquals(project.getId(), result.getString("id"));
		// the etag must change
		assertNotEquals(project.getEtag(), result.getString("etag"));
		assertEquals(project.getName(), result.getString("name"));
		// the 'parentId' annotation value should not override the real parentId.
		assertEquals(project.getParentId(), result.getString("parentId"));

		// the annotations:
		assertEquals("two", result.getJSONArray("singleString").getString(0));
		doubleArray = result.getJSONArray("listOfDoubles");
		assertNotNull(doubleArray);
		assertEquals(2, doubleArray.length());
		assertEquals(new Double(4.5), doubleArray.getDouble(0));
		assertEquals(new Double(6.7), doubleArray.getDouble(1));

		Annotations afterAnnotations = entityManager.getAnnotations(adminUserInfo, pid);
		assertNotNull(afterAnnotations);
		assertEquals(project.getId(), afterAnnotations.getId());
		Map<String, AnnotationsValue> map = afterAnnotations.getAnnotations();
		assertNotNull(map);
		assertEquals(2, map.size());
		AnnotationsValue value = map.get("singleString");
		assertEquals(AnnotationsValueType.STRING, value.getType());
		assertEquals( Arrays.asList("two"), value.getValue());
		value = map.get("listOfDoubles");
		assertEquals(AnnotationsValueType.DOUBLE, value.getType());
		assertEquals( Arrays.asList("4.5","6.7"), value.getValue());
	}

	@Test
	public void testUpdateEntityJsonWithBooleanList() {
		// Test for PLFM-6874: To show that boolean lists do not become string lists
		Project project = new Project();
		project.setName("project");
		String pid = entityManager.createEntity(userInfo, project, null);
		toDelete.add(pid);
		project = entityManager.getEntity(userInfo, pid, Project.class);
		// get entity JSON
		JSONObject toUpdate = entityManager.getEntityJson(pid, false);
		// add a list of booleans annotation
		toUpdate.put("key", Arrays.asList(true, false));
		// put entity JSON
		entityManager.updateEntityJson(adminUserInfo, pid, toUpdate);
		// get the entity JSON
		JSONObject projectJSON = entityManager.getEntityJson(adminUserInfo, pid, false);
		assertEquals(projectJSON.getJSONArray("key").get(0).getClass(), Boolean.class);
		assertEquals(projectJSON.getJSONArray("key").get(1).getClass(), Boolean.class);
	}

	@Test
	public void testAddEmptyStringAnnotationWIthEntityJson() {
		Project project = new Project();
		project.setName("project");
		String pid = entityManager.createEntity(userInfo, project, null);
		toDelete.add(pid);

		// get entity JSON
		JSONObject toUpdate = entityManager.getEntityJson(pid, false);
		// add a empty value key annotation
		toUpdate.put("key", "");
		// put entity JSON
		entityManager.updateEntityJson(adminUserInfo, pid, toUpdate);
		//call under test
		JSONObject projectJSON = entityManager.getEntityJson(adminUserInfo, pid, false);

		assertEquals("[\"\"]", projectJSON.get("key").toString());

		Annotations annotations = entityManager.getAnnotations(userInfo, pid);
		Annotations expectedAnnotation = new Annotations().setId(pid).setEtag(annotations.getEtag());
		AnnotationsV2TestUtils.putAnnotations(expectedAnnotation, "key", List.of(""), AnnotationsValueType.STRING);
		assertEquals(expectedAnnotation, annotations);
	}

	@Test
	public void testAddEmptyStringAnnotationWIthAnnotationAPI() {
		Project project = new Project();
		project.setName("project");
		String pid = entityManager.createEntity(userInfo, project, null);
		toDelete.add(pid);

		Annotations annotations = entityManager.getAnnotations(userInfo, pid);
		AnnotationsV2TestUtils.putAnnotations(annotations, "key", List.of(""),
				AnnotationsValueType.STRING);
		annotations.setEtag(annotations.getEtag());

		//call under test
		entityManager.updateAnnotations(adminUserInfo, pid, annotations);

		JSONObject projectJSON = entityManager.getEntityJson(adminUserInfo, pid, false);
		assertEquals("[\"\"]", projectJSON.get("key").toString());

		Annotations updatedAnnotations = entityManager.getAnnotations(userInfo, pid);
		Annotations expectedAnnotation = new Annotations().setId(pid).setEtag(updatedAnnotations.getEtag());
		AnnotationsV2TestUtils.putAnnotations(expectedAnnotation, "key", List.of(""), AnnotationsValueType.STRING);
		assertEquals(expectedAnnotation, updatedAnnotations);
	}

	@Test
	public void testAddSingleStringAnnotationWIthEntityJson() {
		Project project = new Project();
		project.setName("project");
		String pid = entityManager.createEntity(userInfo, project, null);
		toDelete.add(pid);

		// get entity JSON
		JSONObject toUpdate = entityManager.getEntityJson(pid, false);
		toUpdate.put("key", "test");

		//call under test
		entityManager.updateEntityJson(adminUserInfo, pid, toUpdate);

		JSONObject projectJSON = entityManager.getEntityJson(adminUserInfo, pid, false);
		assertEquals("[\"test\"]", projectJSON.get("key").toString());

		Annotations annotations = entityManager.getAnnotations(userInfo, pid);
		Annotations expectedAnnotation = new Annotations().setId(pid).setEtag(annotations.getEtag());
		AnnotationsV2TestUtils.putAnnotations(expectedAnnotation, "key", List.of("test"), AnnotationsValueType.STRING);
		assertEquals(expectedAnnotation, annotations);
	}

	@Test
	public void testAddSingleStringAnnotationWIthAnnotationAPI() {
		Project project = new Project();
		project.setName("project");
		String pid = entityManager.createEntity(userInfo, project, null);
		toDelete.add(pid);

		Annotations annotations = entityManager.getAnnotations(userInfo, pid);
		AnnotationsV2TestUtils.putAnnotations(annotations, "key", List.of("one"),
				AnnotationsValueType.STRING);
		annotations.setEtag(annotations.getEtag());

		//call under test
		entityManager.updateAnnotations(userInfo, pid, annotations);

		// get the entity JSON
		JSONObject projectJSON = entityManager.getEntityJson(userInfo, pid, false);
		assertEquals("[\"one\"]", projectJSON.get("key").toString());

		Annotations UpdatedAnnotations = entityManager.getAnnotations(userInfo, pid);
		Annotations expectedAnnotation = new Annotations().setId(pid).setEtag(UpdatedAnnotations.getEtag());
		AnnotationsV2TestUtils.putAnnotations(expectedAnnotation, "key", List.of("one"), AnnotationsValueType.STRING);
		assertEquals(expectedAnnotation, UpdatedAnnotations);
	}

	@Test
	public void testAddMultipleAnnotationWIthEntityJson() {
		Project project = new Project();
		project.setName("project");
		String pid = entityManager.createEntity(userInfo, project, null);
		toDelete.add(pid);
		// get entity JSON
		JSONObject toUpdate = entityManager.getEntityJson(pid, false);
		toUpdate.put("key", Arrays.asList("one", "", "two"));

		//call under test
		entityManager.updateEntityJson(adminUserInfo, pid, toUpdate);

		JSONObject projectJSON = entityManager.getEntityJson(adminUserInfo, pid, false);
		assertEquals("[\"one\",\"\",\"two\"]", projectJSON.get("key").toString());

		Annotations annotations = entityManager.getAnnotations(userInfo, pid);
		Annotations expectedAnnotation = new Annotations().setId(pid).setEtag(annotations.getEtag());
		AnnotationsV2TestUtils.putAnnotations(expectedAnnotation, "key", Arrays.asList("one", "", "two"), AnnotationsValueType.STRING);
		assertEquals(expectedAnnotation, annotations);
	}

	@Test
	public void testAddMultipleAnnotationWIthAnnotationAPI() {
		Project project = new Project();
		project.setName("project");
		String pid = entityManager.createEntity(userInfo, project, null);
		toDelete.add(pid);
		// get entity JSON
		Annotations annotations = entityManager.getAnnotations(userInfo, pid);
		AnnotationsV2TestUtils.putAnnotations(annotations, "key", List.of("one", "", "two"),
				AnnotationsValueType.STRING);
		annotations.setEtag(annotations.getEtag());

		//call under test
		entityManager.updateAnnotations(adminUserInfo, pid, annotations);

		JSONObject projectJSON = entityManager.getEntityJson(adminUserInfo, pid, false);
		assertEquals("[\"one\",\"\",\"two\"]", projectJSON.get("key").toString());

		Annotations updatedAnnotations = entityManager.getAnnotations(userInfo, pid);

		Annotations expectedAnnotation = new Annotations().setId(pid).setEtag(updatedAnnotations.getEtag());
		AnnotationsV2TestUtils.putAnnotations(expectedAnnotation, "key", Arrays.asList("one", "", "two"), AnnotationsValueType.STRING);
		assertEquals(expectedAnnotation, updatedAnnotations);
	}

	@Test
	public void testUpdateEmptyExistingAnnotationWithEmptyStringWIthEntityJson() {
		Project project = new Project();
		project.setName("project");
		String pid = entityManager.createEntity(userInfo, project, null);
		toDelete.add(pid);
		project = entityManager.getEntity(userInfo, pid, Project.class);
		Annotations annotations = AnnotationsV2TestUtils.newEmptyAnnotationsV2();
		AnnotationsV2TestUtils.putAnnotations(annotations, "key", List.of(),
				AnnotationsValueType.LONG);
		annotations.setEtag(project.getEtag());

		entityManager.updateAnnotations(userInfo, pid, annotations);

		JSONObject toUpdate = entityManager.getEntityJson(pid, false);
		assertEquals("", toUpdate.get("key").toString());

		//call under test
		entityManager.updateEntityJson(adminUserInfo, pid, toUpdate);

		JSONObject projectJSON = entityManager.getEntityJson(adminUserInfo, pid, false);
		assertEquals("[\"\"]", projectJSON.get("key").toString());

		Annotations latestAnnotations = entityManager.getAnnotations(userInfo, pid);
		Annotations expectedAnnotation = new Annotations().setId(pid).setEtag(latestAnnotations.getEtag());
		AnnotationsV2TestUtils.putAnnotations(expectedAnnotation, "key", List.of(""), AnnotationsValueType.STRING);
		assertEquals(expectedAnnotation, latestAnnotations);
	}

	@Test
	public void testUpdateExistingAnnotationWithEmptyStringWIthEntityJson() {
		Project project = new Project();
		project.setName("project");
		String pid = entityManager.createEntity(userInfo, project, null);
		toDelete.add(pid);
		project = entityManager.getEntity(userInfo, pid, Project.class);
		Annotations annotations = AnnotationsV2TestUtils.newEmptyAnnotationsV2();
		AnnotationsV2TestUtils.putAnnotations(annotations, "key", List.of("1", "2"),
				AnnotationsValueType.LONG);
		annotations.setEtag(project.getEtag());

		entityManager.updateAnnotations(userInfo, pid, annotations);

		// get the entity JSON will have array of Long
		JSONObject toUpdate = entityManager.getEntityJson(pid, false);
		assertEquals("[1,2]", toUpdate.get("key").toString());

		toUpdate.put("key", "");
		// put entity JSON with empty annotation
		entityManager.updateEntityJson(adminUserInfo, pid, toUpdate);

		JSONObject projectJSON = entityManager.getEntityJson(adminUserInfo, pid, false);
		assertEquals("[\"\"]", projectJSON.get("key").toString());

		Annotations latestAnnotations = entityManager.getAnnotations(userInfo, pid);
		Annotations expectedAnnotation = new Annotations().setId(pid).setEtag(latestAnnotations.getEtag());
		AnnotationsV2TestUtils.putAnnotations(expectedAnnotation, "key", List.of(""), AnnotationsValueType.STRING);
		assertEquals(expectedAnnotation, latestAnnotations);
	}

	@Test
	public void testUpdateExistingAnnotationWithMixedValuesWIthEntityJson() {
		Project project = new Project();
		project.setName("project");
		String pid = entityManager.createEntity(userInfo, project, null);
		toDelete.add(pid);
		project = entityManager.getEntity(userInfo, pid, Project.class);
		Annotations annotations = AnnotationsV2TestUtils.newEmptyAnnotationsV2();
		AnnotationsV2TestUtils.putAnnotations(annotations, "key", List.of("1", "2"),
				AnnotationsValueType.LONG);
		annotations.setEtag(project.getEtag());

		entityManager.updateAnnotations(userInfo, pid, annotations);

		JSONObject toUpdate = entityManager.getEntityJson(pid, false);
		assertEquals("[1,2]", toUpdate.get("key").toString());

		toUpdate.put("key", List.of("", "1"));
		// call under test
		assertEquals("List of mixed types found for key: 'key'", assertThrows(IllegalArgumentException.class, () -> {
			entityManager.updateEntityJson(adminUserInfo, pid, toUpdate);
		}).getMessage());
	}

	@Test
	public void testUpdateExistingAnnotationWithMixedValuesWIthEntityAnnotation() {
		Project project = new Project();
		project.setName("project");
		String pid = entityManager.createEntity(userInfo, project, null);
		toDelete.add(pid);
		project = entityManager.getEntity(userInfo, pid, Project.class);
		Annotations annotations = AnnotationsV2TestUtils.newEmptyAnnotationsV2();
		AnnotationsV2TestUtils.putAnnotations(annotations, "key", List.of("", "2"),
				AnnotationsValueType.LONG);
		annotations.setEtag(project.getEtag());
		// call under test
		assertEquals("Value associated with key=key is not valid for type=LONG: ", assertThrows(IllegalArgumentException.class, () -> {
			entityManager.updateAnnotations(userInfo, pid, annotations);
		}).getMessage());

	}

	@Test
	public void testAddEmptyArrayAnnotationWIthEntityJson() {
		Project project = new Project();
		project.setName("project");
		String pid = entityManager.createEntity(userInfo, project, null);
		toDelete.add(pid);
		// get the entity JSON
		JSONObject toUpdate = entityManager.getEntityJson(pid, false);
		toUpdate.put("key", List.of());

		// call under test
		assertEquals("a value type must be set for values associated with key=key", assertThrows(IllegalArgumentException.class, () -> {
			entityManager.updateEntityJson(adminUserInfo, pid, toUpdate);
		}).getMessage());
	}

	@Test
	public void testAddEmptyArrayAnnotationWIthAnnotationAPI() {
		Project project = new Project();
		project.setName("project");
		String pid = entityManager.createEntity(userInfo, project, null);
		toDelete.add(pid);
		// get the entity JSON
		Annotations annotations = entityManager.getAnnotations(userInfo, pid);
		AnnotationsV2TestUtils.putAnnotations(annotations, "key", List.of(),
				AnnotationsValueType.STRING);

		entityManager.updateAnnotations(adminUserInfo, pid, annotations);

		// call under test
		JSONObject projectJSON = entityManager.getEntityJson(adminUserInfo, pid, false);
		assertEquals("", projectJSON.get("key").toString());

		Annotations latestAnnotations = entityManager.getAnnotations(userInfo, pid);
		Annotations expectedAnnotation = new Annotations().setId(pid).setEtag(latestAnnotations.getEtag());
		AnnotationsV2TestUtils.putAnnotations(expectedAnnotation, "key", List.of(), AnnotationsValueType.STRING);
		assertEquals(expectedAnnotation, latestAnnotations);
	}

	@Test
	public void testUpdateExistingAnnotationWithEmptyArrayWIthEntityJson() {
		Project project = new Project();
		project.setName("project");
		String pid = entityManager.createEntity(userInfo, project, null);
		toDelete.add(pid);
		project = entityManager.getEntity(userInfo, pid, Project.class);
		Annotations annotations = AnnotationsV2TestUtils.newEmptyAnnotationsV2();
		AnnotationsV2TestUtils.putAnnotations(annotations, "key", List.of("1", "2"),
				AnnotationsValueType.LONG);
		annotations.setEtag(project.getEtag());

		entityManager.updateAnnotations(userInfo, pid, annotations);

		JSONObject toUpdate = entityManager.getEntityJson(pid, false);
		assertEquals("[1,2]", toUpdate.get("key").toString());

		toUpdate.put("key", List.of());
		// //call under test
		assertEquals("a value type must be set for values associated with key=key", assertThrows(IllegalArgumentException.class, () -> {
			entityManager.updateEntityJson(adminUserInfo, pid, toUpdate);
		}).getMessage());
	}

	@Test
	public void testAddNullASAStringAnnotationWIthEntityJson() {
		Project project = new Project();
		project.setName("project");
		String pid = entityManager.createEntity(userInfo, project, null);
		toDelete.add(pid);

		JSONObject toUpdate = entityManager.getEntityJson(pid, false);
		toUpdate.put("key", "null");
		//call under test
		entityManager.updateEntityJson(adminUserInfo, pid, toUpdate);

		JSONObject updateEntityJson = entityManager.getEntityJson(pid, false);
		assertEquals("[\"null\"]", updateEntityJson.get("key").toString());

		//get with annotation
		Annotations latestAnnotations = entityManager.getAnnotations(userInfo, pid);
		Annotations expectedAnnotation = new Annotations().setId(pid).setEtag(latestAnnotations.getEtag());
		AnnotationsV2TestUtils.putAnnotations(expectedAnnotation, "key", List.of("null"), AnnotationsValueType.STRING);
		assertEquals(expectedAnnotation, latestAnnotations);
	}

	@Test
	public void testAddNullASAStringAnnotationWIthAnnotation() {
		Project project = new Project();
		project.setName("project");
		String pid = entityManager.createEntity(userInfo, project, null);
		toDelete.add(pid);
		project = entityManager.getEntity(userInfo, pid, Project.class);

		Annotations annotations = AnnotationsV2TestUtils.newEmptyAnnotationsV2();
		AnnotationsV2TestUtils.putAnnotations(annotations, "key", List.of("null"),
				AnnotationsValueType.STRING);
		annotations.setEtag(project.getEtag());
		//call under test
		entityManager.updateAnnotations(userInfo, pid, annotations);

		JSONObject toUpdate = entityManager.getEntityJson(pid, false);
		assertEquals("[\"null\"]", toUpdate.get("key").toString());

		Annotations latestAnnotations = entityManager.getAnnotations(userInfo, pid);
		Annotations expectedAnnotation = new Annotations().setId(pid).setEtag(latestAnnotations.getEtag());
		AnnotationsV2TestUtils.putAnnotations(expectedAnnotation, "key", List.of("null"), AnnotationsValueType.STRING);
		assertEquals(expectedAnnotation, latestAnnotations);
	}

	@Test
	public void testAddNullAnnotationWIthEntityJson() {
		Project project = new Project();
		project.setName("project");
		String pid = entityManager.createEntity(userInfo, project, null);
		toDelete.add(pid);

		JSONObject toUpdate = entityManager.getEntityJson(pid, false);
		toUpdate.put("key", JSONObject.NULL);

		//call under test
		entityManager.updateEntityJson(adminUserInfo, pid, toUpdate);

		JSONObject projectJSON = entityManager.getEntityJson(adminUserInfo, pid, false);
		assertEquals("[\"null\"]", projectJSON.get("key").toString());

		Annotations latestAnnotations = entityManager.getAnnotations(userInfo, pid);
		Annotations expectedAnnotation = new Annotations().setId(pid).setEtag(latestAnnotations.getEtag());
		AnnotationsV2TestUtils.putAnnotations(expectedAnnotation, "key", List.of("null"), AnnotationsValueType.STRING);
		assertEquals(expectedAnnotation, latestAnnotations);

	}

	@Test
	public void testAddNullAnnotationWIthAnnotation() {
		Project project = new Project();
		project.setName("project");
		String pid = entityManager.createEntity(userInfo, project, null);
		toDelete.add(pid);
		project = entityManager.getEntity(userInfo, pid, Project.class);

		Annotations annotations = AnnotationsV2TestUtils.newEmptyAnnotationsV2();
		AnnotationsV2TestUtils.putAnnotations(annotations, "key", Collections.singletonList(null),
				AnnotationsValueType.STRING);
		annotations.setEtag(project.getEtag());
		// call under test
		assertEquals("null is not allowed. To indicate no values, use an empty list.",
				assertThrows(IllegalArgumentException.class, () -> {
					entityManager.updateAnnotations(userInfo, pid, annotations);
				}).getMessage());
	}

    @Test
    public void testJsonSingledValueSchemaWithMultiValueWithEntityJson() throws Exception {
        Project project = new Project();
        project.setName("project1");
        String pid = entityManager.createEntity(adminUserInfo, project, null);
        toDelete.add(pid);

        //create Schema
        final CreateOrganizationRequest createdOrganizationRequest = createOrganizationRequest();
        organization = jsonSchemaManager.createOrganziation(adminUserInfo, createdOrganizationRequest);
        final CreateSchemaResponse createResponse = jsonSchemaManager.createJsonSchema(adminUserInfo, createSingleValueSchemaRequest(organization));
        String schema$id = createResponse.getNewVersionInfo().get$id();

        //bind schema to entity
        BindSchemaToEntityRequest bindRequest = new BindSchemaToEntityRequest();
        bindRequest.setEntityId(pid);
        bindRequest.setSchema$id(schema$id);
        entityManager.bindSchemaToEntity(adminUserInfo, bindRequest);

        // get the entity JSON
        JSONObject toUpdate = entityManager.getEntityJson(pid, false);
        toUpdate.put("key", List.of("one", "two"));
		//call under test
        entityManager.updateEntityJson(adminUserInfo, pid, toUpdate);

        JSONObject jsonObject = entityManager.getEntityJson(pid, false);
        assertEquals("[\"one\",\"two\"]", jsonObject.get("key").toString());

        Annotations updatedAnnotation = entityManager.getAnnotations(adminUserInfo, pid);
        Annotations expectedAnnotation = new Annotations().setId(pid).setEtag(updatedAnnotation.getEtag());
        AnnotationsV2TestUtils.putAnnotations(expectedAnnotation, "key", List.of("one", "two"), AnnotationsValueType.STRING);
        assertEquals(expectedAnnotation, updatedAnnotation);
    }

    @Test
    public void testJsonSingledValueSchemaWithMultiValueWithAnnotationAPI() throws Exception {
        Project project = new Project();
        project.setName("project1");
        String pid = entityManager.createEntity(adminUserInfo, project, null);
        toDelete.add(pid);

        //create Schema
        final CreateOrganizationRequest createdOrganizationRequest = createOrganizationRequest();
        organization = jsonSchemaManager.createOrganziation(adminUserInfo, createdOrganizationRequest);
        final CreateSchemaResponse createResponse = jsonSchemaManager.createJsonSchema(adminUserInfo, createSingleValueSchemaRequest(organization));
        String schema$id = createResponse.getNewVersionInfo().get$id();

        //bind schema to entity
        BindSchemaToEntityRequest bindRequest = new BindSchemaToEntityRequest();
        bindRequest.setEntityId(pid);
        bindRequest.setSchema$id(schema$id);
        entityManager.bindSchemaToEntity(adminUserInfo, bindRequest);

        Annotations annotations = entityManager.getAnnotations(adminUserInfo, pid);
        AnnotationsV2TestUtils.putAnnotations(annotations, "key", List.of("one", "two"),
                AnnotationsValueType.STRING);
		//call under test
        entityManager.updateAnnotations(adminUserInfo, pid, annotations);

        JSONObject jsonObject = entityManager.getEntityJson(pid, false);
        assertEquals("[\"one\",\"two\"]", jsonObject.get("key").toString());

        Annotations updatedAnnotation = entityManager.getAnnotations(adminUserInfo, pid);
        Annotations expectedAnnotation = new Annotations().setId(pid).setEtag(updatedAnnotation.getEtag());
        AnnotationsV2TestUtils.putAnnotations(expectedAnnotation, "key", List.of("one", "two"), AnnotationsValueType.STRING);
        assertEquals(expectedAnnotation, updatedAnnotation);
    }

    @Test
    public void testJsonSingledValueSchemaWithSingleValueInListWithEntityJson() throws Exception {
        Project project = new Project();
        project.setName("project1");
        String pid = entityManager.createEntity(adminUserInfo, project, null);
        toDelete.add(pid);

        //create Schema
        final CreateOrganizationRequest createdOrganizationRequest = createOrganizationRequest();
        organization = jsonSchemaManager.createOrganziation(adminUserInfo, createdOrganizationRequest);
        final CreateSchemaResponse createResponse = jsonSchemaManager.createJsonSchema(adminUserInfo, createSingleValueSchemaRequest(organization));
        String schema$id = createResponse.getNewVersionInfo().get$id();

        //bind schema to entity
        BindSchemaToEntityRequest bindRequest = new BindSchemaToEntityRequest();
        bindRequest.setEntityId(pid);
        bindRequest.setSchema$id(schema$id);
        entityManager.bindSchemaToEntity(adminUserInfo, bindRequest);

        JSONObject toUpdate = entityManager.getEntityJson(pid, false);
        toUpdate.put("key", List.of("one"));

        //call under test
        entityManager.updateEntityJson(adminUserInfo, pid, toUpdate);

        JSONObject jsonObject = entityManager.getEntityJson(pid, false);
        assertEquals("one", jsonObject.get("key").toString());

        Annotations updatedAnnotation = entityManager.getAnnotations(adminUserInfo, pid);
        Annotations expectedAnnotation = new Annotations().setId(pid).setEtag(updatedAnnotation.getEtag());
        AnnotationsV2TestUtils.putAnnotations(expectedAnnotation, "key", List.of("one"), AnnotationsValueType.STRING);
        assertEquals(expectedAnnotation, updatedAnnotation);
    }

    @Test
    public void testJsonSingledValueSchemaWithSingleValueInListWithAnnotationAPI() throws Exception {
        Project project = new Project();
        project.setName("project1");
        String pid = entityManager.createEntity(adminUserInfo, project, null);
        toDelete.add(pid);

        //create Schema
        final CreateOrganizationRequest createdOrganizationRequest = createOrganizationRequest();
        organization = jsonSchemaManager.createOrganziation(adminUserInfo, createdOrganizationRequest);
        final CreateSchemaResponse createResponse = jsonSchemaManager.createJsonSchema(adminUserInfo, createSingleValueSchemaRequest(organization));
        String schema$id = createResponse.getNewVersionInfo().get$id();

        //bind schema to entity
        BindSchemaToEntityRequest bindRequest = new BindSchemaToEntityRequest();
        bindRequest.setEntityId(pid);
        bindRequest.setSchema$id(schema$id);
        entityManager.bindSchemaToEntity(adminUserInfo, bindRequest);

        Annotations annotations = entityManager.getAnnotations(adminUserInfo, pid);
        AnnotationsV2TestUtils.putAnnotations(annotations, "key", List.of("one"),
                AnnotationsValueType.STRING);
        annotations.setEtag(annotations.getEtag());

        //call under test
        entityManager.updateAnnotations(adminUserInfo, pid, annotations);

        JSONObject jsonObject = entityManager.getEntityJson(pid, false);
        assertEquals("one", jsonObject.get("key").toString());

        Annotations updatedAnnotation = entityManager.getAnnotations(adminUserInfo, pid);
        Annotations expectedAnnotation = new Annotations().setId(pid).setEtag(updatedAnnotation.getEtag());
        AnnotationsV2TestUtils.putAnnotations(expectedAnnotation, "key", List.of("one"), AnnotationsValueType.STRING);
        assertEquals(expectedAnnotation, updatedAnnotation);
    }

    @Test
    public void testJsonSingledValueSchemaWithSingleValueWithEntityJson() throws Exception {
        Project project = new Project();
        project.setName("project1");
        String pid = entityManager.createEntity(adminUserInfo, project, null);
        toDelete.add(pid);

        //create Schema
        final CreateOrganizationRequest createdOrganizationRequest = createOrganizationRequest();
        organization = jsonSchemaManager.createOrganziation(adminUserInfo, createdOrganizationRequest);
        final CreateSchemaResponse createResponse = jsonSchemaManager.createJsonSchema(adminUserInfo, createSingleValueSchemaRequest(organization));
        String schema$id = createResponse.getNewVersionInfo().get$id();

        //bind schema to entity
        BindSchemaToEntityRequest bindRequest = new BindSchemaToEntityRequest();
        bindRequest.setEntityId(pid);
        bindRequest.setSchema$id(schema$id);
        entityManager.bindSchemaToEntity(adminUserInfo, bindRequest);

        JSONObject toUpdate = entityManager.getEntityJson(pid, false);
        toUpdate.put("key", "one");

        //call under test
        entityManager.updateEntityJson(adminUserInfo, pid, toUpdate);

        JSONObject jsonObject = entityManager.getEntityJson(pid, false);
        assertEquals("one", jsonObject.get("key").toString());

        Annotations updatedAnnotation = entityManager.getAnnotations(adminUserInfo, pid);
        Annotations expectedAnnotation = new Annotations().setId(pid).setEtag(updatedAnnotation.getEtag());
        AnnotationsV2TestUtils.putAnnotations(expectedAnnotation, "key", List.of("one"), AnnotationsValueType.STRING);
        assertEquals(expectedAnnotation, updatedAnnotation);
    }

    @Test
    public void testJsonSingledValueSchemaWithEmptyStringWithEntityJson() throws Exception {
        Project project = new Project();
        project.setName("project1");
        String pid = entityManager.createEntity(adminUserInfo, project, null);
        toDelete.add(pid);

        //create Schema
        final CreateOrganizationRequest createdOrganizationRequest = createOrganizationRequest();
        organization = jsonSchemaManager.createOrganziation(adminUserInfo, createdOrganizationRequest);
        final CreateSchemaResponse createResponse = jsonSchemaManager.createJsonSchema(adminUserInfo, createSingleValueSchemaRequest(organization));
        String schema$id = createResponse.getNewVersionInfo().get$id();

        //bind schema to entity
        BindSchemaToEntityRequest bindRequest = new BindSchemaToEntityRequest();
        bindRequest.setEntityId(pid);
        bindRequest.setSchema$id(schema$id);
        entityManager.bindSchemaToEntity(adminUserInfo, bindRequest);

        JSONObject toUpdate = entityManager.getEntityJson(pid, false);
        toUpdate.put("key", "");

        // call under test
        entityManager.updateEntityJson(adminUserInfo, pid, toUpdate);

        JSONObject jsonObject = entityManager.getEntityJson(pid, false);
        assertEquals("", jsonObject.get("key").toString());

        Annotations updatedAnnotation = entityManager.getAnnotations(adminUserInfo, pid);
        Annotations expectedAnnotation = new Annotations().setId(pid).setEtag(updatedAnnotation.getEtag());
        AnnotationsV2TestUtils.putAnnotations(expectedAnnotation, "key", List.of(""), AnnotationsValueType.STRING);
        assertEquals(expectedAnnotation, updatedAnnotation);
    }

    @Test
    public void testJsonSingledValueSchemaWithEmptyStringWithAnnotation() throws Exception {
        Project project = new Project();
        project.setName("project1");
        String pid = entityManager.createEntity(adminUserInfo, project, null);
        toDelete.add(pid);

        //create Schema
        final CreateOrganizationRequest createdOrganizationRequest = createOrganizationRequest();
        organization = jsonSchemaManager.createOrganziation(adminUserInfo, createdOrganizationRequest);
        final CreateSchemaResponse createResponse = jsonSchemaManager.createJsonSchema(adminUserInfo, createSingleValueSchemaRequest(organization));
        String schema$id = createResponse.getNewVersionInfo().get$id();

        //bind schema to entity
        BindSchemaToEntityRequest bindRequest = new BindSchemaToEntityRequest();
        bindRequest.setEntityId(pid);
        bindRequest.setSchema$id(schema$id);
        entityManager.bindSchemaToEntity(adminUserInfo, bindRequest);

        Annotations annotations = entityManager.getAnnotations(adminUserInfo, pid);
        AnnotationsV2TestUtils.putAnnotations(annotations, "key", List.of(""),
                AnnotationsValueType.STRING);
        annotations.setEtag(annotations.getEtag());

        // call under test
        entityManager.updateAnnotations(adminUserInfo, pid, annotations);

        JSONObject jsonObject = entityManager.getEntityJson(pid, false);
        assertEquals("", jsonObject.get("key").toString());

        Annotations updatedAnnotation = entityManager.getAnnotations(adminUserInfo, pid);
        Annotations expectedAnnotation = new Annotations().setId(pid).setEtag(updatedAnnotation.getEtag());
        AnnotationsV2TestUtils.putAnnotations(expectedAnnotation, "key", List.of(""), AnnotationsValueType.STRING);
        assertEquals(expectedAnnotation, updatedAnnotation);
    }

    @Test
    public void testJsonSingledValueSchemaWitNullWithEntityJson() throws Exception {
        Project project = new Project();
        project.setName("project1");
        String pid = entityManager.createEntity(adminUserInfo, project, null);
        toDelete.add(pid);

        //create Schema
        final CreateOrganizationRequest createdOrganizationRequest = createOrganizationRequest();
        organization = jsonSchemaManager.createOrganziation(adminUserInfo, createdOrganizationRequest);
        final CreateSchemaResponse createResponse = jsonSchemaManager.createJsonSchema(adminUserInfo, createSingleValueSchemaRequest(organization));
        String schema$id = createResponse.getNewVersionInfo().get$id();

        //bind schema to entity
        BindSchemaToEntityRequest bindRequest = new BindSchemaToEntityRequest();
        bindRequest.setEntityId(pid);
        bindRequest.setSchema$id(schema$id);
        entityManager.bindSchemaToEntity(adminUserInfo, bindRequest);

        JSONObject toUpdate = entityManager.getEntityJson(pid, false);
        toUpdate.put("key", JSONObject.NULL);

        // call under test
        entityManager.updateEntityJson(adminUserInfo, pid, toUpdate);

        JSONObject jsonObject = entityManager.getEntityJson(pid, false);
        assertEquals("null", jsonObject.get("key").toString());

        Annotations updatedAnnotation = entityManager.getAnnotations(adminUserInfo, pid);
        Annotations expectedAnnotation = new Annotations().setId(pid).setEtag(updatedAnnotation.getEtag());
        AnnotationsV2TestUtils.putAnnotations(expectedAnnotation, "key", List.of("null"), AnnotationsValueType.STRING);
        assertEquals(expectedAnnotation, updatedAnnotation);
    }

    @Test
    public void testJsonMultiValueSchemaWithMultiValueWithEntityJson() throws Exception {
        Project project = new Project();
        project.setName("project");
        String pid = entityManager.createEntity(adminUserInfo, project, null);
        toDelete.add(pid);

        //create Schema
        final CreateOrganizationRequest createdOrganizationRequest = createOrganizationRequest();
        organization = jsonSchemaManager.createOrganziation(adminUserInfo, createdOrganizationRequest);
        final CreateSchemaResponse createResponse = jsonSchemaManager.createJsonSchema(adminUserInfo, createMultiValueSchemaRequest(organization));
        String schema$id = createResponse.getNewVersionInfo().get$id();

        //bind schema to entity
        BindSchemaToEntityRequest bindRequest = new BindSchemaToEntityRequest();
        bindRequest.setEntityId(pid);
        bindRequest.setSchema$id(schema$id);
        entityManager.bindSchemaToEntity(adminUserInfo, bindRequest);

        JSONObject toUpdate = entityManager.getEntityJson(pid, false);
        toUpdate.put("key", List.of("one", "two"));

        // call under test
        entityManager.updateEntityJson(adminUserInfo, pid, toUpdate);

        JSONObject jsonObject = entityManager.getEntityJson(pid, false);
        assertEquals("[\"one\",\"two\"]", jsonObject.get("key").toString());

        Annotations updatedAnnotation = entityManager.getAnnotations(adminUserInfo, pid);
        Annotations expectedAnnotation = new Annotations().setId(pid).setEtag(updatedAnnotation.getEtag());
        AnnotationsV2TestUtils.putAnnotations(expectedAnnotation, "key", Arrays.asList("one", "two"), AnnotationsValueType.STRING);
        assertEquals(expectedAnnotation, updatedAnnotation);
    }

    @Test
    public void testJsonMultiValueSchemaWithMultiValueWithAnnotationAPI() throws Exception {
        Project project = new Project();
        project.setName("project");
        String pid = entityManager.createEntity(adminUserInfo, project, null);
        toDelete.add(pid);

        //create Schema
        final CreateOrganizationRequest createdOrganizationRequest = createOrganizationRequest();
        organization = jsonSchemaManager.createOrganziation(adminUserInfo, createdOrganizationRequest);
        final CreateSchemaResponse createResponse = jsonSchemaManager.createJsonSchema(adminUserInfo, createMultiValueSchemaRequest(organization));
        String schema$id = createResponse.getNewVersionInfo().get$id();

        //bind schema to entity
        BindSchemaToEntityRequest bindRequest = new BindSchemaToEntityRequest();
        bindRequest.setEntityId(pid);
        bindRequest.setSchema$id(schema$id);
        entityManager.bindSchemaToEntity(adminUserInfo, bindRequest);

        Annotations annotations = entityManager.getAnnotations(adminUserInfo, pid);
        AnnotationsV2TestUtils.putAnnotations(annotations, "key", List.of("one", "two"),
                AnnotationsValueType.STRING);
        annotations.setEtag(annotations.getEtag());

        // call under test
        entityManager.updateAnnotations(adminUserInfo, pid, annotations);

        JSONObject jsonObject = entityManager.getEntityJson(pid, false);
        assertEquals("[\"one\",\"two\"]", jsonObject.get("key").toString());

        Annotations updatedAnnotation = entityManager.getAnnotations(adminUserInfo, pid);
        Annotations expectedAnnotation = new Annotations().setId(pid).setEtag(updatedAnnotation.getEtag());
        AnnotationsV2TestUtils.putAnnotations(expectedAnnotation, "key", Arrays.asList("one", "two"), AnnotationsValueType.STRING);
        assertEquals(expectedAnnotation, updatedAnnotation);
    }

    @Test
    public void testJsonMultiValueSchemaWithSingleValueInListWithEntityJson() throws Exception {
        Project project = new Project();
        project.setName("project1");
        String pid = entityManager.createEntity(adminUserInfo, project, null);
        toDelete.add(pid);

        //create Schema
        final CreateOrganizationRequest createdOrganizationRequest = createOrganizationRequest();
        organization = jsonSchemaManager.createOrganziation(adminUserInfo, createdOrganizationRequest);
        final CreateSchemaResponse createResponse = jsonSchemaManager.createJsonSchema(adminUserInfo, createMultiValueSchemaRequest(organization));
        String schema$id = createResponse.getNewVersionInfo().get$id();

        //bind schema to entity
        BindSchemaToEntityRequest bindRequest = new BindSchemaToEntityRequest();
        bindRequest.setEntityId(pid);
        bindRequest.setSchema$id(schema$id);
        entityManager.bindSchemaToEntity(adminUserInfo, bindRequest);

        JSONObject toUpdate = entityManager.getEntityJson(pid, false);
        toUpdate.put("key", List.of("one"));

        // call under test
        entityManager.updateEntityJson(adminUserInfo, pid, toUpdate);

        JSONObject jsonObject = entityManager.getEntityJson(pid, false);
        assertEquals("[\"one\"]", jsonObject.get("key").toString());

        Annotations updatedAnnotation = entityManager.getAnnotations(adminUserInfo, pid);
        Annotations expectedAnnotation = new Annotations().setId(pid).setEtag(updatedAnnotation.getEtag());
        AnnotationsV2TestUtils.putAnnotations(expectedAnnotation, "key", List.of("one"), AnnotationsValueType.STRING);
        assertEquals(expectedAnnotation, updatedAnnotation);
    }

    @Test
    public void testJsonMultiValueSchemaWithSingleValueInListWithAnnotationAPI() throws Exception {
        Project project = new Project();
        project.setName("project1");
        String pid = entityManager.createEntity(adminUserInfo, project, null);
        toDelete.add(pid);

        //create Schema
        final CreateOrganizationRequest createdOrganizationRequest = createOrganizationRequest();
        organization = jsonSchemaManager.createOrganziation(adminUserInfo, createdOrganizationRequest);
        final CreateSchemaResponse createResponse = jsonSchemaManager.createJsonSchema(adminUserInfo, createMultiValueSchemaRequest(organization));
        String schema$id = createResponse.getNewVersionInfo().get$id();

        //bind schema to entity
        BindSchemaToEntityRequest bindRequest = new BindSchemaToEntityRequest();
        bindRequest.setEntityId(pid);
        bindRequest.setSchema$id(schema$id);
        entityManager.bindSchemaToEntity(adminUserInfo, bindRequest);

        Annotations annotations = entityManager.getAnnotations(adminUserInfo, pid);
        AnnotationsV2TestUtils.putAnnotations(annotations, "key", List.of("one"),
                AnnotationsValueType.STRING);
        annotations.setEtag(annotations.getEtag());

        // call under test
        entityManager.updateAnnotations(adminUserInfo, pid, annotations);

        JSONObject jsonObject = entityManager.getEntityJson(pid, false);
        assertEquals("[\"one\"]", jsonObject.get("key").toString());

        Annotations updatedAnnotation = entityManager.getAnnotations(adminUserInfo, pid);
        Annotations expectedAnnotation = new Annotations().setId(pid).setEtag(updatedAnnotation.getEtag());
        AnnotationsV2TestUtils.putAnnotations(expectedAnnotation, "key", List.of("one"), AnnotationsValueType.STRING);
        assertEquals(expectedAnnotation, updatedAnnotation);
    }

    @Test
    public void testJsonMultiValueSchemaWithSingleValueASStringWithEntityJson() throws Exception {
        Project project = new Project();
        project.setName("project1");
        String pid = entityManager.createEntity(adminUserInfo, project, null);
        toDelete.add(pid);

        //create Schema
        final CreateOrganizationRequest createdOrganizationRequest = createOrganizationRequest();
        organization = jsonSchemaManager.createOrganziation(adminUserInfo, createdOrganizationRequest);
        final CreateSchemaResponse createResponse = jsonSchemaManager.createJsonSchema(adminUserInfo, createMultiValueSchemaRequest(organization));
        String schema$id = createResponse.getNewVersionInfo().get$id();

        //bind schema to entity
        BindSchemaToEntityRequest bindRequest = new BindSchemaToEntityRequest();
        bindRequest.setEntityId(pid);
        bindRequest.setSchema$id(schema$id);
        entityManager.bindSchemaToEntity(adminUserInfo, bindRequest);

        JSONObject toUpdate = entityManager.getEntityJson(pid, false);
        toUpdate.put("key", "one");

        // call under test
        entityManager.updateEntityJson(adminUserInfo, pid, toUpdate);

        JSONObject jsonObject = entityManager.getEntityJson(pid, false);
        assertEquals("[\"one\"]", jsonObject.get("key").toString());

        Annotations updatedAnnotation = entityManager.getAnnotations(adminUserInfo, pid);
        Annotations expectedAnnotation = new Annotations().setId(pid).setEtag(updatedAnnotation.getEtag());
        AnnotationsV2TestUtils.putAnnotations(expectedAnnotation, "key", List.of("one"), AnnotationsValueType.STRING);
        assertEquals(expectedAnnotation, updatedAnnotation);
    }

    @Test
    public void testJsonMultiValueSchemaWithEmptyStringWIthEntityJson() throws Exception {
        Project project = new Project();
        project.setName("project1");
        String pid = entityManager.createEntity(adminUserInfo, project, null);
        toDelete.add(pid);

        //create Schema
        final CreateOrganizationRequest createdOrganizationRequest = createOrganizationRequest();
        organization = jsonSchemaManager.createOrganziation(adminUserInfo, createdOrganizationRequest);
        final CreateSchemaResponse createResponse = jsonSchemaManager.createJsonSchema(adminUserInfo, createMultiValueSchemaRequest(organization));
        String schema$id = createResponse.getNewVersionInfo().get$id();

        //bind schema to entity
        BindSchemaToEntityRequest bindRequest = new BindSchemaToEntityRequest();
        bindRequest.setEntityId(pid);
        bindRequest.setSchema$id(schema$id);
        entityManager.bindSchemaToEntity(adminUserInfo, bindRequest);

        JSONObject toUpdate = entityManager.getEntityJson(pid, false);
        toUpdate.put("key", "");

        // call under test
        entityManager.updateEntityJson(adminUserInfo, pid, toUpdate);

        JSONObject jsonObject = entityManager.getEntityJson(pid, false);
        assertEquals("[\"\"]", jsonObject.get("key").toString());

        Annotations updatedAnnotation = entityManager.getAnnotations(adminUserInfo, pid);
        Annotations expectedAnnotation = new Annotations().setId(pid).setEtag(updatedAnnotation.getEtag());
        AnnotationsV2TestUtils.putAnnotations(expectedAnnotation, "key", List.of(""), AnnotationsValueType.STRING);
        assertEquals(expectedAnnotation, updatedAnnotation);
    }

    @Test
    public void testJsonMultiValueSchemaWithEmptyStringWithAnnotationAPI() throws Exception {
        Project project = new Project();
        project.setName("project1");
        String pid = entityManager.createEntity(adminUserInfo, project, null);
        toDelete.add(pid);

        //create Schema
        final CreateOrganizationRequest createdOrganizationRequest = createOrganizationRequest();
        organization = jsonSchemaManager.createOrganziation(adminUserInfo, createdOrganizationRequest);
        final CreateSchemaResponse createResponse = jsonSchemaManager.createJsonSchema(adminUserInfo, createMultiValueSchemaRequest(organization));
        String schema$id = createResponse.getNewVersionInfo().get$id();

        //bind schema to entity
        BindSchemaToEntityRequest bindRequest = new BindSchemaToEntityRequest();
        bindRequest.setEntityId(pid);
        bindRequest.setSchema$id(schema$id);
        entityManager.bindSchemaToEntity(adminUserInfo, bindRequest);

        Annotations annotations = entityManager.getAnnotations(adminUserInfo, pid);
        AnnotationsV2TestUtils.putAnnotations(annotations, "key", List.of(""),
                AnnotationsValueType.STRING);
        annotations.setEtag(annotations.getEtag());

        // call under test
        entityManager.updateAnnotations(adminUserInfo, pid, annotations);

        JSONObject jsonObject = entityManager.getEntityJson(pid, false);
        assertEquals("[\"\"]", jsonObject.get("key").toString());

        Annotations updatedAnnotation = entityManager.getAnnotations(adminUserInfo, pid);
        Annotations expectedAnnotation = new Annotations().setId(pid).setEtag(updatedAnnotation.getEtag());
        AnnotationsV2TestUtils.putAnnotations(expectedAnnotation, "key", List.of(""), AnnotationsValueType.STRING);
        assertEquals(expectedAnnotation, updatedAnnotation);
    }

    @Test
    public void testJsonMultiValueSchemaWitNullWithEntityJson() throws Exception {
        Project project = new Project();
        project.setName("project1");
        String pid = entityManager.createEntity(adminUserInfo, project, null);
        toDelete.add(pid);

        //create Schema
        final CreateOrganizationRequest createdOrganizationRequest = createOrganizationRequest();
        organization = jsonSchemaManager.createOrganziation(adminUserInfo, createdOrganizationRequest);
        final CreateSchemaResponse createResponse = jsonSchemaManager.createJsonSchema(adminUserInfo, createMultiValueSchemaRequest(organization));
        String schema$id = createResponse.getNewVersionInfo().get$id();

        //bind schema to entity
        BindSchemaToEntityRequest bindRequest = new BindSchemaToEntityRequest();
        bindRequest.setEntityId(pid);
        bindRequest.setSchema$id(schema$id);
        entityManager.bindSchemaToEntity(adminUserInfo, bindRequest);

        JSONObject toUpdate = entityManager.getEntityJson(pid, false);
        toUpdate.put("key", JSONObject.NULL);

        // call under test
        entityManager.updateEntityJson(adminUserInfo, pid, toUpdate);

        JSONObject jsonObject = entityManager.getEntityJson(pid, false);
        assertEquals("[\"null\"]", jsonObject.get("key").toString());

        Annotations updatedAnnotation = entityManager.getAnnotations(adminUserInfo, pid);
        Annotations expectedAnnotation = new Annotations().setId(pid).setEtag(updatedAnnotation.getEtag());
        AnnotationsV2TestUtils.putAnnotations(expectedAnnotation, "key", List.of("null"), AnnotationsValueType.STRING);
        assertEquals(expectedAnnotation, updatedAnnotation);
    }

    @Test
	public void testUpdateEntityJsonWithStringListOfBooleans() {
		// Test for PLFM-6874: To show that string false/true are still strings
		Project project = new Project();
		project.setName("project");
		String pid = entityManager.createEntity(userInfo, project, null);
		toDelete.add(pid);
		project = entityManager.getEntity(userInfo, pid, Project.class);
		// get entity JSON
		JSONObject toUpdate = entityManager.getEntityJson(pid, false);
		// add a list of string boolean annotation
		toUpdate.put("key", Arrays.asList("true", "false"));
		// put entity JSON
		entityManager.updateEntityJson(adminUserInfo, pid, toUpdate);
		// get the entity JSON
		JSONObject projectJSON = entityManager.getEntityJson(adminUserInfo, pid, false);
		assertEquals(projectJSON.getJSONArray("key").get(0).getClass(), String.class);
		assertEquals(projectJSON.getJSONArray("key").get(1).getClass(), String.class);
	}

	@Test
	public void testGetAndUpdateEntityJson_NaN_Infinity() {
		Project project = new Project();
		project.setName("test project");
		String pid = entityManager.createEntity(userInfo, project, null);
		toDelete.add(pid);
		project = entityManager.getEntity(userInfo, pid, Project.class);
		Annotations annotations = entityManager.getAnnotations(userInfo, pid);
		AnnotationsV2TestUtils.putAnnotations(annotations, "listOfDoubles", Arrays.asList("NaN", "nan", "Infinity", "infinity", "-Infinity", "-infinity"),
				AnnotationsValueType.DOUBLE);
		entityManager.updateAnnotations(userInfo, pid, annotations);
		project = entityManager.getEntity(userInfo, pid, Project.class);

		// Call under test - PLFM-6872
		// Verify that we get an object back and can read it
		JSONObject toUpdate = entityManager.getEntityJson(pid, false);

		assertEquals("NaN", toUpdate.getJSONArray("listOfDoubles").get(0));
		assertEquals("NaN", toUpdate.getJSONArray("listOfDoubles").get(1));
		assertEquals("Infinity", toUpdate.getJSONArray("listOfDoubles").get(2));
		assertEquals("Infinity", toUpdate.getJSONArray("listOfDoubles").get(3));
		assertEquals("-Infinity", toUpdate.getJSONArray("listOfDoubles").get(4));
		assertEquals("-Infinity", toUpdate.getJSONArray("listOfDoubles").get(5));



		// Call under test
		// Verify that we can submit the string representations of the values and they are still treated as doubles
		JSONObject result = entityManager.updateEntityJson(userInfo, pid, toUpdate);

		assertNotNull(result);

		JSONArray doubleArray = result.getJSONArray("listOfDoubles");
		assertNotNull(doubleArray);
		assertEquals("NaN", doubleArray.get(0));
		assertEquals("NaN", doubleArray.get(1));
		assertEquals("Infinity", doubleArray.get(2));
		assertEquals("Infinity", doubleArray.get(3));
		assertEquals("-Infinity", doubleArray.get(4));
		assertEquals("-Infinity", doubleArray.get(5));

		Annotations afterAnnotations = entityManager.getAnnotations(adminUserInfo, pid);
		assertNotNull(afterAnnotations);
		assertEquals(project.getId(), afterAnnotations.getId());
		Map<String, AnnotationsValue> map = afterAnnotations.getAnnotations();
		assertNotNull(map);
		assertEquals(1, map.size());
		AnnotationsValue value = map.get("listOfDoubles");
		assertEquals(AnnotationsValueType.DOUBLE, value.getType());
		assertEquals(Arrays.asList("NaN", "NaN", "Infinity", "Infinity", "-Infinity", "-Infinity"), value.getValue());
	}

    private CreateOrganizationRequest createOrganizationRequest() {
        final String organizationName = "a.z1.b.com";
        final CreateOrganizationRequest createOrganizationRequest = new CreateOrganizationRequest();
        createOrganizationRequest.setOrganizationName(organizationName);
        return createOrganizationRequest;
    }

    private CreateSchemaRequest createSingleValueSchemaRequest(final Organization organization) throws Exception {
        // create the schema
        String fileName = "schemas/StringSingleValued.json";
        String schemaName = "schema.StringSingleValued.json";
        String semanticVersionString = "1.2.0";
        schema = loadStringFromClasspath(fileName);
        schema.set$id(organization.getName() + "-" + schemaName + "-" + semanticVersionString);
        CreateSchemaRequest createSchemaRequest = new CreateSchemaRequest();
        createSchemaRequest.setSchema(schema);
        return createSchemaRequest;
    }

    private CreateSchemaRequest createMultiValueSchemaRequest(final Organization organization) throws Exception {
        // create the schema
        String fileName = "schemas/StringMultiValued.json";
        String schemaName = "schema.StringMultiValued.json";
        String semanticVersionString = "1.2.0";
        JsonSchema schema = loadStringFromClasspath(fileName);
        schema.set$id(organization.getName() + "-" + schemaName + "-" + semanticVersionString);
        CreateSchemaRequest createSchemaRequest = new CreateSchemaRequest();
        createSchemaRequest.setSchema(schema);
        return createSchemaRequest;
    }

    public JsonSchema loadStringFromClasspath(String name) throws Exception {
        try (InputStream in = EntityManagerImplAutowireTest.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) {
                throw new IllegalArgumentException("Cannot find: '" + name + "' on the classpath");
            }
            return EntityFactory.createEntityFromJSONString(IOUtils.toString(in, "UTF-8"), JsonSchema.class);
        }
    }
}
