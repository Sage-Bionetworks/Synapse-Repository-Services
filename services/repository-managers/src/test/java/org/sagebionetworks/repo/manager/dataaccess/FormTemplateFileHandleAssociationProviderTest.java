package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.FormTemplateDao;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;

@ExtendWith(MockitoExtension.class)
public class FormTemplateFileHandleAssociationProviderTest {

	@Mock
	private FormTemplateDao mockFormTemplateDao;

	@InjectMocks
	private FormTemplateFileHandleAssociationProvider provider;

	@Test
	public void testGetAssociateType() {
		// call under test
		assertEquals(FileHandleAssociateType.FormTemplateAttachment, provider.getAssociateType());
	}

	@Test
	public void testGetAuthorizationObjectTypeForAssociatedObjectType() {
		// call under test
		assertEquals(ObjectType.FORM_TEMPLATE, provider.getAuthorizationObjectTypeForAssociatedObjectType());
	}

	@Test
	public void testGetFileHandleIdsDirectlyAssociatedWithObject() {
		List<String> fileHandleIds = List.of("987", "654", "321");
		when(mockFormTemplateDao.getAttachmentFileHandleIds(123L, fileHandleIds)).thenReturn(Set.of("987", "321"));

		// call under test
		Set<String> associated = provider.getFileHandleIdsDirectlyAssociatedWithObject(fileHandleIds, "123");

		assertEquals(Set.of("987", "321"), associated);
	}

	@Test
	public void testGetFileHandleIdsDirectlyAssociatedWithObjectWithNoneAssociated() {
		List<String> fileHandleIds = List.of("987");
		when(mockFormTemplateDao.getAttachmentFileHandleIds(123L, fileHandleIds)).thenReturn(Set.of());

		// call under test
		Set<String> associated = provider.getFileHandleIdsDirectlyAssociatedWithObject(fileHandleIds, "123");

		assertEquals(Set.of(), associated);
	}

	@Test
	public void testGetFileHandleIdsDirectlyAssociatedWithObjectWithNonNumericObjectId() {
		assertThrows(NumberFormatException.class, () -> {
			// call under test
			provider.getFileHandleIdsDirectlyAssociatedWithObject(List.of("987"), "syn123");
		});

		verifyNoInteractions(mockFormTemplateDao);
	}
}
