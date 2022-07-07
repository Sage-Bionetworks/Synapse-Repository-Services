package org.sagebionetworks.table.cluster.filter;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.SubType;
import org.sagebionetworks.table.cluster.view.filter.FlatIdAndVersionFilter;
import org.sagebionetworks.table.cluster.view.filter.IdVersionPair;
import org.sagebionetworks.table.cluster.view.filter.ViewFilter;

import com.google.common.collect.Sets;

public class FlatIdAndVersionFilterTest {

	private ReplicationType mainType;
	private Set<SubType> subTypes;
	private List<String> expectedSubTypes;
	private Set<IdVersionPair> scope;
	private List<Long[]> expectedScope;
	private Set<Long> limitObjectIds;
	private Set<String> excludeKeys;
	private Set<Long> objectIds;

	@BeforeEach
	public void before() {
		mainType = ReplicationType.ENTITY;
		subTypes = Sets.newHashSet(SubType.file);
		expectedSubTypes = subTypes.stream().map(s -> s.name()).collect(Collectors.toList());
		scope = Sets.newHashSet(new IdVersionPair().setId(1L).setVersion(1L),
				new IdVersionPair().setId(1L).setVersion(2L));
		expectedScope = Arrays.asList(new Long[] { 1L, 1L }, new Long[] { 1L, 2L });
		objectIds = scope.stream().map(i->i.getId()).collect(Collectors.toSet());
		limitObjectIds = Sets.newHashSet(1L, 3L);
		excludeKeys = Sets.newHashSet("foo", "bar");
	}

	@Test
	public void testFilter() {
		// call under test
		FlatIdAndVersionFilter filter = new FlatIdAndVersionFilter(mainType, subTypes, scope);
		assertEquals(" R.OBJECT_TYPE = :mainType AND R.SUBTYPE IN (:subTypes)"
				+ " AND (R.OBJECT_ID, R.OBJECT_VERSION) IN (:scopePairs)", filter.getFilterSql());
		Map<String, Object> paramters = filter.getParameters();
		assertEquals(4, paramters.size());
		assertEquals(mainType.name(), paramters.get("mainType"));
		assertEquals(expectedSubTypes, paramters.get("subTypes"));
		assertEquals(objectIds, paramters.get("objectIds"));
		assertEqualsParams(expectedScope, (List<Long[]>) paramters.get("scopePairs"));
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
		assertEquals(
				" R.OBJECT_TYPE = :mainType AND R.SUBTYPE IN (:subTypes) AND R.OBJECT_ID IN (:limitObjectIds)"
				+ " AND A.ANNO_KEY NOT IN (:excludeKeys) AND R.OBJECT_ID IN (:objectIds)",
				filter.getObjectIdFilterSql());
		Map<String, Object> paramters = filter.getParameters();
		assertEquals(6, paramters.size());
		assertEquals(mainType.name(), paramters.get("mainType"));
		assertEquals(expectedSubTypes, paramters.get("subTypes"));
		assertEquals(limitObjectIds, paramters.get("limitObjectIds"));
		assertEquals(excludeKeys, paramters.get("excludeKeys"));
		assertEqualsParams(expectedScope, (List<Long[]>) paramters.get("scopePairs"));
		assertEquals(objectIds, (Set<Long>) paramters.get("objectIds"));
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
	
	@Test
	public void testGetLimitedObjectIds() {
		FlatIdAndVersionFilter filter = new FlatIdAndVersionFilter(mainType, subTypes, limitObjectIds, excludeKeys, scope);
		Optional<Set<Long>> optional = filter.getLimitObjectIds();
		assertNotNull(optional);
		assertTrue(optional.isPresent());
		assertEquals(limitObjectIds, optional.get());
	}
	
	@Test
	public void testGetLimitedObjectIdswithNull() {
		limitObjectIds = null;
		FlatIdAndVersionFilter filter = new FlatIdAndVersionFilter(mainType, subTypes, limitObjectIds, excludeKeys, scope);
		Optional<Set<Long>> optional = filter.getLimitObjectIds();
		assertNotNull(optional);
		assertFalse(optional.isPresent());
	}
	
	@Test
	public void testGetSubViews() {
		FlatIdAndVersionFilter filter = new FlatIdAndVersionFilter(mainType, subTypes, limitObjectIds, excludeKeys, scope);
		// call under test
		Optional<List<ChangeMessage>> results = filter.getSubViews();
		Optional<List<ChangeMessage>> expected = Optional.empty();
		assertEquals(expected, results);
	}
}
