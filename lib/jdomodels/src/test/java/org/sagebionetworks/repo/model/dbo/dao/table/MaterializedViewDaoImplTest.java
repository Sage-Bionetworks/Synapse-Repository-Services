package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.EntityType;
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
public class MaterializedViewDaoImplTest {
	
	@Autowired
	private NodeDAO nodeDao;

	@Autowired
	private NodeDaoObjectHelper nodeHelper;
	
	@Autowired
	private MaterializedViewDao dao;
	
	private IdAndVersion viewId;
	
	@BeforeEach
	public void before() {
		nodeDao.truncateAll();
		
		String nodeId = nodeHelper.create(node-> {
			node.setNodeType(EntityType.materializedview);
		}).getId();
		
		viewId = KeyFactory.idAndVersion(nodeId, null);
	}
	
	@AfterEach
	public void after() {
		nodeDao.truncateAll();
	}

	@Test
	public void testAddAndGetSourceTablesIds() {
		
		Set<IdAndVersion> sourceTables = ImmutableSet.of(
			IdAndVersion.parse("syn123"), IdAndVersion.parse("456"), IdAndVersion.parse("syn123.2")
		);
		
		// Call under test
		dao.addSourceTablesIds(viewId, sourceTables);
		
		assertEquals(sourceTables, dao.getSourceTablesIds(viewId));
	}
	
	@Test
	public void testAddAndGetSourceTableIdsWithVersion() {
		
		IdAndVersion viewIdWithoutVersion = viewId;
		
		Set<IdAndVersion> sourceTablesNoVersion = ImmutableSet.of(
			IdAndVersion.parse("syn123"), IdAndVersion.parse("789")	
		);
		
		dao.addSourceTablesIds(viewIdWithoutVersion, sourceTablesNoVersion);
		
		viewId = IdAndVersion.newBuilder().setId(viewId.getId()).setVersion(2L).build();
		
		Set<IdAndVersion> sourceTables = ImmutableSet.of(
			IdAndVersion.parse("syn123"), IdAndVersion.parse("456"), IdAndVersion.parse("syn123.2")
		);
		
		// Call under test
		dao.addSourceTablesIds(viewId, sourceTables);
		
		assertEquals(sourceTables, dao.getSourceTablesIds(viewId));
		assertEquals(sourceTablesNoVersion, dao.getSourceTablesIds(viewIdWithoutVersion));
	}
	
	@Test
	public void testAddAndGetSourceTableIdsEmpty() {
		
		Set<IdAndVersion> sourceTables = Collections.emptySet();
		
		// Call under test
		assertEquals(sourceTables, dao.getSourceTablesIds(viewId));
		
		// Call under test
		dao.addSourceTablesIds(viewId, sourceTables);
		
		assertEquals(sourceTables, dao.getSourceTablesIds(viewId));
	}
	
	@Test
	public void testAddAndGetSourceTableIdsWithExisting() {
		
		Set<IdAndVersion> sourceTables = ImmutableSet.of(
			IdAndVersion.parse("syn123"), IdAndVersion.parse("456")
		);
			
		dao.addSourceTablesIds(viewId, sourceTables);
		
		Set<IdAndVersion> additionalSourceTables = ImmutableSet.of(
			IdAndVersion.parse("syn123"), IdAndVersion.parse("syn123.2")
		);
		
		// Call under test
		dao.addSourceTablesIds(viewId, additionalSourceTables);
		
		Set<IdAndVersion> expected = ImmutableSet.of(
			IdAndVersion.parse("syn123"), IdAndVersion.parse("syn123.2"), IdAndVersion.parse("456")
		);
		
		assertEquals(expected, dao.getSourceTablesIds(viewId));
	}
	
	@Test
	public void testDeleteSourceTableIds() {
		
		Set<IdAndVersion> sourceTables = ImmutableSet.of(
			IdAndVersion.parse("syn123"), IdAndVersion.parse("456")
		);

		dao.addSourceTablesIds(viewId, sourceTables);

		Set<IdAndVersion> expected = Collections.emptySet();
		
		// Call under test
		dao.deleteSourceTablesIds(viewId, sourceTables);
		
		assertEquals(expected, dao.getSourceTablesIds(viewId));
		
	}
	
	@Test
	public void testDeleteSourceTableIdsPartial() {
		
		Set<IdAndVersion> sourceTables = ImmutableSet.of(
			IdAndVersion.parse("syn123"), IdAndVersion.parse("456")
		);

		dao.addSourceTablesIds(viewId, sourceTables);

		Set<IdAndVersion> expected = ImmutableSet.of(IdAndVersion.parse("456"));
		
		// Call under test
		dao.deleteSourceTablesIds(viewId, ImmutableSet.of(IdAndVersion.parse("123")));
		
		assertEquals(expected, dao.getSourceTablesIds(viewId));
		
	}
	
