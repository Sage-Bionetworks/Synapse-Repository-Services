package org.sagebionetworks.cli;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;

/**
 * This allows for command line arguments to Java-based synapse tools with a
 * mandatory emphasis of credentials being stored in a properties file to help
 * users protect their credentials. Note that those credentials are stored in
 * clear text.
 * 
 * This class is intended for tools to be used by *normal* users of Synapse. It
 * is too much to ask them to create a properties file containing encrypted
 * passwords like we do for Platform code using TemplatedConfiguration library
 * StackConfiguration
 * 
 * @author deflaux
 * 
 */
public class CommandLineInterface {
	
	private static final String INSTRUCTIONS = "Note that all command line properties may be specified in the credentials file instead.  "
			+ "Also note that the password and/or hmac MUST be specified in the credentials file and not on the command line.  "
			+ "A sample credentials file might contain the following:\n"
			+ "\t\t username=YOUR_EMAIL_ADDRESS\n"
			+ "\t\t password=YOUR_SYNAPSE_PASSWORD\n" + "\nHave fun!\n";

	private static final String PROPS_ENVIRONMENT_VARNAME = "SYNAPSE_CREDENTIAL_FILE";
	private static final String DEFAULT_PROPERTIES_FILE = System
			.getProperty("user.home")
			+ File.separator + "synapse.properties";

	private String propertyFilename = DEFAULT_PROPERTIES_FILE;
	private Properties properties = new Properties();
	private Set<String> requiredOptions = new HashSet<String>();
	private Options options;
	private String title;

	/**
	 * Create a new command line argument parser configured with all the basic
	 * synapse options
	 * 
	 * @param title
	 */
	public CommandLineInterface(String title) {
		this.title = title;
		requiredOptions.add("username");
		options = new Options();
		options
				.addOption("e", "repoEndpoint", true,
						"the repository service endpoint (e.g. https://repo.sagebase.org/repo/v1)");
		options
				.addOption("a", "authEndpoint", true,
						"the authentication service endpoint (e.g. https://auth.sagebase.org/auth/v1)");
		options
				.addOption(
						"c",
						"credentialFile",
						true,
						"the path to the Synapse credentials file, alternatively set environment variable "
								+ PROPS_ENVIRONMENT_VARNAME);
		options.addOption("d", "debug", false, "print out debugging information");
		options.addOption("h", "help", false, "print this usage message");
	}

	/**
	 * Configure a custom command line argument or property
	 * 
	 * TODO protect against option conflicts
	 * 
	 * @param opt
	 * @param longOpt
	 * @param hasArg
	 * @param description
	 * @param required
	 */
	public void addOption(String opt, String longOpt, boolean hasArg,
			String description, boolean required) {
		options.addOption(opt, longOpt, hasArg, description);
		if (required) {
			requiredOptions.add(longOpt);
		}
	}

	/**
	 * Pass in the command line arguments to be parsed and allow them to
	 * override any properties specified in the properties file
	 * 
	 * @param args
	 * @throws SynapseException
	 */
	@SuppressWarnings({ "cast", "unchecked" })
	public void processArguments(String args[]) throws SynapseException {
		try {
			CommandLineParser parser = new PosixParser();
			CommandLine commandLine = parser.parse(options, args);

			if (commandLine.hasOption("help")) {
				printUsage("How to use this tool");
				System.exit(0);
			}

			if (commandLine.hasOption("credentialFile")) {
				propertyFilename = commandLine.getOptionValue("credentialFile");
			} else if (null != System.getenv(PROPS_ENVIRONMENT_VARNAME)) {
				propertyFilename = System.getenv(PROPS_ENVIRONMENT_VARNAME);
			}

			File credentialsFile = new File(propertyFilename);
			if (!credentialsFile.canRead()) {
				throw new SynapseException(
						"Unable to open Synapse credentials file: "
								+ credentialsFile.getAbsolutePath());
			}
			properties.load(new FileInputStream(credentialsFile));

			// Command line options override those specified in the credentials
			// file
			Iterator<Option> iter = commandLine.iterator();
			while (iter.hasNext()) {
				Option option = iter.next();
				String value = (null == option.getValue()) ? "" : option.getValue();
				properties.put(option.getOpt(), value);
				properties.put(option.getLongOpt(), value);
			}

			if (!properties.containsKey("password")
					&& !properties.containsKey("hmac")) {
				throw new SynapseException("Credentials file "
						+ credentialsFile
						+ " must contain either 'password' or 'hmac'");
			}

			for (String requiredOption : requiredOptions) {
				if (!properties.containsKey(requiredOption)) {
					throw new SynapseException("Argument '" + requiredOption
							+ "' is required.");
				}
			}
			
			if(properties.containsKey("debug")) {
				LogManager.getLogger(Synapse.class).setLevel((Level)Level.DEBUG);
			}
		} catch (SynapseException e) {
			printUsage(e.getMessage());
			throw e;
		} catch (Exception e) {
			printUsage(e.getMessage());
			throw new SynapseException(e);
		}
	}

	/**
	 * Get the value of the argument regardless of whether it was configured via
	 * the command line or properties file
	 * 
	 * @param key
	 * @return the value of the command line argument or property from the
	 *         credentials file
	 */
	public String getArgument(String key) {
		return properties.getProperty(key);
	}

	/**
	 * @return an instance of Synapse client with the endpoints configured (if
	 *         applicable) and the user logged in
	 * @throws SynapseException
	 */
	public Synapse getSynapseClient() throws SynapseException {
		Synapse synapse = new Synapse();
		if (properties.containsKey("repoEndpoint")) {
			synapse.setRepositoryEndpoint(properties
					.getProperty("repoEndpoint"));
		}
		if (properties.containsKey("authEndpoint")) {
			synapse.setAuthEndpoint(properties.getProperty("authEndpoint"));
		}

		if (properties.containsKey("hmac")) {
			synapse.setUserName(properties.getProperty("username"));
			synapse.setApiKey(properties.getProperty("hmac"));
		} else {
			synapse.login(properties.getProperty("username"), properties
					.getProperty("password"));
		}
		return synapse;
	}

	/**
	 * Display the usage information for this tool
	 * 
	 * @param extraMessage
	 */
	public void printUsage(String extraMessage) {
		HelpFormatter helpFormatter = new HelpFormatter();
		helpFormatter.setWidth(80);
		helpFormatter.printHelp(title, extraMessage, options, INSTRUCTIONS);
	}
}
