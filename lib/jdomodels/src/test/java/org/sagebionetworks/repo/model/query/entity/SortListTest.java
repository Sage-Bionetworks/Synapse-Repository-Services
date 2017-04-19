package org.sagebionetworks.repo.model.query.entity;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.sagebionetworks.repo.model.query.jdo.NodeField;

public class SortListTest {

	@Test
	public void testSortNull(){
		int index = 0;
		boolean isAscending = true;
		SortList sort = new SortList(index, null, isAscending);
		assertEquals("", sort.toSql());
	}
	
	
	@Test
	public void testSortAnnotation(){
		int index = 0;
		boolean isAscending = true;
		SortList sort = new SortList(index, "foo", isAscending);
		assertEquals(" ORDER BY A0.ANNO_VALUE ASC", sort.toSql());
	}

	@Test
	public void testSortNodeField(){
		int index = 0;
		boolean isAscending = false;
		SortList sort = new SortList(index, NodeField.BENEFACTOR_ID.getFieldName(), isAscending);
		assertEquals(" ORDER BY E.BENEFACTOR_ID DESC", sort.toSql());
	}
}
