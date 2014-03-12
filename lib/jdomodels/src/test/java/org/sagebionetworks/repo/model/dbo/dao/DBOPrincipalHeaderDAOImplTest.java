package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.PrincipalHeaderDAO;
import org.sagebionetworks.repo.model.PrincipalHeaderDAO.MATCH_TYPE;
import org.sagebionetworks.repo.model.PrincipalType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Sets;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })

public class DBOPrincipalHeaderDAOImplTest {
	
	private final String PREFIX = "I'm a little tea";
	private final String NAME_ONE = PREFIX + " pot";
	private final String NAME_TWO = PREFIX + " kettle";
	
	private final long DEFAULT_LIMIT = 10;
	private final long DEFAULT_OFFSET = 0;
	
	@Autowired
	private PrincipalHeaderDAO prinHeadDAO;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
		
	private long principalOne;
	private long principalTwo;

	@Before
	public void setUp() throws Exception {
		// We'll need two FKs for this test
		// so that queries can exclude or include one or both
		UserGroup ug = new UserGroup();
		ug.setIsIndividual(true);
		principalOne = Long.parseLong(userGroupDAO.create(ug).toString());
		principalTwo = Long.parseLong(userGroupDAO.create(ug).toString());
	}

	@After
	public void tearDown() throws Exception {
		userGroupDAO.delete("" + principalOne);
		userGroupDAO.delete("" + principalTwo);
		
		prinHeadDAO.delete(principalOne);
		prinHeadDAO.delete(principalTwo);
	}
	
	@Test
	public void testEnumConverter() throws Exception {
		// A null set gets converted into a full set
		Set<PrincipalType> test = null;
		Set<String> converted = DBOPrincipaHeaderDAOImpl.convertEnum(test, PrincipalType.class);
		for (PrincipalType value : PrincipalType.values()) {
			assertTrue("Converted set must contain " + value, converted.contains(value.name()));
		}
		
		// Same for empty sets
		test = Sets.newHashSet();
		converted = DBOPrincipaHeaderDAOImpl.convertEnum(test, PrincipalType.class);
		for (PrincipalType value : PrincipalType.values()) {
			assertTrue("Converted set must contain " + value, converted.contains(value.name()));
		}
		
		// A non-empty set just gets converted into strings
		test = Sets.newHashSet(PrincipalType.TEAM);
		converted = DBOPrincipaHeaderDAOImpl.convertEnum(test, PrincipalType.class);
		assertTrue(converted.contains(PrincipalType.TEAM.name()));
	}
	
