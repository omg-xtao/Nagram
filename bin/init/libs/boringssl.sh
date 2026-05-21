#!/bin/bash

source "bin/init/env.sh"

export NINJA_PATH="$(command -v ninja)"

case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*)
        CMAKE_BIN_DIR="$(ls -d "$ANDROID_HOME"/cmake/*/bin 2>/dev/null | sort -V | tail -n 1)"
        if [ -n "$CMAKE_BIN_DIR" ] && command -v cygpath >/dev/null 2>&1; then
            CMAKE_BIN_DIR="$(cygpath -u "$CMAKE_BIN_DIR")"
        fi
        if [ -n "$CMAKE_BIN_DIR" ]; then
            export PATH="$CMAKE_BIN_DIR:$PATH"
        fi
        if [ -n "$NINJA_PATH" ] && command -v cygpath >/dev/null 2>&1; then
            export NINJA_PATH="$(cygpath -w "$NINJA_PATH")"
        fi
        ;;
    *)
        export PATH="$(echo "$ANDROID_HOME"/cmake/*/bin | tr ' ' ':'):$PATH"
        ;;
esac

cd TMessagesProj/jni || exit 1
git submodule update --init boringssl

cd boringssl
git reset --hard
git clean -fdx
cd ..

./patch_boringssl.sh || exit 1
./build_boringssl.sh
