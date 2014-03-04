package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.UserGroupHeader;

public class PrefixCacheHelperTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testGetPrefixes() {
		List<String> expected = Arrays.asList(new String[]{"foo", "bar", "foo bar"});
		assertEquals(expected, PrefixCacheHelper.getPrefixes("FOO bar"));
	}

	@Test
	public void testGetPrefixesUserGroupHeader() {
		UserGroupHeader header = new UserGroupHeader();
		header.setFirstName("foo");
		header.setLastName("bar");
		header.setUserName("cat dog");
		List<String> expected = Arrays.asList(new String[]{"cat", "dog", "cat dog","foo", "bar"});
		assertEquals(expected, PrefixCacheHelper.getPrefixes(header));
	}
}
