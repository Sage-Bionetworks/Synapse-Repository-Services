package org.sagebionetworks.repo.manager.agent.parameter;

import org.json.JSONArray;

import java.util.List;

public class ListParser implements ParameterParser<List<Object>> {



    @Override
    public List<Object> parse(String value) {
        return   new JSONArray(value).toList();
    }

}
