package org.sagebionetworks.table.cluster.filter;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.SubType;
import org.sagebionetworks.table.cluster.view.filter.FlatIdAndVersionFilter;
import org.sagebionetworks.table.cluster.view.filter.IdVersionPair;
import org.sagebionetworks.table.cluster.view.filter.ViewFilter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import com.google.common.collect.Sets;

public class FlatIdAndVersionFilterTest {

	private ReplicationType mainType;
	private Set<SubType> subTypes;
	private List<String> expectedSubTypes;
	private Set<IdVersionPair> scope;
	private List<Long[]> expectedScope;
	private Set<Long> limitObjectIds;
	private Set<String> excludeKeys;

	@BeforeEach
	public void before() {
		mainType = ReplicationType.ENTITY;
		subTypes = Sets.newHashSet(SubType.file);
		expectedSubTypes = subTypes.stream().map(s -> s.name()).collect(Collectors.toList());
		scope = Sets.newHashSet(new IdVersionPair().setId(1L).setVersion(1L),
				new IdVersionPair().setId(1L).setVersion(2L));
		expectedScope = Arrays.asList(new Long[] { 1L, 1L }, new Long[] { 1L, 2L });
		limitObjectIds = Sets.newHashSet(1L, 3L);
		excludeKeys = Sets.newHashSet("foo", "bar");
	}

	@Test
	public void testFilter() {
		// call under test
		FlatIdAndVersionFilter filter = new FlatIdAndVersionFilter(mainType, subTypes, scope);
		assertEquals(" R.OBJECT_TYPE = :mainType AND R.SUBTYPE IN (:subTypes)"
				+ " AND (R.OBJECT_ID, R.OBJECT_VERSION) IN (:scopePairs)", filter.getFilterSql());
		MapSqlParameterSource paramters = filter.getParameters();
		assertEquals(3, paramters.getValues().size());
		assertEquals(mainType.name(), paramters.getValue("mainType"));
		assertEquals(expectedSubTypes, paramters.getValue("subTypes"));
		assertEqualsParams(expectedScope, (List<Long[]>) paramters.getValue("scopePairs"));
	}

	@Test
	public void testFilterBuilder() {
		// call under test
		ViewFilter filter = new FlatIdAndVersionFilter(mainType, subTypes, scope).newBuilder()
				.addExcludeAnnotationKeys(excludeKeys).addLimitObjectids(limitObjectIds).build();
		assertEquals(
				" R.OBJECT_TYPE = :mainType AND R.SUBTYPE IN (:subTypes) AND R.OBJECT_ID IN (:limitObjectIds) "
						+ "AND A.ANNO_KEY NOT IN (:excludeKeys) AND (R.OBJECT_ID, R.OBJECT_VERSION) IN (:scopePairs)",
				filter.getFilterSql());
		MapSqlParameterSource paramters = filter.getParameters();
		assertEquals(5, paramters.getValues().size());
		assertEquals(mainType.name(), paramters.getValue("mainType"));
		assertEquals(expectedSubTypes, paramters.getValue("subTypes"));
		assertEquals(limitObjectIds, paramters.getValue("limitObjectIds"));
		assertEquals(excludeKeys, paramters.getValue("excludeKeys"));
		assertEqualsParams(expectedScope, (List<Long[]>) paramters.getValue("scopePairs"));
	}

	public void assertEqualsParams(List<Long[]> one, List<Long[]> two) {
		assertEquals(one.size(), two.size());
		for (int i = 0; i < one.size(); i++) {
			assertArrayEquals(one.get(i), two.get(i));
		}
	}
	
	@Test
	public void testBuilderWithAllFields() {
		FlatIdAndVersionFilter filter = new FlatIdAndVersionFilter(mainType, subTypes, limitObjectIds, excludeKeys, scope);
		ViewFilter clone = filter.newBuilder().build();
		assertEquals(filter, clone);
	}
}
