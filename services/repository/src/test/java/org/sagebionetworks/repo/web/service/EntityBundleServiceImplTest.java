package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityBundleCreate;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.discussion.EntityThreadCount;
import org.sagebionetworks.repo.model.discussion.EntityThreadCounts;
import org.sagebionetworks.repo.model.doi.v2.DoiAssociation;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.service.dataaccess.DataAccessService;
import org.sagebionetworks.repo.web.service.discussion.DiscussionService;
import org.sagebionetworks.repo.web.service.table.TableServices;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

@RunWith(MockitoJUnitRunner.class)
public class EntityBundleServiceImplTest {
	
	private EntityBundleService entityBundleService;
	
	private static final Long TEST_USER1 = 8745962384L;

	@Mock
	private ServiceProvider mockServiceProvider;
	@Mock
	private EntityService mockEntityService;
	@Mock
	private TableServices mockTableService;
	@Mock
	private WikiService mockWikiService;
	@Mock
	private DoiServiceV2 mockDoiServiceV2;
	@Mock
	private DiscussionService mockDiscussionService;
	@Mock
	private DataAccessService mockDataAccessService;
	
	private Project project;
	private Folder study;
	private Folder studyWithId;
	private FileEntity file;
	private Annotations annos;
	private AccessControlList acl;
	private EntityThreadCounts threadCounts;
	
	private EntityBundle responseBundle;
	
	private static final String DUMMY_STUDY_1 = "Test Study 1";
	private static final String DUMMY_PROJECT = "Test Project";
	private static final String DUMMY_FILE = "Test File";
	private static final String STUDY_ID = "1";
	private static final String FILE_ID = "syn2";
	private static final long FILE_VERSION = 3L;
	private static final long BOOTSTRAP_USER_GROUP_ID = 0L;
	
	@Before
	public void setUp() {
		entityBundleService = new EntityBundleServiceImpl(mockServiceProvider);
		mockTableService = mock(TableServices.class);
		when(mockServiceProvider.getTableServices()).thenReturn(mockTableService);
		when(mockServiceProvider.getWikiService()).thenReturn(mockWikiService);
		when(mockServiceProvider.getEntityService()).thenReturn(mockEntityService);
		when(mockServiceProvider.getDoiServiceV2()).thenReturn(mockDoiServiceV2);
		when(mockServiceProvider.getDiscussionService()).thenReturn(mockDiscussionService);
		when(mockServiceProvider.getDataAccessService()).thenReturn(mockDataAccessService);
		
		// Entities
		project = new Project();
		project.setName(DUMMY_PROJECT);

		study = new Folder();
		study.setName(DUMMY_STUDY_1);
		study.setParentId(project.getId());

		studyWithId = new Folder();
		studyWithId.setName(DUMMY_STUDY_1);
		studyWithId.setParentId(project.getId());
		studyWithId.setId(STUDY_ID);

		file = new FileEntity();
		file.setName(DUMMY_FILE);
		file.setParentId(studyWithId.getId());
		file.setVersionNumber(FILE_VERSION);
		file.setId(FILE_ID);

		// Annotations
		annos = new Annotations();		
		annos.addAnnotation("doubleAnno", new Double(45.0001));
		annos.addAnnotation("string", "A string");
		
		// ACL
		acl = new AccessControlList();
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(BOOTSTRAP_USER_GROUP_ID);
		Set<ACCESS_TYPE> atypes = new HashSet<ACCESS_TYPE>();
		atypes.add(ACCESS_TYPE.READ);
		ra.setAccessType(atypes);
		Set<ResourceAccess> raSet = new HashSet<ResourceAccess>();
		raSet.add(ra);
		acl.setResourceAccess(raSet);
		
		// Response bundle
		responseBundle = new EntityBundle();
		responseBundle.setEntity(study);
		responseBundle.setAnnotations(annos);
		responseBundle.setAccessControlList(acl);

		// thread count
		threadCounts = new EntityThreadCounts();
		when(mockDiscussionService.getThreadCounts(eq(TEST_USER1), any(EntityIdList.class))).thenReturn(threadCounts);
	}

