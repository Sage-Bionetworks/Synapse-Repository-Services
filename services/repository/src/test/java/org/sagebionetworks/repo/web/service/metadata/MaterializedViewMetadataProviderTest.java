package org.sagebionetworks.repo.web.service.metadata;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.table.MaterializedViewManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.MaterializedView;

@ExtendWith(MockitoExtension.class)
public class MaterializedViewMetadataProviderTest {

	@InjectMocks
	private MaterializedViewMetadataProvider provider;
	
	@Mock
	private MaterializedViewManager mockManager;

	@Mock
	private MaterializedView mockView;
	
	@Mock
	private EntityEvent mockEvent;
	
	@Mock
	private UserInfo mockUser;
	
	@Test
	public void testValidateEntity() {	
		// Call under test
		provider.validateEntity(mockView, mockEvent);
		
		verify(mockManager).validate(mockView);
	}
	
	@Test
	public void testEntityCreated() {
		// Call under test
		provider.entityCreated(mockUser, mockView);
	
		verify(mockManager).registerSourceTables(mockView);
	}
	
	@Test
	public void testEntityUpdated() {
		// Call under test
		provider.entityUpdated(mockUser, mockView, false);
		
		verify(mockManager).registerSourceTables(mockView);
	}

}
