#!/bin/bash

cd "$(dirname "$0")"
pwd

(
cd ../assets/webpages
find . -name "*.css" -or -name "*.js" | \
while read F
do 
	gzip -9c "$F" > "$F.gzip"
	printf "%-20s %6d -> %6d\n" "$F" "$(cat $F | wc -c)" "$(cat $F.gzip | wc -c)"
done


ls -lR | md5 > ../checksums/webpages.md5
)

