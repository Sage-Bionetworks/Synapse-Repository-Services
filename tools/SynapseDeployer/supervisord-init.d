#!/bin/bash
#
# Startup script for the Supervisor server
#
# chkconfig: 2345 85 15
# description: Supervisor is a client/server system that allows its users to \
#              monitor and control a number of processes on UNIX-like \
#              operating systems.
#
# processname: supervisord
# pidfile: /var/run/supervisord.pid

# Source function library.
. /etc/rc.d/init.d/functions

RETVAL=0
prog="supervisord"
SUPERVISORD=/usr/bin/supervisord
PID_FILE=/var/run/supervisord.pid

start()
{
        echo -n $"Starting $prog: "
        $SUPERVISORD --pidfile $PID_FILE && success || failure
        RETVAL=$?
        echo
        return $RETVAL
}

stop()
{
        echo -n $"Stopping $prog: "
        killproc -p $PID_FILE -d 10 $SUPERVISORD
        RETVAL=$?
        echo
}

reload()
{
        echo -n $"Reloading $prog: "
        if [ -n "`pidfileofproc $SUPERVISORD`" ] ; then
            killproc $SUPERVISORD -HUP
        else
            # Fails if the pid file does not exist BEFORE the reload
            failure $"Reloading $prog"
        fi
        sleep 1
        if [ ! -e $PID_FILE ] ; then
            # Fails if the pid file does not exist AFTER the reload
            failure $"Reloading $prog"
        fi
        RETVAL=$?
        echo
}

case "$1" in
        start)
                start
                ;;
        stop)
                stop
                ;;
        restart)
                stop
                start
                ;;
        reload)
                reload
                ;;
        status)
                status -p $PID_FILE $SUPERVISORD
                RETVAL=$?
                ;;
        *)
                echo $"Usage: $0 {start|stop|restart|reload|status}"
                RETVAL=1
esac
exit $RETVAL
