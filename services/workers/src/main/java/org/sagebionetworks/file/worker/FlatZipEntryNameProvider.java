package org.sagebionetworks.file.worker;

import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.util.ValidateArgument;

/**
 * This class is state-full and a new instance should be used for each zip file.
 * 
 */
public class FlatZipEntryNameProvider implements ZipEntryNameProvider {
	
	Map<String, Integer> prefixCounts;
	
	public FlatZipEntryNameProvider() {
		prefixCounts = new HashMap<String, Integer>();
	}

	@Override
	public String createZipEntryName(String fileName, Long fileHandleId) {
		ValidateArgument.required(fileName, "fileName");
		int suffixIndex = fileName.indexOf(".");
		if(suffixIndex < 0) {
			suffixIndex = fileName.length();
		}
		String prefix = fileName.substring(0, suffixIndex);
		String suffix = fileName.substring(suffixIndex, fileName.length());
		// how many times have we seen this prefix?
		Integer prefixCount = prefixCounts.get(prefix);
		if(prefixCount == null) {
			prefixCount = 0;
		}
		prefixCount++;
		prefixCounts.put(prefix, prefixCount);
		if(prefixCount > 1) {
			prefix += "("+(prefixCount-1)+")";
		}
		return prefix+suffix;
	}

}
