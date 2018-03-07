package org.sagebionetworks.repo.manager.search;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;
import static org.sagebionetworks.search.SearchConstants.FIELD_CONSORTIUM;
import static org.sagebionetworks.search.SearchConstants.FIELD_DISEASE;
import static org.sagebionetworks.search.SearchConstants.FIELD_NUM_SAMPLES;
import static org.sagebionetworks.search.SearchConstants.FIELD_PLATFORM;
import static org.sagebionetworks.search.SearchConstants.FIELD_TISSUE;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.search.DocumentFields;

@RunWith(MockitoJUnitRunner.class)
public class SearchDocumentDriverImplTest {
	private DocumentFields documentFields;

	@Mock
	private DocumentFields disableAddAnnotationToDocumentField; //used in tests that want addAnnotationToDocumentField() to do nothing

	private SearchDocumentDriverImpl spySearchDocumentDriver;

	@Mock
	private Annotations mockAnnotations;
	@Mock
	private NamedAnnotations mockNamedAnnotations;


	private final String annoKey1 = "annoKey1";
	private final String annoKey2 = "annoKey2";

	private final String existingIndexField = FIELD_TISSUE;
	private final String existingAnnotationKey = "tissue";
	private final String existingAnnotationKey2 = "tissue_tumor";


	private final String annoValue1 = "The early bird gets the worm";
	private final String annoValue2= "This bird woke up at 2pm";

	private Map<String, String> annoValuesMap;

	@Before
	public void setUp(){

		documentFields = new DocumentFields();
		spySearchDocumentDriver = Mockito.spy(new SearchDocumentDriverImpl());

		annoValuesMap = new HashMap<>();

		// since SEARCHABLE_NODE_ANNOTATIONS is final and unmodifiable, we need to check that values we assume exist
		// actually do exist in this map
		assertTrue(SearchDocumentDriverImpl.SEARCHABLE_NODE_ANNOTATIONS.containsKey(existingIndexField));
		assertTrue(SearchDocumentDriverImpl.SEARCHABLE_NODE_ANNOTATIONS.get(existingIndexField).containsAll(Arrays.asList(existingAnnotationKey, existingAnnotationKey2)));
	}

	@Test
	public void addFirstAnnotationValuesToMap__skipByteArray(){
		when(mockAnnotations.keySet()).thenReturn(Sets.newHashSet(annoKey1));
		when(mockAnnotations.getSingleValue(annoKey1)).thenReturn(new byte[]{});

		spySearchDocumentDriver.addFirstAnnotationValuesToMap(mockAnnotations, annoValuesMap);

		verify(mockAnnotations, times(1)).keySet();
		verify(mockAnnotations, times(1)).getSingleValue(annoKey1);
		assertTrue(annoValuesMap.isEmpty());
	}

	@Test
	public void addFirstAnnotationValuesToMap__skipExistingValue(){
		annoValuesMap.put(annoKey1.toLowerCase(), "asdf");

		when(mockAnnotations.keySet()).thenReturn(Sets.newHashSet(annoKey1));
		when(mockAnnotations.getSingleValue(annoKey1)).thenReturn(annoValue1);

		spySearchDocumentDriver.addFirstAnnotationValuesToMap(mockAnnotations, annoValuesMap);

		verify(mockAnnotations, times(1)).keySet();
		verify(mockAnnotations, times(1)).getSingleValue(annoKey1);
		assertEquals(1, annoValuesMap.size());
		assertEquals("asdf", annoValuesMap.get(annoKey1.toLowerCase()));
	}

	@Test
	public void addFirstAnnotationValuesToMap__multipleValues(){
		when(mockAnnotations.keySet()).thenReturn(Sets.newHashSet(annoKey1, annoKey2));
		when(mockAnnotations.getSingleValue(annoKey1)).thenReturn(annoValue1);
		when(mockAnnotations.getSingleValue(annoKey2)).thenReturn(annoValue2);

		spySearchDocumentDriver.addFirstAnnotationValuesToMap(mockAnnotations, annoValuesMap);

		verify(mockAnnotations, times(1)).keySet();
		verify(mockAnnotations, times(1)).getSingleValue(annoKey1);
		verify(mockAnnotations, times(1)).getSingleValue(annoKey2);
		assertEquals(2, annoValuesMap.size());
		assertEquals(annoValue1, annoValuesMap.get(annoKey1.toLowerCase()));
		assertEquals(annoValue2, annoValuesMap.get(annoKey2.toLowerCase()));
	}

	@Test
	public void getFirsAnnotationValues(){
		Annotations mockPrimaryAnnotations = mockAnnotations;
		Annotations mockAdditionaAnnotations = mock(Annotations.class);
		when(mockNamedAnnotations.getPrimaryAnnotations()).thenReturn(mockPrimaryAnnotations);
		when(mockNamedAnnotations.getAdditionalAnnotations()).thenReturn(mockAdditionaAnnotations);
		doNothing().when(spySearchDocumentDriver).addFirstAnnotationValuesToMap(any(Annotations.class), anyMapOf(String.class, String.class));

		Map<String, String> result = spySearchDocumentDriver.getFirsAnnotationValues(mockNamedAnnotations);

		assertNotNull(result);
		verify(mockNamedAnnotations, times(1)).getAdditionalAnnotations();
		verify(spySearchDocumentDriver, times(1)).addFirstAnnotationValuesToMap(mockAdditionaAnnotations, result);
	}

	@Test
	public void getSearchIndexFieldValue__returnFirstAnnoValues(){
		annoValuesMap.put(existingAnnotationKey, annoValue1);
		annoValuesMap.put(existingAnnotationKey2, annoValue2);

		String result = spySearchDocumentDriver.getSearchIndexFieldValue(annoValuesMap, existingIndexField);
		assertEquals(annoValue1, result);
	}

