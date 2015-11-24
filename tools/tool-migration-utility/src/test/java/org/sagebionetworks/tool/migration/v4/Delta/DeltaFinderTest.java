package org.sagebionetworks.tool.migration.v4.Delta;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.tool.migration.v4.utils.TypeToMigrateMetadata;

public class DeltaFinderTest {
	
	SynapseAdminClient mockSrcClient;
	SynapseAdminClient mockDestClient;

	@Before
	public void setUp() throws Exception {
		mockSrcClient = Mockito.mock(SynapseAdminClient.class);
		mockDestClient = Mockito.mock(SynapseAdminClient.class); 
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testAllOverlap() {
		TypeToMigrateMetadata meta = new TypeToMigrateMetadata();
		meta.setType(MigrationType.FILE_HANDLE);
		meta.setSrcCount(1345L);
		meta.setSrcMinId(1000L);
		meta.setSrcMaxId(2344L);
		meta.setDestCount(1345L);
		meta.setDestMinId(1000L);
		meta.setDestMaxId(2344L);
		DeltaFinder finder = new DeltaFinder(meta, mockSrcClient, mockDestClient);
		DeltaRanges ranges = finder.findDeltaRanges();
		assertEquals(MigrationType.FILE_HANDLE, ranges.getMigrationType());
		assertNotNull(ranges.getInsRanges());
		assertEquals(0, ranges.getInsRanges().size());
		assertNotNull(ranges.getUpdRanges());
		assertEquals(0, ranges.getUpdRanges().size());
		assertEquals(0, ranges.getDelRanges().size());
		assertNotNull(ranges.getDelRanges());
	}
	
	@Test
	public void testInsMinSrc() {
		TypeToMigrateMetadata meta = new TypeToMigrateMetadata();
		meta.setType(MigrationType.FILE_HANDLE);
		meta.setSrcCount(1100L);
		meta.setSrcMinId(900L);
		meta.setSrcMaxId(2344L);
		meta.setDestCount(1000L);
		meta.setDestMinId(1000L);
		meta.setDestMaxId(2344L);
		DeltaFinder finder = new DeltaFinder(meta, mockSrcClient, mockDestClient);
		DeltaRanges ranges = finder.findDeltaRanges();
		assertEquals(MigrationType.FILE_HANDLE, ranges.getMigrationType());
		assertNotNull(ranges.getInsRanges());
		assertEquals(1, ranges.getInsRanges().size());
		assertEquals(900, ranges.getInsRanges().get(0).getMinId());
		assertEquals(999, ranges.getInsRanges().get(0).getMaxId());
		assertNotNull(ranges.getUpdRanges());
		assertEquals(0, ranges.getUpdRanges().size());
		assertEquals(0, ranges.getDelRanges().size());
		assertNotNull(ranges.getDelRanges());
	}

}
