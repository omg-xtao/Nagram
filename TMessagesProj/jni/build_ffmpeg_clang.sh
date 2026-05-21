#!/bin/bash
set -e

# NDK r23+ compatibility:
#  - GCC binutils prefixed with ${triple}- (ld, ar, nm, strip, ranlib) were
#    removed. Use the unified llvm-* tools and the clang driver as linker.
#  - The ${NDK}/toolchains/<arch>-4.9 and ${NDK}/platforms directories no
#    longer exist. Everything lives under the llvm prebuilt sysroot.
#  - ${NDK}/prebuilt/<host>/bin/yasm was dropped in r19; fall back to PATH.
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

	AR="${LLVM_BIN}/llvm-ar${TOOL_EXE}"
	STRIP="${LLVM_BIN}/llvm-strip${TOOL_EXE}"
	NM="${LLVM_BIN}/llvm-nm${TOOL_EXE}"
	RANLIB="${LLVM_BIN}/llvm-ranlib${TOOL_EXE}"

	CC_PREFIX="${LLVM_BIN}/${CLANG_PREFIX}-linux-${BIN_MIDDLE}${ANDROID_API}-"

	CC="${CC_PREFIX}clang${CC_SUFFIX}"
	CXX="${CC_PREFIX}clang++${CC_SUFFIX}"
	# Use the clang driver as the linker; it pulls in the correct sysroot
	# and crt files. Any ${CROSS_PREFIX}<tool> lookups will resolve to llvm-*.
	LD="${CC}"
	CROSS_PREFIX="${LLVM_BIN}/llvm-"

	# Allow overriding yasm via env, otherwise pick it up from PATH.
	YASM="${YASM:-$(command -v yasm || true)}"

	INCLUDES=" -I./${LIBVPXPREFIX}/include"
	LIBS=" -L./${LIBVPXPREFIX}/lib"

	echo "Cleaning..."
	rm -f config.h
	make clean || true

	echo "Configuring..."

	./configure \
	--nm=${NM} \
	--ar=${AR} \
	--strip=${STRIP} \
	--ranlib=${RANLIB} \
	--cc=${CC} \
	--cxx=${CXX} \
	--ld="${LD}" \
	--enable-stripping \
	--arch=$ARCH \
	--target-os=linux \
	--enable-cross-compile \
	--x86asmexe="${YASM:-yasm}" \
	--prefix=$PREFIX \
	--enable-pic \
	--disable-shared \
	--enable-static \
	--enable-asm \
	--enable-inline-asm \
	--enable-x86asm \
	--cross-prefix=$CROSS_PREFIX \
	--sysroot="${SYSROOT}" \
	--extra-cflags="${INCLUDES} -Os -DCONFIG_LINUX_PERF=0 -DANDROID $OPTIMIZE_CFLAGS -fPIC" \
	--extra-cxxflags="${INCLUDES} -Os -DCONFIG_LINUX_PERF=0 -DANDROID $OPTIMIZE_CFLAGS -fPIC" \
	--extra-ldflags="${LIBS} -Wl,-Bsymbolic -Wl,-rpath-link=${PLATFORM_LIB} -L${PLATFORM_LIB} -lc -lm -ldl -fPIC" \
	\
	--enable-version3 \
	--enable-gpl \
	\
	--disable-linux-perf \
	\
	--disable-doc \
	--disable-htmlpages \
	--disable-avx \
	\
	--disable-everything \
	--disable-network \
	--disable-zlib \
	--disable-avfilter \
	--disable-avdevice \
	--disable-postproc \
	--disable-debug \
	--disable-programs \
	--disable-ffplay \
	--disable-ffprobe \
	--disable-postproc \
	\
	--enable-libvpx \
	--enable-decoder=libvpx_vp9 \
	--enable-encoder=libvpx_vp9 \
  --enable-muxer=matroska \
  --enable-bsf=vp9_superframe \
  --enable-bsf=vp9_raw_reorder \
	--enable-runtime-cpudetect \
	--enable-pthreads \
	--enable-avresample \
	--enable-swscale \
	--enable-protocol=file \
	--enable-decoder=h264 \
	--enable-decoder=h265 \
	--enable-decoder=mpeg4 \
	--enable-decoder=mjpeg \
	--enable-decoder=gif \
	--enable-decoder=alac \
	--enable-decoder=opus \
	--enable-decoder=mp3 \
	--enable-decoder=aac \
	--enable-demuxer=mov \
	--enable-demuxer=gif \
	--enable-demuxer=ogg \
	--enable-demuxer=matroska \
	--enable-demuxer=mp3 \
	--enable-demuxer=aac \
	--enable-hwaccels \
	$ADDITIONAL_CONFIGURE_FLAG

	#echo "continue?"
	#read
	make -j$COMPILATION_PROC_COUNT
	make install
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

	if ! [ -d "ffmpeg" ] || ! [ "$(ls -A ffmpeg)" ]; then
		echo -e "\033[31mFailed! Submodule 'ffmpeg' not found!\033[0m"
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

