#!/bin/bash
set -eux
set -o pipefail

verbose="false"
host='http://127.0.0.1:8080'
clients=1
rate=10

display_help ()
{
	echo "Usage: [option...]"
	echo
	echo "   -h                    Show this help message and exit"
	echo
	echo "   -H HOST               Host to load test in the following format:"
	echo "                         http://10.21.32.33:8080"
	echo
	echo "   -c NUM_CLIENTS        Number of concurrent clients."
	echo
	echo "   -r HATCH_RATE         The rate per second in which clients are spawned."
	echo
	echo "   -v                    Output verbose"
	echo

	exit $?
}

while getopts 'H:c:r:vh' flag; do
	case "${flag}" in
		h) display_help ;;
		H) host=${OPTARG} ;;
		c) clients=${OPTARG} ;;
		r) rate=${OPTARG} ;;
		v) verbose="true" ;;
		*) display_help ;;
	esac
done

if [ "$verbose" = "true" ] ; then
echo "Host = ${host}"
echo "Clients = ${clients}"
echo "Spawn Rate = ${rate}"

#data = {"name":"locust.long", "type":"metric"}
curl --verbose -H "Content-Type: application/json" -X POST -d '{"name":"locust.long","type":"metric"}' "$host"/labels
#data = {"name":"locust.double", "type":"metric"}
curl --verbose -H "Content-Type: application/json" -X POST -d '{"name":"locust.float","type":"metric"}' "$host"/labels
#data = {"name":"locust.float", "type":"metric"}
curl --verbose -H "Content-Type: application/json" -X POST -d '{"name":"locust.double","type":"metric"}' "$host"/labels
else
#data = {"name":"locust.long", "type":"metric"}
curl -H "Content-Type: application/json" -X POST -d '{"name":"locust.long","type":"metric"}' "$host"/labels
#data = {"name":"locust.double", "type":"metric"}
curl -H "Content-Type: application/json" -X POST -d '{"name":"locust.float","type":"metric"}' "$host"/labels
#data = {"name":"locust.float", "type":"metric"}
curl -H "Content-Type: application/json" -X POST -d '{"name":"locust.double","type":"metric"}' "$host"/labels
fi

#run locust
locust --no-web -c "$clients" -r "$rate"
