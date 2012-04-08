#!/system/bin/sh

dir=`dirname $0`
su -c "LD_LIBRARY_PATH=$dir $dir/setkey $*" 0:0
