pipeline {
  environment {
      NEXUS_TECNO = credentials('nexus')
      NEXUS_TECNO_USER = "${NEXUS_TECNO_USR}"
      NEXUS_TECNO_PASS = "${NEXUS_TECNO_PSW}"

      NEXUS_TECNO_OLD = credentials('nexus_old')
      NEXUS_TECNO_OLD_USER = "${NEXUS_TECNO_OLD_USR}"
      NEXUS_TECNO_OLD_PASS = "${NEXUS_TECNO_OLD_PSW}"

      DEPLOY_FILE = "build/libs/BigDoors.jar"
      DEPLOY_DEST = "plugins/BigDoors.jar"

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
            karpenter.sh/do-not-disrupt: "true"
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
          affinity:
            nodeAffinity:
              requiredDuringSchedulingIgnoredDuringExecution:
                nodeSelectorTerms:
                  - matchExpressions:
                    - key: beta.kubernetes.io/arch
                      operator: In
                      values:
                        - amd64
                    - key: karpenter.k8s.aws/instance-category
                      operator: In
                      values:
                        - c
                    - key: karpenter.k8s.aws/instance-generation
                      operator: In
                      values:
                        - 6
                    - key: karpenter.k8s.aws/instance-size
                      operator: In
                      values:
                        - 8xlarge
        """
    }
  }

  stages {
      stage('Setup') {
          steps {
              container('maven') {
                sh """
                yum install -y wget git
                wget -O /root/.m2/settings.xml https://gist.githubusercontent.com/zPirroZ3007/4bdcb7e6220dd34b8bf39a562ece8776/raw/settings.xml
                """
              }
          }
      }

      stage('BuildTools') {
          steps {
              container('maven') {
                sh """
                wget https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar
                java -jar BuildTools.jar --rev 1.20.1 --remapped
                java -jar BuildTools.jar --rev 1.20.2 --remapped
                java -jar BuildTools.jar --rev 1.20.4 --remapped
                """
              }
          }
      }

      stage('Build') {
          steps {
              container('maven') {
                sh 'mvn clean package'
              }
              archiveArtifacts artifacts: 'BigDoors.jar', fingerprint: true
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