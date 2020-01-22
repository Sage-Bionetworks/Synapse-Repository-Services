package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
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
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.ProjectHeader;
import org.sagebionetworks.repo.model.ProjectHeaderList;
import org.sagebionetworks.repo.model.ProjectListType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UserBundle;
import org.sagebionetworks.repo.model.UserProfile;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class IT970UserProfileController {

	private static final int MAX_WAIT_MS = 40000;
	private static final String MOCK_TEAM_ENDPOINT = "https://www.synapse.org/#Team:";
	private static final String MOCK_NOTIFICATION_UNSUB_ENDPOINT = "https://www.synapse.org#unsub:";
	private static final int ALL_USER_BUNDLE_FIELDS = 63;
	
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
		adminSynapse.setUsername(StackConfigurationSingleton.singleton().getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfigurationSingleton.singleton().getMigrationAdminAPIKey());
		
		synapse = new SynapseClientImpl();
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);
		Team team = new Team();
		team.setName("team" + new Random().nextInt());
		team = synapse.createTeam(team);
		teamToDelete = team.getId();
	}
	
	@Before
	public void before() {
		entitiesToDelete = new ArrayList<String>();
	}
	
	@After
	public void after() throws Exception {
		for(String id : entitiesToDelete) {
			try {
				synapse.deleteEntityById(id);
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
		// here we are not testing business logic, just making sure evewrything is "wired up" right:
		
		// retrieve my projects
		ProjectHeaderList projects = synapse.getMyProjects(ProjectListType.ALL, null, null, null);
		// retrieve my next page of projects
		projects = synapse.getMyProjects(ProjectListType.ALL, null, null, projects.getNextPageToken());
		// retrieve someone else's projects
		projects = synapse.getProjectsFromUser(userToDelete, null, null, null);
				
		// make sure deprecated services still work
		synapse.getMyProjectsDeprecated(ProjectListType.ALL, null, null, null, null);
		synapse.getProjectsForTeamDeprecated(Long.parseLong(teamToDelete), null, null, null, null);
		synapse.getProjectsFromUserDeprecated(userToDelete, null, null, null, null);
	}

	private List<ProjectHeader> nullOutLastActivity(List<ProjectHeader> alphabetical) {
		return Lists.transform(alphabetical, new Function<ProjectHeader, ProjectHeader>() {
			@Override
			public ProjectHeader apply(ProjectHeader input) {
				ProjectHeader output = new ProjectHeader();
				output.setId(input.getId());
				output.setName(input.getName());
				output.setLastActivity(null);
				return output;
			}
		});
	}
	
	@Test
	public void testGetBundle() throws Exception {
		UserProfile userProfile = synapse.getMyProfile();
		UserBundle bundle = synapse.getMyOwnUserBundle(ALL_USER_BUNDLE_FIELDS);
		assertEquals(userProfile, bundle.getUserProfile());
		
		bundle = synapse.getUserBundle(Long.parseLong(userProfile.getOwnerId()), ALL_USER_BUNDLE_FIELDS);
		assertEquals(userProfile, bundle.getUserProfile());	
	}


}