	@Test
	public void testCreateEntityBundle() throws NameConflictException, JSONObjectAdapterException, ServletException, IOException, NotFoundException, DatastoreException, ConflictingUpdateException, InvalidModelException, UnauthorizedException, ACLInheritanceException, ParseException {
		String activityId = "123";
		when(mockEntityService.getEntity(eq(TEST_USER1), eq(STUDY_ID))).thenReturn(studyWithId);
		when(mockEntityService.createEntity(eq(TEST_USER1), eq(study), eq(activityId))).thenReturn(studyWithId);
		when(mockEntityService.getEntityACL(eq(STUDY_ID), eq(TEST_USER1))).thenReturn(acl);
		when(mockEntityService.createOrUpdateEntityACL(eq(TEST_USER1), eq(acl), isNull())).thenReturn(acl);
		when(mockEntityService.getEntityAnnotations(eq(TEST_USER1), eq(STUDY_ID))).thenReturn(new Annotations());
		when(mockEntityService.updateEntityAnnotations(eq(TEST_USER1), eq(STUDY_ID), eq(annos))).thenReturn(annos);
		when(mockServiceProvider.getEntityService()).thenReturn(mockEntityService);

		// Create the bundle, verify contents
		EntityBundleCreate ebc = new EntityBundleCreate();
		
		ebc.setEntity(study);
		ebc.setAnnotations(annos);
		ebc.setAccessControlList(acl);
		
		EntityBundle eb = entityBundleService.createEntityBundle(TEST_USER1, ebc, activityId, null);		
		Folder s2 = (Folder) eb.getEntity();
		assertNotNull(s2);
		assertEquals(study.getName(), s2.getName());
		
		Annotations a2 = eb.getAnnotations();
		assertNotNull(a2);
		assertEquals("Retrieved Annotations in bundle do not match original ones", annos.getStringAnnotations(), a2.getStringAnnotations());
		assertEquals("Retrieved Annotations in bundle do not match original ones", annos.getDoubleAnnotations(), a2.getDoubleAnnotations());
		
		AccessControlList acl2 = eb.getAccessControlList();
		assertNotNull(acl2);
		assertEquals("Retrieved ACL in bundle does not match original one", acl.getResourceAccess(), acl2.getResourceAccess());
	
		verify(mockEntityService).createEntity(eq(TEST_USER1), eq(study), eq(activityId));
		verify(mockEntityService).updateEntityAnnotations(eq(TEST_USER1), eq(STUDY_ID), eq(annos));
		verify(mockEntityService).createOrUpdateEntityACL(eq(TEST_USER1), eq(acl), isNull());
	}
	
	@Test
	public void testUpdateEntityBundle() throws NameConflictException, JSONObjectAdapterException, ServletException, IOException, NotFoundException, DatastoreException, ConflictingUpdateException, InvalidModelException, UnauthorizedException, ACLInheritanceException, ParseException {
		Annotations annosWithId = new Annotations();
		annosWithId.setId(STUDY_ID);
		String activityId = "1";
			
		when(mockEntityService.getEntity(eq(TEST_USER1), eq(STUDY_ID))).thenReturn(studyWithId);
		when(mockEntityService.updateEntity(eq(TEST_USER1), eq(study), eq(false), eq(activityId))).thenReturn(studyWithId);
		when(mockEntityService.getEntityACL(eq(STUDY_ID), eq(TEST_USER1))).thenReturn(acl);
		when(mockEntityService.createOrUpdateEntityACL(eq(TEST_USER1), eq(acl), isNull())).thenReturn(acl);
		when(mockEntityService.getEntityAnnotations(eq(TEST_USER1), eq(STUDY_ID))).thenReturn(annosWithId);
		when(mockEntityService.updateEntityAnnotations(eq(TEST_USER1), eq(STUDY_ID), eq(annos))).thenReturn(annos);
		when(mockServiceProvider.getEntityService()).thenReturn(mockEntityService);
		
		// Create the bundle, verify contents
		EntityBundleCreate ebc = new EntityBundleCreate();
		study.setId(STUDY_ID);
		annos.setId(STUDY_ID);
		acl.setId(STUDY_ID);
		ebc.setEntity(study);
		ebc.setAnnotations(annos);
		ebc.setAccessControlList(acl);

		EntityBundle eb = entityBundleService.updateEntityBundle(TEST_USER1, STUDY_ID, ebc, activityId, null);
		
		Folder s2 = (Folder) eb.getEntity();
		assertNotNull(s2);
		assertEquals(study.getName(), s2.getName());
		
		Annotations a2 = eb.getAnnotations();
		assertNotNull(a2);
		assertEquals("Retrieved Annotations in bundle do not match original ones", annos.getStringAnnotations(), a2.getStringAnnotations());
		assertEquals("Retrieved Annotations in bundle do not match original ones", annos.getDoubleAnnotations(), a2.getDoubleAnnotations());
		
		AccessControlList acl2 = eb.getAccessControlList();
		assertNotNull(acl2);
		assertEquals("Retrieved ACL in bundle does not match original one", acl.getResourceAccess(), acl2.getResourceAccess());
	
		verify(mockEntityService).updateEntity(eq(TEST_USER1), eq(study), eq(false), eq(activityId));
		verify(mockEntityService).updateEntityAnnotations(eq(TEST_USER1), eq(STUDY_ID), eq(annos));
		verify(mockEntityService).createOrUpdateEntityACL(eq(TEST_USER1), eq(acl), isNull());
	}
	
