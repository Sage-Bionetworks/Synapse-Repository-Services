package org.sagebionetworks.table.cluster.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.SubType;
import org.sagebionetworks.table.cluster.view.filter.HierarchicaFilter;
import org.sagebionetworks.table.cluster.view.filter.ViewFilter;

import com.google.common.collect.Sets;

public class HierarchicaFilterTest {

	private ReplicationType mainType;
	private Set<SubType> subTypes;
	private List<String> expectedSubTypes;
	private Set<Long> scope;
	private Set<Long> limitObjectIds;
	private Set<String> excludeKeys;
	private boolean excludeDerivedKeys;
	
	@BeforeEach
	public void before() {
		mainType = ReplicationType.ENTITY;
		subTypes = Sets.newHashSet(SubType.file);
		expectedSubTypes = subTypes.stream().map(s -> s.name()).collect(Collectors.toList());
		scope = Sets.newHashSet(1L, 2L, 3L);
		limitObjectIds = Sets.newHashSet(1L, 3L);
		excludeKeys = Sets.newHashSet("foo", "bar");
		excludeDerivedKeys = true;
	}

	@Test
	public void testFilter() {
		// call under test
		HierarchicaFilter filter = new HierarchicaFilter(mainType, subTypes, scope);
		assertEquals(
				" R.OBJECT_TYPE = :mainType AND R.SUBTYPE IN (:subTypes)"
						+ " AND R.PARENT_ID IN (:parentIds) AND R.OBJECT_VERSION = R.CURRENT_VERSION",
				filter.getFilterSql());
		assertEquals(" R.OBJECT_TYPE = :mainType AND R.SUBTYPE IN (:subTypes) AND R.PARENT_ID IN (:parentIds)",
				filter.getObjectIdFilterSql());
		Map<String, Object> paramters = filter.getParameters();
		Map<String, Object> expected = new HashMap<>();
		expected.put("mainType", mainType.name());
		expected.put("subTypes", expectedSubTypes);
		expected.put("parentIds", scope);
		assertEquals(expected, paramters);
	}

	@Test
	public void testFilterBuilder() {
		// call under test
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, scope).newBuilder()
				.addExcludeAnnotationKeys(excludeKeys)
				.addLimitObjectids(limitObjectIds)
				.setExcludeDerivedKeys(excludeDerivedKeys)
				.build();
		assertEquals(
				" R.OBJECT_TYPE = :mainType AND R.SUBTYPE IN (:subTypes)"
						+ " AND R.OBJECT_ID IN (:limitObjectIds) AND A.ANNO_KEY NOT IN (:excludeKeys) AND A.IS_DERIVED = FALSE"
						+ " AND R.PARENT_ID IN (:parentIds) AND R.OBJECT_VERSION = R.CURRENT_VERSION",
				filter.getFilterSql());
		Map<String, Object> paramters = filter.getParameters();
		Map<String, Object> expected = new HashMap<>();
		expected.put("mainType", mainType.name());
		expected.put("subTypes", expectedSubTypes);
		expected.put("limitObjectIds", limitObjectIds);
		expected.put("excludeKeys", excludeKeys);
		expected.put("parentIds", scope);
		assertEquals(expected, paramters);
	}

	@Test
	public void testBuilderWithAllFields() {
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, limitObjectIds, excludeKeys, scope, excludeDerivedKeys);
		ViewFilter clone = filter.newBuilder().build();
		assertEquals(filter, clone);
	}

	@Test
	public void testGetLimitedObjectIds() {
		HierarchicaFilter filter = new HierarchicaFilter(mainType, subTypes, limitObjectIds, excludeKeys, scope, excludeDerivedKeys);
		Optional<Set<Long>> optional = filter.getLimitObjectIds();
		assertNotNull(optional);
		assertTrue(optional.isPresent());
		assertEquals(limitObjectIds, optional.get());
	}

	@Test
	public void testGetLimitedObjectIdswithNull() {
		limitObjectIds = null;
		HierarchicaFilter filter = new HierarchicaFilter(mainType, subTypes, limitObjectIds, excludeKeys, scope, excludeDerivedKeys);
		Optional<Set<Long>> optional = filter.getLimitObjectIds();
		assertNotNull(optional);
		assertFalse(optional.isPresent());
	}

	@Test
	public void testGetSubViewsWithEntityAndMultipleParents() {
		mainType = ReplicationType.ENTITY;
		scope = Sets.newHashSet(1L, 2L, 3L);
		HierarchicaFilter filter = new HierarchicaFilter(mainType, subTypes, limitObjectIds, excludeKeys, scope, excludeDerivedKeys);
		// call under test
		Optional<List<ChangeMessage>> results = filter.getSubViews();
		Optional<List<ChangeMessage>> expected = Optional
				.of(Arrays.asList(new ChangeMessage().setObjectType(ObjectType.ENTITY_CONTAINER).setObjectId("syn1"),
						new ChangeMessage().setObjectType(ObjectType.ENTITY_CONTAINER).setObjectId("syn2"),
						new ChangeMessage().setObjectType(ObjectType.ENTITY_CONTAINER).setObjectId("syn3")));
		assertEquals(expected, results);
	}

	@Test
	public void testGetSubViewsWithEntityAndSingleParent() {
		mainType = ReplicationType.ENTITY;
		scope = Sets.newHashSet(1L);
		HierarchicaFilter filter = new HierarchicaFilter(mainType, subTypes, limitObjectIds, excludeKeys, scope, excludeDerivedKeys);
		// call under test
		Optional<List<ChangeMessage>> results = filter.getSubViews();
		Optional<List<ChangeMessage>> expected = Optional.empty();
		assertEquals(expected, results);
	}

	@Test
	public void testGetSubViewsWithSubmissionAndMultipleParents() {
		mainType = ReplicationType.SUBMISSION;
		scope = Sets.newHashSet(1L, 2L, 3L);
		HierarchicaFilter filter = new HierarchicaFilter(mainType, subTypes, limitObjectIds, excludeKeys, scope, excludeDerivedKeys);
		// call under test
		Optional<List<ChangeMessage>> results = filter.getSubViews();
		Optional<List<ChangeMessage>> expected = Optional.empty();
		assertEquals(expected, results);
	}

	@Test
	public void testGetSubViewsWithSubmissionAndSingleParent() {
		mainType = ReplicationType.SUBMISSION;
		scope = Sets.newHashSet(1L);
		HierarchicaFilter filter = new HierarchicaFilter(mainType, subTypes, limitObjectIds, excludeKeys, scope, excludeDerivedKeys);
		// call under test
		Optional<List<ChangeMessage>> results = filter.getSubViews();
		Optional<List<ChangeMessage>> expected = Optional.empty();
		assertEquals(expected, results);
	}
}
