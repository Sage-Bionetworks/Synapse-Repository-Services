export STAGING_ROOT=/home/sagebio/sagebioRPackages/staging
export REPO_ROOT=/srv/www/htdocs/Foswiki/sagebioCRAN
#export REPO_ROOT=/home/ndeflaux/sagebioCRAN2

## Make the CRAN directories, this is just a one-time task so keeping it
## commented out
# mkdir -p $REPO_ROOT/2.13/bin/macosx/leopard/contrib/2.13
# mkdir -p $REPO_ROOT/2.13/bin/windows/contrib/2.13
# ln -s $REPO_ROOT/2.13/bin/windows  $REPO_ROOT/2.13/bin/windows64
# mkdir -p $REPO_ROOT/2.13/src/contrib

## Copy the packages from staging to the right spots in the CRAN directory tree
cp $STAGING_ROOT/*zip $REPO_ROOT/2.13/bin/windows/contrib/2.13
cp $STAGING_ROOT/*tgz $REPO_ROOT/2.13/bin/macosx/leopard/contrib/2.13
cp $STAGING_ROOT/*tar.gz $REPO_ROOT/2.13/src/contrib

## Make the PACKAGE and PACKAGE.gz files
cd $REPO_ROOT/2.13/src/contrib ; R -e 'tools:::write_PACKAGES(".", type="source")'
cd $REPO_ROOT/2.13/bin/macosx/leopard/contrib/2.13; R -e 'tools:::write_PACKAGES(".", type="mac.binary")'
cd $REPO_ROOT/2.13/bin/windows/contrib/2.13; R -e 'tools:::write_PACKAGES(".", type="win.binary")'
