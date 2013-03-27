package org.sagebionetworks.doi;

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
	 * This is all the metadata minus the DOI. Special characters in the values are encoded
	 * by percent-encoding per EZID documentation.
	 */
	public String getMetadataAsString() {
		StringBuilder builder = new StringBuilder();
		builder.append("datacite.title: ").append(encode(getTitle())).append("\r\n");
		builder.append("datacite.creator: ").append(encode(getCreator())).append("\r\n");
		builder.append("datacite.publisher: ").append(encode(getPublisher())).append("\r\n");
		builder.append("datacite.publicationyear: ").append(Integer.toString(publicationYear)).append("\r\n");
		builder.append("_target: ").append(encode(getTarget()));
		return builder.toString();
	}

	private String encode(String value) {
		try {
			return URLEncoder.encode(value, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Error occurred while encoding " + value, e);
		}
	}

	private String title;
	private String creator;
	private String publisher;
	private int publicationYear;
	private String target;
}
