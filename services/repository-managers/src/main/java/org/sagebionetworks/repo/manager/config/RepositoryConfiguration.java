package org.sagebionetworks.repo.manager.config;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.runtime.resource.loader.FileResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RepositoryConfiguration {
	
	private static final String VELOCITY_RESOURCE_LOADERS = "classpath,file";
	private static final String VELOCITY_PARAM_CLASSPATH_LOADER_CLASS = "classpath.resource.loader.class";
	private static final String VELOCITY_PARAM_FILE_LOADER_CLASS = "file.resource.loader.class";
	private static final String VELOCITY_PARAM_RUNTIME_REFERENCES_STRICT = "runtime.references.strict";

	/**
	 * @return The velocity engine instance that can be used within the managers
	 */
	@Bean
	public VelocityEngine velocityEngine() {
		VelocityEngine engine = new VelocityEngine();
		engine.setProperty(RuntimeConstants.RESOURCE_LOADER, VELOCITY_RESOURCE_LOADERS); 
		engine.setProperty(VELOCITY_PARAM_CLASSPATH_LOADER_CLASS, ClasspathResourceLoader.class.getName());
		engine.setProperty(VELOCITY_PARAM_FILE_LOADER_CLASS, FileResourceLoader.class.getName());
		engine.setProperty(VELOCITY_PARAM_RUNTIME_REFERENCES_STRICT, true);
		return engine;
	}
}
