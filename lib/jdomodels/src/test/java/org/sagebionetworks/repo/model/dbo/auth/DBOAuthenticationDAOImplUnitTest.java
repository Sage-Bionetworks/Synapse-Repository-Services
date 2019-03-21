package org.sagebionetworks.repo.model.dbo.auth;

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
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.auth.DBOAuthenticationDAOImpl;
import org.sagebionetworks.util.Clock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

@RunWith(MockitoJUnitRunner.class)
public class DBOAuthenticationDAOImplUnitTest {

	@Mock
	JdbcTemplate mockJdbcTemplate;
	@Mock
	UserGroupDAO mockUserGroupDao;
	@Mock
	DBOBasicDao mockBasicDao;
	@Mock
	Clock mockClock;

	@InjectMocks
	DBOAuthenticationDAOImpl authDao;
	long principalId;
	
	@Before
	public void before(){
		principalId = 789;
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
		assertFalse("The token did not need to be validated yet",authDao.revalidateSessionTokenIfNeeded(principalId));
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
		assertTrue("The token needed to be revalidated",authDao.revalidateSessionTokenIfNeeded(principalId));
		verify(mockUserGroupDao).touch(principalId);
		verify(mockJdbcTemplate).update(anyString(), Matchers.<Object>anyVararg());
	}
}
