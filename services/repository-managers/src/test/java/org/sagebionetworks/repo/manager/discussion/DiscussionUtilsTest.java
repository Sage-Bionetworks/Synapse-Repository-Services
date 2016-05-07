package org.sagebionetworks.repo.manager.discussion;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

public class DiscussionUtilsTest {

	@Test (expected = IllegalArgumentException.class)
	public void testGetMentionedUsernameCaseNullMarkdown() {
		DiscussionUtils.getMentionedUsername(null);
	}

	@Test
	public void testGetMentionedUsernameCaseZeroUsers() {
		assertEquals(new ArrayList<String>(), DiscussionUtils.getMentionedUsername(""));
	}

	@Test
	public void testGetMentionedUsernameCaseOnlyUsername() {
		assertEquals(Arrays.asList("anonymous"), DiscussionUtils.getMentionedUsername("@anonymous"));
	}

	@Test
	public void testGetMentionedUsernameCaseWhiteSpacePrefix(){
		assertEquals(Arrays.asList("anonymous"), DiscussionUtils.getMentionedUsername("\t@anonymous"));
	}

	@Test
	public void testGetMentionedUsernameCaseWhiteSpacePostfix(){
		assertEquals(Arrays.asList("anonymous"), DiscussionUtils.getMentionedUsername("@anonymous\n"));
	}

	@Test
	public void testGetMentionedUsernameCaseDoubleAlpha(){
		assertEquals(Arrays.asList("anonymous"), DiscussionUtils.getMentionedUsername("@anon@ymous"));
	}

	@Test
	public void testGetMentionedUsernameCaseContainSpecialChar(){
		assertEquals(Arrays.asList("anonymous?#$%!^&*)(+-_=`~/'\"|.,<>"), DiscussionUtils.getMentionedUsername("@anonymous?#$%!^&*)(+-_=`~/'\"|.,<>"));
	}

	@Test
	public void testGetMentionedUsernameCaseMultipleUsername() {
		assertEquals(Arrays.asList("anonymous", "saturn"), DiscussionUtils.getMentionedUsername("@anonymous\n@saturn"));
	}
}
