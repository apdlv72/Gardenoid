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
	#echo "================"
	#echo "Compressing $F => $G"
	tr '\t' ' ' < "$F" | tr '\r' '\n' | sed -E -e "s/^ *//g; s/ *$//g; s/build=[0-9]+/build=$BUILD/g; s/ +/ /g;" | grep -v '^$' | grep -v '^<!--.*//-->$' | gzip -c9 > "$G"
	#gzip -c9 < "$F" > "$G"
	printf "%-20s %6d -> %6d %20s\n" "$F" "$(cat $F | wc -c)" "$(cat $G | wc -c)" "$G"
done

find . -name "*.css" -or -name "*.js" | \
while read F
do 
	G="../assets/webpages/$F.gzip"
	#echo "Compressing $F => $G"
	tr '\t' ' ' < "$F" | tr '\r' '\n' | sed -E -e "s/^ *//g; s/ *$//g; s/ +/ /g;" | grep -v '^$' | gzip -c9 > "$G"
	#gzip -9c < "$F" > "$G"
	printf "%-20s %6d -> %6d %20s\n" "$F" "$(cat $F | wc -c)" "$(cat $G | wc -c)" "$G"
done

cp -rv img ../assets/webpages

#ls -lR | md5 > ../checksums/webpages.md5
)

