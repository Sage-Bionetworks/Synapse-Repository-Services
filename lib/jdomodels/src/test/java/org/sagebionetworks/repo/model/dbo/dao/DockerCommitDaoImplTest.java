package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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
	private Node project;
	private Node dockerRepository1;
	private Node dockerRepository2;
	
	@Before
	public void before() throws Exception {
		creatorUserGroupId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		
		project = NodeTestUtils.createNew("Project", creatorUserGroupId);
		project.setNodeType(EntityType.project);
		project = nodeDao.createNewNode(project);
		assertNotNull(project);
		// add a repo
		Thread.sleep(10L); //make sure the rep has a later createdOn date
		dockerRepository1 = NodeTestUtils.createNew("dockerRepository", creatorUserGroupId);
		dockerRepository1.setNodeType(EntityType.dockerrepo);
		dockerRepository1.setParentId(project.getId());
		dockerRepository1 = nodeDao.createNewNode(dockerRepository1);
		
		// add another repo
		Thread.sleep(10L); //make sure the rep has a later createdOn date
		dockerRepository2 = NodeTestUtils.createNew("dockerRepository2", creatorUserGroupId);
		dockerRepository2.setNodeType(EntityType.dockerrepo);
		dockerRepository2.setParentId(project.getId());
		dockerRepository2 = nodeDao.createNewNode(dockerRepository2);
	}
	
	@After
	public void after() {
		nodeDao.delete(project.getId());
	}
	
	private static DockerCommit createCommit(Date date, String tag, String digest) {
		DockerCommit commit = new DockerCommit();
		commit.setCreatedOn(date);
		commit.setTag(tag);
		commit.setDigest(digest);
		return commit;
	}
	
	@Test
	public void testRoundTrip() {
		assertEquals(0, dockerCommitDao.countDockerCommits(dockerRepository1.getId()));
		
		Date createdOn = new Date();
		String digest = "sha256:abcdef0123456789";
		String tag = "latest";
		DockerCommit commit = createCommit(createdOn, tag, digest);
		String newNodeEtag = dockerCommitDao.createDockerCommit(dockerRepository1.getId(), creatorUserGroupId, commit);
		assertNotNull(newNodeEtag);
		
		assertEquals(1, dockerCommitDao.countDockerCommits(dockerRepository1.getId()));			
		
		List<DockerCommit> commits = dockerCommitDao.listDockerTags(
				dockerRepository1.getId(), DockerCommitSortBy.CREATED_ON, 
				/*ascending*/true, 10, 0);
		
		assertEquals(1, commits.size());
		assertEquals(commit, commits.get(0));
		
		// make sure we've affected the node correctly
		Node retrievedRepo = nodeDao.getNode(dockerRepository1.getId());
		assertFalse(newNodeEtag.equals(dockerRepository1.getETag()));
		assertEquals(newNodeEtag, retrievedRepo.getETag());
		assertEquals(createdOn, retrievedRepo.getModifiedOn());
		assertEquals(creatorUserGroupId, retrievedRepo.getModifiedByPrincipalId());
		
		// let's make sure were're not accidentally updating other nodes!!
		Node otherRepository = nodeDao.getNode(dockerRepository2.getId());
		assertFalse(newNodeEtag.equals(otherRepository.getETag()));
		assertFalse(createdOn.equals(otherRepository.getModifiedOn()));
	}

	@Test
	public void createDockerCommitMissingTag() {
		assertEquals(0, dockerCommitDao.countDockerCommits(dockerRepository1.getId()));
		
		Date createdOn = new Date();
		String digest = "sha256:abcdef0123456789";
		String tag = null;
		DockerCommit commit = createCommit(createdOn, tag, digest);
		String newNodeEtag = dockerCommitDao.createDockerCommit(dockerRepository1.getId(), creatorUserGroupId, commit);
		assertNotNull(newNodeEtag);

		List<DockerCommit> commits = dockerCommitDao.listCommitsByOwnerAndDigest(
				dockerRepository1.getId(), digest);

		assertEquals(1, commits.size());
		assertEquals(commit, commits.get(0));

		// make sure we've affected the node correctly
		Node retrievedRepo = nodeDao.getNode(dockerRepository1.getId());
		assertNotEquals(newNodeEtag, dockerRepository1.getETag());
		assertEquals(newNodeEtag, retrievedRepo.getETag());
		assertEquals(createdOn, retrievedRepo.getModifiedOn());
		assertEquals(creatorUserGroupId, retrievedRepo.getModifiedByPrincipalId());
	}

	@Test
	public void testListDockerCommitsSortedBy() throws Exception {
		long now = System.currentTimeMillis();
		DockerCommit commitA = createCommit(new Date(now), "A", UUID.randomUUID().toString());
		dockerCommitDao.createDockerCommit(dockerRepository1.getId(), creatorUserGroupId, commitA);
		DockerCommit commitB = createCommit(new Date(now+1000L), "B", UUID.randomUUID().toString());
		dockerCommitDao.createDockerCommit(dockerRepository1.getId(), creatorUserGroupId, commitB);
		
		DockerCommit commitZ = createCommit(new Date(now+2000L), "Z", UUID.randomUUID().toString());
		dockerCommitDao.createDockerCommit(dockerRepository2.getId(), creatorUserGroupId, commitZ);
		DockerCommit commitY = createCommit(new Date(now+3000L), "Y", UUID.randomUUID().toString());
		dockerCommitDao.createDockerCommit(dockerRepository2.getId(), creatorUserGroupId, commitY);
		
		assertEquals(2, dockerCommitDao.countDockerCommits(dockerRepository1.getId()));
		assertEquals(2, dockerCommitDao.countDockerCommits(dockerRepository2.getId()));
		
		// sort by created_on, ascending
		assertEquals(
				Arrays.asList(new DockerCommit[]{commitA, commitB}),
				dockerCommitDao.listDockerTags(dockerRepository1.getId(),
						DockerCommitSortBy.CREATED_ON, /*ascending*/true, 10, 0)
				);
		assertEquals(
				Arrays.asList(new DockerCommit[]{commitZ, commitY}),
				dockerCommitDao.listDockerTags(dockerRepository2.getId(),
						DockerCommitSortBy.CREATED_ON, /*ascending*/true, 10, 0)
				);
		
		// sort by created_on, descending
		assertEquals(
				Arrays.asList(new DockerCommit[]{commitB, commitA}),
				dockerCommitDao.listDockerTags(dockerRepository1.getId(),
						DockerCommitSortBy.CREATED_ON, /*ascending*/false, 10, 0)
				);
		assertEquals(
				Arrays.asList(new DockerCommit[]{commitY, commitZ}),
				dockerCommitDao.listDockerTags(dockerRepository2.getId(),
						DockerCommitSortBy.CREATED_ON, /*ascending*/false, 10, 0)
				);

		// sort by tag, ascending
		assertEquals(
				Arrays.asList(new DockerCommit[]{commitA, commitB}),
				dockerCommitDao.listDockerTags(dockerRepository1.getId(),
						DockerCommitSortBy.TAG, /*ascending*/true, 10, 0)
				);
		assertEquals(
				Arrays.asList(new DockerCommit[]{commitY, commitZ}),
				dockerCommitDao.listDockerTags(dockerRepository2.getId(),
						DockerCommitSortBy.TAG, /*ascending*/true, 10, 0)
				);
		
		// sort by tag, descending
		assertEquals(
				Arrays.asList(new DockerCommit[]{commitB, commitA}),
				dockerCommitDao.listDockerTags(dockerRepository1.getId(),
						DockerCommitSortBy.TAG, /*ascending*/false, 10, 0)
				);
		assertEquals(
				Arrays.asList(new DockerCommit[]{commitZ, commitY}),
				dockerCommitDao.listDockerTags(dockerRepository2.getId(),
						DockerCommitSortBy.TAG, /*ascending*/false, 10, 0)
				);

	}
	
	@Test
	public void testListDockerCommitsLatestTag() throws Exception {
		long now = System.currentTimeMillis();
		DockerCommit earlierCommit = createCommit(new Date(now), "a", UUID.randomUUID().toString());
		dockerCommitDao.createDockerCommit(dockerRepository1.getId(), creatorUserGroupId, earlierCommit);
		DockerCommit laterCommit = createCommit(new Date(now+1000L), "a", UUID.randomUUID().toString());
		dockerCommitDao.createDockerCommit(dockerRepository1.getId(), creatorUserGroupId, laterCommit);
		
		List<DockerCommit> commits = dockerCommitDao.listDockerTags(dockerRepository1.getId(),
				DockerCommitSortBy.TAG, /*ascending*/false, 10, 0);
		assertEquals(Collections.singletonList(laterCommit), commits);
		
		assertEquals(1, dockerCommitDao.countDockerCommits(dockerRepository1.getId()));
	}

	@Test
	public void testListDockerCommitsLimitOffset() throws Exception {
		long now = System.currentTimeMillis();
		DockerCommit commitA = createCommit(new Date(now), "A", UUID.randomUUID().toString());
		dockerCommitDao.createDockerCommit(dockerRepository1.getId(), creatorUserGroupId, commitA);
		DockerCommit commitB = createCommit(new Date(now+1000L), "B", UUID.randomUUID().toString());
		dockerCommitDao.createDockerCommit(dockerRepository1.getId(), creatorUserGroupId, commitB);
		
		List<DockerCommit> commits = dockerCommitDao.listDockerTags(dockerRepository1.getId(),
				DockerCommitSortBy.TAG, /*ascending*/true, 1, 0);
		assertEquals(Collections.singletonList(commitA), commits);
		commits = dockerCommitDao.listDockerTags(dockerRepository1.getId(),
				DockerCommitSortBy.TAG, /*ascending*/true, 1, 1);
		assertEquals(Collections.singletonList(commitB), commits);
		commits = dockerCommitDao.listDockerTags(dockerRepository1.getId(),
				DockerCommitSortBy.TAG, /*ascending*/true, 10, 3);
		assertTrue(commits.isEmpty());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testCreateDockerCommitsMissingEntityId() {
		dockerCommitDao.createDockerCommit(null, 101L, createCommit(new Date(), "a", UUID.randomUUID().toString()));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testCreateDockerCommitsMissingCreatedOn() {
		dockerCommitDao.createDockerCommit(dockerRepository1.getId(), 101L, createCommit(null, "a", UUID.randomUUID().toString()));
	}
	
	
	@Test(expected=IllegalArgumentException.class)
	public void testCreateDockerCommitsMissingDigest() {
		dockerCommitDao.createDockerCommit(dockerRepository1.getId(), 101L, createCommit(new Date(), "a", null));
	}
	
	
	@Test(expected=IllegalArgumentException.class)
	public void testListDockerCommitsMissingEntityId() {
		dockerCommitDao.listDockerTags(null, DockerCommitSortBy.TAG, /*ascending*/true, 1, 0);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testListDockerCommitsMissingSortyBy() {
		dockerCommitDao.listDockerTags(dockerRepository1.getId(), null, /*ascending*/true, 1, 0);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCountDockerCommitsMissingEntityId() {
		dockerCommitDao.countDockerCommits(null);
	}
	
	@Test
	public void testListCommitsByOwnerAndDigest() throws Exception {
		long now = System.currentTimeMillis();
		DockerCommit commitA = createCommit(new Date(now), "A", UUID.randomUUID().toString());
		dockerCommitDao.createDockerCommit(dockerRepository1.getId(), creatorUserGroupId, commitA);
		DockerCommit commitB = createCommit(new Date(now+1000L), "B", UUID.randomUUID().toString());
		dockerCommitDao.createDockerCommit(dockerRepository1.getId(), creatorUserGroupId, commitB);
		
		DockerCommit commitY = createCommit(new Date(now+3000L), "Y", UUID.randomUUID().toString());
		dockerCommitDao.createDockerCommit(dockerRepository2.getId(), creatorUserGroupId, commitY);
		
		assertEquals(
			Collections.singletonList(commitA)
		,
			dockerCommitDao.listCommitsByOwnerAndDigest(dockerRepository1.getId(), commitA.getDigest())
		);
		
		assertTrue(dockerCommitDao.listCommitsByOwnerAndDigest(
				dockerRepository1.getId(), commitY.getDigest()).
				isEmpty());
	}

}