	@Deprecated
	@Test
	public void testSqlLikeEscape() throws Exception {
		// Shouldn't affect alpha-numerics
		assertEquals("abc123", DBOPrincipaHeaderDAOImpl.preprocessFragment("abc123"));
		
		// Shouldn't affect most characters
		assertEquals("`~!@#$^&*()=+[{]}\\|;:'\",<.>/?", DBOPrincipaHeaderDAOImpl.preprocessFragment("`~!@#$^&*()=+[{]}\\|;:'\",<.>/?"));
		
		// Should replace '%' and '_'
		assertEquals("def\\%456\\_", DBOPrincipaHeaderDAOImpl.preprocessFragment("def%456_"));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testInvalidCreate() throws Exception {
		prinHeadDAO.insertNew(39485L, Sets.newHashSet("asdf"), null, null);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testInsertNotIdempotent() throws Exception {
		prinHeadDAO.insertNew(principalOne, Sets.newHashSet(NAME_ONE), PrincipalType.USER, DomainType.SYNAPSE);
		prinHeadDAO.insertNew(principalOne, Sets.newHashSet(NAME_ONE), PrincipalType.USER, DomainType.SYNAPSE);
	}
	
	@Test
	public void testCreateDelete() throws Exception {
		prinHeadDAO.insertNew(principalOne, Sets.newHashSet(NAME_ONE, NAME_TWO), PrincipalType.USER, DomainType.SYNAPSE);
		assertEquals(2, prinHeadDAO.delete(principalOne));
		assertEquals(0, prinHeadDAO.delete(principalOne));
	}
	
	@Test
	public void testPrefixMatchFilterByEnum() throws Exception {
		// These two rows will have completely different values
		// But the two names will have similar prefixes
		prinHeadDAO.insertNew(principalOne, Sets.newHashSet(NAME_ONE), PrincipalType.USER, DomainType.SYNAPSE);
		prinHeadDAO.insertNew(principalTwo, Sets.newHashSet(NAME_TWO), PrincipalType.TEAM, DomainType.BRIDGE);
		
		// With no enum filters, the prefix match should return both results
		assertEquals(2, prinHeadDAO.countQueryResults(PREFIX, MATCH_TYPE.PREFIX, new HashSet<PrincipalType>(), new HashSet<DomainType>()));
		List<Long> results = prinHeadDAO.query(PREFIX, MATCH_TYPE.PREFIX, null, null, DEFAULT_LIMIT, DEFAULT_OFFSET);
		assertEquals(2, results.size());
		assertTrue(results.contains(principalOne));
		assertTrue(results.contains(principalTwo));
		
		// Filtering by PrincipalType
		assertEquals(1, prinHeadDAO.countQueryResults(PREFIX, MATCH_TYPE.PREFIX, Sets.newHashSet(PrincipalType.USER), null));
		results = prinHeadDAO.query(PREFIX, MATCH_TYPE.PREFIX, Sets.newHashSet(PrincipalType.USER), null, DEFAULT_LIMIT, DEFAULT_OFFSET);
		assertEquals(1, results.size());
		assertTrue(results.contains(principalOne));

		assertEquals(1, prinHeadDAO.countQueryResults(PREFIX, MATCH_TYPE.PREFIX, Sets.newHashSet(PrincipalType.TEAM), null));
		results = prinHeadDAO.query(PREFIX, MATCH_TYPE.PREFIX, Sets.newHashSet(PrincipalType.TEAM), null, DEFAULT_LIMIT, DEFAULT_OFFSET);
		assertEquals(1, results.size());
		assertTrue(results.contains(principalTwo));
		
		// Filtering by DomainType
		assertEquals(1, prinHeadDAO.countQueryResults(PREFIX, MATCH_TYPE.PREFIX, null, Sets.newHashSet(DomainType.SYNAPSE)));
		results = prinHeadDAO.query(PREFIX, MATCH_TYPE.PREFIX, null, Sets.newHashSet(DomainType.SYNAPSE), DEFAULT_LIMIT, DEFAULT_OFFSET);
		assertEquals(1, results.size());
		assertTrue(results.contains(principalOne));

		assertEquals(1, prinHeadDAO.countQueryResults(PREFIX, MATCH_TYPE.PREFIX, null, Sets.newHashSet(DomainType.BRIDGE)));
		results = prinHeadDAO.query(PREFIX, MATCH_TYPE.PREFIX, null, Sets.newHashSet(DomainType.BRIDGE), DEFAULT_LIMIT, DEFAULT_OFFSET);
		assertEquals(1, results.size());
		assertTrue(results.contains(principalTwo));
		
		// Filter by some combination of filters
		results = prinHeadDAO.query(PREFIX, MATCH_TYPE.PREFIX, Sets.newHashSet(PrincipalType.USER), Sets.newHashSet(DomainType.SYNAPSE), DEFAULT_LIMIT, DEFAULT_OFFSET);
		assertEquals(1, results.size());
		assertTrue(results.contains(principalOne));

		results = prinHeadDAO.query(PREFIX, MATCH_TYPE.PREFIX, Sets.newHashSet(PrincipalType.TEAM), Sets.newHashSet(DomainType.BRIDGE), DEFAULT_LIMIT, DEFAULT_OFFSET);
		assertEquals(1, results.size());
		assertTrue(results.contains(principalTwo));

		assertEquals(0, prinHeadDAO.query(PREFIX, MATCH_TYPE.PREFIX, Sets.newHashSet(PrincipalType.USER), Sets.newHashSet(DomainType.BRIDGE), DEFAULT_LIMIT, DEFAULT_OFFSET).size());
		assertEquals(0, prinHeadDAO.query(PREFIX, MATCH_TYPE.PREFIX, Sets.newHashSet(PrincipalType.TEAM), Sets.newHashSet(DomainType.SYNAPSE), DEFAULT_LIMIT, DEFAULT_OFFSET).size());
	}
	
	@Test
	public void testExactMatch() throws Exception {
		prinHeadDAO.insertNew(principalOne, Sets.newHashSet(NAME_ONE, NAME_TWO), PrincipalType.TEAM, DomainType.SYNAPSE);
		
		// Only differs in principalID
		prinHeadDAO.insertNew(principalTwo, Sets.newHashSet(NAME_ONE), PrincipalType.TEAM, DomainType.SYNAPSE);

		// Prefix matching no longer returns any results
		assertEquals(0, prinHeadDAO.countQueryResults(PREFIX, MATCH_TYPE.EXACT, null, null));
		assertEquals(0, prinHeadDAO.query(PREFIX, MATCH_TYPE.EXACT, null, null, DEFAULT_LIMIT, DEFAULT_OFFSET).size());
		
		// Exact match on NAME_ONE
		assertEquals(2, prinHeadDAO.countQueryResults(NAME_ONE, MATCH_TYPE.EXACT, null, null));
		List<Long> results = prinHeadDAO.query(NAME_ONE, MATCH_TYPE.EXACT, null, null, DEFAULT_LIMIT, DEFAULT_OFFSET);
		assertEquals(2, results.size());
		assertTrue(results.contains(principalOne));
		assertTrue(results.contains(principalTwo));
		
		// Exact match on NAME_TWO
		assertEquals(1, prinHeadDAO.countQueryResults(NAME_TWO, MATCH_TYPE.EXACT, null, null));
		results = prinHeadDAO.query(NAME_TWO, MATCH_TYPE.EXACT, null, null, DEFAULT_LIMIT, DEFAULT_OFFSET);
		assertEquals(1, results.size());
		assertTrue(results.contains(principalOne));
	}
	
	@Test
	public void testReturnDistinctIDs() throws Exception {
		// Insert two rows belonging to the same user, but with different names
		prinHeadDAO.insertNew(principalOne, Sets.newHashSet(NAME_ONE, NAME_TWO), PrincipalType.USER, DomainType.BRIDGE);

		// Prefix query should only return one result
		assertEquals(1, prinHeadDAO.countQueryResults(PREFIX, MATCH_TYPE.PREFIX, null, null));
		List<Long> results = prinHeadDAO.query(PREFIX, MATCH_TYPE.PREFIX, null, null, DEFAULT_LIMIT, DEFAULT_OFFSET);
		assertEquals(1, results.size());
		assertEquals(principalOne, results.get(0).longValue());
	}
	
	@Test
	public void testNullNameFilter() throws Exception {
		// Insert two rows belonging to the same user, but with different names
		prinHeadDAO.insertNew(principalOne, Sets.newHashSet(NAME_ONE, NAME_TWO), PrincipalType.USER, DomainType.BRIDGE);

		// A null prefix match should return one result for each ID
		assertEquals(1, prinHeadDAO.countQueryResults(null, MATCH_TYPE.PREFIX, null, null));
		List<Long> results = prinHeadDAO.query(null, MATCH_TYPE.PREFIX, null, null, DEFAULT_LIMIT, DEFAULT_OFFSET);
		assertEquals(1, results.size());
		assertTrue(results.contains(principalOne));
		
		// A null exact match should return nothing
		assertEquals(0, prinHeadDAO.countQueryResults(null, MATCH_TYPE.EXACT, null, null));
		assertEquals(0, prinHeadDAO.query(null, MATCH_TYPE.EXACT, null, null, DEFAULT_LIMIT, DEFAULT_OFFSET).size());
		
		// A null soundex match should return nothing
		assertEquals(0, prinHeadDAO.countQueryResults(null, MATCH_TYPE.EXACT, null, null));
		assertEquals(0, prinHeadDAO.query(null, MATCH_TYPE.EXACT, null, null, DEFAULT_LIMIT, DEFAULT_OFFSET).size());
	}
	
	@Test
	public void testSoundsLikeFilter() throws Exception {
		// This test is based on Soundex values derived from WolframAlpha (query: soundex <word>)
		
		// Soundex of M200
		prinHeadDAO.insertNew(principalOne, Sets.newHashSet("mike"), PrincipalType.TEAM, DomainType.SYNAPSE);
		
		// Soundex of M240
		prinHeadDAO.insertNew(principalTwo, Sets.newHashSet("michel"), PrincipalType.TEAM, DomainType.SYNAPSE);
		
		// Soundex of M200
		List<Long> results = prinHeadDAO.query("mich", MATCH_TYPE.SOUNDEX, null, null, DEFAULT_LIMIT, DEFAULT_OFFSET);
		assertEquals(1, results.size());
		assertTrue(results.contains(principalOne));
		
		// Soundex of M240
		results = prinHeadDAO.query("mikel", MATCH_TYPE.SOUNDEX, null, null, DEFAULT_LIMIT, DEFAULT_OFFSET);
		assertEquals(1, results.size());
		assertTrue(results.contains(principalTwo));

		// Soundex of M324
		assertEquals(0, prinHeadDAO.countQueryResults("mitchel", MATCH_TYPE.EXACT, null, null));
		assertEquals(0, prinHeadDAO.query(null, MATCH_TYPE.EXACT, null, null, DEFAULT_LIMIT, DEFAULT_OFFSET).size());
	}
}
