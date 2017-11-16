FROM centos:6.8
MAINTAINER Andreas Romeyke <romeyke@slub-dresden.de>
ENV checkit_tiff_current_version=v0.2.4
ENV checkit_tiff_upcoming_version=v0.3.1
ENV checkit_tiff_gitrepository=https://github.com/SLUB-digitalpreservation/checkit_tiff.git
ENV pcre_version=8.41
ENV PATCH="\
--- a/src/CMakeLists.txt\n\
+++ b/src/CMakeLists.txt\n\
@@ -28,9 +28,6 @@\n\
 # Make a version file containing the current version from git.\n\
 #\n\
 execute_process( COMMAND git rev-list HEAD --count OUTPUT_VARIABLE REPO_REVISION OUTPUT_STRIP_TRAILING_WHITESPACE )\n\
-execute_process( COMMAND git rev-parse --abbrev-ref HEAD OUTPUT_VARIABLE BUILD_BRANCH OUTPUT_STRIP_TRAILING_WHITESPACE)\n\
-execute_process( COMMAND git rev-parse HEAD OUTPUT_VARIABLE BUILD_REV_ID OUTPUT_STRIP_TRAILING_WHITESPACE)\n\
-execute_process( COMMAND git describe --long --tags --dirty --always OUTPUT_VARIABLE BUILD_REV_ID_SHORT OUTPUT_STRIP_TRAILING_WHITESPACE)\n\
 add_definitions(-DVERSION=\"\${BUILD_BRANCH}\")\n\
 add_definitions(-DREPO_REVISION=\"\${REPO_REVISION}\")\n"



# based on https://hub.docker.com/r/rpmbuild/centos6/
## install base environment
RUN yum install -y \
                   libtool libtool-ltdl \
                   make cmake \
                   pkgconfig \
                   sudo \
                   automake autoconf \
                   wget \
                   yum-utils rpm-build && \
    yum clean all
RUN wget http://people.centos.org/tru/devtools-2/devtools-2.repo -O /etc/yum.repos.d/devtools-2.repo
# git [devtools-2] (1.8.4) for "rev-list --count" feature, git [centos6] (1.7.1) too old
RUN yum install -y devtoolset-2-git
RUN ln -s /opt/rh/devtoolset-2/root/usr/bin/git /usr/bin/git
RUN yum install -y devtoolset-2-gcc devtoolset-2-binutils
RUN yum install -y devtoolset-2-gcc-c++ devtoolset-2-libstdc++-devel devtoolset-2-toolchain
#RUN yum install -y devtoolset-2
RUN scl enable devtoolset-2 bash

## add user environment
RUN useradd builder -u 1000 -m -G users,wheel && \
    echo "builder ALL=(ALL:ALL) NOPASSWD:ALL" >> /etc/sudoers && \
    echo "# macros"                      >  /home/builder/.rpmmacros && \
    echo "%_topdir    /home/builder/rpm" >> /home/builder/.rpmmacros && \
    echo "%_sourcedir %{_topdir}"        >> /home/builder/.rpmmacros && \
    echo "%_builddir  %{_topdir}"        >> /home/builder/.rpmmacros && \
    echo "%_specdir   %{_topdir}"        >> /home/builder/.rpmmacros && \
    echo "%_rpmdir    %{_topdir}"        >> /home/builder/.rpmmacros && \
    echo "%_srcrpmdir %{_topdir}"        >> /home/builder/.rpmmacros && \
    mkdir /home/builder/rpm && \
    chown -R builder /home/builder
USER builder
RUN scl enable devtoolset-2 bash
ENV FLAVOR=rpmbuild OS=centos DIST=el6 CC=/opt/rh/devtoolset-2/root/usr/bin/gcc CXX=/opt/rh/devtoolset-2/root/usr/bin/g++
CMD /srv/pkg

WORKDIR /home/builder/

# get and compile libpcre (v8)
RUN wget -np https://ftp.pcre.org/pub/pcre/pcre-${pcre_version}.tar.bz2
RUN tar xfj pcre-${pcre_version}.tar.bz2
WORKDIR /home/builder/pcre-${pcre_version}/
RUN ./configure --disable-shared -enable-static && /usr/bin/make -j
USER root
RUN /usr/bin/make install
USER builder
WORKDIR /home/builder/

