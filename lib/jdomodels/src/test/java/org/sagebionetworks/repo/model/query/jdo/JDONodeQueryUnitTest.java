package org.sagebionetworks.repo.model.query.jdo;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;
import org.sagebionetworks.repo.model.jdo.AuthorizationSqlUtil;

/**
 * 
 * @author jmhill
 *
 */
public class JDONodeQueryUnitTest {

	Log log = LogFactory.getLog(JDONodeQueryUnitTest.class);
	
	@Test
	public void testAuthorizationSqlAdminUser(){
		UserInfo adminUserInfo = Mockito.mock(UserInfo.class);
		when(adminUserInfo.isAdmin()).thenReturn(true);
		HashMap<String, Object> params = new HashMap<String, Object>();
		// This should produce an empty string for an admin user.
		String sql = JDONodeQueryDaoImpl.buildAuthorizationFilter(adminUserInfo, params);
		assertEquals("The authorization filter for an admin users should be an empty string","", sql);
		assertEquals(0, params.size());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testAuthorizationSqlNonAdminuserNullGroups(){
		UserInfo nonAdminUserInfo = Mockito.mock(UserInfo.class);
		when(nonAdminUserInfo.isAdmin()).thenReturn(false);
		when(nonAdminUserInfo.getGroups()).thenReturn(null);
		HashMap<String, Object> params = new HashMap<String, Object>();
		// should throw an exception
		String sql = JDONodeQueryDaoImpl.buildAuthorizationFilter(nonAdminUserInfo, params);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testAuthorizationSqlNonAdminuserEmptyGroups(){
		UserInfo nonAdminUserInfo = Mockito.mock(UserInfo.class);
		when(nonAdminUserInfo.isAdmin()).thenReturn(false);
		when(nonAdminUserInfo.getGroups()).thenReturn(new ArrayList<UserGroup>());
		HashMap<String, Object> params = new HashMap<String, Object>();
		// Should throw an exception.
		String sql = JDONodeQueryDaoImpl.buildAuthorizationFilter(nonAdminUserInfo, params);
	}
	
	@Test
	public void testAuthorizationSqlNonAdminWithGroups(){
		HashMap<String, Object> params = new HashMap<String, Object>();
		UserInfo nonAdminUserInfo = Mockito.mock(UserInfo.class);
		when(nonAdminUserInfo.isAdmin()).thenReturn(false);
		ArrayList<UserGroup> groups = new ArrayList<UserGroup>();
		UserGroup group = Mockito.mock(UserGroup.class);
		when(group.getId()).thenReturn("123");
		groups.add(group);
		group = Mockito.mock(UserGroup.class);
		when(group.getId()).thenReturn("124");
		groups.add(group);
		when(nonAdminUserInfo.getGroups()).thenReturn(groups);
		// This should build a query with two groups
		String sql = JDONodeQueryDaoImpl.buildAuthorizationFilter(nonAdminUserInfo, params);
		assertNotNull(sql);
		// It should not be an empty string.
		assertFalse("".equals(sql.trim()));
		log.info(sql);
		log.info(params);
		// Check the bind variables.
		assertEquals(ACCESS_TYPE.READ.name(), params.get(AuthorizationSqlUtil.ACCESS_TYPE_BIND_VAR));
		Long groupBindValue0 = (Long) params.get(AuthorizationSqlUtil.BIND_VAR_PREFIX+"0");
		assertNotNull(groupBindValue0);
		Long groupBindValue1 = (Long) params.get(AuthorizationSqlUtil.BIND_VAR_PREFIX+"1");
		assertNotNull(groupBindValue1);
		assertTrue(123L == groupBindValue0.longValue() || 123L == groupBindValue1.longValue());
		assertTrue(124L == groupBindValue0.longValue() || 124L == groupBindValue1.longValue());
	}
}
