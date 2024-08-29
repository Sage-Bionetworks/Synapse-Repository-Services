package org.sagebionetworks.javadoc.velocity.controller;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.lang.model.element.TypeElement;

import org.apache.velocity.context.Context;
import org.sagebionetworks.javadoc.velocity.ClassContext;
import org.sagebionetworks.javadoc.velocity.ClassContextGenerator;
import org.sagebionetworks.javadoc.velocity.ContextInput;
import org.sagebionetworks.javadoc.web.services.FilterUtils;

/**
 * Generates controller context data.
 * 
 * @author John
 *
 */
public class ControllerContextGenerator implements ClassContextGenerator {

	@Override
	public List<ClassContext> generateContext(ContextInput input) throws Exception {
		// Iterate over all of the controllers.
		String authControllerName = input.getAuthControllerName().orElseThrow(()-> new IllegalArgumentException("-authControllerName is required"));
        Iterator<TypeElement> contollers = FilterUtils.controllerIterator(input.getDocletEnvironment());
        List<ClassContext> results = new LinkedList<ClassContext>();
        Controllers controllers = new Controllers();
        controllers.setControllers(new LinkedList<ControllerModel>());
        while(contollers.hasNext()){
        	TypeElement typeElement = contollers.next();

        	// Translate to the model
        	ControllerModel model = ControllerUtils.translateToModel(input.getDocletEnvironment(), typeElement);
        	controllers.getControllers().add(model);
        	// Create a context for each method.
        	if(model.getMethods() != null){
        		for(MethodModel method: model.getMethods()){
                	Context velocityContext = input.getContextFactory().createNewContext();
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
        Context velocityContext = input.getContextFactory().createNewContext();
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

}
