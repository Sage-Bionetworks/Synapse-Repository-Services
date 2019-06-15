package org.sagebionetworks.repo.manager.discussion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.sagebionetworks.repo.model.discussion.DiscussionThreadEntityReference;

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
		expected.add("anon");
		expected.add("ymous");
		assertEquals(expected, DiscussionUtils.getMentionedUsername("@anon@ymous"));
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
	public void getMentionedUsernameSurroundedByParentheses() {
		Set<String> expected = new HashSet<String>();
		expected.add("anonymous");
		assertEquals(expected, DiscussionUtils.getMentionedUsername("(@anonymous)"));
	}

	@Test
	public void getMentionedUsernameFollowedByComma() {
		Set<String> expected = new HashSet<String>();
		expected.add("anonymous");
		assertEquals(expected, DiscussionUtils.getMentionedUsername("@anonymous, check this out"));
	}

	@Test
	public void getUsernameWithValidSpecialChars() {
		Set<String> expected = new HashSet<String>();
		expected.add("a.non-ymo_us");
		assertEquals(expected, DiscussionUtils.getMentionedUsername("@a.non-ymo_us"));
	}

	@Test
	public void testGetMentionedUsernameCaseSimilarUsername() {
		Set<String> expected = new HashSet<String>();
		expected.add("username1");
		expected.add("username2");
		assertEquals(expected, DiscussionUtils.getMentionedUsername("@username1 @username2"));
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetEntityRefCaseNullMarkdown() {
		DiscussionUtils.getEntityReferences(null, "1");
	}

	@Test (expected = IllegalArgumentException.class)
	public void testGetEntityRefCaseNullThreadId() {
		DiscussionUtils.getEntityReferences("", null);
	}

	@Test
	public void testGetEntityRefCaseZeroEntities() {
		assertEquals(new ArrayList<DiscussionThreadEntityReference>(), DiscussionUtils.getEntityReferences("", "1"));
	}

	@Test
	public void testGetEntityRefCaseOnlyOneEntity() {
		List<DiscussionThreadEntityReference> expected = new ArrayList<DiscussionThreadEntityReference>();
		DiscussionThreadEntityReference ref = new DiscussionThreadEntityReference();
		ref.setEntityId("123");
		ref.setThreadId("1");
		expected.add(ref);
		assertEquals(expected, DiscussionUtils.getEntityReferences("syn123", "1"));
	}

	@Test
	public void testGetEntityRefCaseWhiteSpacePrefix(){
		List<DiscussionThreadEntityReference> expected = new ArrayList<DiscussionThreadEntityReference>();
		DiscussionThreadEntityReference ref = new DiscussionThreadEntityReference();
		ref.setEntityId("123");
		ref.setThreadId("1");
		expected.add(ref);
		assertEquals(expected, DiscussionUtils.getEntityReferences("\tsyn123", "1"));
	}

	@Test
	public void testGetEntityRefCaseWhiteSpacePostfix(){
		List<DiscussionThreadEntityReference> expected = new ArrayList<DiscussionThreadEntityReference>();
		DiscussionThreadEntityReference ref = new DiscussionThreadEntityReference();
		ref.setEntityId("123");
		ref.setThreadId("1");
		expected.add(ref);
		assertEquals(expected, DiscussionUtils.getEntityReferences("syn123\n", "1"));
	}

	@Test
	public void testGetEntityRefCaseContainNonDigitChars(){
		assertEquals(new ArrayList<DiscussionThreadEntityReference>(), DiscussionUtils.getEntityReferences("syn123ab56*", "1"));
	}

	@Test
	public void testGetEntityRefeCaseMultipleIds() {
		DiscussionThreadEntityReference ref = new DiscussionThreadEntityReference();
		ref.setEntityId("123");
		ref.setThreadId("1");
		DiscussionThreadEntityReference ref2 = new DiscussionThreadEntityReference();
		ref2.setEntityId("456");
		ref2.setThreadId("1");
		List<DiscussionThreadEntityReference> actual = DiscussionUtils.getEntityReferences("syn123\nsyn456", "1");
		assertTrue(actual.contains(ref));
		assertTrue(actual.contains(ref2));
	}

	@Test
	public void testGetEntityRefCaseMissingDigits() {
		List<DiscussionThreadEntityReference> expected = new ArrayList<DiscussionThreadEntityReference>();
		DiscussionThreadEntityReference ref = new DiscussionThreadEntityReference();
		ref.setEntityId("123");
		ref.setThreadId("1");
		expected.add(ref);
		assertEquals(expected, DiscussionUtils.getEntityReferences("syn syn123", "1"));
	}

	@Test
	public void testGetEntityRefCaseInsensitive() {
		List<DiscussionThreadEntityReference> expected = new ArrayList<DiscussionThreadEntityReference>();
		DiscussionThreadEntityReference ref = new DiscussionThreadEntityReference();
		ref.setEntityId("123");
		ref.setThreadId("1");
		expected.add(ref);
		assertEquals(expected, DiscussionUtils.getEntityReferences("sYn123", "1"));
	}

	@Test
	public void testGetEntityRefCaseParenthese() {
		List<DiscussionThreadEntityReference> expected = new ArrayList<DiscussionThreadEntityReference>();
		DiscussionThreadEntityReference ref = new DiscussionThreadEntityReference();
		ref.setEntityId("123");
		ref.setThreadId("1");
		expected.add(ref);
		assertEquals(expected, DiscussionUtils.getEntityReferences("(syn123)", "1"));
	}

	@Test
	public void testGetEntityRefCaseSpecialChars() {
		List<DiscussionThreadEntityReference> expected = new ArrayList<DiscussionThreadEntityReference>();
		DiscussionThreadEntityReference ref = new DiscussionThreadEntityReference();
		ref.setEntityId("123");
		ref.setThreadId("1");
		expected.add(ref);
		assertEquals(expected, DiscussionUtils.getEntityReferences("*^syn123)/+", "1"));
	}
}
