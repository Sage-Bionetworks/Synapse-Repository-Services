package org.sagebionetworks.translator;

import java.io.FileWriter;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
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
	private boolean shouldRun;

	@Override
	public void init(Locale locale, Reporter reporter) {
		reporter.print(Kind.NOTE, "Doclet using locale: " + locale);
		this.reporter = reporter;
	}

	public boolean run(DocletEnvironment env) {
		if (!shouldRun) {
			reporter.print(Kind.NOTE, "The ControllerModelDoclet will not run since the 'shouldRun' parameter was not set to 'true'");
			return true;
		}
		try {
			reporter.print(Kind.NOTE, "Starting ControllerModelDoclet");
			
			// load ServerSideOnlyFactory based on class path passed in
			Class c = Class.forName(serverSideFactoryPath);
			Method method = c.getMethod("getKeySetIterator", null);
			Iterator<String> concreteClassnames = (Iterator<String>) method
					.invoke(c.getDeclaredConstructor().newInstance(), null);
			ControllersToOpenApiJsonTranslator translator = new ControllersToOpenApiJsonTranslator();
			JSONObject openAPIJson = translator.translate(env, concreteClassnames, reporter);

			Files.createDirectories(Paths.get(targetFile).getParent());
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
		Option shouldRunDoclet = new Option() {
			private final List<String> option = Arrays.asList("--should-run");

			@Override
			public int getArgumentCount() {
				return 1;
			}

			@Override
			public String getDescription() {
				return "When set to anything but 'true', the doclet will not run. Example: --should-run,true";
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
				return "Describes whether the ControllerModelDoclet should run or not.";
			}

			@Override
			public boolean process(String opt, List<String> arguments) {
				shouldRun = arguments.get(0).equals("true");
				return true;
			}
		};

		Option[] options = { targetDir, factoryPath, shouldRunDoclet };
		return new HashSet<>(Arrays.asList(options));
	}

	@Override
	public SourceVersion getSupportedSourceVersion() {
		return SourceVersion.latest();
	}
}
