package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.ActivityDAO;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOActivity;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests non-database related business logic in the DAO
 * This is more efficient than the Autowired test
 * @author dburdick
 *
 */
@ExtendWith(MockitoExtension.class)
public class DBOActivityDAOImplTest {
	
	private ActivityDAO activityDao;
	@Mock
	private DBOBasicDao mockBasicDao;
	@Mock
	private NamedParameterJdbcTemplate mockNamedJdbcTemplate;
	@Mock
	private JdbcTemplate mockJdbcTemplate;
	@Mock
	private IdGenerator mockIdGenerator;
	
	@BeforeEach
	public void before() {
		MockitoAnnotations.initMocks(this);
		activityDao = new DBOActivityDAOImpl();
		ReflectionTestUtils.setField(activityDao, "basicDao", mockBasicDao);
		ReflectionTestUtils.setField(activityDao, "namedJdbcTemplate", mockNamedJdbcTemplate);
		ReflectionTestUtils.setField(activityDao, "jdbcTemplate", mockJdbcTemplate);
	}
	
	@Test
	public void testUpdateNotFound() throws Exception {
		when(mockIdGenerator.generateNewId(IdType.ACTIVITY_ID)).thenReturn((long)123);
		when(mockBasicDao.getObjectByPrimaryKey(eq(DBOActivity.class), any(MapSqlParameterSource.class))).thenThrow(new NotFoundException(""));

		Activity act = new Activity();
		act.setId(mockIdGenerator.generateNewId(IdType.ACTIVITY_ID).toString());
		assertThrows(NotFoundException.class, ()->{
			activityDao.update(act);
		});
	}
	
	@Test
	public void testGetNotFound() throws Exception {
		when(mockIdGenerator.generateNewId(IdType.ACTIVITY_ID)).thenReturn((long)123);
		when(mockBasicDao.getObjectByPrimaryKey(eq(DBOActivity.class), any(MapSqlParameterSource.class))).thenReturn(Optional.empty());
		
		String message = assertThrows(NotFoundException.class, ()->{
			activityDao.get(mockIdGenerator.generateNewId(IdType.ACTIVITY_ID).toString());
		}).getMessage();
		assertEquals("Activity with id '123' could not be found.", message);
	}

	@Test
	public void testLockActivityAndIncrementEtag() throws Exception {
		String oldEtag = "oldEtag";
		String newEtag = "newEtag";
		Long id = 123L;
		DBOActivity mockDbo = mock(DBOActivity.class);
		when(mockDbo.getEtag()).thenReturn(newEtag);
		when(mockJdbcTemplate.queryForObject(anyString(), any(Class.class), any())).thenReturn(oldEtag);
		when(mockBasicDao.getObjectByPrimaryKey(eq(DBOActivity.class), any(MapSqlParameterSource.class))).thenReturn(Optional.of(mockDbo));
		
		ChangeType type = ChangeType.UPDATE;
		String returnedNewEtag = activityDao.lockActivityAndGenerateEtag(id.toString(), oldEtag, type);
		assertEquals(newEtag, returnedNewEtag);
	}

	@Test
	public void testLockActivityAndIncrementEtagNotFound() throws Exception {
		String oldEtag = "oldEtag";
		Long id = 123L;
		when(mockJdbcTemplate.queryForObject(anyString(), any(Class.class), any())).thenReturn(oldEtag);
		when(mockBasicDao.getObjectByPrimaryKey(eq(DBOActivity.class), any(MapSqlParameterSource.class))).thenThrow(new NotFoundException(""));
		
		assertThrows(NotFoundException.class, ()->{
			activityDao.lockActivityAndGenerateEtag(id.toString(), oldEtag, ChangeType.UPDATE);
		});
	}

	@Test
	public void testLockActivityAndIncrementEtagConflictingUpdate() throws Exception {
		when(mockJdbcTemplate.queryForObject(anyString(), any(Class.class), any())).thenReturn("newEtag");
		
		assertThrows(ConflictingUpdateException.class, ()->{
			activityDao.lockActivityAndGenerateEtag("1234", "oldEtag", ChangeType.UPDATE);
		});
	}

}
