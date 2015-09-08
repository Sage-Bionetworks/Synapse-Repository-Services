package org.sagebionetworks.repo.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.TableBundle;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;

public class EntityBundleListTest {

	private static final int NUM_PAGINATED_RESULTS = 5;

	private EntityBundleList entityBundleList;
	
	@Before
	public void setUp() {
		entityBundleList = new EntityBundleList();
	}
	
	@Test
	public void testAddEntityBundle() {
		List<EntityBundle> original = createDummyEntityBundle();
		entityBundleList.getEntityBundles().addAll(original);
		List<EntityBundle> retrieved = entityBundleList.getEntityBundles();
		assertNotNull("EntityBundles were added and should not be null", retrieved);
		assertEquals(retrieved.size(), 2);
		assertEquals(original, retrieved);
		// Duplicates are allowed, and should be added as new items
		entityBundleList.getEntityBundles().addAll(original);
		assertNotNull("EntityBundles were added and should not be null", retrieved);
		assertEquals(retrieved.size(), 4);
		assertTrue(!original.equals(retrieved));
	}
	
	@Test
	public void testJSONRoundTrip() throws Exception{
		List<EntityBundle> bundles = entityBundleList.getEntityBundles();
		assertEquals(bundles.size(), 0);
		bundles.addAll(createDummyEntityBundle());
		JSONObjectAdapter joa = new JSONObjectAdapterImpl();
		joa = entityBundleList.writeToJSONObject(joa);
		String json = joa.toJSONString();
		assertNotNull(json);
		
		EntityBundleList clone = new EntityBundleList();
		clone.initializeFromJSONObject(joa.createNew(json));
		assertEquals(entityBundleList, clone);		
	}
	
	/**
	 * Create an EntityBundle filled with dummy data
	 */
	public static List<EntityBundle> createDummyEntityBundle() {
		EntityBundleList entityBundleList = new EntityBundleList();
		
		// Entities
		Project project = new Project();
		project.setName("Dummy Project");
		FileEntity file = new FileEntity();
		file.setName("Dummy File");
		
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

		EntityBundle projectEntityBundle = new EntityBundle();
		projectEntityBundle.setEntity(project);
		projectEntityBundle.setPermissions(permissions);
		projectEntityBundle.setPath(path);
		projectEntityBundle.setReferencedBy(rb);
		projectEntityBundle.setHasChildren(hasChildren);
		projectEntityBundle.setAccessControlList(acl);
		projectEntityBundle.setBenefactorAcl(benefactorAcl);
		projectEntityBundle.setAccessRequirements(accessRequirements);
		projectEntityBundle.setUnmetAccessRequirements(accessRequirements);
		projectEntityBundle.setFileHandles(fileHandleList);
		projectEntityBundle.setTableBundle(tableBundle);
		projectEntityBundle.setRootWikiId("9876");
		
		EntityBundle fileEntityBundle = new EntityBundle();
		projectEntityBundle.setEntity(file);
		projectEntityBundle.setPermissions(permissions);
		projectEntityBundle.setPath(path);
		
		List<EntityBundle> bundles = new ArrayList<EntityBundle>();
		bundles.add(projectEntityBundle);
		bundles.add(fileEntityBundle);

		return bundles;
	}
	
}
