#!/bin/bash

# This script can be found on the bamboo host:
# /mnt/bamboo-ebs/bin/customise-extras.sh

# 
http://confluence.atlassian.com/display/BAMBOO/Accessing+an+Elastic+Instance
# 
http://confluence.atlassian.com/display/BAMBOO/Configuring+Elastic+Instances+to+use+the+EBS
# 
http://confluence.atlassian.com/display/BAMBOO/Populating+your+EBS+volume


yum install -y mysql 
yum install -y mysql-server 
yum install -y mysql-devel 
chgrp -R mysql /var/lib/mysql 
chmod -R 770 /var/lib/mysql 
service mysqld start 
/usr/bin/mysqladmin create test2
/usr/bin/mysqladmin -u root password 'platform' 
