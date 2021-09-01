package org.sagebionetworks.table.cluster;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.table.AnnotationType;
import org.sagebionetworks.repo.model.table.ColumnConstants;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.MainType;
import org.sagebionetworks.repo.model.table.SubType;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolver;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolverFactory;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldTypeMapper;
import org.sagebionetworks.table.cluster.view.filter.HierarchyFilter;
import org.sagebionetworks.table.cluster.view.filter.ViewFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class TableIndexDAOImplUnitTest {
	@Mock
	private DataSourceTransactionManager mockTransactionManager;
	@Mock
	private TransactionTemplate mockWriteTransactionTemplate;
	@Mock
	private TransactionTemplate mockReadTransactionTemplate;
	@Mock
	private JdbcTemplate mockTemplate;
	@Mock
	private NamedParameterJdbcTemplate mockNamedTemplate;
	@Mock
	private ObjectFieldTypeMapper mockFieldTypeMapper;
	@Mock
	private ObjectFieldModelResolverFactory mockObjectFieldResolverFactory;
	@Mock
	private ObjectFieldModelResolver mockObjectFieldResolver;
	
	@Spy
	@InjectMocks
	private TableIndexDAOImpl spyDao;
	
	private Set<SubType> subTypes = Sets.newHashSet(SubType.file);

	@Test
	public void testValidateMaxListLengthInAnnotationReplication_noListColumns() {

		ColumnModel bar = new ColumnModel();
		bar.setId("1234");
		bar.setName("bar");
		bar.setColumnType(ColumnType.INTEGER);

		List<ColumnModel> currentSchema = Arrays.asList(bar);

		Set<Long> allContainersInScope = Sets.newHashSet(111L, 222L);
		
		ViewFilter filter = new HierarchyFilter(MainType.ENTITY, subTypes, allContainersInScope);

		// method under test
		spyDao.validateMaxListLengthInAnnotationReplication(filter, currentSchema);
		// validation should not have called any additional helpers
		verify(spyDao).validateMaxListLengthInAnnotationReplication(filter, currentSchema);
		verifyNoMoreInteractions(spyDao);
	}

	@Test
	public void testValidateMaxListLengthInAnnotationReplication_maxInReplicationExceeded() {
		ColumnModel foo = new ColumnModel();
		foo.setId("9876");
		foo.setName("foo");
		foo.setColumnType(ColumnType.STRING_LIST);
		foo.setMaximumSize(46L);
		foo.setMaximumListLength(5L);

		ColumnModel bar = new ColumnModel();
		bar.setId("1234");
		bar.setName("bar");
		bar.setColumnType(ColumnType.INTEGER);

		ColumnModel baz = new ColumnModel();
		baz.setId("6666");
		baz.setName("baz");
		baz.setColumnType(ColumnType.INTEGER_LIST);
		baz.setMaximumListLength(15L);

		List<ColumnModel> currentSchema = Arrays.asList(foo, bar, baz);

		Set<Long> allContainersInScope = Sets.newHashSet(111L, 222L);

		HashSet<String> listAnnotationNames = Sets.newHashSet("foo", "baz");
		
		ViewFilter filter = new HierarchyFilter(MainType.ENTITY, subTypes, allContainersInScope);

		// mock return a map where "baz" exceeds its defined limit
		doReturn(ImmutableMap.of("foo", 4L, "baz", 16L)).when(spyDao).getMaxListSizeForAnnotations(filter,
				listAnnotationNames);

		String errorMessage = assertThrows(IllegalArgumentException.class, () ->
		// method under test
		spyDao.validateMaxListLengthInAnnotationReplication(filter, currentSchema)).getMessage();

		assertEquals("maximumListLength for ColumnModel \"baz\" must be at least: 16", errorMessage);

		verify(spyDao).getMaxListSizeForAnnotations(filter, listAnnotationNames);
	}

	@Test
	public void testValidateMaxListLengthInAnnotationReplication_valueNotInReturnedMap() {
		ColumnModel foo = new ColumnModel();
		foo.setId("9876");
		foo.setName("foo");
		foo.setColumnType(ColumnType.STRING_LIST);
		foo.setMaximumSize(46L);
		foo.setMaximumListLength(5L);

		ColumnModel bar = new ColumnModel();
		bar.setId("1234");
		bar.setName("bar");
		bar.setColumnType(ColumnType.INTEGER);

		ColumnModel baz = new ColumnModel();
		baz.setId("6666");
		baz.setName("baz");
		baz.setColumnType(ColumnType.INTEGER_LIST);
		baz.setMaximumListLength(15L);

		List<ColumnModel> currentSchema = Arrays.asList(foo, bar, baz);

		Set<Long> allContainersInScope = Sets.newHashSet(111L, 222L);

		HashSet<String> listAnnotationNames = Sets.newHashSet("foo", "baz");

		ViewFilter filter = new HierarchyFilter(MainType.ENTITY, subTypes, allContainersInScope);

		// mock return a map where only "foo" exists as a key
		doReturn(ImmutableMap.of("foo", 4L)).when(spyDao).getMaxListSizeForAnnotations(filter, listAnnotationNames);

		assertDoesNotThrow(() ->
		// method under test
		spyDao.validateMaxListLengthInAnnotationReplication(filter, currentSchema));

		verify(spyDao).getMaxListSizeForAnnotations(filter, listAnnotationNames);
	}

	@Test
	public void testValidateMaxListLengthInAnnotationReplication_allUnderLimit() {
		ColumnModel foo = new ColumnModel();
		foo.setId("9876");
		foo.setName("foo");
		foo.setColumnType(ColumnType.STRING_LIST);
		foo.setMaximumSize(46L);
		foo.setMaximumListLength(5L);

		ColumnModel bar = new ColumnModel();
		bar.setId("1234");
		bar.setName("bar");
		bar.setColumnType(ColumnType.INTEGER);

		ColumnModel baz = new ColumnModel();
		baz.setId("6666");
		baz.setName("baz");
		baz.setColumnType(ColumnType.INTEGER_LIST);
		baz.setMaximumListLength(15L);

		List<ColumnModel> currentSchema = Arrays.asList(foo, bar, baz);

		Set<Long> allContainersInScope = Sets.newHashSet(111L, 222L);

		HashSet<String> listAnnotationNames = Sets.newHashSet("foo", "baz");

		ViewFilter filter = new HierarchyFilter(MainType.ENTITY, subTypes, allContainersInScope);

		// mock return a map where "baz" does not exceed limit
		doReturn(ImmutableMap.of("foo", 4L, "bar", 15L)).when(spyDao).getMaxListSizeForAnnotations(filter,
				listAnnotationNames);

		assertDoesNotThrow(() ->
		// method under test
		spyDao.validateMaxListLengthInAnnotationReplication(filter, currentSchema));

		verify(spyDao).getMaxListSizeForAnnotations(filter, listAnnotationNames);
	}
	
	// relevant to PLFM-6344
	@Test
	public void testExpandFromAggregationWithZeroMaxStringSize() {
		// string type with max string size of 0
		ColumnAggregation columnAggregation = new ColumnAggregation();
		columnAggregation.setColumnName("foo");
		columnAggregation.setColumnTypeConcat(concatTypes(AnnotationType.STRING));
		columnAggregation.setMaxStringElementSize(0L);
		columnAggregation.setMaxListSize(1L);
		// call under test
		List<ColumnModel> results = TableIndexDAOImpl.expandFromAggregation(Lists.newArrayList(columnAggregation));
		ColumnModel cm = results.get(0);
		assertEquals("foo", cm.getName());
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals(null, cm.getMaximumListLength());
		// gives default string size rather than 0
		assertEquals(ColumnConstants.DEFAULT_STRING_SIZE, cm.getMaximumSize());
	}
	
	// relevant to PLFM-6344
	@Test
	public void testExpandFromAggregationWithStringDoubleTypeAndZeroMaxStringSize() {
		// string and double type with max string size of 0
		ColumnAggregation columnAggregation = new ColumnAggregation();
		columnAggregation.setColumnName("bar");
		columnAggregation.setColumnTypeConcat(concatTypes(AnnotationType.STRING, AnnotationType.DOUBLE));
		columnAggregation.setMaxStringElementSize(0L);
		columnAggregation.setMaxListSize(1L);
		// call under test
		List<ColumnModel> results = TableIndexDAOImpl.expandFromAggregation(Lists.newArrayList(columnAggregation));
		// first column model will be string with default string size as max string size
		ColumnModel cm = results.get(0);
		assertEquals("bar", cm.getName());
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals(null, cm.getMaximumListLength());
		assertEquals(ColumnConstants.DEFAULT_STRING_SIZE, cm.getMaximumSize());
		// second column model will be double with null max string size
		cm = results.get(1);
		assertEquals("bar", cm.getName());
		assertEquals(ColumnType.DOUBLE, cm.getColumnType());
		assertEquals(null, cm.getMaximumListLength());
		assertEquals(null, cm.getMaximumSize());
	}
	
	// relevant to PLFM-6344
	@Test
	public void testExpandFromAggregationWithStringListTypeAndZeroMaxStringSize() {
		// string list with max string size of 0
		ColumnAggregation columnAggregation = new ColumnAggregation();
		columnAggregation.setColumnName("foobar");
		columnAggregation.setColumnTypeConcat(concatTypes(AnnotationType.STRING));
		columnAggregation.setMaxStringElementSize(0L);
		columnAggregation.setMaxListSize(3L);
		// call under test
		List<ColumnModel> results = TableIndexDAOImpl.expandFromAggregation(Lists.newArrayList(columnAggregation));
		ColumnModel cm = results.get(0);
		assertEquals("foobar", cm.getName());
		assertEquals(ColumnType.STRING_LIST, cm.getColumnType());
		assertEquals(3L, cm.getMaximumListLength());
		// gives default string size for max string size, rather than 0
		assertEquals(ColumnConstants.DEFAULT_STRING_SIZE, cm.getMaximumSize());
	}
	
	// relevant to PLFM-6344
	@Test
	public void testExpandFromAggregationWithDoubleStringListTypeAndZeroMaxStringSize() {
		// string list + double type with 0 max string size
		ColumnAggregation columnAggregation = new ColumnAggregation();
		columnAggregation.setColumnName("barbaz");
		columnAggregation.setColumnTypeConcat(concatTypes(AnnotationType.STRING, AnnotationType.DOUBLE));
		columnAggregation.setMaxStringElementSize(0L);
		columnAggregation.setMaxListSize(3L);
		// call under test
		List<ColumnModel> results = TableIndexDAOImpl.expandFromAggregation(Lists.newArrayList(columnAggregation));
		// first column type becomes a string list
		ColumnModel cm = results.get(0);
		assertEquals("barbaz", cm.getName());
		assertEquals(ColumnType.STRING_LIST, cm.getColumnType());
		assertEquals(3L, cm.getMaximumListLength());
		assertEquals(ColumnConstants.DEFAULT_STRING_SIZE, cm.getMaximumSize());
		// second column type is double
		cm = results.get(1);
		assertEquals("barbaz", cm.getName());
		assertEquals(ColumnType.DOUBLE, cm.getColumnType());
		assertEquals(null, cm.getMaximumListLength());
		assertEquals(null, cm.getMaximumSize());
	}
	
	// relevant to PLFM-6344
	@Test
	public void testExpandFromAggregationWithNullMaxStringSize() {
		// string type with max string size of 0
		ColumnAggregation columnAggregation = new ColumnAggregation();
		columnAggregation.setColumnName("foo");
		columnAggregation.setColumnTypeConcat(concatTypes(AnnotationType.STRING));
		columnAggregation.setMaxStringElementSize(null);
		columnAggregation.setMaxListSize(1L);
		// call under test
		List<ColumnModel> results = TableIndexDAOImpl.expandFromAggregation(Lists.newArrayList(columnAggregation));
		ColumnModel cm = results.get(0);
		assertEquals("foo", cm.getName());
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals(null, cm.getMaximumListLength());
		// gives default string size when null is in column aggregation
		assertEquals(ColumnConstants.DEFAULT_STRING_SIZE, cm.getMaximumSize());
	}
	
	/**
	 * Helper to create a concatenated list of column types delimited with dot ('.')
	 * @param types
	 * @return
	 */
	public static String concatTypes(AnnotationType...types) {
		StringJoiner joiner = new StringJoiner(",");
		for(AnnotationType type: types) {
			joiner.add(type.name());
		}
		return joiner.toString();
	}
}
