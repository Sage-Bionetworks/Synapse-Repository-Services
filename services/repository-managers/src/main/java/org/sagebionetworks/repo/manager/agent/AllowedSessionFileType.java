package org.sagebionetworks.repo.manager.agent;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The whitelist of file types that may be added to a code interpreter session. Each type pairs a file
 * name extension with the content type(s) that identify it. A file is admitted if either its
 * FileHandle content type matches one of the {@link #getContentTypes()} OR its file name ends with the
 * {@link #getExtension()} — uploaded files often carry a generic content type (e.g.
 * {@code application/octet-stream}), so the extension is a necessary fallback.
 */
public enum AllowedSessionFileType {

	PDF("pdf", "application/pdf"),
	CSV("csv", "text/csv", "application/csv"),
	TXT("txt", "text/plain"),
	JSON("json", "application/json", "text/json"),
	TSV("tsv", "text/tab-separated-values"),
	MAF("maf", "text/tab-separated-values");

	private final String extension;
	private final Set<String> contentTypes;

	AllowedSessionFileType(String extension, String... contentTypes) {
		this.extension = extension;
		this.contentTypes = Set.of(contentTypes);
	}

	public String getExtension() {
		return extension;
	}

	public Set<String> getContentTypes() {
		return contentTypes;
	}

	/**
	 * Resolves the allowed type for a file from its content type and file name, matching on either.
	 * Content type is matched case-insensitively and ignores any parameters (e.g. {@code ; charset=utf-8});
	 * the file name is matched on its lower-case extension.
	 *
	 * @param contentType The FileHandle content type; may be null.
	 * @param fileName     The file name; may be null.
	 * @return The matching type, or empty if the file is not an allowed type.
	 */
	public static Optional<AllowedSessionFileType> match(String contentType, String fileName) {
		String normalizedContentType = normalizeContentType(contentType);
		String extension = extensionOf(fileName);
		for (AllowedSessionFileType type : values()) {
			if (normalizedContentType != null && type.contentTypes.contains(normalizedContentType)) {
				return Optional.of(type);
			}
			if (extension != null && type.extension.equals(extension)) {
				return Optional.of(type);
			}
		}
		return Optional.empty();
	}

	/**
	 * A human-readable description of the whitelist for use in error messages, e.g. "PDF, CSV, TXT, JSON".
	 */
	public static String describeAllowed() {
		return Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));
	}

	private static String normalizeContentType(String contentType) {
		if (contentType == null) {
			return null;
		}
		int semicolon = contentType.indexOf(';');
		String bare = semicolon >= 0 ? contentType.substring(0, semicolon) : contentType;
		return bare.trim().toLowerCase(Locale.ROOT);
	}

	private static String extensionOf(String fileName) {
		if (fileName == null) {
			return null;
		}
		int dot = fileName.lastIndexOf('.');
		if (dot < 0 || dot == fileName.length() - 1) {
			return null;
		}
		return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
	}
}
