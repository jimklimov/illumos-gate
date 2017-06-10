#!groovy
/*
 * This is a Jenkinsfile to automate Jenkins CI builds of illumos-gate
 * as code updates come into its repo (including posted pull requests)
 * and pass the routine with CCACHE to speed up the compilation steps (D/ND).
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
    agent {
        label "illumos-gate-builder"
    }

    /*
     * This assumes a zone or server with required illumos-gate building tools
     * For OmniOS you'd need to preinstall 'illumos-tools' per
     *   https://omnios.omniti.com/wiki.php/illumos-tools
     * NOTE: Probably you'll need to add a 'text/locale' package too, which may
     * be not covered at least by older LTS release collection of these tools.
     * On OpenIndiana it would be a large set of packages, on OI Hipster just
     * the 'build-essential' per
     *   http://wiki.illumos.org/display/illumos/How+To+Build+illumos
     * On some distros you might need to explicitly add 'gcc-4.4.4-il' and/or
     * legacy "closed binaries" as tarballs or distro-maintained packages.
     * Also pay attention to exact path of GCC you want (dashes, slashes, etc).
     */
    parameters {
        booleanParam(defaultValue: false, description: 'Removes workspace completely before checkout and build', name: 'action_DistcleanRebuild')
        booleanParam(defaultValue: false, description: 'Wipes workspace from untracked files before checkout and build', name: 'action_GitcleanRebuild')
        booleanParam(defaultValue: true,  description: 'Run Git to checkout or update the project sources', name: 'action_DoSCM')
        booleanParam(defaultValue: true,  description: 'Recreate "illumos.sh" with settings for the next build run', name: 'action_PrepIllumos')
        booleanParam(defaultValue: false,  description: 'Recreate "illumos.sh" with settings from vendor-buildtools.git repo (used verbatim, (almost) no further customizations - in particular, no custom option flags unless BUILDOPT_NIGHTLY_OPTIONS_VENDOR_TOO==true)', name: 'action_PrepIllumosVendor')
        string(defaultValue: 'git@bitbucket.org:projectorg/vendor-buildtools.git', description: 'Repo URL with vendor build config', name: 'URL_REPO_VENDOR_BUILDTOOLS')
        string(defaultValue: 'data/vendor-illumos-gate.env', description: 'Relative path to customized "illumos.sh" in the repo with vendor build config', name: 'RELPATH_REPO_VENDOR_BUILDTOOLS__ILLUMOS_SH')
        string(defaultValue: 'vendor-jenkins', description: 'Name of the credential for Git checkout of vendor build tools and data', name: 'CREDENTIAL_REPO_VENDOR_BUILDTOOLS')
        booleanParam(defaultValue: true,  description: 'Run "nightly" script to update or rebuild the project in one big step (depending on other settings)', name: 'action_BuildAll')
        booleanParam(defaultValue: false, description: 'Run "nightly" script to update the project in incremental mode (overrides and disables clobber, lint, check)\nNOTE that an increment that has nothing to do can still take an hour to walk the Makefiles!', name: 'option_BuildIncremental')
        string(defaultValue: '-ntCDAlmprf', description: 'The nightly.sh option flags for the illumos-gate build (gate default is -FnCDAlmprt), including the leading dash.\nNon-DEBUG is the default build type. Recognized flags include:\n*    -A  check for ABI differences in .so files\n*    -C  check for cstyle/hdrchk errors\n*    -D  do a build with DEBUG on\n*    -F  do _not_ do a non-DEBUG build\n*    -G  gate keeper default group of options (-au)\n*    -I  integration engineer default group of options (-ampu)\n*    -M  do not run pmodes (safe file permission checker)\n*    -N  do not run protocmp\n*    -R  default group of options for building a release (-mp)\n*    -U  update proto area in the parent\n*    -f  find unreferenced files (requires -lp, conflicts with incremental)\n*    -i  do an incremental build (no "make clobber")\n*    -l  do "make lint" in \$LINTDIRS (default: "\$SRC y")\n*    -m  send mail to \$MAILTO at end of build\n*    -n  do not do a bringover (aka. pull or clone) from the parent\n*    -p  create packages for PIT/RE\n*    -r  check ELF runtime attributes in the proto area\n*    -t  build and use the tools in \$SRC/tools (default setting)\n*    +t  Use the build tools in \$ONBLD_TOOLS/bin\n*    -u  update proto_list_\$MACH and friends in the parent workspace; when used with -f, also build an unrefmaster.out in the parent\n*    -w  report on differences between previous and current proto areas', name: 'BUILDOPT_NIGHTLY_OPTIONS')
        booleanParam(defaultValue: false, description: 'Do apply BUILDOPT_NIGHTLY_OPTIONS for vendor builds', name: 'BUILDOPT_NIGHTLY_OPTIONS_VENDOR_TOO')
        booleanParam(defaultValue: false, description: 'Run "nightly" script to only produce non-debug binaries of the project', name: 'action_BuildNonDebug')
        string(defaultValue: '-nt', description: 'The alternate nightly.sh option flags for the illumos-gate to produce debug binaries of the project (if selected)', name: 'BUILDOPT_NIGHTLY_OPTIONS_BLDNONDEBUG')
        booleanParam(defaultValue: false, description: 'Run "nightly" script to produce debug binaries of the project', name: 'action_BuildDebug')
        string(defaultValue: '-ntFD', description: 'The alternate nightly.sh option flags for the illumos-gate to produce non-debug binaries of the project (if selected)', name: 'BUILDOPT_NIGHTLY_OPTIONS_BLDDEBUG')
        booleanParam(defaultValue: false, description: 'Run "nightly" script to produce packages of the project (at least one variant should have been built beforehand)', name: 'action_BuildPackages')
        string(defaultValue: '-np', description: 'The alternate nightly.sh option flags for the illumos-gate to produce a local-FS repo with packages of the project (if selected) from previously built binaries', name: 'BUILDOPT_NIGHTLY_OPTIONS_BLDPKG')
        booleanParam(defaultValue: false, description: 'Run "nightly" script to only "make check" the project (and do some related activities)', name: 'action_Check')
        string(defaultValue: '-nCAFir', description: 'The alternate nightly.sh option flags for the illumos-gate post-build checks (if selected)', name: 'BUILDOPT_NIGHTLY_OPTIONS_CHECK')
        booleanParam(defaultValue: false, description: 'Run "nightly" script to only "lint" the project (and do some related activities)', name: 'action_Lint')
        string(defaultValue: '-nl', description: 'The alternate nightly.sh option flags for the illumos-gate post-build linting (if selected)', name: 'BUILDOPT_NIGHTLY_OPTIONS_LINT')
        booleanParam(defaultValue: true, description: 'Enable publishing of local IPS packaging to a remote repository unless earlier steps fail', name: 'action_PublishIPS')
        string(defaultValue: '', description: 'The remote IPS repository URL to which you can publish the updated packages (if empty - then decided by branch name)', name: 'URL_IPS_REPO')
/* TODO: Add a sort of build to just update specifed component(s) like a driver module */
        string(defaultValue: '/opt/onbld/closed', description: 'Location where the "closed binaries" are pre-unpacked into', name: 'BUILDOPT_ON_CLOSED_BINS')
        string(defaultValue: '5.22', description: 'Installed PERL version to use for the build (5.10, 5.16, 5.16.1, 5.22, etc)', name: 'BUILDOPT_PERL_VERSION')
        booleanParam(defaultValue: true, description: 'Use JAVA8 to build illumos-gate (commits after Mar 2017)', name: 'option_UseJAVA8')
        booleanParam(defaultValue: true, description: 'Use CCACHE (if available) to wrap around the GCC compiler', name: 'option_UseCCACHE')
        string(defaultValue: '\${HOME}/.ccache', description: 'If using CCACHE across nodes, you can use a shared cache directory\nNote that if you access nodes via SSH as the same user account, this account can just have a symlink to a shared location on NFS or have wholly the same home using NFS', name: 'CCACHE_DIR')
        string(defaultValue: '/opt/gcc/4.4.4/bin:/opt/gcc/4.4.4/libexec/gcc/i386-pc-solaris2.11/4.4.4:/usr/bin', description: 'If using CCACHE across nodes, these are paths it searches for backend real compilers', name: 'CCACHE_PATH')
    }
    environment {
        _ESC_CPP="/usr/lib/cpp"	// Workaround for ILLUMOS-6219, must specify Sun Studio cpp
        /* Note: according to https://issues.jenkins-ci.org/browse/JENKINS-35230
         * the BRANCH_NAME should be used, with caveat that for PRs it may take
         * the value of PR-1234 rather than Git branch name. It sort of makes
         * sense however to do branch-related activities just for (re-)builds
         * based on an actually merged well-known branch name.
         */
        // BRANCH="${env.GIT_BRANCH}"
        BRANCH="${env.BRANCH_NAME}"
        CCACHE_DIR="${params.CCACHE_DIR}"
        CCACHE_PATH="${params.CCACHE_PATH}"
    }
    options {
        buildDiscarder(logRotator(numToKeepStr:'10'))
        disableConcurrentBuilds()
    }

    stages {
        stage("WORKSPACE:DESTROY") {
            when {
                expression {
                    if (params["action_DistcleanRebuild"] == true) {
                        if (fileExists(file: "${env.WORKSPACE}/.git/config")) {
                            return true;
                        }
                    }
                    return false;
                }
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
                expression {
                    if (params["action_DistcleanRebuild"] == false && params["action_GitcleanRebuild"] == true) {
                        if (fileExists(file: "${env.WORKSPACE}/.git/config")) {
                            return true;
                        }
                    }
                    return false;
                }
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
                expression {
                    return params["action_DoSCM"] == true
                }
            }
            steps {
                echo "WORKSPACE:CHECKOUT"
                sh "mkdir -p '${env.WORKSPACE}'"
                checkout scm
            }
        }

        stage("WORKSPACE:PREPARE-ILLUMOS") {
            when {
                expression {
                    return params["action_PrepIllumos"] == true
                }
            }
            steps {
/* TODO: Download closed bins from the internet by default/fallback? */
                dir("${env.WORKSPACE}") {
                    script {
                        def str_option_UseCCACHE = params["option_UseCCACHE"] ? "true" : "false";
                        env["str_option_UseCCACHE"] = str_option_UseCCACHE;
                        def str_option_UseJAVA8 = params["option_UseJAVA8"] ? "true" : "false";
                        env["str_option_UseJAVA8"] = str_option_UseJAVA8;
                    }
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

sed -e 's,^\\(export CODEMGR_WS=\\).*\$,\\1"${env.WORKSPACE}",' \\
    -i illumos.sh || exit

sed -e 's,^\\(export PKGARCHIVE=\\).*\$,export PKGARCHIVE="\${CODEMGR_WS}/packages/\${MACH}/nightly",' \\
    -i illumos.sh || exit

if [ -n "${params.BUILDOPT_PERL_VERSION}" ]; then
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

if [ "${str_option_UseJAVA8}" = "true" ] ; then
    echo "export BLD_JAVA_8=" >> ./illumos.sh || exit
fi
"""
                }
            }
        }

        stage("WORKSPACE:PREPARE-VENDOR") {
            when {
                expression {
                    return params["action_PrepIllumosVendor"] == true
                }
            }
            steps {
/* TODO: Download closed bins from the internet by default/fallback? */
                dir("${env.WORKSPACE}") {
                    dir("vendor-buildtools.git") {
                        checkout([$class: 'GitSCM',
                            branches: [[name: '*/master']],
                            doGenerateSubmoduleConfigurations: false,
                            extensions: [[$class: 'CleanCheckout']],
                            submoduleCfg: [],
                            userRemoteConfigs: [[credentialsId: "${params.CREDENTIAL_REPO_VENDOR_BUILDTOOLS}", url: "${params.URL_REPO_VENDOR_BUILDTOOLS}"]]
                        ])
                    }
/* Here we bolt the PKGARCHIVE to path used below in publishing */
                    script{
                        def str_BUILDOPT_NIGHTLY_OPTIONS_VENDOR_TOO = "false";
                        if (params["BUILDOPT_NIGHTLY_OPTIONS_VENDOR_TOO"]) str_BUILDOPT_NIGHTLY_OPTIONS_VENDOR_TOO = "true";
                        env["str_BUILDOPT_NIGHTLY_OPTIONS_VENDOR_TOO"] = str_BUILDOPT_NIGHTLY_OPTIONS_VENDOR_TOO;
                    }
                    sh """
cat ./`basename ${params.CREDENTIAL_REPO_VENDOR_BUILDTOOLS}`/${params.RELPATH_REPO_VENDOR_BUILDTOOLS__ILLUMOS_SH} \
> ./illumos.sh && chmod +x illumos.sh || exit

sed -e 's,^\\(export CODEMGR_WS=\\).*\$,\\1"${env.WORKSPACE}",' \\
    -i illumos.sh || exit

sed -e 's,^\\(export PKGARCHIVE=\\).*\$,export PKGARCHIVE="\${CODEMGR_WS}/packages/\${MACH}/nightly-core",' \\
    -i illumos.sh || exit

if [ "\${str_BUILDOPT_NIGHTLY_OPTIONS_VENDOR_TOO}" = true ]; then
    sed -e 's,^\\(export NIGHTLY_OPTIONS=\\).*\$,\\1"${params.BUILDOPT_NIGHTLY_OPTIONS}",' \\
        -i illumos.sh || exit
fi
"""
                }
            }
        }

        stage("WORKSPACE:BUILD-ALL-FULL") {
            environment {
                str_nametag = "build-all-full";
                str_option_BuildIncremental = "";
            }
            when {
                expression {
                    return (params["action_BuildAll"] == true && params["option_BuildIncremental"] == false)
                }
            }
            steps {
                dir("${env.WORKSPACE}") {
                    sh 'if [ ! -x ./nightly.sh ]; then cp -pf ./usr/src/tools/scripts/nightly.sh ./ && chmod +x nightly.sh || exit ; fi'
                    sh '[ -x ./illumos.sh ] && [ -x ./nightly.sh ] && [ -s ./nightly.sh ] && [ -s ./illumos.sh ]'
                    sh """
echo '`date -u`: Envvars :';
set ;
echo '`date -u`: STARTING ILLUMOS-GATE BUILD-ALL (prepare to wait... a lot... and in silence!)';
egrep '[^#]*export NIGHTLY_OPTIONS=' illumos.sh;
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
                        sh """
echo "BUILD LOG - SHORT [${env.str_nametag}]:"; cat "`ls -1d log/log.*/ | sort -n | tail -1`/mail_msg" || echo "No logs found, was build skipped?"
echo "ARCHIVE BUILD LOG REPORT [${env.str_nametag}]:"; ( echo "log/nightly.log" && find "`ls -1d log/log.*/ | sort -n | tail -1`" -type f ) > logs_to_archive-${env.str_nametag}.txt || echo "No logs found, was build skipped?"
tr '\\n' ',' < logs_to_archive-${env.str_nametag}.txt > logs_to_archive-${env.str_nametag}.csv && cat logs_to_archive-${env.str_nametag}.csv
"""
                        script {
                            def fileToArchive = readFile "logs_to_archive-${env.str_nametag}.csv"
                            archiveArtifacts allowEmptyArchive: true, artifacts: "${fileToArchive}", fingerprint: true, name:"archiveLogs-${env.str_nametag}"
                        }
                    }
                }
            }
        }

        stage("WORKSPACE:BUILD-ALL-INCREMENTAL") {
            environment {
                str_nametag = "build-all-incremental";
                str_option_BuildIncremental = "-i";
            }
            when {
                expression {
                    return (params["action_BuildAll"] == true && params["option_BuildIncremental"] == true)
                }
            }
            steps {
                dir("${env.WORKSPACE}") {
                    sh 'if [ ! -x ./nightly.sh ]; then cp -pf ./usr/src/tools/scripts/nightly.sh ./ && chmod +x nightly.sh || exit ; fi'
                    sh '[ -x ./illumos.sh ] && [ -x ./nightly.sh ] && [ -s ./nightly.sh ] && [ -s ./illumos.sh ]'
                    sh """
echo '`date -u`: STARTING ILLUMOS-GATE BUILD-ALL (prepare to wait... a lot... and in silence!)';
egrep '[^#]*export NIGHTLY_OPTIONS=' illumos.sh;
CCACHE_BASEDIR="`pwd`" \\
time ./nightly.sh \${env.str_option_BuildIncremental} illumos.sh; RES=\$?;
[ "\$RES" = 0 ] || echo "BUILD FAILED (code \$RES), see more details in its logs";
exit \$RES;
"""
                }
            }
            post {
                always {
                    dir("${env.WORKSPACE}") {
                        sh """
echo "BUILD LOG - SHORT [${env.str_nametag}]:"; cat "`ls -1d log/log.*/ | sort -n | tail -1`/mail_msg" || echo "No logs found, was build skipped?"
echo "ARCHIVE BUILD LOG REPORT [${env.str_nametag}]:"; ( echo "log/nightly.log" && find "`ls -1d log/log.*/ | sort -n | tail -1`" -type f ) > logs_to_archive-${env.str_nametag}.txt || echo "No logs found, was build skipped?"
tr '\\n' ',' < logs_to_archive-${env.str_nametag}.txt > logs_to_archive-${env.str_nametag}.csv && cat logs_to_archive-${env.str_nametag}.csv
"""
                        script {
                            def fileToArchive = readFile "logs_to_archive-${env.str_nametag}.csv"
                            archiveArtifacts allowEmptyArchive: true, artifacts: "${fileToArchive}", fingerprint: true, name:"archiveLogs-${env.str_nametag}"
                        }
                    }
                }
            }
        }

        stage("WORKSPACE:BUILD_ND") {
            environment {
                str_nametag = "build-non_debug";
            }
            when {
                expression {
                    return params["action_BuildNonDebug"] == true
                }
            }
            steps {
                dir("${env.WORKSPACE}") {
                    sh 'if [ ! -x ./nightly.sh ]; then cp -pf ./usr/src/tools/scripts/nightly.sh ./ && chmod +x nightly.sh || exit ; fi'
                    sh '[ -x ./illumos.sh ] && [ -x ./nightly.sh ] && [ -s ./nightly.sh ] && [ -s ./illumos.sh ]'
                    sh """
tmpscript="./illumos-once-nd.\$\$.sh"
cp ./illumos.sh "\$tmpscript" && chmod +x "\$tmpscript" || exit;
echo 'export NIGHTLY_OPTIONS="${params.BUILDOPT_NIGHTLY_OPTIONS_BLDNONDEBUG}"' >> "\$tmpscript" || exit;
echo '`date -u`: STARTING ILLUMOS-GATE BUILD NON-DEBUG ONLY (prepare to wait... a lot... and in silence!)';
egrep '[^#]*export NIGHTLY_OPTIONS=' "\$tmpscript";
CCACHE_BASEDIR="`pwd`" \\
time ./nightly.sh -i "\$tmpscript"; RES=\$?;
[ "\$RES" = 0 ] || echo "BUILD NON-DEBUG FAILED (code \$RES), see more details in its logs";
rm -f "\$tmpscript";
exit \$RES;
"""
                }
            }
            post {
                always {
                    dir("${env.WORKSPACE}") {
                        sh """
echo "BUILD LOG - SHORT [${env.str_nametag}]:"; cat "`ls -1d log/log.*/ | sort -n | tail -1`/mail_msg" || echo "No logs found, was build skipped?"
echo "ARCHIVE BUILD LOG REPORT [${env.str_nametag}]:"; ( echo "log/nightly.log" && find "`ls -1d log/log.*/ | sort -n | tail -1`" -type f ) > logs_to_archive-${env.str_nametag}.txt || echo "No logs found, was build skipped?"
tr '\\n' ',' < logs_to_archive-${env.str_nametag}.txt > logs_to_archive-${env.str_nametag}.csv && cat logs_to_archive-${env.str_nametag}.csv
"""
                        script {
                            def fileToArchive = readFile "logs_to_archive-${env.str_nametag}.csv"
                            archiveArtifacts allowEmptyArchive: true, artifacts: "${fileToArchive}", fingerprint: true, name:"archiveLogs-${env.str_nametag}"
                        }
                    }
                }
            }
        }

        stage("WORKSPACE:BUILD_DEBUG") {
            environment {
                str_nametag = "build-debug";
            }
            when {
                expression {
                    return params["action_BuildDebug"] == true
                }
            }
            steps {
                dir("${env.WORKSPACE}") {
                    sh 'if [ ! -x ./nightly.sh ]; then cp -pf ./usr/src/tools/scripts/nightly.sh ./ && chmod +x nightly.sh || exit ; fi'
                    sh '[ -x ./illumos.sh ] && [ -x ./nightly.sh ] && [ -s ./nightly.sh ] && [ -s ./illumos.sh ]'
                    sh """
tmpscript="./illumos-once-debug.\$\$.sh"
cp ./illumos.sh "\$tmpscript" && chmod +x "\$tmpscript" || exit;
echo 'export NIGHTLY_OPTIONS="${params.BUILDOPT_NIGHTLY_OPTIONS_BLDDEBUG}"' >> "\$tmpscript" || exit;
echo '`date -u`: STARTING ILLUMOS-GATE BUILD DEBUG ONLY (prepare to wait... a lot... and in silence!)';
egrep '[^#]*export NIGHTLY_OPTIONS=' "\$tmpscript";
CCACHE_BASEDIR="`pwd`" \\
time ./nightly.sh -i "\$tmpscript"; RES=\$?;
[ "\$RES" = 0 ] || echo "BUILD DEBUG FAILED (code \$RES), see more details in its logs";
rm -f "\$tmpscript";
exit \$RES;
"""
                }
            }
            post {
                always {
                    dir("${env.WORKSPACE}") {
                        sh """
echo "BUILD LOG - SHORT [${env.str_nametag}]:"; cat "`ls -1d log/log.*/ | sort -n | tail -1`/mail_msg" || echo "No logs found, was build skipped?"
echo "ARCHIVE BUILD LOG REPORT [${env.str_nametag}]:"; ( echo "log/nightly.log" && find "`ls -1d log/log.*/ | sort -n | tail -1`" -type f ) > logs_to_archive-${env.str_nametag}.txt || echo "No logs found, was build skipped?"
tr '\\n' ',' < logs_to_archive-${env.str_nametag}.txt > logs_to_archive-${env.str_nametag}.csv && cat logs_to_archive-${env.str_nametag}.csv
"""
                        script {
                            def fileToArchive = readFile "logs_to_archive-${env.str_nametag}.csv"
                            archiveArtifacts allowEmptyArchive: true, artifacts: "${fileToArchive}", fingerprint: true, name:"archiveLogs-${env.str_nametag}"
                        }
                    }
                }
            }
        }

        stage("WORKSPACE:BUILD_PKG") {
            environment {
                str_nametag = "build-pkg";
            }
            when {
                expression {
                    return params["action_BuildPackages"] == true
                }
            }
            steps {
                dir("${env.WORKSPACE}") {
                    sh 'if [ ! -x ./nightly.sh ]; then cp -pf ./usr/src/tools/scripts/nightly.sh ./ && chmod +x nightly.sh || exit ; fi'
                    sh '[ -x ./illumos.sh ] && [ -x ./nightly.sh ] && [ -s ./nightly.sh ] && [ -s ./illumos.sh ]'
                    sh """
tmpscript="./illumos-once-pkg.\$\$.sh"
cp ./illumos.sh "\$tmpscript" && chmod +x "\$tmpscript" || exit;
echo 'export NIGHTLY_OPTIONS="${params.BUILDOPT_NIGHTLY_OPTIONS_BLDPKG}"' >> "\$tmpscript" || exit;
echo '`date -u`: STARTING ILLUMOS-GATE BUILD PACKAGES ONLY (prepare to wait... a lot... and in silence!)';
egrep '[^#]*export NIGHTLY_OPTIONS=' "\$tmpscript";
CCACHE_BASEDIR="`pwd`" \\
time ./nightly.sh -i "\$tmpscript"; RES=\$?;
[ "\$RES" = 0 ] || echo "BUILD PACKAGES FAILED (code \$RES), see more details in its logs";
rm -f "\$tmpscript";
exit \$RES;
"""
                }
            }
            post {
                always {
                    dir("${env.WORKSPACE}") {
                        sh """
echo "BUILD LOG - SHORT [${env.str_nametag}]:"; cat "`ls -1d log/log.*/ | sort -n | tail -1`/mail_msg" || echo "No logs found, was build skipped?"
echo "ARCHIVE BUILD LOG REPORT [${env.str_nametag}]:"; ( echo "log/nightly.log" && find "`ls -1d log/log.*/ | sort -n | tail -1`" -type f ) > logs_to_archive-${env.str_nametag}.txt || echo "No logs found, was build skipped?"
tr '\\n' ',' < logs_to_archive-${env.str_nametag}.txt > logs_to_archive-${env.str_nametag}.csv && cat logs_to_archive-${env.str_nametag}.csv
"""
                        script {
                            def fileToArchive = readFile "logs_to_archive-${env.str_nametag}.csv"
                            archiveArtifacts allowEmptyArchive: true, artifacts: "${fileToArchive}", fingerprint: true, name:"archiveLogs-${env.str_nametag}"
                        }
                    }
                }
            }
        }

        stage("WORKSPACE:CHECK") {
            environment {
                str_nametag = "check";
            }
            when {
                expression {
                    return params["action_Check"] == true
                }
            }
            steps {
                dir("${env.WORKSPACE}") {
                    echo "Checking the build results in '${env.WORKSPACE}' at '${env.NODE_NAME}'"
                    /* Note: Can this require a specific "make" dialect interpreter? */
                    sh 'if [ ! -x ./nightly.sh ]; then cp -pf ./usr/src/tools/scripts/nightly.sh ./ && chmod +x nightly.sh || exit ; fi'
                    sh '[ -x ./illumos.sh ] && [ -x ./nightly.sh ] && [ -s ./nightly.sh ] && [ -s ./illumos.sh ]'
                    sh """
tmpscript="./illumos-once-check.\$\$.sh"
cp ./illumos.sh "\$tmpscript" && chmod +x "\$tmpscript" || exit;
echo 'export NIGHTLY_OPTIONS="${params.BUILDOPT_NIGHTLY_OPTIONS_CHECK}"' >> "\$tmpscript" || exit;
echo '`date -u`: STARTING ILLUMOS-GATE CHECK (prepare to wait... a lot... and in silence!)';
egrep '[^#]*export NIGHTLY_OPTIONS=' "\$tmpscript";
CCACHE_BASEDIR="`pwd`" \\
time ./nightly.sh "\$tmpscript"; RES=\$?;
[ "\$RES" = 0 ] || echo "CHECK FAILED (code \$RES), see more details in its logs";
rm -f "\$tmpscript";
exit \$RES;
"""
                }
            }
            post {
                always {
                    dir("${env.WORKSPACE}") {
                        sh """
echo "BUILD LOG - SHORT [${env.str_nametag}]:"; cat "`ls -1d log/log.*/ | sort -n | tail -1`/mail_msg" || echo "No logs found, was build skipped?"
echo "ARCHIVE BUILD LOG REPORT [${env.str_nametag}]:"; ( echo "log/nightly.log" && find "`ls -1d log/log.*/ | sort -n | tail -1`" -type f ) > logs_to_archive-${env.str_nametag}.txt || echo "No logs found, was build skipped?"
tr '\\n' ',' < logs_to_archive-${env.str_nametag}.txt > logs_to_archive-${env.str_nametag}.csv && cat logs_to_archive-${env.str_nametag}.csv
"""
                        script {
                            def fileToArchive = readFile "logs_to_archive-${env.str_nametag}.csv"
                            archiveArtifacts allowEmptyArchive: true, artifacts: "${fileToArchive}", fingerprint: true, name:"archiveLogs-${env.str_nametag}"
                        }
                    }
                }
            }
        }

        stage("WORKSPACE:LINT") {
            environment {
                str_nametag = "lint";
            }
            when {
                expression {
                    return params["action_Lint"] == true
                }
            }
            steps {
                dir("${env.WORKSPACE}") {
                    sh 'if [ ! -x ./nightly.sh ]; then cp -pf ./usr/src/tools/scripts/nightly.sh ./ && chmod +x nightly.sh || exit ; fi'
                    sh '[ -x ./illumos.sh ] && [ -x ./nightly.sh ] && [ -s ./nightly.sh ] && [ -s ./illumos.sh ]'
                    sh """
tmpscript="./illumos-once-lint.\$\$.sh"
cp ./illumos.sh "\$tmpscript" && chmod +x "\$tmpscript" || exit;
echo 'export NIGHTLY_OPTIONS="${params.BUILDOPT_NIGHTLY_OPTIONS_LINT}"' >> "\$tmpscript" || exit;
echo '`date -u`: STARTING ILLUMOS-GATE LINTING ONLY (prepare to wait... a lot... and in silence!)';
egrep '[^#]*export NIGHTLY_OPTIONS=' "\$tmpscript";
CCACHE_BASEDIR="`pwd`" \\
time ./nightly.sh -i "\$tmpscript"; RES=\$?;
[ "\$RES" = 0 ] || echo "LINTING FAILED (code \$RES), see more details in its logs";
rm -f "\$tmpscript";
exit \$RES;
"""
                }
            }
            post {
                always {
                    dir("${env.WORKSPACE}") {
                        sh """
echo "BUILD LOG - SHORT [${env.str_nametag}]:"; cat "`ls -1d log/log.*/ | sort -n | tail -1`/mail_msg" || echo "No logs found, was build skipped?"
echo "ARCHIVE BUILD LOG REPORT [${env.str_nametag}]:"; ( echo "log/nightly.log" && find "`ls -1d log/log.*/ | sort -n | tail -1`" -type f ) > logs_to_archive-${env.str_nametag}.txt || echo "No logs found, was build skipped?"
tr '\\n' ',' < logs_to_archive-${env.str_nametag}.txt > logs_to_archive-${env.str_nametag}.csv && cat logs_to_archive-${env.str_nametag}.csv
"""
                        script {
                            def fileToArchive = readFile "logs_to_archive-${env.str_nametag}.csv"
                            archiveArtifacts allowEmptyArchive: true, artifacts: "${fileToArchive}", fingerprint: true, name:"archiveLogs-${env.str_nametag}"
                        }
                    }
                }
            }
        }

        stage("WORKSPACE:ArchivePackages") {
            steps {
                dir("${env.WORKSPACE}") {
                    sh """
echo "PACKAGES and PROTO locations under `pwd` :"; ( ls -lad packages/*/* ; ls -lad proto/*/* ; ) || true
echo "ARCHIVE RECENT BUILD LOG AND PKGREPO:"; ( echo "log/nightly.log" ; echo "packages/" ; echo "*.sh"; echo "*.env"; find "`ls -1d log/log.*/ | sort -n | tail -1`" -type f ) > products_to_archive.txt || echo "No logs found, was build skipped?"
tr '\\n' ',' < products_to_archive.txt > products_to_archive-csv.txt && cat products_to_archive-csv.txt
"""
                    script {
                            def fileToArchive = readFile 'products_to_archive-csv.txt'
                            archiveArtifacts allowEmptyArchive: true, artifacts: "${fileToArchive}", fingerprint: true, name:"archiveProducts"
                    }
                }
            }
        }

