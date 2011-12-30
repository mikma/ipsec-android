#!/system/bin/sh

stdout=/proc/$$/fd/1

if [ $1 = "all" ]; then
    echo Killall > $stdout
    su -c "killall racoon"
else
    echo Kill $1 > $stdout
    su -c "kill $1"
fi
