/**
 * 
 */
package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.ResourceAccess2;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author bhoff
 *
 */
public class JDOAccessControlListDAOImplTest {
	
	@Autowired
	private AccessControlListDAO jdoAccessControlListDAO;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link org.sagebionetworks.repo.model.jdo.JDOAccessControlListDAOImpl#getForResource(java.lang.String)}.
	 */
	@Test
	public void testGetForResource() {
//		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.sagebionetworks.repo.model.jdo.JDOAccessControlListDAOImpl#canAccess(java.util.Collection, java.lang.String, org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE)}.
	 */
	@Test
	public void testCanAccess() {
//		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.sagebionetworks.repo.model.jdo.JDOAccessControlListDAOImpl#authorizationSQL()}.
	 */
	@Test
	public void testAuthorizationSQL() {
//		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.sagebionetworks.repo.model.jdo.JDOBaseDAOImpl#create(org.sagebionetworks.repo.model.Base)}.
	 */
	@Ignore
	@Test
	public void testCreateGetAndDelete() throws Exception {
		AccessControlList acl = new AccessControlList();
		acl.setCreationDate(new Date());
		acl.setCreatedBy("me");
		acl.setModifiedOn(new Date());
		acl.setModifiedBy("you");
		acl.setResourceId("101");
		Set<ResourceAccess2> ras = new HashSet<ResourceAccess2>();
		ResourceAccess2 ra = new ResourceAccess2();
		ras.add(ra);
		acl.setResourceAccess(ras);
		jdoAccessControlListDAO.create(acl);
		
		//AccessControlList = 
	}

	/**
	 * Test method for {@link org.sagebionetworks.repo.model.jdo.JDOBaseDAOImpl#get(java.lang.String)}.
	 */
	@Test
	public void testGet() {
//		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.sagebionetworks.repo.model.jdo.JDOBaseDAOImpl#getAll()}.
	 */
	@Test
	public void testGetAll() {
//		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link org.sagebionetworks.repo.model.jdo.JDOBaseDAOImpl#update(org.sagebionetworks.repo.model.Base)}.
	 */
	@Test
	public void testUpdate() {
//		fail("Not yet implemented");
	}

}
