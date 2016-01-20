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
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.repo.model.migration.MigrationRangeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeChecksum;
import org.sagebionetworks.tool.migration.v4.delta.DeltaFinder;
import org.sagebionetworks.tool.migration.v4.delta.DeltaRanges;
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
		DeltaFinder finder = new DeltaFinder(meta, mockSrcClient, mockDestClient, "salt", batchSize);
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
		verify(mockSrcClient, never()).getChecksumForIdRange(any(MigrationType.class), anyString(), anyLong(), anyLong());
		verify(mockDestClient, never()).getChecksumForIdRange(any(MigrationType.class), anyString(), anyLong(), anyLong());
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
		DeltaFinder finder = new DeltaFinder(meta, mockSrcClient, mockDestClient, "salt", batchSize);
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
		verify(mockSrcClient, never()).getChecksumForIdRange(any(MigrationType.class), anyString(), anyLong(), anyLong());
		verify(mockDestClient, never()).getChecksumForIdRange(any(MigrationType.class), anyString(), anyLong(), anyLong());
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
		DeltaFinder finder = new DeltaFinder(meta, mockSrcClient, mockDestClient, "salt", batchSize);
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
		verify(mockSrcClient, never()).getChecksumForIdRange(any(MigrationType.class), anyString(), anyLong(), anyLong());
		verify(mockDestClient, never()).getChecksumForIdRange(any(MigrationType.class), anyString(), anyLong(), anyLong());
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
		DeltaFinder finder = new DeltaFinder(meta, mockSrcClient, mockDestClient, "salt", batchSize);
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
		verify(mockSrcClient, never()).getChecksumForIdRange(any(MigrationType.class), anyString(), anyLong(), anyLong());
		verify(mockDestClient, never()).getChecksumForIdRange(any(MigrationType.class), anyString(), anyLong(), anyLong());
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
		MigrationRangeChecksum expectedChecksum = new MigrationRangeChecksum();
		expectedChecksum.setType(MigrationType.FILE_HANDLE);
		expectedChecksum.setMinid(1000L);
		expectedChecksum.setMaxid(2344L);
		expectedChecksum.setChecksum("CRC32");
		when(mockSrcClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(2344L))).thenReturn(expectedChecksum);
		when(mockDestClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(2344L))).thenReturn(expectedChecksum);
		DeltaFinder finder = new DeltaFinder(meta, mockSrcClient, mockDestClient, "salt", batchSize);
		DeltaRanges ranges = finder.findDeltaRanges();
		assertEquals(MigrationType.FILE_HANDLE, ranges.getMigrationType());
		assertNotNull(ranges.getInsRanges());
		assertEquals(0, ranges.getInsRanges().size());
		assertNotNull(ranges.getUpdRanges());
		assertEquals(0, ranges.getUpdRanges().size());
		assertEquals(0, ranges.getDelRanges().size());
		assertNotNull(ranges.getDelRanges());
		verify(mockSrcClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(2344L));
		verify(mockDestClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(2344L));
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
		MigrationRangeChecksum expectedChecksum = new MigrationRangeChecksum();
		expectedChecksum.setType(MigrationType.FILE_HANDLE);
		expectedChecksum.setMinid(1000L);
		expectedChecksum.setMaxid(2344L);
		expectedChecksum.setChecksum("CRC32");
		when(mockSrcClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(2344L))).thenReturn(expectedChecksum);
		when(mockDestClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(2344L))).thenReturn(expectedChecksum);
		DeltaFinder finder = new DeltaFinder(meta, mockSrcClient, mockDestClient, "salt", batchSize);
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
		verify(mockSrcClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(2344L));
		verify(mockDestClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(2344L));
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
		MigrationRangeChecksum expectedChecksum = new MigrationRangeChecksum();
		expectedChecksum.setType(MigrationType.FILE_HANDLE);
		expectedChecksum.setMinid(1000L);
		expectedChecksum.setMaxid(2344L);
		expectedChecksum.setChecksum("CRC32");
		when(mockSrcClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(2344L))).thenReturn(expectedChecksum);
		when(mockDestClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(2344L))).thenReturn(expectedChecksum);
		DeltaFinder finder = new DeltaFinder(meta, mockSrcClient, mockDestClient, "salt", batchSize);
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
		verify(mockSrcClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(2344L));
		verify(mockDestClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(2344L));
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
		MigrationRangeChecksum expectedChecksum = new MigrationRangeChecksum();
		expectedChecksum.setType(MigrationType.FILE_HANDLE);
		expectedChecksum.setMinid(1000L);
		expectedChecksum.setMaxid(2344L);
		expectedChecksum.setChecksum("CRC32");
		when(mockSrcClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(2344L))).thenReturn(expectedChecksum);
		when(mockDestClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(2344L))).thenReturn(expectedChecksum);
		DeltaFinder finder = new DeltaFinder(meta, mockSrcClient, mockDestClient, "salt", batchSize);
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
		verify(mockSrcClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(2344L));
		verify(mockDestClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(2344L));
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
		MigrationRangeChecksum expectedChecksum = new MigrationRangeChecksum();
		expectedChecksum.setType(MigrationType.FILE_HANDLE);
		expectedChecksum.setMinid(1000L);
		expectedChecksum.setMaxid(2344L);
		expectedChecksum.setChecksum("CRC32");
		when(mockSrcClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(2344L))).thenReturn(expectedChecksum);
		when(mockDestClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(2344L))).thenReturn(expectedChecksum);
		DeltaFinder finder = new DeltaFinder(meta, mockSrcClient, mockDestClient, "salt", batchSize);
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
		verify(mockSrcClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(2344L));
		verify(mockDestClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(2344L));
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
		MigrationRangeChecksum expectedChecksum1 = new MigrationRangeChecksum();
		expectedChecksum1.setType(MigrationType.FILE_HANDLE);
		expectedChecksum1.setMinid(1000L);
		expectedChecksum1.setMaxid(2344L);
		expectedChecksum1.setChecksum("CRC32-1");
		MigrationRangeChecksum expectedChecksum2 = new MigrationRangeChecksum();
		expectedChecksum2.setType(MigrationType.FILE_HANDLE);
		expectedChecksum2.setMinid(1000L);
		expectedChecksum2.setMaxid(2344L);
		expectedChecksum2.setChecksum("CRC32-2");
		when(mockSrcClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(2344L))).thenReturn(expectedChecksum1);
		when(mockDestClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(2344L))).thenReturn(expectedChecksum2);
		DeltaFinder finder = new DeltaFinder(meta, mockSrcClient, mockDestClient, "salt", 5000L);
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
		verify(mockSrcClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(2344L));
		verify(mockDestClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(2344L));
	}
	
	@Test
	public void testUpgRangeOneRecursion() throws Exception {
		TypeToMigrateMetadata meta = new TypeToMigrateMetadata();
		meta.setType(MigrationType.FILE_HANDLE);
		meta.setSrcCount(20L);
		meta.setSrcMinId(1000L);
		meta.setSrcMaxId(1019L);
		meta.setDestCount(20L);
		meta.setDestMinId(1000L);
		meta.setDestMaxId(1019L);
		// The first pass will differ
		MigrationRangeChecksum expectedChecksum1 = new MigrationRangeChecksum();
		expectedChecksum1.setType(MigrationType.FILE_HANDLE);
		expectedChecksum1.setMinid(1000L);
		expectedChecksum1.setMaxid(1019L);
		expectedChecksum1.setChecksum("CRC32-1-1");
		MigrationRangeChecksum expectedChecksum2 = new MigrationRangeChecksum();
		expectedChecksum2.setType(MigrationType.FILE_HANDLE);
		expectedChecksum2.setMinid(1000L);
		expectedChecksum2.setMaxid(1019L);
		expectedChecksum2.setChecksum("CRC32-1-2");
		when(mockSrcClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(1019L))).thenReturn(expectedChecksum1);
		when(mockDestClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(1019L))).thenReturn(expectedChecksum2);
		// The second pass will differ in the second half
		// 1st half - same CRC32
		MigrationRangeChecksum expectedChecksum3 = new MigrationRangeChecksum();
		expectedChecksum3.setType(MigrationType.FILE_HANDLE);
		expectedChecksum3.setMinid(1000L);
		expectedChecksum3.setMaxid(1009L);
		expectedChecksum3.setChecksum("CRC32-2-1");
		when(mockSrcClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(1009L))).thenReturn(expectedChecksum3);
		when(mockDestClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(1009L))).thenReturn(expectedChecksum3);
		// 2nd half - different CRC32
		MigrationRangeChecksum expectedChecksum4 = new MigrationRangeChecksum();
		expectedChecksum4.setType(MigrationType.FILE_HANDLE);
		expectedChecksum4.setMinid(1010L);
		expectedChecksum4.setMaxid(1019L);
		expectedChecksum4.setChecksum("CRC32-2-2");
		MigrationRangeChecksum expectedChecksum5 = new MigrationRangeChecksum();
		expectedChecksum5.setType(MigrationType.FILE_HANDLE);
		expectedChecksum5.setMinid(1010L);
		expectedChecksum5.setMaxid(1019L);
		expectedChecksum5.setChecksum("CRC32-2-3");
		when(mockSrcClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1010L), eq(1019L))).thenReturn(expectedChecksum4);
		when(mockDestClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1010L), eq(1019L))).thenReturn(expectedChecksum5);
		// Setup the batch size to 10 so we don't do more than 2 loops
		DeltaFinder finder = new DeltaFinder(meta, mockSrcClient, mockDestClient, "salt", 10L);
		DeltaRanges ranges = finder.findDeltaRanges();
		assertEquals(MigrationType.FILE_HANDLE, ranges.getMigrationType());
		assertNotNull(ranges.getInsRanges());
		assertEquals(0, ranges.getInsRanges().size());
		assertNotNull(ranges.getUpdRanges());
		assertEquals(1, ranges.getUpdRanges().size());
		assertEquals(1010, ranges.getUpdRanges().get(0).getMinId());
		assertEquals(1019, ranges.getUpdRanges().get(0).getMaxId());
		assertEquals(0, ranges.getDelRanges().size());
		assertNotNull(ranges.getDelRanges());
		verify(mockSrcClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(1019L));
		verify(mockDestClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(1019L));
		verify(mockSrcClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(1009L));
		verify(mockDestClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(1009L));
		verify(mockSrcClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1010L), eq(1019L));
		verify(mockDestClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1010L), eq(1019L));
	}
	
	@Test
	public void testUpgRangeOneRecursionOdd() throws Exception {
		TypeToMigrateMetadata meta = new TypeToMigrateMetadata();
		meta.setType(MigrationType.FILE_HANDLE);
		meta.setSrcCount(21L);
		meta.setSrcMinId(1000L);
		meta.setSrcMaxId(1020L);
		meta.setDestCount(21L);
		meta.setDestMinId(1000L);
		meta.setDestMaxId(1020L);
		// The first pass will differ
		MigrationRangeChecksum expectedChecksum1 = new MigrationRangeChecksum();
		expectedChecksum1.setType(MigrationType.FILE_HANDLE);
		expectedChecksum1.setMinid(1000L);
		expectedChecksum1.setMaxid(1020L);
		expectedChecksum1.setChecksum("CRC32-1-1");
		MigrationRangeChecksum expectedChecksum2 = new MigrationRangeChecksum();
		expectedChecksum2.setType(MigrationType.FILE_HANDLE);
		expectedChecksum2.setMinid(1000L);
		expectedChecksum2.setMaxid(1020L);
		expectedChecksum2.setChecksum("CRC32-1-2");
		when(mockSrcClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(1020L))).thenReturn(expectedChecksum1);
		when(mockDestClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(1020L))).thenReturn(expectedChecksum2);
		// The second pass will differ in the second half
		// 1st half - same CRC32
		MigrationRangeChecksum expectedChecksum3 = new MigrationRangeChecksum();
		expectedChecksum3.setType(MigrationType.FILE_HANDLE);
		expectedChecksum3.setMinid(1000L);
		expectedChecksum3.setMaxid(1010L);
		expectedChecksum3.setChecksum("CRC32-2-1");
		when(mockSrcClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(1010L))).thenReturn(expectedChecksum3);
		when(mockDestClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(1010L))).thenReturn(expectedChecksum3);
		// 2nd half - different CRC32
		MigrationRangeChecksum expectedChecksum4 = new MigrationRangeChecksum();
		expectedChecksum4.setType(MigrationType.FILE_HANDLE);
		expectedChecksum4.setMinid(1011L);
		expectedChecksum4.setMaxid(1020L);
		expectedChecksum4.setChecksum("CRC32-2-2");
		MigrationRangeChecksum expectedChecksum5 = new MigrationRangeChecksum();
		expectedChecksum5.setType(MigrationType.FILE_HANDLE);
		expectedChecksum5.setMinid(1011L);
		expectedChecksum5.setMaxid(1020L);
		expectedChecksum5.setChecksum("CRC32-2-3");
		when(mockSrcClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1011L), eq(1020L))).thenReturn(expectedChecksum4);
		when(mockDestClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1011L), eq(1020L))).thenReturn(expectedChecksum5);
		// Setup the batch size to 10 so we don't do more than 2 loops
		DeltaFinder finder = new DeltaFinder(meta, mockSrcClient, mockDestClient, "salt", 10L);
		DeltaRanges ranges = finder.findDeltaRanges();
		assertEquals(MigrationType.FILE_HANDLE, ranges.getMigrationType());
		assertNotNull(ranges.getInsRanges());
		assertEquals(0, ranges.getInsRanges().size());
		assertNotNull(ranges.getUpdRanges());
		assertEquals(1, ranges.getUpdRanges().size());
		assertEquals(1011, ranges.getUpdRanges().get(0).getMinId());
		assertEquals(1020, ranges.getUpdRanges().get(0).getMaxId());
		assertEquals(0, ranges.getDelRanges().size());
		assertNotNull(ranges.getDelRanges());
		verify(mockSrcClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(1020L));
		verify(mockDestClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(1020L));
		verify(mockSrcClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(1010L));
		verify(mockDestClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(1010L));
		verify(mockSrcClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1011L), eq(1020L));
		verify(mockDestClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1011L), eq(1020L));
	}
	
	@Test
	public void testUpgRangeTwoRanges() throws Exception {
		TypeToMigrateMetadata meta = new TypeToMigrateMetadata();
		meta.setType(MigrationType.FILE_HANDLE);
		meta.setSrcCount(40L);
		meta.setSrcMinId(1000L);
		meta.setSrcMaxId(1039L);
		meta.setDestCount(40L);
		meta.setDestMinId(1000L);
		meta.setDestMaxId(1039L);
		// The first pass will differ
		MigrationRangeChecksum expectedChecksum1 = new MigrationRangeChecksum();
		expectedChecksum1.setType(MigrationType.FILE_HANDLE);
		expectedChecksum1.setMinid(1000L);
		expectedChecksum1.setMaxid(1039L);
		expectedChecksum1.setChecksum("CRC32-1-1");
		MigrationRangeChecksum expectedChecksum2 = new MigrationRangeChecksum();
		expectedChecksum2.setType(MigrationType.FILE_HANDLE);
		expectedChecksum2.setMinid(1000L);
		expectedChecksum2.setMaxid(1039L);
		expectedChecksum2.setChecksum("CRC32-1-2");
		when(mockSrcClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(1039L))).thenReturn(expectedChecksum1);
		when(mockDestClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(1039L))).thenReturn(expectedChecksum2);
		// The second pass will differ in both second half
		// 1st half - different CRC32
		MigrationRangeChecksum expectedChecksum3 = new MigrationRangeChecksum();
		expectedChecksum3.setType(MigrationType.FILE_HANDLE);
		expectedChecksum3.setMinid(1000L);
		expectedChecksum3.setMaxid(1019L);
		expectedChecksum3.setChecksum("CRC32-2-1");
		MigrationRangeChecksum expectedChecksum4 = new MigrationRangeChecksum();
		expectedChecksum4.setType(MigrationType.FILE_HANDLE);
		expectedChecksum4.setMinid(1000L);
		expectedChecksum4.setMaxid(1019L);
		expectedChecksum4.setChecksum("CRC32-2-2");
		when(mockSrcClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(1019L))).thenReturn(expectedChecksum3);
		when(mockDestClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(1019L))).thenReturn(expectedChecksum4);
		// 2nd half - different CRC32
		MigrationRangeChecksum expectedChecksum5 = new MigrationRangeChecksum();
		expectedChecksum5.setType(MigrationType.FILE_HANDLE);
		expectedChecksum5.setMinid(1020L);
		expectedChecksum5.setMaxid(1039L);
		expectedChecksum5.setChecksum("CRC32-2-3");
		MigrationRangeChecksum expectedChecksum6 = new MigrationRangeChecksum();
		expectedChecksum6.setType(MigrationType.FILE_HANDLE);
		expectedChecksum6.setMinid(1020L);
		expectedChecksum6.setMaxid(1039L);
		expectedChecksum6.setChecksum("CRC32-2-4");
		when(mockSrcClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1020L), eq(1039L))).thenReturn(expectedChecksum5);
		when(mockDestClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1020L), eq(1039L))).thenReturn(expectedChecksum6);
		// The 3rd pass will differ in first and last block
		// 1st quarter - different CRC32
		MigrationRangeChecksum expectedChecksum7 = new MigrationRangeChecksum();
		expectedChecksum7.setType(MigrationType.FILE_HANDLE);
		expectedChecksum7.setMinid(1000L);
		expectedChecksum7.setMaxid(1009L);
		expectedChecksum7.setChecksum("CRC32-3-1");
		MigrationRangeChecksum expectedChecksum8 = new MigrationRangeChecksum();
		expectedChecksum8.setType(MigrationType.FILE_HANDLE);
		expectedChecksum8.setMinid(1000L);
		expectedChecksum8.setMaxid(1009L);
		expectedChecksum8.setChecksum("CRC32-3-2");
		when(mockSrcClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(1009L))).thenReturn(expectedChecksum7);
		when(mockDestClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(1009L))).thenReturn(expectedChecksum8);
		// 2nd quarter - same CRC32
		MigrationRangeChecksum expectedChecksum9 = new MigrationRangeChecksum();
		expectedChecksum9.setType(MigrationType.FILE_HANDLE);
		expectedChecksum9.setMinid(1010L);
		expectedChecksum9.setMaxid(1019L);
		expectedChecksum9.setChecksum("CRC32-3-3");
		when(mockSrcClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1010L), eq(1019L))).thenReturn(expectedChecksum9);
		when(mockDestClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1010L), eq(1019L))).thenReturn(expectedChecksum9);
		// 3rd quarter - same CRC32
		MigrationRangeChecksum expectedChecksum10 = new MigrationRangeChecksum();
		expectedChecksum10.setType(MigrationType.FILE_HANDLE);
		expectedChecksum10.setMinid(1020L);
		expectedChecksum10.setMaxid(1029L);
		expectedChecksum10.setChecksum("CRC32-3-4");
		when(mockSrcClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1020L), eq(1029L))).thenReturn(expectedChecksum10);
		when(mockDestClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1020L), eq(1029L))).thenReturn(expectedChecksum10);
		// 4th quarter - different CRC32
		MigrationRangeChecksum expectedChecksum11 = new MigrationRangeChecksum();
		expectedChecksum11.setType(MigrationType.FILE_HANDLE);
		expectedChecksum11.setMinid(1030L);
		expectedChecksum11.setMaxid(1039L);
		expectedChecksum11.setChecksum("CRC32-3-5");
		MigrationRangeChecksum expectedChecksum12 = new MigrationRangeChecksum();
		expectedChecksum12.setType(MigrationType.FILE_HANDLE);
		expectedChecksum12.setMinid(1030L);
		expectedChecksum12.setMaxid(1039L);
		expectedChecksum12.setChecksum("CRC32-3-6");
		when(mockSrcClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1030L), eq(1039L))).thenReturn(expectedChecksum11);
		when(mockDestClient.getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1030L), eq(1039L))).thenReturn(expectedChecksum12);
		// Setup the batch size to 10 so we don't do more than 2 loops
		DeltaFinder finder = new DeltaFinder(meta, mockSrcClient, mockDestClient, "salt", 10L);
		DeltaRanges ranges = finder.findDeltaRanges();
		assertEquals(MigrationType.FILE_HANDLE, ranges.getMigrationType());
		assertNotNull(ranges.getInsRanges());
		assertEquals(0, ranges.getInsRanges().size());
		assertNotNull(ranges.getUpdRanges());
		assertEquals(2, ranges.getUpdRanges().size());
		assertEquals(1000, ranges.getUpdRanges().get(0).getMinId());
		assertEquals(1009, ranges.getUpdRanges().get(0).getMaxId());
		assertEquals(1030, ranges.getUpdRanges().get(1).getMinId());
		assertEquals(1039, ranges.getUpdRanges().get(1).getMaxId());
		assertEquals(0, ranges.getDelRanges().size());
		assertNotNull(ranges.getDelRanges());
		verify(mockSrcClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(1039L));
		verify(mockDestClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(1039L));
		verify(mockSrcClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(1019L));
		verify(mockDestClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(1019L));
		verify(mockSrcClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1020L), eq(1039L));
		verify(mockDestClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1020L), eq(1039L));
		verify(mockSrcClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(1009L));
		verify(mockDestClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1000L), eq(1009L));
		verify(mockSrcClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1010L), eq(1019L));
		verify(mockDestClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1010L), eq(1019L));
		verify(mockSrcClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1020L), eq(1029L));
		verify(mockDestClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1020L), eq(1029L));
		verify(mockSrcClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1030L), eq(1039L));
		verify(mockDestClient).getChecksumForIdRange(eq(MigrationType.FILE_HANDLE), eq("salt"), eq(1030L), eq(1039L));
	}	
}
