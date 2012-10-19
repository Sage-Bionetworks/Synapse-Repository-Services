package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.ActivityDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.TagMessenger;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * Tests non-database related business logic in the DAO
 * This is more efficient than the Autowired test
 * @author dburdick
 *
 */
public class DBOActivityDAOImplTest {
	
	ActivityDAO activityDao;
	TagMessenger mockTagMessenger;
	DBOBasicDao mockBasicDao;	
	SimpleJdbcTemplate mockSimpleJdbcTemplate;
	IdGenerator mockIdGenerator;
	
	@Before
	public void before() {
		mockTagMessenger = mock(TagMessenger.class);
		mockBasicDao = mock(DBOBasicDao.class);
		mockSimpleJdbcTemplate = mock(SimpleJdbcTemplate.class);
		mockIdGenerator = mock(IdGenerator.class);
				 	
		activityDao = new DBOActivityDAOImpl(mockTagMessenger, mockBasicDao, mockSimpleJdbcTemplate);		
	}
	
	@Test(expected=NotFoundException.class) 
	public void testUpdateNotFound() throws Exception {
		when(mockIdGenerator.generateNewId()).thenReturn((long)123);
		when(mockSimpleJdbcTemplate.queryForLong(anyString(), any())).thenReturn((long)0);		

		Activity act = new Activity();
		act.setId(mockIdGenerator.generateNewId().toString());
		activityDao.update(act);
		fail();
	}
	
	@Test(expected=NotFoundException.class)
	public void testGetNotFound() throws Exception {
		when(mockIdGenerator.generateNewId()).thenReturn((long)123);
		when(mockSimpleJdbcTemplate.queryForLong(anyString(), any())).thenReturn((long)0);		
		
		activityDao.get(mockIdGenerator.generateNewId().toString());		
		fail();
	}

	@Test(expected=NotFoundException.class)
	public void testDeleteNotFound() throws Exception {
		when(mockIdGenerator.generateNewId()).thenReturn((long)123);
		when(mockSimpleJdbcTemplate.queryForLong(anyString(), any())).thenReturn((long)0);		
		
		activityDao.delete(mockIdGenerator.generateNewId().toString());		
		fail();
	}

	@Test(expected=NotFoundException.class)
	public void testLockActivityAndIncrementEtagNotFound() throws Exception {
		when(mockIdGenerator.generateNewId()).thenReturn((long)123);
		when(mockSimpleJdbcTemplate.queryForLong(anyString(), any())).thenReturn((long)0);		
		
		activityDao.delete(mockIdGenerator.generateNewId().toString());		
		fail();
	}

	@Test(expected=ConflictingUpdateException.class)
	public void testLockActivityAndIncrementEtagConflictingUpdate() throws Exception {
		when(mockIdGenerator.generateNewId()).thenReturn((long)123);
		when(mockSimpleJdbcTemplate.queryForObject(anyString(), eq(String.class), any())).thenReturn("newerEtag");		
		
		activityDao.lockActivityAndIncrementEtag("1234", "oldEtag", ChangeType.UPDATE);		
		fail();
	}

}
