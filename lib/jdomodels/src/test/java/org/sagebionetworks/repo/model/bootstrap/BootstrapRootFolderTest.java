package org.sagebionetworks.repo.model.bootstrap;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_GROUPS;
import org.sagebionetworks.repo.model.ObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:bootstrap-entites-spb.xml" })
public class BootstrapRootFolderTest {
	
	@Autowired
	EntityBootstrapData rootFolderBootstrapData;
	@Autowired
	EntityBootstrapData agreementFolderBootstrapData;
	
	@Test
	public void testRootBootstrapDataLoad(){
		assertNotNull(rootFolderBootstrapData);
		// Check the values
		assertEquals("/root", rootFolderBootstrapData.getEntityPath());
		assertEquals(ObjectType.folder, rootFolderBootstrapData.getEntityType());
		assertNotNull(rootFolderBootstrapData);
		assertNotNull(rootFolderBootstrapData.getAccessList());
		assertEquals(1, rootFolderBootstrapData.getAccessList().size());
		AccessBootstrapData access = rootFolderBootstrapData.getAccessList().get(0);
		assertNotNull(access);
		assertEquals(DEFAULT_GROUPS.AUTHENTICATED_USERS, access.getGroup());
		assertNotNull(access.getAccessTypeList());
		assertEquals(1, access.getAccessTypeList().size());
		assertEquals(ACCESS_TYPE.CREATE, access.getAccessTypeList().get(0));
	}
	
	@Test
	public void testAgreementBootstrapDataLoad(){
		assertNotNull(agreementFolderBootstrapData);
		// Check the values
		assertEquals("/root/agreements", agreementFolderBootstrapData.getEntityPath());
		assertEquals(ObjectType.folder, agreementFolderBootstrapData.getEntityType());
		assertNotNull(agreementFolderBootstrapData);
		assertNotNull(agreementFolderBootstrapData.getAccessList());
		assertEquals(2, agreementFolderBootstrapData.getAccessList().size());
		AccessBootstrapData access = agreementFolderBootstrapData.getAccessList().get(0);
		assertNotNull(access);
		assertEquals(DEFAULT_GROUPS.AUTHENTICATED_USERS, access.getGroup());
		assertNotNull(access.getAccessTypeList());
		assertEquals(2, access.getAccessTypeList().size());
		assertEquals(ACCESS_TYPE.CREATE, access.getAccessTypeList().get(0));
		assertEquals(ACCESS_TYPE.READ, access.getAccessTypeList().get(1));
		
		access = agreementFolderBootstrapData.getAccessList().get(1);
		assertNotNull(access);
		assertEquals(DEFAULT_GROUPS.PUBLIC, access.getGroup());
		assertNotNull(access.getAccessTypeList());
		assertEquals(1, access.getAccessTypeList().size());
		assertEquals(ACCESS_TYPE.READ, access.getAccessTypeList().get(0));
	}

}
