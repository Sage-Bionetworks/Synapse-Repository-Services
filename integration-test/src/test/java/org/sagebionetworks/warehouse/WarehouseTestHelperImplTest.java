package org.sagebionetworks.warehouse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.Clock;

import com.amazonaws.services.athena.AmazonAthena;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ListObjectsV2Result;

@ExtendWith(MockitoExtension.class)
public class WarehouseTestHelperImplTest {

	@Mock
	private AmazonS3 mockS3Client;
	@Mock
	private AmazonAthena mockAthenaClient;
	@Mock
	private Clock mockClock;
	@Mock
	private StackConfiguration mockStackConfig;

	@InjectMocks
	private WarehouseTestHelperImpl warehouseHelper;

	@Test
	public void testSaveQueryToS3() {
		String query = "select count(*) from foo";
		Instant now = Instant.ofEpochMilli(1001L);
		String path = "instance/foo/bar";

		// call under test
		warehouseHelper.saveQueryToS3(query, now, path);

		verify(mockS3Client).putObject(WarehouseTestHelperImpl.BUCKET_NAME,
				"instance/foo/bar/90001001.sql",query);
	}
	
	@Test
	public void testGetExpiresOnFromKey() {
		// call under test
		assertEquals(123L, WarehouseTestHelperImpl.getExpiresOnFromKey("istance/org/sage/44/123.sql"));
	}
	
	@Test
	public void testAssertWarehouseQuery() throws Exception {
		when(mockStackConfig.getStackInstance()).thenReturn("anInstance");
		when(mockClock.currentTimeMillis()).thenReturn(1001L);
		when(mockS3Client.listObjectsV2(any(), any())).thenReturn(new ListObjectsV2Result());
		
		String query = "select count(*) from foo";
		
		// call under test
		warehouseHelper.assertWarehouseQuery(query);
		
		verify(mockS3Client).putObject(WarehouseTestHelperImpl.BUCKET_NAME,
				"anInstance/org/sagebionetworks/warehouse/WarehouseTestHelperImplTest/testAssertWarehouseQuery/60/90001001.sql",query);
		verify(mockClock).currentTimeMillis();
	}
}
