package org.sagebionetworks.search.awscloudsearch;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import com.amazonaws.services.cloudsearchv2.model.DateArrayOptions;
import com.amazonaws.services.cloudsearchv2.model.DateOptions;
import com.amazonaws.services.cloudsearchv2.model.DoubleArrayOptions;
import com.amazonaws.services.cloudsearchv2.model.DoubleOptions;
import com.amazonaws.services.cloudsearchv2.model.IndexField;
import com.amazonaws.services.cloudsearchv2.model.IndexFieldType;
import com.amazonaws.services.cloudsearchv2.model.IntArrayOptions;
import com.amazonaws.services.cloudsearchv2.model.IntOptions;
import com.amazonaws.services.cloudsearchv2.model.LatLonOptions;
import com.amazonaws.services.cloudsearchv2.model.LiteralArrayOptions;
import com.amazonaws.services.cloudsearchv2.model.LiteralOptions;
import com.amazonaws.services.cloudsearchv2.model.TextArrayOptions;
import com.amazonaws.services.cloudsearchv2.model.TextOptions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SynapseCreatedCloudSearchFieldTest {

	private SynapseCreatedCloudSearchField synapseCreatedCloudSearchField;
	private IndexField indexField;

	@Before
	public void setUp(){
		indexField = new IndexField();


		LiteralOptions literalOptions = new LiteralOptions();
		LiteralArrayOptions literalArrayOptions = new LiteralArrayOptions();
		TextOptions textOptions = new TextOptions();
		TextArrayOptions textArrayOptions = new TextArrayOptions();
		IntOptions intOptions = new IntOptions();
		IntArrayOptions intArrayOptions = new IntArrayOptions();
		DateOptions dateOptions = new DateOptions();
		DateArrayOptions dateArrayOptions = new DateArrayOptions();
		LatLonOptions latLonOptions = new LatLonOptions();
		DoubleOptions doubleOptions = new DoubleOptions();
		DoubleArrayOptions doubleArrayOptions = new DoubleArrayOptions();
		indexField.withLiteralOptions(literalOptions).withLiteralArrayOptions(literalArrayOptions)
				.withTextOptions(textOptions).withTextArrayOptions(textArrayOptions)
				.withIntOptions(intOptions).withIntArrayOptions(intArrayOptions)
				.withDateOptions(dateOptions).withDateArrayOptions(dateArrayOptions)
				.withLatLonOptions(latLonOptions)
				.withDoubleOptions(doubleOptions).withDoubleArrayOptions(doubleArrayOptions);

		indexField.setIndexFieldType(IndexFieldType.Literal);
		synapseCreatedCloudSearchField = new SynapseCreatedCloudSearchField(indexField);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructor_nullIndexField(){
		synapseCreatedCloudSearchField = new SynapseCreatedCloudSearchField(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructor_nullFieldTypeInIndexField(){
		indexField.setIndexFieldType((String) null);
		synapseCreatedCloudSearchField = new SynapseCreatedCloudSearchField(indexField);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructor_nullIndexFieldOptionInIndexField(){
		indexField.setIndexFieldType(IndexFieldType.Literal);
		indexField.setLiteralOptions(null);
		synapseCreatedCloudSearchField = new SynapseCreatedCloudSearchField(indexField);
	}

	@Test
	public void testGetIndexField(){
		IndexField clonedIndexField = synapseCreatedCloudSearchField.getIndexField();
		assertNotSame(indexField, clonedIndexField);
		assertEquals(indexField, clonedIndexField);
	}

	@Test
	public void testGetIndexFieldOption(){
		assertCorrectIndexFieldOptionClass(IndexFieldType.Literal, LiteralOptions.class);
		assertCorrectIndexFieldOptionClass(IndexFieldType.LiteralArray, LiteralArrayOptions.class);
		assertCorrectIndexFieldOptionClass(IndexFieldType.Text, TextOptions.class);
		assertCorrectIndexFieldOptionClass(IndexFieldType.TextArray, TextArrayOptions.class);
		assertCorrectIndexFieldOptionClass(IndexFieldType.Int, IntOptions.class);
		assertCorrectIndexFieldOptionClass(IndexFieldType.IntArray, IntArrayOptions.class);
		assertCorrectIndexFieldOptionClass(IndexFieldType.Date, DateOptions.class);
		assertCorrectIndexFieldOptionClass(IndexFieldType.DateArray, DateArrayOptions.class);
		assertCorrectIndexFieldOptionClass(IndexFieldType.Latlon, LatLonOptions.class);
		assertCorrectIndexFieldOptionClass(IndexFieldType.Double, DoubleOptions.class);
		assertCorrectIndexFieldOptionClass(IndexFieldType.DoubleArray, DoubleArrayOptions.class);

	}

	private void assertCorrectIndexFieldOptionClass(IndexFieldType fieldType, Class expectedResultClass){
		indexField.setIndexFieldType(fieldType);
		assertThat(synapseCreatedCloudSearchField.getIndexFieldOption(), instanceOf(expectedResultClass));
	}

	@Test
	public void invokeIndexFieldOptionMethod_methodNotExist(){
		indexField.setIndexFieldType(IndexFieldType.Literal);
		assertFalse(synapseCreatedCloudSearchField.invokeIndexFieldOptionMethod("aNonExistentMethod"));
	}

	@Test
	public void invokeIndexFieldOptionMethod_methodExistsNullResult(){
		indexField.setIndexFieldType(IndexFieldType.Literal);
		indexField.getLiteralOptions().setSearchEnabled(null);
		//null return value should convert to false
		assertFalse(synapseCreatedCloudSearchField.invokeIndexFieldOptionMethod("getSearchEnabled"));
	}

	@Test
	public void invokeIndexFieldOptionMethod_methodExistsResultNotNull(){
		indexField.setIndexFieldType(IndexFieldType.Literal);
		indexField.getLiteralOptions().setSearchEnabled(true);
		assertTrue(synapseCreatedCloudSearchField.invokeIndexFieldOptionMethod("getSearchEnabled"));
	}

	@Test
	public void invokeIndexFieldOption_methodExistsIllegalAccessException() throws Exception{
		//TODO: this test does not work
		indexField.setIndexFieldType(IndexFieldType.Literal);
		indexField.getLiteralOptions().setSearchEnabled(true);


		//use Java Reflection to make the field private
		Method getSearchEnabledMethod = indexField.getLiteralOptions().getClass().getDeclaredMethod("getSearchEnabled");
		getSearchEnabledMethod.setAccessible(false);
		getSearchEnabledMethod.invoke(indexField.getLiteralOptions());

		try {
			System.out.println(synapseCreatedCloudSearchField.invokeIndexFieldOptionMethod("getSearchEnabled"));
			indexField.getLiteralOptions().getSearchEnabled();
			fail("RuntimeException should have been thrown");
		} catch (RuntimeException e){
			//expected
		}finally {
			getSearchEnabledMethod.setAccessible(true);
		}
	}

	@Test
	public void invokeIndexFieldOption_methodExistsInvocationTargetException(){
		indexField.setIndexFieldType(IndexFieldType.Literal);

		LiteralOptions mockLiteralOptions = Mockito.mock(LiteralOptions.class);
		indexField.setLiteralOptions(mockLiteralOptions);
		when(mockLiteralOptions.getSearchEnabled()).thenThrow(IllegalArgumentException.class);

		try {
			synapseCreatedCloudSearchField.invokeIndexFieldOptionMethod("getSearchEnabled");
			fail("RuntimeException should have been thrown");
		} catch (RuntimeException e){
			//expected
		}
	}


}