	@Test
	public void testDeleteSourceTableIdsWithVersions() {
		
		IdAndVersion viewIdWithoutVersion = viewId;
		
		Set<IdAndVersion> sourceTablesNoVersion = ImmutableSet.of(
			IdAndVersion.parse("syn123"), IdAndVersion.parse("456"), IdAndVersion.parse("456.1"), IdAndVersion.parse("123.2")
		);
		
		dao.addSourceTablesIds(viewIdWithoutVersion, sourceTablesNoVersion);
		
		viewId = IdAndVersion.newBuilder().setId(viewId.getId()).setVersion(5L).build();
		
		Set<IdAndVersion> sourceTables = ImmutableSet.of(
			IdAndVersion.parse("syn123"), IdAndVersion.parse("456"), IdAndVersion.parse("456.1"), IdAndVersion.parse("123.2")
		);

		dao.addSourceTablesIds(viewId, sourceTables);

		Set<IdAndVersion> expected = ImmutableSet.of(IdAndVersion.parse("syn123"), IdAndVersion.parse("456.1"));
		
		// Call under test
		dao.deleteSourceTablesIds(viewId, ImmutableSet.of(IdAndVersion.parse("syn123.2"), IdAndVersion.parse("456")));
		
		assertEquals(expected, dao.getSourceTablesIds(viewId));
		assertEquals(sourceTablesNoVersion, dao.getSourceTablesIds(viewIdWithoutVersion));
		
	}
	
	@Test
	public void testDeleteSourceTableIdsWithEmptySet() {
		
		Set<IdAndVersion> sourceTables = ImmutableSet.of(
			IdAndVersion.parse("syn123"), IdAndVersion.parse("456")
		);

		dao.addSourceTablesIds(viewId, sourceTables);

		Set<IdAndVersion> expected = sourceTables;
		
		// Call under test
		dao.deleteSourceTablesIds(viewId, Collections.emptySet());
		
		assertEquals(expected, dao.getSourceTablesIds(viewId));
		
	}
	
	@Test
	public void testDeleteSourceTableIdsWithNoData() {
		
		Set<IdAndVersion> sourceTables = ImmutableSet.of(
			IdAndVersion.parse("syn123"), IdAndVersion.parse("456")
		);

		Set<IdAndVersion> expected = Collections.emptySet();
		
		// Call under test
		dao.deleteSourceTablesIds(viewId, sourceTables);
		
		assertEquals(expected, dao.getSourceTablesIds(viewId));
		
	}
	
	@Test
	public void testGetMaterializedViewIds() {
		
		IdAndVersion sourceTableId = IdAndVersion.parse("syn123");
		
		Set<IdAndVersion> sourceTables = ImmutableSet.of(
			sourceTableId, IdAndVersion.parse("456")
		);

		dao.addSourceTablesIds(viewId, sourceTables);
		
		Set<IdAndVersion> expected = ImmutableSet.of(viewId);
		
		// Call under test
		Set<IdAndVersion> result = dao.getMaterializedViewIds(sourceTableId);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetMaterializedViewIdsWithVersion() {
		
		IdAndVersion sourceTableId = IdAndVersion.parse("syn123.2");
		
		Set<IdAndVersion> sourceTables = ImmutableSet.of(
			sourceTableId, IdAndVersion.parse("456")
		);

		dao.addSourceTablesIds(viewId, sourceTables);
				
		Set<IdAndVersion> expected = ImmutableSet.of(viewId);
		
		// Call under test
		Set<IdAndVersion> result = dao.getMaterializedViewIds(sourceTableId);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetMaterializedViewIdsWithOverlappingIds() {
		
		IdAndVersion sourceTableId = IdAndVersion.parse("syn123");
		
		Set<IdAndVersion> sourceTables = ImmutableSet.of(
			sourceTableId, IdAndVersion.parse("456")
		);

		dao.addSourceTablesIds(viewId, sourceTables);
		
		// Another version of the view that uses the same source table
		IdAndVersion viewWithVersion =  IdAndVersion.newBuilder().setId(viewId.getId()).setVersion(5L).build();
		
		dao.addSourceTablesIds(viewWithVersion, ImmutableSet.of(sourceTableId));
				
		Set<IdAndVersion> expected = ImmutableSet.of(viewId, viewWithVersion);
		
		// Call under test
		Set<IdAndVersion> result = dao.getMaterializedViewIds(sourceTableId);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetMaterializedViewIdsWithNonOverlappingIds() {
		
		IdAndVersion sourceTableId = IdAndVersion.parse("syn123");
		
		Set<IdAndVersion> sourceTables = ImmutableSet.of(
			sourceTableId, IdAndVersion.parse("456")
		);

		dao.addSourceTablesIds(viewId, sourceTables);
		
		// Another version of the view that does not use the source table
		IdAndVersion viewWithVersion =  IdAndVersion.newBuilder().setId(viewId.getId()).setVersion(5L).build();
		
		dao.addSourceTablesIds(viewWithVersion, ImmutableSet.of(IdAndVersion.parse("syn123.2"), IdAndVersion.parse("456")));
				
		Set<IdAndVersion> expected = ImmutableSet.of(viewId);
		
		// Call under test
		Set<IdAndVersion> result = dao.getMaterializedViewIds(sourceTableId);
		
		assertEquals(expected, result);
	}	
	
	@Test
	public void testGetMaterializedViewIdsWithNoData() {
		
		IdAndVersion sourceTableId = IdAndVersion.parse("syn123");
		
		Set<IdAndVersion> expected = Collections.emptySet();
		
		// Call under test
		Set<IdAndVersion> result = dao.getMaterializedViewIds(sourceTableId);
		
		assertEquals(expected, result);
	}
	
}
