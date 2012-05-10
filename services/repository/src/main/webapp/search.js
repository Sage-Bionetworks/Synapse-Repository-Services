
/** Constants **/
var SEARCH = "http://localhost:8080/services-repository-0.13-SNAPSHOT/repo/v1/searchRaw?q=";

var SYNAPSE = "http://localhost:8080/portal-0.13-SNAPSHOT/#Synapse:";
// Uncomment this if your repo svc is pointing to a prod search index
// var SYNAPSE = "https://synapse.sagebase.org/#Synapse:";

var DEFAULT_QUERY = "q=cancer&return-fields=name,id";

var EXAMPLES = [
    {
        desc: "free text search for 'cancer'",
        query: "q=cancer&return-fields=name,id"
    },
    {
        desc: "free text search for 'cancer' showing all facets",
        query: "q=cancer&return-fields=name,id&facet=node_type,disease,species,tissue,platform,num_samples,created_by,modified_by,created_on,modified_on,acl,reference"
    },
    {
        desc: "free text search for 'cancer' showing all facets restricted to datasets only", 
        query: "q=cancer&bq=node_type:'dataset'&return-fields=name,id&facet=node_type,disease,species,tissue,platform,num_samples,created_by,modified_by,created_on,modified_on,acl,reference"
    },
    {
        desc: "free text search for 'cancer' showing all facets restricted to datasets only with at least 1000 samples", 
        query: "q=cancer&bq=(and node_type:'dataset' num_samples:1000..)&return-fields=name,id&facet=node_type,disease,species,tissue,platform,num_samples,created_by,modified_by,created_on,modified_on,acl,reference"
    },
    {
        desc: "layers sorted by creation date", 
        query: "bq=node_type:'layer'&return-fields=name,id&facet=node_type,disease,species,tissue,platform,num_samples,created_by,modified_by,created_on,modified_on,acl,reference&rank=created_on"
    },
    {
        desc: "layers sorted by creation date descending", 
        query: "bq=node_type:'layer'&return-fields=name,id&facet=node_type,disease,species,tissue,platform,num_samples,created_by,modified_by,created_on,modified_on,acl,reference&rank=-created_on"
    },
    {
        desc: "clinical layers (via an annotation)", 
        query: "q=type:C&bq=node_type:'layer'&return-fields=name,id&facet=node_type,disease,species,tissue,platform,num_samples,created_by,modified_by,created_on,modified_on,acl,reference"
    },
    {
        desc: "TCGA Level 3 Layers (via an annotation)", 
        query: "q=tcgaLevel:Level_3&return-fields=name,id&facet=node_type,disease,species,tissue,platform,num_samples,created_by,modified_by,created_on,modified_on,acl,reference"
    },
    {
        desc: "basically a select * on a literal field",
        query: "bq=node_type:'*'&return-fields=name,id&facet=node_type,disease,species,tissue,platform,num_samples,created_by,modified_by,created_on,modified_on,acl,reference"
    },
    {
        desc: "basically a select * on a numeric field",
        query: "bq=created_on:0..&return-fields=name,id&facet=node_type,disease,species,tissue,platform,num_samples,created_by,modified_by,created_on,modified_on,acl,reference"
    },
    {
        desc: "all datasets or layers",
        query: "bq=(or node_type:'layer' node_type:'dataset')&return-fields=name,id&facet=node_type,disease,species,tissue,platform,num_samples,created_by,modified_by,created_on,modified_on,acl,reference"
    },
    {
        desc: "only datasets or layers created by Nicole",
        query: "bq=(and (or node_type:'layer' node_type:'dataset') created_by:'nicole.deflaux@sagebase.org')&return-fields=name,id&facet=node_type,disease,species,tissue,platform,num_samples,created_by,modified_by,created_on,modified_on,acl,reference"
    },
    {
        desc: "Matt's datasets",
        query: "bq=(and node_type:'dataset' created_by:'matt.furia@sagebase.org')&return-fields=name,id&facet=node_type,disease,species,tissue,platform,num_samples,created_by,modified_by,created_on,modified_on,acl,reference"
    },
    {
        desc: "Matt's datasets that Nicole is able to see",
        query: "bq=(and (or acl:'PUBLIC' acl:'AUTHENTICATED_USERS' acl:'nicole.deflaux@gmail.com') node_type:'dataset' created_by:'matt.furia@sagebase.org')&return-fields=name,id&facet=node_type,disease,species,tissue,platform,num_samples,created_by,modified_by,created_on,modified_on,acl,reference"
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
                            + "<span class=\"example\" onclick=\"getFacet('"
                            + addFacetToCurrentSearchQuery(i, item.value)
                            +"')\">"
                            + item.count + "</span></li>";
                    });
                    facetList += "</ol>"
                }
                else if($.isEmptyObject(facet)) {
                    facetList += " none"
                }
                else {
                    facetList += " from " + facet.min + " to " + facet.max + "<ol>"
                    var facetIncrement = Math.ceil((facet.max - facet.min) / 10);
                    for(var j=0; j < 10; j++) {
                        var displayMin,displayMax;
                        var min = facet.min + (facetIncrement * j);
                        var max = facet.min + (facetIncrement * (j+1));
                        if(("created_on" === i) || ("modified_on" === i)) {
                            displayMin = new Date(min * 1000)
                            displayMax = new Date(max * 1000)
                        }
                        else {
                            displayMin = min
                            displayMax = max
                        }

                        var facetDisplay = displayMin + ".." + displayMax
                        
                        facetList += "<li>"
                            + "<span class=\"example\" onclick=\"getFacet('"
                            + addNumericFacetToCurrentSearchQuery(i, min, max)
                            +"')\">"
                            + facetDisplay + "</span></li>";
                    }
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
    return escapeBooleanQuery($('input#query').val() 
                             + "&bq="
                             + facetName
                             + ":'"
                             + facetValue
                             + "'")
}

