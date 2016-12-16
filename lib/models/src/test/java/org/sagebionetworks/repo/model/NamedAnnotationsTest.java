package org.sagebionetworks.repo.model;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class NamedAnnotationsTest {
	
	@Test
	public void testConstructor(){
		NamedAnnotations named = new NamedAnnotations();
		Map<String, Annotations> map = named.getMap();
		assertNotNull(map);
		assertEquals(2, map.size());
		assertNotNull(map.get(AnnotationNameSpace.PRIMARY.name()));
		assertNotNull(map.get(AnnotationNameSpace.ADDITIONAL.name()));
	}
	
	@Test
	public void testGetAnnotationsForName(){
		NamedAnnotations named = new NamedAnnotations();
		assertNull(named.getEtag());
		assertNull(named.getId());
		assertNull(named.getCreationDate());
		Annotations anno = named.getAnnotationsForName(AnnotationNameSpace.ADDITIONAL);
		assertNotNull(anno);
		// Now make sure the values get passed on the get
		named.setId("12");
		assertEquals("12", named.getId());
		named.setEtag("55");
		assertEquals("55", named.getEtag());
		Date now = new Date();
		named.setCreationDate(now);
		assertEquals(now, named.getCreationDate());
		anno = named.getAnnotationsForName(AnnotationNameSpace.ADDITIONAL);
		assertNotNull(anno);
		assertEquals(named.getId(), anno.getId());
		assertEquals(named.getEtag(), anno.getEtag());
		assertEquals(named.getCreationDate(), anno.getCreationDate());
	}
	
	
	@Test
	public void testIterator(){
		NamedAnnotations named = new NamedAnnotations();
		Iterator<AnnotationNameSpace> it = named.nameIterator();
		assertNotNull(it);
		Set<AnnotationNameSpace> expected = new HashSet<AnnotationNameSpace>();
		expected.add(AnnotationNameSpace.ADDITIONAL);
		expected.add(AnnotationNameSpace.PRIMARY);
		while(it.hasNext()){
			AnnotationNameSpace name = it.next();
			assertTrue(expected.contains(name));
			expected.remove(name);
		}
		assertEquals("Did not find all of the expected values using the iterator.",0, expected.size());
	}

}
