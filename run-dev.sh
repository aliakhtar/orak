#!/usr/bin/env bash

#This script runs without setting the proper memory settings (i.e giving full heap size to Java). It should
#not be used for full runs of the scraper, use it just for quick tests. Use run.sh for full scraper runs.

git pull
mvn clean compile
mvn exec:java -Dexec.mainClass="com.github.aliakhtar.orak.Main"