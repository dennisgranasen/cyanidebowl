#!/bin/bash

docker-compose down

cd data || exit 1

sudo -E rsync -rtvzv vserver.wawuschels.de:/var/customers/webs/christian/warpscores_net/data/* ./

cd ..

docker-compose up -d
