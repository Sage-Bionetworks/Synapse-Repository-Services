package org.sagebionetworks.repo.web.rest.doc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation provides additional information used to generating the REST API documents.
 * When the REST document generator detects a class with this annotation, a static HTML page
 * will be generated with the full package name of the class.  The provided csvFileName
 * must be a valid file available on the classpath.  The csv file will be used to generate
 * the static content of the page.
 * 
 * A link to the static page can be added using the full package name of the class.
 * 
 * @author John
 *
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CSVGeneratedExample {
	
	/**
	 * The name of the CSV file that will be used to generate the example static HTML page.
	 * The format of the CSV must be composed of three columns where each row is one example.
	 * The first column of an example is the category name. The second column is the description
	 * of the example.  The third column is the raw example text.
	 * The generated page will have a header for each unique category.  Under each header will be
	 * a list each description followed by the example.
	 * 
	 * @return
	 */
	String csvFileName();
	

}
