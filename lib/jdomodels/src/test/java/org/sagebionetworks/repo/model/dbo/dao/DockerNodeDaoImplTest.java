package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DockerNodeDao;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DockerNodeDaoImplTest {
	@Autowired
	private NodeDAO nodeDao;
	
	@Autowired
	private DockerNodeDao dockerNodeDao;
	
	private Long creatorUserGroupId;
	private List<String> toDelete;
	
	@Before
	public void before(){
		creatorUserGroupId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		toDelete = new ArrayList<String>();
	}
	
	@After
	public void after(){
		if(toDelete != null && nodeDao != null){
			for(String id:  toDelete){
				// Delete each
				try{
					nodeDao.delete(id);
				}catch (Exception e) {}
			}
		}
	}
	
	@Test
	public void testRoundTrip() {
		Node project = NodeTestUtils.createNew("Project", creatorUserGroupId);
		project.setNodeType(EntityType.project);
		project = nodeDao.createNewNode(project);
		toDelete.add(project.getId());
		assertNotNull(project);
		// add a repo
		Node dockerRepository = NodeTestUtils.createNew("dockerRepository", creatorUserGroupId);
		dockerRepository.setNodeType(EntityType.dockerrepo);
		dockerRepository.setParentId(project.getId());
		dockerRepository = nodeDao.createNewNode(dockerRepository);

		String repositoryName = "docker.synapse.org/"+project.getId()+"/repo-name";
		dockerNodeDao.createRepositoryName(dockerRepository.getId(), repositoryName);
		
		String retrievedId = dockerNodeDao.getEntityIdForRepositoryName(repositoryName);
		assertEquals(dockerRepository.getId(), retrievedId);
		
		String retrievedName = dockerNodeDao.getRepositoryNameForEntityId(dockerRepository.getId());
		assertEquals(repositoryName, retrievedName);
	}
	
	@Test
	public void testNonExistentRepo() {
		assertNull(dockerNodeDao.getEntityIdForRepositoryName("this/repo/doesnt/exist"));
		assertNull(dockerNodeDao.getRepositoryNameForEntityId("syn987654321"));
	}

}
