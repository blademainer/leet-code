#!/usr/bin/env bash

process_name="$1"
[ -z "$process_name" ] && process_name="H2O"
jstack -l `jps | grep $process_name | awk '{print $1}'`