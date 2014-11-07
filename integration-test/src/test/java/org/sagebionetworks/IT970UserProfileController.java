package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.EntityBundleCreate;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ProjectHeader;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.util.TimeUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class IT970UserProfileController {

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static Long userToDelete;
	private static String teamToDelete;

	private List<String> entitiesToDelete;
	
	@BeforeClass 
	public static void beforeClass() throws Exception {
		// Create a user
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUserName(StackConfiguration.getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfiguration.getMigrationAdminAPIKey());
		
		synapse = new SynapseClientImpl();
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);
		Team team = new Team();
		team.setName("team" + new Random().nextInt());
		teamToDelete = adminSynapse.createTeam(team).getId();
		adminSynapse.addTeamMember(teamToDelete, userToDelete.toString());
	}
	
	@Before
	public void before() {
		entitiesToDelete = new ArrayList<String>();
	}
	
	@After
	public void after() throws Exception {
		for(String id : entitiesToDelete) {
			try {
				synapse.deleteAndPurgeEntityById(id);
			} catch (Exception e) {
				try {
					synapse.purgeTrashForUser(id);
				} catch (Exception e2) {
				}
			}
		}
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		adminSynapse.deleteTeam(teamToDelete);
		adminSynapse.deleteUser(userToDelete);
	}
	
	@Test
	public void testGetAndUpdateOwnUserProfile() throws Exception {
		UserProfile userProfile = synapse.getMyProfile();
		System.out.println(userProfile);
		// now update the fields
		userProfile.setFirstName("foo");
		userProfile.setLastName("bar");
		
		synapse.updateMyProfile(userProfile);
	}
	
	@Test 
	public void testFavoriteCrud() throws Exception {
		Project entity = new Project();
		entity.setEntityType(Project.class.getName());
		entity = synapse.createEntity(entity);
		entitiesToDelete.add(entity.getId());
		
		// add
		EntityHeader fav = synapse.addFavorite(entity.getId());
		assertNotNull(fav);
		assertEquals(entity.getId(), fav.getId());
		
		// retrieve
		PaginatedResults<EntityHeader> favs = synapse.getFavorites(Integer.MAX_VALUE, 0);
		assertEquals(1, favs.getTotalNumberOfResults());
		assertEquals(1, favs.getResults().size());
		
		// remove
		synapse.removeFavorite(entity.getId());		
		// validate remove
		favs = synapse.getFavorites(Integer.MAX_VALUE, 0);
		assertEquals(0, favs.getTotalNumberOfResults());
		assertEquals(0, favs.getResults().size());
	}

	@Test
	public void testMyProjects() throws Exception {
		Project entity = new Project();
		entity.setEntityType(Project.class.getName());
		entity.setName("aa");
		entity = synapse.createEntity(entity);
		entitiesToDelete.add(entity.getId());

		AccessControlList acl = synapse.getACL(entity.getId());
		ResourceAccess teamAccess = new ResourceAccess();
		teamAccess.setAccessType(Collections.singleton(ACCESS_TYPE.READ));
		teamAccess.setPrincipalId(Long.parseLong(teamToDelete));
		acl.setResourceAccess(Sets.union(acl.getResourceAccess(), Collections.singleton(teamAccess)));
		synapse.updateACL(acl);

		Folder folder = new Folder();
		folder.setEntityType(Folder.class.getName());
		folder.setParentId(entity.getId());
		folder.setName("folder1");
		folder = synapse.createEntity(folder);
		entitiesToDelete.add(folder.getId());

		// ensure time ordering
		Thread.sleep(1000);
		Project entity2 = new Project();
		entity2.setName("bb");
		entity2.setEntityType(Project.class.getName());
		entity2 = synapse.createEntity(entity2);
		entitiesToDelete.add(entity2.getId());

		acl = synapse.getACL(entity2.getId());
		teamAccess = new ResourceAccess();
		teamAccess.setAccessType(Collections.singleton(ACCESS_TYPE.READ));
		teamAccess.setPrincipalId(Long.parseLong(teamToDelete));
		acl.setResourceAccess(Sets.union(acl.getResourceAccess(), Collections.singleton(teamAccess)));
		synapse.updateACL(acl);

		// retrieve my projects
		PaginatedResults<ProjectHeader> projects = synapse.getMyProjects(Integer.MAX_VALUE, 0);
		assertEquals(2, projects.getTotalNumberOfResults());
		assertEquals(2, projects.getResults().size());
		List<ProjectHeader> alphabetical = Lists.newArrayList(projects.getResults());
		Collections.sort(alphabetical, new Comparator<ProjectHeader>() {
			@Override
			public int compare(ProjectHeader o1, ProjectHeader o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

		PaginatedResults<ProjectHeader> projects5 = adminSynapse.getProjectsForTeam(Long.parseLong(teamToDelete), 100, 0);
		// alphabetical
		assertEquals(alphabetical, projects5.getResults());

		// change order
		folder.setName("folder1-renamed");
		EntityBundleCreate ebc = new EntityBundleCreate();
		ebc.setEntity(folder);
		synapse.updateEntityBundle(folder.getId(), ebc);

		TimeUtils.waitFor(20000, 1000, Lists.reverse(projects.getResults()), new Predicate<List<ProjectHeader>>() {
			@Override
			public boolean apply(List<ProjectHeader> expected) {
				try {
					PaginatedResults<ProjectHeader> projects = synapse.getMyProjects(Integer.MAX_VALUE, 0);
					return expected.equals(projects.getResults());
				} catch (SynapseException e) {
					throw new RuntimeException(e.getMessage(), e);
				}
			}
		});

		PaginatedResults<ProjectHeader> projects3 = synapse.getMyProjects(Integer.MAX_VALUE, 0);
		assertEquals(Lists.reverse(projects.getResults()), projects3.getResults());

		PaginatedResults<ProjectHeader> projects4 = adminSynapse.getProjectsFromUser(userToDelete, 100, 0);
		assertEquals(Lists.reverse(projects.getResults()), projects4.getResults());

		projects5 = adminSynapse.getProjectsForTeam(Long.parseLong(teamToDelete), 100, 0);
		// still alphabetical
		assertEquals(alphabetical, projects5.getResults());

		// ignore trashed projects
		synapse.deleteEntity(entity);
		projects = synapse.getMyProjects(Integer.MAX_VALUE, 0);
		assertEquals(1, projects.getTotalNumberOfResults());
		assertEquals(1, projects.getResults().size());
		assertEquals(entity2.getId(), projects.getResults().get(0).getId());
	}
}
