#!/bin/bash
#
# BEGIN_COPYRIGHT
#
# Licensed Materials - Property of IBM
#
# (C) Copyright IBM Corp. 2019, 2021 All Rights Reserved.
#
# US Government Users Restricted Rights - Use, duplication or
# disclosure restricted by GSA ADP Schedule Contract with
# IBM Corp.
#
# END_COPYRIGHT
#


show_help() {
  cat <<EOF
Usage: ${BASH_SOURCE[0]} --endpoint endpoint --user username [--password password] command
Usage: ${BASH_SOURCE[0]} --endpoint endpoint --token token command

Watson Discovery Crawler Plug-in Manager

This script will help you deploy, undeploy, and list your crawler plug-ins for
Watson Discovery.

Commands:
  deploy        Add a new crawler plug-in to your Watson Discovery instance
  undeploy      Undeploy your crawler plug-in by ID
  list          List all crawler plug-ins for your Watson Discovery instance (default)

Options:
  -e --endpoint         The endpoint URL for your cluster and add-on service instance
                        (https://{cpd_cluster_host}{:port}/discovery/{release}/instances/{instance_id}/api)
  -t --token            The authorization token of your Cloud Pak instance
  -u --user             The user name of your Cloud Pak instance
  -p --password         The user password of your Cloud Pak instance
                        If the password is not specified, the command line prompts to input
  -n --name             The name of the crawler plug-in to upload (deploy only)
  -f --file             The path of the crawler plug-in package to upload (deploy only)
  --id                  The crawler_resource_id value to delete the crawler plug-in (undeploy only)
  --help                Show this message
EOF
}

show_error(){
  echo "$1" 1>&2; exit 1
}
check_param() {
  if [ -z "$1" ]; then
    show_error "$2"
  fi
}

WD_API=
USER=
PASSWORD=
PLUGIN_NAME=
PLUGIN_FILE=build/distributions/wd-crawler-plugin-sample.zip
PLUGIN_ID=
ACTION="list"
ACCESS_TOKEN=

if [ $# -eq 0 ]; then
  show_help; exit 0
fi

while (( $# > 0 )); do
  case "$1" in
  -h|--help)
    show_help; exit 0;;
  -e|--endpoint)
    shift;WD_API="$1";;
  -t|--token)
    shift;ACCESS_TOKEN="$1";;
  -u|--user)
    shift;USER="$1";;
  -p|--password)
    shift;PASSWORD="$1";;
  -n|--name)
    shift;PLUGIN_NAME="$1";;
  -f|--file)
    shift;PLUGIN_FILE="$1";;
  --id)
    shift;PLUGIN_ID="$1";;
  deploy|undeploy|list)
    ACTION="$1";;
  *)
    echo "Invalid command arguments ('$1' not supported)";
    echo "";
    show_help; exit 1;;
  esac
  shift
done

check_param "${WD_API}" "No endpoint URL specified"
if [ -z "${ACCESS_TOKEN}" ]; then
  check_param "${USER}" "No user name specified"
  if [ -z "${PASSWORD}" ]; then
    read -sp "Enter host password for user '${USER}': " PASSWORD
    echo
  fi
fi

STATUS_CODE=
call_api (){
  if [ -z "${ACCESS_TOKEN}" ]; then
    CP4D_API=`echo ${WD_API} | sed "s/^\(https:\/\/[^\/]*\/\).*$/\1/"`
    AUTH_API=${CP4D_API}icp4d-api/v1/authorize
    AUTH_RESULT=`curl -Ss -k -H "cache-control: no-cache" -H "Content-Type: application/json" -d "{\"username\":\"${USER}\",\"password\":\"${PASSWORD}\"}" -XPOST ${AUTH_API}`
    ACCESS_TOKEN=`echo ${AUTH_RESULT} | sed "s/^.*\"token\":\"\([^\"]*\)\".*/\1/"`
  fi
  METHOD=$1; shift
  ID=$1; shift
  RES=`curl -Ss -k -H "Authorization: Bearer ${ACCESS_TOKEN}" -H "x-watson-discovery-next: true" -X${METHOD} -w "\n%{http_code}" "${WD_API}/v2/crawler_resources/${ID}?resource_type=crawler_plugin&version=2019-11-22" $*`
  BODY=`echo "$RES" | sed '$d'`
  STATUS_CODE=`echo "$RES" | tail -n 1`
  if [ ${STATUS_CODE} -eq 401 ]; then
    echo "Authorization is required."
  else
    echo ${BODY}
  fi
}

case "${ACTION}" in
  deploy)
    check_param "${PLUGIN_NAME}" "No plug-in name specified"
    check_param "${PLUGIN_FILE}" "No plug-in package specified"
    if [ ! -f ${PLUGIN_FILE} ]; then
      show_error "The crawler plug-in package ${PLUGIN_FILE} cannot be found"
    fi
    call_api POST "" -F "file=@${PLUGIN_FILE}" -F 'metadata={"name":"'"${PLUGIN_NAME}"'","resource_type":"crawler_plugin"}"'
  ;;
  undeploy)
    check_param "${PLUGIN_ID}" "No plug-in ID specified"
    call_api DELETE "${PLUGIN_ID}"
    if [ ${STATUS_CODE} -eq 204 ]; then
      echo "The crawler plug-in ${PLUGIN_ID} was undeployed successfully."
    fi
  ;;
  list)
    call_api GET ""
  ;;
esac
