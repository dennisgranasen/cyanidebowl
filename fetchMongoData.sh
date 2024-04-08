#!/bin/bash

docker-compose down

cd data

sudo -E rsync -rtvzv vserver.wawuschels.de:/var/customers/webs/christian/warpscores_net/data/* ./

cd ..

docker-compose up -d
