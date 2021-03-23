package org.sagebionetworks.projectstats.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.object.snapshot.worker.utils.AclSnapshotUtils;
import org.sagebionetworks.repo.manager.CertifiedUserManager;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.EntityAclManager;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ProjectStat;
import org.sagebionetworks.repo.model.ProjectStatsDAO;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.Pair;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class ProjectStatsWorkerIntegrationTest {

	private static final long WAIT_FOR_STAT_CHANGE_MS = 10000L;

	@Autowired
	private EntityManager entityManager;
	@Autowired
	private UserManager userManager;
	@Autowired
	private TeamManager teamManager;
	@Autowired
	private SemaphoreManager semphoreManager;
	@Autowired
	private ProjectStatsDAO projectStatsDAO;
	@Autowired
	private FileHandleDao fileMetadataDao;
	@Autowired
	private V2WikiPageDao v2wikiPageDAO;
	@Autowired
	private TeamDAO teamDAO;
	@Autowired
	private EntityAclManager entityAclManager;
	@Autowired
	private CertifiedUserManager certifiedUserManager;
	@Autowired
	private IdGenerator idGenerator;

	private UserInfo adminUserInfo;
	private List<String> toDelete = Lists.newArrayList();
	private UserInfo userInfo;
	private Long userId;
	private Long[] userIds = new Long[4];
	private String teamId;
	private S3FileHandle handleOne;
	private V2WikiPage v2RootWiki;
	WikiPageKey rootWikikey;

	@Before
	public void before() throws NotFoundException {
		// Get the admin user
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		semphoreManager.releaseAllLocksAsAdmin(adminUserInfo);
		NewUser user = new NewUser();
		user.setUserName(UUID.randomUUID().toString());
		user.setEmail(user.getUserName() + "@xx.com");
		boolean acceptsTermsOfUse = true;
		userId = userManager.createOrGetTestUser(adminUserInfo, user, acceptsTermsOfUse).getId();
		boolean isCertified = true;
		certifiedUserManager.setUserCertificationStatus(adminUserInfo, userId, isCertified);
		userInfo = userManager.getUserInfo(userId);
		for (int i = 0; i < userIds.length; i++) {
			user = new NewUser();
			user.setUserName(UUID.randomUUID().toString());
			user.setEmail(user.getUserName() + "@xx.com");
			userIds[i] = userManager.createUser(user);
		}
	}

	@After
	public void after() throws Exception {
		if (adminUserInfo != null) {
			for (String id : toDelete) {
				try {
					entityManager.deleteEntity(adminUserInfo, id);
				} catch (Exception e) {
				}
			}
		}
		if (v2RootWiki != null) {
			v2wikiPageDAO.delete(rootWikikey);
		}
		
		if (handleOne != null) {
			fileMetadataDao.delete(handleOne.getId());
		}
		if (teamId != null) {
			teamDAO.delete(teamId);
		}
		if (userId != null) {
			userManager.deletePrincipal(adminUserInfo, userId);
		}
		for (Long userId2 : userIds) {
			if (userId2 != null) {
				userManager.deletePrincipal(adminUserInfo, userId2);
			}
		}
	}

	@Test
	public void testRoundTrip() throws Exception {
		// Create a project
		assertEquals(0, projectStatsDAO.getProjectStatsForUser(userId).size());

		Project project = new Project();
		project.setName(UUID.randomUUID().toString());
		String id = entityManager.createEntity(userInfo, project, null);
		project = entityManager.getEntity(userInfo, id, Project.class);
		toDelete.add(project.getId());

		// Wait for the project stat to be added
		List<ProjectStat> projectStatsForUser = TimeUtils.waitFor(WAIT_FOR_STAT_CHANGE_MS, 200L, new Callable<Pair<Boolean, List<ProjectStat>>>() {
			@Override
			public Pair<Boolean, List<ProjectStat>> call() throws Exception {
				List<ProjectStat> projectStatsForUser = projectStatsDAO.getProjectStatsForUser(userId);
				return Pair.create(projectStatsForUser.size() == 1, projectStatsForUser);
			}
		});
		ProjectStat projectStat = projectStatsForUser.get(0);
		assertEquals(KeyFactory.stringToKey(project.getId()).longValue(), projectStat.getProjectId());
		assertEquals(userId.longValue(), projectStat.getUserId());
	}

	@Test
	public void testSubEntityChange() throws Exception {
		ProjectStat projectStat = setupProject();

		// Create an entity
		Folder folder = new Folder();
		folder.setName("boundForTheTrashCan");
		folder.setParentId(KeyFactory.keyToString(projectStat.getProjectId()));
		String folderId = entityManager.createEntity(userInfo, folder, null);
		toDelete.add(folderId);

		assertTrue(TimeUtils.waitFor(WAIT_FOR_STAT_CHANGE_MS, 200L, projectStat, new Predicate<ProjectStat>() {
			@Override
			public boolean apply(ProjectStat input) {
				List<ProjectStat> projectStatsForUser = projectStatsDAO.getProjectStatsForUser(userId);
				assertEquals("Shouldn't get more than one entry", 1, projectStatsForUser.size());
				return projectStatsForUser.size() == 1 && !projectStatsForUser.get(0).getEtag().equals(input.getEtag());
			}
		}));

		// Create an entity
		Folder subFolder = new Folder();
		subFolder.setName("boundForTheTrashCan2");
		subFolder.setParentId(folderId);
		toDelete.add(entityManager.createEntity(userInfo, subFolder, null));

		assertTrue(TimeUtils.waitFor(WAIT_FOR_STAT_CHANGE_MS, 200L, projectStat, new Predicate<ProjectStat>() {
			@Override
			public boolean apply(ProjectStat input) {
				List<ProjectStat> projectStatsForUser = projectStatsDAO.getProjectStatsForUser(userId);
				assertEquals("Shouldn't get more than one entry", 1, projectStatsForUser.size());
				return projectStatsForUser.size() == 1 && !projectStatsForUser.get(0).getEtag().equals(input.getEtag());
			}
		}));
	}

	@Test
	public void testAddFolderToProjectChange() throws Exception {
		ProjectStat projectStat = setupProject();

		// Create an entity
		Folder folder = new Folder();
		folder.setName("boundForTheTrashCan");
		folder.setParentId(KeyFactory.keyToString(projectStat.getProjectId()));
		entityManager.createEntity(userInfo, folder, null);

		assertTrue(TimeUtils.waitFor(WAIT_FOR_STAT_CHANGE_MS, 200L, projectStat, new Predicate<ProjectStat>() {
			@Override
			public boolean apply(ProjectStat input) {
				List<ProjectStat> projectStatsForUser = projectStatsDAO.getProjectStatsForUser(userId);
				assertEquals("Shouldn't get more than one entry", 1, projectStatsForUser.size());
				return projectStatsForUser.size() == 1 && !projectStatsForUser.get(0).getEtag().equals(input.getEtag());
			}
		}));
	}

	@Test
	public void testAddUserToProjectChanges() throws Exception {
		final ProjectStat projectStat = setupProject();

		Team team = new Team();
		team.setName("team-" + new Random().nextInt());
		team = teamManager.create(adminUserInfo, team);
		teamId = team.getId();

		final Long userInitiallyInTeam = userIds[0];
		final Long userAddedAndRemoveOnTeam = userIds[1];
		final Long userAddedAndRemovedOnProjectAcl = userIds[2];
		final Long userAddedToTeamAndNotRemoved = userIds[3];

		teamManager.addMember(adminUserInfo, teamId, userManager.getUserInfo(userInitiallyInTeam));

		for (int i = 0; i < userIds.length; i++) {
			List<ProjectStat> projectStatsForUser = projectStatsDAO.getProjectStatsForUser(userIds[i]);
			assertEquals(0, projectStatsForUser.size());
		}

		applyAndWaitForStatChange(projectStat, userInitiallyInTeam, new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				addAcl(projectStat.getProjectId(), Long.parseLong(teamId));
				return null;
			}
		});

		applyAndWaitForStatChange(projectStat, userAddedAndRemoveOnTeam, new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				teamManager.addMember(adminUserInfo, teamId, userManager.getUserInfo(userAddedAndRemoveOnTeam));
				return null;
			}
		});

		applyAndWaitForStatChange(projectStat, userAddedAndRemovedOnProjectAcl, new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				addAcl(projectStat.getProjectId(), userAddedAndRemovedOnProjectAcl);
				return null;
			}
		});

		applyAndWaitForStatChange(projectStat, userAddedToTeamAndNotRemoved, new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				teamManager.addMember(adminUserInfo, teamId, userManager.getUserInfo(userAddedToTeamAndNotRemoved));
				return null;
			}
		});
	}

	private void applyAndWaitForStatChange(ProjectStat projectStat, final Long userId, Callable<Void> action) throws Exception {
		final Date before = new Date();
		Thread.sleep(2);

		action.call();

		assertTrue(TimeUtils.waitFor(WAIT_FOR_STAT_CHANGE_MS, 200L, projectStat, new Predicate<ProjectStat>() {
			@Override
			public boolean apply(ProjectStat input) {
				List<ProjectStat> projectStatsForUser = projectStatsDAO.getProjectStatsForUser(userId);
				return projectStatsForUser.size() == 1 && projectStatsForUser.get(0).getLastAccessed().after(before);
			}
		}));
	}

	@Test
	public void testAddWikiToProjectChange() throws Exception {
		ProjectStat projectStat = setupProject();

		handleOne = new S3FileHandle();
		handleOne.setCreatedBy(Long.toString(userId));
		handleOne.setCreatedOn(new Date());
		handleOne.setBucketName("bucket");
		handleOne.setKey("mainFileKey");
		handleOne.setEtag("etag");
		handleOne.setFileName("foo.bar");
		handleOne.setId(idGenerator.generateNewId(IdType.FILE_IDS).toString());
		handleOne.setEtag(UUID.randomUUID().toString());
		handleOne = (S3FileHandle) fileMetadataDao.createFile(handleOne);

		v2RootWiki = new V2WikiPage();
		v2RootWiki.setCreatedBy(Long.toString(userId));
		v2RootWiki.setModifiedBy(Long.toString(userId));
		v2RootWiki.setTitle("Root title");
		v2RootWiki.setMarkdownFileHandleId(handleOne.getId());

		Map<String, FileHandle> map = new HashMap<String, FileHandle>();
		map.put(handleOne.getFileName(), handleOne);
		String projectId = KeyFactory.keyToString(projectStat.getProjectId());
		v2RootWiki = v2wikiPageDAO.create(v2RootWiki, map, projectId, ObjectType.ENTITY,
				Lists.newArrayList(handleOne.getId()));
		
		rootWikikey = new WikiPageKey();
		rootWikikey.setOwnerObjectId(projectId);
		rootWikikey.setOwnerObjectType(ObjectType.ENTITY);
		rootWikikey.setWikiPageId(v2RootWiki.getId());

		assertTrue(TimeUtils.waitFor(WAIT_FOR_STAT_CHANGE_MS, 200L, projectStat, new Predicate<ProjectStat>() {
			@Override
			public boolean apply(ProjectStat input) {
				List<ProjectStat> projectStatsForUser = projectStatsDAO.getProjectStatsForUser(userId);
				assertEquals("Shouldn't get more than one entry", 1, projectStatsForUser.size());
				return projectStatsForUser.size() == 1 && !projectStatsForUser.get(0).getEtag().equals(input.getEtag());
			}
		}));
	}

	private ProjectStat setupProject() throws NotFoundException, Exception {
		// Create a project
		assertEquals(0, projectStatsDAO.getProjectStatsForUser(userId).size());

		Project project = new Project();
		project.setName(UUID.randomUUID().toString());
		String id = entityManager.createEntity(adminUserInfo, project, null);
		project = entityManager.getEntity(adminUserInfo, id, Project.class);
		toDelete.add(project.getId());
		addAcl(KeyFactory.stringToKey(id), adminUserInfo.getId(), userId);

		// Wait for the project stat to be added
		List<ProjectStat> projectStatsForUser = TimeUtils.waitFor(WAIT_FOR_STAT_CHANGE_MS, 200L, new Callable<Pair<Boolean, List<ProjectStat>>>() {
			@Override
			public Pair<Boolean, List<ProjectStat>> call() throws Exception {
				List<ProjectStat> projectStatsForUser = projectStatsDAO.getProjectStatsForUser(userId);
				return Pair.create(projectStatsForUser.size() == 1, projectStatsForUser);
			}
		});
		/* 
		 * This test depends on time stamps changing between calls and can
		 * fail if the test runs too fast. Sleep was added to stabilize the test.
		 */
		Thread.sleep(1001);
		ProjectStat projectStat = projectStatsForUser.get(0);
		return projectStat;
	}

	private void addAcl(Long project, Long... usersToAdd) throws Exception {
		AccessControlList acl = entityAclManager.getACL(KeyFactory.keyToString(project), adminUserInfo);
		Set<ResourceAccess> ras = AclSnapshotUtils.createSetOfResourceAccess(Arrays.asList(usersToAdd), -1);
		acl.getResourceAccess().addAll(ras);

		// update the ACL
		entityAclManager.updateACL(acl, adminUserInfo);
	}
}
