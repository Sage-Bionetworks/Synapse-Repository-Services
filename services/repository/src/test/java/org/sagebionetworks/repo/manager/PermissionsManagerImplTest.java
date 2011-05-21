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
public class PermissionsManagerImplTest {

	@Before
	public void setUp() throws Exception {
		// create a node
		// create a child
		// set ACL on parent
	}

	@After
	public void tearDown() throws Exception {
		// delete parent node
	}

	@Test
	public void testGetACL() {
		// retrieve parent acl
		// retrieve child acl.  should get parent's
//		fail("Not yet implemented");
	}

	@Test
	public void testValidateContent() {
//		fail("Not yet implemented");
	}

	@Test
	public void testUpdateACL() {
		// check that you get an exception if
		// group id is null
		// resource id is null
		// no access type is specified
//		fail("Not yet implemented");
	}

	@Test
	public void testOverrideInheritance() {
		// should get exception if object already has an acl
		// should get exception if you don't have authority to change permissions
		// call 'getACL':  the ACL should match the requested settings and specify the resource as the owner of the ACL
//		fail("Not yet implemented");
	}

	@Test
	public void testRestoreInheritance() {
		// should get exception if resource already inherits
		// should get exception if don't have authority to change permissions
		// should get exception if resource doen't have a parent
		// call 'getACL' on the resource.  The returned ACL should specify someone else as the ACL owner
//		fail("Not yet implemented");
	}

}
