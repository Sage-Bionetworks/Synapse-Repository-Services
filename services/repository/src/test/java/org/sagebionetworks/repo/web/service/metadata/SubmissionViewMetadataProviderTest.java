package org.sagebionetworks.repo.web.service.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.evaluation.EvaluationPermissionsManager;
import org.sagebionetworks.repo.manager.table.TableViewManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.table.SubmissionView;
import org.sagebionetworks.repo.model.table.ViewEntityType;
import org.sagebionetworks.repo.model.table.ViewScope;

import com.google.common.collect.ImmutableList;

@ExtendWith(MockitoExtension.class)
public class SubmissionViewMetadataProviderTest {
	
	@Mock
	private EvaluationPermissionsManager mockPermissionManager;
	
	@Mock
	private TableViewManager mockViewManager;
	
	@InjectMocks
	private SubmissionViewMetadataProvider provider;

	@Mock
	private UserInfo mockUser;
	
	@Mock
	private SubmissionView mockSubmissionView;
	
	@Mock
	private EntityEvent mockEntityEvent;

	private ViewScope scope;
	
	@BeforeEach
	public void before() {
		List<String> viewScope = ImmutableList.of("1", "2", "3");
		Long viewTypeMask = 0L;
		
		scope = new ViewScope();
		
		scope.setViewTypeMask(viewTypeMask);
		scope.setScope(viewScope);
		scope.setViewEntityType(ViewEntityType.submissionview);
	}
	
	@Test
	public void testCreateViewScope() {
		when(mockSubmissionView.getScopeIds()).thenReturn(scope.getScope());
				
		// Call under test
		ViewScope result = provider.createViewScope(mockUser, mockSubmissionView);
	
		assertEquals(scope, result);
	}
	
	@Test
	public void testValidateEntity() {
		when(mockEntityEvent.getUserInfo()).thenReturn(mockUser);
		when(mockSubmissionView.getScopeIds()).thenReturn(scope.getScope());
		when(mockPermissionManager.hasAccess(any(), any(), anyList())).thenReturn(AuthorizationStatus.authorized());
		
		// Call under test
		provider.validateEntity(mockSubmissionView, mockEntityEvent);
		
		verify(mockPermissionManager).hasAccess(mockUser, ACCESS_TYPE.READ_PRIVATE_SUBMISSION, scope.getScope());	
	}
	
	@Test
	public void testValidateEntityWithNoEvaluations() {
		List<String> evaluations = Collections.emptyList();
		scope.setScope(evaluations);
		
		when(mockEntityEvent.getUserInfo()).thenReturn(mockUser);
		when(mockSubmissionView.getScopeIds()).thenReturn(evaluations);
		
		// Call under test
		provider.validateEntity(mockSubmissionView, mockEntityEvent);
		
		verifyZeroInteractions(mockPermissionManager);
	}
}
