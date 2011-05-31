#!/bin/bash

# This script can be found on the bamboo host:
# /mnt/bamboo-ebs/bin/customise-extras.sh

# http://confluence.atlassian.com/display/BAMBOO/Accessing+an+Elastic+Instance
# http://confluence.atlassian.com/display/BAMBOO/Configuring+Elastic+Instances+to+use+the+EBS
# http://confluence.atlassian.com/display/BAMBOO/Populating+your+EBS+volume


### Setup MySQL
yum install -y mysql 
yum install -y mysql-server 
yum install -y mysql-devel 
chgrp -R mysql /var/lib/mysql 
chmod -R 770 /var/lib/mysql 
service mysqld start 
/usr/bin/mysqladmin create test2
/usr/bin/mysqladmin -u root password 'platform' 


### Setup Python2.7, no need to do this everytime if we are sure to
### install it on the EBS volume
#mkdir /mnt/bamboo-ebs/systemDependencies
#cd /mnt/bamboo-ebs/systemDependencies
#wget http://www.python.org/ftp/python/2.7.1/Python-2.7.1.tar.bz2
#tar -xvjf Python-2.7.1.tar.bz2
#cd Python*
#./configure --prefix=/mnt/bamboo/systemDependencies/python27
#make
#make install

### Setup R, no need to do this everytime if we are sure to
### install it on the EBS volume
#cd /mnt/bamboo-ebs/systemDependencies
#wget http://cran.fhcrc.org/src/base/R-2/R-2.13.0.tar.gz
#tar -xvzf  R-2.13.0.tar.gz
#cd R-2.13.0*
#./configure --prefix=/mnt/bamboo/systemDependencies/R
#make
#make install
