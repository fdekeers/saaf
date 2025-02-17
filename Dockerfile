# Base image: Ubuntu 20.04
FROM ubuntu:20.04

# Install packages
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y \
    sudo \
    wget \
    git \
    libaio1 \
    libncurses5 \
    && apt clean

# Install Java
ARG JVM_DIR=/usr/lib/jvm
RUN mkdir -p ${JVM_DIR}
WORKDIR ${JVM_DIR}
ARG JAVA_INSTALLER_BIN=jdk-6u45-linux-x64.bin
COPY ${JAVA_INSTALLER_BIN} ${JVM_DIR}/${JAVA_INSTALLER_BIN}
RUN chmod +x ${JVM_DIR}/${JAVA_INSTALLER_BIN}
RUN ${JVM_DIR}/${JAVA_INSTALLER_BIN} && \
    rm ${JVM_DIR}/${JAVA_INSTALLER_BIN}

## Update environment variables
# JAVA_HOME
ENV JAVA_HOME=${JVM_DIR}/jdk1.6.0_45
RUN echo "export JAVA_HOME=${JAVA_HOME}" >> /etc/environment
# PATH
ENV PATH=${PATH}:${JAVA_HOME}/bin
RUN echo "export PATH=${PATH}:${JAVA_HOME}/bin" >> /etc/environment

# Install MySQL - DEB package
ARG MYSQL_DEB=mysql-5.5.29-debian6.0-x86_64.deb
RUN wget https://downloads.mysql.com/archives/get/p/23/file/${MYSQL_DEB}
RUN dpkg -i ${MYSQL_DEB}
ARG MYSQL_USER=mysql
RUN groupadd ${MYSQL_USER}
RUN useradd -r -g ${MYSQL_USER} ${MYSQL_USER}
RUN chown -R ${MYSQL_USER}:${MYSQL_USER} /opt/mysql
ARG MYSQL_HOME=/opt/mysql/server-5.5
ENV MYSQL_HOME=${MYSQL_HOME}
RUN echo "export MYSQL_HOME=${MYSQL_HOME}" >> /etc/environment
ENV PATH=${PATH}:${MYSQL_HOME}/bin
RUN echo "export PATH=${PATH}:${MYSQL_HOME}/bin" >> /etc/environment
RUN mkdir -p ${MYSQL_HOME}/data
RUN ${MYSQL_HOME}/scripts/mysql_install_db --user=${MYSQL_USER} --basedir=${MYSQL_HOME} --datadir=${MYSQL_HOME}/data
COPY scripts/configure-mysql.sh /tmp/configure-mysql.sh
RUN chmod +x /tmp/configure-mysql.sh
RUN /tmp/configure-mysql.sh

# Create non-root sudo user
ARG USER=ubuntu
ARG HOME=/home/${USER}
RUN useradd -d ${HOME} -s /bin/bash ${USER}
RUN echo "${USER} ALL=(ALL) NOPASSWD: ALL" >> /etc/sudoers
USER ${USER}
WORKDIR ${HOME}

# Test Java installation
RUN java -version
RUN javac -version

# Install Ant
ARG ANT_HOME=${HOME}/ant1.9
ARG ANT_TARBALL=apache-ant-1.9.16-bin.tar.gz
RUN wget https://archive.apache.org/dist/ant/binaries/apache-ant-1.9.16-bin.tar.gz
RUN tar -xzf ${ANT_TARBALL} && \
    rm ${ANT_TARBALL}
RUN mv apache-ant-1.9.16 ${ANT_HOME}
ENV ANT_HOME=${ANT_HOME}
RUN echo "export ANT_HOME=${ANT_HOME}" >> ${HOME}/.profile
ENV PATH=${PATH}:${ANT_HOME}/bin
RUN echo "export PATH=${PATH}:${ANT_HOME}/bin" >> ${HOME}/.profile
# Test Ant installation
RUN ant -version

# Set working directory
WORKDIR ${HOME}/saaf
