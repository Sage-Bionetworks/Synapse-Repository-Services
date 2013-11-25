package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.ActivityDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOActivity;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * Tests non-database related business logic in the DAO
 * This is more efficient than the Autowired test
 * @author dburdick
 *
 */
public class DBOActivityDAOImplTest {
	
	private ActivityDAO activityDao;
	private TransactionalMessenger mockMessenger;
	private DBOBasicDao mockBasicDao;	
	private SimpleJdbcTemplate mockSimpleJdbcTemplate;
	private IdGenerator mockIdGenerator;
	
	@Before
	public void before() {
		mockMessenger = mock(TransactionalMessenger.class);
		mockBasicDao = mock(DBOBasicDao.class);
		mockSimpleJdbcTemplate = mock(SimpleJdbcTemplate.class);
		mockIdGenerator = mock(IdGenerator.class);
				 	
		activityDao = new DBOActivityDAOImpl(mockMessenger, mockBasicDao, mockSimpleJdbcTemplate);		
	}
	
	@Test(expected=NotFoundException.class) 
	public void testUpdateNotFound() throws Exception {
		when(mockIdGenerator.generateNewId()).thenReturn((long)123);
		when(mockSimpleJdbcTemplate.queryForLong(anyString(), any())).thenReturn((long)0);
		when(mockBasicDao.getObjectByPrimaryKey(eq(DBOActivity.class), any(MapSqlParameterSource.class))).thenThrow(new NotFoundException());

		Activity act = new Activity();
		act.setId(mockIdGenerator.generateNewId().toString());
		activityDao.update(act);
	}
	
	@Test(expected=NotFoundException.class)
	public void testGetNotFound() throws Exception {
		when(mockIdGenerator.generateNewId()).thenReturn((long)123);
		when(mockSimpleJdbcTemplate.queryForLong(anyString(), any())).thenReturn((long)0);		
		when(mockBasicDao.getObjectByPrimaryKey(eq(DBOActivity.class), any(MapSqlParameterSource.class))).thenThrow(new NotFoundException());
		
		activityDao.get(mockIdGenerator.generateNewId().toString());		
	}

	@Test
	public void testLockActivityAndIncrementEtag() throws Exception {
		String oldEtag = "oldEtag";
		String newEtag = "newEtag";		
		Long id = 123L;
		DBOActivity mockDbo = mock(DBOActivity.class);
		when(mockDbo.getId()).thenReturn(id);
		when(mockDbo.getEtag()).thenReturn(newEtag);
		when(mockIdGenerator.generateNewId()).thenReturn(id);				
		when(mockSimpleJdbcTemplate.queryForObject(anyString(), eq(String.class), eq(id.toString()))).thenReturn(oldEtag);
		when(mockBasicDao.getObjectByPrimaryKey(eq(DBOActivity.class), any(MapSqlParameterSource.class))).thenReturn(mockDbo);
		
		ChangeType type = ChangeType.UPDATE;
		String returnedNewEtag = activityDao.lockActivityAndGenerateEtag(id.toString(), oldEtag, type);
		verify(mockMessenger).sendMessageAfterCommit(eq(mockDbo), eq(type));
		assertEquals(newEtag, returnedNewEtag);
	}

	@Test(expected=NotFoundException.class)
	public void testLockActivityAndIncrementEtagNotFound() throws Exception {
		String oldEtag = "oldEtag";				
		Long id = 123L;
		when(mockIdGenerator.generateNewId()).thenReturn(id);				
		when(mockSimpleJdbcTemplate.queryForObject(anyString(), eq(String.class), eq(id.toString()))).thenReturn(oldEtag);
		when(mockBasicDao.getObjectByPrimaryKey(eq(DBOActivity.class), any(MapSqlParameterSource.class))).thenThrow(new NotFoundException());
		
		activityDao.lockActivityAndGenerateEtag(id.toString(), oldEtag, ChangeType.UPDATE);		
	}

	@Test(expected=ConflictingUpdateException.class)
	public void testLockActivityAndIncrementEtagConflictingUpdate() throws Exception {
		when(mockIdGenerator.generateNewId()).thenReturn((long)123);
		when(mockSimpleJdbcTemplate.queryForObject(anyString(), eq(String.class), any())).thenReturn("newerEtag");		
		
		activityDao.lockActivityAndGenerateEtag("1234", "oldEtag", ChangeType.UPDATE);		
	}

}
