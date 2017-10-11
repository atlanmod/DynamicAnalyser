#!/usr/bin/env bash
repourl=~/Documents/git/
for jar in ./libs/*; do
    echo "installing $jar"
    mvn install:install-file -Dfile=$jar -Dpackaging=jar
done
