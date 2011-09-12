#!/bin/bash

stack=$1
create=$2

if [ -z "$stack" ] ; then
    echo "missing stack name"
    exit 0
fi

export PACKAGE_ROOT=/home/sagebio/sagebioRPackages/$stack
if [ ! -d $PACKAGE_ROOT ] ; then
    echo "package root $PACKAGE_ROOT does not exist"
    exit 0
fi

export REPO_ROOT=/srv/www/htdocs/Foswiki/CRAN/$stack
if [ ! -d $REPO_ROOT ] ; then
    echo "repository root $REPO_ROOT does not exist"
    exit 0
fi

if [ -n "$create" ] && [ "$create" = "create" ] ; then
    ## Make the CRAN directories
    mkdir -p $REPO_ROOT/2.13/bin/macosx/leopard/contrib/2.13
    mkdir -p $REPO_ROOT/2.13/bin/windows/contrib/2.13
    ln -s $REPO_ROOT/2.13/bin/windows  $REPO_ROOT/2.13/bin/windows64
    mkdir -p $REPO_ROOT/2.13/src/contrib
    ln -s $REPO_ROOT/2.13 $REPO_ROOT/2.14
fi

## Copy the packages from the deployment area to the right spots in
## the CRAN directory tree
cp $PACKAGE_ROOT/*zip $REPO_ROOT/2.13/bin/windows/contrib/2.13
cp $PACKAGE_ROOT/*tgz $REPO_ROOT/2.13/bin/macosx/leopard/contrib/2.13
cp $PACKAGE_ROOT/*tar.gz $REPO_ROOT/2.13/src/contrib

## Make the PACKAGE and PACKAGE.gz files
cd $REPO_ROOT/2.13/src/contrib ; R -e 'tools:::write_PACKAGES(".", type="source")'
cd $REPO_ROOT/2.13/bin/macosx/leopard/contrib/2.13; R -e 'tools:::write_PACKAGES(".", type="mac.binary")'
cd $REPO_ROOT/2.13/bin/windows/contrib/2.13; R -e 'tools:::write_PACKAGES(".", type="win.binary")'
