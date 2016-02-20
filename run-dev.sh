#!/usr/bin/env bash

git pull
mvn clean compile
mvn exec:java -Dexec.mainClass="com.github.aliakhtar.orak.Main"