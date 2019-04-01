package org.sagebionetworks.javadoc.velocity.schema;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.velocity.context.Context;
import org.sagebionetworks.javadoc.velocity.ClassContext;
import org.sagebionetworks.javadoc.velocity.ClassContextGenerator;
import org.sagebionetworks.javadoc.velocity.ContextFactory;
import org.sagebionetworks.javadoc.velocity.controller.ControllerUtils;
import org.sagebionetworks.javadoc.web.services.FilterUtils;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.RootDoc;

import au.com.bytecode.opencsv.CSVReader;

/**
 * This Generator creates static example HTML page from input CSV files for any
 * class with the CSVGeneratedExample annotation.
 * 
 * @author jmhill
 * 
 */
public class CSVExampleContextGenerator implements ClassContextGenerator {

	@Override
	public List<ClassContext> generateContext(ContextFactory factory,
			RootDoc root) throws Exception {
		List<ClassContext> list = new LinkedList<ClassContext>();
		// Find the example classes
		Iterator<ClassDoc> examples = FilterUtils.csvExampleIterator(root
				.classes());
		// Generate a page for each example
		while (examples.hasNext()) {
			// Create a class for each example
			ClassDoc exampleDoc = examples.next();
			Map<String, Object> annotationMap = ControllerUtils
					.mapAnnotation(exampleDoc.annotations());
			String csvName = (String) annotationMap
					.get("org.sagebionetworks.repo.web.rest.doc.CSVGeneratedExample.csvFileName");
			if (csvName == null)
				throw new IllegalArgumentException(
						"csvFileName cannot be null for CSVGeneratedExample");
			// Make sure we can find the CSV on the classpath
			InputStream in = getClass().getClassLoader().getResourceAsStream(csvName);
			if (in == null){
				throw new IllegalArgumentException(
						"Cannot find the file referenced in CSVGeneratedExample.csvFileName= "
								+ csvName + " on the classpath");
			}

			try {
				InputStreamReader reader = new InputStreamReader(in, "UTF-8");
				CSVReader csvReader = new CSVReader(reader);
				// Create the context
				list.add(createContext(factory.createNewContext(), exampleDoc.qualifiedName(), csvReader.readAll()));
			} finally {
				in.close();
			}
		}
		return list;
	}

	/**
	 * Create the class context for a single CSV input file.
	 * @param context
	 * @param className
	 * @param csvData
	 * @return
	 */
	public static ClassContext createContext(Context context, String className,
			List<String[]> csvData) {
		// Populate the context with the data from this CSV
		Map<String, Category> map = new HashMap<String, Category>();
		List<Category> categories = new LinkedList<Category>();
		for (String[] row : csvData) {
			if (row.length != 3) {
				throw new IllegalArgumentException(
						"Expected 3 columns in the CSV but found: "
								+ row.length);
			}
			String categoryName = StringEscapeUtils.escapeHtml4(row[0].trim());
			Category category = map.get(categoryName);
			if (category == null) {
				category = new Category(categoryName);
				// Add it to the map and list
				map.put(categoryName, category);
				categories.add(category);
			}
			String description = StringEscapeUtils.escapeHtml4(row[1]);
			String value = StringEscapeUtils.escapeHtml4(row[2]);
			Example example = new Example(description, value);
			category.add(example);
		}
		context.put("categories", categories);
		return new ClassContext(className, "csvExampleTemplate.html", context);
	}

}
