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
cs-configure-access-policies --domain-name $DOMAIN --update --force --allow 184.72.137.151/32 --service doc

# Folks at the hutch can access all of it
cs-configure-access-policies --domain-name $DOMAIN --update --force --allow 140.107.0.0/16 --service all


#-----------[ Configure the search index fields ]--------------------

# Literal fields to be returned in Search Results
cs-configure-indexing --domain-name $DOMAIN --option result --option search --type literal --name id
cs-configure-indexing --domain-name $DOMAIN --option result --type literal --name etag 

# Free text fields to be returned in Search Results
cs-configure-indexing --domain-name $DOMAIN --option result --name name --type text 
cs-configure-indexing --domain-name $DOMAIN --option result --type text --name description

# Free text fields
cs-configure-indexing --domain-name $DOMAIN --option noresult --type text --name annotations 
cs-configure-indexing --domain-name $DOMAIN --option noresult --type text --name boost

# Literal text fields
cs-configure-indexing --domain-name $DOMAIN --option facet --option search --type literal --name parent_id
cs-configure-indexing --domain-name $DOMAIN --option facet --option search --type literal --name acl
cs-configure-indexing --domain-name $DOMAIN --option facet --option search --type literal --name update_acl
cs-configure-indexing --domain-name $DOMAIN --option facet --option search --type literal --name created_by
cs-configure-indexing --domain-name $DOMAIN --option facet --option search --type literal --name modified_by
cs-configure-indexing --domain-name $DOMAIN --option facet --option search --type literal --name disease 
cs-configure-indexing --domain-name $DOMAIN --option facet --option search --type literal --name node_type
cs-configure-indexing --domain-name $DOMAIN --option facet --option search --type literal --name platform 
cs-configure-indexing --domain-name $DOMAIN --option facet --option search --type literal --name reference
cs-configure-indexing --domain-name $DOMAIN --option facet --option search --type literal --name species
cs-configure-indexing --domain-name $DOMAIN --option facet --option search --type literal --name tissue

# Numeric fields
cs-configure-indexing --domain-name $DOMAIN --type uint --name modified_on
cs-configure-indexing --domain-name $DOMAIN --type uint --name created_on
cs-configure-indexing --domain-name $DOMAIN --type uint --name num_samples

# Now run indexing
cs-index-documents --domain-name $DOMAIN

