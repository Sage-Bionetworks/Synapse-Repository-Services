package org.sagebionetworks.warehouse;

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
		
		when(mockStackConfig.getStackInstance()).thenReturn("instance");

		String query = "select count(*) from foo";
		int maxHours = 3;
		Instant now = Instant.ofEpochMilli(1001L);

		// call under test
		warehouseHelper.saveQueryToS3(query, maxHours, now);

		// Note: The path includes the line number of the above call (45). 
		verify(mockS3Client).putObject(WarehouseTestHelperImpl.BUCKET_NAME,
				"instance/org/sagebionetworks/warehouse/WarehouseTestHelperImplTest/testSaveQueryToS3/44/10801001.json",
				"{\"query\":\"select count(*) from foo\",\"maxNumberOfHours\":3}");
	}
}
