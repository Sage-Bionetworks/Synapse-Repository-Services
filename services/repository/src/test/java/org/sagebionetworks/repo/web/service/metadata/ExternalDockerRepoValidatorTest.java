package org.sagebionetworks.repo.web.service.metadata;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.springframework.test.util.ReflectionTestUtils;

public class ExternalDockerRepoValidatorTest {

	private ExternalDockerRepoValidator provider;
	
	private static final long PARENT_ID_LONG = 98765L;
	private static final String PARENT_ID = "syn"+PARENT_ID_LONG;
	
	@Mock
	private NodeDAO nodeDAO;
	
	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		provider = new ExternalDockerRepoValidator();
		ReflectionTestUtils.setField(provider, "nodeDAO", nodeDAO);
	}
	
	@Test
	public void testIsReserved() throws Exception {
		assertTrue(ExternalDockerRepoValidator.isReserved("docker.synapse.org"));
		assertTrue(ExternalDockerRepoValidator.isReserved("something.else.synapse.org"));
		
		assertFalse(ExternalDockerRepoValidator.isReserved(null));
		assertFalse(ExternalDockerRepoValidator.isReserved("quay.io"));
		assertFalse(ExternalDockerRepoValidator.isReserved("synapse.org.com"));
	}
	
	@Test
	public void testValidateEntityHappyCase() throws Exception {
		DockerRepository repo = new DockerRepository();
		repo.setRepositoryName("quay.io/uname/myrepo");
		repo.setParentId(PARENT_ID);
		EventType type = EventType.CREATE;
		EntityHeader parentHeader = new EntityHeader();
		parentHeader.setId(PARENT_ID);
		parentHeader.setType(Project.class.getName());
		List<EntityHeader> parent = Collections.singletonList(parentHeader);
		when(nodeDAO.getEntityHeader(Collections.singleton(PARENT_ID_LONG))).thenReturn(parent);
		EntityEvent event = new EntityEvent(type, null, null);
		provider.validateEntity(repo, event);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testValidateEntityParentIsNotProject() throws Exception {
		DockerRepository repo = new DockerRepository();
		repo.setRepositoryName("quay.io/uname/myrepo");
		repo.setParentId(PARENT_ID);
		EventType type = EventType.CREATE;
		EntityHeader parentHeader = new EntityHeader();
		parentHeader.setId(PARENT_ID);
		parentHeader.setType(Folder.class.getName());
		List<EntityHeader> parent = Collections.singletonList(parentHeader);
		when(nodeDAO.getEntityHeader(Collections.singleton(PARENT_ID_LONG))).thenReturn(parent);
		EntityEvent event = new EntityEvent(type, null, null);
		provider.validateEntity(repo, event);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testValidateEntityMissingParentId() throws Exception {
		DockerRepository repo = new DockerRepository();
		repo.setRepositoryName("quay.io/uname/myrepo");
		EventType type = EventType.CREATE;
		EntityEvent event = new EntityEvent(type, null, null);
		provider.validateEntity(repo, event);
	}
	
	@Test(expected=InvalidModelException.class)
	public void testValidateEntityManagedHost() throws Exception {
		DockerRepository repo = new DockerRepository();
		repo.setRepositoryName("docker.synapse.org/uname/myrepo");
		repo.setParentId(PARENT_ID);
		EventType type = EventType.CREATE;
		EntityEvent event = new EntityEvent(type, null, null);
		provider.validateEntity(repo, event);
	}
	
	@Test(expected=InvalidModelException.class)
	public void testValidateEntityReservedHost() throws Exception {
		DockerRepository repo = new DockerRepository();
		repo.setRepositoryName("newrepo.synapse.org/uname/myrepo");
		repo.setParentId(PARENT_ID);
		EventType type = EventType.CREATE;
		EntityEvent event = new EntityEvent(type, null, null);
		provider.validateEntity(repo, event);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testValidateEntityIllegalName() throws Exception {
		DockerRepository repo = new DockerRepository();
		repo.setRepositoryName("/invalid/");
		repo.setParentId(PARENT_ID);
		EventType type = EventType.CREATE;
		EntityEvent event = new EntityEvent(type, null, null);
		provider.validateEntity(repo, event);
	}
	
	public void testValidateEntityUpdate() throws Exception {
		DockerRepository repo = new DockerRepository();
		repo.setRepositoryName("quay.io/uname/myrepo");
		repo.setParentId(PARENT_ID);
		EventType type = EventType.UPDATE;
		EntityHeader parentHeader = new EntityHeader();
		parentHeader.setId(PARENT_ID);
		parentHeader.setType(Project.class.getName());
		List<EntityHeader> parent = Collections.singletonList(parentHeader);
		when(nodeDAO.getEntityHeader(Collections.singleton(PARENT_ID_LONG))).thenReturn(parent);
		EntityEvent event = new EntityEvent(type, null, null);
		provider.validateEntity(repo, event);

	}
}
