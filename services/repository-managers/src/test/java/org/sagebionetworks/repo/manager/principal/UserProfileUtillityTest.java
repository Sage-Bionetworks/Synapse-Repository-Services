package org.sagebionetworks.repo.manager.principal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;

public class UserProfileUtillityTest {

	
	@Test
	public void testCreateTempoaryUserName(){
		String expected = "TEMPORARY-123";
		String result = UserProfileUtillity.createTempoaryUserName(123L);
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetPrincipalIds(){
		List<UserProfile> input = new LinkedList<UserProfile>();
		UserProfile p = new UserProfile();
		p.setOwnerId("123");
		input.add(p);
		p = new UserProfile();
		p.setOwnerId("456");
		input.add(p);
		Set<Long> results = UserProfileUtillity.getPrincipalIds(input);
		assertNotNull(results);
		assertTrue(results.contains(123L));
		assertTrue(results.contains(456L));
	}
	
	@Test
	public void testGroupAlieaseByPrincipal(){
		List<PrincipalAlias> input = new LinkedList<PrincipalAlias>();
		PrincipalAlias p = new PrincipalAlias();
		p.setAliasId(1L);
		p.setPrincipalId(123L);
		input.add(p);
		p = new PrincipalAlias();
		p.setAliasId(2L);
		p.setPrincipalId(456L);
		input.add(p);
		p = new PrincipalAlias();
		p.setAliasId(3L);
		p.setPrincipalId(123L);
		input.add(p);
		// Get the results
		Map<Long, List<PrincipalAlias>> map = UserProfileUtillity.groupAlieaseByPrincipal(input);
		assertNotNull(map);
		assertEquals("There should be two entries in the map, one for each of the two principal Ids", 2, map.size());
		// This user has one
		List<PrincipalAlias> princpalAliases = map.get(456L);
		assertNotNull(princpalAliases);
		assertEquals(1, princpalAliases.size());
		assertEquals(new Long(2), princpalAliases.get(0).getAliasId());
		// This user had two
		princpalAliases = map.get(123L);
		assertNotNull(princpalAliases);
		assertEquals(2, princpalAliases.size());
		assertEquals(new Long(1), princpalAliases.get(0).getAliasId());
		assertEquals(new Long(3), princpalAliases.get(1).getAliasId());
	}
	
	@Test
	public void testMergeProfileWithAliases(){
		UserProfile profile = new UserProfile();
		profile.setOwnerId("123");
		
		List<PrincipalAlias> aliases = new LinkedList<PrincipalAlias>();
		// One email
		PrincipalAlias p = new PrincipalAlias();
		p.setAlias("foo@bar.com");
		p.setType(AliasType.USER_EMAIL);
		p.setAliasId(1l);
		aliases.add(p);
		// second email
		p = new PrincipalAlias();
		p.setAlias("foo2@bar.com");
		p.setType(AliasType.USER_EMAIL);
		p.setAliasId(2l);
		aliases.add(p);
		// Username
		p = new PrincipalAlias();
		p.setAlias("jamesBond");
		p.setType(AliasType.USER_NAME);
		p.setAliasId(3l);
		aliases.add(p);
		// Open ID
		p = new PrincipalAlias();
		p.setAlias("http://google.com/123");
		p.setType(AliasType.USER_OPEN_ID);
		p.setAliasId(4l);
		aliases.add(p);
		// Now merge them
		UserProfileUtillity.mergeProfileWithAliases(profile, aliases);
		// username
		assertEquals("jamesBond", profile.getUserName());
		// emails
		assertNotNull(profile.getEmails());
		assertEquals(2, profile.getEmails().size());
		assertEquals("foo@bar.com", profile.getEmails().get(0));
		assertEquals("foo2@bar.com", profile.getEmails().get(1));
		// openId
		assertNotNull(profile.getOpenIds());
		assertEquals(1, profile.getOpenIds().size());
		assertEquals("http://google.com/123", profile.getOpenIds().get(0));
		
	}
	
	
	@Test
	public void testMergeProfileWithAliasesNoReplace(){
		// In this case the profile already has data for emails and openIds and it must not be lost.
		UserProfile profile = new UserProfile();
		profile.setOwnerId("123");
		profile.setEmails(new LinkedList<String>());
		profile.getEmails().add("existingEmail@test.com");
		profile.setOpenIds(new LinkedList<String>());
		profile.getOpenIds().add("http://google.com/existing");
		
		List<PrincipalAlias> aliases = new LinkedList<PrincipalAlias>();
		// One email
		PrincipalAlias p = new PrincipalAlias();
		p.setAlias("foo@bar.com");
		p.setType(AliasType.USER_EMAIL);
		p.setAliasId(1l);
		aliases.add(p);
		// second email
		p = new PrincipalAlias();
		p.setAlias("foo2@bar.com");
		p.setType(AliasType.USER_EMAIL);
		p.setAliasId(2l);
		aliases.add(p);
		// Username
		p = new PrincipalAlias();
		p.setAlias("jamesBond");
		p.setType(AliasType.USER_NAME);
		p.setAliasId(3l);
		aliases.add(p);
		// Open ID
		p = new PrincipalAlias();
		p.setAlias("http://google.com/123");
		p.setType(AliasType.USER_OPEN_ID);
		p.setAliasId(4l);
		aliases.add(p);
		// Now merge them
		UserProfileUtillity.mergeProfileWithAliases(profile, aliases);
		// username
		assertEquals("jamesBond", profile.getUserName());
		// emails
		assertNotNull(profile.getEmails());
		assertEquals(1, profile.getEmails().size());
		assertEquals("The existing email should not have been lost", "existingEmail@test.com", profile.getEmails().get(0));

		// openId
		assertNotNull(profile.getOpenIds());
		assertEquals(1, profile.getOpenIds().size());
		assertEquals("The existing openID should not have been lost","http://google.com/existing", profile.getOpenIds().get(0));
		
	}
	
	@Test
	public void testNullOrEmpty(){
		assertTrue(UserProfileUtillity.isNullOrEmpty(null));
		List<String> list = new LinkedList<String>();
		assertTrue(UserProfileUtillity.isNullOrEmpty(list));
		list.add("not empty any more");
		assertFalse(UserProfileUtillity.isNullOrEmpty(list));
	}
}
