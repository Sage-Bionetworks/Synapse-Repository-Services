package org.sagebionetworks.repo.model.jdo;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Random;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.dbo.persistence.DBORevision;

/**
 * A unit test for JDORevisionUtils
 * 
 *
 */
public class JDORevisionUtilsTest {
	
	@Test
	public void testMakeCopyForNewVersion() throws Exception {
		String createdById = new Long(77777).toString();
		// Create a random blob for this revision's annotatoins
		Random rand = new Random(565);
		int size = rand.nextInt(100);
		// Exclude zero
		size++;
		byte[] blob = new byte[size];
		rand.nextBytes(blob);

		byte[] entityPropertyAnnotations = new byte[size];
		rand.nextBytes(entityPropertyAnnotations);

		DBONode owner = new DBONode();
		owner.setId(12l);
		DBORevision original = new DBORevision();
		original.setOwner(owner.getId());
		original.setRevisionNumber(2L);
		original.setUserAnnotationsJSON("{}");
		original.setEntityPropertyAnnotations(entityPropertyAnnotations);
		original.setLabel("0.3.9");
		original.setModifiedBy(Long.parseLong(createdById));
		original.setModifiedOn(3123L);
		original.setFileHandleId(999999L);
		
		Long newRevisionNumber = original.getRevisionNumber() + 1;
		// Now make a copy
		DBORevision copy = JDORevisionUtils.makeCopyForNewVersion(original, newRevisionNumber);
		
		assertNotNull(copy);
		// The copy should not equal the original since it will have an incremented version number.
		assertNotEquals(original, copy);
		// Make sure the version number is set correctly
		assertEquals(newRevisionNumber, copy.getRevisionNumber());
		// We do not copy over the label or the comment
		assertNull(copy.getLabel());
		assertNull(copy.getComment());
		// We do make a copy of the annotations blob
		// but it should be a copy and not the original
		assertEquals(original.getUserAnnotationsJSON(), copy.getUserAnnotationsJSON());
		assertNotSame(original.getEntityPropertyAnnotations(), copy.getEntityPropertyAnnotations());
		assertArrayEquals(original.getEntityPropertyAnnotations(), copy.getEntityPropertyAnnotations());
		// the file handle should be copied.
		assertEquals(original.getFileHandleId(), copy.getFileHandleId());
	}


}
