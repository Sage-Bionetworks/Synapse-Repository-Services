package org.sagebionetworks.web.client;

import com.google.gwt.i18n.client.Constants;

/**
 * Provides the display names for dataset column names. GWT will auto-generate
 * an implementation based on the the property file of the same name.
 * 
 * @author jmhill
 * 
 */
public interface DatasetConstants extends Constants {

	public String name();

	public String layers();

	public String totalSamples();

	public String status();

	public String species();

	public String tissueType();

	public String disease();

	public String investigator();

	public String datePosted();

	public String dateModified();
}
