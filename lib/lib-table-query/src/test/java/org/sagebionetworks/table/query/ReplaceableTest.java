package org.sagebionetworks.table.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.sagebionetworks.table.query.model.ColumnReference;
import org.sagebionetworks.table.query.model.Element;
import org.sagebionetworks.table.query.model.QuerySpecification;
import org.sagebionetworks.table.query.model.Replaceable;
import org.sagebionetworks.table.query.model.TableReference;
import org.sagebionetworks.table.query.model.WhereClause;

public class ReplaceableTest {

	@Test
	public void testReplaceElement() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 group by bar, a").querySpecification();
		TableReference old = model.getFirstElementOfType(TableReference.class);
		Element oldChild = old.getChild();
		TableReference replacement = new TableQueryParser("T123").tableReference();
		// call under test
		old.replaceElement(replacement);
		assertEquals("SELECT * FROM T123 GROUP BY bar, a", model.toSql());
		assertNotNull(replacement.getParent());
		assertNull(old.getParent());
		assertNull(oldChild.getParent());
	}
	
	@Test
	public void testReplaceElementWithNullReplacement() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 group by bar, a").querySpecification();
		TableReference old = model.getFirstElementOfType(TableReference.class);
		TableReference replacement = null;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			old.replaceElement(replacement);
		}).getMessage();
		assertEquals("Replacement cannot be null", message);
	}
	
	@Test
	public void testReplaceElementWithNullParent() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 group by bar, a").querySpecification();
		TableReference old = model.getFirstElementOfType(TableReference.class);
		old.recursiveClearParent();
		TableReference replacement = new TableQueryParser("T123").tableReference();
		String message = assertThrows(IllegalStateException.class, ()->{
			// call under test
			old.replaceElement(replacement);
		}).getMessage();
		assertEquals("Cannot replace Element since its parent is null", message);
	}
	
	@Test
	public void testPrepareToReplace() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 where foo > 1").querySpecification();
		WhereClause old = model.getTableExpression().getWhereClause();
		assertNotNull(old.getParent());
		ColumnReference oldRef = old.getFirstElementOfType(ColumnReference.class);
		assertNotNull(oldRef.getParent());
		
		WhereClause newWhere = new TableQueryParser("where bar > 2").whereClause();
		assertNull(newWhere.getParent());
		ColumnReference newRef = newWhere.getFirstElementOfType(ColumnReference.class);
		assertNull(newRef.getParent());
		
		// call under test
		WhereClause result =  Replaceable.prepareToReplace(old, newWhere, model.getTableExpression());
		
		assertEquals(result, newWhere);
		assertEquals(model.getTableExpression(), newWhere.getParent());
		assertNotNull(newRef.getParent());
		assertNull(old.getParent());
		assertNull(oldRef.getParent());
	}
	
	@Test
	public void testPrepareToReplaceWithOldNull() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123").querySpecification();
		WhereClause old = model.getTableExpression().getWhereClause();
		assertNull(old);
		
		WhereClause newWhere = new TableQueryParser("where bar > 2").whereClause();
		assertNull(newWhere.getParent());
		ColumnReference newRef = newWhere.getFirstElementOfType(ColumnReference.class);
		assertNull(newRef.getParent());
		
		// call under test
		WhereClause result =  Replaceable.prepareToReplace(old, newWhere, model.getTableExpression());
		
		assertEquals(result, newWhere);
		assertEquals(model.getTableExpression(), newWhere.getParent());
		assertNotNull(newRef.getParent());
	}
	
	@Test
	public void testPrepareToReplaceWithNewNull() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 where foo > 1").querySpecification();
		WhereClause old = model.getTableExpression().getWhereClause();
		assertNotNull(old.getParent());
		ColumnReference oldRef = old.getFirstElementOfType(ColumnReference.class);
		assertNotNull(oldRef.getParent());
		
		WhereClause newWhere = null;
		
		// call under test
		WhereClause result =  Replaceable.prepareToReplace(old, newWhere, model.getTableExpression());
		
		assertNull(result);
		assertNull(old.getParent());
		assertNull(oldRef.getParent());
	}
	
	@Test
	public void testPrepareToReplaceWithOldEqualsNew() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 where foo > 1").querySpecification();
		WhereClause old = model.getTableExpression().getWhereClause();
		assertNotNull(old.getParent());
		ColumnReference oldRef = old.getFirstElementOfType(ColumnReference.class);
		assertNotNull(oldRef.getParent());
		
		
		// call under test
		WhereClause result =  Replaceable.prepareToReplace(old, old, model.getTableExpression());
		
		assertEquals(result, old);
		assertEquals(model.getTableExpression(), old.getParent());
		assertNotNull(oldRef.getParent());
	}
	
	@Test
	public void testPrepareToReplaceWithNullParent() throws ParseException {
		QuerySpecification model = new TableQueryParser("select * from syn123 where foo > 1").querySpecification();
		WhereClause old = model.getTableExpression().getWhereClause();
		assertNotNull(old.getParent());
		ColumnReference oldRef = old.getFirstElementOfType(ColumnReference.class);
		assertNotNull(oldRef.getParent());
		
		WhereClause newWhere = new TableQueryParser("where bar > 2").whereClause();
		assertNull(newWhere.getParent());
		ColumnReference newRef = newWhere.getFirstElementOfType(ColumnReference.class);
		assertNull(newRef.getParent());
		
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			Replaceable.prepareToReplace(old, newWhere, null);
		}).getMessage();
		assertEquals("parent is required.", message);
	}
}