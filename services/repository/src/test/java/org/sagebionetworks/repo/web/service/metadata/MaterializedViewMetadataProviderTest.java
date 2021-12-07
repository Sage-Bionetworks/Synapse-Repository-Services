package org.sagebionetworks.repo.web.service.metadata;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.table.MaterializedViewManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
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
		String id = "syn123";
		String sql = "SELECT * FROM syn456";
		
		when(mockView.getId()).thenReturn(id);
		when(mockView.getDefiningSQL()).thenReturn(sql);
		
		IdAndVersion idAndVersion = IdAndVersion.parse(id);
		
		// Call under test
		provider.entityCreated(mockUser, mockView);
	
		verify(mockManager).registerSourceTables(idAndVersion, sql);
	}
	
	@Test
	public void testEntityUpdated() {
		String id = "syn123";
		String sql = "SELECT * FROM syn456";
		
		when(mockView.getId()).thenReturn(id);
		when(mockView.getDefiningSQL()).thenReturn(sql);
		
		IdAndVersion idAndVersion = IdAndVersion.parse(id);
		
		// Call under test
		provider.entityUpdated(mockUser, mockView, false);
		
		verify(mockManager).registerSourceTables(idAndVersion, sql);
	}

}
