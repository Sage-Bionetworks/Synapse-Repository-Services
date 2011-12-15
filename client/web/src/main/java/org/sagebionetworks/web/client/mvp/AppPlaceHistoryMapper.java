package org.sagebionetworks.web.client.mvp;

import org.sagebionetworks.web.client.place.AnalysesHome;
import org.sagebionetworks.web.client.place.Analysis;
import org.sagebionetworks.web.client.place.ComingSoon;
import org.sagebionetworks.web.client.place.Dataset;
import org.sagebionetworks.web.client.place.DatasetsHome;
import org.sagebionetworks.web.client.place.Home;
import org.sagebionetworks.web.client.place.Layer;
import org.sagebionetworks.web.client.place.LoginPlace;
import org.sagebionetworks.web.client.place.Lookup;
import org.sagebionetworks.web.client.place.PhenoEdit;
import org.sagebionetworks.web.client.place.Profile;
import org.sagebionetworks.web.client.place.Project;
import org.sagebionetworks.web.client.place.ProjectsHome;
import org.sagebionetworks.web.client.place.Step;
import org.sagebionetworks.web.client.place.StepsHome;
import org.sagebionetworks.web.client.place.Synapse;
import org.sagebionetworks.web.client.place.users.PasswordReset;
import org.sagebionetworks.web.client.place.PublicProfile;
import org.sagebionetworks.web.client.place.users.RegisterAccount;

import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;

/**
 * PlaceHistoryMapper interface is used to attach all places which the
 * PlaceHistoryHandler should be aware of. This is done via the @WithTokenizers
 * annotation or by extending PlaceHistoryMapperWithFactory and creating a
 * separate TokenizerFactory.
 */
@WithTokenizers({ Home.Tokenizer.class, DatasetsHome.Tokenizer.class,
		Dataset.Tokenizer.class, Layer.Tokenizer.class,
		LoginPlace.Tokenizer.class, PasswordReset.Tokenizer.class,
		RegisterAccount.Tokenizer.class, ProjectsHome.Tokenizer.class,
		Project.Tokenizer.class, Profile.Tokenizer.class,
		AnalysesHome.Tokenizer.class, StepsHome.Tokenizer.class,
		Analysis.Tokenizer.class, Step.Tokenizer.class,
		ComingSoon.Tokenizer.class, PhenoEdit.Tokenizer.class, Lookup.Tokenizer.class, Synapse.Tokenizer.class })
public interface AppPlaceHistoryMapper extends PlaceHistoryMapper {
}
