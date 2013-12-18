package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.NameType;
import org.sagebionetworks.repo.model.PrincipalHeader;
import org.sagebionetworks.repo.model.PrincipalHeaderDAO;
import org.sagebionetworks.repo.model.PrincipalType;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
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
		ug.setName(UUID.randomUUID().toString());
		ug.setIsIndividual(true);
		principalOne = Long.parseLong(userGroupDAO.create(ug));
		
		ug.setName(UUID.randomUUID().toString());
		principalTwo = Long.parseLong(userGroupDAO.create(ug));
	}

	@After
	public void tearDown() throws Exception {
		userGroupDAO.delete("" + principalOne);
		userGroupDAO.delete("" + principalTwo);
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
	
	@Test
	public void testSqlLikeEscape() throws Exception {
		// Shouldn't affect alpha-numerics
		assertEquals("abc123", DBOPrincipaHeaderDAOImpl.escapeSqlLike("abc123"));
		
		// Shouldn't affect most characters
		assertEquals("`~!@#$^&*()=+[{]}\\|;:'\",<.>/?", DBOPrincipaHeaderDAOImpl.escapeSqlLike("`~!@#$^&*()=+[{]}\\|;:'\",<.>/?"));
		
		// Should replace '%' and '_'
		assertEquals("def\\%456\\_", DBOPrincipaHeaderDAOImpl.escapeSqlLike("def%456_"));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testInvalidCreate() throws Exception {
		PrincipalHeader row = new PrincipalHeader();
		prinHeadDAO.insertNew(row);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testInsertNotIdempotent() throws Exception {
		PrincipalHeader row = new PrincipalHeader();
		row.setPrincipalId(principalOne);
		row.setIdentifier(NAME_ONE);
		row.setPrincipalType(PrincipalType.USER);
		row.setDomainType(DomainType.SYNAPSE);
		row.setNameType(NameType.PRINCIPAL_NAME);
		prinHeadDAO.insertNew(row);
		prinHeadDAO.insertNew(row);
	}
	
	@Test
	public void testCreateDelete() throws Exception {
		PrincipalHeader row = new PrincipalHeader();
		row.setPrincipalId(principalOne);
		row.setIdentifier(NAME_ONE);
		row.setPrincipalType(PrincipalType.USER);
		row.setDomainType(DomainType.SYNAPSE);
		row.setNameType(NameType.PRINCIPAL_NAME);
		
		// Should delete just fine
		prinHeadDAO.insertNew(row);
		assertTrue(prinHeadDAO.delete(principalOne, NAME_ONE));
		assertFalse(prinHeadDAO.delete(principalOne, NAME_ONE));
		assertEquals(0, prinHeadDAO.delete(principalOne));

		// Try two rows
		prinHeadDAO.insertNew(row);
		row.setIdentifier(NAME_TWO);
		prinHeadDAO.insertNew(row);
		assertEquals(2, prinHeadDAO.delete(principalOne));
		assertFalse(prinHeadDAO.delete(principalOne, NAME_ONE));
		assertFalse(prinHeadDAO.delete(principalOne, NAME_TWO));
		assertEquals(0, prinHeadDAO.delete(principalOne));
	}
	
	@Test
	public void testPrefixMatchFilterByEnum() throws Exception {
		// These two rows will have completely different values
		// But the two names will have similar prefixes
		PrincipalHeader rowOne = new PrincipalHeader();
		rowOne.setPrincipalId(principalOne);
		rowOne.setIdentifier(NAME_ONE);
		rowOne.setPrincipalType(PrincipalType.USER);
		rowOne.setDomainType(DomainType.SYNAPSE);
		rowOne.setNameType(NameType.PRINCIPAL_NAME);
		prinHeadDAO.insertNew(rowOne);

		PrincipalHeader rowTwo = new PrincipalHeader();
		rowTwo.setPrincipalId(principalTwo);
		rowTwo.setIdentifier(NAME_TWO);
		rowTwo.setPrincipalType(PrincipalType.TEAM);
		rowTwo.setDomainType(DomainType.BRIDGE);
		rowTwo.setNameType(NameType.ALIAS);
		prinHeadDAO.insertNew(rowTwo);
		
		// With no enum filters, the prefix match should return both results
		assertEquals(2, prinHeadDAO.countQueryResults(PREFIX, false, new HashSet<PrincipalType>(), new HashSet<DomainType>(), new HashSet<NameType>()));
		List<PrincipalHeader> results = prinHeadDAO.query(PREFIX, false, null, null, null, DEFAULT_LIMIT, DEFAULT_OFFSET);
		assertEquals(2, results.size());
		assertTrue(results.contains(rowOne));
		assertTrue(results.contains(rowTwo));
		
		// Filtering by PrincipalType
		assertEquals(1, prinHeadDAO.countQueryResults(PREFIX, false, Sets.newHashSet(PrincipalType.USER), null, null));
		results = prinHeadDAO.query(PREFIX, false, Sets.newHashSet(PrincipalType.USER), null, null, DEFAULT_LIMIT, DEFAULT_OFFSET);
		assertEquals(1, results.size());
		assertTrue(results.contains(rowOne));

		assertEquals(1, prinHeadDAO.countQueryResults(PREFIX, false, Sets.newHashSet(PrincipalType.TEAM), null, null));
		results = prinHeadDAO.query(PREFIX, false, Sets.newHashSet(PrincipalType.TEAM), null, null, DEFAULT_LIMIT, DEFAULT_OFFSET);
		assertEquals(1, results.size());
		assertTrue(results.contains(rowTwo));
		
		// Filtering by DomainType
		assertEquals(1, prinHeadDAO.countQueryResults(PREFIX, false, null, Sets.newHashSet(DomainType.SYNAPSE), null));
		results = prinHeadDAO.query(PREFIX, false, null, Sets.newHashSet(DomainType.SYNAPSE), null, DEFAULT_LIMIT, DEFAULT_OFFSET);
		assertEquals(1, results.size());
		assertTrue(results.contains(rowOne));

		assertEquals(1, prinHeadDAO.countQueryResults(PREFIX, false, null, Sets.newHashSet(DomainType.BRIDGE), null));
		results = prinHeadDAO.query(PREFIX, false, null, Sets.newHashSet(DomainType.BRIDGE), null, DEFAULT_LIMIT, DEFAULT_OFFSET);
		assertEquals(1, results.size());
		assertTrue(results.contains(rowTwo));
		
		// Filtering by NameType
		assertEquals(1, prinHeadDAO.countQueryResults(PREFIX, false, null, null, Sets.newHashSet(NameType.PRINCIPAL_NAME)));
		results = prinHeadDAO.query(PREFIX, false, null, null, Sets.newHashSet(NameType.PRINCIPAL_NAME), DEFAULT_LIMIT, DEFAULT_OFFSET);
		assertEquals(1, results.size());
		assertTrue(results.contains(rowOne));

		assertEquals(1, prinHeadDAO.countQueryResults(PREFIX, false, null, null, Sets.newHashSet(NameType.ALIAS)));
		results = prinHeadDAO.query(PREFIX, false, null, null, Sets.newHashSet(NameType.ALIAS), DEFAULT_LIMIT, DEFAULT_OFFSET);
		assertEquals(1, results.size());
		assertTrue(results.contains(rowTwo));

		assertEquals(0, prinHeadDAO.query(PREFIX, false, null, null, Sets.newHashSet(NameType.FIRST_NAME), DEFAULT_LIMIT, DEFAULT_OFFSET).size());
		assertEquals(0, prinHeadDAO.query(PREFIX, false, null, null, Sets.newHashSet(NameType.LAST_NAME), DEFAULT_LIMIT, DEFAULT_OFFSET).size());
		
		// Filter by some combination of filters
		results = prinHeadDAO.query(PREFIX, false, Sets.newHashSet(PrincipalType.USER), Sets.newHashSet(DomainType.SYNAPSE), null, DEFAULT_LIMIT, DEFAULT_OFFSET);
		assertEquals(1, results.size());
		assertTrue(results.contains(rowOne));

		results = prinHeadDAO.query(PREFIX, false, Sets.newHashSet(PrincipalType.TEAM), Sets.newHashSet(DomainType.BRIDGE), null, DEFAULT_LIMIT, DEFAULT_OFFSET);
		assertEquals(1, results.size());
		assertTrue(results.contains(rowTwo));

		assertEquals(0, prinHeadDAO.query(PREFIX, false, Sets.newHashSet(PrincipalType.USER), Sets.newHashSet(DomainType.BRIDGE), null, DEFAULT_LIMIT, DEFAULT_OFFSET).size());
		assertEquals(0, prinHeadDAO.query(PREFIX, false, Sets.newHashSet(PrincipalType.TEAM), Sets.newHashSet(DomainType.SYNAPSE), null, DEFAULT_LIMIT, DEFAULT_OFFSET).size());
	}
	
	@Test
	public void testExactMatch() throws Exception {
		PrincipalHeader rowOne = new PrincipalHeader();
		rowOne.setPrincipalId(principalOne);
		rowOne.setIdentifier(NAME_ONE);
		rowOne.setPrincipalType(PrincipalType.TEAM);
		rowOne.setDomainType(DomainType.SYNAPSE);
		rowOne.setNameType(NameType.FIRST_NAME);
		prinHeadDAO.insertNew(rowOne);

		// Only differs in name
		PrincipalHeader rowTwo = EntityFactory.createEntityFromJSONObject(EntityFactory.createJSONObjectForEntity(rowOne), PrincipalHeader.class);
		rowTwo.setIdentifier(NAME_TWO);
		prinHeadDAO.insertNew(rowTwo);
		
		// Only differs in principalID
		PrincipalHeader rowThree = EntityFactory.createEntityFromJSONObject(EntityFactory.createJSONObjectForEntity(rowOne), PrincipalHeader.class);
		rowThree.setPrincipalId(principalTwo);
		prinHeadDAO.insertNew(rowThree);

		// Prefix matching no longer returns any results
		assertEquals(0, prinHeadDAO.countQueryResults(PREFIX, true, null, null, null));
		assertEquals(0, prinHeadDAO.query(PREFIX, true, null, null, null, DEFAULT_LIMIT, DEFAULT_OFFSET).size());
		
		// Exact match on NAME_ONE
		assertEquals(2, prinHeadDAO.countQueryResults(NAME_ONE, true, null, null, null));
		List<PrincipalHeader> results = prinHeadDAO.query(NAME_ONE, true, null, null, null, DEFAULT_LIMIT, DEFAULT_OFFSET);
		assertEquals(2, results.size());
		assertTrue(results.contains(rowOne));
		assertTrue(results.contains(rowThree));
		
		// Exact match on NAME_TWO
		assertEquals(1, prinHeadDAO.countQueryResults(NAME_TWO, true, null, null, null));
		results = prinHeadDAO.query(NAME_TWO, true, null, null, null, DEFAULT_LIMIT, DEFAULT_OFFSET);
		assertEquals(1, results.size());
		assertTrue(results.contains(rowTwo));
	}
	
	@Test
	public void testReturnDistinctIDs() throws Exception {
		// Insert two rows belonging to the same user, but with different names
		PrincipalHeader row = new PrincipalHeader();
		row.setPrincipalId(principalOne);
		row.setIdentifier(NAME_ONE);
		row.setPrincipalType(PrincipalType.USER);
		row.setDomainType(DomainType.BRIDGE);
		row.setNameType(NameType.LAST_NAME);
		prinHeadDAO.insertNew(row);

		row.setIdentifier(NAME_TWO);
		prinHeadDAO.insertNew(row);

		// Prefix query should only return one result
		assertEquals(1, prinHeadDAO.countQueryResults(PREFIX, false, null, null, null));
		List<PrincipalHeader> results = prinHeadDAO.query(PREFIX, false, null, null, null, DEFAULT_LIMIT, DEFAULT_OFFSET);
		assertEquals(1, results.size());
		assertEquals(principalOne, results.get(0).getPrincipalId().longValue());
		assertTrue(results.get(0).getIdentifier().equals(NAME_ONE) || results.get(0).getIdentifier().equals(NAME_TWO));
	}
	
	@Test
	public void testNullNameFilter() throws Exception {
		// Insert two rows belonging to the same user, but with different names
		PrincipalHeader row = new PrincipalHeader();
		row.setPrincipalId(principalOne);
		row.setIdentifier(NAME_ONE);
		row.setPrincipalType(PrincipalType.USER);
		row.setDomainType(DomainType.BRIDGE);
		row.setNameType(NameType.LAST_NAME);
		prinHeadDAO.insertNew(row);

		// A null prefix match should return one result for each ID
		assertEquals(1, prinHeadDAO.countQueryResults(null, false, null, null, null));
		List<PrincipalHeader> results = prinHeadDAO.query(null, false, null, null, null, DEFAULT_LIMIT, DEFAULT_OFFSET);
		assertEquals(1, results.size());
		assertTrue(results.contains(row));
		
		// A null exact match should return nothing
		assertEquals(0, prinHeadDAO.countQueryResults(null, true, null, null, null));
		assertEquals(0, prinHeadDAO.query(null, true, null, null, null, DEFAULT_LIMIT, DEFAULT_OFFSET).size());
	}
}
