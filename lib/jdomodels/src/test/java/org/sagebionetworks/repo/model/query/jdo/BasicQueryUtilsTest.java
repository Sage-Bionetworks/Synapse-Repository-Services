package org.sagebionetworks.repo.model.query.jdo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.Comparator;
import org.sagebionetworks.repo.model.query.CompoundId;
import org.sagebionetworks.repo.model.query.Expression;

import com.google.common.collect.Lists;

public class BasicQueryUtilsTest {

	BasicQuery query;
	Expression expression;

	@Before
	public void before() {

		expression = new Expression(new CompoundId(null,
				NodeField.PARENT_ID.getFieldName()), Comparator.EQUALS,
				"syn123");

		query = new BasicQuery();
		query.setFrom(BasicQueryUtils.ENTITY);
		query.setAscending(true);
		query.setSelect(Lists.newArrayList(NodeField.ID.getFieldName(),
				NodeField.NAME.getFieldName()));
		query.setFilters(Lists.newArrayList(expression));
	}

	@Test
	public void testConvertFromToExpressionsEntity() {
		// call under test
		BasicQuery result = BasicQueryUtils.convertFromToExpressions(query);
		assertNotNull(result);
		// the result should be a new copy
		assertFalse(result == query);
		assertEquals(Lists.newArrayList(expression,
				BasicQueryUtils.EXP_BENEFACTOR_NOT_EQUAL_TRASH),
				result.getFilters());
		assertEquals(null, result.getFrom());
	}
	
	@Test
	public void testConvertFromToExpressionsVersionalbe() {
		// from versionable
		query.setFrom(BasicQueryUtils.VERSIONABLE);
		// call under test
		BasicQuery result = BasicQueryUtils.convertFromToExpressions(query);
		assertNotNull(result);
		// the result should be a new copy
		assertFalse(result == query);
		assertEquals(Lists.newArrayList(
				expression,
				BasicQueryUtils.EXP_VERSIONABLE_TYPES,
				BasicQueryUtils.EXP_BENEFACTOR_NOT_EQUAL_TRASH
		),
				result.getFilters());
		assertEquals(null, result.getFrom());
	}
	
	@Test
	public void testConvertFromToExpressionsFolder() {
		Expression folderExpression = new Expression(new CompoundId(null,
				NodeField.NODE_TYPE.getFieldName()), Comparator.EQUALS,
				EntityType.folder.name());
		
		// from folder
		query.setFrom(EntityType.folder.name());
		// call under test
		BasicQuery result = BasicQueryUtils.convertFromToExpressions(query);
		assertNotNull(result);
		// the result should be a new copy
		assertFalse(result == query);
		assertEquals(Lists.newArrayList(
				expression,
				folderExpression,
				BasicQueryUtils.EXP_BENEFACTOR_NOT_EQUAL_TRASH
		),
				result.getFilters());
		assertEquals(null, result.getFrom());
	}
	
	@Test
	public void testCopyConstructor(){
		BasicQuery copy = new BasicQuery(query);
		assertEquals(query, copy);
	}
}
