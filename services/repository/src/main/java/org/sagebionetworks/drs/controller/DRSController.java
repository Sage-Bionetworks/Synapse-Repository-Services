package org.sagebionetworks.drs.controller;

import org.sagebionetworks.repo.model.drs.ServiceInformation;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.web.RequiredScope;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;


/**
 * Services for DRS.
 */
@ControllerInfo(displayName = "DRS Services", path = "ga4gh/drs/v1")
@Controller
@RequestMapping(UrlHelpers.DRS_PATH)
public class DRSController {

    @Autowired
    ServiceProvider serviceProvider;


    /**
     * Get the drs service information.
     *
     * @return the drs service information
     */
    @RequiredScope({OAuthScope.view})
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = {UrlHelpers.DRS_SERVICE_INFO}, method = RequestMethod.GET)
    public @ResponseBody
    ServiceInformation getDRSServiceInfo() {
        return serviceProvider.getDRSService().getServiceInformation();
    }
}
