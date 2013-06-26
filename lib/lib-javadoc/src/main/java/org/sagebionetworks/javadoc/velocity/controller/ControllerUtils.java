package org.sagebionetworks.javadoc.velocity.controller;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.sagebionetworks.javadoc.web.services.FilterUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sun.javadoc.Type;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.AnnotationDesc.ElementValuePair;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;

public class ControllerUtils {

	public static String REQUEST_MAPPING_VALUE = RequestMapping.class.getName()+".value";
	/**
	 * Translate from a a controller class to a Controller model.
	 * 
	 * @param classDoc
	 * @return
	 */
	public static ControllerModel translateToModel(ClassDoc classDoc){
		ControllerModel model = new ControllerModel();
		// Setup the basic data
		model.setName(classDoc.name());
		model.setClassDescription(classDoc.getRawCommentText());
    	Iterator<MethodDoc> methodIt = FilterUtils.requestMappingIterator(classDoc.methods());
    	List<MethodModel> methods = new LinkedList<MethodModel>();
    	model.setMethods(methods);
    	while(methodIt.hasNext()){
    		MethodDoc methodDoc = methodIt.next();
    		MethodModel methodModel = new MethodModel();
            AnnotationDesc[] annos = methodDoc.annotations();
            if(annos != null){
            	for(AnnotationDesc ad: annos){
            		String qualifiedName = ad.annotationType().qualifiedName();
            		if(RequestMapping.class.getName().equals(qualifiedName)){
            			for(ElementValuePair pair: ad.elementValues()){
            				if(REQUEST_MAPPING_VALUE.equals(pair.element().qualifiedName())){
                        		System.out.println(pair.element().qualifiedName());
                        		methodModel.setUrl(pair.value().toString());
            				}
            			}
            		}else if(ResponseBody.class.getName().equals(qualifiedName)){
            			// this means there is a response body for this method.
            			Type returnType = methodDoc.returnType();
            			Link reponseLink = new Link();
            			StringBuilder builder = new StringBuilder();
            			builder.append("${").append(returnType.qualifiedTypeName()).append("}");
            			reponseLink.setHref(builder.toString());
            			reponseLink.setDisplay(returnType.simpleTypeName());
            			methodModel.setResponseBody(reponseLink);
            		}
            	}
            }
    		

    		methodModel.setDescription(methodDoc.commentText());
    		methods.add(methodModel);
    		System.out.println(methodModel);
    	}
		return model;
	}
}
