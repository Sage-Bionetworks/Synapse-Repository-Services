package org.sagebionetworks.repo.manager.file;

import java.util.regex.Pattern;

import org.sagebionetworks.util.ValidateArgument;

public class FileHandleUtils {
	private static final Pattern VALID_MD5_REGEX = Pattern.compile("^[0-9a-fA-F]{32}$");

	public static boolean isValidMd5Digest(String digest) {
		ValidateArgument.required(digest, "digest");
		return VALID_MD5_REGEX.matcher(digest).matches();
	}

}
