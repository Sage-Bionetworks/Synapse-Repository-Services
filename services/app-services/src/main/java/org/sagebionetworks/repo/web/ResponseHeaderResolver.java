package org.sagebionetworks.repo.web;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.sagebionetworks.profiler.Frame;
import org.sagebionetworks.profiler.ProfileFilter;
import org.sagebionetworks.profiler.ProfileSingleton;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.annotation.ModelAndViewResolver;

/**
 * This interceptor adds HTTP response headers as appropriate. Specifically it
 * adds:
 * <ol>
 * <li>an ETag header to any response returning a model object as the body of
 * the response
 * <li>a Location header to any response to a POST returning a model object as
 * the body of the response
 * </ol>
 * <p>
 * Dev Note: Although the class below implements a view resolver interface, it
 * is really more like a postHandle interceptor. I tried to instead implement
 * org.springframework.web.servlet.HandlerInterceptor but it only receives the
 * ModelAndView, not the result object for the response body as facilitated by
 * Spring 3.0's REST support.
 * 
 * @author deflaux
 * 
 */
public class ResponseHeaderResolver implements ModelAndViewResolver {

	@Override
	public ModelAndView resolveModelAndView(Method handlerMethod,
			Class handlerType, Object returnValue,
			ExtendedModelMap implicitModel, NativeWebRequest webRequest) {

		if (null != returnValue) {
			HttpServletRequest request = (HttpServletRequest) webRequest
					.getNativeRequest();
			HttpServletResponse response = (HttpServletResponse) webRequest
					.getNativeResponse();

			/*
			 * Add the ETag header any time we return a resource
			 */
			if (returnValue instanceof Base) { // DAO backed entities
				Base entity = (Base) returnValue;
				response.setHeader(ServiceConstants.ETAG_HEADER, entity
						.getEtag());
			}
			
			if (returnValue instanceof Entity) { // DAO backed entities
				Entity entity = (Entity) returnValue;
				response.setHeader(ServiceConstants.ETAG_HEADER, entity.getEtag());
			}

			/*
			 * Add the Location header any time we create a resource
			 * 
			 * It would be better is see if our response status code is 201
			 * Created but HttpServletResponse does not expose that field
			 */
			if (request.getMethod().equals("POST")) {
				if (returnValue instanceof Base) {
					Base entity = (Base) returnValue;
					response.setHeader(ServiceConstants.LOCATION_HEADER, entity
							.getUri());
				}
				if (returnValue instanceof Entity) {
					Entity entity = (Entity) returnValue;
					response.setHeader(ServiceConstants.LOCATION_HEADER, entity
							.getUri());
				}
			}
			
			/*
			 * Add the profile data to the header when requested.
			 */
			String value = request.getHeader(ProfileFilter.KEY_PROFILE_REQUEST);
			if(value != null){
				Frame frame = ProfileSingleton.getFrame();
				if (frame != null) {
					try {
						String json = Frame.writeFrameJSON(frame);
						response.setHeader(ProfileFilter.KEY_PROFILE_RESPONSE_OBJECT,base64Encode(json));
					} catch (JSONException e) {
						throw new RuntimeException(e);
					} catch (UnsupportedEncodingException e) {
						throw new RuntimeException(e);
					}
				}
			}
			
		}
		return UNRESOLVED; // Tell Spring to keep doing its thing (such as
		// serializing returnValue to the appropriate
		// encoding)
	}
	
	/**
	 * Helper to base64 encode header values
	 * @param toEncode
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	private static String base64Encode(String toEncode) throws UnsupportedEncodingException{
		if(toEncode == null) return null;
		 return new String(Base64.encodeBase64(toEncode.getBytes("UTF-8")), "UTF-8");
	}
}
