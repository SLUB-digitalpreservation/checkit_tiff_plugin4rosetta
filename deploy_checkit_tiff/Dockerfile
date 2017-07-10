FROM centos:6.8
MAINTAINER Andreas Romeyke <romeyke@slub-dresden.de>
ENV checkit_tiff_current_version=v0.2.4
ENV checkit_tiff_upcoming_version=v0.3.0
ENV checkit_tiff_gitrepository=https://github.com/SLUB-digitalpreservation/checkit_tiff.git
ENV pcre_version=8.41

# based on https://hub.docker.com/r/rpmbuild/centos6/
## install base environment
RUN yum install -y \
                   libtool libtool-ltdl \
                   make cmake \
                   git \
                   pkgconfig \
                   sudo \
                   automake autoconf \
                   wget \
                   yum-utils rpm-build && \
    yum clean all
RUN wget http://people.centos.org/tru/devtools-2/devtools-2.repo -O /etc/yum.repos.d/devtools-2.repo
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
RUN mkdir build_stable
WORKDIR /home/builder/checkit_tiff/build_stable
RUN cmake -DCMAKE_INSTALL_PREFIX=/tmp/checkit_tiff_stable/ \
  -DPCRE_LIBRARY=../../pcre-${pcre_version}/.libs/libpcre.a \
  -DPCRE_INCLUDE_DIR=../../pcre-${pcre_version}/ ../src
RUN make && make install

# compile checkit_tiff development
WORKDIR /home/builder/checkit_tiff
RUN git checkout ${checkit_tiff_upcoming_version}
RUN mkdir build_development
WORKDIR /home/builder/checkit_tiff/build_development
RUN cmake -DCMAKE_INSTALL_PREFIX=/tmp/checkit_tiff_development/ \
  -DPCRE_LIBRARY=../../pcre-${pcre_version}/.libs/libpcre.a \
  -DPCRE_INCLUDE_DIR=../../pcre-${pcre_version}/ ../src
RUN make && make install

# create tar-balls
WORKDIR /tmp/
RUN tar cfz checkit_tiff_stable.tgz checkit_tiff_stable/
RUN tar cfz checkit_tiff_development.tgz checkit_tiff_development/

