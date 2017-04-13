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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ActivityDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOActivity;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests non-database related business logic in the DAO
 * This is more efficient than the Autowired test
 * @author dburdick
 *
 */
public class DBOActivityDAOImplTest {
	
	private ActivityDAO activityDao;
	@Mock
	private TransactionalMessenger mockMessenger;
	@Mock
	private DBOBasicDao mockBasicDao;
	@Mock
	private NamedParameterJdbcTemplate mockNamedJdbcTemplate;
	@Mock
	private JdbcTemplate mockJdbcTemplate;
	@Mock
	private IdGenerator mockIdGenerator;
	
	@Before
	public void before() {
		MockitoAnnotations.initMocks(this);
		activityDao = new DBOActivityDAOImpl();
		ReflectionTestUtils.setField(activityDao, "basicDao", mockBasicDao);
		ReflectionTestUtils.setField(activityDao, "transactionalMessenger", mockMessenger);
		ReflectionTestUtils.setField(activityDao, "namedJdbcTemplate", mockNamedJdbcTemplate);
		ReflectionTestUtils.setField(activityDao, "jdbcTemplate", mockJdbcTemplate);
	}
	
	@Test(expected=NotFoundException.class) 
	public void testUpdateNotFound() throws Exception {
		when(mockIdGenerator.generateNewId(IdType.ACTIVITY_ID)).thenReturn((long)123);
		when(mockNamedJdbcTemplate.queryForObject(anyString(), any(SqlParameterSource.class), any(Class.class))).thenReturn((Long)0L);
		when(mockBasicDao.getObjectByPrimaryKey(eq(DBOActivity.class), any(MapSqlParameterSource.class))).thenThrow(new NotFoundException());

		Activity act = new Activity();
		act.setId(mockIdGenerator.generateNewId(IdType.ACTIVITY_ID).toString());
		activityDao.update(act);
	}
	
	@Test(expected=NotFoundException.class)
	public void testGetNotFound() throws Exception {
		when(mockIdGenerator.generateNewId(IdType.ACTIVITY_ID)).thenReturn((long)123);
		when(mockNamedJdbcTemplate.queryForObject(anyString(), any(SqlParameterSource.class), any(Class.class))).thenReturn((Long)0L);
		when(mockBasicDao.getObjectByPrimaryKey(eq(DBOActivity.class), any(MapSqlParameterSource.class))).thenThrow(new NotFoundException());
		
		activityDao.get(mockIdGenerator.generateNewId(IdType.ACTIVITY_ID).toString());
	}

	@Test
	public void testLockActivityAndIncrementEtag() throws Exception {
		String oldEtag = "oldEtag";
		String newEtag = "newEtag";
		Long id = 123L;
		DBOActivity mockDbo = mock(DBOActivity.class);
		when(mockDbo.getId()).thenReturn(id);
		when(mockDbo.getEtag()).thenReturn(newEtag);
		when(mockIdGenerator.generateNewId(IdType.ACTIVITY_ID)).thenReturn(id);
		when(mockJdbcTemplate.queryForObject(anyString(), any(Class.class), any())).thenReturn(oldEtag);
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
		when(mockIdGenerator.generateNewId(IdType.ACTIVITY_ID)).thenReturn(id);
		when(mockJdbcTemplate.queryForObject(anyString(), any(Class.class), any())).thenReturn(oldEtag);
		when(mockBasicDao.getObjectByPrimaryKey(eq(DBOActivity.class), any(MapSqlParameterSource.class))).thenThrow(new NotFoundException());
		
		activityDao.lockActivityAndGenerateEtag(id.toString(), oldEtag, ChangeType.UPDATE);
	}

	@Test(expected=ConflictingUpdateException.class)
	public void testLockActivityAndIncrementEtagConflictingUpdate() throws Exception {
		when(mockIdGenerator.generateNewId(IdType.ACTIVITY_ID)).thenReturn((long)123);
		when(mockJdbcTemplate.queryForObject(anyString(), any(Class.class), any())).thenReturn("newEtag");
		
		activityDao.lockActivityAndGenerateEtag("1234", "oldEtag", ChangeType.UPDATE);
	}

}
