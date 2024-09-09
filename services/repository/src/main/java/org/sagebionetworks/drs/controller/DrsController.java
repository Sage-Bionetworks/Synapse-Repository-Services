package org.sagebionetworks.drs.controller;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.drs.AccessUrl;
import org.sagebionetworks.repo.model.drs.DrsObject;
import org.sagebionetworks.repo.model.drs.ServiceInformation;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.service.ServiceProvider;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.RequiredScope;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;


/**
 * <p>
 * The <a href="https://ga4gh.github.io/data-repository-service-schemas/preview/release/drs-1.2.0/docs/">
 * Data Repository Service</a> API provides access to data objects in single, standard way regardless of
 * where they are stored and how they are managed. The data objects for which information can be fetched are 
 * <a href="${org.sagebionetworks.repo.model.FileEntity}">FileEntity</a> and
 * <a href="${org.sagebionetworks.repo.model.table.Dataset}">Dataset</a>.The data object that is downloaded is a file.
 * </p>
 * <p>
 * The supported end points for DRS are:
 *     <ul>
 *         <li><a href="${GET.service-info}"> GET /service-info</a></li>
 *         <li><a href="${GET.objects.object_id}"> GET /objects/{object_id}</a></li>
 *     </ul>
 * </p>
 * <p>
 *     Use the <a href="${GET.service-info}"> GET /service-info </a> API to get information about GA4GH-compliant web services,
 *     including DRS services, to be aggregated into registries and made available via a standard API.
 *     </p>
 *     Use the <a href="${GET.objects.object_id}"> GET /objects/{object_id} </a> API to get information about individual DRS objects.
 *     </p>
 */
@ControllerInfo(displayName = "Drs Services", path = "ga4gh/drs/v1")
@Controller
@RequestMapping(UrlHelpers.DRS_PATH)
public class DrsController {

    @Autowired
    ServiceProvider serviceProvider;


    /**
     * The service information API will provide information regarding supported GA4GH services. Checkout the drs specification for 
     * <a href="https://ga4gh.github.io/data-repository-service-schemas/preview/release/drs-1.2.0/docs/#tag/GA4GH-Service-Registry">
     * GA4GH Service Registry</a>
     *
     * @return the drs service information
     */
    @RequiredScope({OAuthScope.view})
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = {UrlHelpers.DRS_SERVICE_INFO}, method = RequestMethod.GET)
    public @ResponseBody
    ServiceInformation getDrsServiceInfo() {
        return serviceProvider.getDrsService().getServiceInformation();
    }

    /**
     * The DRSObject API will provide information about a DrsObject, which can be a
     * <a href="${org.sagebionetworks.repo.model.FileEntity}">FileEntity</a> or
     * <a href="${org.sagebionetworks.repo.model.table.Dataset}">Dataset</a>.
     * The DrsObject is fetched by its drsId i.e., its Synapse ID, plus its version, which makes it immutable (e.g.,  syn123.1)
     * , or its file handle ID prepended with the string “fh” (e.g., fh123)). 
     * <a href="https://ga4gh.github.io/data-repository-service-schemas/preview/release/drs-1.2.0/docs/#operation/GetObject">
     * Get info about a DrsObject.</a>
     *
     * @return the DRS object
     */
    @RequiredScope({OAuthScope.view})
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = {UrlHelpers.DRS_OBJECT}, method = RequestMethod.GET)
    public @ResponseBody DrsObject getDrsObject(@PathVariable String object_id,
                                                @RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
                                                @RequestParam(value = "expand", defaultValue = "false") Boolean expand)
            throws NotFoundException, DatastoreException, UnauthorizedException, IllegalArgumentException, UnsupportedOperationException {
        return serviceProvider.getDrsService().getDrsObject(userId, object_id, expand);
    }

    /**
     * This API will provide the url from which a
     * <a href="${org.sagebionetworks.repo.model.FileEntity}">FileEntity</a>
     * may be downloaded.  If the file is stored in AWS S3 or a GCP bucket then 
     * the result is a time limited, presigned URL which can be used without any further authentication.
     * If the file is stored in some other web location (a so-called 'external file'), this service simply returns that file's URL.
     * <p>
     * For more information, see the GA4GH documentation for
     * <a href="https://ga4gh.github.io/data-repository-service-schemas/preview/release/drs-1.2.0/docs/#operation/GetAccessURL"> GetAccessURL </a>
     * To get the accessId parameter, use the <a href="${GET.objects.object_id}"> GET /objects/{object_id} </a> service.
     * <p>
     *
     * @return the presigned url to download a file
     */

    @RequiredScope({OAuthScope.view, OAuthScope.download})
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = {UrlHelpers.DRS_FETCH_BYTES}, method = RequestMethod.GET)
    public @ResponseBody AccessUrl getAccessURL(@PathVariable final String object_id,
                                                @PathVariable final String access_id,
                                                @RequestParam(value = AuthorizationConstants.USER_ID_PARAM) final Long userId)
            throws NotFoundException, DatastoreException, UnauthorizedException, IllegalArgumentException {
        return serviceProvider.getDrsService().getAccessUrl(userId, object_id, access_id);
    }
}
