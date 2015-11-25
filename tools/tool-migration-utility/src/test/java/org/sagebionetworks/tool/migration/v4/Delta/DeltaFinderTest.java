package org.sagebionetworks.tool.migration.v4.Delta;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeChecksum;
import org.sagebionetworks.tool.migration.v4.utils.TypeToMigrateMetadata;

public class DeltaFinderTest {
	
	SynapseAdminClient mockSrcClient;
	SynapseAdminClient mockDestClient;
	Long batchSize;

	@Before
	public void setUp() throws Exception {
		mockSrcClient = Mockito.mock(SynapseAdminClient.class);
		mockDestClient = Mockito.mock(SynapseAdminClient.class);
		batchSize = 100L;
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testEmptySrc() throws Exception {
		TypeToMigrateMetadata meta = new TypeToMigrateMetadata();
		meta.setType(MigrationType.FILE_HANDLE);
		meta.setSrcCount(0L);
		meta.setSrcMinId(null);
		meta.setSrcMaxId(null);
		meta.setDestCount(1345L);
		meta.setDestMinId(1000L);
		meta.setDestMaxId(2344L);
		DeltaFinder finder = new DeltaFinder(meta, mockSrcClient, mockDestClient, batchSize);
		DeltaRanges ranges = finder.findDeltaRanges();
		assertEquals(MigrationType.FILE_HANDLE, ranges.getMigrationType());
		assertNotNull(ranges.getInsRanges());
		assertEquals(0, ranges.getInsRanges().size());
		assertNotNull(ranges.getUpdRanges());
		assertEquals(0, ranges.getUpdRanges().size());
		assertNotNull(ranges.getDelRanges());
		assertEquals(1, ranges.getDelRanges().size());
		assertEquals(1000, ranges.getDelRanges().get(0).getMinId());
		assertEquals(2344, ranges.getDelRanges().get(0).getMaxId());
		verify(mockSrcClient, never()).getChecksumForIdRange(any(MigrationType.class), anyLong(), anyLong());
		verify(mockDestClient, never()).getChecksumForIdRange(any(MigrationType.class), anyLong(), anyLong());
	}
	
	@Test
	public void testEmptyDest() throws Exception {
		TypeToMigrateMetadata meta = new TypeToMigrateMetadata();
		meta.setType(MigrationType.FILE_HANDLE);
		meta.setSrcCount(1345L);
		meta.setSrcMinId(2000L);
		meta.setSrcMaxId(3344L);
		meta.setDestCount(0L);
		meta.setDestMinId(null);
		meta.setDestMaxId(null);
		DeltaFinder finder = new DeltaFinder(meta, mockSrcClient, mockDestClient, batchSize);
		DeltaRanges ranges = finder.findDeltaRanges();
		assertEquals(MigrationType.FILE_HANDLE, ranges.getMigrationType());
		assertNotNull(ranges.getInsRanges());
		assertEquals(1, ranges.getInsRanges().size());
		assertEquals(2000, ranges.getInsRanges().get(0).getMinId());
		assertEquals(3344, ranges.getInsRanges().get(0).getMaxId());
		assertNotNull(ranges.getUpdRanges());
		assertEquals(0, ranges.getUpdRanges().size());
		assertNotNull(ranges.getDelRanges());
		assertEquals(0, ranges.getDelRanges().size());
		verify(mockSrcClient, never()).getChecksumForIdRange(any(MigrationType.class), anyLong(), anyLong());
		verify(mockDestClient, never()).getChecksumForIdRange(any(MigrationType.class), anyLong(), anyLong());
	}
	
	@Test
	public void testNoOverlap1() throws Exception {
		TypeToMigrateMetadata meta = new TypeToMigrateMetadata();
		meta.setType(MigrationType.FILE_HANDLE);
		meta.setSrcCount(500L);
		meta.setSrcMinId(100L);
		meta.setSrcMaxId(599L);
		meta.setDestCount(100L);
		meta.setDestMinId(1000L);
		meta.setDestMaxId(1099L);
		DeltaFinder finder = new DeltaFinder(meta, mockSrcClient, mockDestClient, batchSize);
		DeltaRanges ranges = finder.findDeltaRanges();
		assertEquals(MigrationType.FILE_HANDLE, ranges.getMigrationType());
		assertNotNull(ranges.getInsRanges());
		assertEquals(1, ranges.getInsRanges().size());
		assertEquals(100, ranges.getInsRanges().get(0).getMinId());
		assertEquals(599, ranges.getInsRanges().get(0).getMaxId());
		assertNotNull(ranges.getUpdRanges());
		assertEquals(0, ranges.getUpdRanges().size());
		assertEquals(1, ranges.getDelRanges().size());
		assertNotNull(ranges.getDelRanges());
		assertEquals(1000, ranges.getDelRanges().get(0).getMinId());
		assertEquals(1099, ranges.getDelRanges().get(0).getMaxId());
		verify(mockSrcClient, never()).getChecksumForIdRange(any(MigrationType.class), anyLong(), anyLong());
		verify(mockDestClient, never()).getChecksumForIdRange(any(MigrationType.class), anyLong(), anyLong());
	}
	
	@Test
	public void testNoOverlap2() throws Exception {
		TypeToMigrateMetadata meta = new TypeToMigrateMetadata();
		meta.setType(MigrationType.FILE_HANDLE);
		meta.setSrcCount(500L);
		meta.setSrcMinId(100L);
		meta.setSrcMaxId(599L);
		meta.setDestCount(10L);
		meta.setDestMinId(10L);
		meta.setDestMaxId(19L);
		DeltaFinder finder = new DeltaFinder(meta, mockSrcClient, mockDestClient, batchSize);
		DeltaRanges ranges = finder.findDeltaRanges();
		assertEquals(MigrationType.FILE_HANDLE, ranges.getMigrationType());
		assertNotNull(ranges.getInsRanges());
		assertEquals(1, ranges.getInsRanges().size());
		assertEquals(100, ranges.getInsRanges().get(0).getMinId());
		assertEquals(599, ranges.getInsRanges().get(0).getMaxId());
		assertNotNull(ranges.getUpdRanges());
		assertEquals(0, ranges.getUpdRanges().size());
		assertEquals(1, ranges.getDelRanges().size());
		assertNotNull(ranges.getDelRanges());
		assertEquals(10, ranges.getDelRanges().get(0).getMinId());
		assertEquals(19, ranges.getDelRanges().get(0).getMaxId());
		verify(mockSrcClient, never()).getChecksumForIdRange(any(MigrationType.class), anyLong(), anyLong());
		verify(mockDestClient, never()).getChecksumForIdRange(any(MigrationType.class), anyLong(), anyLong());
	}
	
	@Test
	public void testAllOverlap() throws Exception {
		TypeToMigrateMetadata meta = new TypeToMigrateMetadata();
		meta.setType(MigrationType.FILE_HANDLE);
		meta.setSrcCount(1345L);
		meta.setSrcMinId(1000L);
		meta.setSrcMaxId(2344L);
		meta.setDestCount(1345L);
		meta.setDestMinId(1000L);
		meta.setDestMaxId(2344L);
		MigrationTypeChecksum expectedChecksum = new MigrationTypeChecksum();
		expectedChecksum.setType(MigrationType.FILE_HANDLE);
		expectedChecksum.setMinid(1000L);
		expectedChecksum.setMaxid(2344L);
		expectedChecksum.setChecksum("CRC32");
		when(mockSrcClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq(1000L), eq(2344L))).thenReturn(expectedChecksum);
		when(mockDestClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq(1000L), eq(2344L))).thenReturn(expectedChecksum);
		DeltaFinder finder = new DeltaFinder(meta, mockSrcClient, mockDestClient, batchSize);
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
	public void testInsMinSrc() throws Exception {
		TypeToMigrateMetadata meta = new TypeToMigrateMetadata();
		meta.setType(MigrationType.FILE_HANDLE);
		meta.setSrcCount(1445L);
		meta.setSrcMinId(900L);
		meta.setSrcMaxId(2344L);
		meta.setDestCount(1345L);
		meta.setDestMinId(1000L);
		meta.setDestMaxId(2344L);
		MigrationTypeChecksum expectedChecksum = new MigrationTypeChecksum();
		expectedChecksum.setType(MigrationType.FILE_HANDLE);
		expectedChecksum.setMinid(1000L);
		expectedChecksum.setMaxid(2344L);
		expectedChecksum.setChecksum("CRC32");
		when(mockSrcClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq(1000L), eq(2344L))).thenReturn(expectedChecksum);
		when(mockDestClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq(1000L), eq(2344L))).thenReturn(expectedChecksum);
		DeltaFinder finder = new DeltaFinder(meta, mockSrcClient, mockDestClient, batchSize);
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
	
	@Test
	public void testInsMaxSrc() throws Exception {
		TypeToMigrateMetadata meta = new TypeToMigrateMetadata();
		meta.setType(MigrationType.FILE_HANDLE);
		meta.setSrcCount(1445L);
		meta.setSrcMinId(1000L);
		meta.setSrcMaxId(2444L);
		meta.setDestCount(1345L);
		meta.setDestMinId(1000L);
		meta.setDestMaxId(2344L);
		MigrationTypeChecksum expectedChecksum = new MigrationTypeChecksum();
		expectedChecksum.setType(MigrationType.FILE_HANDLE);
		expectedChecksum.setMinid(1000L);
		expectedChecksum.setMaxid(2344L);
		expectedChecksum.setChecksum("CRC32");
		when(mockSrcClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq(1000L), eq(2344L))).thenReturn(expectedChecksum);
		when(mockDestClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq(1000L), eq(2344L))).thenReturn(expectedChecksum);
		DeltaFinder finder = new DeltaFinder(meta, mockSrcClient, mockDestClient, batchSize);
		DeltaRanges ranges = finder.findDeltaRanges();
		assertEquals(MigrationType.FILE_HANDLE, ranges.getMigrationType());
		assertNotNull(ranges.getInsRanges());
		assertEquals(1, ranges.getInsRanges().size());
		assertEquals(2345, ranges.getInsRanges().get(0).getMinId());
		assertEquals(2444, ranges.getInsRanges().get(0).getMaxId());
		assertNotNull(ranges.getUpdRanges());
		assertEquals(0, ranges.getUpdRanges().size());
		assertEquals(0, ranges.getDelRanges().size());
		assertNotNull(ranges.getDelRanges());
	}
	
	@Test
	public void testDelMinDest() throws Exception {
		TypeToMigrateMetadata meta = new TypeToMigrateMetadata();
		meta.setType(MigrationType.FILE_HANDLE);
		meta.setSrcCount(1345L);
		meta.setSrcMinId(1000L);
		meta.setSrcMaxId(2344L);
		meta.setDestCount(1445L);
		meta.setDestMinId(900L);
		meta.setDestMaxId(2344L);
		MigrationTypeChecksum expectedChecksum = new MigrationTypeChecksum();
		expectedChecksum.setType(MigrationType.FILE_HANDLE);
		expectedChecksum.setMinid(1000L);
		expectedChecksum.setMaxid(2344L);
		expectedChecksum.setChecksum("CRC32");
		when(mockSrcClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq(1000L), eq(2344L))).thenReturn(expectedChecksum);
		when(mockDestClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq(1000L), eq(2344L))).thenReturn(expectedChecksum);
		DeltaFinder finder = new DeltaFinder(meta, mockSrcClient, mockDestClient, batchSize);
		DeltaRanges ranges = finder.findDeltaRanges();
		assertEquals(MigrationType.FILE_HANDLE, ranges.getMigrationType());
		assertNotNull(ranges.getInsRanges());
		assertEquals(0, ranges.getInsRanges().size());
		assertNotNull(ranges.getUpdRanges());
		assertEquals(0, ranges.getUpdRanges().size());
		assertNotNull(ranges.getDelRanges());
		assertEquals(1, ranges.getDelRanges().size());
		assertEquals(900, ranges.getDelRanges().get(0).getMinId());
		assertEquals(999, ranges.getDelRanges().get(0).getMaxId());
	}

	@Test
	public void testDelMaxSrc() throws Exception {
		TypeToMigrateMetadata meta = new TypeToMigrateMetadata();
		meta.setType(MigrationType.FILE_HANDLE);
		meta.setSrcCount(1345L);
		meta.setSrcMinId(1000L);
		meta.setSrcMaxId(2344L);
		meta.setDestCount(1445L);
		meta.setDestMinId(1000L);
		meta.setDestMaxId(2444L);
		MigrationTypeChecksum expectedChecksum = new MigrationTypeChecksum();
		expectedChecksum.setType(MigrationType.FILE_HANDLE);
		expectedChecksum.setMinid(1000L);
		expectedChecksum.setMaxid(2344L);
		expectedChecksum.setChecksum("CRC32");
		when(mockSrcClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq(1000L), eq(2344L))).thenReturn(expectedChecksum);
		when(mockDestClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq(1000L), eq(2344L))).thenReturn(expectedChecksum);
		DeltaFinder finder = new DeltaFinder(meta, mockSrcClient, mockDestClient, batchSize);
		DeltaRanges ranges = finder.findDeltaRanges();
		assertEquals(MigrationType.FILE_HANDLE, ranges.getMigrationType());
		assertNotNull(ranges.getInsRanges());
		assertEquals(0, ranges.getInsRanges().size());
		assertNotNull(ranges.getUpdRanges());
		assertEquals(0, ranges.getUpdRanges().size());
		assertNotNull(ranges.getDelRanges());
		assertEquals(1, ranges.getDelRanges().size());
		assertEquals(2345, ranges.getDelRanges().get(0).getMinId());
		assertEquals(2444, ranges.getDelRanges().get(0).getMaxId());
	}
	
	@Test
	public void testInsBothSides() throws Exception {
		TypeToMigrateMetadata meta = new TypeToMigrateMetadata();
		meta.setType(MigrationType.FILE_HANDLE);
		meta.setSrcCount(1445L);
		meta.setSrcMinId(900L);
		meta.setSrcMaxId(2344L);
		meta.setDestCount(1245L);
		meta.setDestMinId(1000L);
		meta.setDestMaxId(2244L);
		MigrationTypeChecksum expectedChecksum = new MigrationTypeChecksum();
		expectedChecksum.setType(MigrationType.FILE_HANDLE);
		expectedChecksum.setMinid(1000L);
		expectedChecksum.setMaxid(2244L);
		expectedChecksum.setChecksum("CRC32");
		when(mockSrcClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq(1000L), eq(2244L))).thenReturn(expectedChecksum);
		when(mockDestClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq(1000L), eq(2244L))).thenReturn(expectedChecksum);
		DeltaFinder finder = new DeltaFinder(meta, mockSrcClient, mockDestClient, batchSize);
		DeltaRanges ranges = finder.findDeltaRanges();
		assertEquals(MigrationType.FILE_HANDLE, ranges.getMigrationType());
		assertNotNull(ranges.getInsRanges());
		assertEquals(2, ranges.getInsRanges().size());
		assertEquals(900, ranges.getInsRanges().get(0).getMinId());
		assertEquals(999, ranges.getInsRanges().get(0).getMaxId());
		assertEquals(2245, ranges.getInsRanges().get(1).getMinId());
		assertEquals(2344, ranges.getInsRanges().get(1).getMaxId());
		assertNotNull(ranges.getUpdRanges());
		assertEquals(0, ranges.getUpdRanges().size());
		assertNotNull(ranges.getDelRanges());
		assertEquals(0, ranges.getDelRanges().size());
	}
	
	@Test
	public void testInsDelMixed() throws Exception {
		TypeToMigrateMetadata meta = new TypeToMigrateMetadata();
		meta.setType(MigrationType.FILE_HANDLE);
		meta.setSrcCount(1445L);
		meta.setSrcMinId(900L);
		meta.setSrcMaxId(2344L);
		meta.setDestCount(1445L);
		meta.setDestMinId(1000L);
		meta.setDestMaxId(2444L);
		MigrationTypeChecksum expectedChecksum = new MigrationTypeChecksum();
		expectedChecksum.setType(MigrationType.FILE_HANDLE);
		expectedChecksum.setMinid(1000L);
		expectedChecksum.setMaxid(2344L);
		expectedChecksum.setChecksum("CRC32");
		when(mockSrcClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq(1000L), eq(2344L))).thenReturn(expectedChecksum);
		when(mockDestClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq(1000L), eq(2344L))).thenReturn(expectedChecksum);
		DeltaFinder finder = new DeltaFinder(meta, mockSrcClient, mockDestClient, batchSize);
		DeltaRanges ranges = finder.findDeltaRanges();
		assertEquals(MigrationType.FILE_HANDLE, ranges.getMigrationType());
		assertNotNull(ranges.getInsRanges());
		assertEquals(1, ranges.getInsRanges().size());
		assertEquals(900, ranges.getInsRanges().get(0).getMinId());
		assertEquals(999, ranges.getInsRanges().get(0).getMaxId());
		assertNotNull(ranges.getUpdRanges());
		assertEquals(0, ranges.getUpdRanges().size());
		assertNotNull(ranges.getDelRanges());
		assertEquals(1, ranges.getDelRanges().size());
		assertEquals(2345, ranges.getDelRanges().get(0).getMinId());
		assertEquals(2444, ranges.getDelRanges().get(0).getMaxId());
	}
	
	@Test
	public void testUpgRange() throws Exception {
		TypeToMigrateMetadata meta = new TypeToMigrateMetadata();
		meta.setType(MigrationType.FILE_HANDLE);
		meta.setSrcCount(1345L);
		meta.setSrcMinId(1000L);
		meta.setSrcMaxId(2344L);
		meta.setDestCount(1345L);
		meta.setDestMinId(1000L);
		meta.setDestMaxId(2344L);
		MigrationTypeChecksum expectedChecksum1 = new MigrationTypeChecksum();
		expectedChecksum1.setType(MigrationType.FILE_HANDLE);
		expectedChecksum1.setMinid(1000L);
		expectedChecksum1.setMaxid(2344L);
		expectedChecksum1.setChecksum("CRC32-1");
		MigrationTypeChecksum expectedChecksum2 = new MigrationTypeChecksum();
		expectedChecksum2.setType(MigrationType.FILE_HANDLE);
		expectedChecksum2.setMinid(1000L);
		expectedChecksum2.setMaxid(2344L);
		expectedChecksum2.setChecksum("CRC32-2");
		when(mockSrcClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq(1000L), eq(2344L))).thenReturn(expectedChecksum1);
		when(mockDestClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq(1000L), eq(2344L))).thenReturn(expectedChecksum2);
		DeltaFinder finder = new DeltaFinder(meta, mockSrcClient, mockDestClient, 5000L);
		DeltaRanges ranges = finder.findDeltaRanges();
		assertEquals(MigrationType.FILE_HANDLE, ranges.getMigrationType());
		assertNotNull(ranges.getInsRanges());
		assertEquals(0, ranges.getInsRanges().size());
		assertNotNull(ranges.getUpdRanges());
		assertEquals(1, ranges.getUpdRanges().size());
		assertEquals(1000, ranges.getUpdRanges().get(0).getMinId());
		assertEquals(2344, ranges.getUpdRanges().get(0).getMaxId());
		assertEquals(0, ranges.getDelRanges().size());
		assertNotNull(ranges.getDelRanges());
	}
}
