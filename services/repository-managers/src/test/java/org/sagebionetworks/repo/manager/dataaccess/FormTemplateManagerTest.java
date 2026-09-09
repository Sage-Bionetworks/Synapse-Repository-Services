package org.sagebionetworks.repo.manager.dataaccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplate;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplateSearchRequest;
import org.sagebionetworks.repo.model.dataaccess.schema.FormTemplateSearchResponse;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.FormTemplateDao;
import org.sagebionetworks.repo.model.dbo.dao.dataaccess.FormTemplateInfoForUpdate;
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
public class FormTemplateManagerTest {

	private static final Long TEMPLATE_ID = 123L;
	private static final String ETAG = "e6bf1a02-4e5a-4a3f-8f7f-3f0a4a1f5e01";

	@Mock
	private FormTemplateDao mockFormTemplateDao;

	@Mock
	private FormTemplateValidator mockFormTemplateValidator;

	@Mock
	private AuthorizationManager mockAuthorizationManager;

	@InjectMocks
	private FormTemplateManager manager;

	private UserInfo userInfo;
	private FormTemplate template;

	@BeforeEach
	public void before() {
		userInfo = new UserInfo(false, 555L, AuthorizationConstants.DEFAULT_REALM_ID);
		template = new FormTemplate().setName("NF Standard DAR")
				.setSchema$id("org.sagebionetworks.test-DataAccessRequest-1.0.0");
	}

	@Test
	public void testCreate() {
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		FormTemplate created = new FormTemplate().setId(TEMPLATE_ID.toString());
		when(mockFormTemplateDao.create(userInfo.getId(), template)).thenReturn(created);

		// call under test
		assertSame(created, manager.create(userInfo, template));

		verify(mockFormTemplateValidator).validate(template);
	}

