package org.sagebionetworks.repo.manager.table.metadata.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.evaluation.dao.EvaluationDAO;
import org.sagebionetworks.evaluation.dao.SubmissionDAO;
import org.sagebionetworks.evaluation.dao.SubmissionField;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.repo.manager.evaluation.SubmissionManager;
import org.sagebionetworks.repo.manager.table.metadata.DefaultColumnModel;
import org.sagebionetworks.repo.model.IdAndEtag;
import org.sagebionetworks.repo.model.LimitExceededException;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.dbo.dao.table.ViewScopeDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ObjectField;
import org.sagebionetworks.repo.model.table.ReplicationType;
import org.sagebionetworks.repo.model.table.SubType;
import org.sagebionetworks.repo.model.table.TableConstants;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.repo.model.table.ViewTypeMask;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldTypeMapper;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.sagebionetworks.table.cluster.view.filter.HierarchicaFilter;
import org.sagebionetworks.table.cluster.view.filter.ViewFilter;

@ExtendWith(MockitoExtension.class)
public class SubmissionMetadataIndexProviderUnitTest {

	@Mock
	private SubmissionManager mockSubmissionManager;

	@Mock
	private SubmissionDAO mockSubmissionDao;

	@Mock
	private EvaluationDAO mockEvaluationDao;

	@InjectMocks
	private SubmissionMetadataIndexProvider provider;

	@Mock
	private ObjectDataDTO mockData;

	@Mock
	private UserInfo mockUser;

	@Mock
	private Annotations mockAnnotations;

	@Mock
	private ColumnModel mockModel;

	@Mock
	private IdAndEtag mockIdAndEtag;

	@Mock
	private SubmissionStatus mockSubmissionStatus;

	private Long mockViewTypeMask;

    @Mock
    private ViewScopeDao mockViewScopeDao;

	private ViewObjectType viewObjectType = ViewObjectType.SUBMISSION;

	@BeforeEach
	public void before() {
		// Set it to null so that we know that there is no interaction
		mockViewTypeMask = null;
	}

	@Test
	public void testGetObjectType() {
		// Call under test
		assertEquals(viewObjectType, provider.getObjectType());
	}

	@Test
	public void testObjectFieldTypeMapper() {
		ObjectFieldTypeMapper mapper = provider;

		// Call under test
		assertEquals(ColumnType.SUBMISSIONID, mapper.getIdColumnType());
		assertEquals(ColumnType.EVALUATIONID, mapper.getParentIdColumnType());
		assertEquals(ColumnType.EVALUATIONID, mapper.getBenefactorIdColumnType());
	}

	@Test
	public void testGetAnnotations() {
		String objectId = "syn123";

		when(mockSubmissionStatus.getSubmissionAnnotations()).thenReturn(mockAnnotations);
		when(mockSubmissionManager.getSubmissionStatus(any(), any())).thenReturn(mockSubmissionStatus);

		// Call under test
		Optional<Annotations> result = provider.getAnnotations(mockUser, objectId);

		assertTrue(result.isPresent());
		assertEquals(mockAnnotations, result.get());

		verify(mockSubmissionManager).getSubmissionStatus(mockUser, KeyFactory.stringToKey(objectId).toString());
		verify(mockSubmissionStatus).getSubmissionAnnotations();
	}

