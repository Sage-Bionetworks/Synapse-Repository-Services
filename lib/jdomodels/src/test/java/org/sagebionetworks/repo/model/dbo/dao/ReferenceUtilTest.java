package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.dbo.persistence.DBOReference;

public class ReferenceUtilTest {

	@Test
	public void testCreateDBOReference() throws DatastoreException{
		// Build up references
		Long ownerId = new Long(34);
		Reference ref = new Reference();
		ref.setTargetId("123");
		ref.setTargetVersionNumber(new Long(0));
		DBOReference dbo =  ReferenceUtil.createDBOReference(ownerId, ref);
		assertEquals(ownerId, dbo.getOwner());
		assertEquals(new Long(123), dbo.getTargetId());
		assertEquals(new Long(0), dbo.getTargetRevision());
	}
}
