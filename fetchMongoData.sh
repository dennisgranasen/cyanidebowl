#!/bin/bash

docker-compose down

cd data || exit 1

sudo -E rsync -rtvzv --exclude mongod.lock --exclude WiredTiger.lock vserver.wawuschels.de:/var/customers/webs/christian/warpscores_net/data/* ./

cd ..

docker run -v ./data:/data/db mongo:7.0.5 mongod --repair

docker-compose up -d
