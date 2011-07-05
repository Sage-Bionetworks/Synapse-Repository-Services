#!/bin/bash

# This script can be found on the bamboo host:
# /mnt/bamboo-ebs/bin/customise-extras.sh

# http://confluence.atlassian.com/display/BAMBOO/Accessing+an+Elastic+Instance
# http://confluence.atlassian.com/display/BAMBOO/Configuring+Elastic+Instances+to+use+the+EBS
# http://confluence.atlassian.com/display/BAMBOO/Populating+your+EBS+volume

echo 'export LD_LIBRARY_PATH=/mnt/bamboo-ebs/lib/:/mnt/bamboo-ebs/R/lib/:/opt/jdk-6/jre/lib/i386/client/$LD_LIBRARY_PATH' >> /etc/profile.d/bamboo
echo 'export PATH=/mnt/bamboo-ebs/bin:$PATH' >> /etc/profile.d/bamboo

export LD_LIBRARY_PATH=/mnt/bamboo-ebs/lib/:$LD_LIBRARY_PATH
/usr/sbin/groupadd -g 27 mysql
/usr/sbin/useradd -u 27 -g 27 mysql
cd /mnt/bamboo-ebs;./bin/mysqld_safe &

# To shutdown mysql
# /mnt/bamboo-ebs/bin/mysqladmin -u root -p shutdown