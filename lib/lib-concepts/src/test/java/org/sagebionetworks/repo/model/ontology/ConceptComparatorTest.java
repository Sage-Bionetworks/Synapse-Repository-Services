package org.sagebionetworks.repo.model.ontology;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * Test for a comparator.
 * @author jmhill
 *
 */
public class ConceptComparatorTest {
	
	Concept a;
	Concept b;
	
	@Before
	public void before(){
		a = new Concept();
		b = new Concept();
	}
	
	
	@Test
	public void testCompareEquals(){
		a.setPreferredLabel("abc");
		a.setUri("urn:123");
		b.setPreferredLabel("abc");
		b.setUri("urn:123");
		// First test they are the same
		ConceptComparator comparator = new ConceptComparator();
		assertEquals(0, comparator.compare(a, a));
		assertEquals(0, comparator.compare(a, b));
		assertEquals(0, comparator.compare(b, a));
	}
	
	@Test (expected=NullPointerException.class)
	public void testCompareEqualsNullLabelA(){
		a.setPreferredLabel(null);
		a.setUri("urn:123");
		b.setPreferredLabel("abc");
		b.setUri("urn:123");
		// First test they are the same
		ConceptComparator comparator = new ConceptComparator();
		assertEquals(0, comparator.compare(a, b));
	}
	
	@Test (expected=NullPointerException.class)
	public void testCompareEqualsNullUriA(){
		a.setPreferredLabel("abc");
		a.setUri(null);
		b.setPreferredLabel("abc");
		b.setUri("urn:123");
		// First test they are the same
		ConceptComparator comparator = new ConceptComparator();
		assertEquals(0, comparator.compare(a, b));
	}
	
	@Test (expected=NullPointerException.class)
	public void testCompareEqualsNullLabelB(){
		a.setPreferredLabel("abc");
		a.setUri("urn:123");
		b.setPreferredLabel(null);
		b.setUri("urn:123");
		// First test they are the same
		ConceptComparator comparator = new ConceptComparator();
		assertEquals(0, comparator.compare(a, b));
	}
	
	@Test (expected=NullPointerException.class)
	public void testCompareEqualsNullUriB(){
		a.setPreferredLabel("abc");
		a.setUri("urn:123");
		b.setPreferredLabel("abc");
		b.setUri(null);
		// First test they are the same
		ConceptComparator comparator = new ConceptComparator();
		assertEquals(0, comparator.compare(a, b));
	}
	
	@Test
	public void testCompareEqualsNull(){
		// First test they are the same
		ConceptComparator comparator = new ConceptComparator();
		assertEquals(0, comparator.compare(null, null));
	}
	
	@Test
	public void testLabelFirst(){
		a.setPreferredLabel("a");
		a.setUri("urn:123");
		b.setPreferredLabel("b");
		b.setUri("urn:123");
		// First test they are the same
		ConceptComparator comparator = new ConceptComparator();
		assertEquals(-1, comparator.compare(a, b));
		assertEquals(1, comparator.compare(b, a));
	}
	
	@Test
	public void testLabelEquals(){
		a.setPreferredLabel("a");
		a.setUri("urn:a");
		b.setPreferredLabel("a");
		b.setUri("urn:b");
		// First test they are the same
		ConceptComparator comparator = new ConceptComparator();
		assertEquals(-1, comparator.compare(a, b));
		assertEquals(1, comparator.compare(b, a));
	}
	
	@Test
	public void testSortLabelFirst(){
		List<Concept> list = new ArrayList();
		a.setPreferredLabel("a");
		a.setUri("urn:123");
		b.setPreferredLabel("b");
		b.setUri("urn:123");
		list.add(b);
		list.add(a);
		Collections.sort(list, new ConceptComparator());
		assertEquals("a", list.get(0).getPreferredLabel());
		assertEquals("b", list.get(1).getPreferredLabel());
	}
	
	@Test
	public void testSortLabelEquals(){
		List<Concept> list = new ArrayList();
		a.setPreferredLabel("a");
		a.setUri("urn:a");
		b.setPreferredLabel("a");
		b.setUri("urn:b");
		list.add(b);
		list.add(a);
		Collections.sort(list, new ConceptComparator());
		assertEquals("urn:a", list.get(0).getUri());
		assertEquals("urn:b", list.get(1).getUri());
	}
	
	@Test
	public void testSortLabelCaseEquals(){
		List<Concept> list = new ArrayList();
		a.setPreferredLabel("B");
		a.setUri("urn:b");
		b.setPreferredLabel("a");
		b.setUri("urn:a");
		list.add(b);
		list.add(a);
		Collections.sort(list, new ConceptComparator());
		assertEquals("urn:a", list.get(0).getUri());
		assertEquals("urn:b", list.get(1).getUri());
	}
}
