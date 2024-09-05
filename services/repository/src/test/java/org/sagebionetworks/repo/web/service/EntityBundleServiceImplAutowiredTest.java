package org.sagebionetworks.repo.web.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundle;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundleCreate;
import org.sagebionetworks.repo.model.entitybundle.v2.EntityBundleRequest;
import org.sagebionetworks.repo.queryparser.ParseException;
import org.sagebionetworks.repo.service.EntityBundleService;
import org.sagebionetworks.repo.service.EntityService;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class EntityBundleServiceImplAutowiredTest {

	@Autowired
	private EntityBundleService entityBundleService;
	
	@Autowired
	private EntityService entityService;
	
	
	private Long adminUserId;
	private Project project;
	private Folder studyWithId;
	
	
	@BeforeEach
	public void beforeEach() {
		adminUserId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		
		project = entityService.createEntity(adminUserId, new Project(), null);
		
		studyWithId = new Folder()
				.setParentId(project.getId());
	}
	
	// Added to reproduce comments in: https://sagebionetworks.jira.com/browse/PLFM-8272
	@Test
	public void testGetEntityBundleWithActivityWithEntityWithNullActivity() throws NotFoundException, DatastoreException, UnauthorizedException, 
			ACLInheritanceException, ParseException {
		EntityBundleRequest request = new EntityBundleRequest().setIncludeActivity(true).setIncludeEntity(true);
		
		EntityBundle eb = entityBundleService.getEntityBundle(adminUserId, project.getId(), request);
		
		EntityBundle expected = new EntityBundle()
				.setEntity(project)
				.setEntityType(EntityType.project)
				.setActivity(null);
		assertEquals(expected, eb);
	}
	
	// Added to reproduce: https://sagebionetworks.jira.com/browse/PLFM-8235.
	@Test
	public void testCreateEntityBundleWithNullAnnotations() throws ConflictingUpdateException, DatastoreException, InvalidModelException, 
			UnauthorizedException, NotFoundException, ACLInheritanceException, ParseException {
		EntityBundleCreate ebc = new EntityBundleCreate()
				.setEntity(studyWithId)
				.setAnnotations(new Annotations().setAnnotations(null));
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			entityBundleService.createEntityBundle(adminUserId, ebc, null);
		}).getMessage();
				
		assertEquals("entityBundleCreate.annotations.annotations is required.", errorMessage);
	}
	
}
