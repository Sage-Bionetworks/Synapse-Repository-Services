
/** Constants **/
var SEARCH = "http://localhost:8080/services-repository-0.10-SNAPSHOT/repo/v1/search?q=";

var SYNAPSE = "http://localhost:8080/portal-0.10-SNAPSHOT/#Synapse:";

var DEFAULT_QUERY = "q=cancer&return-fields=name,id";

var EXAMPLES = [
    {
        desc: "free text search for 'cancer'",
        query: "q=cancer&return-fields=name,id"
    },
    {
        desc: "free text search for 'cancer' showing all facets",
        query: "q=cancer&return-fields=name,id&facet=node_type,disease,species,tissue,platform,num_samples,created_by,modified_by,created_on,modified_on,acl"
    },
    {
        desc: "free text search for 'cancer' showing all facets restricted to datasets only", 
        query: "q=cancer&bq=node_type:'dataset'&return-fields=name,id&facet=node_type,disease,species,tissue,platform,num_samples,created_by,modified_by,created_on,modified_on,acl"
    },
    {
        desc: "free text search for 'cancer' showing all facets restricted to datasets only with at least 1000 samples", 
        query: "q=cancer&bq=(and node_type:'dataset' num_samples:1000..)&return-fields=name,id&facet=node_type,disease,species,tissue,platform,num_samples,created_by,modified_by,created_on,modified_on,acl"
    },
    {
        desc: "layers sorted by creation date", 
        query: "bq=node_type:'layer'&return-fields=name,id&facet=node_type,disease,species,tissue,platform,num_samples,created_by,modified_by,created_on,modified_on,acl&rank=created_on"
    },
    {
        desc: "layers sorted by creation date descending", 
        query: "bq=node_type:'layer'&return-fields=name,id&facet=node_type,disease,species,tissue,platform,num_samples,created_by,modified_by,created_on,modified_on,acl&rank=-created_on"
    },
    {
        desc: "clinical layers (via an annotation)", 
        query: "q=type:C&bq=node_type:'layer'&return-fields=name,id&facet=node_type,disease,species,tissue,platform,num_samples,created_by,modified_by,created_on,modified_on,acl"
    },
    {
        desc: "basically a select * on a literal field",
        query: "bq=node_type:'*'&return-fields=name,id&facet=node_type,disease,species,tissue,platform,num_samples,created_by,modified_by,created_on,modified_on,acl"
    },
    {
        desc: "basically a select * on a numeric field",
        query: "bq=created_on:0..&return-fields=name,id&facet=node_type,disease,species,tissue,platform,num_samples,created_by,modified_by,created_on,modified_on,acl"
    },
    {
        desc: "an OR boolean query",
        query: "bq=(or node_type:'layer' node_type:'dataset')&return-fields=name,id&facet=node_type,disease,species,tissue,platform,num_samples,created_by,modified_by,created_on,modified_on,acl"
    },
    {
        desc: "an AND plus OR boolean query",
        query: "bq=(and (or node_type:'layer' node_type:'dataset') created_by:'nicole.deflaux@sagebase.org')&return-fields=name,id&facet=node_type,disease,species,tissue,platform,num_samples,created_by,modified_by,created_on,modified_on,acl"
    },
    {
        desc: "Matt's datasets",
        query: "bq=(and node_type:'dataset' created_by:'matt.furia@sagebase.org')&return-fields=name,id&facet=node_type,disease,species,tissue,platform,num_samples,created_by,modified_by,created_on,modified_on,acl"
    },
    {
        desc: "Matt's datasets that Nicole is able to see",
        query: "bq=(and (or acl:'PUBLIC' acl:'AUTHENTICATED_USERS' acl:'nicole.deflaux@gmail.com') node_type:'dataset' created_by:'matt.furia@sagebase.org')&return-fields=name,id&facet=node_type,disease,species,tissue,platform,num_samples,created_by,modified_by,created_on,modified_on,acl"
    }
];

/**
 * displaySearchResults
 */
