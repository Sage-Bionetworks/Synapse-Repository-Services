package org.sagebionetworks.table.cluster.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.SubType;
import org.sagebionetworks.table.cluster.view.filter.HierarchicaFilter;
import org.sagebionetworks.table.cluster.view.filter.ViewFilter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import com.google.common.collect.Sets;

public class HierarchicaFilterTest {

	private ReplicationType mainType;
	private Set<SubType> subTypes;
	private List<String> expectedSubTypes;
	private Set<Long> scope;
	private Set<Long> limitObjectIds;
	private Set<String> excludeKeys;

	@BeforeEach
	public void before() {
		mainType = ReplicationType.ENTITY;
		subTypes = Sets.newHashSet(SubType.file);
		expectedSubTypes = subTypes.stream().map(s -> s.name()).collect(Collectors.toList());
		scope = Sets.newHashSet(1L, 2L, 3L);
		limitObjectIds = Sets.newHashSet(1L, 3L);
		excludeKeys = Sets.newHashSet("foo", "bar");
	}

	@Test
	public void testFilter() {
		// call under test
		HierarchicaFilter filter = new HierarchicaFilter(mainType, subTypes, scope);
		assertEquals(
				" R.OBJECT_TYPE = :mainType AND R.SUBTYPE IN (:subTypes)"
						+ " AND R.PARENT_ID IN (:parentIds) AND R.OBJECT_VERSION = R.CURRENT_VERSION",
				filter.getFilterSql());
		MapSqlParameterSource paramters = filter.getParameters();
		MapSqlParameterSource expected = new MapSqlParameterSource();
		expected.addValue("mainType", mainType.name());
		expected.addValue("subTypes", expectedSubTypes);
		expected.addValue("parentIds", scope);
		assertEquals(expected.getValues(), paramters.getValues());
	}

	@Test
	public void testFilterBuilder() {
		// call under test
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, scope).newBuilder()
				.addExcludeAnnotationKeys(excludeKeys).addLimitObjectids(limitObjectIds).build();
		assertEquals(
				" R.OBJECT_TYPE = :mainType AND R.SUBTYPE IN (:subTypes)"
						+ " AND R.OBJECT_ID IN (:limitObjectIds) AND A.ANNO_KEY NOT IN (:excludeKeys)"
						+ " AND R.PARENT_ID IN (:parentIds) AND R.OBJECT_VERSION = R.CURRENT_VERSION",
				filter.getFilterSql());
		MapSqlParameterSource paramters = filter.getParameters();
		MapSqlParameterSource expected = new MapSqlParameterSource();
		expected.addValue("mainType", mainType.name());
		expected.addValue("subTypes", expectedSubTypes);
		expected.addValue("limitObjectIds", limitObjectIds);
		expected.addValue("excludeKeys", excludeKeys);
		expected.addValue("parentIds", scope);
		assertEquals(expected.getValues(), paramters.getValues());
	}
	
	@Test
	public void testBuilderWithAllFields() {
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, limitObjectIds, excludeKeys, scope);
		ViewFilter clone = filter.newBuilder().build();
		assertEquals(filter, clone);
	}
	
	@Test
	public void testGetLimitedObjectIds() {
		HierarchicaFilter filter = new HierarchicaFilter(mainType, subTypes, limitObjectIds, excludeKeys, scope);
		Optional<Set<Long>> optional = filter.getLimitObjectIds();
		assertNotNull(optional);
		assertTrue(optional.isPresent());
		assertEquals(limitObjectIds, optional.get());
	}
	
	@Test
	public void testGetLimitedObjectIdswithNull() {
		limitObjectIds = null;
		HierarchicaFilter filter = new HierarchicaFilter(mainType, subTypes, limitObjectIds, excludeKeys, scope);
		Optional<Set<Long>> optional = filter.getLimitObjectIds();
		assertNotNull(optional);
		assertFalse(optional.isPresent());
	}
}