	@Test
	public void testTableData() throws Exception {
		String entityId = "syn123";
		PaginatedColumnModels page = new PaginatedColumnModels();
		ColumnModel cm = new ColumnModel();
		cm.setId("9999");
		page.setResults(Arrays.asList(cm));
		when(mockTableService.getColumnModelsForTableEntity(TEST_USER1, entityId)).thenReturn(page);
		when(mockTableService.getMaxRowsPerPage(page.getResults())).thenReturn(12345L);
		int mask = EntityBundle.TABLE_DATA;
		EntityBundle bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, mask, null);
		assertNotNull(bundle);
		assertNotNull(bundle.getTableBundle());
		assertEquals(page.getResults(), bundle.getTableBundle().getColumnModels());
		assertEquals(new Long(12345), bundle.getTableBundle().getMaxRowsPerPage());
	}

	@Test
	public void testDoiAssociation() throws Exception {
		String entityId = "syn123";
		int mask = EntityBundle.DOI;
		DoiAssociation doi = new DoiAssociation();
		doi.setObjectType(ObjectType.ENTITY);
		doi.setObjectId(entityId);
		doi.setObjectVersion(null);
		when(mockDoiServiceV2.getDoiAssociation(TEST_USER1, entityId, ObjectType.ENTITY, null)).thenReturn(doi);
		// Call under test
		EntityBundle bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, mask, null);
		assertNotNull(bundle);
		assertEquals(doi, bundle.getDoiAssociation());
	}

	@Test
	public void testDoiAssociationForUnversionedRequestForVersionable() throws Exception {
		// Must retrieve entity to determine if it is VersionableEntity
		int mask = EntityBundle.ENTITY | EntityBundle.DOI;
		DoiAssociation doi = new DoiAssociation();
		doi.setObjectType(ObjectType.ENTITY);
		doi.setObjectId(FILE_ID);
		doi.setObjectVersion(FILE_VERSION);

		when(mockEntityService.getEntity(eq(TEST_USER1), eq(FILE_ID))).thenReturn(file);
		when(mockDoiServiceV2.getDoiAssociation(TEST_USER1, FILE_ID, ObjectType.ENTITY, FILE_VERSION)).thenReturn(doi);

		// Call under test. Note the bundle requests 'null' version
		EntityBundle bundle = entityBundleService.getEntityBundle(TEST_USER1, FILE_ID, mask, null);

		verify(mockDoiServiceV2, never()).getDoiAssociation(TEST_USER1, FILE_ID, ObjectType.ENTITY, null);
		assertNotNull(bundle);
		assertEquals(doi, bundle.getDoiAssociation());
	}

	@Test
	public void testDoiV2NotFound() throws Exception {
		int mask = EntityBundle.DOI;
		String entityId = "ID that has no object";
		when(mockDoiServiceV2.getDoi(TEST_USER1, entityId, ObjectType.ENTITY, null)).thenThrow(new NotFoundException());
		// Call under test
		EntityBundle bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, mask, null);
		assertNotNull(bundle);
		assertNull(bundle.getDoiAssociation());
	}
	
	@Test
	public void testRootWikiId() throws Exception {
		String entityId = "syn123";
		int mask = EntityBundle.ROOT_WIKI_ID;
		String rootWikiId = "456";
		WikiPageKey key = new WikiPageKey();
		key.setOwnerObjectId(entityId);
		key.setOwnerObjectType(ObjectType.ENTITY);
		key.setWikiPageId(rootWikiId);
		when(mockWikiService.getRootWikiKey(TEST_USER1, entityId, ObjectType.ENTITY)).thenReturn(key);
		EntityBundle bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, mask, null);
		assertNotNull(bundle);
		assertEquals(rootWikiId, bundle.getRootWikiId());
	}
	
	@Test
	public void testRootWikiIdNotFound() throws Exception {
		String entityId = "syn123";
		int mask = EntityBundle.ROOT_WIKI_ID;
		when(mockWikiService.getRootWikiKey(TEST_USER1, entityId, ObjectType.ENTITY)).thenThrow(new NotFoundException("does not exist"));
		EntityBundle bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, mask, null);
		assertNotNull(bundle);
		assertEquals("ID should be null when it does not exist",null, bundle.getRootWikiId());
	}
	
	/**
	 * For this case, the entity is its own benefactor.
	 */
	@Test
	public void testGetBenefactorAclOwnBenefactor() throws Exception {
		AccessControlList acl = new AccessControlList();
		acl.setId("123");
		String entityId = "syn123";
		int mask = EntityBundle.BENEFACTOR_ACL;
		when(mockEntityService.getEntityACL(anyString(), anyLong())).thenReturn(acl);
		EntityBundle bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, mask, null);
		assertNotNull(bundle);
		assertEquals(acl, bundle.getBenefactorAcl());
	}
	
	@Test
	public void testGetBenefactorAclInherited() throws Exception {
		AccessControlList acl = new AccessControlList();
		acl.setId("456");
		String entityId = "syn123";
		String benefactorId = "syn456";
		int mask = EntityBundle.BENEFACTOR_ACL;
		// this entity inherits its permissions.
		when(mockEntityService.getEntityACL(entityId, TEST_USER1)).thenThrow(new ACLInheritanceException("Has a benefactor", benefactorId));
		// return the benefactor ACL.
		when(mockEntityService.getEntityACL(benefactorId, TEST_USER1)).thenReturn(acl);
		EntityBundle bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, mask, null);
		assertNotNull(bundle);
		assertEquals(acl, bundle.getBenefactorAcl());
	}
	
	@Test
	public void testFileNameNoOverride() throws Exception {
		String entityId = "syn123";
		int mask = EntityBundle.FILE_NAME;
		FileEntity entity = new FileEntity();
		long dataFileHandleId = 101L;
		entity.setDataFileHandleId(""+dataFileHandleId);
		String fileName = "foo.txt";
		when(mockEntityService.getEntity(TEST_USER1, entityId)).thenReturn(entity);
		FileHandleResults fhr = new FileHandleResults();
		List<FileHandle> fhs = new ArrayList<FileHandle>();
		fhr.setList(fhs);
		S3FileHandle fh = new S3FileHandle();
		fh.setId(""+dataFileHandleId);
		fh.setFileName(fileName);
		fhs.add(fh);
		fh = new S3FileHandle();
		fh.setId(""+(dataFileHandleId+1));
		fh.setFileName("preview.txt");
		fhs.add(fh);
		fhr.setList(fhs);
		when(mockEntityService.getEntityFileHandlesForCurrentVersion(TEST_USER1, entityId)).thenReturn(fhr);
		EntityBundle bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, mask, null);
		assertNotNull(bundle);
		assertEquals(fileName, bundle.getFileName());
	}
	
	@Test
	public void testFileNameWithOverride() throws Exception {
		String entityId = "syn123";
		int mask = EntityBundle.FILE_NAME;
		FileEntity entity = new FileEntity();
		String fileNameOverride = "foo.txt";
		entity.setFileNameOverride(fileNameOverride);
		when(mockEntityService.getEntity(TEST_USER1, entityId)).thenReturn(entity);
		EntityBundle bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, mask, null);
		assertNotNull(bundle);
		assertEquals(fileNameOverride, bundle.getFileName());
	}

	@Test
	public void testThreadCountNoEntityRef() throws Exception {
		String entityId = "syn123";
		int mask = EntityBundle.THREAD_COUNT;
		threadCounts.setList(new LinkedList<EntityThreadCount>());
		EntityBundle bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, mask, null);
		assertNotNull(bundle);
		assertEquals((Long)0L, bundle.getThreadCount());
	}

	@Test
	public void testThreadCount() throws Exception {
		String entityId = "syn123";
		int mask = EntityBundle.THREAD_COUNT;
		EntityThreadCount threadCount = new EntityThreadCount();
		threadCount.setEntityId(entityId);
		threadCount.setCount(1L);
		threadCounts.setList(Arrays.asList(threadCount));
		EntityBundle bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, mask, null);
		assertNotNull(bundle);
		assertEquals((Long)1L, bundle.getThreadCount());
	}

	@Test (expected=IllegalStateException.class)
	public void testThreadCountUnexpectedResultListSize() throws Exception {
		String entityId = "syn123";
		int mask = EntityBundle.THREAD_COUNT;
		EntityThreadCount threadCount = new EntityThreadCount();
		threadCount.setEntityId(entityId);
		threadCount.setCount(1L);
		threadCounts.setList(Arrays.asList(threadCount, threadCount));
		entityBundleService.getEntityBundle(TEST_USER1, entityId, mask, null);
	}

	@Test
	public void testRestrictionInfo() throws Exception {
		String entityId = "syn123";
		int mask = EntityBundle.RESTRICTION_INFORMATION;
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(entityId);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		RestrictionInformationResponse response = new RestrictionInformationResponse();
		when(mockDataAccessService.getRestrictionInformation(TEST_USER1, request)).thenReturn(response );
		EntityBundle bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, mask, null);
		assertEquals(response, bundle.getRestrictionInformation());
		verify(mockDataAccessService).getRestrictionInformation(TEST_USER1, request);
	}
}
