# Copyright 2020-2023 NXP

DESCRIPTION = "i.MX Verisilicon Software ISP"
LICENSE = "Proprietary"
LIC_FILES_CHKSUM = "file://COPYING;md5=63a38e9f392d8813d6f1f4d0d6fbe657"
DEPENDS = "boost libdrm virtual/libg2d libtinyxml2"

SRC_URI = " \
    ${FSL_MIRROR}/${BP}.bin;fsl-eula=true \
    file://0001-cpp-netlib-parsers.ipp-add-missing-include.patch \
"

SRC_URI[md5sum] = "693e76b20985de607208c21d996019f8"
SRC_URI[sha256sum] = "c2f450502390442926920d6457bbf24378c0338a445be180130ec6b1b12d1056"

inherit fsl-eula-unpack cmake systemd use-imx-headers

# Build the sub-folder appshell
OECMAKE_SOURCEPATH = "${S}/appshell"

# Use make instead of ninja
OECMAKE_GENERATOR = "Unix Makefiles"

# Workaround for linking issues seen with gold linker
LDFLAGS:append = "${@bb.utils.contains('DISTRO_FEATURES', 'ld-is-gold', ' -fuse-ld=bfd ', '', d)}"

SYSTEMD_SERVICE:${PN} = "imx8-isp.service"

EXTRA_OECMAKE += " \
    -DSDKTARGETSYSROOT=${STAGING_DIR_HOST} \
    -DCMAKE_BUILD_TYPE=release \
    -DCMAKE_BUILD_WITH_INSTALL_RPATH=ON \
    -DISP_VERSION=ISP8000NANO_V1802 \
    -DPLATFORM=ARM64 \
    -DAPPMODE=V4L2 \
    -DTUNINGEXT=1 \
    -DQTLESS=1 \
    -DFULL_SRC_COMPILE=1 \
    -DWITH_DRM=1 \
    -DWITH_DWE=1 \
    -DSERVER_LESS=1 \
    -DSUBDEV_V4L2=1 \
    -DENABLE_IRQ=1 \
    -DPARTITION_BUILD=0 \
    -D3A_SRC_BUILD=0 \
    -DIMX_G2D=ON \
    -Wno-dev \
"

do_install() {
    install -d ${D}/${libdir}
    install -d ${D}/${includedir}
    install -d ${D}/opt/imx8-isp/bin
    install -d ${D}/opt/imx8-isp/bin/dewarp_config

    cp -r ${B}/generated/release/bin/*_test ${D}/opt/imx8-isp/bin
    cp -r ${B}/generated/release/bin/*.xml ${D}/opt/imx8-isp/bin
    cp -r ${B}/generated/release/bin/*.drv ${D}/opt/imx8-isp/bin
    cp -r ${B}/generated/release/bin/tuningext ${D}/opt/imx8-isp/bin
    cp -r ${B}/generated/release/bin/isp_media_server ${D}/opt/imx8-isp/bin
    cp -r ${B}/generated/release/bin/vvext ${D}/opt/imx8-isp/bin

    cp -r ${B}/generated/release/lib/*.so* ${D}/${libdir}
    cp -r ${B}/generated/release/include/* ${D}/${includedir}

    cp -r ${S}/dewarp/dewarp_config/ ${D}/opt/imx8-isp/bin
    cp ${S}/imx/run.sh ${D}/opt/imx8-isp/bin
    cp ${S}/imx/start_isp.sh ${D}/opt/imx8-isp/bin

    chmod +x ${D}/opt/imx8-isp/bin/run.sh
    chmod +x ${D}/opt/imx8-isp/bin/start_isp.sh

    if ${@bb.utils.contains('DISTRO_FEATURES','systemd','true','false',d)}; then
      install -d ${D}${systemd_system_unitdir}
      install -m 0644 ${S}/imx/imx8-isp.service ${D}${systemd_system_unitdir}
    fi
}

# The build contains a mix of versioned and unversioned libraries, so
# the default packaging configuration needs some modification so that
# unversioned .so libraries go to the main package and versioned .so
# symlinks go to -dev.
FILES_SOLIBSDEV = ""
FILES:${PN} += "/opt ${libdir}/lib*${SOLIBSDEV}"
FILES:${PN}-dev += "${FILES_SOLIBS_VERSIONED}"
FILES_SOLIBS_VERSIONED = " \
    ${libdir}/libcppnetlib-client-connections.so \
    ${libdir}/libcppnetlib-server-parsers.so \
    ${libdir}/libcppnetlib-uri.so \
    ${libdir}/libjsoncpp.so \
    ${libdir}/libos08a20.so \
"

RDEPENDS:${PN} = "libdrm"

COMPATIBLE_MACHINE = "(mx8mp-nxp-bsp)"
