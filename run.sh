#!/usr/bin/env bash

#Use this script for a full run of the scraper. Note: It requires install.sh to be run first if a recompile is required

java -d64 -Xms512m -Xmx4g -jar  orak-1.0-SNAPSHOT-fat.jar -c ../config.json
