#!/bin/bash
#
# rsuite
#
# Startup script for RSuite
#
# chkconfig: 345 55 25
# description: RSuite Tomcat service
#
HOST=`hostname`
RSUITE_HOME=/opt/RSuite
export RSUITE_HOME
CATALINA_HOME=$RSUITE_HOME/tomcat
CATALINA_PID=$RSUITE_HOME/rsuite.pid
export CATALINA_PID
RSUITE_LANG=en_US.UTF-8
RSUITE_USER=rsuite
OLD_JAVA_HOME=$JAVA_HOME
JAVA_HOME="/usr/java/jdk1.8.0_92"
export JAVA_HOME
# Is JRE_HOME used or needed?
JRE_HOME="$JAVA_HOME/jre"
export JRE_HOME

JAVA_OPTS_START="-XX:+UseConcMarkSweepGC -Xms1024M -Xmx4096M -Drsuite.home=$RSUITE_HOME -Dfile.encoding=UTF8"
JAVA_OPTS_STOP="-XX:+UseConcMarkSweepGC -Xms256M -Xmx512M"

RSUITE_PORT=8080
RSUITE_PORT_SHUTDOWN=8005
COMMAND_WAIT_SECS=15

. /etc/rc.d/init.d/functions
if [ -f /etc/sysconfig/rsuite ]; then
	. /etc/sysconfig/rsuite
fi

RETVAL=0
PROG=rsuite


#
# isrunning()
#
isrunning()
{
    ISRUNNING=`netstat -vatn|grep LISTEN|grep $RSUITE_PORT|wc -l`
}


#
# start()
#
start()
{
	START_OK=0

        export JAVA_OPTS=$JAVA_OPTS_START

	echo -n $"Starting $PROG on $HOST: "
	
	if [ -f $CATALINA_HOME/bin/startup.sh ]; then
		isrunning
		if [ $ISRUNNING -eq 0 ]; then
                        #cd $CATALINA_HOME
			/bin/su $RSUITE_USER -c $CATALINA_HOME/bin/startup.sh #  > /dev/null
			RETVAL=$?
            if [ $RETVAL -eq 0 ]
            then
                LOOPCOUNT=$COMMAND_WAIT_SECS
                while [ $LOOPCOUNT -ne 0 ]
                do
                    isrunning
                    if [ $ISRUNNING -eq 1 ]
                    then
						START_OK=1
						LOOPCOUNT=0
					else
                        LOOPCOUNT=$(($LOOPCOUNT-1))
						sleep 1
                    fi
                done
            fi
		fi
	fi

	if [ $START_OK -ne 0 ]
	then
		echo_success
	else
		echo_failure
	fi

	echo
	[ $RETVAL -eq 0 ] && touch /var/lock/rsuite
	
	return $RETVAL
}


#
# stop()
#
stop()
{
	STOP_OK=0

	export JAVA_OPTS=$JAVA_OPTS_STOP

        echo -n $"Stopping $PROG on $HOST: "

	if [ -f $CATALINA_HOME/bin/shutdown.sh ]; then
		isrunning
		if [ $ISRUNNING -ne 0 ]
		then
			/bin/su $RSUITE_USER -c "$CATALINA_HOME/bin/shutdown.sh -force" > /dev/null
			RETVAL=$?
			if [ $RETVAL -eq 0 ]
			then
				LOOPCOUNT=$COMMAND_WAIT_SECS
				while [ $LOOPCOUNT -gt 0 ]
				do
					isrunning
					if [ $ISRUNNING -eq 0 ]
					then
						STOP_OK=1
						LOOPCOUNT=0
					else
						LOOPCOUNT=$(($LOOPCOUNT-1))
						sleep 1
					fi
				done
			fi
		fi
	fi

	if [ $STOP_OK -ne 0 ]
	then
		echo_success
	else
		echo_failure
	fi

    echo
    [ $RETVAL -eq 0 ] && rm -f /var/lock/rsuite

	return $RETVAL
}


#
# status()
#
status()
{
	isrunning
	if [ $ISRUNNING -ne 0 ]; then
		echo "$PROG is running."
	else
		echo "$PROG is stopped."
	fi
}




if [ `whoami | grep root` ]
then
    case "$1" in
        start)
            start
            ;;
        stop)
            stop
            ;;
        restart)
            stop
			
			sleep 1
            start
            ;;
        status)
            status
            ;;
        *)
            echo $"Usage: $prog {start|stop|restart|status}"
            RETVAL=1
    esac
else
    echo $"This script must be run as root."
fi

JAVA_HOME=$OLD_JAVA_HOME
export JAVA_HOME

exit $RETVAL

