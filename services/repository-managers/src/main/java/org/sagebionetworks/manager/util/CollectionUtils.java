package org.sagebionetworks.manager.util;

import java.util.Collection;

import org.sagebionetworks.util.ValidateArgument;

public class CollectionUtils {

	/**
	 * Convert a collection of longs to a collection of strings.
	 * @param ids
	 * @return
	 */
	public static void convertLongToString(Collection<Long> in, Collection<String> out) {
		ValidateArgument.required(in, "input");
		ValidateArgument.required(out, "output");
		for(Long id: in){
			if(id != null){
				out.add(id.toString());
			}
		}
	}

	/**
	 * Convert a collection of strings to a collection of longs.
	 * @param ids
	 * @return
	 */
	public static void convertStringToLong(Collection<String> in, Collection<Long> out){
		ValidateArgument.required(in, "input");
		ValidateArgument.required(out, "output");
		for(String id: in){
			if(id != null){
				try {
					out.add(Long.parseLong(id));
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException(e);
				}
			}
		}
	}
}
