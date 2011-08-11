package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.persistence.JDOAccessControlList;
import org.sagebionetworks.repo.model.jdo.persistence.JDONode;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Test for AccessControlListUtil.
 * 
 * @author jmhill
 *
 */
public class AccessControlListUtilTest {
	
	private String userOneName = "userOne";
	private Long userOneId = new Long(45);
	
	private String userTwoName = "userTwo";
	private Long userTwoId = new Long(33);
	
	private UserGroupCache mockCache;
	private UserInfo mockInfo;
	@Before
	public void setup() throws DatastoreException, NotFoundException{
		mockInfo = UserInfoUtils.createValidUserInfo();
		UserGroup ug = mockInfo.getIndividualGroup();
		
		
		mockCache = Mockito.mock(UserGroupCache.class);
		// User one
		when(mockCache.getIdForUserGroupName(userOneName)).thenReturn(userOneId);
		when(mockCache.getUserGroupNameForId(userOneId)).thenReturn(userOneName);
		// Two
		when(mockCache.getIdForUserGroupName(userTwoName)).thenReturn(userTwoId);
		when(mockCache.getUserGroupNameForId(userTwoId)).thenReturn(userTwoName);
		// Three
		when(mockCache.getIdForUserGroupName(ug.getName())).thenReturn(KeyFactory.stringToKey(ug.getId()));
		when(mockCache.getUserGroupNameForId(KeyFactory.stringToKey(ug.getId()))).thenReturn(ug.getName());
		
	}
	
	@Test
	public void testRoundTrip() throws DatastoreException, InvalidModelException, NotFoundException{
		JDONode mockNode = Mockito.mock(JDONode.class);
		when(mockNode.getId()).thenReturn(110001L);
		when(mockNode.geteTag()).thenReturn(4L);
		// use a stub user info

		// Now create a populated ACL
		AccessControlList dto = AccessControlList.createACLToGrantAll(mockNode.getId().toString(), mockInfo);
		assertNotNull(dto);
	
		// Setup two users with different access.
		ResourceAccess as2 = new ResourceAccess();
		as2.setAccessType(new HashSet<ACCESS_TYPE>());
		as2.getAccessType().add(ACCESS_TYPE.CHANGE_PERMISSIONS);
		as2.getAccessType().add(ACCESS_TYPE.UPDATE);
		as2.setGroupName(userOneName);
		dto.getResourceAccess().add(as2);
		
		ResourceAccess secondAcces = new ResourceAccess();
		secondAcces.setAccessType(new HashSet<ACCESS_TYPE>());
		secondAcces.getAccessType().add(ACCESS_TYPE.READ);
		secondAcces.getAccessType().add(ACCESS_TYPE.UPDATE);
		dto.getResourceAccess().add(secondAcces);
		secondAcces.setGroupName(userTwoName);
		assertEquals(3, dto.getResourceAccess().size());
		// Now do the round trip
		JDOAccessControlList jdo = AccessControlListUtil.createJdoFromDto(dto, mockNode, mockCache);
		assertNotNull(jdo);
		// Now go from the jdo back to a dto
		AccessControlList dtoClone = AccessControlListUtil.createDtoFromJdo(jdo, "4", mockCache);
		assertNotNull(dtoClone);
		// This should match what was sent
		assertEquals(dtoClone.getResourceAccess(), dto.getResourceAccess());
		assertEquals(dtoClone, dto);
	}
}
