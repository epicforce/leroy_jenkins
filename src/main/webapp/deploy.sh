#!/bin/sh

cd "$1"

"$1/controller" --workflow "$2" --environment "$3"