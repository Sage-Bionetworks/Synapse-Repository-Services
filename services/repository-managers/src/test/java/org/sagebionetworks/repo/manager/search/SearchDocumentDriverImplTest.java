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
import org.sagebionetworks.repo.model.search.DocumentFields;

@RunWith(MockitoJUnitRunner.class)
public class SearchDocumentDriverImplTest {
	private DocumentFields documentFields;

	@Mock
	private DocumentFields disableAddAnnotationToDocumentField; //used in tests that want addAnnotationToDocumentField() to do nothing

	private SearchDocumentDriverImpl spySearchDocumentDriver;

	@Mock
	private Annotations mockAnnotations;

	private final String annoKey1 = "annoKey1";
	private final String annoKey2 = "annoKey2";


	private final String annoValue1 = "The early bird gets the worm";
	private final String annoValue2= "This bird woke up at 2pm";

	private Map<String, String> annoValuesMap;

	@Before
	public void setUp(){

		documentFields = new DocumentFields();
		spySearchDocumentDriver = Mockito.spy(new SearchDocumentDriverImpl());

		annoValuesMap = new HashMap<>();
	}

	@Test
	public void addFirstAnnotationValuesToMap__skipByteArray(){
		when(mockAnnotations.keySet()).thenReturn(Sets.newHashSet(annoKey1));
		when(mockAnnotations.getSingleValue(annoKey1)).thenReturn(new byte[]{});

		spySearchDocumentDriver.addFirstAnnotationValuesToMap(mockAnnotations, annoValuesMap);
		assertTrue(annoValuesMap.isEmpty());
	}

	@Test
	public void addFirstAnnotationValuesToMap__skipExistingValue(){
		annoValuesMap.put(annoKey1.toLowerCase(), "asdf");

		when(mockAnnotations.keySet()).thenReturn(Sets.newHashSet(annoKey1));
		when(mockAnnotations.getSingleValue(annoKey1)).thenReturn(annoValue1);

		spySearchDocumentDriver.addFirstAnnotationValuesToMap(mockAnnotations, annoValuesMap);
		assertEquals(1, annoValuesMap.size());
		assertEquals("asdf", annoValuesMap.get(annoKey1.toLowerCase()));
	}
}
