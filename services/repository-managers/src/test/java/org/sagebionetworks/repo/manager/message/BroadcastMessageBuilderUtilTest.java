package org.sagebionetworks.repo.manager.message;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.repo.model.message.ChangeType;

public class BroadcastMessageBuilderUtilTest {

	@Test
	public void testGetAction() {
		assertEquals("created", BroadcastMessageBuilderUtil.getAction(ChangeType.CREATE));
		assertEquals("updated", BroadcastMessageBuilderUtil.getAction(ChangeType.UPDATE));
		assertEquals("removed", BroadcastMessageBuilderUtil.getAction(ChangeType.DELETE));
	}

	
	@Test
	public void testTruncateStringOver(){
		String input = "123456789";
		String truncate = BroadcastMessageBuilderUtil.truncateString(input, 4);
		assertEquals("1234...", truncate);
	}

	@Test
	public void testTruncateStringUnder(){
		String input = "123456789";
		String truncate = BroadcastMessageBuilderUtil.truncateString(input, input.length());
		assertEquals(input, truncate);
	}
}
