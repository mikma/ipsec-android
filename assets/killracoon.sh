#!/system/bin/sh

if [ $1 = "all" ]; then
    su -c "killall -INT racoon"
else
    su -c "kill -INT $1"
fi
