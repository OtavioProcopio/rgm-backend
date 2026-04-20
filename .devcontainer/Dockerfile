FROM eclipse-temurin:21-jdk-jammy

RUN apt-get update && \
    apt-get install -y --no-install-recommends \
    curl \
    make \
    jq \
    git \
    vim \
    sudo \
    unzip && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# cria usuário vscode (se vai usar remoteUser=vscode)
RUN useradd -ms /bin/bash vscode && \
    echo "vscode ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers

USER vscode

ENV SHELL=/bin/bash

WORKDIR /workspace