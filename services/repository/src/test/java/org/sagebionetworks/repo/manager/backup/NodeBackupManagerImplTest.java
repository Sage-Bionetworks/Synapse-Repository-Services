package org.sagebionetworks.repo.manager.backup;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.repo.model.UserGroup;

/**
 * Unit tests for NodeBackupManagerImpl
 * @author jmhill
 *
 */
public class NodeBackupManagerImplTest {
	
	@Test
	public void testCreateUserGroupForNameUser(){
		// All user names are email addresses.
		String userName = "something@someother.net";
		UserGroup ug = NodeBackupManagerImpl.createUserGroupForName(userName);
		assertNotNull(ug);
		assertEquals(userName, ug.getName());
		assertTrue(ug.isIndividual());
	}
	
	@Test
	public void testCreateUserGroupForNameGroup(){
		// A group cannot have an email address for a name.
		String groupName = "AUTHENTICATED_USERS";
		UserGroup ug = NodeBackupManagerImpl.createUserGroupForName(groupName);
		assertNotNull(ug);
		assertEquals(groupName, ug.getName());
		assertFalse(ug.isIndividual());
	}

}
