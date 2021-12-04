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
	public void testAddAndGetSourceTables() {
		
		Set<IdAndVersion> sourceTables = ImmutableSet.of(
			IdAndVersion.parse("syn123"), IdAndVersion.parse("456"), IdAndVersion.parse("syn123.2")
		);
		
		// Call under test
		dao.addSourceTables(viewId, sourceTables);
		
		assertEquals(sourceTables, dao.getSourceTables(viewId));
	}
	
	@Test
	public void testAddAndGetSourceTablesWithVersion() {
		
		viewId = IdAndVersion.newBuilder().setId(viewId.getId()).setVersion(2L).build();
		
		Set<IdAndVersion> sourceTables = ImmutableSet.of(
			IdAndVersion.parse("syn123"), IdAndVersion.parse("456"), IdAndVersion.parse("syn123.2")
		);
		
		// Call under test
		dao.addSourceTables(viewId, sourceTables);
		
		assertEquals(sourceTables, dao.getSourceTables(viewId));
	}
	
	@Test
	public void testAddAndGetSourceTablesEmpty() {
		
		Set<IdAndVersion> sourceTables = Collections.emptySet();
		
		// Call under test
		assertEquals(sourceTables, dao.getSourceTables(viewId));
		
		// Call under test
		dao.addSourceTables(viewId, sourceTables);
		
		assertEquals(sourceTables, dao.getSourceTables(viewId));
	}
	
	@Test
	public void testAddAndGetSourceTablesWithExisting() {
		
		Set<IdAndVersion> sourceTables = ImmutableSet.of(
			IdAndVersion.parse("syn123"), IdAndVersion.parse("456")
		);
			
		dao.addSourceTables(viewId, sourceTables);
		
		Set<IdAndVersion> additionalSourceTables = ImmutableSet.of(
			IdAndVersion.parse("syn123"), IdAndVersion.parse("syn123.2")
		);
		
		// Call under test
		dao.addSourceTables(viewId, additionalSourceTables);
		
		Set<IdAndVersion> expected = ImmutableSet.of(
			IdAndVersion.parse("syn123"), IdAndVersion.parse("syn123.2"), IdAndVersion.parse("456")
		);
		
		assertEquals(expected, dao.getSourceTables(viewId));
	}
	
}
