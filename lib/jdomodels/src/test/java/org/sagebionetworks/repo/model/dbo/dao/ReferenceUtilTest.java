package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.dbo.persistence.DBOReference;

public class ReferenceUtilTest {

	@Test
	public void testCreateDBOReferences() throws DatastoreException{
		// Build up references
		Long ownerId = new Long(34);
		Map<String, Set<Reference>> map = new HashMap<String, Set<Reference>>();
		Set<Reference> set = new HashSet<Reference>();
		Reference ref = new Reference();
		ref.setTargetId("123");
		ref.setTargetVersionNumber(new Long(0));
		set.add(ref);
		map.put("groupOne", set);
		List<DBOReference> results =  ReferenceUtil.createDBOReferences(ownerId, map);
		assertNotNull(results);
		assertEquals(1, results.size());
		DBOReference dbo = results.get(0);
		assertEquals(ownerId, dbo.getOwner());
		assertEquals("groupOne", dbo.getGroupName());
		assertEquals(new Long(123), dbo.getTargetId());
		assertEquals(new Long(0), dbo.getTargetRevision());
		
	}
}
