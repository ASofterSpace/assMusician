#!/bin/bash

cd `dirname "$0"`

java -classpath "`dirname "$0"`/bin" -Xms256m -Xmx8192m com.asofterspace.assMusician.AssMusician "$@"
