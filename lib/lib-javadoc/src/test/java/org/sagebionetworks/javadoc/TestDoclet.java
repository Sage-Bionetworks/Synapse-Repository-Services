package org.sagebionetworks.javadoc;

import java.io.IOException;

import javax.xml.stream.XMLStreamException;

import org.sagebionetworks.javadoc.web.services.Options;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.RootDoc;

public class TestDoclet {
	
	private static RootDoc lastRoot;
	/**
	 * The main entry point of the Doclet
	 * 
	 * @param root
	 * @return
	 * @throws XMLStreamException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	public static boolean start(RootDoc root) throws Exception {
		lastRoot = root;
		return true;
	}
	
	/**
	 * This method can only called onces after a start.
	 * @return
	 */
	public static RootDoc getLastRoot(){
		RootDoc root = lastRoot;
		// Clear the last root.
		lastRoot = null;
		return root;
	}

	/**
	 * Check for doclet added options here.
	 * 
	 * @return number of arguments to option. Zero return means option not
	 *         known. Negative value means error occurred.
	 */
	public static int optionLength(String option) {
		return Options.optionLength(option);
	}

	/**
	 * Check that options have the correct arguments here.
	 * <P>
	 * This method is not required and will default gracefully (to true) if
	 * absent.
	 * <P>
	 * Printing option related error messages (using the provided
	 * DocErrorReporter) is the responsibility of this method.
	 * 
	 * @return true if the options are valid.
	 */
	public static boolean validOptions(String[][] options,
			DocErrorReporter reporter) {
		System.out.println("options:");
		for (int i = 0; i < options.length; i++) {
			for (int j = 0; j < options[i].length; j++) {
				System.out.print(" " + options[i][j]);
			}
			System.out.println();
		}
		return true;
	}

	/**
	 * Indicate that this doclet supports the 1.5 language features.
	 * 
	 * @return JAVA_1_5, indicating that the new features are supported.
	 */
	public static LanguageVersion languageVersion() {
		return LanguageVersion.JAVA_1_5;
	}
}
