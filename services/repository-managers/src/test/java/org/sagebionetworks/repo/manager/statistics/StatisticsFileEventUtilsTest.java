package org.sagebionetworks.repo.manager.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.statistics.FileEvent;

public class StatisticsFileEventUtilsTest {

	@Test
	public void testBuildFileDownloadEvent() {
		StatisticsFileEvent expectedEvent = new StatisticsFileEvent(FileEvent.FILE_DOWNLOAD, 123L, "345",
				"678", FileHandleAssociateType.FileEntity);

		FileHandleAssociation association = new FileHandleAssociation();

		association.setFileHandleId(expectedEvent.getFileHandleId());
		association.setAssociateObjectId(expectedEvent.getAssociationId());
		association.setAssociateObjectType(expectedEvent.getAssociationType());

		// Call under test
		StatisticsFileEvent result = StatisticsFileEventUtils.buildFileDownloadEvent(expectedEvent.getUserId(),
				association);

		assertEquals(expectedEvent.getActionType(), result.getActionType());
		assertEquals(expectedEvent.getAssociationId(), result.getAssociationId());
		assertEquals(expectedEvent.getAssociationType(), result.getAssociationType());
		assertEquals(expectedEvent.getFileHandleId(), result.getFileHandleId());
		assertEquals(expectedEvent.getUserId(), result.getUserId());
	}
	
	@Test
	public void testBuildFileUploadEvent() {
		StatisticsFileEvent expectedEvent = new StatisticsFileEvent(FileEvent.FILE_UPLOAD, 123L, "345",
				"678", FileHandleAssociateType.FileEntity);

		FileHandleAssociation association = new FileHandleAssociation();

		association.setFileHandleId(expectedEvent.getFileHandleId());
		association.setAssociateObjectId(expectedEvent.getAssociationId());
		association.setAssociateObjectType(expectedEvent.getAssociationType());

		// Call under test
		StatisticsFileEvent result = StatisticsFileEventUtils.buildFileUploadEvent(expectedEvent.getUserId(),
				association);

		assertEquals(expectedEvent.getActionType(), result.getActionType());
		assertEquals(expectedEvent.getAssociationId(), result.getAssociationId());
		assertEquals(expectedEvent.getAssociationType(), result.getAssociationType());
		assertEquals(expectedEvent.getFileHandleId(), result.getFileHandleId());
		assertEquals(expectedEvent.getUserId(), result.getUserId());
	}

	@Test
	public void testWithNullParams() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			FileHandleAssociation association = new FileHandleAssociation();

			association.setFileHandleId("id");
			association.setAssociateObjectId("id");
			association.setAssociateObjectType(FileHandleAssociateType.FileEntity);

			StatisticsFileEventUtils.buildFileDownloadEvent(null, association);
		});

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			FileHandleAssociation association = new FileHandleAssociation();

			association.setFileHandleId(null);
			association.setAssociateObjectId("id");
			association.setAssociateObjectType(FileHandleAssociateType.FileEntity);

			StatisticsFileEventUtils.buildFileDownloadEvent(123L, association);
		});

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			FileHandleAssociation association = new FileHandleAssociation();

			association.setFileHandleId("id");
			association.setAssociateObjectId(null);
			association.setAssociateObjectType(FileHandleAssociateType.FileEntity);

			StatisticsFileEventUtils.buildFileDownloadEvent(123L, association);
		});

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			FileHandleAssociation association = new FileHandleAssociation();

			association.setFileHandleId("id");
			association.setAssociateObjectId("id");
			association.setAssociateObjectType(null);

			StatisticsFileEventUtils.buildFileDownloadEvent(123L, association);
		});

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			StatisticsFileEventUtils.buildFileDownloadEvent(123L, null);
		});

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			StatisticsFileEventUtils.buildFileDownloadEvent(123L, null, "id", FileHandleAssociateType.FileEntity);
		});

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			StatisticsFileEventUtils.buildFileDownloadEvent(123L, "id", null, FileHandleAssociateType.FileEntity);
		});

		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			StatisticsFileEventUtils.buildFileDownloadEvent(123L, "id", "id", null);
		});
	}

}
