package org.sagebionetworks.repo.web.service;

import static org.mockito.Mockito.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityBundleCreate;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class EntityBundleServiceImplTest {
	
	EntityBundleService entityBundleService;
	
	private static final String TEST_USER1 = AuthorizationConstants.TEST_USER_NAME;
	
	private ServiceProvider mockServiceProvider;
	private EntityService mockEntityService;
	
	Project project;
	Study study;
	Study studyWithId;
	Annotations annos;
	AccessControlList acl;
	
	EntityBundle responseBundle;
	
	private static final String DUMMY_STUDY_1 = "Test Study 1";
	private static final String DUMMY_PROJECT = "Test Project";
	private static final String STUDY_ID = "1";
	private static final long BOOTSTRAP_USER_GROUP_ID = 0L;
	
	
	
	@Before
	public void setUp() {
		// Mocks
		mockServiceProvider = mock(ServiceProvider.class);
		mockEntityService = mock(EntityService.class);
		
		entityBundleService = new EntityBundleServiceImpl(mockServiceProvider);
		
		// Entities
		project = new Project();
		project.setName(DUMMY_PROJECT);
		project.setEntityType(project.getClass().getName());
		
		study = new Study();
		study.setName(DUMMY_STUDY_1);
		study.setEntityType(study.getClass().getName());
		study.setParentId(project.getId());
		
		studyWithId = new Study();
		studyWithId.setName(DUMMY_STUDY_1);
		studyWithId.setEntityType(study.getClass().getName());
		studyWithId.setParentId(project.getId());
		studyWithId.setId(STUDY_ID);
		
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
	}

	@Test
	public void testCreateEntityBundle() throws NameConflictException, JSONObjectAdapterException, ServletException, IOException, NotFoundException, DatastoreException, ConflictingUpdateException, InvalidModelException, UnauthorizedException, ACLInheritanceException, ParseException {
		String activityId = "123";
		when(mockEntityService.getEntity(eq(TEST_USER1), eq(STUDY_ID), any(HttpServletRequest.class))).thenReturn(studyWithId);
		when(mockEntityService.createEntity(eq(TEST_USER1), eq(study), eq(activityId), any(HttpServletRequest.class))).thenReturn(studyWithId);
		when(mockEntityService.getEntityACL(eq(STUDY_ID), eq(TEST_USER1), any(HttpServletRequest.class))).thenReturn(acl);
		when(mockEntityService.createOrUpdateEntityACL(eq(TEST_USER1), eq(acl), anyString(), any(HttpServletRequest.class))).thenReturn(acl);
		when(mockEntityService.getEntityAnnotations(eq(TEST_USER1), eq(STUDY_ID), any(HttpServletRequest.class))).thenReturn(new Annotations());
		when(mockEntityService.updateEntityAnnotations(eq(TEST_USER1), eq(STUDY_ID), eq(annos), any(HttpServletRequest.class))).thenReturn(annos);
		when(mockServiceProvider.getEntityService()).thenReturn(mockEntityService);
		
		// Create the bundle, verify contents
		EntityBundleCreate ebc = new EntityBundleCreate();
		
		ebc.setEntity(study);
		ebc.setAnnotations(annos);
		ebc.setAccessControlList(acl);
		
		EntityBundle eb = entityBundleService.createEntityBundle(TEST_USER1, ebc, activityId, null);		
		Study s2 = (Study) eb.getEntity();
		assertNotNull(s2);
		assertEquals(study.getName(), s2.getName());
		
		Annotations a2 = eb.getAnnotations();
		assertNotNull(a2);
		assertEquals("Retrieved Annotations in bundle do not match original ones", annos.getStringAnnotations(), a2.getStringAnnotations());
		assertEquals("Retrieved Annotations in bundle do not match original ones", annos.getDoubleAnnotations(), a2.getDoubleAnnotations());
		
		AccessControlList acl2 = eb.getAccessControlList();
		assertNotNull(acl2);
		assertEquals("Retrieved ACL in bundle does not match original one", acl.getResourceAccess(), acl2.getResourceAccess());
	
		verify(mockEntityService).createEntity(eq(TEST_USER1), eq(study), eq(activityId), any(HttpServletRequest.class));
		verify(mockEntityService).updateEntityAnnotations(eq(TEST_USER1), eq(STUDY_ID), eq(annos), any(HttpServletRequest.class));
		verify(mockEntityService).createOrUpdateEntityACL(eq(TEST_USER1), eq(acl), anyString(), any(HttpServletRequest.class));
	}
	
	@Test
	public void testUpdateEntityBundle() throws NameConflictException, JSONObjectAdapterException, ServletException, IOException, NotFoundException, DatastoreException, ConflictingUpdateException, InvalidModelException, UnauthorizedException, ACLInheritanceException, ParseException {
		Annotations annosWithId = new Annotations();
		annosWithId.setId(STUDY_ID);
		String activityId = "1";
			
		when(mockEntityService.getEntity(eq(TEST_USER1), eq(STUDY_ID), any(HttpServletRequest.class))).thenReturn(studyWithId);
		when(mockEntityService.updateEntity(eq(TEST_USER1), eq(study), eq(false), eq(activityId), any(HttpServletRequest.class))).thenReturn(studyWithId);
		when(mockEntityService.getEntityACL(eq(STUDY_ID), eq(TEST_USER1), any(HttpServletRequest.class))).thenReturn(acl);
		when(mockEntityService.createOrUpdateEntityACL(eq(TEST_USER1), eq(acl), anyString(), any(HttpServletRequest.class))).thenReturn(acl);
		when(mockEntityService.getEntityAnnotations(eq(TEST_USER1), eq(STUDY_ID), any(HttpServletRequest.class))).thenReturn(annosWithId);
		when(mockEntityService.updateEntityAnnotations(eq(TEST_USER1), eq(STUDY_ID), eq(annos), any(HttpServletRequest.class))).thenReturn(annos);
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
		
		Study s2 = (Study) eb.getEntity();
		assertNotNull(s2);
		assertEquals(study.getName(), s2.getName());
		
		Annotations a2 = eb.getAnnotations();
		assertNotNull(a2);
		assertEquals("Retrieved Annotations in bundle do not match original ones", annos.getStringAnnotations(), a2.getStringAnnotations());
		assertEquals("Retrieved Annotations in bundle do not match original ones", annos.getDoubleAnnotations(), a2.getDoubleAnnotations());
		
		AccessControlList acl2 = eb.getAccessControlList();
		assertNotNull(acl2);
		assertEquals("Retrieved ACL in bundle does not match original one", acl.getResourceAccess(), acl2.getResourceAccess());
	
		verify(mockEntityService).updateEntity(eq(TEST_USER1), eq(study), eq(false), eq(activityId), any(HttpServletRequest.class));
		verify(mockEntityService).updateEntityAnnotations(eq(TEST_USER1), eq(STUDY_ID), eq(annos), any(HttpServletRequest.class));
		verify(mockEntityService).createOrUpdateEntityACL(eq(TEST_USER1), eq(acl), anyString(), any(HttpServletRequest.class));
	}
	
}
