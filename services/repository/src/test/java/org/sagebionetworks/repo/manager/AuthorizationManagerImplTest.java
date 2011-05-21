package org.sagebionetworks.repo.manager;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:manager-test-context.xml" })
public class AuthorizationManagerImplTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testCanAccess() {
		// test that a user can access something they've been given access to individually
		// test that a user can access something accessible to a group they belong to
		// test that a user can't access something they haven't been given access to
		// test that anonymous can't access something a group can access
		// test that an admin can access anything
		// test that a user can access a Public resource
		// test that anonymous can access a Public resource
		
		// test access to something that inherits its permissions
		// test lack of access to something that doesn't inherit its permissions, whose parent you CAN access
//		fail("Not yet implemented");
	}

	@Test
	public void testCanCreate() {
		// make an object on which you have READ and WRITE permission
		// try to add a child
		// should fail
		// add CREATE permission on the parent
		// try to add the child
		// should be successful
		
		// admin should be able to add child, without any explicit permissions
		// anonymous should not be able to add child
		
		// try to create node with no parent.  should fail
		// admin creates a node with no parent.  should work
//		fail("Not yet implemented");
	}

}
