#!/usr/bin/env bash

set -e

DOCKER_REGISTRY=686140181923.dkr.ecr.us-east-1.amazonaws.com

LATEST_COMMIT=`git rev-parse --short HEAD`
CURRENT_BRANCH=`git rev-parse --abbrev-ref HEAD`

DOCKER_TAG=${CURRENT_BRANCH//\//_}"-"$LATEST_COMMIT

# Tag it with DOCKER_TAG
docker tag sample-sdk-server:latest $DOCKER_REGISTRY/sample-sdk-server:$DOCKER_TAG

# docker login to our ecr for us-east-1 region
`aws ecr get-login --region us-east-1`

docker push $DOCKER_REGISTRY/sample-sdk-server:$DOCKER_TAG
