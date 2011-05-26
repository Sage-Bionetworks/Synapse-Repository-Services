package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.HashSet;

import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.persistence.JDOAccessControlList;
import org.sagebionetworks.repo.model.jdo.persistence.JDONode;

/**
 * Test for AccessControlListUtil.
 * 
 * @author jmhill
 *
 */
public class AccessControlListUtilTest {
	
	@Test
	public void testRoundTrip() throws DatastoreException, InvalidModelException{
		JDONode mockNode = Mockito.mock(JDONode.class);
		when(mockNode.getId()).thenReturn(110001L);
		// use a stub user info
		UserInfo info = UserInfoUtils.createValidUserInfo();
		// Now create a populated ACL
		AccessControlList dto = AccessControlList.createACLToGrantAll(mockNode.getId().toString(), info);
		assertNotNull(dto);
		dto.setId("33");
		dto.setEtag("44");

		//
		ResourceAccess as2 = new ResourceAccess();
		as2.setId("213");
		as2.setAccessType(new HashSet<ACCESS_TYPE>());
		as2.getAccessType().add(ACCESS_TYPE.CHANGE_PERMISSIONS);
		as2.getAccessType().add(ACCESS_TYPE.UPDATE);
		as2.setUserGroupId("234");
		dto.getResourceAccess().add(as2);
		ResourceAccess secondAcces = new ResourceAccess();
		secondAcces.setAccessType(new HashSet<ACCESS_TYPE>());
		secondAcces.getAccessType().add(ACCESS_TYPE.READ);
		secondAcces.getAccessType().add(ACCESS_TYPE.UPDATE);
		secondAcces.setUserGroupId("345");
		dto.getResourceAccess().add(secondAcces);
		assertEquals(3, dto.getResourceAccess().size());
		// Now do the round trip
		JDOAccessControlList jdo = AccessControlListUtil.createJdoFromDto(dto, mockNode);
		assertNotNull(jdo);
		// Now go from the jdo back to a dto
		AccessControlList dtoClone = AccessControlListUtil.createDtoFromJdo(jdo);
		assertNotNull(dtoClone);
		// This should match what was sent
		assertEquals(dtoClone.getResourceAccess(), dto.getResourceAccess());
		assertEquals(dtoClone, dto);
	}
}
