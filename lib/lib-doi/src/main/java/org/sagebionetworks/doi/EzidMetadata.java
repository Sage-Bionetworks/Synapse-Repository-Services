package org.sagebionetworks.doi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * EZID metadata stored with the DOI.
 */
public class EzidMetadata {

	public String getTitle() {
		if (title == null || title.isEmpty()) {
			throw new NullPointerException("Missing title. Title is required");
		}
		return title;
	}

	public void setTitle(String title) {
		if (title == null || title.isEmpty()) {
			throw new IllegalArgumentException("Missing title. Title is required");
		}
		this.title = title;
	}

	public String getCreator() {
		if (creator == null || creator.isEmpty()) {
			throw new NullPointerException("Missing creator. Creator is required");
		}
		return creator;
	}

	public void setCreator(String creator) {
		if (creator == null || creator.isEmpty()) {
			throw new IllegalArgumentException("Missing creator. Creator is required");
		}
		this.creator = creator;
	}

	public String getTarget() {
		if (target == null || target.isEmpty()) {
			throw new NullPointerException("Missing target. Target is required");
		}
		return target;
	}

	public void setTarget(String target) {
		if (target == null || target.isEmpty()) {
			throw new IllegalArgumentException("Missing target. Target is required");
		}
		this.target = target;
	}

	public String getPublisher() {
		if (publisher == null || publisher.isEmpty()) {
			throw new NullPointerException("Missing publisher. Publisher is required");
		}
		return publisher;
	}

	public void setPublisher(String publisher) {
		if (publisher == null || publisher.isEmpty()) {
			throw new IllegalArgumentException("Missing publisher. Publisher is required");
		}
		this.publisher = publisher;
	}

	public int getPublicationYear() {
		return publicationYear;
	}

	public void setPublicationYear(int publicationYear) {
		this.publicationYear = publicationYear;
	}

	/**
	 * This is the minimum required metadata to create a DataCite DOI via EZID.
	 * Special characters in the values are encoded by percent-encoding per EZID documentation.
	 */
	public String getMetadataAsString() {
		StringBuilder builder = new StringBuilder();
		builder.append(FIELD_TITLE + SEPARATOR + " ").append(getTitle()).append("\r\n");
		builder.append(FIELD_CREATOR + SEPARATOR + " ").append(getCreator()).append("\r\n");
		builder.append(FIELD_PUBLISHER + SEPARATOR + " ").append(getPublisher()).append("\r\n");
		builder.append(FIELD_PUBLICATION_YEAR + SEPARATOR + " ").append(Integer.toString(publicationYear)).append("\r\n");
		builder.append(FIELD_TARGET + SEPARATOR + " ").append(encode(getTarget()));
		return builder.toString();
	}

	void initFromString(String metadata) {

		if (metadata == null || metadata.isEmpty()) {
			throw new IllegalArgumentException("Metadata cannot be null.");
		}

		this.originalMetadata = metadata;

		try {
			BufferedReader r = new BufferedReader(new StringReader(metadata));
			String line = r.readLine();
			while (line != null) {
				final int splitAt = line.indexOf(SEPARATOR);
				if (splitAt > 0 && splitAt < line.length() - 1) {
					String value = line.substring(splitAt + 1, line.length());
					value = value.trim();
					String field = line.substring(0, splitAt);
					field = field.trim().toLowerCase();
					if (FIELD_TITLE.equals(field)) {
						this.setTitle(value);
					} else if (FIELD_CREATOR.equals(field)) {
						this.setCreator(value);
					} else if (FIELD_PUBLISHER.equals(field)) {
						this.setPublisher(value);
					} else if (FIELD_PUBLICATION_YEAR.equals(field)) {
						this.setPublicationYear(Integer.valueOf(value));
					} else if (FIELD_TARGET.equals(field)) {
						this.setTarget(value);
					}
 				}
				line = r.readLine();
			}
			r.close();
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	/**
	 * This is the response string from the get operation.
	 * Besides the required fields, these additional fields
	 * appear in the response. Example:
	 *
	 * success: doi:10.5072/FK2.5FB2B17A
	 * _ownergroup: apitest
	 * _owner: apitest
	 * _created: 1365186258
	 * _updated: 1365186258
	 * _profile: datacite
	 * _status: public
	 * _shadowedby: ark:/b5072/fk2.5fb2b17a
	 * _export: yes
	 *
	 */
	String getOriginalMetadata() {
		return originalMetadata;
	}

	private String encode(String value) {
		try {
			return URLEncoder.encode(value, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Error occurred while encoding " + value, e);
		}
	}

	private static final char SEPARATOR = ':';
	private static final String FIELD_TITLE = "datacite.title";
	private static final String FIELD_CREATOR = "datacite.creator";
	private static final String FIELD_PUBLISHER = "datacite.publisher";
	private static final String FIELD_PUBLICATION_YEAR = "datacite.publicationyear";
	private static final String FIELD_TARGET = "_target";

	// Required
	private String title;
	private String creator;
	private String publisher;
	private int publicationYear;
	private String target;

	// Optional. This is a temporary holder
	// of the response string from the get operation.
	// Besides the required fields, these additional
	// fields appear in the response. Example:
	//
	//		success: doi:10.5072/FK2.5FB2B17A
	//		_ownergroup: apitest
	//		_owner: apitest
	//		_created: 1365186258
	//		_updated: 1365186258
	//		_profile: datacite
	//		_status: public
	//		_shadowedby: ark:/b5072/fk2.5fb2b17a
	//		_export: yes
	//
	private String originalMetadata;
}
