#!/bin/bash

#Creating a CA authority certificate and adding it into keystore
https://community.jboss.org/wiki/KeystoreFormatsJKSAndPEMCheatsheet


#https://thomas-leister.de/internet/eigene-openssl-certificate-authority-ca-erstellen-und-zertifikate-signieren/

# http://www.coderanch.com/t/435198/Security/Creating-CA-keytool
# Create a private key and certificate request for your own CA:  
#(cat << EOF
#DE
#NRW
#Cologne
#.
#.
#apdlv72@gmail.com
#EOF
#) | \

openssl req -new -newkey rsa:1024 -nodes -out rootCA.csr -keyout rootCA.key

# Create your CA's self-signed certificate (note lasts one year -  
# increase the days setting to whatever you want):  
openssl x509 -trustout -signkey rootCA.key -days 10000 -req -in rootCA.csr -out rootCA.pem  

rm keystore
echo | keytool \
    -genkey -v \
    -alias Gardenoid \
    -dname "CN=Android Watering App, OU=Gardenoid, O=APdlV, ST=Cologne, C=DE" \
    -validity 10000 \
    -keyalg RSA -keysize 512 \
    -keystore keystore \
    -storepass test99

#keytool -list -keystore keystore -storepass test99
# Create certificate sign request
keytool -certreq -alias Gardenoid -keyalg RSA -file gardenoid.csr -keystore keystore -storepass test99

# Sign the 
openssl  x509  -req  -CA rootCA.pem -CAkey rootCA.key -in gardenoid.csr -out gardenoid.cert -days 10000 -CAcreateserial

# Use the keytool to import the signed certificate for the associated client alias in the keystore.

KEYSTORE="../assets/ssl/gardenoid.BKS"
TYPEARGS="-storetype BKS -providerpath ../extras/bcprov-jdk16-1.45.jar -provider org.bouncycastle.jce.provider.BouncyCastleProvider"

#KEYSTORE="../assets/ssl/gardenoid.PKCS12"
#TYPEARGS="-storetype PKCS12"

# create a self siogned BKS only
rm "$KEYSTORE"
keytool -genkey -keyalg RSA -alias Gardenoid -keystore "$KEYSTORE" -storepass test99 -validity 10000 -keysize 2048 $TYPEARGS

keytool -exportcert -alias Gardenoid -keystore "$KEYSTORE" -file Gardenoid.cer -storetype keytool

# create BKS signed by above CA
#keytool -importcert -v -alias Gardenoid -file gardenoid.cert -keystore "$KEYSTORE" -storepass test99 $TYPEARGS
keytool -list -keystore "$KEYSTORE" -storepass test99 $TYPEARGS

exit 0


