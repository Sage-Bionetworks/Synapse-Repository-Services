package org.sagebionetworks.repo.web.service.metadata;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.table.VirtualTableManager;
import org.sagebionetworks.repo.model.table.VirtualTable;

@ExtendWith(MockitoExtension.class)
public class VirtualTableMetadataProviderTest {

	@Mock
	private VirtualTableManager mockManager;
	
	@InjectMocks
	private VirtualTableMetadataProvider provider;

	@Mock
	private VirtualTable mockTable;

	@Mock
	private EntityEvent mockEvent;

	@Test
	public void testValidateEntity() {
		
		// Call under test
		provider.validateEntity(mockTable, mockEvent);
		
		verify(mockManager).validate(mockTable);
	}
}
