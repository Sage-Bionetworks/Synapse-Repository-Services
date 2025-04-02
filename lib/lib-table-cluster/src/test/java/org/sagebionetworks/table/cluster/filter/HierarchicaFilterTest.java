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
	private Set<Long> parentIds;
	private Set<Long> limitObjectIds;
	private Set<String> excludeKeys;
	private boolean excludeDerivedKeys;
	private Set<Long> scope;
	
	@BeforeEach
	public void before() {
		mainType = ReplicationType.ENTITY;
		subTypes = Sets.newHashSet(SubType.file);
		expectedSubTypes = subTypes.stream().map(s -> s.name()).collect(Collectors.toList());
		parentIds = Sets.newHashSet(1L, 2L, 3L);
		limitObjectIds = Sets.newHashSet(1L, 3L);
		excludeKeys = Sets.newHashSet("foo", "bar");
		excludeDerivedKeys = true;
		scope = Sets.newHashSet(1L);
	}

	@Test
	public void testFilter() {
		// call under test
		HierarchicaFilter filter = new HierarchicaFilter(mainType, subTypes, parentIds);
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
		expected.put("parentIds", parentIds);
		assertEquals(expected, paramters);
	}

	@Test
	public void testFilterBuilder() {
		// call under test
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, parentIds).newBuilder()
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
		expected.put("parentIds", parentIds);
		assertEquals(expected, paramters);
	}

	@Test
	public void testBuilderWithAllFields() {
		ViewFilter filter = new HierarchicaFilter(mainType, subTypes, limitObjectIds, excludeKeys, parentIds, excludeDerivedKeys, scope);
		ViewFilter clone = filter.newBuilder().build();
		assertEquals(filter, clone);
	}

	@Test
	public void testGetLimitedObjectIds() {
		HierarchicaFilter filter = new HierarchicaFilter(mainType, subTypes, limitObjectIds, excludeKeys, parentIds, excludeDerivedKeys, scope);
		Optional<Set<Long>> optional = filter.getLimitObjectIds();
		assertNotNull(optional);
		assertTrue(optional.isPresent());
		assertEquals(limitObjectIds, optional.get());
	}

	@Test
	public void testGetLimitedObjectIdswithNull() {
		limitObjectIds = null;
		HierarchicaFilter filter = new HierarchicaFilter(mainType, subTypes, limitObjectIds, excludeKeys, parentIds, excludeDerivedKeys, scope);
		Optional<Set<Long>> optional = filter.getLimitObjectIds();
		assertNotNull(optional);
		assertFalse(optional.isPresent());
	}

	@Test
	public void testGetSubViewsWithEntityAndMultipleParents() {
		mainType = ReplicationType.ENTITY;
		parentIds = Sets.newHashSet(1L, 2L, 3L);
		HierarchicaFilter filter = new HierarchicaFilter(mainType, subTypes, limitObjectIds, excludeKeys, parentIds, excludeDerivedKeys, scope);
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
		parentIds = Sets.newHashSet(1L);
		HierarchicaFilter filter = new HierarchicaFilter(mainType, subTypes, limitObjectIds, excludeKeys, parentIds, excludeDerivedKeys, scope);
		// call under test
		Optional<List<ChangeMessage>> results = filter.getSubViews();
		Optional<List<ChangeMessage>> expected = Optional.empty();
		assertEquals(expected, results);
	}

	@Test
	public void testGetSubViewsWithSubmissionAndMultipleParents() {
		mainType = ReplicationType.SUBMISSION;
		parentIds = Sets.newHashSet(1L, 2L, 3L);
		HierarchicaFilter filter = new HierarchicaFilter(mainType, subTypes, limitObjectIds, excludeKeys, parentIds, excludeDerivedKeys, scope);
		// call under test
		Optional<List<ChangeMessage>> results = filter.getSubViews();
		Optional<List<ChangeMessage>> expected = Optional.empty();
		assertEquals(expected, results);
	}

	@Test
	public void testGetSubViewsWithSubmissionAndSingleParent() {
		mainType = ReplicationType.SUBMISSION;
		parentIds = Sets.newHashSet(1L);
		HierarchicaFilter filter = new HierarchicaFilter(mainType, subTypes, limitObjectIds, excludeKeys, parentIds, excludeDerivedKeys, scope);
		// call under test
		Optional<List<ChangeMessage>> results = filter.getSubViews();
		Optional<List<ChangeMessage>> expected = Optional.empty();
		assertEquals(expected, results);
	}
}
