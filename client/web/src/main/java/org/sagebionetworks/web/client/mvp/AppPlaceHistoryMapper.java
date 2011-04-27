package org.sagebionetworks.web.client.mvp;

import org.sagebionetworks.web.client.place.DatasetsHomePlace;
import org.sagebionetworks.web.client.place.DatasetPlace;
import org.sagebionetworks.web.client.place.HomePlace;
import org.sagebionetworks.web.client.place.LayerPlace;
import org.sagebionetworks.web.client.place.LoginPlace;

import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;

/**
 * PlaceHistoryMapper interface is used to attach all places which the
 * PlaceHistoryHandler should be aware of. This is done via the @WithTokenizers
 * annotation or by extending PlaceHistoryMapperWithFactory and creating a
 * separate TokenizerFactory.
 */
@WithTokenizers( { HomePlace.Tokenizer.class, DatasetsHomePlace.Tokenizer.class, DatasetPlace.Tokenizer.class, LayerPlace.Tokenizer.class, LoginPlace.Tokenizer.class})
public interface AppPlaceHistoryMapper extends PlaceHistoryMapper {
}
