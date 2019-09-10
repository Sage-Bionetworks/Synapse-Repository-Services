package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@ControllerInfo(displayName="Entity Bundle Services", path="repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class EntityBundleControllerV2 {
}
