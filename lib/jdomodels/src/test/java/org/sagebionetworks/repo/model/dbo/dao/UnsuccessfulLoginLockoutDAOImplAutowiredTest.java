package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.UnsuccessfulLoginLockoutDAO;
import org.sagebionetworks.repo.model.UnsuccessfulLoginLockoutDTO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class UnsuccessfulLoginLockoutDAOImplAutowiredTest {
	@Autowired
	UnsuccessfulLoginLockoutDAO dao;
	
	@Autowired
	UserGroupDAO userGroupDao;
	
	@Autowired
	DBOBasicDao basicDao;

	Long userId;


	@Before
	public void setUp(){
		if(userId == null) {
			// Initialize a UserGroup
			UserGroup ug = new UserGroup();
			ug.setIsIndividual(true);
			userId = userGroupDao.create(ug);

			// Make a row of Credentials
			DBOCredential credential = new DBOCredential();
			credential.setPrincipalId(userId);
			credential.setPassHash("{PKCS5S2}1234567890abcdefghijklmnopqrstuvwxyz");
			credential.setSecretKey("ABCDEFGHIJKLMNOPQRSTUVWXYZ");
			credential = basicDao.createNew(credential);
		}
	}

	@After
	public void cleanUp(){
		if(userId != null) {
			userGroupDao.delete(userId.toString());
		}
	}

	@Transactional
	@Test
	public void testRoundTrip(){
		//try to get DTO before creation
		assertNull(dao.getUnsuccessfulLoginLockoutInfoIfExist(userId));

		//create new DTO and fetch it from database
		UnsuccessfulLoginLockoutDTO dto = new UnsuccessfulLoginLockoutDTO(userId)
				.withUnsuccessfulLoginCount(42)
				.withLockoutExpiration(134560984923L);
		dao.createOrUpdateUnsuccessfulLoginLockoutInfo(dto);
		assertEquals(dto, dao.getUnsuccessfulLoginLockoutInfoIfExist(userId));

		//update the DTO
		dto.withLockoutExpiration(128).withLockoutExpiration(2L);
		dao.createOrUpdateUnsuccessfulLoginLockoutInfo(dto);
		assertEquals(dto, dao.getUnsuccessfulLoginLockoutInfoIfExist(userId));
	}
}
