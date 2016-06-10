package org.sagebionetworks.repo.web.service.metadata;

import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.manager.table.TableViewManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.FileView;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;

public class FileViewMetadataProviderTest {
	
	@Mock
	TableViewManager fileViewManager;

	FileViewMetadataProvider provider;
	
	FileView view;
	UserInfo user;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		provider = new FileViewMetadataProvider();
		ReflectionTestUtils.setField(provider, "fileViewManager", fileViewManager);
		
		view = new FileView();
		view.setColumnIds(Lists.newArrayList("111","222"));
		view.setScopeIds(Lists.newArrayList("syn444"));
		view.setId("syn888");
		
		user = new UserInfo(false, 55L);
	}
	
	@Test
	public void testValidateEntityCreate(){
		provider.entityCreated(user, view);
		verify(fileViewManager).setViewSchemaAndScope(user, view.getColumnIds(), view.getScopeIds(), view.getId());
	}

	@Test
	public void testValidateEntityUpdate(){
		provider.entityUpdated(user, view);
		verify(fileViewManager).setViewSchemaAndScope(user, view.getColumnIds(), view.getScopeIds(), view.getId());
	}
	
}
