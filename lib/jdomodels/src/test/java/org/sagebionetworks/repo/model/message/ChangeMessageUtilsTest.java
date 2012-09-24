package org.sagebionetworks.repo.model.message;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.sagebionetworks.repo.model.dbo.persistence.DBOChange;

/**
 * Test for ChangeMessageUtils.
 * 
 * @author John
 *
 */
public class ChangeMessageUtilsTest {
	
	@Test
	public void testRoundTrip(){
		// Start with a list
		List<ChangeMessage> list = new ArrayList<ChangeMessage>();
		for(int i=0; i<3; i++){
			ChangeMessage cm = new ChangeMessage();
			cm.setChangeType(ChangeType.CREATE);
			cm.setObjectEtag("etag"+i);
			cm.setObjectType(ObjectType.ENTITY);
			cm.setObjectId("syn"+i);
			list.add(cm);
		}
		// Make the round trip
		List<DBOChange> dboList = ChangeMessageUtils.createDBOList(list);
		assertNotNull(dboList);
		// Now return
		List<ChangeMessage> clone = ChangeMessageUtils.createDTOList(dboList);
		assertNotNull(clone);
		assertEquals("Failed to make a round trip without data loss", list, clone);
	}

}
