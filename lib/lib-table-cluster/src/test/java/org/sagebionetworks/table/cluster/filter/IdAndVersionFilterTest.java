package org.sagebionetworks.table.cluster.filter;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.SubType;
import org.sagebionetworks.table.cluster.view.filter.IdAndVersionFilter;
import org.sagebionetworks.table.cluster.view.filter.ViewFilter;

public class IdAndVersionFilterTest {

	private ReplicationType mainType;
	private Set<SubType> subTypes;
	private List<String> expectedSubTypes;
	private Set<Long> limitObjectIds;
	private Set<String> excludeKeys;
	private boolean excludeDerivedKeys;

	@BeforeEach
	public void before() {
		mainType = ReplicationType.ENTITY;
		subTypes = Set.of(SubType.file);
		expectedSubTypes = subTypes.stream().map(s -> s.name()).collect(Collectors.toList());
		limitObjectIds = Set.of(1L, 3L);
		excludeKeys = Set.of("foo", "bar");
		excludeDerivedKeys = true;
	}

	@Test
	public void testFilter() {
		Set<IdAndVersion> scope = Set.of(KeyFactory.idAndVersion("1", 1L), KeyFactory.idAndVersion("1", 2L));
		List<Long[]> expectedScope = scope.stream().map(id -> new Long[] {id.getId(), id.getVersion().get()}).collect(Collectors.toList());
		Set<Long> objectIds = Set.of(1L);
		// call under test
		IdAndVersionFilter filter = new IdAndVersionFilter(mainType, subTypes, scope);
		assertEquals(" R.OBJECT_TYPE = :mainType AND R.SUBTYPE IN (:subTypes)"
				+ " AND (((R.OBJECT_ID, R.OBJECT_VERSION) IN (:versionedRefs)))", filter.getFilterSql());
		Map<String, Object> paramters = filter.getParameters();
		assertEquals(4, paramters.size());
		assertEquals(mainType.name(), paramters.get("mainType"));
		assertEquals(expectedSubTypes, paramters.get("subTypes"));
		assertEquals(objectIds, paramters.get("objectIds"));
		assertEqualsParams(expectedScope, (List<Long[]>) paramters.get("versionedRefs"));
	}
	
	@Test
	public void testFilterWithNonVersionedRefs() {
		Set<IdAndVersion> scope =  Set.of(KeyFactory.idAndVersion("1", null), KeyFactory.idAndVersion("2", null));
		List<Long> expectedScope = scope.stream().map(IdAndVersion::getId).collect(Collectors.toList());
		Set<Long> objectIds = Set.of(1L, 2L);
		// call under test
		IdAndVersionFilter filter = new IdAndVersionFilter(mainType, subTypes, scope);
		assertEquals(" R.OBJECT_TYPE = :mainType AND R.SUBTYPE IN (:subTypes)"
				+ " AND ((R.OBJECT_ID IN (:nonVersionedRefs) AND R.OBJECT_VERSION = R.CURRENT_VERSION))", filter.getFilterSql());
		Map<String, Object> paramters = filter.getParameters();
		assertEquals(4, paramters.size());
		assertEquals(mainType.name(), paramters.get("mainType"));
		assertEquals(expectedSubTypes, paramters.get("subTypes"));
		assertEquals(objectIds, paramters.get("objectIds"));
		assertEquals(expectedScope, paramters.get("nonVersionedRefs"));
	}
	
	@Test
	public void testFilterWithVersionedAndNonVersionedRefs() {
		Set<IdAndVersion> scope =  Set.of(KeyFactory.idAndVersion("1", null), KeyFactory.idAndVersion("2", 3L));
		List<Long[]> expectedVersionedScope = scope.stream().filter(id -> id.getVersion().isPresent()).map(id -> new Long[] {id.getId(), id.getVersion().get()}).collect(Collectors.toList());
		List<Long> expectedNonVersionedScope = scope.stream().filter(id -> id.getVersion().isEmpty()).map(IdAndVersion::getId).collect(Collectors.toList());
		Set<Long> objectIds = Set.of(1L, 2L);
		// call under test
		IdAndVersionFilter filter = new IdAndVersionFilter(mainType, subTypes, scope);
		assertEquals(" R.OBJECT_TYPE = :mainType AND R.SUBTYPE IN (:subTypes)"
				+ " AND (((R.OBJECT_ID, R.OBJECT_VERSION) IN (:versionedRefs)) OR (R.OBJECT_ID IN (:nonVersionedRefs) AND R.OBJECT_VERSION = R.CURRENT_VERSION))", filter.getFilterSql());
		Map<String, Object> paramters = filter.getParameters();
		assertEquals(5, paramters.size());
		assertEquals(mainType.name(), paramters.get("mainType"));
		assertEquals(expectedSubTypes, paramters.get("subTypes"));
		assertEquals(objectIds, paramters.get("objectIds"));
		assertEqualsParams(expectedVersionedScope, (List<Long[]>) paramters.get("versionedRefs"));
		assertEquals(expectedNonVersionedScope, paramters.get("nonVersionedRefs"));
	}

