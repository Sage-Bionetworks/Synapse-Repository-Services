package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.jdo.JDOSecondaryPropertyUtils;

public class DBORevisionUtilsTest {

	@Test
	public void nullValue() throws IOException {
		Map<String, Set<Reference>> nullMap = null;
		byte[] blob = JDOSecondaryPropertyUtils.compressReferences(nullMap);
		assertNull(blob);
		assertNull(DBORevisionUtils.convertBlobToReference(blob));
	}
	
	@Test
	public void emptyMap() throws IOException {
		Map<String, Set<Reference>> emptyMap = new HashMap<String, Set<Reference>>();
		byte[] blob = JDOSecondaryPropertyUtils.compressReferences(emptyMap);
		assertNotNull(blob);
		assertNull(DBORevisionUtils.convertBlobToReference(blob));
	}
	
	@Test
	public void oneElementMap() throws IOException {
		Map<String, Set<Reference>> map = new HashMap<String, Set<Reference>>();
		Reference ref = new Reference();
		ref.setTargetId("123");
		ref.setTargetVersionNumber(1L);
		Set<Reference> set = new HashSet<Reference>();
		set.add(ref);
		map.put("linksTo", set);
		byte[] blob = JDOSecondaryPropertyUtils.compressReferences(map);
		assertEquals(ref, DBORevisionUtils.convertBlobToReference(blob));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void multiElementsSet() throws IOException {
		Map<String, Set<Reference>> map = new HashMap<String, Set<Reference>>();
		Reference ref1 = new Reference();
		ref1.setTargetId("123");
		ref1.setTargetVersionNumber(1L);
		Reference ref2 = new Reference();
		ref2.setTargetId("456");
		ref2.setTargetVersionNumber(2L);
		Set<Reference> set = new HashSet<Reference>();
		set.add(ref1);
		set.add(ref2);
		map.put("linksTo", set);
		byte[] blob = JDOSecondaryPropertyUtils.compressReferences(map);
		DBORevisionUtils.convertBlobToReference(blob);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void multiElementsMap() throws IOException {
		Map<String, Set<Reference>> map = new HashMap<String, Set<Reference>>();
		Reference ref1 = new Reference();
		ref1.setTargetId("123");
		ref1.setTargetVersionNumber(1L);
		Reference ref2 = new Reference();
		ref2.setTargetId("456");
		ref2.setTargetVersionNumber(2L);
		Set<Reference> set1 = new HashSet<Reference>();
		set1.add(ref1);
		Set<Reference> set2 = new HashSet<Reference>();
		set2.add(ref2);
		map.put("linksTo", set1);
		map.put("shortcut", set2);
		byte[] blob = JDOSecondaryPropertyUtils.compressReferences(map);
		DBORevisionUtils.convertBlobToReference(blob);
	}
	
	@Test 
	public void singleReference() throws IOException {
		Reference ref = new Reference();
		ref.setTargetId("123");
		ref.setTargetVersionNumber(1L);
		byte[] blob = JDOSecondaryPropertyUtils.compressReference(ref);
		assertEquals(ref, DBORevisionUtils.convertBlobToReference(blob));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void inValidInput() throws IOException {
		List<Reference> list = new ArrayList<Reference>();
		byte[] blob = JDOSecondaryPropertyUtils.compressObject(list);
		DBORevisionUtils.convertBlobToReference(blob);
	}
}
