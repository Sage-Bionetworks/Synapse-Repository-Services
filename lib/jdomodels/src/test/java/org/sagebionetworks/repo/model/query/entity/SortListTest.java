package org.sagebionetworks.repo.model.query.entity;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.query.jdo.NodeField;

public class SortListTest {
	
	IndexProvider indexProvider;
	
	@Before
	public void before(){
		indexProvider = new IndexProvider();
	}

	@Test
	public void testSortNull(){
		boolean isAscending = true;
		SortList sort = new SortList(null, isAscending, indexProvider);
		assertEquals("", sort.toSql());
	}
	
	
	@Test
	public void testSortAnnotation(){
		boolean isAscending = true;
		SortList sort = new SortList("foo", isAscending, indexProvider);
		assertEquals(" ORDER BY A0.ANNO_VALUE ASC", sort.toSql());
	}

	@Test
	public void testSortNodeField(){
		boolean isAscending = false;
		SortList sort = new SortList(NodeField.BENEFACTOR_ID.getFieldName(), isAscending, indexProvider);
		assertEquals(" ORDER BY E.BENEFACTOR_ID DESC", sort.toSql());
	}
}
