package org.sagebionetworks.tool.migration.v5.utils;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeList;

public class ToolMigrationUtilsTest {
	
	List<MigrationTypeCount> srcTypeCounts;
	List<MigrationTypeCount> destTypeCounts;
	MigrationTypeList typesToMigrate;

	@Before
	public void setUp() throws Exception {
		srcTypeCounts = generateMigrationTypeCounts();
		destTypeCounts = generateMigrationTypeCounts();
		typesToMigrate = new MigrationTypeList();
		typesToMigrate.setList(generateTypesToMigrate());
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGenerateMigrationTypeCounts() {
		assertNotNull(srcTypeCounts);
		assertEquals(MigrationType.values().length, srcTypeCounts.size());
	}
	
	@Test
	public void testGenerateTypesToMigrate() {
		assertNotNull(typesToMigrate.getList());
		assertEquals(5, typesToMigrate.getList().size());
	}
	
	@Test
	public void testBuildTypesToMigrateMetadataInvalidArgs() throws Exception {
		try {
			ToolMigrationUtils.buildTypeToMigrateMetadata(null, destTypeCounts, typesToMigrate.getList());
		} catch (IllegalArgumentException e) {
			
		} catch (Exception e) {
			throw(e);
		}
		try {
			ToolMigrationUtils.buildTypeToMigrateMetadata(srcTypeCounts, null, typesToMigrate.getList());
		} catch (IllegalArgumentException e) {
			
		} catch (Exception e) {
			throw(e);
		}
		try {
			ToolMigrationUtils.buildTypeToMigrateMetadata(srcTypeCounts, destTypeCounts, null);
		} catch (IllegalArgumentException e) {
			
		} catch (Exception e) {
			throw(e);
		}
	}
	
	@Test
	public void testBuildTypesToMigrateMetadata() {
		List<TypeToMigrateMetadata> expectedMetadata = new LinkedList<TypeToMigrateMetadata>();
		int idx = 0;
		for (MigrationType t: typesToMigrate.getList()) {
			TypeToMigrateMetadata d = new TypeToMigrateMetadata();
			d.setDestCount(destTypeCounts.get(idx).getCount());
			d.setDestMaxId(destTypeCounts.get(idx).getMaxid());
			d.setDestMinId(destTypeCounts.get(idx).getMinid());
			d.setSrcCount(srcTypeCounts.get(idx).getCount());
			d.setSrcMaxId(srcTypeCounts.get(idx).getMaxid());
			d.setSrcMinId(srcTypeCounts.get(idx).getMinid());
			d.setType(t);
			expectedMetadata.add(d);
			idx++;
		}
		List<TypeToMigrateMetadata> l = ToolMigrationUtils.buildTypeToMigrateMetadata(srcTypeCounts, destTypeCounts, typesToMigrate.getList());
		assertEquals(expectedMetadata, l);
	}
	
	@Test
	public void testBuildTypesToMigrateMetadataNullValue() {
		srcTypeCounts.get(0).setCount(0L);
		srcTypeCounts.get(0).setMaxid(null);
		srcTypeCounts.get(0).setMinid(null);
		destTypeCounts.get(0).setCount(0L);
		destTypeCounts.get(0).setMaxid(null);
		destTypeCounts.get(0).setMinid(null);
		List<TypeToMigrateMetadata> expectedMetadata = new LinkedList<TypeToMigrateMetadata>();
		int idx = 0;
		for (MigrationType t: typesToMigrate.getList()) {
			TypeToMigrateMetadata d = new TypeToMigrateMetadata();
			if (idx == 0) {
				d.setDestCount(0L);
				d.setDestMaxId(null);
				d.setDestMinId(null);
				d.setSrcCount(0L);
				d.setSrcMaxId(null);
				d.setSrcMinId(null);
			} else {
				d.setDestMaxId(destTypeCounts.get(idx).getMaxid());
				d.setDestMinId(destTypeCounts.get(idx).getMinid());
				d.setDestCount(destTypeCounts.get(idx).getCount());
				d.setSrcCount(srcTypeCounts.get(idx).getCount());
				d.setSrcMaxId(srcTypeCounts.get(idx).getMaxid());
				d.setSrcMinId(srcTypeCounts.get(idx).getMinid());
			}
			d.setType(t);
			expectedMetadata.add(d);
			idx++;
		}
		List<TypeToMigrateMetadata> l = ToolMigrationUtils.buildTypeToMigrateMetadata(srcTypeCounts, destTypeCounts, typesToMigrate.getList());
		assertEquals(expectedMetadata, l);
	}
	
	@Test
	public void testMigrationOutcomeGetDelta() {
		MigrationTypeCountDiff outcome = new MigrationTypeCountDiff(MigrationType.ACCESS_APPROVAL, null, null);
		Long delta = outcome.getDelta();
		assertNull(delta);
		outcome.setSourceCount(10L);
		delta = outcome.getDelta();
		assertNull(delta);
		outcome.setSourceCount(null);
		outcome.setDestinationCount(20L);
		delta = outcome.getDelta();
		assertNull(delta);
		outcome.setSourceCount(10L);
		delta = outcome.getDelta();
		assertNotNull(delta);
		assertEquals(20L-10L, delta.longValue());
	}
	
	@Test
	public void testGetMigrationOutcomesAllSourcesExist() {
		List<MigrationTypeCount> srcCounts = generateMigrationTypeCounts();
		List<MigrationTypeCount> destCounts = generateMigrationTypeCounts();
		
		List<MigrationTypeCountDiff> expectedOutcomes = new LinkedList<MigrationTypeCountDiff>();
		int idx = 0;
		for (MigrationTypeCount tc: destCounts) {
			MigrationTypeCountDiff outcome = new MigrationTypeCountDiff(tc.getType(), srcCounts.get(idx++).getCount(), tc.getCount());
			expectedOutcomes.add(outcome);
		}
		
		List<MigrationTypeCountDiff> outcomes = ToolMigrationUtils.getMigrationTypeCountDiffs(srcCounts, destCounts);
		
		assertNotNull(outcomes);
		assertEquals(expectedOutcomes.size(), outcomes.size());
	}
	
	private List<MigrationTypeCount> generateMigrationTypeCounts() {
		Random r = new Random();
		List<MigrationTypeCount> l = new LinkedList<MigrationTypeCount>();
		for (MigrationType t: MigrationType.values()) {
			MigrationTypeCount tc = new MigrationTypeCount();
			tc.setType(t);
			tc.setCount(Math.abs(r.nextLong()));
			tc.setMinid(Math.abs(r.nextLong()));
			tc.setMaxid(Math.abs(r.nextLong()));
			l.add(tc);
		}
		
		return l;
	}
	
	private List<MigrationType> generateTypesToMigrate() {
		List<MigrationType> l = new LinkedList<MigrationType>();
		// Only migrate first 5 types
		int i = 0;
		for (MigrationType t: MigrationType.values()) {
			if (i++ > 4) {
				break;
			}
			l.add(t);
		}
		return l;
	}

}
