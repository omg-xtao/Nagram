#!/bin/bash
set -e

# NDK r23+ compatibility:
#  - GCC binutils prefixed with ${triple}- (ld, ar, nm, strip, ranlib) were
#    removed. Use the unified llvm-* tools and the clang driver as linker.
#  - The ${NDK}/toolchains/<arch>-4.9 and ${NDK}/platforms directories no
#    longer exist. Everything lives under the llvm prebuilt sysroot.
function build_one {
	echo "Building ${ARCH}..."

	SYSROOT="${LLVM_PREFIX}/sysroot"
	PLATFORM_LIB="${SYSROOT}/usr/lib/${ARCH_NAME}-linux-${BIN_MIDDLE}/${ANDROID_API}"

	# On Windows the clang/clang++ drivers are actually .cmd wrappers.
	CC_SUFFIX=""
	if [ -x "${LLVM_BIN}/${CLANG_PREFIX}-linux-${BIN_MIDDLE}${ANDROID_API}-clang.cmd" ]; then
		CC_SUFFIX=".cmd"
	fi
	TOOL_EXE=""
	if [ -x "${LLVM_BIN}/llvm-ar.exe" ]; then
		TOOL_EXE=".exe"
	fi

	export AR="${LLVM_BIN}/llvm-ar${TOOL_EXE}"
	export STRIP="${LLVM_BIN}/llvm-strip${TOOL_EXE}"
	export RANLIB="${LLVM_BIN}/llvm-ranlib${TOOL_EXE}"
	export NM="${LLVM_BIN}/llvm-nm${TOOL_EXE}"

	export CC_PREFIX="${LLVM_BIN}/${CLANG_PREFIX}-linux-${BIN_MIDDLE}${ANDROID_API}-"

	export CC="${CC_PREFIX}clang${CC_SUFFIX}"
	export CXX="${CC_PREFIX}clang++${CC_SUFFIX}"
	export AS="${CC_PREFIX}clang${CC_SUFFIX}"
	# Use the clang driver itself for linking so it pulls in the correct
	# sysroot/crt files; libvpx's check_ld only invokes ${LD} on object files.
	export LD="${CC}"
	# All ${CROSS_PREFIX}<tool> lookups (ar/nm/strip/ranlib) should resolve to
	# the llvm-* binaries.
	export CROSS_PREFIX="${LLVM_BIN}/llvm-"


	export CFLAGS="-DANDROID -fpic -fpie ${OPTIMIZE_CFLAGS}"
	export CPPFLAGS="${CFLAGS}"
	export CXXFLAGS="${CFLAGS} -std=c++11"
	export ASFLAGS="-D__ANDROID__"
	export LDFLAGS="-L${PLATFORM_LIB}"

  if [ "x86" = ${ARCH} ]; then
    patch -p1 < ../patches/libvpx_x86_fix.patch
  fi
	echo "Cleaning..."
	make clean || true

	echo "Configuring..."



	./configure \
	--extra-cflags="${OPTIMIZE_CFLAGS}" \
	--extra-cxxflags="${OPTIMIZE_CFLAGS}" \
	--libc="${LLVM_PREFIX}/sysroot" \
	--prefix=${PREFIX} \
	--target=${TARGET} \
	${CPU_DETECT} \
	--as=yasm \
	--enable-static \
	--enable-pic \
	--disable-docs \
	--enable-libyuv \
	--enable-small \
	--enable-optimizations \
	--enable-better-hw-compatibility \
	--disable-examples \
	--disable-tools \
	--disable-debug \
	--disable-neon-asm \
	--disable-neon-dotprod \
	--disable-unit-tests \
	--disable-install-docs \
	--enable-realtime-only \
	--enable-vp8 \
	--enable-vp9 \
	--disable-webm-io

	make -j$COMPILATION_PROC_COUNT install

  if [ "x86" = ${ARCH} ]; then
    patch -p1 -R < ../patches/libvpx_x86_fix.patch
  fi
}

