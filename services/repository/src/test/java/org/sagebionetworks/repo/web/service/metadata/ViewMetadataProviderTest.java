package org.sagebionetworks.repo.web.service.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.table.TableViewManager;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.table.View;
import org.sagebionetworks.repo.model.table.ViewScope;

import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
public class ViewMetadataProviderTest {

	@Mock
	private TableViewManager mockViewManager;

	private ViewMetadataProvider<View> provider;
	
	@Mock
	private UserInfo mockUser;
	
	@Mock
	private View mockEntity;

	@Mock
	private ViewScope mockViewScope;
	
	@SuppressWarnings("unchecked")
	@BeforeEach
	public void before() {
		provider = Mockito.mock(ViewMetadataProvider.class,
				withSettings()
					.useConstructor(mockViewManager)
					.defaultAnswer(Answers.CALLS_REAL_METHODS)
			);
	}
	
	@Test
	public void testEntityCreated() {
		String id = "123";
		List<String> schema = ImmutableList.of("1", "2", "3");
		
		when(provider.createViewScope(any(), any())).thenReturn(mockViewScope);
		when(mockEntity.getId()).thenReturn(id);
		when(mockEntity.getColumnIds()).thenReturn(schema);
		
		// Call under test
		provider.entityCreated(mockUser, mockEntity);
		
		verify(provider).createViewScope(mockUser, mockEntity);
		verify(mockViewManager).setViewSchemaAndScope(mockUser, schema, mockViewScope, id);
	}
	
	@Test
	public void testEntityUpdated() {
		String id = "123";
		List<String> schema = ImmutableList.of("1", "2", "3");
		boolean newVersionCreated = false;
		
		when(provider.createViewScope(any(), any())).thenReturn(mockViewScope);
		when(mockEntity.getId()).thenReturn(id);
		when(mockEntity.getColumnIds()).thenReturn(schema);
		
		// Call under test
		provider.entityUpdated(mockUser, mockEntity, newVersionCreated);
		
		verify(provider).createViewScope(mockUser, mockEntity);
		verify(mockViewManager).setViewSchemaAndScope(mockUser, schema, mockViewScope, id);
	}
	
	@Test
	public void testEntityUpdatedWithNewVersionCreated() {
		boolean newVersionCreated = true;
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			provider.entityUpdated(mockUser, mockEntity, newVersionCreated);
		}).getMessage();

		assertEquals("A view version can only be created by creating a view snapshot.", errorMessage);
		verifyNoMoreInteractions(mockViewManager);
	}
	
	@Test
	public void testAddTypeSpecificMetadata() {
		Long id = 123L;
		Long version = 1L;
		
		IdAndVersion idAndVersion = IdAndVersion.newBuilder()
				.setId(id)
				.setVersion(version)
				.build();
		
		List<String> schema = ImmutableList.of("1", "2", "3");
		EventType eventType = null; // unused
		
		when(mockEntity.getId()).thenReturn(id.toString());
		when(mockEntity.getVersionNumber()).thenReturn(version);
		when(mockViewManager.getViewSchemaIds(any())).thenReturn(schema);

		// Call under test
		provider.addTypeSpecificMetadata(mockEntity, mockUser, eventType);

		verify(mockEntity).getId();
		verify(mockEntity).getVersionNumber();
		verify(mockViewManager).getViewSchemaIds(idAndVersion);
		verify(mockEntity).setColumnIds(schema);
	}
}
