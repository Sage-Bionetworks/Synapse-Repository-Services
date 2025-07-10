package org.sagebionetworks.repo.model.grid;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.sagebionetworks.util.ValidateArgument;

public class GridUtils {

	/**
	 * Convert a grid session ID to a string.
	 * 
	 * @param id
	 * @return
	 */
	public static String gridSessionIdAsString(Long id) {
		ValidateArgument.required(id, "id");
		return new String(Base64.getEncoder().encode(id.toString().getBytes(StandardCharsets.UTF_8)),
				StandardCharsets.UTF_8);
	}

	/**
	 * Convert a grid session ID to a long;
	 * 
	 * @param id
	 * @return
	 */
	public static Long gridSessionIdAsLong(String id) {
		ValidateArgument.required(id, "id");
		return Long.parseLong(
				new String(Base64.getDecoder().decode(id.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
	}

}
