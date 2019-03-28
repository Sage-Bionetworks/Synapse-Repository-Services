package org.sagebionetworks.repo.model.query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.evaluation.dbo.DBOConstants;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.Annotations;
import org.sagebionetworks.repo.model.annotation.DoubleAnnotation;
import org.sagebionetworks.repo.model.annotation.LongAnnotation;
import org.sagebionetworks.repo.model.annotation.StringAnnotation;
import org.sagebionetworks.repo.model.dbo.dao.SubmissionStatusAnnotationsAsyncManagerImpl;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.evaluation.AnnotationsDAO;
import org.sagebionetworks.repo.model.evaluation.EvaluationSubmissionsDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class QueryDAOImplTest {
	
	private static final int NUM_SUBMISSIONS = 30;
	
	@Autowired
	QueryDAO queryDAO;
	@Autowired
	private AnnotationsDAO annotationsDAO;
	@Autowired
	private EvaluationSubmissionsDAO evaluationSubmissionsDAO;

	SubmissionStatusAnnotationsAsyncManagerImpl ssAnnoAsyncManager;
	
	private static final String EVAL_ID1 = "42";
	private static final String EVAL_ID2 = "99";
    private Set<String> submissionIds;
    private AccessControlListDAO mockAclDAO;
    private UserInfo mockUserInfo;
    private Map<String, Object> annoMap;
    
	
	@Before
	public void setUp() throws DatastoreException, JSONObjectAdapterException, NotFoundException {
		ssAnnoAsyncManager = new SubmissionStatusAnnotationsAsyncManagerImpl(annotationsDAO, evaluationSubmissionsDAO);
		// create Annotations
		Annotations annos;
		submissionIds = new HashSet<String>();
		annoMap = new HashMap<String, Object>();
		
		List<Annotations> eval1List = new ArrayList<Annotations>();
		List<Annotations> eval2List = new ArrayList<Annotations>();
		for (int i = 0; i < NUM_SUBMISSIONS; i++) {
	    	annos = TestUtils.createDummyAnnotations(i);
	    	annos.getLongAnnos().add(createScopeAnno(Long.parseLong(EVAL_ID1)));
	    	annos.setScopeId(EVAL_ID1);
	    	annos.setVersion(0L);
	    	submissionIds.add(annos.getObjectId());
	    	dumpAnnosToMap(annoMap, annos);
	    	eval1List.add(annos);
	    	
	    	annos = TestUtils.createDummyAnnotations(i + NUM_SUBMISSIONS);
	    	annos.getLongAnnos().add(createScopeAnno(Long.parseLong(EVAL_ID2)));
	    	annos.setScopeId(EVAL_ID2);
	    	annos.setVersion(0L);
	    	submissionIds.add(annos.getObjectId());
	    	dumpAnnosToMap(annoMap, annos);
	    	eval2List.add(annos);
		}
    	annotationsDAO.replaceAnnotations(eval1List);
    	annotationsDAO.replaceAnnotations(eval2List);
		
		// set up mocks
		mockUserInfo = mock(UserInfo.class);
		mockAclDAO = mock(AccessControlListDAO.class);
		when(mockAclDAO.canAccess(any(), eq(EVAL_ID1), eq(ObjectType.EVALUATION), eq(ACCESS_TYPE.READ))).thenReturn(true);
		when(mockAclDAO.canAccess(any(), eq(EVAL_ID1), eq(ObjectType.EVALUATION), eq(ACCESS_TYPE.READ_PRIVATE_SUBMISSION))).thenReturn(true);
		when(mockAclDAO.canAccess(any(), eq(EVAL_ID2), eq(ObjectType.EVALUATION), eq(ACCESS_TYPE.READ))).thenReturn(true);
		when(mockAclDAO.canAccess(any(), eq(EVAL_ID2), eq(ObjectType.EVALUATION), eq(ACCESS_TYPE.READ_PRIVATE_SUBMISSION))).thenReturn(false);
		queryDAO.setAclDAO(mockAclDAO);
	}
	
	private static LongAnnotation createScopeAnno(long scopeId) {
    	LongAnnotation scopeAnno = new LongAnnotation();
    	scopeAnno.setIsPrivate(false);
    	scopeAnno.setKey("scopeId");
    	scopeAnno.setValue(scopeId);
		return scopeAnno;
	}
	
	@After
	public void tearDown() {
		annotationsDAO.deleteAnnotationsByScope(Long.parseLong(EVAL_ID1));
		annotationsDAO.deleteAnnotationsByScope(Long.parseLong(EVAL_ID2));
	}

	@Test
	public void testBasicQuery() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
		// SELECT * FROM evaluation_1
		BasicQuery query = new BasicQuery();
		query.setFrom("evaluation" + QueryTools.FROM_TYPE_ID_DELIMTER + EVAL_ID1);
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);
		
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
			// validate expected values
			String id = "" + i;
			assertEquals(id, values.get(headers.indexOf(DBOConstants.PARAM_ANNOTATION_OBJECT_ID)));
			for (int j = 1; j < headers.size(); j++) {
				Object expected = annoMap.get(id + headers.get(j));
				if (expected == null) {
					// this is a system-defined Annotation; ignore it
					continue;
				}
				String actual = values.get(j);
				assertEquals(expected.toString(), actual);
			}
		}
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testQueryMissingId() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
		BasicQuery query = new BasicQuery();
		query.setFrom("evaluation");
		queryDAO.executeQuery(query, mockUserInfo);
	}
	
	@Test
	public void testQueryPaging() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
		// SELECT * FROM evaluation_1 LIMIT 10 OFFSET 10
		int limit = 10;
		int offset = 10;
		BasicQuery query = new BasicQuery();
		query.setFrom("evaluation" + QueryTools.FROM_TYPE_ID_DELIMTER + EVAL_ID1);
		query.setLimit(limit);
		query.setOffset(offset);
				
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
			// validate expected values
			String id = "" + (offset + i);
			assertEquals(id, values.get(headers.indexOf(DBOConstants.PARAM_ANNOTATION_OBJECT_ID)));
			for (int j = 1; j < headers.size(); j++) {
				Object expected = annoMap.get(id + headers.get(j));
				if (expected == null) {
					// this is a system-defined Annotation; ignore it
					continue;
				}
				String actual = values.get(j);
				assertEquals(expected.toString(), actual);
			}
		}
	}
	
	@Test
	public void testNoPrivateAnnosInResults() throws Exception {
		BasicQuery query = new BasicQuery();
		query.setFrom("evaluation" + QueryTools.FROM_TYPE_ID_DELIMTER + EVAL_ID2);
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);
		
		// perform the query
		QueryTableResults results = queryDAO.executeQuery(query, mockUserInfo);
		
		// since we don't have private read access to Eval2, this annotation will be omitted from the results
		assertFalse(results.getHeaders().contains(TestUtils.PRIVATE_LONG_ANNOTATION_NAME));
		
		// let's specify the private annotation explicitly in the SELECT clause
		query.setSelect(Arrays.asList(new String[]{"objectId", TestUtils.PRIVATE_LONG_ANNOTATION_NAME}));
		results = queryDAO.executeQuery(query, mockUserInfo);
		// it DOES appear in the headers
		assertEquals(query.getSelect(), results.getHeaders());
		List<Row> rows = results.getRows();
		Row row = rows.get(0);
		List<String> values = row.getValues();
		String privateValue = values.get(1); // the 'long' annotation is the second in the list
		assertNull(privateValue); // but the value is omitted
	}
	
	@Test
	public void testBasicQueryNoPrivate() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
		// SELECT * FROM evaluation_2
		// we do not have READ_PRIVATE_ANNOTATIONS permission on evaluation_2
		BasicQuery query = new BasicQuery();
		query.setFrom("evaluation" + QueryTools.FROM_TYPE_ID_DELIMTER + EVAL_ID2);
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);
		
		// perform the query
		QueryTableResults results = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(NUM_SUBMISSIONS, results.getTotalNumberOfResults().longValue());
		assertEquals(NUM_SUBMISSIONS, results.getRows().size());
		
		// examine the results
		List<Row> rows = results.getRows();
		List<String> headers = new ArrayList<String>(results.getHeaders());
		// since we don't have private read access to Eval2, this annotation will be omitted from the results
		assertFalse(headers.contains(TestUtils.PRIVATE_LONG_ANNOTATION_NAME));
		for (int i = 0; i < rows.size(); i++) {
			Row row = rows.get(i);
			List<String> values = row.getValues();
			// validate expected values
			String id = "" + (NUM_SUBMISSIONS + i);
			assertEquals(id, values.get(headers.indexOf(DBOConstants.PARAM_ANNOTATION_OBJECT_ID)));
			for (int j = 1; j < headers.size(); j++) {
				Object expected = annoMap.get(id + headers.get(j));
				if (expected == null) {
					// this is a system-defined Annotation; ignore it
					continue;
				}
				String actual = values.get(j);
				assertEquals(expected.toString(), actual);
			}
		}
	}
	
	// filtering on a private annotation (when we lack private read access) omits the matches from the result set
	@Test
	public void testQueryNoPrivateRead_FilterOnPrivate() throws DatastoreException, NotFoundException, JSONObjectAdapterException {		
		// SELECT * FROM evaluation_2 where long_anno=300
		// we do not have READ_PRIVATE_ANNOTATIONS permission on evaluation_2
		BasicQuery query = new BasicQuery();
		query.setFrom("evaluation" + QueryTools.FROM_TYPE_ID_DELIMTER + EVAL_ID2);
		Expression expression = new Expression(new CompoundId(null, TestUtils.PRIVATE_LONG_ANNOTATION_NAME), Comparator.EQUALS, NUM_SUBMISSIONS*10L);
		List<Expression> filters = new ArrayList<Expression>();
		filters.add(expression);
		query.setFilters(filters);		
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);
		
		// perform the query
		QueryTableResults results = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(0, results.getTotalNumberOfResults().longValue());
		assertEquals(0, results.getRows().size());
		
		// if we have private read access we CAN see the results
		when(mockAclDAO.canAccess(any(), eq(EVAL_ID2),
				eq(ObjectType.EVALUATION), eq(ACCESS_TYPE.READ_PRIVATE_SUBMISSION))).thenReturn(true);
		results = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(1, results.getTotalNumberOfResults().longValue());
		assertEquals(1, results.getRows().size());
	}
	
	// ordering by a private annotation (when we lack private read access) doesn't affect the results
	@Test
	public void testQueryNoPrivateRead_OrderByPrivate() throws DatastoreException, NotFoundException, JSONObjectAdapterException {		
		// SELECT * FROM evaluation_2 where long_anno=300
		// we do not have READ_PRIVATE_ANNOTATIONS permission on evaluation_2
		BasicQuery query = new BasicQuery();
		query.setFrom("evaluation" + QueryTools.FROM_TYPE_ID_DELIMTER + EVAL_ID2);
		List<Expression> filters = new ArrayList<Expression>();
		query.setFilters(filters);		
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);
		query.setSort(TestUtils.PRIVATE_LONG_ANNOTATION_NAME);
		
		// perform the query
		QueryTableResults noPrivateAccessResults = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(noPrivateAccessResults);
		assertEquals(30, noPrivateAccessResults.getTotalNumberOfResults().longValue());
		assertEquals(30, noPrivateAccessResults.getRows().size());
		int noPrivateObjectIdIndex = findString("objectId", noPrivateAccessResults.getHeaders());
		assertFalse(noPrivateObjectIdIndex==-1);
		
		// if we have private read access we CAN see the results
		when(mockAclDAO.canAccess(any(), eq(EVAL_ID2),
				eq(ObjectType.EVALUATION), eq(ACCESS_TYPE.READ_PRIVATE_SUBMISSION))).thenReturn(true);
		QueryTableResults privateAccessResults = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(privateAccessResults);
		assertEquals(30, privateAccessResults.getTotalNumberOfResults().longValue());
		assertEquals(30, privateAccessResults.getRows().size());
		int privateObjectIdIndex = findString("objectId", privateAccessResults.getHeaders());
		assertFalse(privateObjectIdIndex==-1);
		
		// show that the ordering doesn't change!
		for (int i=0; i<30; i++) {
			Row privateRow = privateAccessResults.getRows().get(i);
			Row noPrivateRow = noPrivateAccessResults.getRows().get(i);
			assertEquals(noPrivateRow.getValues().get(noPrivateObjectIdIndex), privateRow.getValues().get(privateObjectIdIndex));
		}
	}
	
	private static int findString(String s, List<String> list) {
		for (int i=0; i<list.size(); i++) if (s.equals(list.get(i))) return i;
		return -1;
	}
	
	@Test
	public void testQueryNoPrivateRead_InequalityFilterOnPrivate() throws DatastoreException, NotFoundException, JSONObjectAdapterException {		
		// SELECT * FROM evaluation_2 where long_anno=300
		// we do not have READ_PRIVATE_ANNOTATIONS permission on evaluation_2
		BasicQuery query = new BasicQuery();
		query.setFrom("evaluation" + QueryTools.FROM_TYPE_ID_DELIMTER + EVAL_ID2);
		Expression expression = new Expression(new CompoundId(null, TestUtils.PRIVATE_LONG_ANNOTATION_NAME), Comparator.NOT_EQUALS, NUM_SUBMISSIONS*10L);
		List<Expression> filters = new ArrayList<Expression>();
		filters.add(expression);
		query.setFilters(filters);		
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);
		
		// perform the query
		QueryTableResults results = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(0, results.getTotalNumberOfResults().longValue());
		assertEquals(0, results.getRows().size());
		
		// if we have private read access we CAN see the results
		when(mockAclDAO.canAccess(any(), eq(EVAL_ID2),
				eq(ObjectType.EVALUATION), eq(ACCESS_TYPE.READ_PRIVATE_SUBMISSION))).thenReturn(true);
		results = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(NUM_SUBMISSIONS-1, results.getTotalNumberOfResults().longValue());
		assertEquals(NUM_SUBMISSIONS-1, results.getRows().size());
	}
	
	@Test
	public void testQueryNoPrivateFilterOnPrivateAndPublic() throws DatastoreException, NotFoundException, JSONObjectAdapterException {		
		// SELECT * FROM evaluation_2 where long_anno=300 and string_anno="foo 30"
		// we do not have READ_PRIVATE_ANNOTATIONS permission on evaluation_2
		BasicQuery query = new BasicQuery();
		query.setFrom("evaluation" + QueryTools.FROM_TYPE_ID_DELIMTER + EVAL_ID2);
		List<Expression> filters = new ArrayList<Expression>();
		{
			Expression expression = new Expression(new CompoundId(null, TestUtils.PRIVATE_LONG_ANNOTATION_NAME), Comparator.EQUALS, NUM_SUBMISSIONS*10L);
			filters.add(expression);
		}
		{
			Expression expression = new Expression(new CompoundId(null, TestUtils.PUBLIC_STRING_ANNOTATION_NAME), Comparator.EQUALS, "foo "+NUM_SUBMISSIONS);
			filters.add(expression);
		}
		query.setFilters(filters);		
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);
		
		// perform the query
		QueryTableResults results = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(0, results.getTotalNumberOfResults().longValue());
		assertEquals(0, results.getRows().size());
		
		// if we have private read access we CAN see the results
		when(mockAclDAO.canAccess(any(), eq(EVAL_ID2),
				eq(ObjectType.EVALUATION), eq(ACCESS_TYPE.READ_PRIVATE_SUBMISSION))).thenReturn(true);
		results = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(1, results.getTotalNumberOfResults().longValue());
		assertEquals(1, results.getRows().size());
	}
	
	@Test
	public void testQueryFilterByString() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
		// SELECT * FROM evaluation_1 WHERE "string_anno"="foo 3"
		String attName = TestUtils.PUBLIC_STRING_ANNOTATION_NAME;
		BasicQuery query = new BasicQuery();
		query.setFrom("evaluation" + QueryTools.FROM_TYPE_ID_DELIMTER + EVAL_ID1);
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);	
		Expression exp = new Expression(new CompoundId(null, attName), Comparator.EQUALS, "foo 3");
		List<Expression> filters = new ArrayList<Expression>();
		filters.add(exp);
		query.setFilters(filters);		
		
		// perform the query
		QueryTableResults results = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(1, results.getTotalNumberOfResults().longValue());
		assertEquals(1, results.getRows().size());
		
		// examine the results
		List<Row> rows = results.getRows();
		List<String> headers = new ArrayList<String>(results.getHeaders());
		assertTrue(headers.contains(attName));
		for (int i = 0; i < rows.size(); i++) {
			Row row = rows.get(i);
			List<String> values = row.getValues();
			// validate all values
			String id = "3";
			assertEquals(id, values.get(headers.indexOf(DBOConstants.PARAM_ANNOTATION_OBJECT_ID)));
			for (int j = 1; j < headers.size(); j++) {
				Object expected = annoMap.get(id + headers.get(j));
				if (expected == null) {
					// this is a system-defined Annotation; ignore it
					continue;
				}
				String actual = values.get(j);
				assertEquals(expected.toString(), actual);
			}
		}
	}
	
	@Test
	public void testQueryFilterByLong() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
		// SELECT * FROM evaluation_1 WHERE "long anno"="40"
		String attName = TestUtils.PRIVATE_LONG_ANNOTATION_NAME;
		BasicQuery query = new BasicQuery();
		query.setFrom("evaluation" + QueryTools.FROM_TYPE_ID_DELIMTER + EVAL_ID1);
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);		
		Expression exp = new Expression(new CompoundId(null, attName), Comparator.EQUALS, 40);
		List<Expression> filters = new ArrayList<Expression>();
		filters.add(exp);
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
			assertTrue(headers.contains(attName));
			// validate all values
			String id = "4";
			assertEquals(id, values.get(headers.indexOf(DBOConstants.PARAM_ANNOTATION_OBJECT_ID)));
			for (int j = 1; j < headers.size(); j++) {
				Object expected = annoMap.get(id + headers.get(j));
				if (expected == null) {
					// this is a system-defined Annotation; ignore it
					continue;
				}
				String actual = values.get(j);
				assertEquals(expected.toString(), actual);
			}
		}
	}
		
	@Test
	public void testQueryFilterByDouble() throws DatastoreException, NotFoundException, JSONObjectAdapterException {		
		// SELECT * FROM evaluation_1 WHERE "double anno"="5.5"
		String attName = "double anno";
		BasicQuery query = new BasicQuery();
		query.setFrom("evaluation" + QueryTools.FROM_TYPE_ID_DELIMTER + EVAL_ID1);
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);		
		Expression exp = new Expression(new CompoundId(null, attName), Comparator.EQUALS, 5.5);
		List<Expression> filters = new ArrayList<Expression>();
		filters.add(exp);
		query.setFilters(filters);		
		
		// perform the query
		QueryTableResults results = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(1, results.getTotalNumberOfResults().longValue());
		assertEquals(1, results.getRows().size());
		
		// examine the results
		List<Row> rows = results.getRows();
		List<String> headers = new ArrayList<String>(results.getHeaders());
		assertTrue(headers.contains(attName));
		for (int i = 0; i < rows.size(); i++) {
			Row row = rows.get(i);
			List<String> values = row.getValues();
			// validate all values
			String id = "5";
			assertEquals(id, values.get(headers.indexOf(DBOConstants.PARAM_ANNOTATION_OBJECT_ID)));
			for (int j = 1; j < headers.size(); j++) {
				Object expected = annoMap.get(id + headers.get(j));
				if (expected == null) {
					// this is a system-defined Annotation; ignore it
					continue;
				}
				String actual = values.get(j);
				assertEquals(expected.toString(), actual);
			}
		}
	}
	
	@Test
	public void testQueryGreaterThanEqualToLong() throws DatastoreException, NotFoundException, JSONObjectAdapterException {		
		// SELECT * FROM evaluation_1 WHERE "long anno">"150"
		String attName = TestUtils.PRIVATE_LONG_ANNOTATION_NAME;
		BasicQuery query = new BasicQuery();
		query.setFrom("evaluation" + QueryTools.FROM_TYPE_ID_DELIMTER + EVAL_ID1);
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);		
		Expression exp = new Expression(new CompoundId(null, attName), Comparator.GREATER_THAN_OR_EQUALS, 150);
		List<Expression> filters = new ArrayList<Expression>();
		filters.add(exp);
		query.setFilters(filters);		
		
		// perform the query
		QueryTableResults results = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(15, results.getTotalNumberOfResults().longValue());
		assertEquals(15, results.getRows().size());
		
		// examine the results
		List<Row> rows = results.getRows();
		List<String> headers = new ArrayList<String>(results.getHeaders());
		assertTrue(headers.contains(attName));
		for (int i = 0; i < rows.size(); i++) {
			Row row = rows.get(i);
			List<String> values = row.getValues();
			// validate all values
			String id = "" + (15+i);
			assertEquals(id, values.get(headers.indexOf(DBOConstants.PARAM_ANNOTATION_OBJECT_ID)));
			for (int j = 1; j < headers.size(); j++) {
				Object expected = annoMap.get(id + headers.get(j));
				if (expected == null) {
					// this is a system-defined Annotation; ignore it
					continue;
				}
				String actual = values.get(j);
				assertEquals(expected.toString(), actual);
			}
		}
	}
	
	@Test
	public void testQueryLessThanDouble() throws DatastoreException, NotFoundException, JSONObjectAdapterException {		
		// SELECT * FROM evaluation_1 WHERE "double anno"<"15.0"
		String attName = "double anno";
		BasicQuery query = new BasicQuery();
		query.setFrom("evaluation" + QueryTools.FROM_TYPE_ID_DELIMTER + EVAL_ID1);
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);		
		Expression exp = new Expression(new CompoundId(null, attName), Comparator.LESS_THAN, 15.0);
		List<Expression> filters = new ArrayList<Expression>();
		filters.add(exp);
		query.setFilters(filters);		
		
		// perform the query
		QueryTableResults results = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(15, results.getTotalNumberOfResults().longValue());
		assertEquals(15, results.getRows().size());
		
		// examine the results
		List<Row> rows = results.getRows();
		List<String> headers = new ArrayList<String>(results.getHeaders());
		assertTrue(headers.contains(attName));
		for (int i = 0; i < rows.size(); i++) {
			Row row = rows.get(i);
			List<String> values = row.getValues();
			// validate all values
			String id = "" + i;
			assertEquals(id, values.get(headers.indexOf(DBOConstants.PARAM_ANNOTATION_OBJECT_ID)));
			for (int j = 1; j < headers.size(); j++) {
				Object expected = annoMap.get(id + headers.get(j));
				if (expected == null) {
					// this is a system-defined Annotation; ignore it
					continue;
				}
				String actual = values.get(j);
				assertEquals(expected.toString(), actual);
			}
		}
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testQueryNullAttribute() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
		// SELECT * FROM evaluation_1 WHERE null="foo 3"
		BasicQuery query = new BasicQuery();
		query.setFrom("evaluation" + QueryTools.FROM_TYPE_ID_DELIMTER + EVAL_ID1);
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);	
		Expression exp = new Expression(new CompoundId(null, null), Comparator.EQUALS, "foo 3");
		List<Expression> filters = new ArrayList<Expression>();
		filters.add(exp);
		query.setFilters(filters);		
		
		queryDAO.executeQuery(query, mockUserInfo);
	}
	
	@Test
	public void testQueryNullValue() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
		// SELECT * FROM evaluation_1 WHERE "string anno_null"=null
		String attName ="string anno_null";
		BasicQuery query = new BasicQuery();
		query.setFrom("evaluation" + QueryTools.FROM_TYPE_ID_DELIMTER + EVAL_ID1);
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);	
		Expression exp = new Expression(new CompoundId(null, attName), Comparator.EQUALS, null);
		List<Expression> filters = new ArrayList<Expression>();
		filters.add(exp);
		query.setFilters(filters);		
		
		QueryTableResults results = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(8, results.getTotalNumberOfResults().longValue());
		assertEquals(8, results.getRows().size());
		
		// examine the results
		List<Row> rows = results.getRows();
		List<String> headers = new ArrayList<String>(results.getHeaders());
		assertTrue(headers.contains(attName));
		int index = headers.indexOf(attName);
		for (int i = 0; i < rows.size(); i++) {
			Row row = rows.get(i);
			List<String> values = row.getValues();
			// validate all values
			assertNull(values.get(index));
		}
	}
	
	@Test
	public void testQueryNotNullValue() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
		// SELECT * FROM evaluation_1 WHERE "string anno_null"!=null
		String attName ="string anno_null";
		BasicQuery query = new BasicQuery();
		query.setFrom("evaluation" + QueryTools.FROM_TYPE_ID_DELIMTER + EVAL_ID1);
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);	
		Expression exp = new Expression(new CompoundId(null, attName), Comparator.NOT_EQUALS, null);
		List<Expression> filters = new ArrayList<Expression>();
		filters.add(exp);
		query.setFilters(filters);		
		
		QueryTableResults results = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(15, results.getTotalNumberOfResults().longValue());
		assertEquals(15, results.getRows().size());
		
		// examine the results
		List<Row> rows = results.getRows();
		List<String> headers = new ArrayList<String>(results.getHeaders());
		assertTrue(headers.contains(attName));
		int index = headers.indexOf(attName);
		for (int i = 0; i < rows.size(); i++) {
			Row row = rows.get(i);
			List<String> values = row.getValues();
			// validate all values
			assertNotNull(values.get(index));
		}
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testQueryBadComparator() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
		// SELECT * FROM evaluation_1 WHERE "string_anno"<"foo 3"
		BasicQuery query = new BasicQuery();
		query.setFrom("evaluation" + QueryTools.FROM_TYPE_ID_DELIMTER + EVAL_ID1);
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);	
		Expression exp = new Expression(new CompoundId(null, TestUtils.PUBLIC_STRING_ANNOTATION_NAME), Comparator.LESS_THAN, "foo 3");
		List<Expression> filters = new ArrayList<Expression>();
		filters.add(exp);
		query.setFilters(filters);		
		
		queryDAO.executeQuery(query, mockUserInfo);
	}
	
	@Test
	public void testQueryNoResults() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
		// SELECT * FROM evaluation_1 WHERE "other anno"="does not exist"
		BasicQuery query = new BasicQuery();
		query.setFrom("evaluation" + QueryTools.FROM_TYPE_ID_DELIMTER + EVAL_ID1);
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);		
		Expression exp = new Expression(new CompoundId(null, "other anno"), Comparator.EQUALS, "does not exist");		
		List<Expression> filters = new ArrayList<Expression>();
		filters.add(exp);
		query.setFilters(filters);		
		
		// perform the query
		QueryTableResults results = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(0, results.getTotalNumberOfResults().longValue());
		assertEquals(0, results.getRows().size());
	}
	
	@Test
	public void testQueryWithProjection() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
		// SELECT "objectId", "string_anno" FROM evaluation_1
		BasicQuery query = new BasicQuery();
		query.setFrom("evaluation" + QueryTools.FROM_TYPE_ID_DELIMTER + EVAL_ID1);
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);
		List<String> select = new ArrayList<String>();
		select.add(DBOConstants.PARAM_ANNOTATION_OBJECT_ID);
		select.add(TestUtils.PUBLIC_STRING_ANNOTATION_NAME);
		query.setSelect(select);
		
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
			// validate all values
			String id = "" + i;
			assertEquals(id, values.get(0));
			for (int j = 1; j < headers.size(); j++) {
				Object expected = annoMap.get(id + headers.get(j));
				if (expected == null) {
					// this is a system-defined Annotation; ignore it
					continue;
				}
				String actual = values.get(j);
				assertEquals(expected.toString(), actual);
			}
		}
	}
	
	@Test
	public void testQuerySortAscending() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
		// SELECT * FROM evaluation_1 ORDER BY "string_anno" ASC
		BasicQuery query = new BasicQuery();
		query.setFrom("evaluation" + QueryTools.FROM_TYPE_ID_DELIMTER + EVAL_ID1);
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);
		query.setSort(TestUtils.PUBLIC_STRING_ANNOTATION_NAME);
		query.setAscending(true);
		List<String> select = new ArrayList<String>();
		select.add(TestUtils.PUBLIC_STRING_ANNOTATION_NAME);
		query.setSelect(select);
		
		// perform the query
		QueryTableResults results = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(NUM_SUBMISSIONS, results.getTotalNumberOfResults().longValue());
		assertEquals(NUM_SUBMISSIONS, results.getRows().size());
		
		// examine the results
		List<Row> rows = results.getRows();
		int index = results.getHeaders().indexOf(TestUtils.PUBLIC_STRING_ANNOTATION_NAME);
		String previous = null;
		for (int i = 0; i < rows.size(); i++) {
			Row row = rows.get(i);
			List<String> values = row.getValues();
			// validate ordering
			String current = values.get(index);
			if (previous != null) {
				assertTrue(current.compareTo(previous) > 0);
			}
			previous = current;
		}
	}
	
	@Test
	public void testQuerySortDescending() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
		// SELECT * FROM evaluation_1 ORDER BY "string_anno" DESC
		BasicQuery query = new BasicQuery();
		query.setFrom("evaluation" + QueryTools.FROM_TYPE_ID_DELIMTER + EVAL_ID1);
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);
		query.setSort(TestUtils.PUBLIC_STRING_ANNOTATION_NAME);
		query.setAscending(false);
		List<String> select = new ArrayList<String>();
		select.add(TestUtils.PUBLIC_STRING_ANNOTATION_NAME);
		query.setSelect(select);
		
		// perform the query
		QueryTableResults results = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(NUM_SUBMISSIONS, results.getTotalNumberOfResults().longValue());
		assertEquals(NUM_SUBMISSIONS, results.getRows().size());
		
		// examine the results
		List<Row> rows = results.getRows();
		int index = results.getHeaders().indexOf(TestUtils.PUBLIC_STRING_ANNOTATION_NAME);
		String previous = null;
		for (int i = 0; i < rows.size(); i++) {
			Row row = rows.get(i);
			List<String> values = row.getValues();
			// validate ordering
			String current = values.get(index);
			if (previous != null) {
				assertTrue(current.compareTo(previous) < 0);
			}
			previous = current;
		}
	}
	
	@Test
	public void testQuerySortLongAscending() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
		// SELECT * FROM evaluation_1 ORDER BY "long_anno" ASC
		BasicQuery query = new BasicQuery();
		query.setFrom("evaluation" + QueryTools.FROM_TYPE_ID_DELIMTER + EVAL_ID1);
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);
		query.setSort(TestUtils.PRIVATE_LONG_ANNOTATION_NAME);
		query.setAscending(true);
		List<String> select = new ArrayList<String>();
		select.add(TestUtils.PRIVATE_LONG_ANNOTATION_NAME);
		query.setSelect(select);
		
		// perform the query
		QueryTableResults results = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(NUM_SUBMISSIONS, results.getTotalNumberOfResults().longValue());
		assertEquals(NUM_SUBMISSIONS, results.getRows().size());
		
		// examine the results
		List<Row> rows = results.getRows();
		int index = results.getHeaders().indexOf(TestUtils.PRIVATE_LONG_ANNOTATION_NAME);
		Long previous = null;
		for (int i = 0; i < rows.size(); i++) {
			Row row = rows.get(i);
			System.out.println(row);
			List<String> values = row.getValues();
			// validate ordering
			Long current = Long.parseLong(values.get(index));
			if (previous != null) {
				assertTrue(""+current+" should be bigger than "+previous+" but it's not.", current.compareTo(previous) >= 0);
			}
			previous = current;
		}
	}
	
	@Test
	public void testSortOnColumnHavingNulls() throws Exception {
		// SELECT "string anno_null" FROM evaluation_1 ORDER BY "string anno_null" ASC
		BasicQuery query = new BasicQuery();
		query.setFrom("evaluation" + QueryTools.FROM_TYPE_ID_DELIMTER + EVAL_ID1);
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);
		query.setSort(TestUtils.PUBLIC_STRING_ANNOTATION_WITH_NULLS_NAME);
		query.setAscending(true);
		List<String> select = new ArrayList<String>();
		select.add(TestUtils.PUBLIC_STRING_ANNOTATION_NAME);
		query.setSelect(select);
		
		// In PLFM-2778 this generates an exception 
		queryDAO.executeQuery(query, mockUserInfo);
	}
	
	@Test
	public void testQuerySortLongAscendingBadSortBy() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
		// SELECT * FROM evaluation_1 ORDER BY "gobbledygook" ASC
		BasicQuery query = new BasicQuery();
		query.setFrom("evaluation" + QueryTools.FROM_TYPE_ID_DELIMTER + EVAL_ID1);
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);
		query.setSort("gobbledygook");
		query.setAscending(true);
		List<String> select = new ArrayList<String>();
		select.add(TestUtils.PRIVATE_LONG_ANNOTATION_NAME);
		query.setSelect(select);
		
		// perform the query, sort is ignored
		QueryTableResults results = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(NUM_SUBMISSIONS, results.getTotalNumberOfResults().longValue());
		assertEquals(NUM_SUBMISSIONS, results.getRows().size());
	}
	
	@Test
	public void testQueryNoAnnotations() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
		// SELECT * FROM evaluation_1
		BasicQuery query = new BasicQuery();
		query.setFrom("evaluation" + QueryTools.FROM_TYPE_ID_DELIMTER + EVAL_ID1);
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);
		query.setAscending(true);
		List<String> select = new ArrayList<String>();
		select.add(TestUtils.PRIVATE_LONG_ANNOTATION_NAME);
		query.setSelect(select);
		
		// for this test remove all the annotations
		annotationsDAO.deleteAnnotationsByScope(Long.parseLong(EVAL_ID1));
		// perform the query, should work, with no results
		QueryTableResults results = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(0, results.getTotalNumberOfResults().longValue());
		assertEquals(0, results.getRows().size());
	}
	
	@Test
	public void testQueryNoAnnotationsOrderBy() throws DatastoreException, NotFoundException, JSONObjectAdapterException {
		// SELECT * FROM evaluation_1 ORDER BY "long_anno" ASC
		BasicQuery query = new BasicQuery();
		query.setFrom("evaluation" + QueryTools.FROM_TYPE_ID_DELIMTER + EVAL_ID1);
		query.setLimit(NUM_SUBMISSIONS);
		query.setOffset(0);
		query.setSort(TestUtils.PRIVATE_LONG_ANNOTATION_NAME);
		query.setAscending(true);
		List<String> select = new ArrayList<String>();
		select.add(TestUtils.PRIVATE_LONG_ANNOTATION_NAME);
		query.setSelect(select);
		
		// for this test remove all the annotations
		annotationsDAO.deleteAnnotationsByScope(Long.parseLong(EVAL_ID1));
		// perform the query, should work, with no results
		QueryTableResults results = queryDAO.executeQuery(query, mockUserInfo);
		assertNotNull(results);
		assertEquals(0, results.getTotalNumberOfResults().longValue());
		assertEquals(0, results.getRows().size());
	}
	
	// Flatten an Annotations object to a map. The key is given by [Object ID] + [attribute name],
	// and the value is simply the [attribute value]
	private static void dumpAnnosToMap(Map<String, Object> annoMap, Annotations annos) {
		List<StringAnnotation> stringAnnos = annos.getStringAnnos();
		for (StringAnnotation sa : stringAnnos) {
			annoMap.put(annos.getObjectId() + sa.getKey(), sa.getValue());
		}
		List<LongAnnotation> longAnnos = annos.getLongAnnos();
		for (LongAnnotation la : longAnnos) {
			annoMap.put(annos.getObjectId() + la.getKey(), la.getValue());
		}
		List<DoubleAnnotation> doubleAnnos = annos.getDoubleAnnos();
		for (DoubleAnnotation da : doubleAnnos) {
			annoMap.put(annos.getObjectId() + da.getKey(), da.getValue());
		}
	}	
}
