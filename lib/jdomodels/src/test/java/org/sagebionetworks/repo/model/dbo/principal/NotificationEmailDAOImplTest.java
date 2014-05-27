package org.sagebionetworks.repo.model.dbo.principal;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class NotificationEmailDAOImplTest {
	
	@Autowired
	private UserGroupDAO userGroupDao;

	@Autowired
	private PrincipalAliasDAO principalAliasDao;
	
	@Autowired
	private NotificationEmailDAO notificationEmailDao;
	
	private PrincipalAlias alias;
	private PrincipalAlias alias2;
	private List<PrincipalAlias> toDelete;


	@Before
	public void before() throws Exception {
		toDelete = new ArrayList<PrincipalAlias>();
		// Create a test user
		UserGroup ug = new UserGroup();
		ug.setCreationDate(new Date());
		ug.setIsIndividual(true);
		Long principalId = userGroupDao.create(ug);
		
		// bind an alias to a principal
		alias = new PrincipalAlias();
		// Use to upper as the alias
		alias.setAlias(UUID.randomUUID().toString()+"@test.com");
		alias.setType(AliasType.USER_EMAIL);
		alias.setPrincipalId(principalId);
		alias = principalAliasDao.bindAliasToPrincipal(alias);
		toDelete.add(alias);
		
		// make a second one
		alias2 = new PrincipalAlias();
		// Use to upper as the alias
		alias2.setAlias(UUID.randomUUID().toString()+"@test.com");
		alias2.setType(AliasType.USER_EMAIL);
		alias2.setPrincipalId(principalId);
		alias2 = principalAliasDao.bindAliasToPrincipal(alias2);
		toDelete.add(alias2);
		
	}

	@After
	public void after() {
		for (PrincipalAlias alias : toDelete) {
			try {
				userGroupDao.delete(alias.getPrincipalId().toString());
			} catch (Exception e) {} 
		}
	}
	
	@Test
	public void testRoundTrip() throws Exception {
		// create 
		notificationEmailDao.create(alias);
		// get
		assertEquals(alias.getAlias(), notificationEmailDao.getNotificationEmailForPrincipal(alias.getPrincipalId()));
		// update 
		notificationEmailDao.update(alias2);
		// get
		assertEquals(alias2.getAlias(), notificationEmailDao.getNotificationEmailForPrincipal(alias2.getPrincipalId()));
	}
	
	@Test(expected=NotFoundException.class)
	public void testNotFound() throws Exception {
		notificationEmailDao.getNotificationEmailForPrincipal(-999L);
	}
}
