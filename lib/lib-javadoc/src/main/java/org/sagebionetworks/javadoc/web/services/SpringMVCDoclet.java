package org.sagebionetworks.javadoc.web.services;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.lang.model.SourceVersion;
import javax.tools.Diagnostic.Kind;
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
import org.sagebionetworks.javadoc.velocity.ContextInput;
import org.sagebionetworks.javadoc.velocity.ContextInputImpl;
import org.sagebionetworks.javadoc.velocity.controller.ControllerContextGenerator;
import org.sagebionetworks.javadoc.velocity.schema.CSVExampleContextGenerator;
import org.sagebionetworks.javadoc.velocity.schema.SchemaClassContextGenerator;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

/**
 * Java Doclet for generating javadocs for Spring MVC web-services.
 * 
 * @author jmhill
 * 
 */
public class SpringMVCDoclet implements Doclet {

	private final Linker linker;
	private final List<ClassContextGenerator> generators;

	private File outputDirectory;
	private String authControllerName;
	private Reporter reporter;

	public SpringMVCDoclet() {
		this.linker = new PropertyRegExLinker();
		this.generators = List.of(new SchemaClassContextGenerator(), new ControllerContextGenerator(),
				new CSVExampleContextGenerator());
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
	@Override
	public boolean run(DocletEnvironment root) {
		try {
			CopyBaseFiles.copyDirectory(outputDirectory);

			// Run all of the generators
			List<ClassContext> contextList = new LinkedList<ClassContext>();
			ContextFactory factory = new ContextFactoryImpl();
			ContextInput input = new ContextInputImpl(factory, root, authControllerName);
			for (ClassContextGenerator generator : generators) {
				// let each generator generate the context objects.
				List<ClassContext> subList = generator.generateContext(input);
				contextList.addAll(subList);
			}
			// Merge the context of each object.
			VelocityEngine ve = new VelocityEngine();
			ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
			ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
			ve.setProperty("runtime.references.strict", true);
			List<FileLink> links = new ArrayList<>(contextList.size());
			for (ClassContext classContext : contextList) {
				System.out.println(classContext);
				// The velocity context provides the model data for the view.
				Context context = classContext.getContext();
				// Create the empty file
				File file = BasicFileUtils.createNewFileForClassName(outputDirectory, classContext.getClassName(),
						"html");
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
			var contollers = FilterUtils.controllerIterator(root);
			while (contollers.hasNext()) {
				var cd = contollers.next();
				FileLink link = new FileLink(index, cd.getQualifiedName().toString());
				link.setHashTagId(true);
				links.add(link);
			}
			// Link all of the files.
			linker.link(outputDirectory, links);
			return true;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void init(Locale locale, Reporter reporter) {
		reporter.print(Kind.NOTE, "Doclet using locale: " + locale);
		this.reporter = reporter;
	}

	@Override
	public String getName() {
		return SpringMVCDoclet.class.getSimpleName();
	}

	@Override
	public Set<? extends Option> getSupportedOptions() {
		Option outputDir = new Option() {

			@Override
			public int getArgumentCount() {
				return 1;
			}

			@Override
			public String getDescription() {
				return "The standard doclet output directory.";
			}

			@Override
			public Option.Kind getKind() {
				return Option.Kind.STANDARD;
			}

			@Override
			public List<String> getNames() {
				return List.of("-d");
			}

			@Override
			public String getParameters() {
				return "";
			}

			@Override
			public boolean process(String opt, List<String> arguments) {
				outputDirectory = new File(arguments.get(0));
				return true;
			}
		};
		Option authConOptions = new Option() {

			@Override
			public int getArgumentCount() {
				return 1;
			}

			@Override
			public String getDescription() {
				return "The full class name of the Authentication Controller.";
			}

			@Override
			public Option.Kind getKind() {
				return Option.Kind.STANDARD;
			}

			@Override
			public List<String> getNames() {
				return List.of("-authControllerName");
			}

			@Override
			public String getParameters() {
				return "";
			}

			@Override
			public boolean process(String opt, List<String> arguments) {
				authControllerName = arguments.get(0);
				return true;
			}
		};
		return Set.of(outputDir, authConOptions);
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latest();
	}
}
