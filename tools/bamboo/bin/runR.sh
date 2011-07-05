#!/bin/bash

export LD_LIBRARY_PATH=/mnt/bamboo-ebs/lib/:/mnt/bamboo-ebs/R/lib/:/opt/jdk-6/jre/lib/i386/client/$LD_LIBRARY_PATH

/mnt/bamboo-ebs/bin/R "$@"


