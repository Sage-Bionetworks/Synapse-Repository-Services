package org.sagebionetworks.repo.web.service;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class PrefixCacheHelperTest {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testGetPrefixes() {
		List<String> expected = Arrays.asList(new String[]{"foo", "bar", "foo bar"});
		assertEquals(expected, PrefixCacheHelper.getPrefixes("FOO bar"));
	}

}
