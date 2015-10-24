package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.util.Clock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.util.ReflectionTestUtils;

public class DBOAuthenticationDAOImplUnitTest {

	JdbcTemplate mockJdbcTemplate;
	UserGroupDAO mockUserGroupDao;
	DBOBasicDao mockBasicDao;
	Clock mockClock;
	DBOAuthenticationDAOImpl authDao;
	long principalId;
	DomainType domainType;
	
	@Before
	public void before(){
		mockJdbcTemplate = Mockito.mock(JdbcTemplate.class);
		mockUserGroupDao = Mockito.mock(UserGroupDAO.class);
		mockBasicDao = Mockito.mock(DBOBasicDao.class);
		mockClock = Mockito.mock(Clock.class);
		authDao = new DBOAuthenticationDAOImpl();
		ReflectionTestUtils.setField(authDao, "jdbcTemplate", mockJdbcTemplate);
		ReflectionTestUtils.setField(authDao, "userGroupDAO", mockUserGroupDao);
		ReflectionTestUtils.setField(authDao, "basicDAO", mockBasicDao);
		ReflectionTestUtils.setField(authDao, "clock", mockClock);
		
		principalId = 789;
		domainType = DomainType.SYNAPSE;
	}
	
	/**
	 * For this case the token was last updated one millisecond ago, so there is no need to revaliate it again.
	 * See: PLFM-3206 & PLFM-3202
	 */
	@Test
	public void testRevaliateNotNeeded(){
		long lastValidate = 0;
		long now = lastValidate + 1L;
		when(mockClock.currentTimeMillis()).thenReturn(now);
		when(mockJdbcTemplate.queryForObject(any(String.class), Matchers.<RowMapper<Long>>any(),Matchers.<Object>anyVararg())).thenReturn(lastValidate);
		// call under test
		assertFalse("The token did not need to be validated yet",authDao.revalidateSessionTokenIfNeeded(principalId, domainType));
		verify(mockUserGroupDao, never()).touch(anyLong());
		verify(mockJdbcTemplate, never()).update(anyString(), any(MapSqlParameterSource.class));
	}
	
	/**
	 * For this case the token was last updated one millisecond past the half-life of a session token, so
	 * we need to revalidate it.
	 */
	@Test
	public void testRevaliateNeeded(){
		long lastValidate = 1;
		long now = lastValidate + DBOAuthenticationDAOImpl.HALF_SESSION_EXPIRATION+1L;
		when(mockClock.currentTimeMillis()).thenReturn(now);
		when(mockJdbcTemplate.queryForObject(any(String.class), Matchers.<RowMapper<Long>>any(),Matchers.<Object>anyVararg())).thenReturn(lastValidate);
		// call under test
		assertTrue("The token needed to be revalidated",authDao.revalidateSessionTokenIfNeeded(principalId, domainType));
		verify(mockUserGroupDao).touch(principalId);
		verify(mockJdbcTemplate).update(anyString(), Matchers.<Object>anyVararg());
	}
}
