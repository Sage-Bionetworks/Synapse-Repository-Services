package org.sagebionetworks.table.cluster;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.table.AnnotationType;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewScopeFilter;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolver;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldModelResolverFactory;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldTypeMapper;
import org.sagebionetworks.util.EnumUtils;
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

	private ViewObjectType objectType = ViewObjectType.ENTITY;

	private ViewScopeFilter getScopeFilter(Set<Long> containerIds) {
		return new ViewScopeFilter(objectType, EnumUtils.names(EntityType.file), false, containerIds);
	}

	@Test
	public void testValidateMaxListLengthInAnnotationReplication_noListColumns() {

		ColumnModel bar = new ColumnModel();
		bar.setId("1234");
		bar.setName("bar");
		bar.setColumnType(ColumnType.INTEGER);

		List<ColumnModel> currentSchema = Arrays.asList(bar);

		Set<Long> allContainersInScope = Sets.newHashSet(111L, 222L);
		Set<Long> objectIdFilter = null;

		ViewScopeFilter scopeFilter = getScopeFilter(allContainersInScope);

		// method under test
		spyDao.validateMaxListLengthInAnnotationReplication(scopeFilter, currentSchema, objectIdFilter);
		// validation should not have called any additional helpers
		verify(spyDao).validateMaxListLengthInAnnotationReplication(scopeFilter, currentSchema, objectIdFilter);
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
		Set<Long> objectIdFilter = null;

		HashSet<String> listAnnotationNames = Sets.newHashSet("foo", "baz");

		ViewScopeFilter scopeFilter = getScopeFilter(allContainersInScope);

		// mock return a map where "baz" exceeds its defined limit
		doReturn(ImmutableMap.of("foo", 4L, "baz", 16L)).when(spyDao).getMaxListSizeForAnnotations(scopeFilter,
				listAnnotationNames, objectIdFilter);

		String errorMessage = assertThrows(IllegalArgumentException.class, () ->
		// method under test
		spyDao.validateMaxListLengthInAnnotationReplication(scopeFilter, currentSchema, objectIdFilter)).getMessage();

		assertEquals("maximumListLength for ColumnModel \"baz\" must be at least: 16", errorMessage);

		verify(spyDao).getMaxListSizeForAnnotations(scopeFilter, listAnnotationNames, objectIdFilter);
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
		Set<Long> objectIdFilter = null;

		HashSet<String> listAnnotationNames = Sets.newHashSet("foo", "baz");

		ViewScopeFilter scopeFilter = getScopeFilter(allContainersInScope);

		// mock return a map where only "foo" exists as a key
		doReturn(ImmutableMap.of("foo", 4L)).when(spyDao).getMaxListSizeForAnnotations(scopeFilter, listAnnotationNames,
				objectIdFilter);

		assertDoesNotThrow(() ->
		// method under test
		spyDao.validateMaxListLengthInAnnotationReplication(scopeFilter, currentSchema, objectIdFilter));

		verify(spyDao).getMaxListSizeForAnnotations(scopeFilter, listAnnotationNames, objectIdFilter);
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
		Set<Long> objectIdFilter = null;

		HashSet<String> listAnnotationNames = Sets.newHashSet("foo", "baz");

		ViewScopeFilter scopeFilter = getScopeFilter(allContainersInScope);

		// mock return a map where "baz" does not exceed limit
		doReturn(ImmutableMap.of("foo", 4L, "bar", 15L)).when(spyDao).getMaxListSizeForAnnotations(scopeFilter,
				listAnnotationNames, objectIdFilter);

		assertDoesNotThrow(() ->
		// method under test
		spyDao.validateMaxListLengthInAnnotationReplication(scopeFilter, currentSchema, objectIdFilter));

		verify(spyDao).getMaxListSizeForAnnotations(scopeFilter, listAnnotationNames, objectIdFilter);
	}
	
	// relevant to PLFM-6344
	@Test
	public void testExpandFromAggregationWithZeroMaxStringElementSize() {
		
		// case 1: string column type, max string size is 0
		ColumnAggregation zero = new ColumnAggregation();
		zero.setColumnName("foo");
		zero.setColumnTypeConcat(concatTypes(AnnotationType.STRING));
		zero.setMaxStringElementSize(0L);
		zero.setMaxListSize(1L);
		
		// case 2: string and double column type, max string size is 0
		ColumnAggregation one = new ColumnAggregation();
		one.setColumnName("bar");
		one.setColumnTypeConcat(concatTypes(AnnotationType.STRING, AnnotationType.DOUBLE));
		one.setMaxStringElementSize(0L);
		one.setMaxListSize(1L);

		// case 3: string list column type, max string size is 0
		ColumnAggregation two = new ColumnAggregation();
		two.setColumnName("foobar");
		two.setColumnTypeConcat(concatTypes(AnnotationType.STRING));
		two.setMaxStringElementSize(0L);
		two.setMaxListSize(3L);
		
		// case 4: string and double list column type, max string size is 0
		// 		   string and double column type, max string size is 0
		ColumnAggregation three = new ColumnAggregation();
		three.setColumnName("barbaz");
		three.setColumnTypeConcat(concatTypes(AnnotationType.STRING, AnnotationType.DOUBLE));
		three.setMaxStringElementSize(0L);
		three.setMaxListSize(3L);

		// call under test
		List<ColumnModel> results = TableIndexDAOImpl.expandFromAggregation(Lists.newArrayList(zero,one,two,three));
		assertEquals(6, results.size());
		
		// case 1: string column type with 0L max size, should give 50L max size
		ColumnModel cm = results.get(0);
		assertEquals("foo", cm.getName());
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals(null, cm.getMaximumListLength());
		assertEquals(50L, cm.getMaximumSize());
		
		// case 2: column type with string and double type with 0L max size, should give 50L max size
		// for string column model, and double column model with null max size
		cm = results.get(1);
		assertEquals("bar", cm.getName());
		assertEquals(ColumnType.STRING, cm.getColumnType());
		assertEquals(null, cm.getMaximumListLength());
		assertEquals(50L, cm.getMaximumSize());
		
		cm = results.get(2);
		assertEquals("bar", cm.getName());
		assertEquals(ColumnType.DOUBLE, cm.getColumnType());
		assertEquals(null, cm.getMaximumListLength());
		assertEquals(null, cm.getMaximumSize());
		
		// case 3: string list column type with 0L max size should give 50L max size
		cm = results.get(3);
		assertEquals("foobar", cm.getName());
		assertEquals(ColumnType.STRING_LIST, cm.getColumnType());
		assertEquals(3L, cm.getMaximumListLength());
		assertEquals(50L, cm.getMaximumSize());
		
		// case 4: column type of string and double with list length of 3 and 0L max string size,
		// should give string list with 50L string size for string column model, and a double column model
		// with null list length and null max size since there is no such thing as a double list
		cm = results.get(4);
		assertEquals("barbaz", cm.getName());
		assertEquals(ColumnType.STRING_LIST, cm.getColumnType());
		assertEquals(3L, cm.getMaximumListLength());
		assertEquals(50L, cm.getMaximumSize());

		cm = results.get(5);
		assertEquals("barbaz", cm.getName());
		assertEquals(ColumnType.DOUBLE, cm.getColumnType());
		assertEquals(null, cm.getMaximumListLength());
		assertEquals(null, cm.getMaximumSize());
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