	@Test
	public void testCreateWithNonACTUser() {
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(false);

		String message = assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.create(userInfo, template);
		}).getMessage();

		assertEquals("Only ACT members may create or update a form template.", message);
		verifyNoInteractions(mockFormTemplateValidator, mockFormTemplateDao);
	}

	@Test
	public void testCreateWithNullUserInfo() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.create(null, template);
		}).getMessage();

		assertEquals("userInfo is required.", message);
		verifyNoInteractions(mockFormTemplateValidator, mockFormTemplateDao, mockAuthorizationManager);
	}

	@Test
	public void testCreateWithNullTemplate() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.create(userInfo, null);
		}).getMessage();

		assertEquals("template is required.", message);
		verifyNoInteractions(mockFormTemplateValidator, mockFormTemplateDao, mockAuthorizationManager);
	}

	@Test
	public void testCreateNewVersion() {
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		when(mockFormTemplateDao.getForUpdate(TEMPLATE_ID))
				.thenReturn(Optional.of(new FormTemplateInfoForUpdate(TEMPLATE_ID, ETAG, 3L)));
		FormTemplate created = new FormTemplate().setId(TEMPLATE_ID.toString()).setVersionNumber(4L);
		when(mockFormTemplateDao.createNewVersion(eq(userInfo.getId()), any())).thenReturn(created);
		template.setId(TEMPLATE_ID.toString()).setEtag(ETAG);

		// call under test
		assertSame(created, manager.createNewVersion(userInfo, template));

		verify(mockFormTemplateValidator).validate(template);
		// The new version is derived from the version that is current, not from the requested body.
		verify(mockFormTemplateDao).createNewVersion(userInfo.getId(), template.setVersionNumber(4L));
	}

	@Test
	public void testCreateNewVersionWithStaleEtag() {
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		when(mockFormTemplateDao.getForUpdate(TEMPLATE_ID))
				.thenReturn(Optional.of(new FormTemplateInfoForUpdate(TEMPLATE_ID, ETAG, 3L)));
		template.setId(TEMPLATE_ID.toString()).setEtag("a-stale-etag");

		String message = assertThrows(ConflictingUpdateException.class, () -> {
			// call under test
			manager.createNewVersion(userInfo, template);
		}).getMessage();

		assertEquals("The form template was updated since you last fetched it, retrieve it again and reapply"
				+ " the update.", message);
		verify(mockFormTemplateDao, never()).createNewVersion(anyLong(), any());
	}

	@Test
	public void testCreateNewVersionWithNonExistentId() {
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		when(mockFormTemplateDao.getForUpdate(TEMPLATE_ID)).thenReturn(Optional.empty());
		template.setId(TEMPLATE_ID.toString()).setEtag(ETAG);

		String message = assertThrows(NotFoundException.class, () -> {
			// call under test
			manager.createNewVersion(userInfo, template);
		}).getMessage();

		assertEquals("A form template with the id '123' does not exist.", message);
		verify(mockFormTemplateDao, never()).createNewVersion(anyLong(), any());
	}

	@Test
	public void testCreateNewVersionWithNonACTUser() {
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(false);
		template.setId(TEMPLATE_ID.toString()).setEtag(ETAG);

		assertThrows(UnauthorizedException.class, () -> {
			// call under test
			manager.createNewVersion(userInfo, template);
		});

		verifyNoInteractions(mockFormTemplateValidator, mockFormTemplateDao);
	}

	@Test
	public void testCreateNewVersionWithNullId() {
		template.setEtag(ETAG);

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.createNewVersion(userInfo, template);
		}).getMessage();

		assertEquals("template.id is required and must not be the empty string.", message);
		verifyNoInteractions(mockFormTemplateValidator, mockFormTemplateDao, mockAuthorizationManager);
	}

	@Test
	public void testCreateNewVersionWithNullEtag() {
		template.setId(TEMPLATE_ID.toString());

		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.createNewVersion(userInfo, template);
		}).getMessage();

		assertEquals("template.etag is required and must not be the empty string.", message);
		verifyNoInteractions(mockFormTemplateValidator, mockFormTemplateDao, mockAuthorizationManager);
	}

	@Test
	public void testCreateNewVersionWithNonNumericId() {
		when(mockAuthorizationManager.isACTTeamMemberOrAdmin(userInfo)).thenReturn(true);
		template.setId("not-a-number").setEtag(ETAG);

		assertThrows(NumberFormatException.class, () -> {
			// call under test
			manager.createNewVersion(userInfo, template);
		});

		verifyNoInteractions(mockFormTemplateDao);
	}

	@Test
	public void testGetLatestVersion() {
		FormTemplate latest = new FormTemplate().setId(TEMPLATE_ID.toString()).setVersionNumber(3L);
		when(mockFormTemplateDao.getLatestVersion(TEMPLATE_ID)).thenReturn(Optional.of(latest));

		// call under test
		assertSame(latest, manager.getLatestVersion(TEMPLATE_ID.toString()));
	}

	@Test
	public void testGetLatestVersionWithNonExistentId() {
		when(mockFormTemplateDao.getLatestVersion(TEMPLATE_ID)).thenReturn(Optional.empty());

		String message = assertThrows(NotFoundException.class, () -> {
			// call under test
			manager.getLatestVersion(TEMPLATE_ID.toString());
		}).getMessage();

		assertEquals("A form template with the id '123' does not exist.", message);
	}

	@Test
	public void testGetLatestVersionWithNullId() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.getLatestVersion(null);
		}).getMessage();

		assertEquals("id is required and must not be the empty string.", message);
		verifyNoInteractions(mockFormTemplateDao);
	}

	@Test
	public void testGetVersion() {
		FormTemplate version = new FormTemplate().setId(TEMPLATE_ID.toString()).setVersionNumber(2L);
		when(mockFormTemplateDao.getVersion(TEMPLATE_ID, 2L)).thenReturn(Optional.of(version));

		// call under test
		assertSame(version, manager.getVersion(TEMPLATE_ID.toString(), 2L));
	}

	@Test
	public void testGetVersionWithNonExistentVersion() {
		when(mockFormTemplateDao.getVersion(TEMPLATE_ID, 2L)).thenReturn(Optional.empty());

		String message = assertThrows(NotFoundException.class, () -> {
			// call under test
			manager.getVersion(TEMPLATE_ID.toString(), 2L);
		}).getMessage();

		assertEquals("Version 2 of the form template with the id '123' does not exist.", message);
	}

	@Test
	public void testGetVersionWithNullVersionNumber() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.getVersion(TEMPLATE_ID.toString(), null);
		}).getMessage();

		assertEquals("versionNumber is required.", message);
		verifyNoInteractions(mockFormTemplateDao);
	}

	@Test
	public void testSearch() {
		List<FormTemplate> page = newTemplates(2);
		when(mockFormTemplateDao.searchLatestVersions(null, false, NextPageToken.DEFAULT_LIMIT + 1, 0L))
				.thenReturn(page);

		// call under test
		FormTemplateSearchResponse response = manager.search(new FormTemplateSearchRequest());

		assertEquals(new FormTemplateSearchResponse().setResults(page), response);
	}

	@Test
	public void testSearchWithFilters() {
		when(mockFormTemplateDao.searchLatestVersions("NF", true, NextPageToken.DEFAULT_LIMIT + 1, 0L))
				.thenReturn(newTemplates(1));

		// call under test
		manager.search(new FormTemplateSearchRequest().setName("NF").setIncludeDeprecated(true));

		verify(mockFormTemplateDao).searchLatestVersions("NF", true, NextPageToken.DEFAULT_LIMIT + 1, 0L);
	}

	@Test
	public void testSearchWithMorePages() {
		// One more result than the page holds signals that there is a next page.
		List<FormTemplate> page = newTemplates((int) NextPageToken.DEFAULT_LIMIT + 1);
		when(mockFormTemplateDao.searchLatestVersions(null, false, NextPageToken.DEFAULT_LIMIT + 1, 0L))
				.thenReturn(page);

		// call under test
		FormTemplateSearchResponse response = manager.search(new FormTemplateSearchRequest());

		assertEquals("50a50", response.getNextPageToken());
		assertEquals(NextPageToken.DEFAULT_LIMIT, response.getResults().size());
	}

	@Test
	public void testSearchWithNextPageToken() {
		when(mockFormTemplateDao.searchLatestVersions(null, false, NextPageToken.DEFAULT_LIMIT + 1,
				NextPageToken.DEFAULT_LIMIT)).thenReturn(newTemplates(1));

		// call under test
		FormTemplateSearchResponse response = manager.search(new FormTemplateSearchRequest().setNextPageToken("50a50"));

		assertEquals(null, response.getNextPageToken());
	}

	@Test
	public void testSearchWithNullRequest() {
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			manager.search(null);
		}).getMessage();

		assertEquals("request is required.", message);
		verify(mockFormTemplateDao, never()).searchLatestVersions(any(), anyBoolean(), anyLong(), anyLong());
	}

	/**
	 * The pagination helper trims the returned list in place, so the list must be mutable.
	 */
	private static List<FormTemplate> newTemplates(int count) {
		return new ArrayList<>(LongStream.range(0, count)
				.mapToObj(index -> new FormTemplate().setId(Long.toString(index)).setVersionNumber(1L)).toList());
	}
}
