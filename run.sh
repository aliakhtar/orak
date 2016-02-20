#!/usr/bin/env bash

#Use this script for a full run of the scraper. Note: It requires install.sh to be run first if a recompile is required

git pull
mvn clean compile
mvn exec:exec