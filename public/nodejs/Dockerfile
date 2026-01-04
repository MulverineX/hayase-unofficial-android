FROM saschpe/android-ndk:35-jdk23.0.2_7-ndk28.0.13004108-cmake3.31.5

USER root

# Use libnode headers bundled with capacitor-nodejs (mounted at runtime via -v)
ENV LIBNODE_PATH=/app/node_modules/capacitor-nodejs/android/libnode

# Install Node.js for running npm
ARG NODE_MAJOR=24

RUN apt-get update
RUN apt-get install -y ca-certificates curl gnupg make
RUN mkdir -p /etc/apt/keyrings
RUN curl -fsSL https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key | gpg --dearmor -o /etc/apt/keyrings/nodesource.gpg
RUN echo "deb [signed-by=/etc/apt/keyrings/nodesource.gpg] https://deb.nodesource.com/node_$NODE_MAJOR.x nodistro main" | tee /etc/apt/sources.list.d/nodesource.list
RUN apt-get update
RUN apt-get install nodejs -y

WORKDIR /app