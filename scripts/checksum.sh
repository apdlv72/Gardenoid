#!/bin/bash

cd "$(dirname "$0")"
pwd

ls -lR ../assets/webpages | md5 > ../assets/checksums/webpages.md5

