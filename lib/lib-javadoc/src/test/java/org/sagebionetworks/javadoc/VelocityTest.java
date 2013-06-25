package org.sagebionetworks.javadoc;

import java.io.InputStream;
import java.io.StringWriter;
import java.util.ArrayList;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.junit.Test;

public class VelocityTest {

	@Test
	public void test() {
		String resource = "VelocityExampleTemplate.vm";
		InputStream in = VelocityTest.class.getClassLoader().getResourceAsStream(resource);
		if(in == null) throw new IllegalArgumentException("Cannot find: "+resource+" on classpath");
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath"); 
		ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		// The context creates the data that we want in this template.
		VelocityContext context = new VelocityContext();
		context.put("list", getNames());
		context.put("name", "joe");
		Template template = ve.getTemplate(resource);
		StringWriter writer = new StringWriter();
		template.merge(context, writer);
		System.out.println(writer.toString());
	}

	public ArrayList getNames() {
		ArrayList list = new ArrayList();
		list.add("ArrayList element 1");
		list.add("ArrayList element 2");
		list.add("ArrayList element 3");
		list.add("ArrayList element 4");
		return list;
	}
}
