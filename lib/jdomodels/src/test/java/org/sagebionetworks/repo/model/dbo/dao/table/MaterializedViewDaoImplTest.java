package org.sagebionetworks.repo.model.dbo.dao.table;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

		long limit = 10;
		long offset = 0;
		
		IdAndVersion sourceTableId = IdAndVersion.parse("syn123");
		
		Set<IdAndVersion> sourceTables = ImmutableSet.of(
			sourceTableId, IdAndVersion.parse("456")
		);

		dao.addSourceTablesIds(viewId, sourceTables);
		
		List<IdAndVersion> expected = Arrays.asList(viewId);
		
		// Call under test
		List<IdAndVersion> result = dao.getMaterializedViewIdsPage(sourceTableId, limit, offset);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetMaterializedViewIdsMultiplePages() {
		
		IdAndVersion sourceTableId = IdAndVersion.parse("syn123");

		// Multiple versions that reference the same source table
		dao.addSourceTablesIds(viewId, ImmutableSet.of(
			sourceTableId, IdAndVersion.parse("456"), IdAndVersion.parse("789")
		));
		
		IdAndVersion viewIdV2 = IdAndVersion.parse(viewId.getId() + ".2"); 
		
		dao.addSourceTablesIds(viewIdV2, ImmutableSet.of(
			sourceTableId, IdAndVersion.parse("654"), IdAndVersion.parse("345")
		));
		
		IdAndVersion viewIdV3 = IdAndVersion.parse(viewId.getId() + ".3");
		
		dao.addSourceTablesIds(viewIdV3, ImmutableSet.of(
			sourceTableId, IdAndVersion.parse("456"), IdAndVersion.parse("345")
		));

		long limit = 2;
		long offset = 0;
		
		List<IdAndVersion> expectedfirstPage = Arrays.asList(viewId, viewIdV2);
		
		// Call under test
		List<IdAndVersion> firstPage = dao.getMaterializedViewIdsPage(sourceTableId, limit, offset);
		
		assertEquals(expectedfirstPage, firstPage);
		
		offset = 2;
		
		List<IdAndVersion> expectedSecondPage = Arrays.asList(viewIdV3);
		
		// Call under test
		List<IdAndVersion> secondPage = dao.getMaterializedViewIdsPage(sourceTableId, limit, offset);
		
		assertEquals(expectedSecondPage, secondPage);
	}
	
	@Test
	public void testGetMaterializedViewIdsWithVersion() {
		
		long limit = 10;
		long offset = 0;
		
		IdAndVersion sourceTableId = IdAndVersion.parse("syn123.2");
		
		Set<IdAndVersion> sourceTables = ImmutableSet.of(
			sourceTableId, IdAndVersion.parse("456")
		);

		dao.addSourceTablesIds(viewId, sourceTables);
				
		List<IdAndVersion> expected = Arrays.asList(viewId);
		
		// Call under test
		List<IdAndVersion> result = dao.getMaterializedViewIdsPage(sourceTableId, limit, offset);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetMaterializedViewIdsWithOverlappingIds() {
		
		long limit = 10;
		long offset = 0;
		
		IdAndVersion sourceTableId = IdAndVersion.parse("syn123");
		
		Set<IdAndVersion> sourceTables = ImmutableSet.of(
			sourceTableId, IdAndVersion.parse("456")
		);

		dao.addSourceTablesIds(viewId, sourceTables);
		
		// Another version of the view that uses the same source table
		IdAndVersion viewWithVersion =  IdAndVersion.newBuilder().setId(viewId.getId()).setVersion(5L).build();
		
		dao.addSourceTablesIds(viewWithVersion, ImmutableSet.of(sourceTableId));
				
		List<IdAndVersion> expected = Arrays.asList(viewId, viewWithVersion);
		
		// Call under test
		List<IdAndVersion> result = dao.getMaterializedViewIdsPage(sourceTableId, limit, offset);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testGetMaterializedViewIdsWithNonOverlappingIds() {
		
		long limit = 10;
		long offset = 0;
		
		IdAndVersion sourceTableId = IdAndVersion.parse("syn123");
		
		Set<IdAndVersion> sourceTables = ImmutableSet.of(
			sourceTableId, IdAndVersion.parse("456")
		);

		dao.addSourceTablesIds(viewId, sourceTables);
		
		// Another version of the view that does not use the source table
		IdAndVersion viewWithVersion =  IdAndVersion.newBuilder().setId(viewId.getId()).setVersion(5L).build();
		
		dao.addSourceTablesIds(viewWithVersion, ImmutableSet.of(IdAndVersion.parse("syn123.2"), IdAndVersion.parse("456")));
				
		List<IdAndVersion> expected = Arrays.asList(viewId);
		
		// Call under test
		List<IdAndVersion> result = dao.getMaterializedViewIdsPage(sourceTableId, limit, offset);
		
		assertEquals(expected, result);
	}	
	
	@Test
	public void testGetMaterializedViewIdsWithNoData() {
		
		long limit = 10;
		long offset = 0;
		
		IdAndVersion sourceTableId = IdAndVersion.parse("syn123");
		
		List<IdAndVersion> expected = Collections.emptyList();
		
		// Call under test
		List<IdAndVersion> result = dao.getMaterializedViewIdsPage(sourceTableId, limit, offset);
		
		assertEquals(expected, result);
	}
	
}
