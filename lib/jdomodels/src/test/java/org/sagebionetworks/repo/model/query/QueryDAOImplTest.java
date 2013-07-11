package org.sagebionetworks.repo.model.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.sagebionetworks.evaluation.dao.AnnotationsDAO;
import org.sagebionetworks.evaluation.dbo.DBOConstants;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.repo.model.annotation.DoubleAnnotation;
import org.sagebionetworks.repo.model.annotation.LongAnnotation;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class QueryDAOImplTest {
	
	private static final int NUM_SUBMISSIONS = 30;
	
	@Autowired
	QueryDAO queryDAO;
	@Autowired
	AnnotationsDAO annotationsDAO;
	
	private static final String EVAL_ID1 = "42";
	private static final String EVAL_ID2 = "99";
    private Set<String> submissionIds;
    private AccessControlListDAO mockAclDAO;
    private UserInfo mockUserInfo;
    private Map<String, Object> annoMap;
	
	@Before
	public void setUp() throws DatastoreException, JSONObjectAdapterException {
		// create Annotations
		Annotations annos;
		submissionIds = new HashSet<String>();
		annoMap = new HashMap<String, Object>();
		
		for (int i = 0; i < NUM_SUBMISSIONS; i++) {
	    	annos = TestUtils.createDummyAnnotations(i);
	    	annos.setOwnerParentId(EVAL_ID1);
	    	annotationsDAO.replaceAnnotations(annos);
	    	submissionIds.add(annos.getOwnerId());
	    	dumpAnnosToMap(annoMap, annos);
	    	
	    	annos = TestUtils.createDummyAnnotations(i + NUM_SUBMISSIONS);
	    	annos.setOwnerParentId(EVAL_ID2);
	    	annotationsDAO.replaceAnnotations(annos);
	    	submissionIds.add(annos.getOwnerId());
	    	dumpAnnosToMap(annoMap, annos);
		}
		
		mockUserInfo = mock(UserInfo.class);
		mockAclDAO = mock(AccessControlListDAO.class);
		when(mockAclDAO.canAccess(Matchers.<Collection<UserGroup>>any(), eq(EVAL_ID1), eq(ACCESS_TYPE.READ))).thenReturn(true);
		when(mockAclDAO.canAccess(Matchers.<Collection<UserGroup>>any(), eq(EVAL_ID1), eq(ACCESS_TYPE.READ_PRIVATE_ANNOTATIONS))).thenReturn(true);
		when(mockAclDAO.canAccess(Matchers.<Collection<UserGroup>>any(), eq(EVAL_ID2), eq(ACCESS_TYPE.READ))).thenReturn(true);
		when(mockAclDAO.canAccess(Matchers.<Collection<UserGroup>>any(), eq(EVAL_ID2), eq(ACCESS_TYPE.READ_PRIVATE_ANNOTATIONS))).thenReturn(false);
		queryDAO.setAclDAO(mockAclDAO);
	}
	
	@After
	public void tearDown() {
		for (String subId : submissionIds) {
			annotationsDAO.deleteAnnotationsByOwnerId(Long.parseLong(subId));
		}
	}

	@Test
	public void testBasicQuery() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
		// build the basic SELECT * query
		BasicQuery query = new BasicQuery();
		query.setFrom("submission");
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);
		CompoundId compoundId = new CompoundId(null, DBOConstants.PARAM_ANNOTATION_OWNER_PARENT_ID);
		Expression exp = new Expression(compoundId, Comparator.EQUALS, EVAL_ID1);
		List<Expression> filters = new ArrayList<Expression>();
		filters.add(exp);
		query.setFilters(filters);
		
		// perform the query
		QueryTableResults results = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(NUM_SUBMISSIONS, results.getTotalNumberOfResults().longValue());
		assertEquals(NUM_SUBMISSIONS, results.getRows().size());
		
		// examine the results
		List<Row> rows = results.getRows();
		List<String> headers = new ArrayList<String>(results.getHeaders());
		for (int i = 0; i < rows.size(); i++) {
			Row row = rows.get(i);
			List<String> values = row.getValues();
			// we expect 1 column for object ID + 3 columns for unique attributes
			assertEquals(4, values.size());
			assertFalse(values.contains(null));
			// validate all values
			String id = "" + i;
			assertEquals(id, values.get(0));
			for (int j = 1; j < headers.size(); j++) {
				String expected = annoMap.get(id + headers.get(j)).toString();
				String actual = values.get(j);
				assertEquals(expected, actual);
			}
		}
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testQueryMissingId() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
		BasicQuery query = new BasicQuery();
		query.setFrom("submission");
		queryDAO.executeQuery(query, mockUserInfo);
	}
	
	@Test
	public void testQueryPaging() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
		// build the basic SELECT * query
		// limit 10, offset 10
		int limit = 10;
		int offset = 10;
		BasicQuery query = new BasicQuery();
		query.setFrom("submission");
		query.setLimit(limit);
		query.setOffset(offset);
		CompoundId compoundId = new CompoundId(null, DBOConstants.PARAM_ANNOTATION_OWNER_PARENT_ID);
		Expression exp = new Expression(compoundId, Comparator.EQUALS, EVAL_ID1);
		List<Expression> filters = new ArrayList<Expression>();
		filters.add(exp);
		query.setFilters(filters);
				
		// perform the query
		// we expect 10 results (IDs 10 - 19)
		QueryTableResults results = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(NUM_SUBMISSIONS, results.getTotalNumberOfResults().longValue());
		assertEquals(10, results.getRows().size());
		
		// examine the results
		List<Row> rows = results.getRows();
		List<String> headers = new ArrayList<String>(results.getHeaders());
		for (int i = 0; i < rows.size(); i++) {
			Row row = rows.get(i);
			List<String> values = row.getValues();
			// we expect 1 column for object ID + 3 columns for unique attributes
			assertEquals(4, values.size());
			assertFalse(values.contains(null));
			// validate all values
			String id = "" + (offset + i);
			assertEquals(id, values.get(0));
			for (int j = 1; j < headers.size(); j++) {
				String expected = annoMap.get(id + headers.get(j)).toString();
				String actual = values.get(j);
				assertEquals(expected, actual);
			}
		}
	}
	
	@Test
	public void testBasicQueryNoPrivate() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
		// build the basic SELECT * query
		// this time, use EVAL_ID2, on which we do not have READ_PRIVATE_ANNOTATIONS permission
		BasicQuery query = new BasicQuery();
		query.setFrom("submission");
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);
		CompoundId compoundId = new CompoundId(null, DBOConstants.PARAM_ANNOTATION_OWNER_PARENT_ID);
		Expression exp = new Expression(compoundId, Comparator.EQUALS, EVAL_ID2);
		List<Expression> filters = new ArrayList<Expression>();
		filters.add(exp);
		query.setFilters(filters);		
		
		// perform the query
		QueryTableResults results = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(NUM_SUBMISSIONS, results.getTotalNumberOfResults().longValue());
		assertEquals(NUM_SUBMISSIONS, results.getRows().size());
		
		// examine the results
		List<Row> rows = results.getRows();
		List<String> headers = new ArrayList<String>(results.getHeaders());
		for (int i = 0; i < rows.size(); i++) {
			Row row = rows.get(i);
			List<String> values = row.getValues();
			// we expect 1 column for object ID + 2 columns for unique attributes
			assertEquals(3, values.size());
			assertFalse(values.contains(null));
			// validate all values
			String id = "" + (NUM_SUBMISSIONS + i);
			assertEquals(id, values.get(0));
			for (int j = 1; j < headers.size(); j++) {
				String expected = annoMap.get(id + headers.get(j)).toString();
				String actual = values.get(j);
				assertEquals(expected, actual);
			}
		}
	}
	
	@Test
	public void testQueryFilterByString() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
		// SELECT * WHERE "string anno"="foo 3"
		BasicQuery query = new BasicQuery();
		query.setFrom("submission");
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);		
		Expression exp = new Expression(new CompoundId(null, "string anno"), Comparator.EQUALS, "foo 3");		
		CompoundId compoundId = new CompoundId(null, DBOConstants.PARAM_ANNOTATION_OWNER_PARENT_ID);
		Expression exp2 = new Expression(compoundId, Comparator.EQUALS, EVAL_ID1);
		List<Expression> filters = new ArrayList<Expression>();
		filters.add(exp);
		filters.add(exp2);
		query.setFilters(filters);		
		
		// perform the query
		QueryTableResults results = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(1, results.getTotalNumberOfResults().longValue());
		assertEquals(1, results.getRows().size());
		
		// examine the results
		List<Row> rows = results.getRows();
		List<String> headers = new ArrayList<String>(results.getHeaders());
		for (int i = 0; i < rows.size(); i++) {
			Row row = rows.get(i);
			List<String> values = row.getValues();
			// we expect 1 column for object ID + 3 columns for unique attributes
			assertEquals(4, values.size());
			assertFalse(values.contains(null));
			// validate all values
			String id = "3";
			assertEquals(id, values.get(0));
			for (int j = 1; j < headers.size(); j++) {
				String expected = annoMap.get(id + headers.get(j)).toString();
				String actual = values.get(j);
				assertEquals(expected, actual);
			}
		}
	}
	
	@Test
	public void testQueryFilterByLong() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
		// SELECT * WHERE "long anno"="40"
		BasicQuery query = new BasicQuery();
		query.setFrom("submission");
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);		
		Expression exp = new Expression(new CompoundId(null, "long anno"), Comparator.EQUALS, 40);		
		CompoundId compoundId = new CompoundId(null, DBOConstants.PARAM_ANNOTATION_OWNER_PARENT_ID);
		Expression exp2 = new Expression(compoundId, Comparator.EQUALS, EVAL_ID1);
		List<Expression> filters = new ArrayList<Expression>();
		filters.add(exp);
		filters.add(exp2);
		query.setFilters(filters);		
		
		// perform the query
		QueryTableResults results = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(1, results.getTotalNumberOfResults().longValue());
		assertEquals(1, results.getRows().size());
		
		// examine the results
		List<Row> rows = results.getRows();
		List<String> headers = new ArrayList<String>(results.getHeaders());
		for (int i = 0; i < rows.size(); i++) {
			Row row = rows.get(i);
			List<String> values = row.getValues();
			// we expect 1 column for object ID + 3 columns for unique attributes
			assertEquals(4, values.size());
			assertFalse(values.contains(null));
			// validate all values
			String id = "4";
			assertEquals(id, values.get(0));
			for (int j = 1; j < headers.size(); j++) {
				String expected = annoMap.get(id + headers.get(j)).toString();
				String actual = values.get(j);
				assertEquals(expected, actual);
			}
		}
	}
	
	@Test
	public void testQueryfilterByDouble() throws DatastoreException, NotFoundException, JSONObjectAdapterException {		
		// SELECT * WHERE "double anno"="5.5"
		BasicQuery query = new BasicQuery();
		query.setFrom("submission");
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);		
		Expression exp = new Expression(new CompoundId(null, "double anno"), Comparator.EQUALS, 5.5);		
		CompoundId compoundId = new CompoundId(null, DBOConstants.PARAM_ANNOTATION_OWNER_PARENT_ID);
		Expression exp2 = new Expression(compoundId, Comparator.EQUALS, EVAL_ID1);
		List<Expression> filters = new ArrayList<Expression>();
		filters.add(exp);
		filters.add(exp2);
		query.setFilters(filters);		
		
		// perform the query
		QueryTableResults results = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(1, results.getTotalNumberOfResults().longValue());
		assertEquals(1, results.getRows().size());
		
		// examine the results
		List<Row> rows = results.getRows();
		List<String> headers = new ArrayList<String>(results.getHeaders());
		for (int i = 0; i < rows.size(); i++) {
			Row row = rows.get(i);
			List<String> values = row.getValues();
			// we expect 1 column for object ID + 3 columns for unique attributes
			assertEquals(4, values.size());
			assertFalse(values.contains(null));
			// validate all values
			String id = "5";
			assertEquals(id, values.get(0));
			for (int j = 1; j < headers.size(); j++) {
				String expected = annoMap.get(id + headers.get(j)).toString();
				String actual = values.get(j);
				assertEquals(expected, actual);
			}
		}
	}
	
	@Test
	public void testQueryNoResults() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
		// SELECT * WHERE "other anno"="does not exist"
		BasicQuery query = new BasicQuery();
		query.setFrom("submission");
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);		
		Expression exp = new Expression(new CompoundId(null, "other anno"), Comparator.EQUALS, "does not exist");		
		CompoundId compoundId = new CompoundId(null, DBOConstants.PARAM_ANNOTATION_OWNER_PARENT_ID);
		Expression exp2 = new Expression(compoundId, Comparator.EQUALS, EVAL_ID1);
		List<Expression> filters = new ArrayList<Expression>();
		filters.add(exp);
		filters.add(exp2);
		query.setFilters(filters);		
		
		// perform the query
		QueryTableResults results = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(0, results.getTotalNumberOfResults().longValue());
		assertEquals(0, results.getRows().size());
	}
	
	@Test
	public void testQueryWithProjection() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
		// SELECT "string anno"
		BasicQuery query = new BasicQuery();
		query.setFrom("submission");
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);
		List<String> select = new ArrayList<String>();
		select.add("string anno");
		query.setSelect(select);
		CompoundId compoundId = new CompoundId(null, DBOConstants.PARAM_ANNOTATION_OWNER_PARENT_ID);
		Expression exp = new Expression(compoundId, Comparator.EQUALS, EVAL_ID1);
		List<Expression> filters = new ArrayList<Expression>();
		filters.add(exp);
		query.setFilters(filters);
		
		// perform the query
		QueryTableResults results = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(NUM_SUBMISSIONS, results.getTotalNumberOfResults().longValue());
		assertEquals(NUM_SUBMISSIONS, results.getRows().size());
		
		// examine the results
		List<Row> rows = results.getRows();
		List<String> headers = new ArrayList<String>(results.getHeaders());
		for (int i = 0; i < rows.size(); i++) {
			Row row = rows.get(i);
			List<String> values = row.getValues();
			// we expect 1 column for object ID + 1 columns for the selected attribute
			assertEquals(2, values.size());
			assertFalse(values.contains(null));
			// validate all values
			String id = "" + i;
			assertEquals(id, values.get(0));
			for (int j = 1; j < headers.size(); j++) {
				String expected = annoMap.get(id + headers.get(j)).toString();
				String actual = values.get(j);
				assertEquals(expected, actual);
			}
		}
	}
	
	@Test
	public void testQuerySortAscending() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
		// build the basic SELECT * query
		BasicQuery query = new BasicQuery();
		query.setFrom("submission");
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);
		query.setSort("string anno");
		query.setAscending(true);
		List<String> select = new ArrayList<String>();
		select.add("string anno");
		query.setSelect(select);
		CompoundId compoundId = new CompoundId(null, DBOConstants.PARAM_ANNOTATION_OWNER_PARENT_ID);
		Expression exp = new Expression(compoundId, Comparator.EQUALS, EVAL_ID1);
		List<Expression> filters = new ArrayList<Expression>();
		filters.add(exp);
		query.setFilters(filters);
		
		// perform the query
		QueryTableResults results = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(NUM_SUBMISSIONS, results.getTotalNumberOfResults().longValue());
		assertEquals(NUM_SUBMISSIONS, results.getRows().size());
		
		// examine the results
		List<Row> rows = results.getRows();
		String previous = null;
		for (int i = 0; i < rows.size(); i++) {
			Row row = rows.get(i);
			List<String> values = row.getValues();
			// we expect 1 column for object ID + 1 columns for the selected attribute
			assertEquals(2, values.size());
			assertFalse(values.contains(null));
			// validate ordering
			String current = values.get(1);
			if (previous != null) {
				assertTrue(current.compareTo(previous) > 0);
			}
			previous = current;
		}
	}
	
	@Test
	public void testQuerySortDescending() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
		// build the basic SELECT * query
		BasicQuery query = new BasicQuery();
		query.setFrom("submission");
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);
		query.setSort("string anno");
		query.setAscending(false);
		List<String> select = new ArrayList<String>();
		select.add("string anno");
		query.setSelect(select);
		CompoundId compoundId = new CompoundId(null, DBOConstants.PARAM_ANNOTATION_OWNER_PARENT_ID);
		Expression exp = new Expression(compoundId, Comparator.EQUALS, EVAL_ID1);
		List<Expression> filters = new ArrayList<Expression>();
		filters.add(exp);
		query.setFilters(filters);
		
		// perform the query
		QueryTableResults results = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(NUM_SUBMISSIONS, results.getTotalNumberOfResults().longValue());
		assertEquals(NUM_SUBMISSIONS, results.getRows().size());
		
		// examine the results
		List<Row> rows = results.getRows();
		String previous = null;
		for (int i = 0; i < rows.size(); i++) {
			Row row = rows.get(i);
			List<String> values = row.getValues();
			// we expect 1 column for object ID + 1 columns for the selected attribute
			assertEquals(2, values.size());
			assertFalse(values.contains(null));
			// validate ordering
			String current = values.get(1);
			if (previous != null) {
				assertTrue(current.compareTo(previous) < 0);
			}
			previous = current;
		}
	}
	
	// Flatten an Annotations object to a map. The key is given by [Owner ID] + [attribute name],
	// and the value is simply the [attribute value]
	private static void dumpAnnosToMap(Map<String, Object> annoMap, Annotations annos) {
		List<StringAnnotation> stringAnnos = annos.getStringAnnos();
		for (StringAnnotation sa : stringAnnos) {
			annoMap.put(annos.getOwnerId() + sa.getKey(), sa.getValue());
		}
		List<LongAnnotation> longAnnos = annos.getLongAnnos();
		for (LongAnnotation la : longAnnos) {
			annoMap.put(annos.getOwnerId() + la.getKey(), la.getValue());
		}
		List<DoubleAnnotation> doubleAnnos = annos.getDoubleAnnos();
		for (DoubleAnnotation da : doubleAnnos) {
			annoMap.put(annos.getOwnerId() + da.getKey(), da.getValue());
		}
	}

}
