package org.sagebionetworks.repo.manager.grid.util;

import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.patch.ConValue;
import org.sagebionetworks.util.ValidateArgument;

public class GridJsonUtils {

    /**
     * Transforms a list of ordered column names and a list of CRDT ConstantNode values into a JSON object
     */
    public static JSONObject gridRowToJsonObject(List<String> orderedColumnNames, List<ConstantNode> rowDataConstantNodes) {
        ValidateArgument.required(orderedColumnNames, "orderedColumnNames");
        ValidateArgument.required(rowDataConstantNodes, "rowDataConstantNodes");

        if (rowDataConstantNodes.isEmpty()) {
            return new JSONObject();
        }

        JSONObject json = new JSONObject();
        for (int i = 0; i < orderedColumnNames.size() && i < rowDataConstantNodes.size(); i++) {
            String col = orderedColumnNames.get(i);
            if (rowDataConstantNodes.get(i) == null || rowDataConstantNodes.get(i).getConValue() == null || rowDataConstantNodes.get(i).getConValue().isUndefined()) {
                // The JSON Joy CRDT spec allows 'undefined' values; omit these from the JSON object
                continue;
            }
            json.put(col, rowDataConstantNodes.get(i).getConValue().getValue());
        }
        return json;
    }

    /**
     * Transforms a list of ordered column names and a list of raw JSON values into a JSON object
     */
    public static JSONObject gridRowToJsonObject(List<String> orderedColumnNames, JSONArray values) {
        ValidateArgument.required(orderedColumnNames, "orderedColumnNames");
        ValidateArgument.required(values, "values");

        JSONObject json = new JSONObject();
        for (int i = 0; i < orderedColumnNames.size() && i < values.length(); i++) {
            json.put(orderedColumnNames.get(i), values.get(i));
        }
        return json;
    }

    /**
     * Transforms a list of ordered column names and a map of (columnName, ConValue) pairs into a JSON object
     */
    public static JSONObject gridRowToJsonObject(List<String> orderedColumnNames, Map<String, ConValue> cells) {
        ValidateArgument.required(orderedColumnNames, "orderedColumnNames");
        ValidateArgument.required(cells, "cells");
        JSONObject json = new JSONObject();
        for (String column : orderedColumnNames) {
            ConValue value = cells.get(column);
            if (value == null || value.isUndefined()) {
                continue;
            }
            json.put(column, value.getValue());
        }
        return json;
    }
}
