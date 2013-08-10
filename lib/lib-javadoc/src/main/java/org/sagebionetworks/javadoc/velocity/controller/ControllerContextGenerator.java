package org.sagebionetworks.javadoc.velocity.controller;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.velocity.context.Context;
import org.sagebionetworks.javadoc.velocity.ClassContext;
import org.sagebionetworks.javadoc.velocity.ClassContextGenerator;
import org.sagebionetworks.javadoc.velocity.ContextFactory;
import org.sagebionetworks.javadoc.web.services.FilterUtils;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.RootDoc;

/**
 * Generates controller context data.
 * 
 * @author John
 *
 */
public class ControllerContextGenerator implements ClassContextGenerator {

	@Override
	public List<ClassContext> generateContext(ContextFactory factory, RootDoc root) throws Exception {
		// Iterate over all of the controllers.
		String authControllerName = getAuthControllerName(root.options());
        Iterator<ClassDoc> contollers = FilterUtils.controllerIterator(root.classes());
        List<ClassContext> results = new LinkedList<ClassContext>();
        Controllers controllers = new Controllers();
        controllers.setControllers(new LinkedList<ControllerModel>());
        while(contollers.hasNext()){
        	ClassDoc classDoc = contollers.next();

        	// Translate to the model
        	ControllerModel model = ControllerUtils.translateToModel(classDoc);
        	controllers.getControllers().add(model);
        	// Create a context for each method.
        	if(model.getMethods() != null){
        		for(MethodModel method: model.getMethods()){
                	Context velocityContext = factory.createNewContext();
                	// Add this to the controller's model
                	velocityContext.put("method", method);
                	velocityContext.put("controllerPath", model.getPath());
                	velocityContext.put("authControllerName", "${"+authControllerName+"}");
                	ClassContext classContext = new ClassContext(method.getFullMethodName(), "methodHtmlTemplate.html", velocityContext);
                	results.add(classContext);
        		}
        	}
        } 
        // Create the context for all controllers.
        Context velocityContext = factory.createNewContext();
        // Sort the controllers by DisplayName
        Collections.sort(controllers.getControllers(), new Comparator<ControllerModel>() {
			@Override
			public int compare(ControllerModel o1, ControllerModel o2) {
				return o1.getDisplayName().compareTo(o2.getDisplayName());
			}
		});
        velocityContext.put("controllers", controllers);
    	ClassContext classContext = new ClassContext("index", "controllersHtmlTemplate.html", velocityContext);
    	results.add(classContext);
        return results;
	}
	
	/**
	 * Extract the name of the controller from the options.
	 * @param options
	 * @return
	 */
	public static String getAuthControllerName(String[][] options){
		if(options == null) return null;
		for(int key=0; key <options.length; key++){
			if("-authControllerName".equals(options[key][0])){
				return options[key][1];
			}
		}
		throw new IllegalArgumentException("Cannot find the -authControllerName option");
	}

}
