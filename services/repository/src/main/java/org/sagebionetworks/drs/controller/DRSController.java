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
 * <p>
 * The <a href="https://ga4gh.github.io/data-repository-service-schemas/preview/release/drs-1.2.0/docs/">
 * Data Repository Service</a> API provides access to data objects in single, standard way regardless of
 * where they are stored and how they are managed.Data Objects for which information can be fetched is
 * <a href="${org.sagebionetworks.repo.model.FileEntity}">FileEntity</a> and
 * <a href="${org.sagebionetworks.repo.model.table.Dataset}">Dataset</a>.The data object that be downloaded is file.
 * </p>
 * <p>
 * The supported end point for drs are:
 *     <ul>
 *         <li><a href="${GET.service-info}"> GET /service-info</a></li>
 *     </ul>
 * </p>
 * <p>
 *     Use <a href="${GET.service-info}> GET /service-info</a> API to get information about GA4GH-compliant web services,
 *     including DRS services, to be aggregated into registries and made available via a standard API
 * </p>
 */
@ControllerInfo(displayName = "DRS Services", path = "ga4gh/drs/v1")
@Controller
@RequestMapping(UrlHelpers.DRS_PATH)
public class DRSController {

    @Autowired
    ServiceProvider serviceProvider;


    /**
     * Get the drs service information.See the drs specification:
     *<a href="https://ga4gh.github.io/data-repository-service-schemas/preview/release/drs-1.2.0/docs/#tag/GA4GH-Service-Registry">
     *     GA4GH Service Registry</a>
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
