package org.sagebionetworks.repo.manager.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ProjectResolverUnitTest {
	
	@Mock
	NodeDAO mockNodeDao;
	
	@InjectMocks
	ProjectResolverImpl projectResolver;
	
	@Test
	public void testResolveProjectForSupportedEntity() {
		String objectId = "123";
		String projectIdString = "456";
		when(mockNodeDao.getProjectId(objectId)).thenReturn(projectIdString);
		
		// Call under test
		Long projectId = projectResolver.resolveProject(FileHandleAssociateType.FileEntity, objectId);
		
		assertEquals(Long.valueOf(projectIdString), projectId);
		
	}
	
	@Test
	public void testResolveProjectForUnsupportedEntity() {
		String objectId = "123";
		Assertions.assertThrows(UnsupportedOperationException.class, () -> {
			// Call under test
			projectResolver.resolveProject(FileHandleAssociateType.TeamAttachment, objectId);
		});		
	}

}
