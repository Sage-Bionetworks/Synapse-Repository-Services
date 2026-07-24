package org.sagebionetworks.repo.service.metadata;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.entity.RecordSetManager;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;

@ExtendWith(MockitoExtension.class)
public class RecordSetMetadataProviderTest {

	@Mock
	private FileEntityMetadataProvider mockFileEntityMetadataProvider;

	@Mock
	private RecordSetManager mockRecordSetManager;

	@InjectMocks
	private RecordSetMetadataProvider recordSetMetadataProvider;

	private RecordSet recordSet;
	private UserInfo userInfo;
	private List<EntityHeader> path;

	@BeforeEach
	public void before() {
		recordSet = new RecordSet();
		recordSet.setId("syn123");

		userInfo = new UserInfo(false, 55L);

		path = List.of(
			new EntityHeader().setId("syn123456").setName("project").setType(Project.class.getName()),
			new EntityHeader().setId("syn234567").setName("folder").setType(Folder.class.getName())
		);
	}

	@Test
	public void testValidateEntity() throws Exception {
		EntityEvent event = new EntityEvent(EventType.CREATE, path, userInfo, false);

		// call under test
		recordSetMetadataProvider.validateEntity(recordSet, event);

		// RecordSet-specific validation/sanitization must run before the generic file validation.
		InOrder order = inOrder(mockRecordSetManager, mockFileEntityMetadataProvider);
		order.verify(mockRecordSetManager).validateRecordSet(recordSet, event);
		order.verify(mockFileEntityMetadataProvider).validateEntity(recordSet, event);
	}

	@Test
	public void testEntityCreated() {
		// call under test
		recordSetMetadataProvider.entityCreated(userInfo, recordSet);

		// The file provider runs first, then the schema is inferred and bound.
		InOrder order = inOrder(mockFileEntityMetadataProvider, mockRecordSetManager);
		order.verify(mockFileEntityMetadataProvider).entityCreated(userInfo, recordSet);
		order.verify(mockRecordSetManager).inferSchemaAndBindToIndex(userInfo, recordSet);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testEntityUpdated(boolean wasNewVersionCreated) {
		// call under test
		recordSetMetadataProvider.entityUpdated(userInfo, recordSet, wasNewVersionCreated);

		// The file provider runs first, then the schema is inferred and bound.
		InOrder order = inOrder(mockFileEntityMetadataProvider, mockRecordSetManager);
		order.verify(mockFileEntityMetadataProvider).entityUpdated(userInfo, recordSet, wasNewVersionCreated);
		order.verify(mockRecordSetManager).inferSchemaAndBindToIndex(userInfo, recordSet);
	}

	@ParameterizedTest
	@EnumSource(value = EventType.class)
	public void testAddTypeSpecificMetadata(EventType eventType) throws Exception {
		// call under test
		recordSetMetadataProvider.addTypeSpecificMetadata(recordSet, userInfo, eventType);

		verify(mockRecordSetManager).updateWithValidationResults(recordSet);
	}
}
