#!/usr/bin/env bash

main() {
  local tag=marnikita/alpinist
  mvn clean install \
    && docker build -t "$tag" application \
    && sudo docker push "$tag" \
    && ssh betty \
    "docker pull \"$tag\" && docker-compose up -d --no-deps alpinist"
}
main
