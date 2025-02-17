#!/bin/bash
#get the actual path of this script
SAAF_BIN="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo $SAAF_BIN
VM_ARGS="-Xms512M -Xmx2G"
cd $SAAF_BIN/..
java $VM_ARGS \
-Dfile.encoding=UTF-8 \
-jar dist/SAAF.jar "$@"
