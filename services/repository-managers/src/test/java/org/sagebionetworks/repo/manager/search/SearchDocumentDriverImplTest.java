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

		assertTrue(SearchDocumentDriverImpl.SEARCHABLE_NODE_ANNOTATIONS.containsKey(indexableAnnotationKey1));
		assertTrue(SearchDocumentDriverImpl.SEARCHABLE_NODE_ANNOTATIONS.containsKey(indexableAnnotationKey2));
	}

	//TODO: tests
	@Test
	public void TODOTest(){
	}
}
