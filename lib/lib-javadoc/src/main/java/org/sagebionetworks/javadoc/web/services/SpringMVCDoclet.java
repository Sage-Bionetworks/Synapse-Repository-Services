package org.sagebionetworks.javadoc.web.services;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.RootDoc;
/**
 * Java Doclet for generating javadocs for Spring MVC web-services.
 * 
 * @author jmhill
 *
 */
public class SpringMVCDoclet {

	/**
	 * Identifies a Spring controller.
	 */
	public static final String ORG_SPRINGFRAMEWORK_STEREOTYPE_CONTROLLER = "@org.springframework.stereotype.Controller";

	public static boolean start(RootDoc root) {
		// Pass this along to the standard doclet
        ClassDoc[] classes = root.classes();
        Iterator<ClassDoc> contollers = controllerIterator(classes);
        while(contollers.hasNext()){
        	ClassDoc classDoc = contollers.next();
        	System.out.println(classDoc);
        }

		return true;
	}
	
	/**
	 * Create an iterator that only includes contollers
	 * @param classes
	 * @return
	 */
	public static Iterator<ClassDoc> controllerIterator(ClassDoc[] classes){
		if(classes == null) throw new IllegalArgumentException("classes cannot be null");
		List<ClassDoc> list = new LinkedList<ClassDoc>();
		for(ClassDoc classDoc: classes){
            AnnotationDesc[] annos = classDoc.annotations();
            if(annos != null){
            	for(AnnotationDesc ad: annos){
                    if(ORG_SPRINGFRAMEWORK_STEREOTYPE_CONTROLLER.equals(ad.toString())){
                    	// This is a controller
                    	list.add(classDoc);
                    }
            	}
            }
		}
		return list.iterator();
	}
}
