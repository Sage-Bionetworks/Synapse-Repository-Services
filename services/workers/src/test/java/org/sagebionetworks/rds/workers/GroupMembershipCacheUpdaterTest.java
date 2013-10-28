package org.sagebionetworks.rds.workers;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.dao.DBOGroupMembersDAOImpl;
import org.sagebionetworks.repo.model.dbo.dao.GroupMembersUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class GroupMembershipCacheUpdaterTest {
	
	@Autowired
	private SimpleJdbcTemplate simpleJdbcTemplate;
	
	@Autowired
	private GroupMembersDAO groupMembersDAO;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private GroupMembershipCacheUpdater runner;

	/**
	 * Note: this is essentially the same test as testValidateCache() in DBOGroupMembersDAOImplTest
	 */
	@Test
	public void testValidateCache() throws Exception {
		UserGroup testUser = userGroupDAO.findGroup(AuthorizationConstants.TEST_USER_NAME, true);
		String principalId = testUser.getId();
		
		// Add a cache row for the test user
		MapSqlParameterSource param = new MapSqlParameterSource();
		param.addValue(DBOGroupMembersDAOImpl.GROUP_ID_PARAM_NAME, principalId);
		simpleJdbcTemplate.update(DBOGroupMembersDAOImpl.INSERT_NEW_PARENTS_CACHE_ROW, param);
		
		// This is not a valid cache entry
		// It references some evil "negative" groups
		List<String> fosterParents = Arrays.asList(new String[] { "-1", "-2", "-4", "-8", "-16", "-32", "-64" });
		param.addValue(DBOGroupMembersDAOImpl.PARENT_BLOB_PARAM_NAME, GroupMembersUtils.zip(fosterParents));
		simpleJdbcTemplate.update(DBOGroupMembersDAOImpl.UPDATE_BLOB_IN_PARENTS_CACHE, param);
		
		// Shouldn't return anything
		List<UserGroup> uggaBugga = groupMembersDAO.getUsersGroups(principalId);
		assertEquals(0, uggaBugga.size());
		
		// Now it should return the correct test group parent
		runner.run();
		List<UserGroup> ugs = groupMembersDAO.getUsersGroups(principalId);
		assertEquals(1, ugs.size());
	}
}