# TODO: fix env variable for NDK
# NDK=/opt/android-sdk/ndk-bundle

cd ffmpeg

## common
LLVM_PREFIX="${NDK}/toolchains/llvm/prebuilt/${BUILD_PLATFORM}"
LLVM_BIN="${LLVM_PREFIX}/bin"
PREFIX_D=$(realpath ..)
# VERSION/PREBUILT_ARCH/PREBUILT_MIDDLE are legacy variables left for
# backward compatibility; they are no longer consumed by build_one.
VERSION="4.9"

function build {
	for arg in "$@"; do
		case "${arg}" in
			x86_64)
				ANDROID_API=21

				ARCH=x86_64
				ARCH_NAME=x86_64
				PREBUILT_ARCH=x86_64
				PREBUILT_MIDDLE=
				CLANG_PREFIX=x86_64
				BIN_MIDDLE=android
				CPU=x86_64
				OPTIMIZE_CFLAGS="-march=x86-64 -msse4.2 -mpopcnt"
				PREFIX=./build/$CPU
				LIBVPXPREFIX=../libvpx/build/x86_64
				ADDITIONAL_CONFIGURE_FLAG="--disable-asm"
				build_one
			;;
			arm64)
				ANDROID_API=21

				ARCH=arm64
				ARCH_NAME=aarch64
				PREBUILT_ARCH=aarch64
				PREBUILT_MIDDLE="-linux-android"
				CLANG_PREFIX=aarch64
				BIN_MIDDLE=android
				CPU=arm64-v8a
				OPTIMIZE_CFLAGS=
				PREFIX=./build/$CPU
				LIBVPXPREFIX=../libvpx/build/arm64-v8a
				ADDITIONAL_CONFIGURE_FLAG="--enable-neon --enable-optimizations"
				build_one
			;;
			arm)
				ANDROID_API=21

				ARCH=arm
				ARCH_NAME=arm
				PREBUILT_ARCH=arm
				PREBUILT_MIDDLE="-linux-androideabi"
				CLANG_PREFIX=armv7a
				BIN_MIDDLE=androideabi
				CPU=armv7-a
				OPTIMIZE_CFLAGS="-marm -march=$CPU"
				PREFIX=./build/armeabi-v7a
				LIBVPXPREFIX=../libvpx/build/armeabi-v7a
				ADDITIONAL_CONFIGURE_FLAG="--enable-neon"
				build_one
			;;
			x86)
				ANDROID_API=21

				ARCH=x86
				ARCH_NAME=i686
				PREBUILT_ARCH=x86
				PREBUILT_MIDDLE=
				CLANG_PREFIX=i686
				BIN_MIDDLE=android
				CPU=i686
				OPTIMIZE_CFLAGS="-march=$CPU"
				PREFIX=./build/x86
				LIBVPXPREFIX=../libvpx/build/x86
				ADDITIONAL_CONFIGURE_FLAG="--disable-x86asm --disable-inline-asm --disable-asm"
				build_one
			;;
			*)
			;;
		esac
	done
}

if (( $# == 0 )); then
	build arm64 arm
else
	build $@
fi
