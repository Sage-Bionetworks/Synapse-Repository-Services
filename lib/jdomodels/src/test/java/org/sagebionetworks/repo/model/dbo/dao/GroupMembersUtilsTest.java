package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class GroupMembersUtilsTest {

	@Test
	public void testZipUnzip() throws Exception {
		List<String> original = new ArrayList<String>();
		original.add("1");
		original.add("2");
		original.add("34");
		original.add("567");
		original.add("8901");
		original.add("23456789");
		original.add(Long.toString(Long.MIN_VALUE));
		original.add(Long.toString(Long.MAX_VALUE));
		
		List<String> processed = GroupMembersUtils.unzip(GroupMembersUtils.zip(original));
		assertTrue(original.containsAll(processed));
		assertTrue(processed.containsAll(original));
	}

}