function addNumericFacetToCurrentSearchQuery(facetName, facetMinValue, facetMaxValue) {
    return escapeBooleanQuery($('input#query').val()
                             + "&bq="
                             + facetName
                             + ":"
                             + facetMinValue
                             + ".."
                             + facetMaxValue)
//         + "&facet-"
//         + facetName
//         + "-constraints="
//         + facetMinValue
//         + ".."
//         + facetMaxValue
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
function getSearchResults() {
    // We need to *double* escape boolean query
    var escapedQuery = escapeBooleanQuery($('input#query').val())
    
    var url = SEARCH + escape(escapedQuery);
    $("#results").html("")
    $("#facets").html("")   
    $("#debug_cloudsearch_url").html("");
    $("#debug_response").html("");
    $("#debug_repo_url").html("<a href='" + url + "'>" + url + "</a>");
    $.ajax({
        url: url,
        type: 'GET',
        dataType: 'json',
        success: displaySearchResults,
        error: displayError,
        beforeSend: setHeader
    });
}

function setHeader(xhr) {
	xhr.setRequestHeader('sessionToken', $("#sessionToken").val());
}

function displayError(jqXHR, textStatus, errorThrown) {
	alert(jqXHR.responseText)
}

function getExample(exampleNum) {
	$('input#query').val(EXAMPLES[exampleNum].query)
	getSearchResults()
}

function getFacet(facetQuery) {
	$('input#query').val(facetQuery)
	getSearchResults()
}

/**
 * Stuff to do when the document loads
 */
$(document).ready(function() {

    $(".button").click(getSearchResults)
    $("#examples").append("<table>")
    $.each(EXAMPLES, function(i, example) {
        $("#examples").append("<tr><td>" + example.desc + "</td><td><span class=\"example\" onclick=\"getExample('"+ i +"')\">" + example.query + "</span></td></tr><br>");
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

