#!groovy
/*
 * This is a Jenkinsfile to automate Jenkins CI builds of uillumos-gate
 * as code updates come into its repo (including posted pull requests)
 * and pass the routine with CCACHE to speed this up.
 *
 * This file and its contents are supplied under the terms of the
 * Common Development and Distribution License ("CDDL"). You may
 * only use this file in accordance with the terms of the CDDL.
 *
 * A full copy of the text of the CDDL should have accompanied this
 * source. A copy of the CDDL is also available via the Internet at
 * http://www.illumos.org/license/CDDL.
 *
 * Copyright 2016 Jim Klimov
 *
 */

pipeline {
    agent label:"illumos-gate-builder"
    /*
     * This assumes a zone or server with required illumos-gate building tools
     * For OmniOS you'd need to preinstall 'illumos-tools' per
     *   https://omnios.omniti.com/wiki.php/illumos-tools
     * On OpenIndiana it would be a large set of packages, on OI Hipster just
     * the 'build-essential' per
     *   http://wiki.illumos.org/display/illumos/How+To+Build+illumos
     * On some distros you might need to explicitly add 'gcc-4.4.4-il' and/or
     * legacy "closed binaries" as tarballs or distro-maintained packages.
     */
    parameters {
        booleanParam(defaultValue: false, description: 'Removes workspace completely before checkout and build', name: 'action_DistcleanRebuild')
        booleanParam(defaultValue: false, description: 'Wipes workspace from untracked files before checkout and build', name: 'action_GitcleanRebuild')
        booleanParam(defaultValue: true,  description: 'Run Git to checkout or update the project sources', name: 'action_DoSCM')
        booleanParam(defaultValue: true,  description: 'Recreate "illumos.sh" with settings for the next build run', name: 'action_PrepIllumos')
        booleanParam(defaultValue: true,  description: 'Run "nightly" script to update or rebuild the project (depending on other settings)', name: 'action_Build')
        booleanParam(defaultValue: false, description: 'Run "nightly" script to update the project in incremental mode', name: 'option_BuildIncremental')
        booleanParam(defaultValue: false, description: 'Run "nightly" script to "make check" the project (as a separate step, may be redundant with "m" in NIGHTLY_OPTIONS)', name: 'action_Check')
        booleanParam(defaultValue: false, description: 'Enable publishing IPS packaging to a remote repository unless earlier steps fail', name: 'action_PublishIPS')
        string(defaultValue: 'illumos-gate-buildspace', description: 'Subdirectory under Jenkins workspace where the checkout is made into and build runs (may be empty)', name: 'REL_BUILDDIR')
        string(defaultValue: '', description: 'The remote IPS repository URL to which you can publish the updated packages', name: 'URL_IPS_REPO')
        string(defaultValue: '-nCDAlmprt', description: 'The nightly.sh option flags for the illumos-gate build (gate default is -FnCDAlmprt), including the leading dash:\n* DEBUG build only (-D, -F) or DEBUG and non-DEBUG builds (just -D)\n* do not bringover (aka. pull or clone) from the parent (-n)\n* runs "make check" (-C)\n* checks for new interfaces in libraries (-A)\n* runs lint in usr/src (-l plus the LINTDIRS variable)\n* sends mail on completion (-m and the MAILTO variable)\n* creates packages for PIT/RE (-p)\n* checks for changes in ELF runpaths (-r)\n* build and use this workspaces tools in $SRC/tools (-t)', name: 'BUILDOPT_NIGHTLY_OPTIONS')
        string(defaultValue: '/opt/onbld/closed', description: 'Location where the "closed binaries" are pre-unpacked into', name: 'BUILDOPT_ON_CLOSED_BINS')
        booleanParam(defaultValue: true, description: 'Use CCACHE (if available) to wrap around the GCC compiler', name: 'option_UseCCACHE')
        string(defaultValue: '\${HOME}/.ccache', description: 'If using CCACHE across nodes, you can use a shared cache directory\nNote that if you access nodes via SSH as the same user account, this account can just have a symlink to a shared location on NFS or have wholly the same home using NFS', name: 'CCACHE_DIR')
        string(defaultValue: '/opt/gcc/4.4.4/bin:/opt/gcc/4.4.4/libexec/gcc/i386-pc-solaris2.11/4.4.4:/usr/bin', description: 'If using CCACHE across nodes, these are paths it searches for backend real compilers', name: 'CCACHE_PATH')
    }
    environment {
//        PATH="/opt/gcc/4.4.4/bin:/opt/gcc/4.4.4/libexec/gcc/i386-pc-solaris2.11/4.4.4/:/usr/lib/ccache:/usr/bin:/usr/gnu/bin:\$PATH"
//        CC="/usr/lib/ccache/gcc"
//        CXX="/usr/lib/ccache/g++"
        _ESC_CPP="/usr/lib/cpp"	// Workaround for ILLUMOS-6219, must specify Sun Studio cpp
        CCACHE_DIR="${param.CCACHE_DIR}"
        CCACHE_PATH="${param.CCACHE_PATH}"
//        CCACHE_DIR="/home/jim/.ccache"
//        CCACHE_PATH="/opt/gcc/4.4.4/bin:/opt/gcc/4.4.4/libexec/gcc/i386-pc-solaris2.11/4.4.4:/usr/bin"
//        CCACHE_LOGFILE="/dev/stderr"
    }
    jobProperties {
        buildDiscarder(logRotator(numToKeepStr:'10'))
        disableConcurrentBuilds()
    }

    stages {
        stage("WORKSPACE:DESTROY") {
/* NOTE: This definition does not work because at this point the
 * "${env.WORKSPACE}" is "null". So we must use the concatenated form
 * "${env.WORKSPACE}/${params.REL_BUILDDIR}" everywhere it matters.
 *           environment {
 *                ABS_BUILDDIR="${env.WORKSPACE}/" + params["REL_BUILDDIR"]
 *           }
 */
            when {
                if (params["action_DistcleanRebuild"] == true) {
                    if (fileExists(file: "${env.WORKSPACE}/${params.REL_BUILDDIR}/.git/config")) {
                        return true;
                    }
                }
                return false;
            }
            steps {
                echo "Removing '${env.WORKSPACE}/${params.REL_BUILDDIR}' at '${env.NODE_NAME}'"
                dir("${env.WORKSPACE}/${params.REL_BUILDDIR}") {
                    deleteDir()
                }
            }
        }
        stage("WORKSPACE:GITWIPE") {
            when {
                if (params["action_DistcleanRebuild"] == false && params["action_GitcleanRebuild"] == true) {
                    if (fileExists(file: "${env.WORKSPACE}/${params.REL_BUILDDIR}/.git/config")) {
                        return true;
                    }
                }
                return false;
            }
            steps {
                echo "Git-cleaning '${env.WORKSPACE}/${params.REL_BUILDDIR}' at '${env.NODE_NAME}'"
                dir("${env.WORKSPACE}/${params.REL_BUILDDIR}") {
                    sh 'git checkout -f'
                    sh 'git clean -d -ff -x'
                }
            }
            post {
                failure {
                    echo "ERROR: Git clean failed in '${env.WORKSPACE}/${params.REL_BUILDDIR}' at '${env.NODE_NAME}', removing workspace completely"
                    dir("${env.WORKSPACE}/${params.REL_BUILDDIR}") {
                        deleteDir()
                    }
                }
            }
        }
        stage("WORKSPACE:CHECKOUT") {
            when {
                params["action_DoSCM"] == true
            }
            steps {
                sh "mkdir -p '${env.WORKSPACE}/${params.REL_BUILDDIR}'"
                dir("${env.WORKSPACE}/${params.REL_BUILDDIR}") {
                    checkout scm
                }
            }
        }
        stage("WORKSPACE:PREPARE") {
            when {
                params["action_PrepIllumos"] == true
            }
            environment {
                str_option_UseCCACHE = params["option_UseCCACHE"] ? "true" : "false"
            }
            steps {
                dir("${env.WORKSPACE}/${params.REL_BUILDDIR}") {
/* TODO: Download closed bins from the internet by default/fallback? */
                    sh """
sed -e 's,^\\(export NIGHTLY_OPTIONS=\\).*\$,\\1"${params.BUILDOPT_NIGHTLY_OPTIONS}",' \\
    -e 's,^\\(export ON_CLOSED_BINS=\\).*\$,\\1"${params.BUILDOPT_ON_CLOSED_BINS}",' \\
    -e 's,^\\(export ENABLE_IPP_PRINTING=\\),### \\1,' \\
    -e 's,^\\(export ENABLE_SMB_PRINTING=\\),### \\1,' \\
< ./usr/src/tools/env/illumos.sh > ./illumos.sh \\
&& chmod +x illumos.sh || exit

[ -n "${env._ESC_CPP}" ] && [ -x "${env._ESC_CPP}" ] && \\
    echo 'export _ESC_CPP="${env._ESC_CPP}"' >> ./illumos.sh

if [ "${str_option_UseCCACHE}" = "true" ] && [ -x "/usr/bin/ccache" ]; then
    if [ ! -d ccache/bin ] ; then
        mkdir -p ccache/bin || exit
        for F in gcc cc g++ c++ i386-pc-solaris2.11-c++ i386-pc-solaris2.11-gcc i386-pc-solaris2.11-g++ i386-pc-solaris2.11-gcc-4.4.4 cc1 cc1obj cc1plus collect2; do
            ln -s /usr/bin/ccache "ccache/bin/\$F" || exit
        done
    fi
    echo "export GCC_ROOT='`pwd`/ccache'" >> ./illumos.sh
    echo 'export CW_GCC_DIR="\$GCC_ROOT/bin"' >> ./illumos.sh
fi
"""
                }
            }
        }
        stage("WORKSPACE:BUILD") {
            environment {
                str_option_BuildIncremental = params["option_BuildIncremental"] ? "-i" : ""
            }
            when {
                params["action_Build"] == true
                def $str_option_BuildIncremental = ""
                if (params["option_BuildIncremental"] == true) {
                    str_option_BuildIncremental = "-i"
                }
                return true;
            }
            steps {
                dir("${env.WORKSPACE}/${params.REL_BUILDDIR}") {
                    sh 'if [ ! -x ./nightly.sh ]; then cp -pf ./usr/src/tools/scripts/nightly.sh ./ && chmod +x nightly.sh || exit ; fi'
                    sh """
export CCACHE_BASEDIR="`pwd`";
echo 'STARTING ILLUMOS-GATE BUILD (prepare to wait... a lot... and in silence!)';
time ./nightly.sh \${str_option_BuildIncremental} illumos.sh;
"""
                }
            }
            post {
                always {
                    dir("${env.WORKSPACE}/${params.REL_BUILDDIR}") {
                        sh 'echo "BUILD LOG - SHORT:"; cat "`ls -1d log/log.* | tail -1`/mail_msg"'
                        // sh 'echo "BUILD LOG - LONG:";  cat "`ls -1d log/log.* | tail -1`/nightly.log"'
                    }
                }
            }
        }
        stage("WORKSPACE:CHECK") {
            when {
                params["action_Check"] == true
            }
            steps {
                dir("${env.WORKSPACE}/${params.REL_BUILDDIR}") {
                    echo "Checking the build results in '${env.WORKSPACE}/${params.REL_BUILDDIR}' at '${env.NODE_NAME}'"
                    sh '. illumos.sh ; CCACHE_BASEDIR="`pwd`" make check '
                }
            }
        }
        stage("WORKSPACE:PUBLISH_IPS") {
            when {
/* TODO: Additional/alternate conditions, like "env.BRANCH == "master" ? */
                params["action_PublishIPS"] == true
            }
            steps {
                dir("${env.WORKSPACE}/${params.REL_BUILDDIR}") {
                    echo "Publishing IPS packages from '${env.WORKSPACE}/${params.REL_BUILDDIR}' at '${env.NODE_NAME}' to '${env.URL_IPS_REPO}'"
                    echo 'TODO: No-op yet'
                }
            }
        }
    }
}
