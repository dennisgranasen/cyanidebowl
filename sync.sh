#!/bin/bash

date=$(date +%Y%m%d%H%M%S)

for i in backend frontend; do
	docker save warpscores/warpscores-$i:1.0.0-SNAPSHOT -o warpscores-$i.tar
done

rsync -rtvzv -b --suffix .$date warpscores-*.tar vserver.wawuschels.de:/var/customers/webs/christian/warpscores_net/