	@Test
	public void testGetAnnotationsWithNoUser() {
		mockUser = null;
		String objectId = "syn123";

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			provider.getAnnotations(mockUser, objectId);
		}).getMessage();

		assertEquals("The user is required.", errorMessage);
	}

	@Test
	public void testGetAnnotationsWithNoObjectId() {
		String objectId = null;

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			provider.getAnnotations(mockUser, objectId);
		}).getMessage();

		assertEquals("The object id is required.", errorMessage);
	}

	@Test
	public void testUpdateAnnotations() {
		String objectId = "syn123";
		String etag = "etag";

		Annotations updatedAnnotations = Mockito.mock(Annotations.class);

		when(updatedAnnotations.getEtag()).thenReturn(etag);

		when(mockSubmissionManager.getSubmissionStatus(any(), any())).thenReturn(mockSubmissionStatus);

		when(mockSubmissionManager.updateSubmissionStatus(any(), any())).thenReturn(mockSubmissionStatus);

		// Call under test
		provider.updateAnnotations(mockUser, objectId, updatedAnnotations);

		verify(mockSubmissionManager).getSubmissionStatus(mockUser, KeyFactory.stringToKey(objectId).toString());
		// Verify that we sync the etag from the input annotations before saving
		verify(mockSubmissionStatus).setEtag(etag);

		verify(mockSubmissionStatus).setSubmissionAnnotations(updatedAnnotations);
		verify(mockSubmissionManager).updateSubmissionStatus(mockUser, mockSubmissionStatus);
	}

	@Test
	public void testUpdateAnnotationsWithNoUser() {
		when(mockAnnotations.getEtag()).thenReturn("etag");
		String objectId = "syn123";

		mockUser = null;

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			provider.updateAnnotations(mockUser, objectId, mockAnnotations);
		}).getMessage();

		assertEquals("The user is required.", errorMessage);
	}

	@Test
	public void testUpdateAnnotationsWithNoObjectId() {
		when(mockAnnotations.getEtag()).thenReturn("etag");

		String objectId = null;

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			provider.updateAnnotations(mockUser, objectId, mockAnnotations);
		}).getMessage();

		assertEquals("The object id is required.", errorMessage);
	}

	@Test
	public void testUpdateAnnotationsWithNoAnnotations() {
		String objectId = "syn123";
		mockAnnotations = null;

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			provider.updateAnnotations(mockUser, objectId, mockAnnotations);
		}).getMessage();

		assertEquals("The annotations is required.", errorMessage);
	}

	@Test
	public void testUpdateAnnotationsWithNoEtag() {
		when(mockAnnotations.getEtag()).thenReturn(null);

		String objectId = "syn123";

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			provider.updateAnnotations(mockUser, objectId, mockAnnotations);
		}).getMessage();

		assertEquals("The annotations etag is required.", errorMessage);
	}

	@Test
	public void testCanUpdateAnnotation() {

		// Cannot update any of the submission fields
		for (SubmissionField field : SubmissionField.values()) {

			when(mockModel.getName()).thenReturn(field.getColumnName());

			// Call under test
			boolean result = provider.canUpdateAnnotation(mockModel);

			assertFalse(result);

		}

		String annotationKey = "someAnnotation";

		when(mockModel.getName()).thenReturn(annotationKey);

		boolean result = provider.canUpdateAnnotation(mockModel);

		assertTrue(result);
	}

	@Test
	public void testDefaultColumnModel() {
		// Call under test
		DefaultColumnModel model = provider.getDefaultColumnModel(mockViewTypeMask);

		List<ObjectField> expectedFields = ImmutableList.of(ObjectField.id, ObjectField.name, ObjectField.createdOn,
				ObjectField.createdBy, ObjectField.etag, ObjectField.modifiedOn, ObjectField.projectId);

		assertNotNull(model);
		assertEquals(expectedFields, model.getDefaultFields());
		assertEquals(SubmissionField.values().length, model.getCustomFields().size());

		for (SubmissionField field : SubmissionField.values()) {
			boolean present = model.findCustomFieldByColumnName(field.getColumnName()).isPresent();
			assertTrue(present);
		}
	}	

	@Test
	public void testValidateScopeAndTypeWithUnderLimit() {
		Long typeMask = 0L;
		Set<Long> scopeIds = Sets.newHashSet(1L, 2L);
		int maxContainersPerView = 3;
		// call under test
		provider.validateScopeAndType(typeMask, scopeIds, maxContainersPerView);
	}

	@Test
	public void testValidateScopeAndTypeWithOverLimit() {
		Long typeMask = 0L;
		Set<Long> scopeIds = Sets.newHashSet(1L, 2L);
		int maxContainersPerView = 1;
		String message = assertThrows(IllegalArgumentException.class, () -> {
			// call under test
			provider.validateScopeAndType(typeMask, scopeIds, maxContainersPerView);
		}).getMessage();
		assertEquals("The view's scope exceeds the maximum number of 1 evaluations.", message);
	}

    @Test
    public void testGetViewFilterWithFilterGetScopeCalled() throws LimitExceededException {
        long viewTypeMask = ViewTypeMask.File.getMask();
		Set<Long> fullScope = Sets.newHashSet(1L,2L,3L);
		when(mockViewScopeDao.getViewScope(viewTypeMask)).thenReturn(fullScope);
        HierarchicaFilter filter = (HierarchicaFilter) provider.getViewFilter(viewTypeMask);
        // call under test
        Set<Long> result = filter.getParentIds();
        assertEquals(fullScope, result);
		verify(mockViewScopeDao, times(1)).getViewScope(viewTypeMask);
    }

	@Test
	public void testGetViewFilterWithFilterGetScopeNotCalled() throws LimitExceededException {
		long viewTypeMask = ViewTypeMask.File.getMask();
		Set<Long> fullScope = Sets.newHashSet(1L,2L,3L);
		// call under test
		ViewFilter filter = provider.getViewFilter(viewTypeMask);
		ViewFilter expected = new HierarchicaFilter(ReplicationType.SUBMISSION, Sets.newHashSet(SubType.submission), () -> fullScope);
		assertEquals(expected, filter);
		verify(mockViewScopeDao, never()).getViewScopeType(anyLong());
	}


}

