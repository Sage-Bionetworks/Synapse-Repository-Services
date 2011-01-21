/**
 *
 */
package org.sagebionetworks.repo.web;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.sagebionetworks.repo.model.Base;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.annotation.ModelAndViewResolver;

/**
 * This interceptor adds HTTP response headers as appropriate. 
 * Specifically it adds:<ol>
 * <li> an ETag header to any response returning a model object as the body of the response
 * <li> a Location header to any response to a POST returning a model object as the body of the response
 * </ol>
 * <p>
 * Dev Note: Although the class below implements a view resolver interface, it is really more like a postHandle
 * interceptor.  I tried to instead implement org.springframework.web.servlet.HandlerInterceptor but it only receives
 * the ModelAndView, not the result object for the response body as facilitated by Spring 3.0's REST support.
 *
 * @author deflaux
 *
 */
public class ResponseHeaderResolver implements ModelAndViewResolver {

    private static final Logger log = Logger.getLogger(ResponseHeaderResolver.class.getName());

    @SuppressWarnings("unchecked")
    @Override
    public ModelAndView resolveModelAndView(Method handlerMethod,
            Class handlerType, Object returnValue,
            ExtendedModelMap implicitModel, NativeWebRequest webRequest) {

        if(null != returnValue) {
            HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();
            HttpServletResponse response = (HttpServletResponse) webRequest.getNativeResponse();

            /*
             * Add the ETag header any time we return a resource
             */
            if(returnValue instanceof Base) { // DAO backed entities
                Base entity = (Base) returnValue;
                response.setHeader(ServiceConstants.ETAG_HEADER, entity.getEtag());
            }
            else { // Any other sort of entity
                Integer etag = returnValue.hashCode();
                log.fine("adding Etag: " + etag);
                response.setIntHeader(ServiceConstants.ETAG_HEADER, etag);
            }

            /*
             * Add the Location header any time we create a resource
             *
             * It would be better is see if our response status code is 201 Created but
             * HttpServletResponse does not expose that field
             *
             * Dev Note: if we have POST /repo/v1/message/123/annotation something like
             * request.getServletPath() + UrlPrefixes.MODEL2URL.get(returnValue.getClass())
             * instead of request.getRequestURI() will not work
             */
            if(request.getMethod().equals("POST")) {
                if(returnValue instanceof Base) {
                    Base entity = (Base) returnValue;
                    response.setHeader(ServiceConstants.LOCATION_HEADER, entity.getUri());
                }
                else {
                    Method getId = ReflectionUtils.findMethod(returnValue.getClass(), "getId");
                    response.setHeader(ServiceConstants.LOCATION_HEADER,
                            request.getRequestURI()
                            + "/"
                            + ReflectionUtils.invokeMethod(getId, returnValue));
                }
            }
        }
        return UNRESOLVED; // Tell Spring to keep doing its thing (such as serializing returnValue to the appropriate encoding)
    }
}
