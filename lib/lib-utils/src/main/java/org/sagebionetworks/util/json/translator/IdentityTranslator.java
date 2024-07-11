package org.sagebionetworks.util.json.translator;

import org.sagebionetworks.util.ValidateArgument;

/**
 * A translator where the both the Java type and JSON type are identical, so no
 * translation is needed.
 * 
 * @param <T>
 */
public class IdentityTranslator<T> implements Translator<T, T> {

	private final Class<? extends T> type;
	private final Class<?> primitiveType;

	/**
	 * his translator will only be used when the JavaType <F> matches the provide
	 * type exactly. Note: The primitive type will be null for this constructor.
	 * 
	 * @param type
	 */
	public IdentityTranslator(Class<? extends T> type) {
		this(type, null);
	}

	/**
	 * This translator will only be used when the JavaType <F> matches the provide
	 * type exactly, or the provided primitive type.
	 * 
	 * @param type
	 * @param primitiveType Only required for types with a corresponding primitive
	 *                      type. When provided, a the {@link #canTranslate(Class)}
	 *                      method will be true for either the main type or the
	 *                      primitive type.
	 */
	public IdentityTranslator(Class<? extends T> type, Class<?> primitiveType) {
		ValidateArgument.required(type, "type");
		this.type = type;
		this.primitiveType = primitiveType;
	}

	@Override
	public boolean canTranslate(Class<?> javaType) {
		return this.type.equals(javaType) || (primitiveType != null && primitiveType.equals(javaType));
	}

	/**
	 * The passed value is simply returned for this translator.
	 */
	@Override
	public T translateFromJSONToJava(Class<? extends T> type, T jsonValue) {
		return jsonValue;
	}

	/**
	 * The passed value is simply returned for this translator.
	 */
	@Override
	public T translateFromJavaToJSON(T javaValue) {
		return javaValue;
	}

	@Override
	public Class<? extends T> getJSONClass() {
		return this.type;
	}

}
