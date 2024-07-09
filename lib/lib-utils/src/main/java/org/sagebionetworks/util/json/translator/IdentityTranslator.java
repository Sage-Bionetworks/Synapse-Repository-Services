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

	/**
	 * This translator will only be used when the JavaType <F> matches the provide type exactly.
	 * @param type
	 */
	public IdentityTranslator(Class<? extends T> type) {
		ValidateArgument.required(type, "type");
		this.type = type;
	}

	@Override
	public boolean canTranslate(Class<?> javaType) {
		return this.type.equals(javaType);
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
