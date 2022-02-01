#!/usr/bin/env bash

# Basic test to start the service, add data, and retrieve that data sorted by last name
lein with-profile service run </dev/null >/dev/null 2>&1 &
echo Waiting for service to start
nc -z localhost 8080 > /dev/null 2>&1
while [ 1 = $? ]; do
  sleep 1
  nc -z localhost 8080 > /dev/null 2>&1
done
echo Service started

while read line; do
  curl -s -X POST -H "Content-type: text/plain" http://localhost:8080/records -d "${line}"
done < resources/data.csv

output=`curl -s -H "Accept: application/json" http://localhost:8080/records/name`

expected='[{"last-name":"Bennet","first-name":"Elizabeth","email":"liz@longbourn.com","favorite-color":"blue","dob":"1\/28\/1813"},{"last-name":"Bennet","first-name":"Jane","email":"jane@longbourne.com","favorite-color":"blue","dob":"6\/5\/1811"},{"last-name":"Bingley","first-name":"Charles","email":"chuck@netherfield.co.uk","favorite-color":"blue","dob":"7\/10\/1807"},{"last-name":"Darcy","first-name":"Fitzwilliam","email":"bill@pemberley.com","favorite-color":"red","dob":"1\/28\/1806"},{"last-name":"Lucas","first-name":"Charlotte","email":"charlotte@hunsford.org","favorite-color":"lavendar","dob":"8\/22\/1806"},{"last-name":"Wickham","first-name":"George","email":"george@cad.com","favorite-color":"red","dob":"3\/19\/1808"}]'

if [ "${output}" = "${expected}" ]; then
  echo test PASSED
else
  echo test FAILED
fi

PID=$(ps auxwww | grep java | grep "with-profile service run" | cut -c 16-23)
if [ "${PID}" != "" ]; then
  kill "${PID}"
  echo Service stopped
fi

