package org.sagebionetworks.repo.manager.table.metadata.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;
import org.sagebionetworks.repo.model.table.ObjectDataDTO;
import org.sagebionetworks.repo.model.table.ObjectField;
import org.sagebionetworks.repo.model.table.ViewObjectType;
import org.sagebionetworks.table.cluster.metadata.ObjectFieldTypeMapper;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

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
	public void testIsFilterScopeByObjectId() {
		// Call under test
		boolean result = provider.isFilterScopeByObjectId(mockViewTypeMask);

		assertFalse(result);

	}

	@Test
	public void testGetSubTypesForMask() {
		// Call under test
		List<String> result = provider.getSubTypesForMask(mockViewTypeMask);

		assertEquals(ImmutableList.of(viewObjectType.defaultSubType()), result);

	}

	@Test
	public void testGetAllContainerIdsForScope() throws Exception {

		Set<Long> scope = ImmutableSet.of(1L, 2L);
		int containerLimit = 10;

		Set<Long> expected = ImmutableSet.of(1L, 2L);

		// Call under test
		Set<Long> result = provider.getContainerIdsForScope(scope, mockViewTypeMask, containerLimit);

		assertEquals(expected, result);

	}

	@Test
	public void testcreateViewOverLimitMessageFileView() {
		int limit = 10;

		// call under test
		String message = provider.createViewOverLimitMessage(mockViewTypeMask, limit);

		assertEquals("The view's scope exceeds the maximum number of " + limit + " evaluations.", message);

	}

	@Test
	public void testGetObjectData() {

		List<ObjectDataDTO> expected = Collections.singletonList(mockData);

		List<Long> objectIds = ImmutableList.of(1L, 2L, 3L);

		int maxAnnotationChars = 5;

		when(mockSubmissionDao.getSubmissionData(any(), anyInt())).thenReturn(expected);

		// Call under test
		List<ObjectDataDTO> result = provider.getObjectData(objectIds, maxAnnotationChars);

		assertEquals(expected, result);
		verify(mockSubmissionDao).getSubmissionData(objectIds, maxAnnotationChars);
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

		List<ObjectField> expectedFields = ImmutableList.of(
				ObjectField.id,
				ObjectField.name, 
				ObjectField.createdOn, 
				ObjectField.createdBy,
				ObjectField.etag, 
				ObjectField.modifiedOn,
				ObjectField.projectId
		);
		
		assertNotNull(model);
		assertEquals(expectedFields, model.getDefaultFields());
		assertEquals(SubmissionField.values().length, model.getCustomFields().size());
		
		for (SubmissionField field : SubmissionField.values()) {
			boolean present = model.findCustomFieldByColumnName(field.getColumnName()).isPresent();
			assertTrue(present);
		}
	}

	@Test
	public void testGetContainerIdsForReconciliation() throws LimitExceededException {

		Set<Long> scope = ImmutableSet.of(1L, 2L);
		int containerLimit = 10;

		Set<Long> expected = ImmutableSet.of(1L, 2L);

		// Call under test
		Set<Long> result = provider.getContainerIdsForReconciliation(scope, mockViewTypeMask, containerLimit);

		assertEquals(expected, result);

	}

	@Test
	public void testGetAvaliableContainers() {

		List<Long> containerIds = ImmutableList.of(1L, 2L);
		Set<Long> expectedIds = ImmutableSet.of(1L);

		when(mockEvaluationDao.getAvailableEvaluations(any())).thenReturn(expectedIds);

		// Call under test
		Set<Long> result = provider.getAvailableContainers(containerIds);

		assertEquals(expectedIds, result);

		verify(mockEvaluationDao).getAvailableEvaluations(containerIds);
	}

	@Test
	public void testGetChildren() {

		Long containerId = 1L;
		List<IdAndEtag> expected = ImmutableList.of(mockIdAndEtag, mockIdAndEtag);

		when(mockSubmissionDao.getSubmissionIdAndEtag(anyLong())).thenReturn(expected);

		// Call under test
		List<IdAndEtag> result = provider.getChildren(containerId);

		assertEquals(expected, result);
		verify(mockSubmissionDao).getSubmissionIdAndEtag(containerId);
	}

	@Test
	public void testGetSumOfChildCRCsForEachContainer() {
		List<Long> containerIds = ImmutableList.of(1L, 2L);

		Map<Long, Long> expected = ImmutableMap.of(1L, 10L, 2L, 30L);

		when(mockSubmissionDao.getSumOfSubmissionCRCsForEachEvaluation(any())).thenReturn(expected);

		Map<Long, Long> result = provider.getSumOfChildCRCsForEachContainer(containerIds);

		assertEquals(expected, result);
		verify(mockSubmissionDao).getSumOfSubmissionCRCsForEachEvaluation(containerIds);
	}

	@Test
	public void testGetBenefactorObjectType() {

		// Call under test
		ObjectType objectType = provider.getBenefactorObjectType();

		assertEquals(ObjectType.EVALUATION, objectType);

	}

}
