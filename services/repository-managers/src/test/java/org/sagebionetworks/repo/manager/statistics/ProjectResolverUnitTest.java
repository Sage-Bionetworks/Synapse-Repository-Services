package org.sagebionetworks.repo.manager.statistics;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

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
		when(mockNodeDao.getProjectId(objectId)).thenReturn(Optional.of(projectIdString));
		
		// Call under test
		Optional<Long> projectId = projectResolver.resolveProject(FileHandleAssociateType.FileEntity, objectId);
		
		assertEquals(Long.valueOf(projectIdString), projectId.orElseThrow());
		
	}
	
	@Test
	public void testResolveProjectForUnsupportedEntity() {
		String objectId = "123";
		when(mockNodeDao.getProjectId(objectId)).thenReturn(Optional.empty());

		// Call under test
		Optional<Long> projectId = projectResolver.resolveProject(FileHandleAssociateType.WikiAttachment, objectId);

		assertEquals(null, projectId.orElse(null));
	}

}
