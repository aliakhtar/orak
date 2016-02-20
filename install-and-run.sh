#!/usr/bin/env bash

#Does a full recompile, then runs it.

git pull
mvn clean install -DskipTests=true
./run.sh
