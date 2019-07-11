package org.sagebionetworks.file.worker;

import java.util.HashMap;
import java.util.Map;

import org.sagebionetworks.util.ValidateArgument;

/**
 * This class is state-full and a new instance should be used for each zip file.
 * 
 */
public class FlatZipEntryNameProvider implements ZipEntryNameProvider {

	Map<String, Integer> fileNamesCount;

	public FlatZipEntryNameProvider() {
		fileNamesCount = new HashMap<String, Integer>();
	}

	@Override
	public String createZipEntryName(final String fileName, final Long fileHandleId) {
		ValidateArgument.required(fileName, "fileName");

		Integer fileNameCount = fileNamesCount.getOrDefault(fileName, 0);
		String finalFileName;

		if (fileNameCount == 0) {
			// All good, first time we see this file
			finalFileName = fileName;
			fileNameCount++;
		} else {
			// Split over the first dot to create versioned name
			int suffixIndex = fileName.indexOf(".");
			
			if (suffixIndex < 0) {
				suffixIndex = fileName.length();
			}
			
			String prefix = fileName.substring(0, suffixIndex);
			String suffix = fileName.substring(suffixIndex, fileName.length());

			// Try to generate a version until there is no collision
			do {
				finalFileName = prefix + "(" + fileNameCount + ")" + suffix;
				fileNameCount++;
			} while (fileNamesCount.containsKey(finalFileName));
			
		}
		
		fileNamesCount.put(fileName, fileNameCount);

		return finalFileName;

	}

}
