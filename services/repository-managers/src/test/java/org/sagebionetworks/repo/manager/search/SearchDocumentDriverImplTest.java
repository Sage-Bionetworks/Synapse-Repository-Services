package org.sagebionetworks.repo.manager.search;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.search.SearchConstants.FIELD_CONSORTIUM;
import static org.sagebionetworks.search.SearchConstants.FIELD_DISEASE;
import static org.junit.Assert.assertEquals;
import static org.sagebionetworks.search.SearchConstants.FIELD_NUM_SAMPLES;
import static org.sagebionetworks.search.SearchConstants.FIELD_PLATFORM;
import static org.sagebionetworks.search.SearchConstants.FIELD_TISSUE;

import java.util.Arrays;
import java.util.Collections;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.search.DocumentFields;

@RunWith(MockitoJUnitRunner.class)
public class SearchDocumentDriverImplTest {
	private DocumentFields documentFields;

	@Mock
	private DocumentFields disableAddAnnotationToDocumentField; //used in tests that want addAnnotationToDocumentField() to do nothing

	private SearchDocumentDriverImpl spySearchDocumentDriver;

	@Mock
	private Annotations mockAnnotations;

	private final String annoValue = "The early bird gets the worm";
	private final String annoAdditionalValue = "This bird woke up at 2pm";

	private final String indexableAnnotationKey1 = "disease";
	private final String indexableAnnotationKey2 = "platform";
	@Before
	public void setUp(){

		documentFields = new DocumentFields();
		spySearchDocumentDriver = Mockito.spy(new SearchDocumentDriverImpl());
		doNothing().when(spySearchDocumentDriver).addAnnotationToDocumentField(eq(disableAddAnnotationToDocumentField), anyString(), anyObject());

		assertTrue(SearchDocumentDriverImpl.SEARCHABLE_NODE_ANNOTATIONS.containsKey(indexableAnnotationKey1));
		assertTrue(SearchDocumentDriverImpl.SEARCHABLE_NODE_ANNOTATIONS.containsKey(indexableAnnotationKey2));
	}



	////////////////////////////////////////
	// addAnnotationToDocumentField() tests
	////////////////////////////////////////

	@Test(expected = IllegalArgumentException.class)
	public void testAddAnnotationToDocumentField_UnknownKey(){
		spySearchDocumentDriver.addAnnotationToDocumentField(documentFields, "SOME_OTHER_KEY", annoValue);
	}

	@Test
	public void testAddAnnotationToDocumentField_KeyIsDisease(){
		spySearchDocumentDriver.addAnnotationToDocumentField(documentFields, FIELD_DISEASE, annoValue);
		assertEquals(annoValue, documentFields.getDisease());

		//assert value is not overwritten when a second value comes in
		spySearchDocumentDriver.addAnnotationToDocumentField(documentFields, FIELD_DISEASE, annoAdditionalValue);
		assertEquals(annoValue, documentFields.getDisease());
	}

	@Test
	public void testAddAnnotationToDocumentField_KeyIsConsortium(){
		spySearchDocumentDriver.addAnnotationToDocumentField(documentFields, FIELD_CONSORTIUM, annoValue);
		assertEquals(annoValue, documentFields.getConsortium());

		//assert value is not overwritten when a second value comes in
		spySearchDocumentDriver.addAnnotationToDocumentField(documentFields, FIELD_CONSORTIUM, annoAdditionalValue);
		assertEquals(annoValue, documentFields.getConsortium());
	}

	@Test
	public void testAddAnnotationToDocumentField_KeyIsTissue(){
		spySearchDocumentDriver.addAnnotationToDocumentField(documentFields, FIELD_TISSUE, annoValue);
		assertEquals(annoValue, documentFields.getTissue());

		//assert value is not overwritten when a second value comes in
		spySearchDocumentDriver.addAnnotationToDocumentField(documentFields, FIELD_TISSUE, annoAdditionalValue);
		assertEquals(annoValue, documentFields.getTissue());
	}

	@Test
	public void testAddAnnotationToDocumentField_KeyIsPlatform(){
		spySearchDocumentDriver.addAnnotationToDocumentField(documentFields, FIELD_PLATFORM, annoValue);
		assertEquals(annoValue, documentFields.getPlatform());

		//assert value is not overwritten when a second value comes in
		spySearchDocumentDriver.addAnnotationToDocumentField(documentFields, FIELD_PLATFORM, annoAdditionalValue);
		assertEquals(annoValue, documentFields.getPlatform());
	}

