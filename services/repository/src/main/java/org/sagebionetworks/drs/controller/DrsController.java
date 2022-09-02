package org.sagebionetworks.drs.controller;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.drs.AccessUrl;
import org.sagebionetworks.repo.model.drs.DrsObject;
import org.sagebionetworks.repo.model.drs.ServiceInformation;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.RequiredScope;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
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
 * where they are stored and how they are managed.Data Objects for which information can be fetched is
 * <a href="${org.sagebionetworks.repo.model.FileEntity}">FileEntity</a> and
 * <a href="${org.sagebionetworks.repo.model.table.Dataset}">Dataset</a>.The data object that be downloaded is file.
 * </p>
 * <p>
 * The supported end point for drs are:
 *     <ul>
 *         <li><a href="${GET.service-info}"> GET /service-info</a></li>
 *         <li><a href="${GET.objects.id}"> GET /objects/{id}</a></li>
 *     </ul>
 * </p>
 * <p>
 *     Use <a href="${GET.service-info}"> GET /service-info </a> API to get information about GA4GH-compliant web services,
 *     including drs services, to be aggregated into registries and made available via a standard API.
 *     </p>
 *     Use <a href="${GET.objects.id}"> GET /objects/{id} </a> API to get information about drs object.
 *     </p>
 */
@ControllerInfo(displayName = "Drs Services", path = "ga4gh/drs/v1")
@Controller
@RequestMapping(UrlHelpers.DRS_PATH)
public class DrsController {

    @Autowired
    ServiceProvider serviceProvider;


    /**
     * Get service information API will provide the drs service information.Checkout the drs specification for:
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
     * Get DRSObject API will provide information about the DrsObject which can be
     * <a href="${org.sagebionetworks.repo.model.FileEntity}">FileEntity</a> or
     * <a href="${org.sagebionetworks.repo.model.table.Dataset}">Dataset</a>.
     * DrsObject is fetched by drsId i.e Synapse Id plus version which makes it immutable , example id = syn123.1.
     * <a href="https://ga4gh.github.io/data-repository-service-schemas/preview/release/drs-1.2.0/docs/#operation/GetObject">
     * Get info about a DrsObject.</a>
     *
     * @return the drs object
     */
    @RequiredScope({OAuthScope.view})
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = {UrlHelpers.DRS_OBJECT}, method = RequestMethod.GET)
    public @ResponseBody DrsObject getDrsObject(@PathVariable String id,
                                                @RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
                                                @RequestParam(value = "expand", defaultValue = "false") Boolean expand)
            throws NotFoundException, DatastoreException, UnauthorizedException, IllegalArgumentException, UnsupportedOperationException {
        return serviceProvider.getDrsService().getDrsObject(userId, id, expand);
    }

    /**
     * GET an url for fetching byte API will provide the actual url of
     * <a href="${org.sagebionetworks.repo.model.FileEntity}">FileEntity</a>
     * for example s3 bucket, google cloud etc., from where file can be downloaded.
     * <p>
     * The method only need to be called when using
     * <a href="https://ga4gh.github.io/data-repository-service-schemas/preview/release/drs-1.2.0/docs/#tag/AccessMethodModel"> AccessMethod </a>
     * that contains an access_id.(To get access_id call <a href="${GET.objects.id}"> GET /objects/{id} </a> Api for file)
     * <p>
     * The preassigned url will be sent to the user and file can be downloaded directly from the url without any authentication.
     * As preassigned url has tokens included, which expires with time.
     *
     * @return the preassigned url to download a file
     */

    @RequiredScope({OAuthScope.download})
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = {UrlHelpers.DRS_FETCH_BYTES}, method = RequestMethod.GET)
    public @ResponseBody AccessUrl getAccessURL(@PathVariable final String id,
                                                @PathVariable final String accessId,
                                                @RequestParam(value = AuthorizationConstants.USER_ID_PARAM) final Long userId)
            throws NotFoundException, DatastoreException, UnauthorizedException, IllegalArgumentException {
        return serviceProvider.getDrsService().getAccessUrl(userId, id, accessId);
    }
}
