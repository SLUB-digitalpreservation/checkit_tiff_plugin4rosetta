How to build checkit-tiff variants for Redhat system used by Rosetta
====================================================================

:lang: en
:encoding: utf-8
:author: Andreas Romeyke

== Prerequisites

* you need 'docker'

== How to build

docker build -t checkit-tiff --rm=true ./

== find out which image

docker images

lists:
REPOSITORY          TAG                 IMAGE ID            CREATED             SIZE
checkit-tiff        latest              acc13d4a83d9        3 minutes ago       827 MB
<none>              <none>              e3be26e603f0        5 minutes ago       824 MB
centos              6.8                 0cd976dc0a98        10 months ago       195 MB


== How to copy already built binaries

id=$(docker create checkit-tiff)

docker cp $id:/tmp/checkit_tiff_stable.tgz ./
docker cp $id:/tmp/checkit_tiff_development.tgz ./

docker rm -v $id


