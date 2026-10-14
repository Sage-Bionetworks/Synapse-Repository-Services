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
     * Transforms a list of ordered column names and a row's CRDT nodes into a JSON object.
     *
     * @param orderedColumnNames the column names, in order.
     * @param nodesByIndex       the row's nodes, indexed by their position in {@code orderedColumnNames}.
     *                           A column with no node for the row is {@code null} at that index.
     * @return a JSON object holding one key per column that has a value. A column absent from
     *         {@code nodesByIndex} (either past the array's length or {@code null}) has no value, and
     *         the JSON Joy CRDT spec also allows 'undefined' values; both are omitted.
     */
    public static JSONObject gridRowToJsonObject(List<String> orderedColumnNames, ConstantNode[] nodesByIndex) {
        ValidateArgument.required(orderedColumnNames, "orderedColumnNames");
        ValidateArgument.required(nodesByIndex, "nodesByIndex");

        JSONObject json = new JSONObject();
        for (int i = 0; i < orderedColumnNames.size() && i < nodesByIndex.length; i++) {
            ConstantNode node = nodesByIndex[i];
            if (node == null || node.getConValue() == null || node.getConValue().isUndefined()) {
                continue;
            }
            json.put(orderedColumnNames.get(i), node.getConValue().getValue());
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
    public static JSONObject gridCellsToJsonObject(List<String> orderedColumnNames, Map<String, ConValue> cells) {
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
