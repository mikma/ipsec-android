#!/system/bin/sh

dir=`dirname $0`
LD_LIBRARY_PATH=$dir $dir/setkey "$@"
