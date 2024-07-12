package org.sagebionetworks.util.json.translator;

/**
 * An abstraction for a translator used to translate between the Java
 * representation of a value <F> and its corresponding representation in JSON
 * <J>.
 * 
 * @param <F> The type of the value represented in Java.
 * @param <J> The type of the value represented in JSON.
 */
public interface Translator<F, J> {

	/**
	 * Can this translator translate a value from the given Java type?
	 * 
	 * @param javaType
	 * @return
	 */
	boolean canTranslate(Class<?> javaType);

	/**
	 * Translate the provide JSON value to its corresponding Java value.
	 * 
	 * @param type      When <F> is an interface, this type will be the concrete
	 *                  Java type that implements the interface <F>.
	 * @param jsonValue
	 * @return
	 */
	F translateFromJSONToJava(Class<? extends F> type, J jsonValue);

	/**
	 * Translate from the provided Java value <F> to its corresponding JSON value <J>.
	 * @param javaValue
	 * @return
	 */
	J translateFromJavaToJSON(F javaValue);

	/**
	 * Get the class that represents the JSON value <J>.
	 * @return
	 */
	Class<? extends J> getJSONClass();
}
