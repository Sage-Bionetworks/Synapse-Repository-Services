package org.sagebionetworks.repo.manager.form;


import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.form.FormDao;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
public class FormFileHandleAssociationProviderTest {
	
	@Mock
	private FormDao mockFormDao;

	@Mock
	private JdbcTemplate mockJdbcTemplate;
	
	@Mock
	private NamedParameterJdbcTemplate mockNamedJdbcTemplate;
	
	@InjectMocks
	private FormFileHandleAssociationProvider provider;
	
	@Test
	public void testgetAuthorizationObjectTypeForAssociatedObjectType() {
		// call under test
		assertEquals(ObjectType.FORM_DATA,  provider.getAuthorizationObjectTypeForAssociatedObjectType());
	}
	
	@Test
	public void testGetAssociateType() {
		assertEquals(FileHandleAssociateType.FormData, provider.getAssociateType());
	}
	
	@Test
	public void testGetFileHandleIdsDirectlyAssociatedWithObject() {
		List<String> handles = Lists.newArrayList("123", "456");
		String formDataId = "888";
		when(mockFormDao.getFormDataFileHandleId(formDataId)).thenReturn("456");
		// call under test
		Set<String> results = provider.getFileHandleIdsDirectlyAssociatedWithObject(handles, formDataId);
		assertEquals(Sets.newHashSet("456"), results);
	}
	
	@Test
	public void testGetFileHandleIdsDirectlyAssociatedWithObjectNot() {
		List<String> handles = Lists.newArrayList("123", "456");
		String formDataId = "888";
		// no match.
		when(mockFormDao.getFormDataFileHandleId(formDataId)).thenReturn("333");
		// call under test
		Set<String> results = provider.getFileHandleIdsDirectlyAssociatedWithObject(handles, formDataId);
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}
	
	@Test
	public void testGetFileHandleIdsDirectlyAssociatedWithObjectNullList() {
		List<String> handles = null;
		String formDataId = "888";
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			provider.getFileHandleIdsDirectlyAssociatedWithObject(handles, formDataId);
		});
	}
	
	@Test
	public void testGetFileHandleIdsDirectlyAssociatedWithObjectNullId() {
		List<String> handles = Lists.newArrayList("123", "456");
		String formDataId = null;
		assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			provider.getFileHandleIdsDirectlyAssociatedWithObject(handles, formDataId);
		});
	}

}