	@Test
	public void testFilterBuilder() {
		Set<IdAndVersion> scope = Set.of(KeyFactory.idAndVersion("1", 1L), KeyFactory.idAndVersion("1", 2L));
		List<Long[]> expectedScope = scope.stream().map(id -> new Long[] {id.getId(), id.getVersion().get()}).collect(Collectors.toList());
		Set<Long> objectIds = Set.of(1L);
		// call under test
		ViewFilter filter = new IdAndVersionFilter(mainType, subTypes, scope).newBuilder()
				.addExcludeAnnotationKeys(excludeKeys)
				.addLimitObjectids(limitObjectIds)
				.setExcludeDerivedKeys(excludeDerivedKeys)
				.build();
		assertEquals(
				" R.OBJECT_TYPE = :mainType AND R.SUBTYPE IN (:subTypes) AND R.OBJECT_ID IN (:limitObjectIds) "
						+ "AND A.ANNO_KEY NOT IN (:excludeKeys) AND A.IS_DERIVED = FALSE AND (((R.OBJECT_ID, R.OBJECT_VERSION) IN (:versionedRefs)))",
				filter.getFilterSql());
		assertEquals(
				" R.OBJECT_TYPE = :mainType AND R.SUBTYPE IN (:subTypes) AND R.OBJECT_ID IN (:limitObjectIds)"
				+ " AND A.ANNO_KEY NOT IN (:excludeKeys) AND A.IS_DERIVED = FALSE AND R.OBJECT_ID IN (:objectIds)",
				filter.getObjectIdFilterSql());
		Map<String, Object> paramters = filter.getParameters();
		assertEquals(6, paramters.size());
		assertEquals(mainType.name(), paramters.get("mainType"));
		assertEquals(expectedSubTypes, paramters.get("subTypes"));
		assertEquals(limitObjectIds, paramters.get("limitObjectIds"));
		assertEquals(excludeKeys, paramters.get("excludeKeys"));
		assertEqualsParams(expectedScope, (List<Long[]>) paramters.get("versionedRefs"));
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
		Set<IdAndVersion> scope = Set.of(KeyFactory.idAndVersion("1", 1L), KeyFactory.idAndVersion("1", 2L));
		IdAndVersionFilter filter = new IdAndVersionFilter(mainType, subTypes, limitObjectIds, excludeKeys, scope, excludeDerivedKeys);
		ViewFilter clone = filter.newBuilder().build();
		assertEquals(filter, clone);
	}
	
	@Test
	public void testGetLimitedObjectIds() {
		Set<IdAndVersion> scope = Set.of(KeyFactory.idAndVersion("1", 1L), KeyFactory.idAndVersion("1", 2L));
		IdAndVersionFilter filter = new IdAndVersionFilter(mainType, subTypes, limitObjectIds, excludeKeys, scope, excludeDerivedKeys);
		Optional<Set<Long>> optional = filter.getLimitObjectIds();
		assertNotNull(optional);
		assertTrue(optional.isPresent());
		assertEquals(limitObjectIds, optional.get());
	}
	
	@Test
	public void testGetLimitedObjectIdswithNull() {
		Set<IdAndVersion> scope = Set.of(KeyFactory.idAndVersion("1", 1L), KeyFactory.idAndVersion("1", 2L));
		limitObjectIds = null;
		IdAndVersionFilter filter = new IdAndVersionFilter(mainType, subTypes, limitObjectIds, excludeKeys, scope, excludeDerivedKeys);
		Optional<Set<Long>> optional = filter.getLimitObjectIds();
		assertNotNull(optional);
		assertFalse(optional.isPresent());
	}
	
	@Test
	public void testGetSubViews() {
		Set<IdAndVersion> scope = Set.of(KeyFactory.idAndVersion("1", 1L), KeyFactory.idAndVersion("1", 2L));
		IdAndVersionFilter filter = new IdAndVersionFilter(mainType, subTypes, limitObjectIds, excludeKeys, scope, excludeDerivedKeys);
		// call under test
		Optional<List<ChangeMessage>> results = filter.getSubViews();
		Optional<List<ChangeMessage>> expected = Optional.empty();
		assertEquals(expected, results);
	}
}
