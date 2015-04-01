package org.sagebionetworks.repo.model.dbo.principal;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
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
	
	Long principalOne;
	Long principalTwo;
	
	@Before
	public void before(){
		UserGroup ug = new UserGroup();
		ug.setIsIndividual(true);
		principalOne = Long.parseLong(userGroupDAO.create(ug).toString());
		principalTwo = Long.parseLong(userGroupDAO.create(ug).toString());
		
		principalPrefixDao.truncateTable();
	}
	
	@After
	public void after(){
		if(principalOne != null){
			try {
				userGroupDAO.delete(principalOne.toString());
			} catch (Exception e) {} 
		}
		if(principalTwo != null){
			try {
				userGroupDAO.delete(principalTwo.toString());
			} catch (Exception e) {} 
		}
	}
	
	@Test
	public void testPreProcessToken(){
		assertEquals("foobar", PrincipalPrefixDAOImpl.preProcessToken("Foo Bar"));
		assertEquals("", PrincipalPrefixDAOImpl.preProcessToken("!@#$%^&*()_+"));
		assertEquals("", PrincipalPrefixDAOImpl.preProcessToken(null));
	}
	
	@Test
	public void testAddName(){
		principalPrefixDao.addPrincipalName("FirstOne", "LastOne", principalOne);
		// add it again should not fail
		principalPrefixDao.addPrincipalName("FirstOne", "LastOne", principalOne);
		principalPrefixDao.addPrincipalName("FirstTwo", "LastTwo", principalTwo);
		assertEquals(new Long(2), principalPrefixDao.countUsersForPrefix("First"));
		assertEquals(new Long(1), principalPrefixDao.countUsersForPrefix("FirstO"));
		assertEquals(new Long(1), principalPrefixDao.countUsersForPrefix("FirstT"));
		assertEquals(new Long(2), principalPrefixDao.countUsersForPrefix("Last"));
		assertEquals(new Long(1), principalPrefixDao.countUsersForPrefix("LastT"));
		assertEquals(new Long(1), principalPrefixDao.countUsersForPrefix("LastO"));
	}
	
	@Test
	public void testAddAlias(){
		principalPrefixDao.addPrincipalAlias("batman", principalOne);
		// add it again should not fail
		principalPrefixDao.addPrincipalAlias("batman", principalOne);
		principalPrefixDao.addPrincipalAlias("batwoman", principalTwo);
		assertEquals(new Long(2), principalPrefixDao.countUsersForPrefix("bat"));
		assertEquals(new Long(1), principalPrefixDao.countUsersForPrefix("batM"));
		assertEquals(new Long(1), principalPrefixDao.countUsersForPrefix("batW"));
	}
	
	@Test
	public void testClear(){
		principalPrefixDao.addPrincipalName("FirstOne", "LastOne", principalOne);
		principalPrefixDao.addPrincipalAlias("batman", principalOne);
		assertEquals(new Long(1), principalPrefixDao.countUsersForPrefix("bat"));
		assertEquals(new Long(1), principalPrefixDao.countUsersForPrefix("FirstOne L"));
		principalPrefixDao.clearPrincipal(principalOne);
		assertEquals(new Long(0), principalPrefixDao.countUsersForPrefix("bat"));
		assertEquals(new Long(0), principalPrefixDao.countUsersForPrefix("FirstOne L"));
	}
}
