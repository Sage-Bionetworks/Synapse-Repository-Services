package profiler.org.sagebionetworks.usagemetrics;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * This aspect is for holding any system-level concepts about how we define a tier of the
 * service that we want to do some AoP on.
 * 
 * @author geoff
 *
 */
@Aspect
public class SystemArchitecture {

    /**
     * A join point must be in the org.sagebionetworks.repo.web.controller  
     * package to be a web service.
     */
    @Pointcut("within(org.sagebionetworks.repo.web.controller.*)")
    public void inWebControllerPkg() {};

    /**
     * A join point must be in a class with the annotation "Controller"
     * to be a web service.
     */
    @Pointcut("@within(org.springframework.stereotype.Controller)")
    public void inWebController() {};

    /**
     * A join point must have the annotation "ResponseStatus" to be a web service.
     */
    @Pointcut("@annotation(org.springframework.web.bind.annotation.ResponseStatus)")
    public void isResponseStatus() {};

    /**
     * A join point must be a ResponseStatus method that is contained in a WebController
     * class, in the WebController package.
     */
    @Pointcut("inWebControllerPkg() && inWebController() && isResponseStatus()")
    public void isWebService() {};
}
