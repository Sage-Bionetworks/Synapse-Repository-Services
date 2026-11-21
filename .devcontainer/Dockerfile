# Same base image as docker_build.sh, for CI parity.
FROM maven:3-amazoncorretto-21

# Base is Amazon Linux 2023. The MySQL client matches the server version
# (docker-compose.yml) so caching_sha2_password authentication works. The MySQL repo has no
# AL2023 path, so $releasever is pinned to its el9 builds.
# The which2 profile scripts are deleted because they export a multi-line bash function that
# IDE backends cannot re-import; /usr/bin/which is unaffected.
RUN dnf install -y unzip tar gzip git jq sudo shadow-utils procps-ng \
    && dnf install -y https://repo.mysql.com/mysql84-community-release-el9-2.noarch.rpm \
    && sed -i 's/$releasever/9/g' /etc/yum.repos.d/mysql-community*.repo \
    && dnf install -y mysql-community-client \
    && dnf clean all \
    && rm -f /etc/profile.d/which2.sh /etc/profile.d/which2.csh

# AWS CLI v2 (arch-aware for Apple Silicon). Credentials come from `aws sso login`.
RUN ARCH="$(uname -m)" \
    && curl -fsSL "https://awscli.amazonaws.com/awscli-exe-linux-${ARCH}.zip" -o /tmp/awscliv2.zip \
    && unzip -q /tmp/awscliv2.zip -d /tmp \
    && /tmp/aws/install \
    && rm -rf /tmp/awscliv2.zip /tmp/aws

ARG USERNAME=dev
ARG USER_UID=1000
ARG USER_GID=1000
RUN groupadd --gid $USER_GID $USERNAME \
    && useradd --uid $USER_UID --gid $USER_GID -m -s /bin/bash $USERNAME \
    && echo "$USERNAME ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/$USERNAME \
    && chmod 0440 /etc/sudoers.d/$USERNAME

# Pre-create the volume mount points so fresh named volumes inherit dev ownership.
RUN mkdir -p /home/dev/.m2/repository /home/dev/.aws \
    && chown -R dev:dev /home/dev/.m2 /home/dev/.aws

# The maven image defaults MAVEN_CONFIG to /root/.m2.
ENV MAVEN_CONFIG=/home/dev/.m2

USER dev
