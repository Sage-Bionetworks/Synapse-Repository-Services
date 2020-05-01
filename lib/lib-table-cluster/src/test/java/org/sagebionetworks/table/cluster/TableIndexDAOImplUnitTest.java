package org.sagebionetworks.table.cluster;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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

	@Spy
	@InjectMocks
	TableIndexDAOImpl spyDao;



	@Test
	public void testValidateMaxListLengthInAnnotationReplication_noListColumns(){

		ColumnModel bar = new ColumnModel();
		bar.setId("1234");
		bar.setName("bar");
		bar.setColumnType(ColumnType.INTEGER);

		List<ColumnModel> currentSchema = Arrays.asList(bar);


		ObjectType objectType = ObjectType.FILE;
		long viewId = 123L;
		long viewTypeMask = 1L;
		Set<Long> allContainersInScope = Sets.newHashSet(111L,222L);
		Set<Long> objectIdFilter = null;

		// method under test
		spyDao.validateMaxListLengthInAnnotationReplication(objectType,viewId,viewTypeMask,
				allContainersInScope,currentSchema,objectIdFilter);
		//validation should not have called any additional helpers
		verify(spyDao).validateMaxListLengthInAnnotationReplication(objectType,viewId,viewTypeMask,
				allContainersInScope,currentSchema,objectIdFilter);
		verifyNoMoreInteractions(spyDao);
	}


	@Test
	public void testValidateMaxListLengthInAnnotationReplication_maxInReplicationExceeded(){
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

		List<ColumnModel> currentSchema = Arrays.asList(foo,bar,baz);

		ObjectType objectType = ObjectType.FILE;
		long viewId = 123L;
		long viewTypeMask = 1L;
		Set<Long> allContainersInScope = Sets.newHashSet(111L,222L);
		Set<Long> objectIdFilter = null;

		HashSet<String> listAnnotationNames = Sets.newHashSet("foo", "baz");

		//mock return a map where "baz" exceeds its defined limit
		doReturn(ImmutableMap.of("foo",4L,"baz",16L)).when(spyDao).getMaxListSizeForAnnotations(objectType, viewId,
				viewTypeMask,allContainersInScope, listAnnotationNames, objectIdFilter);

		String errorMessage = assertThrows(IllegalArgumentException.class, () ->
				// method under test
				spyDao.validateMaxListLengthInAnnotationReplication(objectType,viewId,viewTypeMask,
						allContainersInScope,currentSchema,objectIdFilter)
		).getMessage();

		assertEquals("maximumListLength for ColumnModel \"baz\" must be at least: 16", errorMessage);

		verify(spyDao).getMaxListSizeForAnnotations(objectType, viewId,
				viewTypeMask,allContainersInScope, listAnnotationNames, objectIdFilter);
	}

	@Test
	public void testValidateMaxListLengthInAnnotationReplication_valueNotInReturnedMap(){
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

		List<ColumnModel> currentSchema = Arrays.asList(foo,bar,baz);

		ObjectType objectType = ObjectType.FILE;
		long viewId = 123L;
		long viewTypeMask = 1L;
		Set<Long> allContainersInScope = Sets.newHashSet(111L,222L);
		Set<Long> objectIdFilter = null;

		HashSet<String> listAnnotationNames = Sets.newHashSet("foo", "baz");

		//mock return a map where only "foo" exists as a key
		doReturn(ImmutableMap.of("foo",4L)).when(spyDao).getMaxListSizeForAnnotations(objectType, viewId,
				viewTypeMask,allContainersInScope, listAnnotationNames, objectIdFilter);

		assertDoesNotThrow(() ->
				// method under test
				spyDao.validateMaxListLengthInAnnotationReplication(objectType,viewId,viewTypeMask,
						allContainersInScope,currentSchema,objectIdFilter)
		);

		verify(spyDao).getMaxListSizeForAnnotations(objectType, viewId,
				viewTypeMask,allContainersInScope, listAnnotationNames, objectIdFilter);
	}

	@Test
	public void testValidateMaxListLengthInAnnotationReplication_allUnderLimit(){
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

		List<ColumnModel> currentSchema = Arrays.asList(foo,bar,baz);

		ObjectType objectType = ObjectType.FILE;
		long viewId = 123L;
		long viewTypeMask = 1L;
		Set<Long> allContainersInScope = Sets.newHashSet(111L,222L);
		Set<Long> objectIdFilter = null;

		HashSet<String> listAnnotationNames = Sets.newHashSet("foo", "baz");

		//mock return a map where "baz" does not exceed limit
		doReturn(ImmutableMap.of("foo",4L,"bar",15L)).when(spyDao).getMaxListSizeForAnnotations(objectType, viewId,
				viewTypeMask,allContainersInScope, listAnnotationNames, objectIdFilter);

		assertDoesNotThrow(() ->
				// method under test
				spyDao.validateMaxListLengthInAnnotationReplication(objectType,viewId,viewTypeMask,
						allContainersInScope,currentSchema,objectIdFilter)
		);

		verify(spyDao).getMaxListSizeForAnnotations(objectType, viewId,
				viewTypeMask,allContainersInScope, listAnnotationNames, objectIdFilter);
	}
}
