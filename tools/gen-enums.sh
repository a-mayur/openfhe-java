#!/usr/bin/env bash
# regenerate the enum classes, needs OPENFHE_HOME
set -euo pipefail
cd "$(dirname "$0")/.."
OPENFHE_HOME=${OPENFHE_HOME:-/usr/local}
I=$OPENFHE_HOME/include/openfhe
mkdir -p build
g++ -std=c++17 -fopenmp -DMATHBACKEND=4 -I"$I" -I"$I/core" -I"$I/pke" -I"$I/binfhe" -I"$I/cereal" tools/GenEnums.cpp \
    -L"$OPENFHE_HOME/lib" -Wl,-rpath,"$OPENFHE_HOME/lib" -lOPENFHEpke -lOPENFHEbinfhe -lOPENFHEcore -o build/gen-enums
build/gen-enums src/main/java/io/github/amayur/openfhe
