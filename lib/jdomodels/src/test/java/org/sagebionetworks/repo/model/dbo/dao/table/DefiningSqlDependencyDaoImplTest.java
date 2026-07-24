package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.helper.NodeDaoObjectHelper;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.collect.ImmutableSet;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DefiningSqlDependencyDaoImplTest {

	private static final String MV_TYPE = ObjectType.MATERIALIZED_VIEW.name();
	private static final String SEARCH_TYPE = ObjectType.SEARCH_INDEX.name();

	@Autowired
	private NodeDAO nodeDao;

	@Autowired
	private NodeDaoObjectHelper nodeHelper;

	@Autowired
	private DefiningSqlDependencyDaoImpl dao;

	private IdAndVersion viewId;

	@BeforeEach
	public void before() {
		nodeDao.truncateAll();

		String nodeId = nodeHelper.create(node -> {
			node.setNodeType(EntityType.materializedview);
		}).getId();

		viewId = KeyFactory.idAndVersion(nodeId, null);
	}

	@AfterEach
	public void after() {
		nodeDao.truncateAll();
	}

	@Test
	public void testAddAndGetSourceTables() {

		Set<IdAndVersion> sourceTables = ImmutableSet.of(IdAndVersion.parse("syn123"), IdAndVersion.parse("456"),
				IdAndVersion.parse("syn123.2"));

		// Call under test
		dao.addSourceTables(viewId, MV_TYPE, sourceTables);

		assertEquals(sourceTables, dao.getSourceTables(viewId));
	}

	@Test
	public void testAddAndGetSourceTablesWithVersion() {

		IdAndVersion viewIdWithoutVersion = viewId;

		Set<IdAndVersion> sourceTablesNoVersion = ImmutableSet.of(IdAndVersion.parse("syn123"),
				IdAndVersion.parse("789"));

		dao.addSourceTables(viewIdWithoutVersion, MV_TYPE, sourceTablesNoVersion);

		viewId = IdAndVersion.newBuilder().setId(viewId.getId()).setVersion(2L).build();

		Set<IdAndVersion> sourceTables = ImmutableSet.of(IdAndVersion.parse("syn123"), IdAndVersion.parse("456"),
				IdAndVersion.parse("syn123.2"));

		// Call under test
		dao.addSourceTables(viewId, MV_TYPE, sourceTables);

		assertEquals(sourceTables, dao.getSourceTables(viewId));
		assertEquals(sourceTablesNoVersion, dao.getSourceTables(viewIdWithoutVersion));
	}

	@Test
	public void testAddAndGetSourceTablesEmpty() {

		Set<IdAndVersion> sourceTables = Collections.emptySet();

		// Call under test
		assertEquals(sourceTables, dao.getSourceTables(viewId));

		// Call under test
		dao.addSourceTables(viewId, MV_TYPE, sourceTables);

		assertEquals(sourceTables, dao.getSourceTables(viewId));
	}

	@Test
	public void testAddAndGetSourceTablesWithExisting() {

		Set<IdAndVersion> sourceTables = ImmutableSet.of(IdAndVersion.parse("syn123"), IdAndVersion.parse("456"));

		dao.addSourceTables(viewId, MV_TYPE, sourceTables);

		Set<IdAndVersion> additionalSourceTables = ImmutableSet.of(IdAndVersion.parse("syn123"),
				IdAndVersion.parse("syn123.2"));

		// Call under test
		dao.addSourceTables(viewId, MV_TYPE, additionalSourceTables);

		Set<IdAndVersion> expected = ImmutableSet.of(IdAndVersion.parse("syn123"), IdAndVersion.parse("syn123.2"),
				IdAndVersion.parse("456"));

		assertEquals(expected, dao.getSourceTables(viewId));
	}

	@Test
	public void testDeleteSourceTables() {

		Set<IdAndVersion> sourceTables = ImmutableSet.of(IdAndVersion.parse("syn123"), IdAndVersion.parse("456"));

		dao.addSourceTables(viewId, MV_TYPE, sourceTables);

		Set<IdAndVersion> expected = Collections.emptySet();

		// Call under test
		dao.deleteSourceTables(viewId, sourceTables);

		assertEquals(expected, dao.getSourceTables(viewId));

	}

	@Test
	public void testDeleteSourceTablesPartial() {

		Set<IdAndVersion> sourceTables = ImmutableSet.of(IdAndVersion.parse("syn123"), IdAndVersion.parse("456"));

		dao.addSourceTables(viewId, MV_TYPE, sourceTables);

		Set<IdAndVersion> expected = ImmutableSet.of(IdAndVersion.parse("456"));

		// Call under test
		dao.deleteSourceTables(viewId, ImmutableSet.of(IdAndVersion.parse("123")));

		assertEquals(expected, dao.getSourceTables(viewId));

	}

	@Test
	public void testDeleteSourceTablesWithVersions() {

		IdAndVersion viewIdWithoutVersion = viewId;

		Set<IdAndVersion> sourceTablesNoVersion = ImmutableSet.of(IdAndVersion.parse("syn123"),
				IdAndVersion.parse("456"), IdAndVersion.parse("456.1"), IdAndVersion.parse("123.2"));

		dao.addSourceTables(viewIdWithoutVersion, MV_TYPE, sourceTablesNoVersion);

		viewId = IdAndVersion.newBuilder().setId(viewId.getId()).setVersion(5L).build();

		Set<IdAndVersion> sourceTables = ImmutableSet.of(IdAndVersion.parse("syn123"), IdAndVersion.parse("456"),
				IdAndVersion.parse("456.1"), IdAndVersion.parse("123.2"));

		dao.addSourceTables(viewId, MV_TYPE, sourceTables);

		Set<IdAndVersion> expected = ImmutableSet.of(IdAndVersion.parse("syn123"), IdAndVersion.parse("456.1"));

		// Call under test
		dao.deleteSourceTables(viewId, ImmutableSet.of(IdAndVersion.parse("syn123.2"), IdAndVersion.parse("456")));

		assertEquals(expected, dao.getSourceTables(viewId));
		assertEquals(sourceTablesNoVersion, dao.getSourceTables(viewIdWithoutVersion));

	}

	@Test
	public void testDeleteSourceTablesWithEmptySet() {

		Set<IdAndVersion> sourceTables = ImmutableSet.of(IdAndVersion.parse("syn123"), IdAndVersion.parse("456"));

		dao.addSourceTables(viewId, MV_TYPE, sourceTables);

		Set<IdAndVersion> expected = sourceTables;

		// Call under test
		dao.deleteSourceTables(viewId, Collections.emptySet());

		assertEquals(expected, dao.getSourceTables(viewId));

	}

	@Test
	public void testDeleteSourceTablesWithNoData() {

		Set<IdAndVersion> sourceTables = ImmutableSet.of(IdAndVersion.parse("syn123"), IdAndVersion.parse("456"));

		Set<IdAndVersion> expected = Collections.emptySet();

		// Call under test
		dao.deleteSourceTables(viewId, sourceTables);

		assertEquals(expected, dao.getSourceTables(viewId));

	}

	@Test
	public void testDeleteObject() {

		Set<IdAndVersion> sourceTables = ImmutableSet.of(IdAndVersion.parse("syn123"), IdAndVersion.parse("456"));

		dao.addSourceTables(viewId, MV_TYPE, sourceTables);

		// Call under test
		dao.deleteObject(viewId);

		assertEquals(Collections.emptySet(), dao.getSourceTables(viewId));
	}

	@Test
	public void testSetAndGetSourceTable() {

		IdAndVersion source = IdAndVersion.parse("syn123.2");

		// Call under test
		dao.setSourceTable(viewId, SEARCH_TYPE, source);

		assertEquals(Optional.of(source), dao.getSourceTable(viewId));
		assertEquals(ImmutableSet.of(source), dao.getSourceTables(viewId));
	}

	@Test
	public void testSetSourceTableReplacesExisting() {

		dao.setSourceTable(viewId, SEARCH_TYPE, IdAndVersion.parse("syn123"));

		IdAndVersion replacement = IdAndVersion.parse("syn999.4");

		// Call under test
		dao.setSourceTable(viewId, SEARCH_TYPE, replacement);

		assertEquals(Optional.of(replacement), dao.getSourceTable(viewId));
		assertEquals(ImmutableSet.of(replacement), dao.getSourceTables(viewId));
	}

	@Test
	public void testGetSourceTableWithNoData() {

		// Call under test
		assertEquals(Optional.empty(), dao.getSourceTable(viewId));
	}

	@Test
	public void testGetDependentsPageReturnsAllTypes() {
		// The unfiltered fan-out lookup returns dependents of every type, each paired with its type.
		String searchNodeId = nodeHelper.create(node -> {
			node.setNodeType(EntityType.searchindex);
		}).getId();
		IdAndVersion searchIndexId = KeyFactory.idAndVersion(searchNodeId, null);

		IdAndVersion sourceTableId = IdAndVersion.parse("syn123");
		dao.addSourceTables(viewId, MV_TYPE, ImmutableSet.of(sourceTableId));
		dao.addSourceTables(searchIndexId, SEARCH_TYPE, ImmutableSet.of(sourceTableId));

		// Call under test
		List<DefiningSqlDependencyDao.DependentObject> result = dao.getDependentsPage(sourceTableId, 10, 0);

		// Ordered by OBJECT_TYPE then id: materializedview before searchindex.
		assertEquals(Arrays.asList(
				new DefiningSqlDependencyDao.DependentObject(viewId, MV_TYPE),
				new DefiningSqlDependencyDao.DependentObject(searchIndexId, SEARCH_TYPE)),
				result);
	}

	@Test
	public void testGetDependentsPageDiscriminatesBySourceVersion() {
		// A source table with a current (unversioned) index and a snapshot version 2. A distinct
		// dependent is registered against each. The fan-out for one source version must return only
		// the dependent registered against that exact version — never the other — because each
		// version builds and fires its TABLE_STATUS_EVENT independently.
		IdAndVersion currentSource = IdAndVersion.parse("syn123");
		IdAndVersion snapshotSource = IdAndVersion.parse("syn123.2");

		String currentDependentNodeId = nodeHelper.create(node -> node.setNodeType(EntityType.searchindex)).getId();
		IdAndVersion currentDependent = KeyFactory.idAndVersion(currentDependentNodeId, null);

		String snapshotDependentNodeId = nodeHelper.create(node -> node.setNodeType(EntityType.searchindex)).getId();
		IdAndVersion snapshotDependent = KeyFactory.idAndVersion(snapshotDependentNodeId, null);

		dao.addSourceTables(currentDependent, SEARCH_TYPE, ImmutableSet.of(currentSource));
		dao.addSourceTables(snapshotDependent, SEARCH_TYPE, ImmutableSet.of(snapshotSource));

		// Call under test — current-version source wakes only the current-version dependent.
		assertEquals(Arrays.asList(new DefiningSqlDependencyDao.DependentObject(currentDependent, SEARCH_TYPE)),
				dao.getDependentsPage(currentSource, 10, 0));
		// Call under test — snapshot source wakes only the snapshot dependent.
		assertEquals(Arrays.asList(new DefiningSqlDependencyDao.DependentObject(snapshotDependent, SEARCH_TYPE)),
				dao.getDependentsPage(snapshotSource, 10, 0));
	}

	@Test
	public void testGetDependentsPagePaginates() {
		IdAndVersion sourceTableId = IdAndVersion.parse("syn123");
		String searchNodeId = nodeHelper.create(node -> {
			node.setNodeType(EntityType.searchindex);
		}).getId();
		IdAndVersion searchIndexId = KeyFactory.idAndVersion(searchNodeId, null);

		dao.addSourceTables(viewId, MV_TYPE, ImmutableSet.of(sourceTableId));
		dao.addSourceTables(searchIndexId, SEARCH_TYPE, ImmutableSet.of(sourceTableId));

		// Call under test — first page (ordered by OBJECT_TYPE: materializedview first)
		assertEquals(Arrays.asList(new DefiningSqlDependencyDao.DependentObject(viewId, MV_TYPE)),
				dao.getDependentsPage(sourceTableId, 1, 0));
		// Call under test — second page
		assertEquals(Arrays.asList(new DefiningSqlDependencyDao.DependentObject(searchIndexId, SEARCH_TYPE)),
				dao.getDependentsPage(sourceTableId, 1, 1));
	}

}
