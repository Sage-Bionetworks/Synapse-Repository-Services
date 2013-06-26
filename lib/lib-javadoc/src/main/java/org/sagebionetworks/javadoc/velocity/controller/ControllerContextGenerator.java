package org.sagebionetworks.javadoc.velocity.controller;

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
        Iterator<ClassDoc> contollers = FilterUtils.controllerIterator(root.classes());
        List<ClassContext> results = new LinkedList<ClassContext>();
        while(contollers.hasNext()){
        	ClassDoc classDoc = contollers.next();
        	Context velocityContext = factory.createNewContext();
        	// Translate to the model
        	ControllerModel model = ControllerUtils.translateToModel(classDoc);
        	velocityContext.put("model", model);
        	ClassContext classContext = new ClassContext(classDoc.qualifiedName(), "controllerHtmlTemplate.html", velocityContext);
        	results.add(classContext);
        }       
        return results;
	}

}
