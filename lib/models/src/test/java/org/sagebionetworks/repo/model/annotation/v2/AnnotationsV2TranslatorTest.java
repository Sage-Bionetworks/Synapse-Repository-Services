package org.sagebionetworks.repo.model.annotation.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AnnotationsV2TranslatorTest {

	Annotations annotationsV2;
	org.sagebionetworks.repo.model.Annotations annotationsV1;

	final String stringKey1 = "stringKey1";
	final String stringKey2 = "stringKey2";
	final String dateKey1 = "dateKey1";
	final String dateKey2 = "dateKey2";
	final String longKey1 = "longKey1";
	final String longKey2 = "longKey2";
	final String doubleKey1 = "doubleKey1";
	final String doubleKey2 = "doubleKey2";

	@BeforeEach
	void setUp() {

		//create an annotations v1 and v2 that are equivalent with all value types
		annotationsV1 = new org.sagebionetworks.repo.model.Annotations();
		annotationsV1.addAnnotation(stringKey1, Arrays.asList("val1", "val2"));
		annotationsV1.addAnnotation(stringKey2, Arrays.asList("val3", "val4"));

		annotationsV1.addAnnotation(doubleKey1, Arrays.asList(1.2, 2.3));
		annotationsV1.addAnnotation(doubleKey2, Arrays.asList(3.4, 4.5));

		annotationsV1.addAnnotation(dateKey1, Arrays.asList(new Date(123), new Date(456)));
		annotationsV1.addAnnotation(dateKey2, Arrays.asList(new Date(789), new Date(890)));

		annotationsV1.addAnnotation(longKey1, Arrays.asList(123L, 456L));
		annotationsV1.addAnnotation(longKey2, Arrays.asList(789L, 890L));



		annotationsV2 = new Annotations();

		AnnotationsV2TestUtils.putAnnotations(annotationsV2, stringKey1, Arrays.asList("val1", "val2"), AnnotationsValueType.STRING);
		AnnotationsV2TestUtils.putAnnotations(annotationsV2, stringKey2, Arrays.asList("val3", "val4"), AnnotationsValueType.STRING);

		AnnotationsV2TestUtils.putAnnotations(annotationsV2, doubleKey1, Arrays.asList("1.2", "2.3"), AnnotationsValueType.DOUBLE);
		AnnotationsV2TestUtils.putAnnotations(annotationsV2, doubleKey2, Arrays.asList("3.4", "4.5"), AnnotationsValueType.DOUBLE);

		AnnotationsV2TestUtils.putAnnotations(annotationsV2, dateKey1, Arrays.asList("123", "456"), AnnotationsValueType.TIMESTAMP_MS);
		AnnotationsV2TestUtils.putAnnotations(annotationsV2, dateKey2, Arrays.asList("789", "890"), AnnotationsValueType.TIMESTAMP_MS);

		AnnotationsV2TestUtils.putAnnotations(annotationsV2, longKey1, Arrays.asList("123", "456"), AnnotationsValueType.LONG);
		AnnotationsV2TestUtils.putAnnotations(annotationsV2, longKey2, Arrays.asList("789", "890"), AnnotationsValueType.LONG);
	}

	@Test
	public void testToAnnotationsV1(){
		//method under test
		org.sagebionetworks.repo.model.Annotations translated = AnnotationsV2Translator.toAnnotationsV1(annotationsV2);

		assertEquals(annotationsV1, translated);
	}

	@Test
	public void testToAnnotationsV1_null(){
		assertEquals(null, AnnotationsV2Translator.toAnnotationsV1(null));
	}

	@Test
	public void testToAnnotationsV1_nullMap(){
		annotationsV2.setAnnotations(null);

		//method under test
		org.sagebionetworks.repo.model.Annotations translated = AnnotationsV2Translator.toAnnotationsV1(annotationsV2);

		annotationsV1 = new org.sagebionetworks.repo.model.Annotations();
		assertEquals(annotationsV1, translated);
	}

	@Test
	public void testToAnnotationsV1_EmptyListValues(){
		//replace stringKey1 's value with an empty list
		AnnotationsValue emptyValue = new AnnotationsValue();
		emptyValue.setType(AnnotationsValueType.STRING);
		emptyValue.setValue(Collections.emptyList());
		annotationsV2.getAnnotations().put(stringKey1, emptyValue);

		//method under test
		org.sagebionetworks.repo.model.Annotations translated = AnnotationsV2Translator.toAnnotationsV1(annotationsV2);

		//expected value should have an empty list
		annotationsV1.getStringAnnotations().put(stringKey1, Collections.emptyList());
		assertEquals(annotationsV1, translated);
	}

	@Test
	public void testToAnnotationsV1_ListWithNullValues(){
		//replace stringKey1 's value with an empty list
		AnnotationsValue emptyValue = new AnnotationsValue();
		emptyValue.setType(AnnotationsValueType.STRING);
		emptyValue.setValue(Arrays.asList("a", null, null, "b", null));
		annotationsV2.getAnnotations().put(stringKey1, emptyValue);

		//method under test
		org.sagebionetworks.repo.model.Annotations translated = AnnotationsV2Translator.toAnnotationsV1(annotationsV2);

		//expected value should have an empty list
		annotationsV1.getStringAnnotations().put(stringKey1, Arrays.asList("a", "b"));
		assertEquals(annotationsV1, translated);
	}



	@Test
	public void testToAnnotationsV1_doubleNaN(){
		String nanKey = "nanKey";
		annotationsV2 = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(annotationsV2, nanKey, Arrays.asList("NaN","nan", "NAN", "nAn"), AnnotationsValueType.DOUBLE);

		//method under test
		org.sagebionetworks.repo.model.Annotations translated = AnnotationsV2Translator.toAnnotationsV1(annotationsV2);

		//expected value should not have stringkey1 value;
		annotationsV1 = new org.sagebionetworks.repo.model.Annotations();
		annotationsV1.addAnnotation(nanKey, Arrays.asList(Double.NaN, Double.NaN, Double.NaN, Double.NaN));

		assertEquals(annotationsV1, translated);
	}

	@Test
	public void testToAnnotationsV1_doublePositiveInf(){
		String posInfKey = "posInf";
		annotationsV2 = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(annotationsV2, posInfKey, Arrays.asList("infinity","inf", "iNfiNity", "iNF", "+infinity", "+inf", "+iNfiNity", "+iNF"), AnnotationsValueType.DOUBLE);

		//method under test
		org.sagebionetworks.repo.model.Annotations translated = AnnotationsV2Translator.toAnnotationsV1(annotationsV2);

		//expected value should not have stringkey1 value;
		annotationsV1 = new org.sagebionetworks.repo.model.Annotations();
		Double[] posInfinities = new Double[8];
		Arrays.fill(posInfinities, Double.POSITIVE_INFINITY);
		annotationsV1.addAnnotation(posInfKey, Arrays.asList(posInfinities));

		assertEquals(annotationsV1, translated);
	}

	@Test
	public void testToAnnotationsV1_doubleNegativeInf(){
		String negInfKey = "negInf";
		annotationsV2 = new Annotations();
		AnnotationsV2TestUtils.putAnnotations(annotationsV2, negInfKey, Arrays.asList("-infinity", "-inf", "-iNfiNity", "-iNF"), AnnotationsValueType.DOUBLE);

		//method under test
		org.sagebionetworks.repo.model.Annotations translated = AnnotationsV2Translator.toAnnotationsV1(annotationsV2);

		//expected value should not have stringkey1 value;
		annotationsV1 = new org.sagebionetworks.repo.model.Annotations();
		Double[] negInfinities = new Double[4];
		Arrays.fill(negInfinities, Double.NEGATIVE_INFINITY);
		annotationsV1.addAnnotation(negInfKey, Arrays.asList(negInfinities));

		assertEquals(annotationsV1, translated);
	}

	@Test
	public void testToAnnotationsV2(){
		//method under test
		Annotations translated = AnnotationsV2Translator.toAnnotationsV2(annotationsV1);

		assertEquals(annotationsV2, translated);
	}

	@Test
	public void testToAnnotationsV2_null(){
		org.sagebionetworks.repo.model.Annotations annotationsV1 = null;
		//method under test
		Annotations translated = AnnotationsV2Translator.toAnnotationsV2(annotationsV1);

		assertEquals(null, translated);
	}

	@Test
	public void testToAnnotationsV2_emptyListValues(){
		//set list for stringkey1 to empty
		annotationsV1.getStringAnnotations().put(stringKey1, Collections.emptyList());
		//method under test
		Annotations translated = AnnotationsV2Translator.toAnnotationsV2(annotationsV1);

		//annotationsV2 should have an empty list mapping
		AnnotationsV2TestUtils.putAnnotations(annotationsV2, stringKey1, Collections.emptyList(), AnnotationsValueType.STRING);
		assertEquals(annotationsV2, translated);
	}

	@Test
	public void testToAnnotationsV2_nullListValues(){
		//set list for stringkey1 to empty
		annotationsV1.getStringAnnotations().put(stringKey1, null);
		//method under test
		Annotations translated = AnnotationsV2Translator.toAnnotationsV2(annotationsV1);

		//annotationsV2 should have an empty list mapping
		AnnotationsV2TestUtils.putAnnotations(annotationsV2, stringKey1, Collections.emptyList(), AnnotationsValueType.STRING);
		assertEquals(annotationsV2, translated);
	}

	@Test
	public void testToAnnotationsV2_ListWithNullValues(){
		//set list for stringkey1 to empty
		annotationsV1.getStringAnnotations().put(stringKey1, Arrays.asList(null, null, null, "value", null, "val2"));
		//method under test
		Annotations translated = AnnotationsV2Translator.toAnnotationsV2(annotationsV1);

		//annotationsV2 nulls should have been filtered out
		AnnotationsV2TestUtils.putAnnotations(annotationsV2, stringKey1, Arrays.asList("value", "val2"), AnnotationsValueType.STRING);
		assertEquals(annotationsV2, translated);
	}

	@Test
	public void testToAnnotationsV2_V1WithAnEmptyMap(){
		//set list for stringkey1 to empty
		annotationsV1.setStringAnnotations(Collections.emptyMap());
		//method under test
		Annotations translated = AnnotationsV2Translator.toAnnotationsV2(annotationsV1);

		//should stringKey and stringKey2 should no longer exist because the string annotations were empty
		annotationsV2.getAnnotations().remove(stringKey1);
		annotationsV2.getAnnotations().remove(stringKey2);
		assertEquals(annotationsV2, translated);
	}

}