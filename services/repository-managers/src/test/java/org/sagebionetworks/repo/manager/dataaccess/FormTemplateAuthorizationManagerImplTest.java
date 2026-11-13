package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_REALM_ID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.AuthorizationManagerImpl;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.FormTemplateDao;
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
public class FormTemplateAuthorizationManagerImplTest {

	@Mock
	private FormTemplateDao mockFormTemplateDao;

	@InjectMocks
	private FormTemplateAuthorizationManagerImpl manager;

	private UserInfo userInfo;

	@BeforeEach
	public void before() {
		userInfo = new UserInfo(false, 123L, DEFAULT_REALM_ID);
	}

	@Test
	public void testCanAccessWithDownload() {
		when(mockFormTemplateDao.exists(456L)).thenReturn(true);
		// call under test
		AuthorizationStatus status = manager.canAccess(userInfo, "456", ACCESS_TYPE.DOWNLOAD);
		assertEquals(AuthorizationStatus.authorized(), status);
	}

	@Test
	public void testCanAccessWithAnonymous() {
		when(mockFormTemplateDao.exists(456L)).thenReturn(true);
		// Anonymous is recognized per realm.
		userInfo.setRealmAnonymousUserId(userInfo.getId());
		// call under test
		AuthorizationStatus status = manager.canAccess(userInfo, "456", ACCESS_TYPE.DOWNLOAD);
		assertEquals(AuthorizationStatus.accessDenied(AuthorizationManagerImpl.ANONYMOUS_ACCESS_DENIED_REASON), status);
	}

	@Test
	public void testCanAccessWithRead() {
		when(mockFormTemplateDao.exists(456L)).thenReturn(true);
		// call under test
		AuthorizationStatus status = manager.canAccess(userInfo, "456", ACCESS_TYPE.READ);
		assertEquals(AuthorizationStatus.accessDenied("Unexpected access type READ"), status);
	}

	@Test
	public void testCanAccessWithNonExistentTemplate() {
		when(mockFormTemplateDao.exists(456L)).thenReturn(false);
		String message = assertThrows(NotFoundException.class, () -> {
			// call under test
			manager.canAccess(userInfo, "456", ACCESS_TYPE.READ);
		}).getMessage();
		assertEquals("A form template with the id '456' does not exist.", message);
		verify(mockFormTemplateDao).exists(456L);
	}
}
