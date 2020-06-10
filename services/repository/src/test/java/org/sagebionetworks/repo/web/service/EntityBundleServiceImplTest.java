package org.sagebionetworks.repo.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.RestrictableObjectType;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.annotation.v2.AnnotationsV2Translator;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.discussion.EntityThreadCount;
import org.sagebionetworks.repo.model.discussion.EntityThreadCounts;
import org.sagebionetworks.repo.model.doi.v2.DoiAssociation;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundle;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundleRequest;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.TableBundle;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.service.dataaccess.DataAccessService;
import org.sagebionetworks.repo.web.service.discussion.DiscussionService;
import org.sagebionetworks.repo.web.service.table.TableServices;

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
	private org.sagebionetworks.repo.model.Annotations annos;
	private Annotations annotationsV2;
	private AccessControlList acl;
	private EntityThreadCounts threadCounts;

	private org.sagebionetworks.repo.model.EntityBundle responseBundle;

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
		annos = new org.sagebionetworks.repo.model.Annotations();
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
		responseBundle = new org.sagebionetworks.repo.model.EntityBundle();
		responseBundle.setEntity(study);
		responseBundle.setAnnotations(annos);
		responseBundle.setAccessControlList(acl);
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
		EntityBundleRequest request = new EntityBundleRequest();
		request.setIncludeTableBundle(true);
		// call under test
		EntityBundle bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, request);
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
		EntityBundleRequest request = new EntityBundleRequest();
		request.setIncludeTableBundle(true);
		// call under test
		EntityBundle bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, versionNumber, request);
		assertNotNull(bundle);
		assertEquals(tableBundle, bundle.getTableBundle());
		verify(mockTableService).getTableBundle(idAndVersion);
	}

	@Test
	public void testDoiAssociation() throws Exception {
		String entityId = "syn123";
		EntityBundleRequest request = new EntityBundleRequest();
		request.setIncludeDOIAssociation(true);
		DoiAssociation doi = new DoiAssociation();
		doi.setObjectType(ObjectType.ENTITY);
		doi.setObjectId(entityId);
		doi.setObjectVersion(null);
		when(mockDoiServiceV2.getDoiAssociation(entityId, ObjectType.ENTITY, null)).thenReturn(doi);
		// Call under test
		EntityBundle bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, request);
		assertNotNull(bundle);
		assertEquals(doi, bundle.getDoiAssociation());
	}

	@Test
	public void testDoiAssociationForUnversionedRequestForVersionable() throws Exception {
		// Must retrieve entity to determine if it is VersionableEntity
		EntityBundleRequest request = new EntityBundleRequest();
		request.setIncludeEntity(true);
		request.setIncludeDOIAssociation(true);
		DoiAssociation doi = new DoiAssociation();
		doi.setObjectType(ObjectType.ENTITY);
		doi.setObjectId(FILE_ID);
		doi.setObjectVersion(FILE_VERSION);

		when(mockEntityService.getEntity(eq(TEST_USER1), eq(FILE_ID))).thenReturn(file);
		when(mockDoiServiceV2.getDoiAssociation(FILE_ID, ObjectType.ENTITY, FILE_VERSION)).thenReturn(doi);

		// Call under test. Note the bundle requests 'null' version
		EntityBundle bundle = entityBundleService.getEntityBundle(TEST_USER1, FILE_ID, request);

		verify(mockDoiServiceV2, never()).getDoiAssociation(FILE_ID, ObjectType.ENTITY, null);
		assertNotNull(bundle);
		assertEquals(doi, bundle.getDoiAssociation());
	}

	@Test
	public void testDoiV2NotFound() throws Exception {
		EntityBundleRequest request = new EntityBundleRequest();
		request.setIncludeDOIAssociation(true);
		String entityId = "syn123";
		when(mockDoiServiceV2.getDoiAssociation(entityId, ObjectType.ENTITY, null)).thenThrow(new NotFoundException());
		// Call under test
		EntityBundle bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, request);
		assertNotNull(bundle);
		assertNull(bundle.getDoiAssociation());
		verify(mockDoiServiceV2).getDoiAssociation(entityId, ObjectType.ENTITY, null);
	}
	
	@Test
	public void testRootWikiId() throws Exception {
		String entityId = "syn123";
		EntityBundleRequest request = new EntityBundleRequest();
		request.setIncludeRootWikiId(true);
		String rootWikiId = "456";
		WikiPageKey key = new WikiPageKey();
		key.setOwnerObjectId(entityId);
		key.setOwnerObjectType(ObjectType.ENTITY);
		key.setWikiPageId(rootWikiId);
		when(mockWikiService.getRootWikiKey(TEST_USER1, entityId, ObjectType.ENTITY)).thenReturn(key);
		EntityBundle bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, request);
		assertNotNull(bundle);
		assertEquals(rootWikiId, bundle.getRootWikiId());
	}
	
	@Test
	public void testRootWikiIdNotFound() throws Exception {
		String entityId = "syn123";
		EntityBundleRequest request = new EntityBundleRequest();
		request.setIncludeRootWikiId(true);
		when(mockWikiService.getRootWikiKey(TEST_USER1, entityId, ObjectType.ENTITY)).thenThrow(new NotFoundException("does not exist"));
		EntityBundle bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, request);
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
		EntityBundleRequest request = new EntityBundleRequest();
		request.setIncludeBenefactorACL(true);
		when(mockEntityService.getEntityACL(anyString(), anyLong())).thenReturn(acl);
		EntityBundle bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, request);
		assertNotNull(bundle);
		assertEquals(acl, bundle.getBenefactorAcl());
	}

	@Test
	public void testGetBenefactorAclInherited() throws Exception {
		AccessControlList acl = new AccessControlList();
		acl.setId("456");
		String entityId = "syn123";
		String benefactorId = "syn456";
		EntityBundleRequest request = new EntityBundleRequest();
		request.setIncludeBenefactorACL(true);
		// this entity inherits its permissions.
		when(mockEntityService.getEntityACL(entityId, TEST_USER1)).thenThrow(new ACLInheritanceException("Has a benefactor", benefactorId));
		// return the benefactor ACL.
		when(mockEntityService.getEntityACL(benefactorId, TEST_USER1)).thenReturn(acl);
		EntityBundle bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, request);
		assertNotNull(bundle);
		assertEquals(acl, bundle.getBenefactorAcl());
	}
	
	@Test
	public void testFileNameNoOverride() throws Exception {
		String entityId = "syn123";
		EntityBundleRequest request = new EntityBundleRequest();
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
		EntityBundle bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, request);
		assertNotNull(bundle);
		assertEquals(fileName, bundle.getFileName());
	}
	
	@Test
	public void testFileNameWithOverride() throws Exception {
		long dataFileHandleId = 101L;
		String fileName = "foo.txt";
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

		String entityId = "syn123";
		EntityBundleRequest request = new EntityBundleRequest();
		request.setIncludeFileName(true);
		FileEntity entity = new FileEntity();
		String fileNameOverride = "foo.txt";
		entity.setFileNameOverride(fileNameOverride);
		when(mockEntityService.getEntity(TEST_USER1, entityId)).thenReturn(entity);
		when(mockEntityService.getEntityFileHandlesForCurrentVersion(TEST_USER1, entityId)).thenReturn(fhr);
		EntityBundle bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, request);
		assertNotNull(bundle);
		assertEquals(fileNameOverride, bundle.getFileName());
	}

	@Test
	public void testThreadCountNoEntityRef() throws Exception {
		// thread count
		threadCounts = new EntityThreadCounts();
		when(mockDiscussionService.getThreadCounts(eq(TEST_USER1), any(EntityIdList.class))).thenReturn(threadCounts);

		String entityId = "syn123";
		EntityBundleRequest request = new EntityBundleRequest();
		request.setIncludeThreadCount(true);
		threadCounts.setList(new LinkedList<EntityThreadCount>());
		EntityBundle bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, request);
		assertNotNull(bundle);
		assertEquals((Long)0L, bundle.getThreadCount());
	}

	@Test
	public void testThreadCount() throws Exception {
		// thread count
		threadCounts = new EntityThreadCounts();
		when(mockDiscussionService.getThreadCounts(eq(TEST_USER1), any(EntityIdList.class))).thenReturn(threadCounts);

		String entityId = "syn123";
		EntityBundleRequest request = new EntityBundleRequest();
		request.setIncludeThreadCount(true);
		EntityThreadCount threadCount = new EntityThreadCount();
		threadCount.setEntityId(entityId);
		threadCount.setCount(1L);
		threadCounts.setList(Arrays.asList(threadCount));
		EntityBundle bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, request);
		assertNotNull(bundle);
		assertEquals((Long)1L, bundle.getThreadCount());
	}

	@Test
	public void testThreadCountUnexpectedResultListSize() throws Exception {
		// thread count
		threadCounts = new EntityThreadCounts();
		when(mockDiscussionService.getThreadCounts(eq(TEST_USER1), any(EntityIdList.class))).thenReturn(threadCounts);

		String entityId = "syn123";
		EntityBundleRequest request = new EntityBundleRequest();
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
		EntityBundleRequest bundleV2Request = new EntityBundleRequest();
		bundleV2Request.setIncludeRestrictionInformation(true);
		RestrictionInformationRequest request = new RestrictionInformationRequest();
		request.setObjectId(entityId);
		request.setRestrictableObjectType(RestrictableObjectType.ENTITY);
		RestrictionInformationResponse response = new RestrictionInformationResponse();
		when(mockDataAccessService.getRestrictionInformation(TEST_USER1, request)).thenReturn(response );
		EntityBundle bundle = entityBundleService.getEntityBundle(TEST_USER1, entityId, bundleV2Request);
		assertEquals(response, bundle.getRestrictionInformation());
		verify(mockDataAccessService).getRestrictionInformation(TEST_USER1, request);
	}
}
