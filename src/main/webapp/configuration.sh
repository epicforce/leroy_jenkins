#!/bin/sh

temp=`pwd`

cp -fR . "$1"

cd $1

if [ -d $1/temp-generated_configs ]; then
 rm -rf $1/temp-generated_configs
fi

if [ ! -f configurations.xml ]; then
 echo "[ERROR] Missing configurations.xml in LEROY_HOME check your configuration"
 exit 1
fi

$1/controller --generate-configs configurations.xml

rm -rf "$temp"
mkdir "$temp"

cp -fR "$1/artifacts/configurations.zip" "$temp"
