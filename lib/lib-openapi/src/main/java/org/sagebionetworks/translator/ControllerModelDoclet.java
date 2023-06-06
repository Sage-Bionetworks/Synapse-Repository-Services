package org.sagebionetworks.translator;

import java.io.FileWriter;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.lang.model.SourceVersion;
import javax.tools.Diagnostic.Kind;

import org.json.JSONObject;

import jdk.javadoc.doclet.Doclet;
import jdk.javadoc.doclet.DocletEnvironment;
import jdk.javadoc.doclet.Reporter;

public class ControllerModelDoclet implements Doclet {
	private Reporter reporter;
	private String targetFile;
	private String serverSideFactoryPath;

	@Override
	public void init(Locale locale, Reporter reporter) {
		reporter.print(Kind.NOTE, "Doclet using locale: " + locale);
		this.reporter = reporter;
	}

	public boolean run(DocletEnvironment env) {
		try {
			// load ServerSideOnlyFactory based on class path passed in
			Class c = Class.forName(serverSideFactoryPath);
			Method method = c.getMethod("getKeySetIterator", null);
			Iterator<String> concreteClassnames = (Iterator<String>) method.invoke(c.getDeclaredConstructor().newInstance(), null);
			
			ControllersToOpenAPIJsonTranslator translator = new ControllersToOpenAPIJsonTranslator();
			JSONObject openAPIJson = translator.translate(env, concreteClassnames);
			
			// write resulting json to file
			try (FileWriter fileWriter = new FileWriter(targetFile, false)) {
				fileWriter.write(openAPIJson.toString(5));
				return true;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String getName() {
		return "ControllerModelDoclet";
	}

	@Override
	public Set<? extends Option> getSupportedOptions() {
		Option targetDir = new Option() {
			private final List<String> option = Arrays.asList("--target-file");

			@Override
			public int getArgumentCount() {
				return 1;
			}

			@Override
			public String getDescription() {
				return "The absolute path of the out file";
			}

			@Override
			public Option.Kind getKind() {
				return Option.Kind.STANDARD;
			}

			@Override
			public List<String> getNames() {
				return option;
			}

			@Override
			public String getParameters() {
				return "target file";
			}

			@Override
			public boolean process(String opt, List<String> arguments) {
				targetFile = arguments.get(0);
				return true;
			}
		};
		
		Option factoryPath = new Option() {
			private final List<String> option = Arrays.asList("--factory-path");

			@Override
			public int getArgumentCount() {
				return 1;
			}

			@Override
			public String getDescription() {
				return "The full package name of the server-side class.";
			}

			@Override
			public Option.Kind getKind() {
				return Option.Kind.STANDARD;
			}

			@Override
			public List<String> getNames() {
				return option;
			}

			@Override
			public String getParameters() {
				return "server-side factory full package name.";
			}

			@Override
			public boolean process(String opt, List<String> arguments) {
				serverSideFactoryPath = arguments.get(0);
				return true;
			}
		};
		
		Option[] options = {targetDir, factoryPath};
		return new HashSet<>(Arrays.asList(options));
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latest();
	}
}
