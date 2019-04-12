package org.sagebionetworks.repo.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NamedAnnotationsTest {

	NamedAnnotations namedAnnotations;

	@BeforeEach
	public void setUp(){
		namedAnnotations = new NamedAnnotations();
	}
	
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
		Annotations anno = named.getAnnotationsForName(AnnotationNameSpace.ADDITIONAL);
		assertNotNull(anno);
		// Now make sure the values get passed on the get
		named.setId("12");
		assertEquals("12", named.getId());
		named.setEtag("55");
		assertEquals("55", named.getEtag());
		anno = named.getAnnotationsForName(AnnotationNameSpace.ADDITIONAL);
		assertNotNull(anno);
		assertEquals(named.getId(), anno.getId());
		assertEquals(named.getEtag(), anno.getEtag());
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
		assertEquals(0, expected.size(), "Did not find all of the expected values using the iterator.");
	}

	@Test
	public void testIsEmpty_mapIsEmpty(){
		NamedAnnotations namedAnnos = new NamedAnnotations();
		assertTrue(namedAnnos.isEmpty());
	}

	@Test
	public void testIsEmpty_PrimaryAnnotationsNotEmpty(){
		assertTrue(namedAnnotations.isEmpty());
		namedAnnotations.getPrimaryAnnotations().addAnnotation("key", "value");
		assertFalse(namedAnnotations.isEmpty());
	}

	@Test
	public void testIsEmpty_AdditionalAnnotationsNotEmpty(){
		assertTrue(namedAnnotations.isEmpty());
		namedAnnotations.getAdditionalAnnotations().addAnnotation("key", "value");
		assertFalse(namedAnnotations.isEmpty());
	}

}
