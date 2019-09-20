package org.sagebionetworks.repo.model.athena.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.athena.AthenaSupport;
import org.sagebionetworks.repo.model.statistics.FileEvent;

@ExtendWith(MockitoExtension.class)
public class FileEventTableNameProviderTest {

	@Mock
	private AthenaSupport mockAthenaSupport;

	@InjectMocks
	private FileEventTableNameProvider provider;

	@Test
	public void testGetTableName() {
		String expectedTableName = "sometablename";
		when(mockAthenaSupport.getTableName(any())).thenReturn(expectedTableName);

		for (FileEvent eventType : FileEvent.values()) {
			// Call under test
			String result = provider.getTableName(eventType);
			assertEquals(expectedTableName, result);
			verify(mockAthenaSupport).getTableName(eventType.getGlueTableName());
		}
		
	}

}
