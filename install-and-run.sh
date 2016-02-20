#!/usr/bin/env bash

#Does a full recompile, then runs it.

git pull
mvn clean install -DskipTests=true
java -Xms27g -Xmx27g -jar target/orak*.jar
