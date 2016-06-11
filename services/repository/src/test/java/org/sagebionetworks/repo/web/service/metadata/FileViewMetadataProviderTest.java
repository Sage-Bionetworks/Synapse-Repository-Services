package org.sagebionetworks.repo.web.service.metadata;

import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.manager.table.TableViewManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.ViewType;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;

public class FileViewMetadataProviderTest {
	
	@Mock
	TableViewManager fileViewManager;

	EntityViewMetadataProvider provider;
	
	EntityView view;
	UserInfo user;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		provider = new EntityViewMetadataProvider();
		ReflectionTestUtils.setField(provider, "fileViewManager", fileViewManager);
		
		view = new EntityView();
		view.setColumnIds(Lists.newArrayList("111","222"));
		view.setScopeIds(Lists.newArrayList("syn444"));
		view.setId("syn888");
		view.setType(ViewType.file);
		
		user = new UserInfo(false, 55L);
	}
	
	@Test
	public void testValidateEntityCreate(){
		provider.entityCreated(user, view);
		verify(fileViewManager).setViewSchemaAndScope(user, view.getColumnIds(), view.getScopeIds(), view.getType(), view.getId());
	}

	@Test
	public void testValidateEntityUpdate(){
		provider.entityUpdated(user, view);
		verify(fileViewManager).setViewSchemaAndScope(user, view.getColumnIds(), view.getScopeIds(), view.getType(), view.getId());
	}
	
}
