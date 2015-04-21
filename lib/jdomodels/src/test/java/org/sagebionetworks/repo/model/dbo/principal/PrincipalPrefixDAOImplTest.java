package org.sagebionetworks.repo.model.dbo.principal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TeamDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class PrincipalPrefixDAOImplTest {

	@Autowired
	PrincipalPrefixDAO principalPrefixDao;
	@Autowired
	UserGroupDAO userGroupDAO;
	@Autowired
	private GroupMembersDAO groupMembersDAO;
	@Autowired
	TeamDAO teamDAO;

	Long principalOne;
	Long principalTwo;

	Long romaneId;
	Long romanusId;
	Long romulusId;
	Long rubensId;
	Long ruberId;
	Long rubiconId;
	Long rubicundusId;

	Long teamAllId;
	Long teamEvenId;
	Long teamOddId;

	List<Long> toDelete;

	@Before
	public void before() throws DatastoreException, IllegalArgumentException,
			NotFoundException {
		principalPrefixDao.truncateTable();
		toDelete = new LinkedList<Long>();
		UserGroup ug = new UserGroup();
		ug.setIsIndividual(true);
		principalOne = Long.parseLong(userGroupDAO.create(ug).toString());
		toDelete.add(principalOne);
		principalTwo = Long.parseLong(userGroupDAO.create(ug).toString());
		toDelete.add(principalTwo);
		// Create the rest
		romaneId = Long.parseLong(userGroupDAO.create(ug).toString());
		toDelete.add(romaneId);
		romanusId = Long.parseLong(userGroupDAO.create(ug).toString());
		toDelete.add(romanusId);
		romulusId = Long.parseLong(userGroupDAO.create(ug).toString());
		toDelete.add(romulusId);
		rubensId = Long.parseLong(userGroupDAO.create(ug).toString());
		toDelete.add(rubensId);
		ruberId = Long.parseLong(userGroupDAO.create(ug).toString());
		toDelete.add(ruberId);
		rubiconId = Long.parseLong(userGroupDAO.create(ug).toString());
		toDelete.add(rubiconId);
		rubicundusId = Long.parseLong(userGroupDAO.create(ug).toString());
		toDelete.add(rubicundusId);
		// all
		ug = new UserGroup();
		ug.setIsIndividual(false);
		teamAllId =  Long.parseLong(userGroupDAO.create(ug).toString());
		Team team = new Team();
		team.setId(""+teamAllId);
		teamDAO.create(team);
		groupMembersDAO.addMembers(teamAllId.toString(), new LinkedList<String>(
				Arrays.asList(
						romaneId.toString(),
						romanusId.toString(),
						romulusId.toString(),
						rubensId.toString(),
						ruberId.toString(),
						rubiconId.toString(),
						rubicundusId.toString()
						)));
		// even
		teamEvenId = Long.parseLong(userGroupDAO.create(ug).toString());
		team = new Team();
		team.setId(""+teamEvenId);
		teamDAO.create(team);
		groupMembersDAO.addMembers(teamEvenId.toString(), new LinkedList<String>(
				Arrays.asList(
						romanusId.toString(),
						rubensId.toString(),
						rubiconId.toString()
						)));
		// odd
		teamOddId =  Long.parseLong(userGroupDAO.create(ug).toString());
		team = new Team();
		team.setId(""+teamOddId);
		teamDAO.create(team);
		groupMembersDAO.addMembers(teamOddId.toString(), new LinkedList<String>(
				Arrays.asList(
						romaneId.toString(),
						romulusId.toString(),
						ruberId.toString(),
						rubicundusId.toString()
						)));
	}

	@After
	public void after() {
		if (toDelete != null) {
			for (Long id : toDelete) {
				try {
					userGroupDAO.delete(id.toString());
				} catch (Exception e) {
				}
			}
		}
	}

	@Test
	public void testPreProcessToken() {
		assertEquals("foobar",
				PrincipalPrefixDAOImpl.preProcessToken("Foo Bar"));
		assertEquals("", PrincipalPrefixDAOImpl.preProcessToken("!@#$%^&*()_+"));
		assertEquals("", PrincipalPrefixDAOImpl.preProcessToken(null));
	}

	@Test
	public void testAddName() {
		principalPrefixDao
				.addPrincipalName("FirstOne", "LastOne", principalOne);
		// add it again should not fail
		principalPrefixDao
				.addPrincipalName("FirstOne", "LastOne", principalOne);
		principalPrefixDao
				.addPrincipalName("FirstTwo", "LastTwo", principalTwo);
		assertEquals(new Long(2),
				principalPrefixDao.countPrincipalsForPrefix("First"));
		assertEquals(new Long(1),
				principalPrefixDao.countPrincipalsForPrefix("FirstO"));
		assertEquals(new Long(1),
				principalPrefixDao.countPrincipalsForPrefix("FirstT"));
		assertEquals(new Long(2),
				principalPrefixDao.countPrincipalsForPrefix("Last"));
		assertEquals(new Long(1),
				principalPrefixDao.countPrincipalsForPrefix("LastT"));
		assertEquals(new Long(1),
				principalPrefixDao.countPrincipalsForPrefix("LastO"));
	}

	@Test
	public void testAddAlias() {
		principalPrefixDao.addPrincipalAlias("batman", principalOne);
		// add it again should not fail
		principalPrefixDao.addPrincipalAlias("batman", principalOne);
		principalPrefixDao.addPrincipalAlias("batwoman", principalTwo);
		assertEquals(new Long(2),
				principalPrefixDao.countPrincipalsForPrefix("bat"));
		assertEquals(new Long(1),
				principalPrefixDao.countPrincipalsForPrefix("batM"));
		assertEquals(new Long(1),
				principalPrefixDao.countPrincipalsForPrefix("batW"));
	}

	@Test
	public void testClear() {
		principalPrefixDao
				.addPrincipalName("FirstOne", "LastOne", principalOne);
		principalPrefixDao.addPrincipalAlias("batman", principalOne);
		assertEquals(new Long(1),
				principalPrefixDao.countPrincipalsForPrefix("bat"));
		assertEquals(new Long(1),
				principalPrefixDao.countPrincipalsForPrefix("FirstOne L"));
		principalPrefixDao.clearPrincipal(principalOne);
		assertEquals(new Long(0),
				principalPrefixDao.countPrincipalsForPrefix("bat"));
		assertEquals(new Long(0),
				principalPrefixDao.countPrincipalsForPrefix("FirstOne L"));
	}

	/**
	 * listPrincipalsForPrefix() must only return distinct principal IDs.
	 */
	@Test
	public void testListPrincipalsForPrefixDistinct() {
		principalPrefixDao.addPrincipalAlias("aabb", principalOne);
		principalPrefixDao.addPrincipalAlias("aabc", principalOne);
		List<Long> results = principalPrefixDao.listPrincipalsForPrefix("aab",
				1000L, 0L);
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(principalOne, results.get(0));
		// the count should give the same results
		assertEquals(new Long(1), principalPrefixDao.countPrincipalsForPrefix("aab"));
	}
	
	@Test
	public void testListPrincipalsForPrefixEmptyPrefix() {
		addDefaultAlias();
		// Prefix with no alpha-numerics
		String prefixWithNoAlphaNumerics = "#$%";
		List<Long> results = principalPrefixDao.listPrincipalsForPrefix(prefixWithNoAlphaNumerics,
				1000L, 0L);
		assertNotNull(results);
		assertEquals(10, results.size());
		// the count should give the same results
		assertEquals(new Long(10), principalPrefixDao.countPrincipalsForPrefix(prefixWithNoAlphaNumerics));
	}

	@Test
	public void testListPrincipalsForPrefix() {
		principalPrefixDao.addPrincipalAlias("foo-bar", principalOne);
		principalPrefixDao.addPrincipalName("James", "Bond", principalOne);
		principalPrefixDao.addPrincipalAlias("FooBarBar", principalTwo);
		principalPrefixDao.addPrincipalName("James", "Smith", principalTwo);

		// Common prefix by alias.
		List<Long> results = principalPrefixDao.listPrincipalsForPrefix("foo",
				1000L, 0L);
		assertNotNull(results);
		assertEquals(2, results.size());
		assertEquals(principalOne, results.get(0));
		assertEquals(principalTwo, results.get(1));
		// Unique alias
		results = principalPrefixDao.listPrincipalsForPrefix("foobarb", 1000L,
				0L);
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(principalTwo, results.get(0));
		// Common first name
		results = principalPrefixDao.listPrincipalsForPrefix("James ", 1000L,
				0L);
		assertNotNull(results);
		assertEquals(2, results.size());
		assertEquals(principalOne, results.get(0));
		assertEquals(principalTwo, results.get(1));
		// unique name
		results = principalPrefixDao.listPrincipalsForPrefix("James S", 1000L,
				0L);
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(principalTwo, results.get(0));
	}
	
	@Test
	public void testListPrincipalsForPrefixOrder() {
		addDefaultAlias();
		List<Long> results = principalPrefixDao.listPrincipalsForPrefix("r",
				1000L, 0L);
		assertNotNull(results);
		assertEquals(10, results.size());
		// Counts should match
		assertEquals(new Long(10), principalPrefixDao.countPrincipalsForPrefix("r"));
		assertEquals(romaneId, results.get(0));
		assertEquals(romanusId, results.get(1));
		assertEquals(romulusId, results.get(2));
		assertEquals(teamAllId, results.get(3));
		assertEquals(teamEvenId, results.get(4));
		assertEquals(teamOddId, results.get(5));
		assertEquals(rubensId, results.get(6));
		assertEquals(ruberId, results.get(7));
		assertEquals(rubiconId, results.get(8));
		assertEquals(rubicundusId, results.get(9));
		
		// another letter
		results = principalPrefixDao.listPrincipalsForPrefix("Ru",
				1000L, 0L);
		assertNotNull(results);
		assertEquals(4, results.size());
		// Counts should match
		assertEquals(new Long(4), principalPrefixDao.countPrincipalsForPrefix("Ru"));
		assertEquals(rubensId, results.get(0));
		assertEquals(ruberId, results.get(1));
		assertEquals(rubiconId, results.get(2));
		assertEquals(rubicundusId, results.get(3));
		
		// another letter
		results = principalPrefixDao.listPrincipalsForPrefix("Rom",
				1000L, 0L);
		assertNotNull(results);
		assertEquals(3, results.size());
		// Counts should match
		assertEquals(new Long(3), principalPrefixDao.countPrincipalsForPrefix("Rom"));
		assertEquals(romaneId, results.get(0));
		assertEquals(romanusId, results.get(1));
		assertEquals(romulusId, results.get(2));

	}
	
	@Test
	public void testListPrincipalsForPrefixPaging() {
		addDefaultAlias();
		List<Long> results = principalPrefixDao.listPrincipalsForPrefix("r",
				2L, 4L);
		assertNotNull(results);
		assertEquals(2, results.size());
		assertEquals(teamEvenId, results.get(0));
		assertEquals(teamOddId, results.get(1));

		
		// another letter
		results = principalPrefixDao.listPrincipalsForPrefix("Ru",
				1L, 1L);
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(ruberId, results.get(0));
		
		// another letter
		results = principalPrefixDao.listPrincipalsForPrefix("Rom",
				2L, 2L);
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(romulusId, results.get(0));
	}
	
	@Test
	public void testListTeamMembersForPrefixDistict(){
		// Add two aliases for romane that share a prefix
		principalPrefixDao.addPrincipalAlias("romane1", romaneId);
		principalPrefixDao.addPrincipalAlias("romane2", romaneId);
		List<Long> results = principalPrefixDao.listTeamMembersForPrefix("romane", teamAllId , 1000L, 0L);
		assertNotNull(results);
		assertEquals(1, results.size());
		assertEquals(romaneId, results.get(0));
		// The count should be the same
		assertEquals(new Long(1), principalPrefixDao.countTeamMembersForPrefix("romane", teamAllId));
	}
	
	@Test
	public void testListTeamMembersForPrefixEmptyPrefix() {
		addDefaultAlias();
		// Prefix with no alpha-numerics
		String prefixWithNoAlphaNumerics = "#$%";
		List<Long> results = principalPrefixDao.listTeamMembersForPrefix(prefixWithNoAlphaNumerics, teamAllId,
				1000L, 0L);
		assertNotNull(results);
		assertEquals(7, results.size());
		// the count should give the same results
		assertEquals(new Long(7), principalPrefixDao.countTeamMembersForPrefix(prefixWithNoAlphaNumerics, teamAllId));
	}
	
	@Test
	public void testListTeamMembersForPrefix(){
		addDefaultAlias();
		List<Long> results = principalPrefixDao.listTeamMembersForPrefix("r", teamAllId,
				1000L, 0L);
		assertNotNull(results);
		assertEquals(7, results.size());
		// Counts should match
		assertEquals(new Long(7), principalPrefixDao.countTeamMembersForPrefix("r", teamAllId));
		assertEquals(romaneId, results.get(0));
		assertEquals(romanusId, results.get(1));
		assertEquals(romulusId, results.get(2));
		assertEquals(rubensId, results.get(3));
		assertEquals(ruberId, results.get(4));
		assertEquals(rubiconId, results.get(5));
		assertEquals(rubicundusId, results.get(6));
		
		// Odd team
		results = principalPrefixDao.listTeamMembersForPrefix("r", teamOddId,
				1000L, 0L);
		assertNotNull(results);
		assertEquals(4, results.size());
		// Counts should match
		assertEquals(new Long(4), principalPrefixDao.countTeamMembersForPrefix("r", teamOddId));
		assertEquals(romaneId, results.get(0));
		assertEquals(romulusId, results.get(1));
		assertEquals(ruberId, results.get(2));
		assertEquals(rubicundusId, results.get(3));
		
		// even team
		results = principalPrefixDao.listTeamMembersForPrefix("r", teamEvenId,
				1000L, 0L);
		assertNotNull(results);
		assertEquals(3, results.size());
		// Counts should match
		assertEquals(new Long(3), principalPrefixDao.countTeamMembersForPrefix("r", teamEvenId));
		assertEquals(romanusId, results.get(0));
		assertEquals(rubensId, results.get(1));
		assertEquals(rubiconId, results.get(2));
	}
	
	
	@Test
	public void testListTeamMembersForPrefixPaging(){
		addDefaultAlias();
		List<Long> results = principalPrefixDao.listTeamMembersForPrefix("r", teamEvenId, 3L, 1L);
		assertNotNull(results);
		assertEquals(2, results.size());
		assertEquals(rubensId, results.get(0));
		assertEquals(rubiconId, results.get(1));
	}
	
	@Test
	public void testListTeamMembersForPrefixSub(){
		addDefaultAlias();
		List<Long> results = principalPrefixDao.listTeamMembersForPrefix("rom", teamAllId,
				1000L, 0L);
		assertNotNull(results);
		assertEquals(3, results.size());
		// Counts should match
		assertEquals(new Long(3), principalPrefixDao.countTeamMembersForPrefix("rom", teamAllId));
		assertEquals(romaneId, results.get(0));
		assertEquals(romanusId, results.get(1));
		assertEquals(romulusId, results.get(2));
	}
	
	@Test
	public void testListTeamsForPrefix(){
		addDefaultAlias();
		List<Long> results = principalPrefixDao.listTeamsForPrefix("r",	1000L, 0L);
		assertNotNull(results);
		assertEquals(3, results.size());
		// Counts should match
		assertEquals(new Long(3), principalPrefixDao.countTeamsForPrefix("r"));
		assertEquals(teamAllId, results.get(0));
		assertEquals(teamEvenId, results.get(1));
		assertEquals(teamOddId, results.get(2));
		
		// Full paging
		results = principalPrefixDao.listTeamsForPrefix("rteam",2L, 1L);
		assertNotNull(results);
		assertEquals(2, results.size());
		// Counts should match
		assertEquals(new Long(3), principalPrefixDao.countTeamsForPrefix("rteam"));
		assertEquals(teamEvenId, results.get(0));
		assertEquals(teamOddId, results.get(1));
		
		// single
		results = principalPrefixDao.listTeamsForPrefix("rteama",100L, 0L);
		assertNotNull(results);
		assertEquals(1, results.size());
		// Counts should match
		assertEquals(new Long(1), principalPrefixDao.countTeamsForPrefix("rteama"));
		assertEquals(teamAllId, results.get(0));
	}
	
	@Test
	public void testListTeamsForPrefixEmptyPrefix() {
		addDefaultAlias();
		// Prefix with no alpha-numerics
		String prefixWithNoAlphaNumerics = "#$%";
		List<Long> results = principalPrefixDao.listTeamsForPrefix(prefixWithNoAlphaNumerics,1000L, 0L);
		assertNotNull(results);
		assertEquals(3, results.size());
		// the count should give the same results
		assertEquals(new Long(3), principalPrefixDao.countTeamsForPrefix(prefixWithNoAlphaNumerics));
	}
	
	/**
	 * Add default alias to all named principals.
	 */
	public void addDefaultAlias(){
		// teams
		principalPrefixDao.addPrincipalAlias("rTeamAll", teamAllId);
		principalPrefixDao.addPrincipalAlias("rTeamEven", teamEvenId);
		principalPrefixDao.addPrincipalAlias("rTeamOdd", teamOddId);
		// users
		principalPrefixDao.addPrincipalAlias("romane", romaneId);
		principalPrefixDao.addPrincipalAlias("romanus", romanusId);
		principalPrefixDao.addPrincipalAlias("romulus", romulusId);
		principalPrefixDao.addPrincipalAlias("rubens", rubensId);
		principalPrefixDao.addPrincipalAlias("ruber", ruberId);
		principalPrefixDao.addPrincipalAlias("rubicon", rubiconId);
		principalPrefixDao.addPrincipalAlias("rubicundus", rubicundusId);
	}

}
