#!/bin/bash -xe

java -jar /var/gerrit/bin/gerrit.war init --batch -d /var/gerrit
exec /var/gerrit/bin/gerrit.sh run