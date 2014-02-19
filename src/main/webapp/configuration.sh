#!/bin/sh

temp=`pwd`

cp -fR . "$1"

cd $1

if [ -d $1/temp-generated_configs ]; then
 rm -rf $1/temp-generated_configs
fi

$1/controller --generate-configs configurations.xml

rm -rf "$temp"
mkdir "$temp"

cp -fR "$1/artifacts/configurations.zip" "$temp"