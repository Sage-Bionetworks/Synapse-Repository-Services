package org.sagebionetworks.repo.model.bootstrap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class BootstrapRootFolderTest {
	
	@Autowired
	private EntityBootstrapData rootFolderBootstrapData;
	
	@Autowired
	private EntityBootstrapData trashFolderBootstrapData;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Test
	public void testRootBootstrapDataLoad() throws Exception {
		assertNotNull(rootFolderBootstrapData);
		// Check the values
		assertEquals("/root", rootFolderBootstrapData.getEntityPath());
		assertEquals(EntityType.folder, rootFolderBootstrapData.getEntityType());
		assertNotNull(rootFolderBootstrapData);
		assertNotNull(rootFolderBootstrapData.getAccessList());
		assertEquals(1, rootFolderBootstrapData.getAccessList().size());
		AccessBootstrapData access = rootFolderBootstrapData.getAccessList().get(0);
		assertNotNull(access);

		assertNotNull(access.getAccessTypeList());
		assertEquals(1, access.getAccessTypeList().size());
		assertEquals(ACCESS_TYPE.CREATE, access.getAccessTypeList().get(0));
	}


	@Test
	public void testTrashBootstrapDataLoad() throws Exception {

		assertNotNull(trashFolderBootstrapData);
		assertEquals("/root/trash", trashFolderBootstrapData.getEntityPath());
		assertEquals(EntityType.folder, trashFolderBootstrapData.getEntityType());

		assertNotNull(trashFolderBootstrapData.getAccessList());
		assertEquals(1, trashFolderBootstrapData.getAccessList().size());
		AccessBootstrapData access = trashFolderBootstrapData.getAccessList().get(0);
		assertNotNull(access);
		assertNotNull(access.getAccessTypeList());
		assertEquals(access.getGroup(), BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP);
		assertEquals(1, access.getAccessTypeList().size());
		assertEquals(ACCESS_TYPE.CREATE, access.getAccessTypeList().get(0));
	}
}
