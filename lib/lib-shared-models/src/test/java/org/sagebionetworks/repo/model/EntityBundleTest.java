package org.sagebionetworks.repo.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.doi.v2.DoiAssociation;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.TableBundle;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

/**
 * Test basic operations of EntityBundles.
 * @author bkng
 *
 */
public class EntityBundleTest {
	
	private static final int NUM_PAGINATED_RESULTS = 5;
	
	private EntityBundle entityBundle;
	
	@BeforeEach
	public void setUp() {
		entityBundle = new EntityBundle();
	}
	
	@Test
	public void testAddProject() {
		testAddEntity(new Project(), Project.class);
	}
	
	
	@Test
	public void testAddFolder() {
		testAddEntity(new Folder(), Folder.class);
	}
	
	@SuppressWarnings("rawtypes")
	private void testAddEntity(Entity original, Class clazz){
		entityBundle.setEntity(original);
		Entity retrieved = entityBundle.getEntity();
		assertNotNull(retrieved, "Entity was set / should not be null.");
		assertEquals(retrieved.getClass().getName(), clazz.getName(), "Entity type was '" + retrieved.getClass().getName() + "'; Expected '"
				+ clazz.getName());
	}

	@Test
	public void testAddDoiAssociation() {
		DoiAssociation doi = new DoiAssociation();
		entityBundle.setDoiAssociation(doi);
		DoiAssociation retrieved = entityBundle.getDoiAssociation();
		assertNotNull(retrieved, "Doi was set / should not be null");
		assertEquals(retrieved, doi, "Set/Retrieved doi do not match original");
	}
	
	@Test
	public void testAddAnnotations() {
		Annotations annotations = new Annotations();
		entityBundle.setAnnotations(annotations);
		Annotations retrieved = entityBundle.getAnnotations();
		assertNotNull(retrieved, "Annotations were set / should not be null");
		assertEquals(retrieved, annotations, "Set/Retrieved annotations do not match original");
	}
		
	@Test
	public void testJSONRoundTrip() throws Exception{
		entityBundle = createDummyEntityBundle();
		
		JSONObjectAdapter joa = new JSONObjectAdapterImpl();
		joa = entityBundle.writeToJSONObject(joa);
		String json = joa.toJSONString();
		assertNotNull(json);
		
		EntityBundle clone = new EntityBundle();
		clone.initializeFromJSONObject(joa.createNew(json));
		assertEquals(entityBundle, clone);		
	}
	
	/**
	 * Create an EntityBundle filled with dummy data
	 */
	public static EntityBundle createDummyEntityBundle() {
		
		// Entities
		Project project = new Project();
		project.setName("Dummy Project");		
		
		// Permissions
		UserEntityPermissions permissions = new UserEntityPermissions();
		permissions.setOwnerPrincipalId(123L);
		permissions.setCanView(true);
		
		// Path
		EntityPath path = new EntityPath(); 

		List<EntityHeader> pathHeaders = new ArrayList<EntityHeader>();		
		EntityHeader rootHeader = new EntityHeader();
		rootHeader.setId("1");
		rootHeader.setName("root");
		pathHeaders.add(rootHeader);		
		EntityHeader projHeader = new EntityHeader();
		projHeader.setId("2");
		projHeader.setName("project");
		pathHeaders.add(projHeader);		
		EntityHeader dsHeader = new EntityHeader();
		dsHeader.setId("3");
		dsHeader.setName("ds");
		pathHeaders.add(dsHeader);		
		path.setPath(pathHeaders);
		
		// Access Control List
		AccessControlList acl = new AccessControlList(); 
		acl.setCreatedBy("John Doe");
		acl.setId("syn456");
		
		AccessControlList benefactorAcl = new AccessControlList(); 
		benefactorAcl.setCreatedBy("John Doe");
		benefactorAcl.setId("syn456");
		
		// Child Count
		Boolean hasChildren = true;
		
		// Annotations
		Annotations annotations = new Annotations();
		annotations.addAnnotation("key1", "value1");
		annotations.addAnnotation("key1", "value2");
		annotations.addAnnotation("key2", "value3");

		// Referencing Entities
		List<EntityHeader> rb = new ArrayList<EntityHeader>(NUM_PAGINATED_RESULTS);
		for (int i = 0; i < NUM_PAGINATED_RESULTS; i++) {
			EntityHeader eh = new EntityHeader();
			eh.setId("syn" + i);
			eh.setName("EntityHeader " + i);
			eh.setType("Folder");
			rb.add(eh);
		}

		List<AccessRequirement> accessRequirements = new ArrayList<AccessRequirement>();
		TermsOfUseAccessRequirement ar = new TermsOfUseAccessRequirement();
		ar.setAccessType(ACCESS_TYPE.DOWNLOAD);
		RestrictableObjectDescriptor rod1 = new RestrictableObjectDescriptor(); rod1.setId("101"); rod1.setType(RestrictableObjectType.EVALUATION);
		RestrictableObjectDescriptor rod2 = new RestrictableObjectDescriptor(); rod1.setId("102"); rod1.setType(RestrictableObjectType.EVALUATION);
		ar.setSubjectIds(Arrays.asList(new RestrictableObjectDescriptor[]{rod1, rod2}));
		ar.setConcreteType(TermsOfUseAccessRequirement.class.getName());
		ar.setTermsOfUse("foo");
		accessRequirements.add(ar);
		
		// File handles
		S3FileHandle fileHandle = new S3FileHandle();
		fileHandle.setBucketName("bucket");
		fileHandle.setKey("key");
		fileHandle.setId("00000");
		List<FileHandle> fileHandleList = new LinkedList<FileHandle>();
		fileHandleList.add(fileHandle);
		
		TableBundle tableBundle = new  TableBundle();
		tableBundle.setMaxRowsPerPage(123L);
		ColumnModel cm1 = new ColumnModel();
		cm1.setId("456");
		ColumnModel cm2 = new ColumnModel();
		cm2.setId("890");
		tableBundle.setColumnModels(Arrays.asList(cm1, cm2));

		DoiAssociation doiAssociation = new DoiAssociation();
		doiAssociation.setAssociatedBy("John Doe");
		doiAssociation.setObjectId("syn456");
		doiAssociation.setObjectId("1");

		EntityBundle entityBundle = new EntityBundle();
		entityBundle.setEntity(project);
		entityBundle.setPermissions(permissions);
		entityBundle.setPath(path);
		entityBundle.setReferencedBy(rb);
		entityBundle.setHasChildren(hasChildren);
		entityBundle.setAccessControlList(acl);
		entityBundle.setBenefactorAcl(benefactorAcl);
		entityBundle.setAccessRequirements(accessRequirements);
		entityBundle.setUnmetAccessRequirements(accessRequirements);
		entityBundle.setFileHandles(fileHandleList);
		entityBundle.setTableBundle(tableBundle);
		entityBundle.setRootWikiId("9876");
		entityBundle.setDoiAssociation(doiAssociation);
		entityBundle.setFileName("foo.txt");
		
		return entityBundle;
	}

	@Test
	public void testSetEntity_nullEntity(){
		//first set a non-null Entity to give entityType a value
		entityBundle.setEntity(new Project());
		assertEquals("org.sagebionetworks.repo.model.Project", entityBundle.getEntityType());
		//then set to entity null and check that entity type has also been set to null
		entityBundle.setEntity(null);
		assertNull(entityBundle.getEntityType());
	}
}
