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
public class ConceptSummaryComparatorTest {
	
	ConceptSummary a;
	ConceptSummary b;
	
	@Before
	public void before(){
		a = new ConceptSummary();
		b = new ConceptSummary();
	}
	
	
	@Test
	public void testCompareEquals(){
		a.setPreferredLabel("abc");
		a.setUri("urn:123");
		b.setPreferredLabel("abc");
		b.setUri("urn:123");
		// First test they are the same
		ConceptSummaryComparator comparator = new ConceptSummaryComparator();
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
		ConceptSummaryComparator comparator = new ConceptSummaryComparator();
		assertEquals(0, comparator.compare(a, b));
	}
	
	@Test (expected=NullPointerException.class)
	public void testCompareEqualsNullUriA(){
		a.setPreferredLabel("abc");
		a.setUri(null);
		b.setPreferredLabel("abc");
		b.setUri("urn:123");
		// First test they are the same
		ConceptSummaryComparator comparator = new ConceptSummaryComparator();
		assertEquals(0, comparator.compare(a, b));
	}
	
	@Test (expected=NullPointerException.class)
	public void testCompareEqualsNullLabelB(){
		a.setPreferredLabel("abc");
		a.setUri("urn:123");
		b.setPreferredLabel(null);
		b.setUri("urn:123");
		// First test they are the same
		ConceptSummaryComparator comparator = new ConceptSummaryComparator();
		assertEquals(0, comparator.compare(a, b));
	}
	
	@Test (expected=NullPointerException.class)
	public void testCompareEqualsNullUriB(){
		a.setPreferredLabel("abc");
		a.setUri("urn:123");
		b.setPreferredLabel("abc");
		b.setUri(null);
		// First test they are the same
		ConceptSummaryComparator comparator = new ConceptSummaryComparator();
		assertEquals(0, comparator.compare(a, b));
	}
	
	@Test
	public void testCompareEqualsNull(){
		// First test they are the same
		ConceptSummaryComparator comparator = new ConceptSummaryComparator();
		assertEquals(0, comparator.compare(null, null));
	}
	
	@Test
	public void testLabelFirst(){
		a.setPreferredLabel("a");
		a.setUri("urn:123");
		b.setPreferredLabel("b");
		b.setUri("urn:123");
		// First test they are the same
		ConceptSummaryComparator comparator = new ConceptSummaryComparator();
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
		ConceptSummaryComparator comparator = new ConceptSummaryComparator();
		assertEquals(-1, comparator.compare(a, b));
		assertEquals(1, comparator.compare(b, a));
	}
	
	@Test
	public void testSortLabelFirst(){
		List<ConceptSummary> list = new ArrayList();
		a.setPreferredLabel("a");
		a.setUri("urn:123");
		b.setPreferredLabel("b");
		b.setUri("urn:123");
		list.add(b);
		list.add(a);
		Collections.sort(list, new ConceptSummaryComparator());
		assertEquals("a", list.get(0).getPreferredLabel());
		assertEquals("b", list.get(1).getPreferredLabel());
	}
	
	@Test
	public void testSortLabelEquals(){
		List<ConceptSummary> list = new ArrayList();
		a.setPreferredLabel("a");
		a.setUri("urn:a");
		b.setPreferredLabel("a");
		b.setUri("urn:b");
		list.add(b);
		list.add(a);
		Collections.sort(list, new ConceptSummaryComparator());
		assertEquals("urn:a", list.get(0).getUri());
		assertEquals("urn:b", list.get(1).getUri());
	}
}