	@Test
	public void testAddAnnotationToDocumentField_KeyIsNumSamples(){
		Long longValue = 64L;
		Long otherLongValue = 99999L;
		spySearchDocumentDriver.addAnnotationToDocumentField(documentFields, FIELD_NUM_SAMPLES, longValue);
		assertEquals(longValue, documentFields.getNum_samples());

		//assert value is not overwritten when a second value comes in
		spySearchDocumentDriver.addAnnotationToDocumentField(documentFields, FIELD_NUM_SAMPLES, otherLongValue);
		assertEquals(longValue, documentFields.getNum_samples());
	}

	@Test
	public void testAddAnnotationToDocumentField_KeyIsNumSamplesValueIsString(){
		String goodString = "64";
		String badString = "I'm not a long.";
		spySearchDocumentDriver.addAnnotationToDocumentField(documentFields, FIELD_NUM_SAMPLES, badString);
		assertEquals(null, documentFields.getNum_samples());

		spySearchDocumentDriver.addAnnotationToDocumentField(documentFields, FIELD_NUM_SAMPLES, goodString);
		assertEquals((Long) 64L, documentFields.getNum_samples());
	}

	//////////////////////////////////////////
	// addAnnotationsToSearchDocument() tests
	//////////////////////////////////////////
	@Test
	public void testAddAnnotationsToSearchDocument_SkipUnrecognizedAnnoKeys(){
		String nonExistentKey = "not actualy search index key";
		assertFalse(SearchDocumentDriverImpl.SEARCHABLE_NODE_ANNOTATIONS.containsKey(nonExistentKey));

		when(mockAnnotations.keySet()).thenReturn(Sets.newHashSet(nonExistentKey));

		spySearchDocumentDriver.addAnnotationsToSearchDocument(disableAddAnnotationToDocumentField, mockAnnotations);

		verify(spySearchDocumentDriver,never()).addAnnotationToDocumentField(any(DocumentFields.class),anyString(), anyObject());
	}

	@Test
	public void testAddAnnotationsToSearchDocument_SkipByteArray(){

		when(mockAnnotations.keySet()).thenReturn(Sets.newHashSet(indexableAnnotationKey1));
		when(mockAnnotations.getAllValues(indexableAnnotationKey1)).thenReturn(Collections.singletonList( new byte[]{} )); //list containing a byte array

		spySearchDocumentDriver.addAnnotationsToSearchDocument(disableAddAnnotationToDocumentField, mockAnnotations);

		verify(mockAnnotations, times(1)).getAllValues(indexableAnnotationKey1);
		verify(spySearchDocumentDriver,never()).addAnnotationToDocumentField(any(DocumentFields.class),anyString(), anyObject());
	}

	@Test
	public void testAddAnnotationsToSearchDocument_SingleAnnotationKeyWithMultipleAnnotationValues(){
		when(mockAnnotations.keySet()).thenReturn(Sets.newHashSet(indexableAnnotationKey1));
		when(mockAnnotations.getAllValues(indexableAnnotationKey1)).thenReturn(Arrays.asList(null, annoValue, annoAdditionalValue));

		spySearchDocumentDriver.addAnnotationsToSearchDocument(disableAddAnnotationToDocumentField, mockAnnotations);

		verify(mockAnnotations, times(1)).getAllValues(indexableAnnotationKey1);
		verify(spySearchDocumentDriver,times(1)).addAnnotationToDocumentField(disableAddAnnotationToDocumentField, indexableAnnotationKey1, annoValue);
	}

	@Test
	public void testAddAnnotationsToSearchDocument_MultipleAnnotationKeys(){
		when(mockAnnotations.keySet()).thenReturn(Sets.newHashSet(indexableAnnotationKey1, indexableAnnotationKey2));
		when(mockAnnotations.getAllValues(indexableAnnotationKey1)).thenReturn(Collections.singletonList(annoValue));
		when(mockAnnotations.getAllValues(indexableAnnotationKey2)).thenReturn(Collections.singletonList(annoAdditionalValue));

		spySearchDocumentDriver.addAnnotationsToSearchDocument(disableAddAnnotationToDocumentField, mockAnnotations);

		verify(mockAnnotations, times(1)).getAllValues(indexableAnnotationKey1);
		verify(mockAnnotations, times(1)).getAllValues(indexableAnnotationKey2);
		verify(spySearchDocumentDriver,times(1)).addAnnotationToDocumentField(disableAddAnnotationToDocumentField, indexableAnnotationKey1, annoValue);
		verify(spySearchDocumentDriver,times(1)).addAnnotationToDocumentField(disableAddAnnotationToDocumentField, indexableAnnotationKey2, annoAdditionalValue);
	}
}
