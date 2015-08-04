#!/bin/bash

set -eu

user="freki"

getent group $user > /dev/null || \
    groupadd -r $user

getent passwd $user > /dev/null || \
    useradd -r -g $user -d /opt/freki -s /sbin/nologin \
    -c "The Freki sensor data collection and query service" $user

exit 0
