package org.sagebionetworks.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class EnumUtils {

	public static List<String> names(Enum<?> ...values) {
		if (values.length == 0) {
			return Collections.emptyList();
		}
		List<String> result = new ArrayList<>(values.length);
		for (Enum<?> other : values) {
			result.add(other.name());
		}
		return result;
	}
	
	public static List<String> names(List<Enum<?>> values) {
		return values.stream().map(Enum::name).collect(Collectors.toList());
	}
	
	public static List<String> names(Class<? extends Enum<?>> e) {
		return names(e.getEnumConstants());
	}
}
