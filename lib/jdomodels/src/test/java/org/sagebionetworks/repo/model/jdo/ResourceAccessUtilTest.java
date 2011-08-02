package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import java.util.HashSet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.jdo.persistence.JDOResourceAccess;

/**
 * Test for ResourceAccessUtil.
 * @author jmhill
 *
 */
public class ResourceAccessUtilTest {

	private String userOneName = "userOne";
	private Long userOneId = new Long(45);
	
	
	@Test
	public void testRoundTrip() throws DatastoreException, InvalidModelException{
		// Start with the DTO
		ResourceAccess dto = new ResourceAccess();
		dto.setGroupName(userOneName);
		dto.setAccessType(new HashSet<ACCESS_TYPE>());
		dto.getAccessType().add(ACCESS_TYPE.CHANGE_PERMISSIONS);
		dto.getAccessType().add(ACCESS_TYPE.DELETE);
		// Now create a JDO from this object
		JDOResourceAccess jdo = ResourceAccessUtil.createJdoFromDto(dto, 123l);
		assertNotNull(jdo);
		// Now clone the dto from the JDO
		ResourceAccess dtoClone = ResourceAccessUtil.createDtoFromJdo(jdo, userOneName);
		assertNotNull(dtoClone);
		assertEquals(dto, dtoClone);
		// Check the sets as well
		assertNotNull(dtoClone.getAccessType());
		assertEquals(dto.getAccessType(), dtoClone.getAccessType());
	}
	
	@Test
	public void testNonNullIdRoudnTrip() throws DatastoreException, InvalidModelException{
		// Start with the DTO
		ResourceAccess dto = new ResourceAccess();
		dto.setGroupName(userOneName);
		dto.setAccessType(new HashSet<ACCESS_TYPE>());
		dto.getAccessType().add(ACCESS_TYPE.CHANGE_PERMISSIONS);
		dto.getAccessType().add(ACCESS_TYPE.DELETE);
		// Now create a JDO from this object
		JDOResourceAccess jdo = ResourceAccessUtil.createJdoFromDto(dto, 445l);
		assertNotNull(jdo);
		// Now clone the dto from the JDO
		ResourceAccess dtoClone = ResourceAccessUtil.createDtoFromJdo(jdo, userOneName);
		assertNotNull(dtoClone);
		assertEquals(dto, dtoClone);
		// Check the sets as well
		assertNotNull(dtoClone.getAccessType());
		assertEquals(dto.getAccessType(), dtoClone.getAccessType());
	}
}
