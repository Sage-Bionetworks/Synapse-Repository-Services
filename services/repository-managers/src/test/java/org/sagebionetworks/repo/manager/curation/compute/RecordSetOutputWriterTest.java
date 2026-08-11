package org.sagebionetworks.repo.manager.curation.compute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.RecordSet;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
import org.sagebionetworks.repo.model.schema.JsonSchemaVersionInfo;

@ExtendWith(MockitoExtension.class)
public class RecordSetOutputWriterTest {

	@Mock
	private EntityManager entityManager;

	@InjectMocks
	private RecordSetOutputWriter writer;

	private UserInfo user;

	@BeforeEach
	public void setup() {
		user = new UserInfo(false, 101L);
	}

	@Test
	public void testGetBoundSchemaId() {
		when(entityManager.getBoundSchema(user, "syn300")).thenReturn(new JsonSchemaObjectBinding()
				.setJsonSchemaVersionInfo(new JsonSchemaVersionInfo().set$id("my.org-Sheet-1.0.0")));

		// call under test
		String schemaId = writer.getBoundSchemaId(user, "syn300");

		assertEquals("my.org-Sheet-1.0.0", schemaId);
	}

	@Test
	public void testStoreCsvAsNewRecordSetVersion() {
		when(entityManager.getEntity(user, "syn300", RecordSet.class)).thenReturn(new RecordSet()
				.setId("syn300").setParentId("syn200").setUpsertKey(List.of("sampleId"))
				.setVersionLabel("1").setVersionComment("original"));

		// call under test
		writer.storeCsvAsNewRecordSetVersion(user, "syn300", "999");

		// The CSV is stored as a new version of the existing RecordSet, preserving its properties.
		ArgumentCaptor<RecordSet> recordSetCaptor = ArgumentCaptor.forClass(RecordSet.class);
		verify(entityManager).updateEntity(eq(user), recordSetCaptor.capture(), eq(true), isNull());
		RecordSet updated = recordSetCaptor.getValue();
		assertEquals("syn300", updated.getId());
		assertEquals("999", updated.getDataFileHandleId());
		assertEquals("syn200", updated.getParentId());
		assertEquals(List.of("sampleId"), updated.getUpsertKey());
		// Version label/comment cleared so the DAO assigns a unique label for the new version.
		assertNull(updated.getVersionLabel());
		assertNull(updated.getVersionComment());

		// The schema is bound by the data manager ahead of execution (entity-scoped), so the writer does
		// not re-bind it.
		verify(entityManager, never()).bindSchemaToEntity(eq(user), any());
	}
}
