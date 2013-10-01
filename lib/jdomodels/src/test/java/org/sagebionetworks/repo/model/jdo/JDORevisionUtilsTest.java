package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;
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
		
		DBONode owner = new DBONode();
		owner.setId(12l);
		DBORevision original = new DBORevision();
		original.setOwner(owner.getId());
		original.setRevisionNumber(2L);
		original.setAnnotations(blob);
		original.setLabel("0.3.9");
		original.setModifiedBy(Long.parseLong(createdById));
		original.setModifiedOn(3123l);
		original.setFileHandleId(999999l);
		// Now make a copy
		DBORevision copy = JDORevisionUtils.makeCopyForNewVersion(original);
		assertNotNull(copy);
		// The copy should not equal the original since it will have an incremented version number.
		assertFalse(original.equals(copy));
		// Make sure the version number is incremented
		assertEquals(new Long(original.getRevisionNumber()+1), copy.getRevisionNumber());
		// We do not copy over the label or the comment
		assertEquals(null, copy.getLabel());
		assertEquals(null, copy.getComment());
		// We do make a copy of the annotations blob
		// but it should be a copy and not the original
		assertTrue(original.getAnnotations() != copy.getAnnotations());
		assertTrue(Arrays.equals(original.getAnnotations(), copy.getAnnotations()));
		// the file handle should be copied.
		assertEquals(original.getFileHandleId(), copy.getFileHandleId());
	}


}
