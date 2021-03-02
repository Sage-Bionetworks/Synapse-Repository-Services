package org.sagebionetworks.repo.manager.wiki;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.v2.dao.V2WikiPageDao;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.google.common.collect.ImmutableSet;

@ExtendWith(MockitoExtension.class)
public class WikiAttachmentFileHandleAssociationProviderTest {
	
	@Mock
	private V2WikiPageDao mockDao;

	@Mock
	private JdbcTemplate mockJdbcTemplate;
	
	@Mock
	private NamedParameterJdbcTemplate mockNamedJdbcTemplate;
	
	@InjectMocks
	private WikiAttachmentFileHandleAssociationProvider provider;
	
	@Test
	public void testGetFileHandleIdsAssociatedWithObject() {
		String wikiId = "syn1233";
		
		List<String> fileHandleIds = Arrays.asList("1", "2", "3");
		Set<String> expected = ImmutableSet.of("1", "2");
		
		when(mockDao.getFileHandleIdsAssociatedWithWikiAttachments(any(), any())).thenReturn(expected);
		
		// Call under test
		Set<String> associated = provider.getFileHandleIdsDirectlyAssociatedWithObject(fileHandleIds, wikiId);
		
		assertEquals(expected, associated);
		
		verify(mockDao).getFileHandleIdsAssociatedWithWikiAttachments(fileHandleIds, wikiId);
		
	}

	@Test
	public void testGetObjectTypeForAssociatedType() {
		assertEquals(ObjectType.WIKI, provider.getAuthorizationObjectTypeForAssociatedObjectType());
	}
	
	@Test
	public void testGetAssociateType() {
		assertEquals(FileHandleAssociateType.WikiAttachment, provider.getAssociateType());
	}
}