	@Test
	public void getSearchIndexFieldValue__returnNoValues(){
		assertTrue(annoValuesMap.isEmpty());
		String result = spySearchDocumentDriver.getSearchIndexFieldValue(annoValuesMap, existingIndexField);
		assertNull(result);
	}

	@Test
	public void getSearchIndexFieldValue__testCaseInsensitive(){
		annoValuesMap.put("platformdesc", annoValue1);
		assertTrue(SearchDocumentDriverImpl.SEARCHABLE_NODE_ANNOTATIONS.containsKey(FIELD_PLATFORM));
		assertTrue(SearchDocumentDriverImpl.SEARCHABLE_NODE_ANNOTATIONS.get(FIELD_PLATFORM).contains("platformDesc"));//camel cased
		String result = spySearchDocumentDriver.getSearchIndexFieldValue(annoValuesMap, FIELD_PLATFORM);
		assertEquals(annoValue1, result);
	}

	@Test
	public void addAnnotationsToSearchDocument_AnnotationValueIsNull(){
		doReturn(annoValuesMap).when(spySearchDocumentDriver).getFirsAnnotationValues(mockNamedAnnotations);
		doReturn(null).when(spySearchDocumentDriver).getSearchIndexFieldValue(eq(annoValuesMap), anyString());

		spySearchDocumentDriver.addAnnotationsToSearchDocument(documentFields, mockNamedAnnotations);

		assertNull(documentFields.getNum_samples());
		assertNull(documentFields.getDisease());
		assertNull(documentFields.getConsortium());
		assertNull(documentFields.getTissue());
		assertNull(documentFields.getPlatform());
		assertNull(documentFields.getDisease());
		verify(spySearchDocumentDriver,times(1)).getSearchIndexFieldValue(annoValuesMap, FIELD_DISEASE);
		verify(spySearchDocumentDriver,times(1)).getSearchIndexFieldValue(annoValuesMap, FIELD_CONSORTIUM);
		verify(spySearchDocumentDriver,times(1)).getSearchIndexFieldValue(annoValuesMap, FIELD_TISSUE);
		verify(spySearchDocumentDriver,times(1)).getSearchIndexFieldValue(annoValuesMap, FIELD_PLATFORM);
		verify(spySearchDocumentDriver,times(1)).getSearchIndexFieldValue(annoValuesMap, FIELD_NUM_SAMPLES);
	}

	@Test
	public void addAnnotationsToSearchDocument_AnnotationValueIsNotNumericString(){
		doReturn(annoValuesMap).when(spySearchDocumentDriver).getFirsAnnotationValues(mockNamedAnnotations);
		doReturn(annoValue1).when(spySearchDocumentDriver).getSearchIndexFieldValue(eq(annoValuesMap), anyString());

		spySearchDocumentDriver.addAnnotationsToSearchDocument(documentFields, mockNamedAnnotations);

		assertNull(documentFields.getNum_samples());
		assertEquals(annoValue1, documentFields.getDisease());
		assertEquals(annoValue1, documentFields.getConsortium());
		assertEquals(annoValue1, documentFields.getTissue());
		assertEquals(annoValue1, documentFields.getPlatform());
		assertEquals(annoValue1, documentFields.getDisease());
		verify(spySearchDocumentDriver,times(1)).getSearchIndexFieldValue(annoValuesMap, FIELD_DISEASE);
		verify(spySearchDocumentDriver,times(1)).getSearchIndexFieldValue(annoValuesMap, FIELD_CONSORTIUM);
		verify(spySearchDocumentDriver,times(1)).getSearchIndexFieldValue(annoValuesMap, FIELD_TISSUE);
		verify(spySearchDocumentDriver,times(1)).getSearchIndexFieldValue(annoValuesMap, FIELD_PLATFORM);
		verify(spySearchDocumentDriver,times(1)).getSearchIndexFieldValue(annoValuesMap, FIELD_NUM_SAMPLES);
	}

	@Test
	public void addAnnotationsToSearchDocument_AnnotationValueIsNumericString(){
		String numberString = "5";
		doReturn(annoValuesMap).when(spySearchDocumentDriver).getFirsAnnotationValues(mockNamedAnnotations);
		doReturn(numberString).when(spySearchDocumentDriver).getSearchIndexFieldValue(eq(annoValuesMap), anyString());

		spySearchDocumentDriver.addAnnotationsToSearchDocument(documentFields, mockNamedAnnotations);

		assertEquals((Long) Long.parseLong(numberString), documentFields.getNum_samples());
		assertEquals(numberString, documentFields.getDisease());
		assertEquals(numberString, documentFields.getConsortium());
		assertEquals(numberString, documentFields.getTissue());
		assertEquals(numberString, documentFields.getPlatform());
		assertEquals(numberString, documentFields.getDisease());
		verify(spySearchDocumentDriver,times(1)).getSearchIndexFieldValue(annoValuesMap, FIELD_DISEASE);
		verify(spySearchDocumentDriver,times(1)).getSearchIndexFieldValue(annoValuesMap, FIELD_CONSORTIUM);
		verify(spySearchDocumentDriver,times(1)).getSearchIndexFieldValue(annoValuesMap, FIELD_TISSUE);
		verify(spySearchDocumentDriver,times(1)).getSearchIndexFieldValue(annoValuesMap, FIELD_PLATFORM);
		verify(spySearchDocumentDriver,times(1)).getSearchIndexFieldValue(annoValuesMap, FIELD_NUM_SAMPLES);
	}


}
