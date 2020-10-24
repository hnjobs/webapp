#!/bin/bash

set -eu

mvn package

ssh root@hnjobs '/etc/init.d/jetty8 stop'

scp target/HnJobs-1.0-SNAPSHOT.war root@hnjobs:/var/lib/jetty8/webapps/root.war

ssh root@hnjobs '/etc/init.d/jetty8 start'

