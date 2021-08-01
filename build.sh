#!/usr/bin/env bash

pushd "$(dirname "$BASH_SOURCE")"

# Build and install the required Spigot dependencies:
./scripts/installSpigotDependencies.sh

# We require Java 16 to build:
source scripts/installJDK.sh 16

# Build via Gradle:
./gradlew cleanInstall

popd
