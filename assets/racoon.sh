#!/system/bin/sh

dir=`dirname $0`
exec su -c "exec su -c \"LD_LIBRARY_PATH=$dir $dir/racoon $*\" 0:0"
