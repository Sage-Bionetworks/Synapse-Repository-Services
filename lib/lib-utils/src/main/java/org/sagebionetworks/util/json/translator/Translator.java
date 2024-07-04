package org.sagebionetworks.util.json.translator;

/**
 * 
 * @param <F>
 * @param <J>
 */
public interface Translator<F, J> {

	boolean canTranslate(Class fieldType);

	F translateFromJSONToFieldValue(Class type, J jsonValue);

	J translateFieldValueToJSON(Class type, F fieldValue);
	
	Class<? extends F> getFieldClass();
	
	Class<? extends J> getJSONClass();
}