function setCurrentPlatform {

	CURRENT_PLATFORM="$(uname -s)"
	case "${CURRENT_PLATFORM}" in
		Darwin*)
			BUILD_PLATFORM=darwin-x86_64
			COMPILATION_PROC_COUNT=`sysctl -n hw.physicalcpu`
			;;
		Linux*)
			BUILD_PLATFORM=linux-x86_64
			COMPILATION_PROC_COUNT=$(nproc)
			;;
	  MSYS*|MINGW*|CYGWIN*)
	    BUILD_PLATFORM=windows-x86_64
      COMPILATION_PROC_COUNT=$(nproc)
      ;;
		*)
			echo -e "\033[33mWarning! Unknown platform ${CURRENT_PLATFORM}! falling back to linux-x86_64\033[0m"
			BUILD_PLATFORM=linux-x86_64
			COMPILATION_PROC_COUNT=1
			;;
	esac

	echo "Build platform: ${BUILD_PLATFORM}"
	echo "Parallel jobs: ${COMPILATION_PROC_COUNT}"

}

function checkPreRequisites {

	if ! [ -d "libvpx" ] || ! [ "$(ls -A libvpx)" ]; then
		echo -e "\033[31mFailed! Submodule 'libvpx' not found!\033[0m"
		echo -e "\033[31mTry to run: 'git submodule init && git submodule update'\033[0m"
		exit
	fi

	if [ -z "$NDK" -a "$NDK" == "" ]; then
		echo -e "\033[31mFailed! NDK is empty. Run 'export NDK=[PATH_TO_NDK]'\033[0m"
		exit
	fi
}

setCurrentPlatform
checkPreRequisites

cd libvpx

## common
LLVM_PREFIX="${NDK}/toolchains/llvm/prebuilt/${BUILD_PLATFORM}"
LLVM_BIN="${LLVM_PREFIX}/bin"
VERSION="4.9"
ANDROID_API=21

function build {
	for arg in "$@"; do
		case "${arg}" in
			x86_64)
        ANDROID_API=21
				ARCH=x86_64
				ARCH_NAME=x86_64
				PREBUILT_ARCH=x86_64
				CLANG_PREFIX=x86_64
				BIN_MIDDLE=android
				CPU=x86_64
				OPTIMIZE_CFLAGS="-O3 -march=x86-64 -mtune=intel -msse4.2 -mpopcnt -m64 -fPIC"
				TARGET="x86_64-android-gcc"
				PREFIX=./build/$CPU
        CPU_DETECT="--enable-runtime-cpu-detect"
				build_one
			;;
			x86)
        ANDROID_API=21
				ARCH=x86
				ARCH_NAME=i686
				PREBUILT_ARCH=x86
				CLANG_PREFIX=i686
				BIN_MIDDLE=android
				CPU=i686
				OPTIMIZE_CFLAGS="-O3 -march=i686 -mtune=intel -msse3 -mfpmath=sse -m32 -fPIC"
				TARGET="x86-android-gcc"
				PREFIX=./build/x86
				CPU_DETECT="--enable-runtime-cpu-detect"
				build_one
			;;
			arm64)
        ANDROID_API=21
				ARCH=arm64
				ARCH_NAME=aarch64
				PREBUILT_ARCH=aarch64
				CLANG_PREFIX=aarch64
				BIN_MIDDLE=android
				CPU=arm64-v8a
				OPTIMIZE_CFLAGS="-O3 -march=armv8-a"
				TARGET="arm64-android-gcc"
				PREFIX=./build/$CPU
				CPU_DETECT="--disable-runtime-cpu-detect"
				build_one
			;;
			arm)
        ANDROID_API=21
				ARCH=arm
				ARCH_NAME=arm
				PREBUILT_ARCH=arm
				CLANG_PREFIX=armv7a
				BIN_MIDDLE=androideabi
				CPU=armeabi-v7a
				OPTIMIZE_CFLAGS="-Os -D_LIBCPP_HAS_QUICK_EXIT -O3 -march=armv7-a -mfloat-abi=softfp -mfpu=neon -mtune=cortex-a8 -mthumb -D__thumb__"
				TARGET="armv7-android-gcc --enable-neon --disable-neon-asm"
				PREFIX=./build/$CPU
				CPU_DETECT="--disable-runtime-cpu-detect"
				build_one
			;;
			*)
			;;
		esac
	done
}

if (( $# == 0 )); then
	build arm arm64
else
	build $@
fi