/* NOTE: These publish steps might be moot on a build zone, and instead
 * the job scheduler would have the builder archive the package area and
 * run the publishig activities on a worker with appropriate networking
 * or filesystem access. But this depends on deployment (and we have the
 * toggle build parameter here) */

        stage("WORKSPACE:PUBLISH_IPS-NON_DEBUG:i386") {
            environment {
                URL_IPS_REPO = "${params.URL_IPS_REPO}";
                str_nametag = "publish-non_debug";
            }
            when {
/* TODO: Additional/alternate conditions, like "env.BRANCH == "master" ? */
/* TODO: Default target repos should be networked, to avoid FS security issues... or maybe not - if we intend to create them as needed */
/* TODO: Queue a separate parametrized job to fetch this one's artifacts and pkgsend them with rewrite to specific version etc. */
                expression {
//                    def JOB_NAME_UNSLASHED = "${env.JOB_NAME}".replaceAll('/', '_')
//                    def BRANCH_UNSLASHED = "${env.BRANCH}".replaceAll('/', '_')
                    if (params["URL_IPS_REPO"] == "") {
                        if (env["BRANCH"] == "master") {
                            URL_IPS_REPO = "/export/ips/jenkins/pkg";
                        } else {
                            if (env["BRANCH"] == "bugfix") {
                                URL_IPS_REPO = "/export/ips/jenkins/bugfix";
                            } else {
                                URL_IPS_REPO = "/export/ips/jenkins/pr";
                            }
                        }
                    }
                    if (params["action_PublishIPS"] == true) {
                        if (fileExists(file: "${env.WORKSPACE}/packages/i386/nightly-nd/repo.redist/cfg_cache")) {
                            return true;
                        }
                        echo "WARNING: Can not publish resulting packages to ${URL_IPS_REPO} because '${env.WORKSPACE}/packages/i386/nightly-nd/repo.redist/cfg_cache' is missing"
                    }
                    return false;
                }
            }
            steps {
                dir("${env.WORKSPACE}") {
                    script {
                        def JOB_NAME_UNSLASHED = "${env.JOB_NAME}".replaceAll('/', '_')
                        def BRANCH_UNSLASHED = "${env.BRANCH}".replaceAll('/', '_')
//                        def str_option_UseJAVA8 = params["option_UseJAVA8"] ? "true" : "false";
//                        env["str_option_UseJAVA8"] = str_option_UseJAVA8;
                    }
                    echo "Publishing IPS packages from '${env.WORKSPACE}/packages/i386/nightly-nd/repo.redist/' at '${env.NODE_NAME}' to '${URL_IPS_REPO}'"
                    sh """
case "$URL_IPS_REPO" in
    *://*) ;;
    /) if [ ! -d "$URL_IPS_REPO" ]; then
        mkdir -p "$URL_IPS_REPO" && \
        pkgrepo create "$URL_IPS_REPO" && \
        pkgrepo set -s "$URL_IPS_REPO" publisher/prefix="on-nightly-${JOB_NAME_UNSLASHED}-nd" && \
        { pkgrepo set -s "$URL_IPS_REPO" -p "on-nightly-${JOB_NAME_UNSLASHED}-nd" publisher/alias= || true; } && \
        pkgrepo refresh -s "$URL_IPS_REPO" || \
        echo "FAILED to prepare empty IPS repo in '$URL_IPS_REPO' at '${env.NODE_NAME}'"
       fi
       ;;
esac
pkgrecv -s "${env.WORKSPACE}/packages/i386/nightly-nd/repo.redist/" -d "$URL_IPS_REPO" 'pkg:/*'
"""
                }
            }
        }

        stage("WORKSPACE:PUBLISH_IPS-DEBUG:i386") {
            environment {
                URL_IPS_REPO = "${params.URL_IPS_REPO}";
                str_nametag = "publish-debug";
            }
            when {
/* TODO: Additional/alternate conditions, like "env.BRANCH == "master" ? */
/* TODO: Default target repos should be networked, to avoid FS security issues... or maybe not - if we intend to create them as needed */
/* TODO: Queue a separate parametrized job to fetch this one's artifacts and pkgsend them with rewrite to specific version etc. */
                expression {
//                    def JOB_NAME_UNSLASHED = "${env.JOB_NAME}".replaceAll('/', '_')
//                    def BRANCH_UNSLASHED = "${env.BRANCH}".replaceAll('/', '_')
                    if (params["URL_IPS_REPO"] == "") {
                        if (env["BRANCH"] == "master") {
                            URL_IPS_REPO = "/export/ips/jenkins/pkg-debug";
                        } else {
                            if (env["BRANCH"] == "bugfix") {
                                URL_IPS_REPO = "/export/ips/jenkins/bugfix-debug";
                            } else {
                                URL_IPS_REPO = "/export/ips/jenkins/pr-debug";
                            }
                        }
                    }
                    if (params["action_PublishIPS"] == true) {
                        if (fileExists(file: "${env.WORKSPACE}/packages/i386/nightly/repo.redist/cfg_cache")) {
                            return true;
                        }
                        echo "WARNING: Can not publish resulting packages to ${URL_IPS_REPO} because '${env.WORKSPACE}/packages/i386/nightly/repo.redist/cfg_cache' is missing"
                    }
                    return false;
                }
            }
            steps {
                dir("${env.WORKSPACE}") {
                    script {
                        def JOB_NAME_UNSLASHED = "${env.JOB_NAME}".replaceAll('/', '_')
                        def BRANCH_UNSLASHED = "${env.BRANCH}".replaceAll('/', '_')
//                        def str_option_UseJAVA8 = params["option_UseJAVA8"] ? "true" : "false";
//                        env["str_option_UseJAVA8"] = str_option_UseJAVA8;
                    }
                    echo "Publishing IPS packages from '${env.WORKSPACE}/packages/i386/nightly/repo.redist/' at '${env.NODE_NAME}' to '${URL_IPS_REPO}'"
                    sh """
case "$URL_IPS_REPO" in
    *://*) ;;
    /) if [ ! -d "$URL_IPS_REPO" ]; then
        mkdir -p "$URL_IPS_REPO" && \
        pkgrepo create "$URL_IPS_REPO" && \
        pkgrepo set -s "$URL_IPS_REPO" publisher/prefix="on-nightly-${JOB_NAME_UNSLASHED}-debug" && \
        { pkgrepo set -s "$URL_IPS_REPO" -p "on-nightly-${JOB_NAME_UNSLASHED}-debug" publisher/alias= || true; } && \
        pkgrepo refresh -s "$URL_IPS_REPO" || \
        echo "FAILED to prepare empty IPS repo in '$URL_IPS_REPO' at '${env.NODE_NAME}'"
       fi
       ;;
esac
pkgrecv -s "${env.WORKSPACE}/packages/i386/nightly/repo.redist/" -d "$URL_IPS_REPO" 'pkg:/*'
"""
                }
            }
        }
    }
}
