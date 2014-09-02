#!/bin/bash

cd "$(dirname "$0")"
pwd

BUILD="$(date +%s)"

(
cd ../webpages

find . -name "*.html" | \
while read F
do
	G="../assets/webpages/$F.gzip"
	#echo "Compressing $F => $G"
	sed -E -e "s/build=[0-9]+/build=$BUILD/g" "$F" | gzip -c9 > "$G"
	printf "%-20s %6d -> %6d %20s\n" "$F" "$(cat $F | wc -c)" "$(cat $G | wc -c)" "$G"
done

find . -name "*.css" -or -name "*.js" | \
while read F
do 
	G="../assets/webpages/$F.gzip"
	#echo "Compressing $F => $G"
	gzip -9c "$F" > "$G"
	printf "%-20s %6d -> %6d %20s\n" "$F" "$(cat $F | wc -c)" "$(cat $G | wc -c)" "$G"
done

cp -rv img ../assets/webpages

#ls -lR | md5 > ../checksums/webpages.md5
)

