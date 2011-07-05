#!/system/bin/sh

dir=`dirname $0`
LD_LIBRARY_PATH=$dir su -c "$dir/racoon $*"
