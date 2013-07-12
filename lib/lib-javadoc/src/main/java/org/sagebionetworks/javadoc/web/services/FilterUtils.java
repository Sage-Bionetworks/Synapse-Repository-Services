package org.sagebionetworks.javadoc.web.services;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;

/**
 * Utilities for filtering elements by annotations.
 * @author John
 *
 */
public class FilterUtils {
	/**
	 * Create an iterator that only includes @Controller
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
                    if(ControllerInfo.class.getName().equals(ad.annotationType().qualifiedName())){
                    	list.add(classDoc);
                    }
            	}
            }
		}
		return list.iterator();
	}
	
	/**
	 * Create an iterator that only includes @RequestMapping methods.
	 * @param classes
	 * @return
	 */
	public static Iterator<MethodDoc> requestMappingIterator(MethodDoc[] methods){
		if(methods == null) throw new IllegalArgumentException("classes cannot be null");
		List<MethodDoc> list = new LinkedList<MethodDoc>();
		for(MethodDoc methodDoc: methods){
            AnnotationDesc[] annos = methodDoc.annotations();
            if(annos != null){
            	for(AnnotationDesc ad: annos){
            		if(RequestMapping.class.getName().equals(ad.annotationType().qualifiedName())){
                		list.add(methodDoc);
            		}
            	}
            }
		}
		return list.iterator();
	}
}
