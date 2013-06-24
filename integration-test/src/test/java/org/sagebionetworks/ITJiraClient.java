package org.sagebionetworks;
import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.repo.util.jrjc.*;

import com.atlassian.jira.rest.client.api.domain.Field;


public class ITJiraClient {
	
	/**
	 * We can't test creating issues since there is no API for deleting them once created.
	 * But we *can* test connectivity to the server which is the thing most likely to break.
	 * @throws Exception
	 */
	@Test
	public void testConnectivity() throws Exception {
		JiraClient jiraClient = new JiraClientImpl();
		Iterable<Field> fieldsIterator = jiraClient.getFields();
		// we simply check that there is at least one field returned
		boolean foundAny = false;
		for (Field field : fieldsIterator) {
			foundAny = true;
		}
		assertTrue(foundAny);
	}
}
