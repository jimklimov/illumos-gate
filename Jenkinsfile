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
        booleanParam(defaultValue: false, description: 'Run "nightly" script to update the project in incremental mode (overrides and disables clobber, lint, check)', name: 'option_BuildIncremental')
        string(defaultValue: '-nCDAlmprt', description: 'The nightly.sh option flags for the illumos-gate build (gate default is -FnCDAlmprt), including the leading dash.\nNon-DEBUG is the default build type. Recognized flags include:\n*    -A  check for ABI differences in .so files\n*    -C  check for cstyle/hdrchk errors\n*    -D  do a build with DEBUG on\n*    -F  do _not_ do a non-DEBUG build\n*    -G  gate keeper default group of options (-au)\n*    -I  integration engineer default group of options (-ampu)\n*    -M  do not run pmodes (safe file permission checker)\n*    -N  do not run protocmp\n*    -R  default group of options for building a release (-mp)\n*    -U  update proto area in the parent\n*    -f  find unreferenced files\n*    -i  do an incremental build (no "make clobber")\n*    -l  do "make lint" in \$LINTDIRS (default: \$SRC y)\n*    -m  send mail to \$MAILTO at end of build\n*    -n  do not do a bringover (aka. pull or clone) from the parent\n*    -p  create packages for PIT/RE\n*    -r  check ELF runtime attributes in the proto area\n*    -t  build and use the tools in \$SRC/tools (default setting)\n*    +t  Use the build tools in \$ONBLD_TOOLS/bin\n*    -u  update proto_list_\$MACH and friends in the parent workspace; when used with -f, also build an unrefmaster.out in the parent\n*    -w  report on differences between previous and current proto areas', name: 'BUILDOPT_NIGHTLY_OPTIONS')
        booleanParam(defaultValue: false, description: 'Run "nightly" script to "make check" the project (as a separate step, may be redundant with "m" in NIGHTLY_OPTIONS)', name: 'action_Check')
        string(defaultValue: '-nCAFifr', description: 'The alternate nightly.sh option flags for the illumos-gate post-build checks (if action_Check is selected)', name: 'BUILDOPT_NIGHTLY_OPTIONS_CHECK')
        booleanParam(defaultValue: false, description: 'Enable publishing IPS packaging to a remote repository unless earlier steps fail', name: 'action_PublishIPS')
        string(defaultValue: '', description: 'The remote IPS repository URL to which you can publish the updated packages', name: 'URL_IPS_REPO')
        string(defaultValue: '/opt/onbld/closed', description: 'Location where the "closed binaries" are pre-unpacked into', name: 'BUILDOPT_ON_CLOSED_BINS')
        string(defaultValue: '5.22', description: 'Installed PERL version to use for the build (5.10, 5.16, 5.22, etc)', name: 'BUILDOPT_PERL_VERSION')
        booleanParam(defaultValue: true, description: 'Use CCACHE (if available) to wrap around the GCC compiler', name: 'option_UseCCACHE')
        string(defaultValue: '\${HOME}/.ccache', description: 'If using CCACHE across nodes, you can use a shared cache directory\nNote that if you access nodes via SSH as the same user account, this account can just have a symlink to a shared location on NFS or have wholly the same home using NFS', name: 'CCACHE_DIR')
        string(defaultValue: '/opt/gcc/4.4.4/bin:/opt/gcc/4.4.4/libexec/gcc/i386-pc-solaris2.11/4.4.4:/usr/bin', description: 'If using CCACHE across nodes, these are paths it searches for backend real compilers', name: 'CCACHE_PATH')
    }
    environment {
        _ESC_CPP="/usr/lib/cpp"	// Workaround for ILLUMOS-6219, must specify Sun Studio cpp
        CCACHE_DIR="${params.CCACHE_DIR}"
        CCACHE_PATH="${params.CCACHE_PATH}"
    }
    jobProperties {
        buildDiscarder(logRotator(numToKeepStr:'10'))
        disableConcurrentBuilds()
    }

    stages {
        stage("WORKSPACE:DESTROY") {
            when {
                if (params["action_DistcleanRebuild"] == true) {
                    if (fileExists(file: "${env.WORKSPACE}/.git/config")) {
                        return true;
                    }
                }
                return false;
            }
            steps {
                echo "Removing '${env.WORKSPACE}' at '${env.NODE_NAME}'"
                dir("${env.WORKSPACE}") {
                    deleteDir()
                }
            }
        }
        stage("WORKSPACE:GITWIPE") {
            when {
                if (params["action_DistcleanRebuild"] == false && params["action_GitcleanRebuild"] == true) {
                    if (fileExists(file: "${env.WORKSPACE}/.git/config")) {
                        return true;
                    }
                }
                return false;
            }
            steps {
                dir("${env.WORKSPACE}") {
                    echo "Git-cleaning '${env.WORKSPACE}' at '${env.NODE_NAME}'"
                    sh 'git checkout -f'
                    sh 'git clean -d -ff -x'
                }
            }
            post {
                failure {
                    echo "ERROR: Git clean failed in '${env.WORKSPACE}' at '${env.NODE_NAME}', removing workspace completely"
                    dir("${env.WORKSPACE}") {
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
                sh "mkdir -p '${env.WORKSPACE}'"
                checkout scm
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
/* TODO: Download closed bins from the internet by default/fallback? */
                dir("${env.WORKSPACE}") {
                    sh """
sed -e 's,^\\(export NIGHTLY_OPTIONS=\\).*\$,\\1"${params.BUILDOPT_NIGHTLY_OPTIONS}",' \\
    -e 's,^\\(export ON_CLOSED_BINS=\\).*\$,\\1"${params.BUILDOPT_ON_CLOSED_BINS}",' \\
< ./usr/src/tools/env/illumos.sh > ./illumos.sh \\
&& chmod +x illumos.sh || exit

grep -i omnios /etc/release && \\
sed \\
    -e 's,^\\(export ENABLE_IPP_PRINTING=\\),### \\1,' \\
    -e 's,^\\(export ENABLE_SMB_PRINTING=\\),### \\1,' \\
    -i illumos.sh

[ -n "${env._ESC_CPP}" ] && [ -x "${env._ESC_CPP}" ] && \\
    { echo 'export _ESC_CPP="${env._ESC_CPP}"' >> ./illumos.sh || exit ; }

if [ -n "${params.BUILDOPT_PERL_VERSION}" ]; then \\
    [ -d "/usr/perl5/${params.BUILDOPT_PERL_VERSION}/bin" ] || echo "WARNING: Can not find a PERL home at /usr/perl5/${params.BUILDOPT_PERL_VERSION}/bin; will try this version as asked anyways, but the build can fail" >&2
    echo "export PERL_VERSION='${params.BUILDOPT_PERL_VERSION}'" >> ./illumos.sh || exit
    echo "export PERL_PKGVERS='-`echo "${params.BUILDOPT_PERL_VERSION}" | sed s,-,,`'" >> ./illumos.sh || exit
fi

if [ "${str_option_UseCCACHE}" = "true" ] && [ -x "/usr/bin/ccache" ]; then
    if [ ! -d ccache/bin ] ; then
        mkdir -p ccache/bin || exit
        for F in gcc cc g++ c++ i386-pc-solaris2.11-c++ i386-pc-solaris2.11-gcc i386-pc-solaris2.11-g++ i386-pc-solaris2.11-gcc-4.4.4 cc1 cc1obj cc1plus collect2; do
            ln -s /usr/bin/ccache "ccache/bin/\$F" || exit
        done
    fi
    echo "export GCC_ROOT='`pwd`/ccache'" >> ./illumos.sh || exit
    echo 'export CW_GCC_DIR="\$GCC_ROOT/bin"' >> ./illumos.sh || exit
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
            }
            steps {
                dir("${env.WORKSPACE}") {
                    sh 'if [ ! -x ./nightly.sh ]; then cp -pf ./usr/src/tools/scripts/nightly.sh ./ && chmod +x nightly.sh || exit ; fi'
                    sh '[ -x ./illumos.sh ] && [ -x ./nightly.sh ] && [ -s ./nightly.sh ] && [ -s ./illumos.sh ]'
                    sh """
echo 'STARTING ILLUMOS-GATE BUILD (prepare to wait... a lot... and in silence!)';
CCACHE_BASEDIR="`pwd`" \\
time ./nightly.sh \${str_option_BuildIncremental} illumos.sh; RES=\$?;
[ "\$RES" = 0 ] || echo "BUILD FAILED (code \$RES), see more details in its logs";
exit \$RES;
"""
                }
            }
            post {
                always {
                    dir("${env.WORKSPACE}") {
                        sh 'echo "BUILD LOG - SHORT:"; cat "`ls -1d log/log.*/ | sort -n | tail -1`/mail_msg"'
                        sh 'echo "ARCHIVE BUILD LOG REPORT:"; find "`ls -1d log/log.*/ | sort -n | tail -1`" -type f > logs_to_archive.txt && cat logs_to_archive.txt'
                        script {
                            def fileToArchive = readFile 'logs_to_archive.txt'
                            archive fileToArchive
                            sh 'rm -f logs_to_archive.txt'
                        }
                    } /* TODO: Just archive this, at least the big log? */
                }
            }
        }
        stage("WORKSPACE:CHECK") {
            when {
                params["action_Check"] == true
            }
            steps {
                dir("${env.WORKSPACE}") {
                    echo "Checking the build results in '${env.WORKSPACE}' at '${env.NODE_NAME}'"
                    /* Note: Can this require a specific "make" dialect interpreter? */
                    sh 'if [ ! -x ./nightly.sh ]; then cp -pf ./usr/src/tools/scripts/nightly.sh ./ && chmod +x nightly.sh || exit ; fi'
                    sh '[ -x ./illumos.sh ] && [ -x ./nightly.sh ] && [ -s ./nightly.sh ] && [ -s ./illumos.sh ]'
                    sh """
cp ./illumos.sh ./illumos-once.sh && chmod +x ./illumos-once.sh || exit;
echo 'export NIGHTLY_OPTIONS="${params.BUILDOPT_NIGHTLY_OPTIONS_CHECK}"' >> ./illumos-once.sh || exit;
echo 'STARTING ILLUMOS-GATE CHECK (prepare to wait... a lot... and in silence!)';
CCACHE_BASEDIR="`pwd`" \\
time ./nightly.sh illumos-once.sh; RES=\$?;
[ "\$RES" = 0 ] || echo "CHECK FAILED (code \$RES), see more details in its logs";
rm -f illumos-once.sh;
exit \$RES;
"""
                }
            }
            post {
                always {
                    dir("${env.WORKSPACE}") {
                        sh 'echo "BUILD LOG - SHORT:"; cat "`ls -1d log/log.*/ | sort -n | tail -1`/mail_msg"'
                        sh 'echo "ARCHIVE BUILD LOG REPORT:"; find "`ls -1d log/log.*/ | sort -n | tail -1`" -type f > logs_to_archive.txt && cat logs_to_archive.txt'
                        script {
                            def fileToArchive = readFile 'logs_to_archive.txt'
                            archive fileToArchive
                            sh 'rm -f logs_to_archive.txt'
                        }
                    } /* TODO: Just archive this, at least the big log? */
                }
            }
        }
        stage("WORKSPACE:PUBLISH_IPS") {
            when {
/* TODO: Additional/alternate conditions, like "env.BRANCH == "master" ? */
                params["action_PublishIPS"] == true
            }
            steps {
                dir("${env.WORKSPACE}") {
                    echo "Publishing IPS packages from '${env.WORKSPACE}' at '${env.NODE_NAME}' to '${env.URL_IPS_REPO}'"
                    echo 'TODO: No-op yet'
                }
            }
        }
    }
}