# get checkit_tiff
RUN git clone ${checkit_tiff_gitrepository}
RUN rm -Rf /home/builder/checkit_tiff/build_*
# compile checkit_tiff stable
WORKDIR /home/builder/checkit_tiff
RUN git checkout ${checkit_tiff_current_version}
RUN git checkout -- src/CMakeLists.txt
# add patches to workaround outdated git
RUN cat src/CMakeLists.txt | \
sed -e "s/execute_process( COMMAND git rev-parse --abbrev-ref HEADOUTPUT_VARIABLE BUILD_BRANCH OUTPUT_STRIP_TRAILING_WHITESPACE)//g" \
 -e "s/execute_process( COMMAND git rev-parse HEAD OUTPUT_VARIABLE BUILD_REV_ID OUTPUT_STRIP_TRAILING_WHITESPACE)//g" \
 -e "s/\(add_definitions(-DVERSION=\"\)\([^\"]*\)/\1${checkit_tiff_current_version}/"  \
 -e "s/execute_process( COMMAND git describe --long --tags --dirty --always OUTPUT_VARIABLE BUILD_REV_ID_SHORT OUTPUT_STRIP_TRAILING_WHITESPACE)//g" > src/CMakeLists.txt.patched
RUN mv src/CMakeLists.txt.patched src/CMakeLists.txt
RUN cat -n src/CMakeLists.txt
RUN mkdir build_stable
WORKDIR /home/builder/checkit_tiff/build_stable
RUN cmake -DCMAKE_INSTALL_PREFIX=/tmp/checkit_tiff_stable/ \
  -DBUILD_BRANCH=${checkit_tiff_current_version} \
  -DPCRE_LIBRARY=../../pcre-${pcre_version}/.libs/libpcre.a \
  -DPCRE_INCLUDE_DIR=../../pcre-${pcre_version}/ ../src
RUN make && make install

# compile checkit_tiff development
WORKDIR /home/builder/checkit_tiff
RUN git checkout -- src/CMakeLists.txt
RUN git checkout ${checkit_tiff_upcoming_version}
RUN git checkout -- src/CMakeLists.txt
# add patch to workaround outdated git
RUN cat src/CMakeLists.txt | \
sed -e "s/execute_process( COMMAND git rev-parse --abbrev-ref HEADOUTPUT_VARIABLE BUILD_BRANCH OUTPUT_STRIP_TRAILING_WHITESPACE)//g" \
 -e "s/execute_process( COMMAND git rev-parse HEAD OUTPUT_VARIABLE BUILD_REV_ID OUTPUT_STRIP_TRAILING_WHITESPACE)//g" \
 -e "s/\(add_definitions(-DVERSION=\"\)\([^\"]*\)/\1${checkit_tiff_upcoming_version}/"  \
 -e "s/execute_process( COMMAND git describe --long --tags --dirty --always OUTPUT_VARIABLE BUILD_REV_ID_SHORT OUTPUT_STRIP_TRAILING_WHITESPACE)//g" > src/CMakeLists.txt.patched
RUN mv src/CMakeLists.txt.patched src/CMakeLists.txt
RUN cat -n src/CMakeLists.txt
RUN mkdir build_development
WORKDIR /home/builder/checkit_tiff/build_development
RUN cmake -DCMAKE_INSTALL_PREFIX=/tmp/checkit_tiff_development/ \
  -DBUILD_BRANCH=${checkit_tiff_upcoming_version} \
  -DPCRE_LIBRARY=../../pcre-${pcre_version}/.libs/libpcre.a \
  -DPCRE_INCLUDE_DIR=../../pcre-${pcre_version}/ ../src
RUN make && make install

# create tar-balls
WORKDIR /tmp/
RUN tar cfz checkit_tiff_current.tgz checkit_tiff_stable/
RUN tar cfz checkit_tiff_upcoming.tgz checkit_tiff_development/

