package org.sagebionetworks.repo.web.service.metadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Preview;
import org.sagebionetworks.repo.model.Row;
import org.sagebionetworks.repo.model.UserInfo;

/**
 *
 */
public class PreviewMetadataProvider implements
		TypeSpecificMetadataProvider<Preview> {

	@Override
	public void addTypeSpecificMetadata(Preview entity,
			HttpServletRequest request, UserInfo user, EventType eventType) {
		// Clear the blob and set the string
		if (entity.getPreviewString() != null) {
			try {
				createPreviewMap(entity);
			} catch (DatastoreException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}

	/**
	 * Given a preview build the header and map.
	 * @param preview
	 * @throws DatastoreException
	 */
	public static void createPreviewMap(Preview preview) throws DatastoreException {
		String rawPreview = preview.getPreviewString();
		if (rawPreview == null)
			return;

		// Split the lines
		String lines[] = rawPreview.split("(?m)\n");
		String header[] = lines[0].split("\t");
		int minColumns = 2;
		int minLines = 4;

		// Confirm that we are able to interpret this as a tab-delimited file
		if ((header.length < minColumns) || (lines.length < minLines)) {
			// This means we will not set the header or rows
			return;
//			throw new DatastoreException("Unable to convert preview data to map format");
		}
		// These are our headers
		preview.setHeaders(Arrays.asList(header));
		List<Row> results = new ArrayList<Row>();
		for (int row = 1; row < lines.length; row++) {
			Row result = new Row();
			String values[] = lines[row].split("\t");

			// Confirm that the tab-delimited data is well-formed
			if (header.length != values.length) {
				throw new DatastoreException("Unable to convert preview data to map format");
			}
			result.setCells(Arrays.asList(values));
			results.add(result);
		}
		preview.setRows(results);
	}
}
