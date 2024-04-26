#!/bin/bash

date=$(date +%Y%m%d%H%M%S)

echo "Exporting docker images..."
for i in backend frontend; do
	docker save warpscores/warpscores-$i:1.0.0-SNAPSHOT -o warpscores-$i.tar
done

echo "Uploading docker images..."
rsync -rtvzv -b --suffix .$date warpscores-*.tar vserver.wawuschels.de:/var/customers/webs/christian/warpscores_net/
