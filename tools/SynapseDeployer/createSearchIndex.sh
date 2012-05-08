#!/bin/sh

DOMAIN=$1

echo Note that this script is idempotent so its okay to add a new index field and then re-run the script to configure the new index field 

if [ -z "$DOMAIN" ] ; then
        echo "missing AwesomeSearch domain name, the name of the search index to be created (e.g., prod-20120401)"
                exit 0
fi

#-----------[ Create the new domain ]--------------------

# Create the new search domain and wait for it to be available
cs-create-domain --domain-name $DOMAIN --wait

#-----------[ Configure network access policies ]--------------------

# Anyone can access the search service
cs-configure-access-policies --domain-name $DOMAIN --update --force --allow 0.0.0.0/0 --service search

# hudson.sagebase.org can access the document service
cs-configure-access-policies --domain-name $DOMAIN --update --force --allow 50.17.192.58/32 --service doc

# Folks at the hutch can access all of it
cs-configure-access-policies --domain-name $DOMAIN --update --force --allow 140.107.0.0/16 --service all

STACK=`echo  $DOMAIN | cut -d "-" -f "1"`
echo The stack is $STACK

if [ $STACK = 'dev' ] || [ $STACK = 'bamboo' ] ; then
    echo 'Anyone can access the document service for dev or bamboo'
    cs-configure-access-policies --domain-name $DOMAIN --update --force --allow 0.0.0.0/0 --service doc
fi

#-----------[ Configure the search index fields ]--------------------

# Literal fields to be returned in Search Results
cs-configure-fields --domain-name $DOMAIN --option result --option search --type literal --name id
cs-configure-fields --domain-name $DOMAIN --option result --type literal --name etag 

# Free text fields to be returned in Search Results
cs-configure-fields --domain-name $DOMAIN --option result --type text --name name 
cs-configure-fields --domain-name $DOMAIN --option result --type text --name description

# Free text fields
cs-configure-fields --domain-name $DOMAIN --option noresult --type text --name annotations 
cs-configure-fields --domain-name $DOMAIN --option noresult --type text --name boost

# Numeric fields (by default these are both faceted and available to be returned in search results)
cs-configure-fields --domain-name $DOMAIN --type uint --name modified_on
cs-configure-fields --domain-name $DOMAIN --type uint --name created_on
cs-configure-fields --domain-name $DOMAIN --type uint --name num_samples

# Literal text field facets
cs-configure-fields --domain-name $DOMAIN --option facet --option search --type literal --name parent_id
cs-configure-fields --domain-name $DOMAIN --option facet --option search --type literal --name acl
cs-configure-fields --domain-name $DOMAIN --option facet --option search --type literal --name update_acl
cs-configure-fields --domain-name $DOMAIN --option facet --option search --type literal --name created_by
cs-configure-fields --domain-name $DOMAIN --option facet --option search --type literal --name modified_by
cs-configure-fields --domain-name $DOMAIN --option facet --option search --type literal --name disease 
cs-configure-fields --domain-name $DOMAIN --option facet --option search --type literal --name node_type
cs-configure-fields --domain-name $DOMAIN --option facet --option search --type literal --name platform 
cs-configure-fields --domain-name $DOMAIN --option facet --option search --type literal --name reference
cs-configure-fields --domain-name $DOMAIN --option facet --option search --type literal --name species
cs-configure-fields --domain-name $DOMAIN --option facet --option search --type literal --name tissue

# Literal text fields to be returned in Search Results whose source is a facet
cs-configure-fields --domain-name $DOMAIN --option result --type literal --source created_by --name created_by_r
cs-configure-fields --domain-name $DOMAIN --option result --type literal --source modified_by --name modified_by_r
cs-configure-fields --domain-name $DOMAIN --option result --type literal --source node_type --name node_type_r
cs-configure-fields --domain-name $DOMAIN --option result --type literal --source disease --name disease_r
cs-configure-fields --domain-name $DOMAIN --option result --type literal --source tissue --name tissue_r

# Now run indexing
cs-index-documents --domain-name $DOMAIN

