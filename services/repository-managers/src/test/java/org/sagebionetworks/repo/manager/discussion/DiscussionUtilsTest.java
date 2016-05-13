package org.sagebionetworks.repo.manager.discussion;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class DiscussionUtilsTest {

	@Test (expected = IllegalArgumentException.class)
	public void testGetMentionedUsernameCaseNullMarkdown() {
		DiscussionUtils.getMentionedUsername(null);
	}

	@Test
	public void testGetMentionedUsernameCaseZeroUsers() {
		assertEquals(new HashSet<String>(), DiscussionUtils.getMentionedUsername(""));
	}

	@Test
	public void testGetMentionedUsernameCaseOnlyUsername() {
		Set<String> expected = new HashSet<String>();
		expected.add("anonymous");
		assertEquals(expected, DiscussionUtils.getMentionedUsername("@anonymous"));
	}

	@Test
	public void testGetMentionedUsernameCaseWhiteSpacePrefix(){
		Set<String> expected = new HashSet<String>();
		expected.add("anonymous");
		assertEquals(expected, DiscussionUtils.getMentionedUsername("\t@anonymous"));
	}

	@Test
	public void testGetMentionedUsernameCaseWhiteSpacePostfix(){
		Set<String> expected = new HashSet<String>();
		expected.add("anonymous");
		assertEquals(expected, DiscussionUtils.getMentionedUsername("@anonymous\n"));
	}

	@Test
	public void testGetMentionedUsernameCaseDoubleAlpha(){
		Set<String> expected = new HashSet<String>();
		expected.add("anonymous");
		assertEquals(expected, DiscussionUtils.getMentionedUsername("@anon@ymous"));
	}

	@Test
	public void testGetMentionedUsernameCaseContainSpecialChar(){
		Set<String> expected = new HashSet<String>();
		expected.add("anonymous?#$%!^&*)(+-_=`~/'\"|.,<>");
		assertEquals(expected, DiscussionUtils.getMentionedUsername("@anonymous?#$%!^&*)(+-_=`~/'\"|.,<>"));
	}

	@Test
	public void testGetMentionedUsernameCaseMultipleUsername() {
		Set<String> expected = new HashSet<String>();
		expected.add("anonymous");
		expected.add("saturn");
		assertEquals(expected, DiscussionUtils.getMentionedUsername("@anonymous\n@saturn"));
	}

	@Test
	public void testGetMentionedUsernameCaseEmptyStringUsername() {
		Set<String> expected = new HashSet<String>();
		expected.add("anonymous");
		assertEquals(expected, DiscussionUtils.getMentionedUsername("@ @anonymous"));
	}

	@Test
	public void testGetMentionedUsernameCaseSimilarUsername() {
		Set<String> expected = new HashSet<String>();
		expected.add("username1");
		expected.add("username2");
		assertEquals(expected, DiscussionUtils.getMentionedUsername("@username1 @username2"));
	}
}
