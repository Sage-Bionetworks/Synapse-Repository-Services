package org.sagebionetworks.repo.model.query.entity;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.query.jdo.NodeField;

import com.google.common.collect.Lists;

public class SelectListTest {
	
	List<String> nodeToEntitySql;
	
	@Before
	public void before(){
		nodeToEntitySql = new LinkedList<String>();
		for(NodeToEntity nte: NodeToEntity.values()){
			SelectColumn sc = new SelectColumn(nte);
			nodeToEntitySql.add(sc.toSql());
		}
	}

	@Test
	public void testSelectStarEmpty(){
		SelectList list = new SelectList(new LinkedList<String>());
		String sql = list.toSql();
		assertTrue(sql.startsWith("SELECT "));
		// should contain the SQL for each type
		for(String typeSql: nodeToEntitySql){
			assertTrue(sql.contains(typeSql));
		}
		assertTrue(list.isSelectStar());
		assertFalse(list.includesAnnotations());
	}
	
	@Test
	public void testSelectStarNull(){
		SelectList list = new SelectList(null);
		String sql = list.toSql();
		assertTrue(sql.startsWith("SELECT "));
		// should contain the SQL for each type
		for(String typeSql: nodeToEntitySql){
			assertTrue(sql.contains(typeSql));
		}
		assertTrue(list.isSelectStar());
		assertFalse(list.includesAnnotations());
	}
	
	@Test
	public void testSelectNodeFieldsOnly(){
		List<String> input = Lists.newArrayList(
				NodeField.ID.getFieldName()
				, NodeField.E_TAG.getFieldName()
				);
		SelectList list = new SelectList(input);
		assertEquals("SELECT E.ID AS 'id', E.ETAG AS 'eTag'", list.toSql());
		assertFalse(list.isSelectStar());
		assertFalse(list.includesAnnotations());
	}

	@Test
	public void testSelectAnnotationsOnly(){
		List<String> input = Lists.newArrayList(
				"foo"
				, "bar"
				);
		SelectList list = new SelectList(input);
		assertEquals("SELECT NULL AS 'foo', NULL AS 'bar'", list.toSql());
		assertFalse(list.isSelectStar());
		assertTrue(list.includesAnnotations());
	}
	
	@Test
	public void testSelectNodeFieldsAndAnnotations(){
		List<String> input = Lists.newArrayList(
				NodeField.ID.getFieldName()
				, "foo"
				);
		SelectList list = new SelectList(input);
		assertEquals("SELECT E.ID AS 'id', NULL AS 'foo'", list.toSql());
		assertFalse(list.isSelectStar());
		assertTrue(list.includesAnnotations());
	}
}
