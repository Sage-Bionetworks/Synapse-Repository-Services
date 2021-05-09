package org.sagebionetworks.repo.manager.file.scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;

import com.google.common.collect.ImmutableSet;

@ExtendWith(MockitoExtension.class)
public class FileHandleScannerUtilsTest {
	
	private FileHandleAssociateType associateType = FileHandleAssociateType.FileEntity;
	
	@Test
	public void testMapAssociationWithNullFileHandles() {
		Instant timestamp = Instant.now();
		Long objectId = 123L;
		
		ScannedFileHandleAssociation scanned = new ScannedFileHandleAssociation(objectId);
		
		Set<FileHandleAssociationRecord> expected = Collections.emptySet();
		
		Set<FileHandleAssociationRecord> result = FileHandleScannerUtils.mapAssociation(associateType, scanned, timestamp.toEpochMilli());
		
		assertEquals(expected, result);
		
	}
	
	@Test
	public void testMapAssociationWithEmptyFileHandles() {
		Instant timestamp = Instant.now();
		Long objectId = 123L;
		
		ScannedFileHandleAssociation scanned = new ScannedFileHandleAssociation(objectId).withFileHandleIds(Collections.emptySet());
		
		Set<FileHandleAssociationRecord> expected = Collections.emptySet();
		
		Set<FileHandleAssociationRecord> result = FileHandleScannerUtils.mapAssociation(associateType, scanned, timestamp.toEpochMilli());
		
		assertEquals(expected, result);
		
	}
	
	@Test
	public void testMapAssociationWithSingleFileHandle() {
		Instant timestamp = Instant.now();
		Long objectId = 123L;
		Long fileHandleId = 456L;
		
		ScannedFileHandleAssociation scanned = new ScannedFileHandleAssociation(objectId, fileHandleId);
		
		Set<FileHandleAssociationRecord> expected = Collections.singleton(new FileHandleAssociationRecord().withAssociateType(associateType).withTimestamp(timestamp.toEpochMilli()).withAssociateId(objectId).withFileHandleId(fileHandleId));
		
		Set<FileHandleAssociationRecord> result = FileHandleScannerUtils.mapAssociation(associateType, scanned, timestamp.toEpochMilli());
		
		assertEquals(expected, result);
		
	}
	
	@Test
	public void testMapAssociationWithMultipleFileHandles() {
		Instant timestamp = Instant.now();
		Long objectId = 123L;
		
		ScannedFileHandleAssociation scanned = new ScannedFileHandleAssociation(objectId).withFileHandleIds(ImmutableSet.of(456L, 789L));
		
		Set<FileHandleAssociationRecord> expected = ImmutableSet.of(
				new FileHandleAssociationRecord().withAssociateType(associateType).withTimestamp(timestamp.toEpochMilli()).withAssociateId(objectId).withFileHandleId(456L),
				new FileHandleAssociationRecord().withAssociateType(associateType).withTimestamp(timestamp.toEpochMilli()).withAssociateId(objectId).withFileHandleId(789L)
		);
		
		Set<FileHandleAssociationRecord> result = FileHandleScannerUtils.mapAssociation(associateType, scanned, timestamp.toEpochMilli());
		
		assertEquals(expected, result);
		
	}
	

}
