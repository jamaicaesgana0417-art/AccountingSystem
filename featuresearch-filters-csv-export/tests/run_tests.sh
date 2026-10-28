#!/bin/sh
# Run from the project folder:  sh tests/run_tests.sh
# Needs a JDK (javac) on PATH. No database or internet needed.
rm -rf test-out && mkdir test-out || exit 1
javac -d test-out -cp lib/postgresql-42.7.14.jar src/*.java tests/*.java || exit 1
java -Djava.awt.headless=true -cp test-out FilterExportTests
