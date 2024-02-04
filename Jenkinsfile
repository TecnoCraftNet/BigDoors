pipeline {
  environment {
      NEXUS_TECNO = credentials('nexus')
      NEXUS_TECNO_USER = "${NEXUS_TECNO_USR}"
      NEXUS_TECNO_PASS = "${NEXUS_TECNO_PSW}"

      NEXUS_TECNO_OLD = credentials('nexus_old')
      NEXUS_TECNO_OLD_USER = "${NEXUS_TECNO_OLD_USR}"
      NEXUS_TECNO_OLD_PASS = "${NEXUS_TECNO_OLD_PSW}"

      DEPLOY_FILE = ""
      DEPLOY_DEST = ""

      DEPLOY_CREDS = credentials('deploy')
      DEPLOY_SERVER = "${DEPLOY_CREDS_USR}"
      DEPLOY_KEY = "${DEPLOY_CREDS_PSW}"

      TELEGRAM_TOKEN = credentials('telegram')
  }

  agent {
    kubernetes {
      yaml """
        apiVersion: v1
        kind: Pod
        metadata:
          annotations:
            dynamic-pvc-provisioner.kubernetes.io/maven-cache.enabled: "true"
            dynamic-pvc-provisioner.kubernetes.io/maven-cache.pvc: |
              apiVersion: v1
              kind: PersistentVolumeClaim
              spec:
                storageClassName: jenkins-maven-cache
                resources:
                  requests:
                    storage: 1Gi
                accessModes:
                  - ReadWriteOnce
        spec:
          containers:
          - name: maven
            image: "maven:3.8.6-amazoncorretto-17"
            command:
            - cat
            tty: true
            volumeMounts:
              - name: maven-cache
                mountPath: /root/.m2
          volumes:
          - name: maven-cache
            persistentVolumeClaim:
              claimName: "${UUID.randomUUID()}"
        """
    }
  }

  stages {
      stage('Setup') {
          steps {
              container('maven') {
                sh """
                yum install -y wget
                wget -O /root/.m2/settings.xml https://gist.githubusercontent.com/zPirroZ3007/4bdcb7e6220dd34b8bf39a562ece8776/raw/settings.xml
                """
              }
          }
      }

      stage('Build') {
          steps {
              container('maven') {
                sh 'mvn clean package'
              }
          }
      }

      stage('Deploy') {
          when {
             branch 'master'
          }
          steps {
             container('maven') {
                sh '''#!/bin/bash

                    if [[ "${DEPLOY_FILE}" = "" ]]; then
                       exit 0
                    fi
                    curl -F "key=${DEPLOY_KEY}" -F "file=@${DEPLOY_FILE}" -F "destination=${DEPLOY_DEST}" "${DEPLOY_SERVER}" | grep 'successo' &> /dev/null
                    if [ $? != 0 ]; then
                       exit 1
                    fi
                '''
             }
          }
      }
  }

  post {
      success {
          sh """#!/bin/bash
          curl --location 'https://api.telegram.org/bot{$TELEGRAM_TOKEN}/sendMessage' --form 'text="🟢 successo <b>${currentBuild.fullProjectName.split('/')[1]}:${BRANCH_NAME}</b> <code>#${env.BUILD_NUMBER}</code>\n\n${BUILD_URL}"' --form 'chat_id="-1001566677189"' --form 'parse_mode="html"'
          """
      }
      failure {
          sh """#!/bin/bash
          curl --location 'https://api.telegram.org/bot{$TELEGRAM_TOKEN}/sendMessage' --form 'text="🔴 fallita <b>${currentBuild.fullProjectName.split('/')[1]}:${BRANCH_NAME}</b> <code>#${env.BUILD_NUMBER}</code>\n\n${BUILD_URL}"' --form 'chat_id="-1001566677189"' --form 'parse_mode="html"'
          """
      }
  }
}
