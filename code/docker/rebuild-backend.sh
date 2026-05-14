#!/bin/bash
cd /vol3/1000/private/workProject/info-organization-product/code/docker
docker compose build backend
docker compose up -d backend
