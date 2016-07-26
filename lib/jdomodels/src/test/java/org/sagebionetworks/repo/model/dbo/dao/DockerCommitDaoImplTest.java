package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DockerCommitDao;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.docker.DockerCommit;
import org.sagebionetworks.repo.model.docker.DockerCommitSortBy;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DockerCommitDaoImplTest {
	@Autowired
	private NodeDAO nodeDao;
	
	@Autowired
	private DockerCommitDao dockerCommitDao;
	
	private Long creatorUserGroupId;
	private List<String> toDelete;
	private Node dockerRepository;
	
	@Before
	public void before(){
		creatorUserGroupId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		toDelete = new ArrayList<String>();
		
		Node project = NodeTestUtils.createNew("Project", creatorUserGroupId);
		project.setNodeType(EntityType.project);
		project = nodeDao.createNewNode(project);
		toDelete.add(project.getId());
		assertNotNull(project);
		// add a repo
		dockerRepository = NodeTestUtils.createNew("dockerRepository", creatorUserGroupId);
		dockerRepository.setNodeType(EntityType.dockerrepo);
		dockerRepository.setParentId(project.getId());
		dockerRepository = nodeDao.createNewNode(dockerRepository);
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
		assertEquals(0, dockerCommitDao.countDockerCommits(dockerRepository.getId()));
		
		DockerCommit commit = new DockerCommit();
		Date createdOn = new Date();
		String digest = "sha256:abcdef0123456789";
		String tag = "latest";
		commit.setCreatedOn(createdOn);
		commit.setDigest(digest);
		commit.setTag(tag);
		String newNodeEtag = dockerCommitDao.createDockerCommit(dockerRepository.getId(), creatorUserGroupId, commit);
		assertNotNull(newNodeEtag);
		assertFalse(newNodeEtag.equals(dockerRepository.getETag()));
		
		assertEquals(1, dockerCommitDao.countDockerCommits(dockerRepository.getId()));			
		
		List<DockerCommit> commits = dockerCommitDao.listDockerCommits(
				dockerRepository.getId(), DockerCommitSortBy.CREATED_ON, 
				/*ascending*/true, 10, 0);
		
		assertEquals(1, commits.size());
		assertEquals(commit, commits.get(0));
	}
	

}
