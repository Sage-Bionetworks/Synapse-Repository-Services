package org.sagebionetworks.javadoc.velocity.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.sagebionetworks.javadoc.velocity.schema.SchemaUtils;
import org.sagebionetworks.javadoc.web.services.FilterUtils;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.web.RequiredScope;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Lists;
import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.AnnotationDesc.ElementValuePair;
import com.sun.javadoc.AnnotationValue;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.Type;

public class ControllerUtils {
	
	public static String REQUEST_MAPPING_VALUE = RequestMapping.class.getName()+".value";
	public static String REQUEST_MAPPING_METHOD = RequestMapping.class.getName()+".method";
	public static String REQUEST_PARAMETER_VALUE = RequestParam.class.getName()+".value";
	public static String REQUEST_HEADER_VALUE = RequestHeader.class.getName()+".value";
	public static String REQUEST_PARAMETER_REQUIRED = RequestParam.class.getName()+".required";
	
	public static String CONTROLLER_INFO_DISPLAY_NAME = ControllerInfo.class.getName()+".displayName";
	public static String CONTROLLER_INFO_PATH = ControllerInfo.class.getName()+".path";
	
	public static int MAX_SHORT_DESCRIPTION_LENGTH = 150;
	public static String ELLIPSES = "&#8230";
	
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
		model.setFullClassName(classDoc.qualifiedName());
		// Map the annotations of the class
		Map<String, Object> annotationMap = mapAnnotation(classDoc.annotations());
		// Get the display name and path if they exist
		model.setDisplayName((String) annotationMap.get(CONTROLLER_INFO_DISPLAY_NAME));
		model.setPath((String)annotationMap.get(CONTROLLER_INFO_PATH));
    	Iterator<MethodDoc> methodIt = FilterUtils.requestMappingIterator(classDoc.methods());
    	List<MethodModel> methods = new LinkedList<MethodModel>();
    	model.setMethods(methods);
    	while(methodIt.hasNext()){
    		MethodDoc methodDoc = methodIt.next();
    		MethodModel methodModel = translateMethod(methodDoc);
    		methods.add(methodModel);
    	}
		return model;
	}

	public static MethodModel translateMethod(MethodDoc methodDoc) {
		MethodModel methodModel = new MethodModel();
		// Process the method annotations.
		processMethodAnnotations(methodDoc, methodModel);
		// Now process the parameters
		processParameterAnnotations(methodDoc, methodModel);
		methodModel.setDescription(methodDoc.commentText());
		String truncated = createTruncatedText(MAX_SHORT_DESCRIPTION_LENGTH, methodModel.getDescription());
		methodModel.setShortDescription(truncated);
		// remove regular expressions
		String urlDisplay = methodModel.getUrl().replaceAll("\\:[^\\}]+", "").replace("*", "");
		methodModel.setUrl(urlDisplay);
		String fullNameSuffix = urlDisplay.replaceAll("[\\{\\}]", "").replaceAll("/", ".");
		String fullName = methodModel.getHttpType() + fullNameSuffix;
		methodModel.setFullMethodName(fullName);
		Link methodLink = new Link("${" + fullName + "}", methodModel.getHttpType() + " " + urlDisplay);
		methodModel.setMethodLink(methodLink);
		return methodModel;
	}

	/**
	 * Create a text string that is shorter than the max number of characters.
	 * @param maxChars
	 * @param text
	 * @return
	 */
	public static String createTruncatedText(int maxChars, String text){
		if(text == null) return null;
		// Strip out all HTML text from the description.
		// If we do not remove the HTML, we could truncate in the middle of a tag
		// which would break the final HTML.
		text = Jsoup.parse(text).text();
		if(text.length() < maxChars) return text;
		// We need to ensure that we do not cut on any HTML tags
		StringBuilder builder = new StringBuilder();
		for(int i=0; i<maxChars; i++){
			char ch = text.charAt(i);
			builder.append(ch);
		}
		builder.append(ELLIPSES);
		return builder.toString();
	}
	
	private static void processParameterAnnotations(MethodDoc methodDoc, MethodModel methodModel) {
		Parameter[] params = methodDoc.parameters();
		// Start with false here.  If we find the userId parameter this will be changed to true.
		methodModel.setIsAuthenticationRequired(false);
		Map<String, ParameterModel> paramMap = new HashMap<String, ParameterModel>();
        if(params != null){
        	for(Parameter param: params){
        		AnnotationDesc[] paramAnnos = param.annotations();
        		if(paramAnnos != null){
        			for(AnnotationDesc ad: paramAnnos){
        				String qualifiedName = ad.annotationType().qualifiedName();
        				Map<String, Object> annotationMap = mapAnnotation(ad);
        				System.out.println(annotationMap);
        				if(RequestBody.class.getName().equals(qualifiedName)){
        					// Request body
        					String schema = SchemaUtils.getEffectiveSchema(param.type().qualifiedTypeName());
        					if(schema != null){
								Type paramType = param.type();
								if (paramType.asParameterizedType() != null) {
									Link paramLink = new Link("${" + paramType.qualifiedTypeName() + "}", paramType.simpleTypeName());
									methodModel.setRequestBody(paramLink);

									List<Link> genericParameters = Lists.newArrayList();
									for (Type type : paramType.asParameterizedType().typeArguments()) {
										Link link = new Link();
										link.setHref("${" + type.qualifiedTypeName() + "}");
										link.setDisplay(type.simpleTypeName());
										genericParameters.add(link);
									}
									methodModel.setRequestBodyGenericParams(genericParameters.toArray(new Link[] {}));
								} else {
									Link paramLink = new Link("${" + param.type().qualifiedTypeName() + "}", param.typeName());
									methodModel.setRequestBody(paramLink);
								}
							}
        				}else if(PathVariable.class.getName().equals(qualifiedName)){
        					// Path parameter
        					ParameterModel paramModel = new ParameterModel();
        					paramModel.setName(param.name());
        					methodModel.addPathVariable(paramModel);
        					paramMap.put(param.name(), paramModel);
        				}else if(RequestParam.class.getName().equals(qualifiedName)){
        					// if this is the userId parameter then we do not show it,
        					// rather it means this method requires authentication.
        					if(AuthorizationConstants.USER_ID_PARAM.equals(annotationMap.get(REQUEST_PARAMETER_VALUE))){
        						methodModel.setIsAuthenticationRequired(true);
        					}else{
            					ParameterModel paramModel = new ParameterModel();
            					// If the annotation has a value then it should be used as the name.
            					String requestParameterValue = (String)annotationMap.get(REQUEST_PARAMETER_VALUE);
            					if(requestParameterValue != null){
            						paramModel.setName(requestParameterValue);
            					}else{
            						paramModel.setName(param.name());
            					}
            					paramModel.setIsOptional(!(isRequired(annotationMap)));
            					methodModel.addParameter(paramModel);
            					paramMap.put(param.name(), paramModel);
        					}
        				} else if (RequestHeader.class.getName().equals(qualifiedName)) {
        					// if this is the authorization header we do not show it,
        					// rather it means that this method requires authentication
        					if (AuthorizationConstants.SYNAPSE_AUTHORIZATION_HEADER_NAME.equals(annotationMap.get(REQUEST_HEADER_VALUE))) {
        						methodModel.setIsAuthenticationRequired(true);
        					}
        				}
        			}
        		}
        	}
        }
        ParamTag[] paramTags = methodDoc.paramTags();
        if(paramTags != null){
        	for(ParamTag paramTag: paramTags){
        		ParameterModel paramModel = paramMap.get(paramTag.parameterName());
        		if(paramModel != null){
        			paramModel.setDescription(paramTag.parameterComment());
        		}
        	}
        }
		System.out.println(methodModel);
        // Lookup the parameter descriptions
	}

	private static void processMethodAnnotations(MethodDoc methodDoc,
			MethodModel methodModel) {
		AnnotationDesc[] annos = methodDoc.annotations();
        if(annos != null){
        	for (AnnotationDesc ad: annos) {
        		String qualifiedName = ad.annotationType().qualifiedName();
        		System.out.println(qualifiedName);
        	if (RequestMapping.class.getName().equals(qualifiedName)){
        			extractRequestMapping(methodModel, ad);
        		} else if (ResponseBody.class.getName().equals(qualifiedName)) {
					extractResponseLink(methodDoc, methodModel);
        		} else if (RequiredScope.class.getName().equals(qualifiedName)) {
        			extractRequiredScope(methodModel, ad);
        		}
        	}
        }
	}
	
	private static void extractRequiredScope(MethodModel methodModel, AnnotationDesc ad) {
		List<String> requiredScopes = new ArrayList<String>();
		for (ElementValuePair evp : ad.elementValues()) {
			for (AnnotationValue av : (AnnotationValue[])evp.value().value()) {
				FieldDoc fieldDoc = (FieldDoc)av.value();
				String name = fieldDoc.name();
				requiredScopes.add(name);
			}
		}
		if (requiredScopes.isEmpty()) {
			methodModel.setRequiredScopes(null);
		} else {
			methodModel.setRequiredScopes(requiredScopes.toArray(new String[] {}));
		}
	}	

	private static void extractResponseLink(MethodDoc methodDoc, MethodModel methodModel) {
		// this means there is a response body for this method.
		Type returnType = methodDoc.returnType();
		String schema = SchemaUtils.getEffectiveSchema(returnType.qualifiedTypeName());
		if (schema == null) {
			return;
		}

		Link responseLink = new Link();
		responseLink.setHref("${" + returnType.qualifiedTypeName() + "}");
		responseLink.setDisplay(returnType.simpleTypeName());
		methodModel.setResponseBody(responseLink);

		if (returnType.asParameterizedType() != null) {
			List<Link> genericParameters = Lists.newArrayList();
			for (Type type : returnType.asParameterizedType().typeArguments()) {
				Link link = new Link();
				link.setHref("${" + type.qualifiedTypeName() + "}");
				link.setDisplay(type.simpleTypeName());
				genericParameters.add(link);
			}
			methodModel.setResponseBodyGenericParams(genericParameters.toArray(new Link[] {}));
		}
	}

	/**
	 * Extract the request mapping data from the annotation.
	 * @param methodModel
	 * @param ad
	 */
	private static void extractRequestMapping(MethodModel methodModel, AnnotationDesc ad) {
		for(ElementValuePair pair: ad.elementValues()){
			String pairName = pair.element().qualifiedName();
			if(REQUEST_MAPPING_VALUE.equals(pairName)){
				String rawValue = pair.value().toString();
				if(rawValue!= null){
		    		methodModel.setUrl(rawValue.substring(1, rawValue.length()-1));
				}
			}else if(REQUEST_MAPPING_METHOD.equals(pairName)){
				String value = pair.value().toString();
				if(value != null){
					int inxed = RequestMethod.class.getName().length();
					methodModel.setHttpType(value.substring(inxed+1));
				}
			}
		}
	}
	
	/**
	 * Put all annotation value key pairs into a map for easier lookup.
	 * @param annotations
	 * @return
	 */
	public static Map<String, Object> mapAnnotation(AnnotationDesc[] annotations) {
		Map<String, Object> map = new HashMap<String, Object>();
		if(annotations != null){
			for(AnnotationDesc anno: annotations){
				mapAnnotation(anno, map);
			}
		}
		return map;
	}

	/**
	 * Put all annotation value key pairs into a map for easier lookup.
	 * @param ad
	 * @return
	 */
	public static Map<String, Object> mapAnnotation(AnnotationDesc ad){
		 Map<String, Object> map = new HashMap<String, Object>();
		 mapAnnotation(ad, map);
		 return map;
	}
	
	/**
	 * Put all annotation value key pairs into a map for easier lookup.
	 * @param ad
	 * @param map
	 */
	public static void mapAnnotation(AnnotationDesc ad, Map<String, Object> map){
		 ElementValuePair[] pairs = ad.elementValues();
		 if(pairs != null){
			 for(ElementValuePair evp: pairs){
				 String name = evp.element().qualifiedName();
				 Object value = evp.value().value();
				 map.put(name, value);
			 }
		 }
	}
	/**
	 * Check for the required annotation.
	 * @param map
	 * @return
	 */
	public static boolean isRequired(Map<String, Object> map){
		Boolean value = (Boolean) map.get(REQUEST_PARAMETER_REQUIRED);
		if(value != null){
			return value;
		}else{
			return false;
		}
	}
}
