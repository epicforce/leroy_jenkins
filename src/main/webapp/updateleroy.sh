#!/bin/sh

if [ "$2"="86" ]; then
	wget "https://dl.dropboxusercontent.com/u/250424534/leroy_Linux-i686.tgz"
	if [ -d $3/Linux-i686 ]; then
		rm -rf $3/Linux-i686
	fi	
	tar zxvf leroy_Linux-i686.tgz
	cp -fR "$3/Linux-i686/" "$1" 
else
	wget "https://dl.dropboxusercontent.com/u/250424534/leroy_Linux-x86_64.tgz"
	if [ -d $3/Linux-x86_64 ]; then
		rm -rf $3/Linux-x86_64
	fi
	tar zxvf leroy_Linux-x86_64.tgz
	cp -fR "$3/Linux-x86_64/" "$1"
fi