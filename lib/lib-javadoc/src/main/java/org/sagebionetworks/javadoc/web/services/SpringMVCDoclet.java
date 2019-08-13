package org.sagebionetworks.javadoc.web.services;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.FileUtils;
import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.sagebionetworks.javadoc.BasicFileUtils;
import org.sagebionetworks.javadoc.CopyBaseFiles;
import org.sagebionetworks.javadoc.linker.FileLink;
import org.sagebionetworks.javadoc.linker.Linker;
import org.sagebionetworks.javadoc.linker.PropertyRegExLinker;
import org.sagebionetworks.javadoc.velocity.ClassContext;
import org.sagebionetworks.javadoc.velocity.ClassContextGenerator;
import org.sagebionetworks.javadoc.velocity.ContextFactory;
import org.sagebionetworks.javadoc.velocity.ContextFactoryImpl;
import org.sagebionetworks.javadoc.velocity.controller.ControllerContextGenerator;
import org.sagebionetworks.javadoc.velocity.schema.CSVExampleContextGenerator;
import org.sagebionetworks.javadoc.velocity.schema.SchemaClassContextGenerator;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.RootDoc;

/**
 * Java Doclet for generating javadocs for Spring MVC web-services.
 * 
 * @author jmhill
 * 
 */
public class SpringMVCDoclet {

	static Linker linker = new PropertyRegExLinker();
	static List<ClassContextGenerator> generators = new LinkedList<ClassContextGenerator>();
	static {
		generators.add(new SchemaClassContextGenerator());
		generators.add(new ControllerContextGenerator());
		generators.add(new CSVExampleContextGenerator());
	}

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
		// Pass this along to the standard doclet
		// First determine the output directory.
		File outputDirectory = getOutputDirectory(root.options());

		// Copy all of the base file to the output directory
		CopyBaseFiles.copyDirectory(outputDirectory);

		// Run all of the generators
		List<ClassContext> contextList = new LinkedList<ClassContext>();
		ContextFactory factory = new ContextFactoryImpl();
		for (ClassContextGenerator generator : generators) {
			// let each generator generate the context objects.
			List<ClassContext> subList = generator.generateContext(factory,	root);
			contextList.addAll(subList);
		}
		// Merge the context of each object.
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath"); 
		ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		ve.setProperty("runtime.references.strict", true);
		List<FileLink> links = new ArrayList<>(contextList.size());
		for(ClassContext classContext: contextList){
			System.out.println(classContext);
			// The velocity context provides the model data for the view.
			Context context = classContext.getContext();
			// Create the empty file
			File file = BasicFileUtils.createNewFileForClassName(outputDirectory, classContext.getClassName(), "html");
        	FileLink link = new FileLink(file, classContext.getClassName());
        	links.add(link);
        	// Calculate the path to root.
        	String pathToRoot = BasicFileUtils.pathToRoot(outputDirectory, file);
        	context.put("pathToRoot", pathToRoot);
        	// Add the path to root to the context
			Template template = ve.getTemplate(classContext.getTemplateName());
			StringWriter writer = new StringWriter();
			// This will merge the template with the model creating the HTML string.
			template.merge(context, writer);
			// Write output to the file
			FileUtils.writeStringToFile(file, writer.toString(), "UTF-8");
		}
		// Map each controller to the index file
		String fileName = BasicFileUtils.getFileNameForClassName("index", "html");
		File index = new File(outputDirectory, fileName);
        Iterator<ClassDoc> contollers = FilterUtils.controllerIterator(root.classes());
        while(contollers.hasNext()){
        	ClassDoc cd = contollers.next();
        	FileLink link = new FileLink(index, cd.qualifiedName());
        	link.setHashTagId(true);
        	links.add(link);
        }
		// Link all of the files.
		linker.link(outputDirectory, links);
		return true;
	}

	/**
	 * Get the output directory.
	 * 
	 * @param options
	 * @return
	 */
	public static File getOutputDirectory(String[][] options) {
		String outputDirectoryPath = Options.getOptionValue(options,
				Options.DIRECTORY_FLAG);
		if (outputDirectoryPath == null) {
			outputDirectoryPath = System.getProperty("user.dir");
		}
		File outputDirectory = new File(outputDirectoryPath);
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs();
		}
		return outputDirectory;
	}

	/**
	 * Check for doclet added options here.
	 * 
	 * @return number of arguments to option. Zero return means option not
	 *         known. Negative value means error occurred.
	 */
	public static int optionLength(String option) {
		return 2;
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
