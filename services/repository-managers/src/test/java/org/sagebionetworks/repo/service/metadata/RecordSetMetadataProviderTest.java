package org.sagebionetworks.repo.service.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.CsvTableDescriptor;

@ExtendWith(MockitoExtension.class)
public class RecordSetMetadataProviderTest {

	@Mock
	private FileEntityMetadataProvider mockFileEntityMetadataProvider;
	
	@InjectMocks
	private RecordSetMetadataProvider recordSetMetadataProvider;
	
	private RecordSet recordSet;
	private UserInfo userInfo;
	private List<EntityHeader> path;

	@BeforeEach
	public void before() {

		recordSet = new RecordSet();
		recordSet.setId("syn123");
		recordSet.setDataFileHandleId("456");
		recordSet.setParentId("syn234567");
		recordSet.setUpsertKey(List.of("a", "b"));

		userInfo = new UserInfo(false, 55L);

		path = List.of(
			new EntityHeader().setId("syn123456").setName("project").setType(Project.class.getName()),
			new EntityHeader().setId("syn234567").setName("folder").setType(Folder.class.getName())
		);
		
		
	}
	
	@ParameterizedTest
	@EnumSource(value = EventType.class, mode = Mode.INCLUDE, names = {"CREATE", "UPDATE"})
	public void testValidateEntity(EventType eventType) throws Exception {
		
		EntityEvent event = new EntityEvent(eventType, path, userInfo);
		
		// Call under test
		recordSetMetadataProvider.validateEntity(recordSet, event);
		
		verify(mockFileEntityMetadataProvider).validateEntity(recordSet, event);
	}
	
	@ParameterizedTest
	@EnumSource(value = EventType.class, mode = Mode.INCLUDE, names = {"CREATE", "UPDATE"})
	public void testValidateEntityWithNoUpsertKey(EventType eventType) throws Exception {
		recordSet.setUpsertKey(null);
		
		EntityEvent event = new EntityEvent(eventType, path, userInfo);
		
		assertEquals("The upsertKey is required and must not be empty.", 
			assertThrows(IllegalArgumentException.class, () -> {
				// Call under test
				recordSetMetadataProvider.validateEntity(recordSet, event);
			}).getMessage()
		);
		
		recordSet.setUpsertKey(Collections.emptyList());
		
		assertEquals("The upsertKey is required and must not be empty.", 
			assertThrows(IllegalArgumentException.class, () -> {
				// Call under test
				recordSetMetadataProvider.validateEntity(recordSet, event);
			}).getMessage()
		);
		
		verifyZeroInteractions(mockFileEntityMetadataProvider);
	}
	
	@ParameterizedTest
	@EnumSource(value = EventType.class, mode = Mode.INCLUDE, names = {"CREATE", "UPDATE"})
	public void testValidateEntityWithCsvDescriptorAndIsFirstLineHeaderMissingOrFalse(EventType eventType) throws Exception {
		recordSet.setCsvDescriptor(new CsvTableDescriptor().setIsFirstLineHeader(false));
		
		EntityEvent event = new EntityEvent(eventType, path, userInfo);
		
		assertEquals("The csvDescriptor.isFirstLineHeader must be true.", 
			assertThrows(IllegalArgumentException.class, () -> {
				// Call under test
				recordSetMetadataProvider.validateEntity(recordSet, event);
			}).getMessage()
		);
		
		recordSet.setCsvDescriptor(new CsvTableDescriptor().setIsFirstLineHeader(null));
		
		assertEquals("The csvDescriptor.isFirstLineHeader must be true.", 
			assertThrows(IllegalArgumentException.class, () -> {
				// Call under test
				recordSetMetadataProvider.validateEntity(recordSet, event);
			}).getMessage()
		);
		
		verifyZeroInteractions(mockFileEntityMetadataProvider);
	}
	
	@Test
	public void testEntityCreated() {
		// Call under test
		recordSetMetadataProvider.entityCreated(userInfo, recordSet);
	
		verify(mockFileEntityMetadataProvider).entityCreated(userInfo, recordSet);
	}
	
	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testEntityUpdated(boolean wasNewVersionCreated) {
		// Call under test
		recordSetMetadataProvider.entityUpdated(userInfo, recordSet, wasNewVersionCreated);
	
		verify(mockFileEntityMetadataProvider).entityUpdated(userInfo, recordSet, wasNewVersionCreated);
	}
	
}
