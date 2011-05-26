package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.*;

import java.util.HashSet;

import org.junit.Test;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.jdo.persistence.JDOResourceAccess;

/**
 * Test for ResourceAccessUtil.
 * @author jmhill
 *
 */
public class ResourceAccessUtilTest {

	
	@Test
	public void testRoundTrip() throws DatastoreException, InvalidModelException{
		// Start with the DTO
		ResourceAccess dto = new ResourceAccess();
		dto.setUserGroupId("33");
		dto.setAccessType(new HashSet<ACCESS_TYPE>());
		dto.getAccessType().add(ACCESS_TYPE.CHANGE_PERMISSIONS);
		dto.getAccessType().add(ACCESS_TYPE.DELETE);
		// Now create a JDO from this object
		JDOResourceAccess jdo = ResourceAccessUtil.createJdoFromDto(dto);
		assertNotNull(jdo);
		// Now clone the dto from the JDO
		ResourceAccess dtoClone = ResourceAccessUtil.createDtoFromJdo(jdo);
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
		dto.setUserGroupId("33");
		dto.setId("33");
		dto.setAccessType(new HashSet<ACCESS_TYPE>());
		dto.getAccessType().add(ACCESS_TYPE.CHANGE_PERMISSIONS);
		dto.getAccessType().add(ACCESS_TYPE.DELETE);
		// Now create a JDO from this object
		JDOResourceAccess jdo = ResourceAccessUtil.createJdoFromDto(dto);
		assertNotNull(jdo);
		// Now clone the dto from the JDO
		ResourceAccess dtoClone = ResourceAccessUtil.createDtoFromJdo(jdo);
		assertNotNull(dtoClone);
		assertEquals(dto, dtoClone);
		// Check the sets as well
		assertNotNull(dtoClone.getAccessType());
		assertEquals(dto.getAccessType(), dtoClone.getAccessType());
	}
}
