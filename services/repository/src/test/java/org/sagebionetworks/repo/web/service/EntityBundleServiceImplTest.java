package org.sagebionetworks.repo.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.servlet.ServletException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityBundleCreate;
import org.sagebionetworks.repo.model.EntityBundleV2;
import org.sagebionetworks.repo.model.EntityBundleV2Create;
import org.sagebionetworks.repo.model.EntityBundleV2Request;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.EntityPath;
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
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2TestUtils;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Translator;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.discussion.EntityThreadCount;
import org.sagebionetworks.repo.model.discussion.EntityThreadCounts;
import org.sagebionetworks.repo.model.doi.v2.DoiAssociation;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.TableBundle;
import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.service.dataaccess.DataAccessService;
import org.sagebionetworks.repo.web.service.discussion.DiscussionService;
import org.sagebionetworks.repo.web.service.table.TableServices;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

@ExtendWith(MockitoExtension.class)
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
	private AnnotationsV2 annotationsV2;
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
	
	@BeforeEach
	public void setUp() {
		entityBundleService = new EntityBundleServiceImpl(mockServiceProvider);
		mockTableService = mock(TableServices.class);
		lenient().when(mockServiceProvider.getTableServices()).thenReturn(mockTableService);
		lenient().when(mockServiceProvider.getWikiService()).thenReturn(mockWikiService);
		lenient().when(mockServiceProvider.getEntityService()).thenReturn(mockEntityService);
		lenient().when(mockServiceProvider.getDoiServiceV2()).thenReturn(mockDoiServiceV2);
		lenient().when(mockServiceProvider.getDiscussionService()).thenReturn(mockDiscussionService);
		lenient().when(mockServiceProvider.getDataAccessService()).thenReturn(mockDataAccessService);
		
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
		annos.setId(STUDY_ID);
		annos.addAnnotation("doubleAnno", new Double(45.0001));
		annos.addAnnotation("string", "A string");
		//TODO: init v2 with actual values
		annotationsV2 = AnnotationsV2Translator.toAnnotationsV2(annos);

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
	}

	@Test
	public void testCreateEntityBundle() throws NameConflictException, JSONObjectAdapterException, ServletException, IOException, NotFoundException, DatastoreException, ConflictingUpdateException, InvalidModelException, UnauthorizedException, ACLInheritanceException, ParseException {
		String activityId = "123";
		when(mockEntityService.getEntity(eq(TEST_USER1), eq(STUDY_ID))).thenReturn(studyWithId);
		when(mockEntityService.createEntity(eq(TEST_USER1), eq(study), eq(activityId))).thenReturn(studyWithId);
		when(mockEntityService.getEntityACL(eq(STUDY_ID), eq(TEST_USER1))).thenReturn(acl);
		when(mockEntityService.createOrUpdateEntityACL(eq(TEST_USER1), eq(acl), isNull())).thenReturn(acl);
		when(mockEntityService.getEntityAnnotations(eq(TEST_USER1), eq(STUDY_ID))).thenReturn(AnnotationsV2TestUtils.newEmptyAnnotationsV2(STUDY_ID));
		when(mockEntityService.updateEntityAnnotations(eq(TEST_USER1), eq(STUDY_ID), eq(annotationsV2))).thenReturn(annotationsV2);
		when(mockServiceProvider.getEntityService()).thenReturn(mockEntityService);

		// Create the bundle, verify contents
		EntityBundleCreate ebc = new EntityBundleCreate();
		
		ebc.setEntity(study);
		ebc.setAnnotations(annos);
		ebc.setAccessControlList(acl);
		
		EntityBundle eb = entityBundleService.createEntityBundle(TEST_USER1, ebc, activityId);		
		Folder s2 = (Folder) eb.getEntity();
		assertNotNull(s2);
		assertEquals(study.getName(), s2.getName());
		
		Annotations a2 = eb.getAnnotations();
		assertNotNull(a2);
		assertEquals(annos.getStringAnnotations(), a2.getStringAnnotations(), "Retrieved Annotations in bundle do not match original ones");
		assertEquals(annos.getDoubleAnnotations(), a2.getDoubleAnnotations(), "Retrieved Annotations in bundle do not match original ones");
		
		AccessControlList acl2 = eb.getAccessControlList();
		assertNotNull(acl2);
		assertEquals(acl.getResourceAccess(), acl2.getResourceAccess(), "Retrieved ACL in bundle does not match original one");
	
		verify(mockEntityService).createEntity(eq(TEST_USER1), eq(study), eq(activityId));
		verify(mockEntityService).updateEntityAnnotations(eq(TEST_USER1), eq(STUDY_ID), eq(annotationsV2));
		verify(mockEntityService).createOrUpdateEntityACL(eq(TEST_USER1), eq(acl), isNull());
	}
	
	@Test
	public void testUpdateEntityBundle() throws NameConflictException, JSONObjectAdapterException, ServletException, IOException, NotFoundException, DatastoreException, ConflictingUpdateException, InvalidModelException, UnauthorizedException, ACLInheritanceException, ParseException {
		AnnotationsV2 annosWithId = AnnotationsV2TestUtils.newEmptyAnnotationsV2();
		annosWithId.setId(STUDY_ID);
		String activityId = "1";
			
		when(mockEntityService.getEntity(eq(TEST_USER1), eq(STUDY_ID))).thenReturn(studyWithId);
		when(mockEntityService.updateEntity(eq(TEST_USER1), eq(study), eq(false), eq(activityId))).thenReturn(studyWithId);
		when(mockEntityService.getEntityACL(eq(STUDY_ID), eq(TEST_USER1))).thenReturn(acl);
		when(mockEntityService.createOrUpdateEntityACL(eq(TEST_USER1), eq(acl), isNull())).thenReturn(acl);
		when(mockEntityService.getEntityAnnotations(eq(TEST_USER1), eq(STUDY_ID))).thenReturn(annosWithId);
		when(mockEntityService.updateEntityAnnotations(eq(TEST_USER1), eq(STUDY_ID), eq(annotationsV2))).thenReturn(annotationsV2);
		when(mockServiceProvider.getEntityService()).thenReturn(mockEntityService);
		
		// Create the bundle, verify contents
		EntityBundleCreate ebc = new EntityBundleCreate();
		study.setId(STUDY_ID);
		annos.setId(STUDY_ID);
		acl.setId(STUDY_ID);
		ebc.setEntity(study);
		ebc.setAnnotations(annos);
		ebc.setAccessControlList(acl);

		EntityBundle eb = entityBundleService.updateEntityBundle(TEST_USER1, STUDY_ID, ebc, activityId);

		Folder s2 = (Folder) eb.getEntity();
		assertNotNull(s2);
		assertEquals(study.getName(), s2.getName());
		
		Annotations a2 = eb.getAnnotations();
		assertNotNull(a2);
		assertEquals(annos.getStringAnnotations(), a2.getStringAnnotations(), "Retrieved Annotations in bundle do not match original ones");
		assertEquals(annos.getDoubleAnnotations(), a2.getDoubleAnnotations(), "Retrieved Annotations in bundle do not match original ones");
		
		AccessControlList acl2 = eb.getAccessControlList();
		assertNotNull(acl2);
		assertEquals(acl.getResourceAccess(), acl2.getResourceAccess(), "Retrieved ACL in bundle does not match original one");
	
		verify(mockEntityService).updateEntity(eq(TEST_USER1), eq(study), eq(false), eq(activityId));
		verify(mockEntityService).updateEntityAnnotations(eq(TEST_USER1), eq(STUDY_ID), eq(annotationsV2));
		verify(mockEntityService).createOrUpdateEntityACL(eq(TEST_USER1), eq(acl), isNull());
	}
	
	@Test
	public void testTableData() throws Exception {
		String entityId = "syn123";
		IdAndVersion idAndVersion = IdAndVersion.parse(entityId);
		ColumnModel cm = new ColumnModel();
		cm.setId("9999");
		TableBundle tableBundle = new TableBundle();
		tableBundle.setColumnModels(Arrays.asList(cm));
		tableBundle.setMaxRowsPerPage(new Long(12345));
		when(mockTableService.getTableBundle(idAndVersion)).thenReturn(tableBundle);
		EntityBundleV2Request request = new EntityBundleV2Request();
		request.setIncludeTableBundle(true);
		// call under test
		EntityBundleV2 bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, request);
		assertNotNull(bundle);
		assertEquals(tableBundle, bundle.getTableBundle());
		verify(mockTableService).getTableBundle(idAndVersion);
	}
	
	@Test
	public void testTableDataVersion() throws Exception {
		String entityId = "syn123";
		Long versionNumber = 22L;
		IdAndVersion idAndVersion = IdAndVersion.parse(entityId+"."+versionNumber);
		ColumnModel cm = new ColumnModel();
		cm.setId("9999");
		TableBundle tableBundle = new TableBundle();
		tableBundle.setColumnModels(Arrays.asList(cm));
		tableBundle.setMaxRowsPerPage(new Long(12345));
		when(mockTableService.getTableBundle(idAndVersion)).thenReturn(tableBundle);
		EntityBundleV2Request request = new EntityBundleV2Request();
		request.setIncludeTableBundle(true);
		// call under test
		EntityBundleV2 bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, versionNumber, request);
		assertNotNull(bundle);
		assertEquals(tableBundle, bundle.getTableBundle());
		verify(mockTableService).getTableBundle(idAndVersion);
	}

	@Test
	public void testDoiAssociation() throws Exception {
		String entityId = "syn123";
		EntityBundleV2Request request = new EntityBundleV2Request();
		request.setIncludeDOIAssociation(true);
		DoiAssociation doi = new DoiAssociation();
		doi.setObjectType(ObjectType.ENTITY);
		doi.setObjectId(entityId);
		doi.setObjectVersion(null);
		when(mockDoiServiceV2.getDoiAssociation(TEST_USER1, entityId, ObjectType.ENTITY, null)).thenReturn(doi);
		// Call under test
		EntityBundleV2 bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, request);
		assertNotNull(bundle);
		assertEquals(doi, bundle.getDoiAssociation());
	}

	@Test
	public void testDoiAssociationForUnversionedRequestForVersionable() throws Exception {
		// Must retrieve entity to determine if it is VersionableEntity
		EntityBundleV2Request request = new EntityBundleV2Request();
		request.setIncludeEntity(true);
		request.setIncludeDOIAssociation(true);
		DoiAssociation doi = new DoiAssociation();
		doi.setObjectType(ObjectType.ENTITY);
		doi.setObjectId(FILE_ID);
		doi.setObjectVersion(FILE_VERSION);

		when(mockEntityService.getEntity(eq(TEST_USER1), eq(FILE_ID))).thenReturn(file);
		when(mockDoiServiceV2.getDoiAssociation(TEST_USER1, FILE_ID, ObjectType.ENTITY, FILE_VERSION)).thenReturn(doi);

		// Call under test. Note the bundle requests 'null' version
		EntityBundleV2 bundle = entityBundleService.getEntityBundle(TEST_USER1, FILE_ID, request);

		verify(mockDoiServiceV2, never()).getDoiAssociation(TEST_USER1, FILE_ID, ObjectType.ENTITY, null);
		assertNotNull(bundle);
		assertEquals(doi, bundle.getDoiAssociation());
	}

	@Test
	public void testDoiV2NotFound() throws Exception {
		EntityBundleV2Request request = new EntityBundleV2Request();
		request.setIncludeDOIAssociation(true);
		String entityId = "syn123";
		when(mockDoiServiceV2.getDoiAssociation(TEST_USER1, entityId, ObjectType.ENTITY, null)).thenThrow(new NotFoundException());
		// Call under test
		EntityBundleV2 bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, request);
		assertNotNull(bundle);
		assertNull(bundle.getDoiAssociation());
		verify(mockDoiServiceV2).getDoiAssociation(TEST_USER1, entityId, ObjectType.ENTITY, null);
	}
	
	@Test
	public void testRootWikiId() throws Exception {
		String entityId = "syn123";
		EntityBundleV2Request request = new EntityBundleV2Request();
		request.setIncludeRootWikiId(true);
		String rootWikiId = "456";
		WikiPageKey key = new WikiPageKey();
		key.setOwnerObjectId(entityId);
		key.setOwnerObjectType(ObjectType.ENTITY);
		key.setWikiPageId(rootWikiId);
		when(mockWikiService.getRootWikiKey(TEST_USER1, entityId, ObjectType.ENTITY)).thenReturn(key);
		EntityBundleV2 bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, request);
		assertNotNull(bundle);
		assertEquals(rootWikiId, bundle.getRootWikiId());
	}
	
	@Test
	public void testRootWikiIdNotFound() throws Exception {
		String entityId = "syn123";
		EntityBundleV2Request request = new EntityBundleV2Request();
		request.setIncludeRootWikiId(true);
		when(mockWikiService.getRootWikiKey(TEST_USER1, entityId, ObjectType.ENTITY)).thenThrow(new NotFoundException("does not exist"));
		EntityBundleV2 bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, request);
		assertNotNull(bundle);
		assertNull(bundle.getRootWikiId(), "ID should be null when it does not exist");
	}
	
	/**
	 * For this case, the entity is its own benefactor.
	 */
	@Test
	public void testGetBenefactorAclOwnBenefactor() throws Exception {
		AccessControlList acl = new AccessControlList();
		acl.setId("123");
		String entityId = "syn123";
		EntityBundleV2Request request = new EntityBundleV2Request();
		request.setIncludeBenefactorACL(true);
		when(mockEntityService.getEntityACL(anyString(), anyLong())).thenReturn(acl);
		EntityBundleV2 bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, request);
		assertNotNull(bundle);
		assertEquals(acl, bundle.getBenefactorAcl());
	}

	@Test
	public void testGetBenefactorAclInherited() throws Exception {
		AccessControlList acl = new AccessControlList();
		acl.setId("456");
		String entityId = "syn123";
		String benefactorId = "syn456";
		EntityBundleV2Request request = new EntityBundleV2Request();
		request.setIncludeBenefactorACL(true);
		// this entity inherits its permissions.
		when(mockEntityService.getEntityACL(entityId, TEST_USER1)).thenThrow(new ACLInheritanceException("Has a benefactor", benefactorId));
		// return the benefactor ACL.
		when(mockEntityService.getEntityACL(benefactorId, TEST_USER1)).thenReturn(acl);
		EntityBundleV2 bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, request);
		assertNotNull(bundle);
		assertEquals(acl, bundle.getBenefactorAcl());
	}
	
	@Test
	public void testFileNameNoOverride() throws Exception {
		String entityId = "syn123";
		EntityBundleV2Request request = new EntityBundleV2Request();
		request.setIncludeFileName(true);
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
		EntityBundleV2 bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, request);
		assertNotNull(bundle);
		assertEquals(fileName, bundle.getFileName());
	}
	
	@Test
	public void testFileNameWithOverride() throws Exception {
		String entityId = "syn123";
		EntityBundleV2Request request = new EntityBundleV2Request();
		request.setIncludeFileName(true);
		FileEntity entity = new FileEntity();
		String fileNameOverride = "foo.txt";
		entity.setFileNameOverride(fileNameOverride);
		when(mockEntityService.getEntity(TEST_USER1, entityId)).thenReturn(entity);
		EntityBundleV2 bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, request);
		assertNotNull(bundle);
		assertEquals(fileNameOverride, bundle.getFileName());
	}

	@Test
	public void testThreadCountNoEntityRef() throws Exception {
		// thread count
		threadCounts = new EntityThreadCounts();
		when(mockDiscussionService.getThreadCounts(eq(TEST_USER1), any(EntityIdList.class))).thenReturn(threadCounts);

		String entityId = "syn123";
		EntityBundleV2Request request = new EntityBundleV2Request();
		request.setIncludeThreadCount(true);
		threadCounts.setList(new LinkedList<EntityThreadCount>());
		EntityBundleV2 bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, request);
		assertNotNull(bundle);
		assertEquals((Long)0L, bundle.getThreadCount());
	}

	@Test
	public void testThreadCount() throws Exception {
		// thread count
		threadCounts = new EntityThreadCounts();
		when(mockDiscussionService.getThreadCounts(eq(TEST_USER1), any(EntityIdList.class))).thenReturn(threadCounts);

		String entityId = "syn123";
		EntityBundleV2Request request = new EntityBundleV2Request();
		request.setIncludeThreadCount(true);
		EntityThreadCount threadCount = new EntityThreadCount();
		threadCount.setEntityId(entityId);
		threadCount.setCount(1L);
		threadCounts.setList(Arrays.asList(threadCount));
		EntityBundleV2 bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, request);
		assertNotNull(bundle);
		assertEquals((Long)1L, bundle.getThreadCount());
	}

	@Test
	public void testThreadCountUnexpectedResultListSize() throws Exception {
		// thread count
		threadCounts = new EntityThreadCounts();
		when(mockDiscussionService.getThreadCounts(eq(TEST_USER1), any(EntityIdList.class))).thenReturn(threadCounts);

		String entityId = "syn123";
		EntityBundleV2Request request = new EntityBundleV2Request();
		request.setIncludeThreadCount(true);
		EntityThreadCount threadCount = new EntityThreadCount();
		threadCount.setEntityId(entityId);
		threadCount.setCount(1L);
		threadCounts.setList(Arrays.asList(threadCount, threadCount));
		assertThrows(IllegalStateException.class, ()-> {
			entityBundleService.getEntityBundle(TEST_USER1, entityId, request);
		});
	}

	@Test
	public void testRestrictionInfo() throws Exception {
		String entityId = "syn123";
		EntityBundleV2Request bundleV2Request = new EntityBundleV2Request();
		bundleV2Request.setIncludeRestrictionInformation(true);
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(entityId);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		RestrictionInformationResponse response = new RestrictionInformationResponse();
		when(mockDataAccessService.getRestrictionInformation(TEST_USER1, request)).thenReturn(response );
		EntityBundleV2 bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, bundleV2Request);
		assertEquals(response, bundle.getRestrictionInformation());
		verify(mockDataAccessService).getRestrictionInformation(TEST_USER1, request);
	}

	@Test
	public void testRequestFromMask_individualMasks() {
		//assert individual requests
		assertTrue(EntityBundleServiceImpl.requestFromMask(EntityBundle.ENTITY).getIncludeEntity());
		assertTrue(EntityBundleServiceImpl.requestFromMask(EntityBundle.ANNOTATIONS).getIncludeAnnotations());
		assertTrue(EntityBundleServiceImpl.requestFromMask(EntityBundle.PERMISSIONS).getIncludePermissions());
		assertTrue(EntityBundleServiceImpl.requestFromMask(EntityBundle.ENTITY_PATH).getIncludeEntityPath());
		assertTrue(EntityBundleServiceImpl.requestFromMask(EntityBundle.HAS_CHILDREN).getIncludeHasChildren());
		assertTrue(EntityBundleServiceImpl.requestFromMask(EntityBundle.ACL).getIncludeAccessControlList());
		assertTrue(EntityBundleServiceImpl.requestFromMask(EntityBundle.FILE_HANDLES).getIncludeFileHandles());
		assertTrue(EntityBundleServiceImpl.requestFromMask(EntityBundle.TABLE_DATA).getIncludeTableBundle());
		assertTrue(EntityBundleServiceImpl.requestFromMask(EntityBundle.ROOT_WIKI_ID).getIncludeRootWikiId());
		assertTrue(EntityBundleServiceImpl.requestFromMask(EntityBundle.BENEFACTOR_ACL).getIncludeBenefactorACL());
		assertTrue(EntityBundleServiceImpl.requestFromMask(EntityBundle.DOI).getIncludeDOIAssociation());
		assertTrue(EntityBundleServiceImpl.requestFromMask(EntityBundle.FILE_NAME).getIncludeFileName());
		assertTrue(EntityBundleServiceImpl.requestFromMask(EntityBundle.THREAD_COUNT).getIncludeThreadCount());
		assertTrue(EntityBundleServiceImpl.requestFromMask(EntityBundle.RESTRICTION_INFORMATION).getIncludeRestrictionInformation());
	}

	@Test
	public void testRequestFromMask_combinationMask() {
		//assert individual requests
		int mask = EntityBundle.ENTITY | EntityBundle.PERMISSIONS | EntityBundle.HAS_CHILDREN | EntityBundle.FILE_HANDLES
				| EntityBundle.ROOT_WIKI_ID | EntityBundle.DOI | EntityBundle.THREAD_COUNT;
		EntityBundleV2Request request = EntityBundleServiceImpl.requestFromMask(mask);
		assertTrue(request.getIncludeEntity());
		assertFalse(request.getIncludeAnnotations());
		assertTrue(request.getIncludePermissions());
		assertFalse(request.getIncludeEntityPath());
		assertTrue(request.getIncludeHasChildren());
		assertFalse(request.getIncludeAccessControlList());
		assertTrue(request.getIncludeFileHandles());
		assertFalse(request.getIncludeTableBundle());
		assertTrue(request.getIncludeRootWikiId());
		assertFalse(request.getIncludeBenefactorACL());
		assertTrue(request.getIncludeDOIAssociation());
		assertFalse(request.getIncludeFileName());
		assertTrue(request.getIncludeThreadCount());
		assertFalse(request.getIncludeRestrictionInformation());
	}

	@Test
	public void testTranslateEntityBundleCreate(){
		EntityBundleCreate entityBundleCreate = new EntityBundleCreate();
		entityBundleCreate.setAnnotations(new Annotations());
		entityBundleCreate.setAccessControlList(new AccessControlList());
		entityBundleCreate.setEntity(new FileEntity());


		EntityBundleV2Create v2Create = EntityBundleServiceImpl.translateEntityBundleCreate(entityBundleCreate);

		//directly copied over
		assertSame(entityBundleCreate.getAccessControlList(), v2Create.getAccessControlList());
		assertSame(entityBundleCreate.getEntity(), v2Create.getEntity());
		//this should have been translated
		assertEquals(entityBundleCreate.getAnnotations(), AnnotationsV2Translator.toAnnotationsV1(v2Create.getAnnotations()));
	}

	@Test
	public void testTranslateEntityBundle(){
		EntityBundleV2 entityBundleV2 = new EntityBundleV2();
		entityBundleV2.setEntity(new FileEntity());
		entityBundleV2.setAnnotations(AnnotationsV2TestUtils.newEmptyAnnotationsV2("syn123"));
		entityBundleV2.setPermissions(new UserEntityPermissions());
		entityBundleV2.setPath(new EntityPath());
		entityBundleV2.setHasChildren(false);
		entityBundleV2.setAccessControlList(new AccessControlList());
		entityBundleV2.setFileHandles(Collections.emptyList());
		entityBundleV2.setTableBundle(new TableBundle());
		entityBundleV2.setRootWikiId("root wiki id");
		entityBundleV2.setBenefactorAcl(new AccessControlList());
		entityBundleV2.setDoiAssociation(new DoiAssociation());
		entityBundleV2.setFileName("filename");
		entityBundleV2.setThreadCount(42L);
		entityBundleV2.setRestrictionInformation(new RestrictionInformationResponse());

		EntityBundle v1Bundle = EntityBundleServiceImpl.translateEntityBundle(entityBundleV2);
		assertSame(entityBundleV2.getEntity(), v1Bundle.getEntity());
		assertEquals(entityBundleV2.getAnnotations(), AnnotationsV2Translator.toAnnotationsV2(v1Bundle.getAnnotations()));
		assertSame(entityBundleV2.getPermissions(), v1Bundle.getPermissions());
		assertSame(entityBundleV2.getPath(), v1Bundle.getPath());
		assertSame(entityBundleV2.getHasChildren(), v1Bundle.getHasChildren());
		assertSame(entityBundleV2.getAccessControlList(), v1Bundle.getAccessControlList());
		assertSame(entityBundleV2.getFileHandles(), v1Bundle.getFileHandles());
		assertSame(entityBundleV2.getTableBundle(), v1Bundle.getTableBundle());
		assertSame(entityBundleV2.getRootWikiId(), v1Bundle.getRootWikiId());
		assertSame(entityBundleV2.getBenefactorAcl(), v1Bundle.getBenefactorAcl());
		assertSame(entityBundleV2.getDoiAssociation(), v1Bundle.getDoiAssociation());
		assertSame(entityBundleV2.getFileName(), v1Bundle.getFileName());
		assertSame(entityBundleV2.getThreadCount(), v1Bundle.getThreadCount());
		assertSame(entityBundleV2.getRestrictionInformation(), v1Bundle.getRestrictionInformation());
	}
}
