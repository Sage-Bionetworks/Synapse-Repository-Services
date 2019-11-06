package org.sagebionetworks.repo.web.service.metadata;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.model.DockerNodeDao;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.docker.DockerRepository;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.test.util.ReflectionTestUtils;

public class ExternalDockerRepoValidatorTest {

	private ExternalDockerRepoValidator provider;
	
	private static final long ENTITY_ID_LONG = 12345L;
	private static final long PARENT_ID_LONG = 98765L;
	private static final String PARENT_ID = "syn"+PARENT_ID_LONG;
	
	@Mock
	private NodeDAO nodeDAO;
	
	@Mock
	private DockerNodeDao dockerNodeDao;
	
	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		provider = new ExternalDockerRepoValidator();
		ReflectionTestUtils.setField(provider, "nodeDAO", nodeDAO);
		ReflectionTestUtils.setField(provider, "dockerNodeDao", dockerNodeDao);
		ReflectionTestUtils.setField(provider, "stackConfiguration", StackConfigurationSingleton.singleton());
		when(dockerNodeDao.getRepositoryNameForEntityId(KeyFactory.keyToString(ENTITY_ID_LONG))).thenReturn(null);
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
		when(nodeDAO.getNodeTypeById(PARENT_ID)).thenReturn(EntityType.project);
		EntityEvent event = new EntityEvent(type, null, null);
		provider.validateEntity(repo, event);
	}
	
	@Test(expected=InvalidModelException.class)
	public void testValidateEntityParentIsNotProject() throws Exception {
		DockerRepository repo = new DockerRepository();
		repo.setRepositoryName("quay.io/uname/myrepo");
		repo.setParentId(PARENT_ID);
		EventType type = EventType.CREATE;
		when(nodeDAO.getNodeTypeById(PARENT_ID)).thenReturn(EntityType.folder);
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
	
	@Test
	public void testValidateEntityUpdate() throws Exception {
		DockerRepository repo = new DockerRepository();
		repo.setRepositoryName("quay.io/uname/myrepo");
		repo.setId(KeyFactory.keyToString(ENTITY_ID_LONG));
		repo.setParentId(PARENT_ID);
		
		EventType type = EventType.UPDATE;
		when(nodeDAO.getNodeTypeById(PARENT_ID)).thenReturn(EntityType.project);
		EntityEvent event = new EntityEvent(type, null, null);
		provider.validateEntity(repo, event);

	}
	
	@Test(expected=InvalidModelException.class)
	public void testIllegalConversionofManagedToUnmanagedRepo() throws Exception {
		DockerRepository repo = new DockerRepository();
		repo.setRepositoryName("quay.io/uname/myrepo");
		repo.setId(KeyFactory.keyToString(ENTITY_ID_LONG));
		repo.setParentId(PARENT_ID);
		EventType type = EventType.UPDATE;
		when(nodeDAO.getNodeTypeById(PARENT_ID)).thenReturn(EntityType.project);
		when(dockerNodeDao.getRepositoryNameForEntityId(KeyFactory.keyToString(ENTITY_ID_LONG))).
			thenReturn("docker.synapse.org/syn192837/repo-name");

		EntityEvent event = new EntityEvent(type, null, null);
		provider.validateEntity(repo, event);

	}

}