function displaySearchResults(data) {
    try {

        var result = eval('(' + data.result + ')');

        $("#debug_cloudsearch_url").html("<a href='" + data.url + "'>" + data.url + "</a>");
        $("#debug_response").html("<pre>" + JSON.stringify(result, null, 4) + "</pre>");

        $("#results").html("<b>found:</b> " + result.hits.found + "<br>"
                          + "<b>start:</b> " + result.hits.start + "<p />");
        $("#results").append("<ol>")
        $.each(result.hits.hit, function(i,result){
                $("#results").append("<li>"
                                      + result.data.name + ": "
                                      + "<a href=\""
                                      + SYNAPSE + result.id + "\">"
                                      + result.id + "</a></li>");
            });
        $("#results").append("</ol>")
        if(result.facets) {
            var facetList = "";
            facetList += "<ol>"
            $.each(result.facets, function(i, facet) {
                facetList += "<li><b>" + i + "</b>"
                if(facet.constraints) {
                    facetList += "<ol>"
                    $.each(facet.constraints, function(j, item) {
                        facetList += "<li>"
                            + item.value +": "
                            + "<a href=\""
                            + addFacetToCurrentSearchQuery(i, item.value)
                            + "\">"
                            + item.count + "</a></li>";
                    });
                    facetList += "</ol>"
                }
                else if($.isEmptyObject(facet)) {
                    facetList += " none"
                }
                else {
                    var min,max;
                    if(("created_on" === i) || ("modified_on" === i)) {
                        min = new Date(facet.min * 1000)
                        max = new Date(facet.max * 1000)
                    }
                    else {
                        min = facet.min
                        max = facet.max
                    }
                    
                    facetList += "<ol>"
                    facetList += "<li>"
                        + "min: "
                        + "<a href=\""
                        + addFacetToCurrentSearchQuery(i, min)
                        + "\">"
                        + min + "</a></li>";
                    facetList += "<li>"
                        + "max: "
                        + "<a href=\""
                        + addFacetToCurrentSearchQuery(i, max)
                        + "\">"
                        + max + "</a></li>";
                    facetList += "</ol>"
                }
                facetList += "<p />"
            });
            facetList += "</ol>";
            $("#facets").html(facetList)
        }
    }
    catch(exception) {
        alert("Caught: " + exception);
    }
}

function addFacetToCurrentSearchQuery(facetName, facetValue) {
    return window.location.pathname 
        + escapeBooleanQuery(window.location.search 
                             + "&bq="
                             + facetName
                             + ":'"
                             + facetValue
                             + "'")
}

/**
 * escapeBooleanQuery
 */
function escapeBooleanQuery(query) {
    
    // we need to merge and escape the boolean query
    var booleanQuery = "";
    var escapedQuery = "";
    var splits = query.split('&')
    for(var i in splits) {
        if(0 == splits[i].search("bq=")) {
            if(0 < booleanQuery.length) {
                booleanQuery += " "
            }
            var bqValue = unescape(splits[i].substr(3))
            if(0 == bqValue.indexOf("(and ")) {
                bqValue = bqValue.substring(4, bqValue.length-1)
            }
            booleanQuery += bqValue
        }
        else {
            if(0 < escapedQuery.length) {
                escapedQuery += "&"
            }
            escapedQuery += splits[i]
        }
    }

    if(0 != booleanQuery.length) {
        if(-1 < booleanQuery.indexOf(" ")) {
            booleanQuery = "(and " + booleanQuery + ")"
        }

        escapedQuery += "&bq=" + escape(booleanQuery)
    }

    return escapedQuery;
}

/**
 * getSearchResults
 */
function getSearchResults(query) {
    // We need to *double* escape boolean query
    var escapedQuery = escapeBooleanQuery(query)
    
    var url = SEARCH + escape(escapedQuery);
    $("#debug_repo_url").html("<a href='" + url + "'>" + url + "</a>");
    $.get(url, displaySearchResults, "json");
}

/**
 * setSearchQuery
 */
function setSearchQuery(query) {
    window.location.assign(window.location.href.split("?")[0] + "?" + query)
}

/**
 * Stuff to do when the document loads
 */
$(document).ready(function() {

    var query = window.location.search.substring(1)

    if(!query) {
        setSearchQuery(DEFAULT_QUERY)
    }

    query = unescape(query)
    
    $("#query").val(query);
    getSearchResults(query);

    $("#examples").append("<table>")
    $.each(EXAMPLES, function(i, example) {
        var exampleUrl = window.location.href.split("?")[0] + "?" + escapeBooleanQuery(example.query);
        $("#examples").append("<tr><td>" + example.desc + "</td><td><a href='" + exampleUrl + "'>" + example.query + "</a></td></tr><br>");
    });
    $("#examples").append("</table>")

    $("#documentation").html
    (
            "<ul><li>q" +
            "<li>bq" +
            "<li>return-fields" +
            "<li>rank" +
            "<li>facet" +
            "<li>facet-FIELD-constraints" +
            "<li>facet-FIELD-sort" +
            "<li>facet-FIELD-top-n" +
            "<li>t-FIELD" +
            "<li>start" +
            "<li>size" +
            "</ul><p />");
});

