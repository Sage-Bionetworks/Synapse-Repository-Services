package org.sagebionetworks.repo.manager.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.file.scanner.FileHandleAssociationScanner;
import org.sagebionetworks.repo.manager.file.scanner.ScannedFileHandleAssociation;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.IdRange;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class FileHandleAssociationManagerImplTest {

	@Mock
	private FileHandleAssociationProvider mockProvider;
	
	@Mock
	private FileHandleAssociationScanner mockScanner;

	@Mock
	private FileHandleDao mockFileHandleDao;

	@InjectMocks
	private FileHandleAssociationManagerImpl fileHandleAssociationManager;

	@BeforeEach
	public void before() {
		fileHandleAssociationManager.configureProviderMap(Collections.singletonMap(FileHandleAssociateType.TableEntity, mockProvider));
		fileHandleAssociationManager.configureScannerMap(Collections.singletonMap(FileHandleAssociateType.TableEntity, mockScanner));
	}

	@Test
	public void testGetObjectTypeForAssociatedType() {
		when(mockProvider.getAuthorizationObjectTypeForAssociatedObjectType()).thenReturn(ObjectType.ENTITY);
		assertEquals(ObjectType.ENTITY,
				fileHandleAssociationManager.getAuthorizationObjectTypeForAssociatedObjectType(FileHandleAssociateType.TableEntity));
	}

	@Test
	public void testGetFileHandleIdsAssociatedWithObject() {
		Set<String> sample = Sets.newHashSet("1");
		when(mockProvider.getFileHandleIdsDirectlyAssociatedWithObject(anyList(), anyString())).thenReturn(sample);
		assertEquals(sample, fileHandleAssociationManager.getFileHandleIdsAssociatedWithObject(Arrays.asList("1"), "456",
				FileHandleAssociateType.TableEntity));
	}
	
	@Test
	public void testGetFileHandleIdsAssociatedWithObjectIncludingPreviews() {
		// The first is a file handle, the second is its preview
		List<String> fileHandleIds = Arrays.asList("1", "2");
		
		when(mockFileHandleDao.getFileHandlePreviewIds(fileHandleIds)).thenReturn(ImmutableMap.of("2", "1"));
		
		when(mockProvider.getFileHandleIdsDirectlyAssociatedWithObject(anyList(), anyString())).thenReturn(ImmutableSet.of("1"));
		
		Set<String> result = fileHandleAssociationManager.getFileHandleIdsAssociatedWithObject(fileHandleIds, "123", FileHandleAssociateType.TableEntity);
		
		verify(mockFileHandleDao, times(1)).getFileHandlePreviewIds(fileHandleIds);
		verify(mockProvider, times(1)).getFileHandleIdsDirectlyAssociatedWithObject(anyList(), anyString());
		
		assertEquals(ImmutableSet.copyOf(fileHandleIds), result);
	}
	
	@Test
	public void testGetFileHandleIdsAssociatedWithObjectWithAnotherPreview() {
		// These are all previews, the second is not associated with the object
		List<String> fileHandleIds = Arrays.asList("1", "3");
		
		when(mockFileHandleDao.getFileHandlePreviewIds(fileHandleIds)).thenReturn(ImmutableMap.of("1", "2", "3", "4"));
		when(mockProvider.getFileHandleIdsDirectlyAssociatedWithObject(anyList(), anyString())).thenReturn(ImmutableSet.of("2"));
		
		Set<String> result = fileHandleAssociationManager.getFileHandleIdsAssociatedWithObject(fileHandleIds, "123", FileHandleAssociateType.TableEntity);
		
		verify(mockFileHandleDao, times(1)).getFileHandlePreviewIds(fileHandleIds);
		verify(mockProvider, times(1)).getFileHandleIdsDirectlyAssociatedWithObject(anyList(), anyString());
		
		assertEquals(ImmutableSet.of("1"), result);
	}
	
	@Test
	public void testGetFileHandleIdsAssociatedWithObjectWithOnlyPreviews() {
		// These are all previews associated with the object
		List<String> fileHandleIds = Arrays.asList("1", "3");
		
		when(mockFileHandleDao.getFileHandlePreviewIds(fileHandleIds)).thenReturn(ImmutableMap.of("1", "2", "3", "4"));

		when(mockProvider.getFileHandleIdsDirectlyAssociatedWithObject(anyList(), anyString())).thenReturn(ImmutableSet.of("2", "4"));
		
		Set<String> result = fileHandleAssociationManager.getFileHandleIdsAssociatedWithObject(fileHandleIds, "123", FileHandleAssociateType.TableEntity);
		
		verify(mockFileHandleDao, times(1)).getFileHandlePreviewIds(fileHandleIds);
		verify(mockProvider, times(1)).getFileHandleIdsDirectlyAssociatedWithObject(anyList(), anyString());
		
		assertEquals(ImmutableSet.copyOf(fileHandleIds), result);
	}
	
	@Test
	public void testGetFileHandleIdsAssociatedWithObjectWithNoPreview() {
		// These are handles, no preview
		List<String> fileHandleIds = Arrays.asList("1", "2");
		
		when(mockFileHandleDao.getFileHandlePreviewIds(fileHandleIds)).thenReturn(Collections.emptyMap());
		when(mockProvider.getFileHandleIdsDirectlyAssociatedWithObject(anyList(), anyString())).thenReturn(ImmutableSet.of("1", "2"));
		
		Set<String> result = fileHandleAssociationManager.getFileHandleIdsAssociatedWithObject(fileHandleIds, "123", FileHandleAssociateType.TableEntity);
		
		verify(mockFileHandleDao, times(1)).getFileHandlePreviewIds(anyList());
		verify(mockProvider, times(1)).getFileHandleIdsDirectlyAssociatedWithObject(anyList(), anyString());
		
		assertEquals(ImmutableSet.copyOf(fileHandleIds), result);
	}
	
	@Test
	public void testGetIdRange() {

		IdRange expected = new IdRange(1, 10);
		
		when(mockScanner.getIdRange()).thenReturn(expected);
		
		// Call under test
		IdRange result = fileHandleAssociationManager.getIdRange(FileHandleAssociateType.TableEntity);
		
		assertEquals(expected, result);
		
		verify(mockScanner).getIdRange();
		
	}
	
	@Test
	public void testGetMaxIdRangeSize() {
		
		long expectedMaxIdRange = 10;
		
		when(mockScanner.getMaxIdRangeSize()).thenReturn(expectedMaxIdRange);
		
		// Call under test
		long result = fileHandleAssociationManager.getMaxIdRangeSize(FileHandleAssociateType.TableEntity);
		
		assertEquals(expectedMaxIdRange, result);
		
	}
	
	public void testScanRange() {
		
		Iterable<ScannedFileHandleAssociation> expected = new ArrayList<>();
		
		when(mockScanner.scanRange(any())).thenReturn(expected);
		
		IdRange range = new IdRange(1, 10);
		
		// Call under test
		Iterable<ScannedFileHandleAssociation> result = fileHandleAssociationManager.scanRange(FileHandleAssociateType.TableEntity, range);
		
		assertEquals(expected, result);
		
		verify(mockScanner).scanRange(range);
		
	}

}
