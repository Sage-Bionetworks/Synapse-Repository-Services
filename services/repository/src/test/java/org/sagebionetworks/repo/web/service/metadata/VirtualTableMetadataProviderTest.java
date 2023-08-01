package org.sagebionetworks.repo.web.service.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.table.VirtualTableManager;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
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
	
	@Test
	public void testAddTypeSpecificMetadata() {
		when(mockTable.getId()).thenReturn("syn123");
		when(mockTable.getVersionNumber()).thenReturn(2L);
		List<String> columnIds = List.of("1","2","3");
		when(mockManager.getSchemaIds(any())).thenReturn(columnIds);
		
		// call under test
		provider.addTypeSpecificMetadata(mockTable, null, null);
		
		verify(mockManager).getSchemaIds(IdAndVersion.parse("syn123.2"));
		verify(mockTable).setColumnIds(columnIds);
	}
	
	@Test
	public void testEntityUpdated() {
		when(mockTable.getId()).thenReturn("syn123");
		when(mockTable.getDefiningSQL()).thenReturn("select * from syn345");
		boolean wasNewVersionCreated = false;
		// call under test
		provider.entityUpdated(null, mockTable, wasNewVersionCreated);
		
		verify(mockManager).registerDefiningSql(IdAndVersion.parse("syn123"), "select * from syn345");
	}
	
	@Test
	public void testEntityUpdatedWithNewVersion() {
		boolean wasNewVersionCreated = true;
		String message = assertThrows(IllegalArgumentException.class, ()->{
			// call under test
			provider.entityUpdated(null, mockTable, wasNewVersionCreated);
		}).getMessage();
		assertEquals("A VirtualTable version can only be created by creating a snapshot.", message);

		verifyZeroInteractions(mockManager);
	}
	
	@Test
	public void testEntityCreated() {
		when(mockTable.getId()).thenReturn("syn123");
		when(mockTable.getDefiningSQL()).thenReturn("select * from syn345");
		// call under test
		provider.entityCreated(null, mockTable);
		
		verify(mockManager).registerDefiningSql(IdAndVersion.parse("syn123"), "select * from syn345");
	}
}
