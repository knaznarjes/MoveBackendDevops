pipeline {
    agent any

    environment {
        // Docker Configuration
        DOCKER_REGISTRY = 'votre-username' // Remplacez par votre username Docker Hub
        DOCKER_CREDENTIALS_ID = 'docker-hub-credentials'
        BUILD_NUMBER = "${env.BUILD_NUMBER}"
        GIT_COMMIT_SHORT = "${env.GIT_COMMIT.take(8)}"

        // Services Configuration
        SERVICES = 'discovery-service,auth-service,content-service,search-service,community-service,api-gateway'

        // Maven Configuration
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository -Xmx1024m'
    }

    tools {
        maven 'Maven-3.8.6'
        jdk 'OpenJDK-17'
    }

    stages {
        stage('Preparation') {
            steps {
                script {
                    env.BUILD_TAG = "${BUILD_NUMBER}-${GIT_COMMIT_SHORT}"
                    echo "Build Tag: ${env.BUILD_TAG}"

                    // Clean workspace
                    cleanWs()
                    checkout scm

                    // Create .m2 directory for Maven cache
                    sh 'mkdir -p .m2/repository'
                }
            }
        }

        stage('Build & Test Services') {
            parallel {
                stage('Discovery Service') {
                    steps {
                        dir('discovery-service') {
                            sh '''
                                mvn clean compile test-compile
                                mvn test -Dmaven.test.failure.ignore=true
                                mvn package -DskipTests
                            '''
                        }
                    }
                    post {
                        always {
                            dir('discovery-service') {
                                publishTestResults testResultsPattern: 'target/surefire-reports/*.xml'
                                publishHTML([
                                    allowMissing: true,
                                    alwaysLinkToLastBuild: true,
                                    keepAll: true,
                                    reportDir: 'target/site/jacoco',
                                    reportFiles: 'index.html',
                                    reportName: 'Discovery Service Coverage'
                                ])
                            }
                        }
                    }
                }

                stage('Auth Service') {
                    steps {
                        dir('auth-service') {
                            sh '''
                                mvn clean compile test-compile
                                mvn test -Dmaven.test.failure.ignore=true
                                mvn package -DskipTests
                            '''
                        }
                    }
                    post {
                        always {
                            dir('auth-service') {
                                publishTestResults testResultsPattern: 'target/surefire-reports/*.xml'
                            }
                        }
                    }
                }

                stage('Content Service') {
                    steps {
                        dir('content-service') {
                            sh '''
                                mvn clean compile test-compile
                                mvn test -Dmaven.test.failure.ignore=true
                                mvn package -DskipTests
                            '''
                        }
                    }
                    post {
                        always {
                            dir('content-service') {
                                publishTestResults testResultsPattern: 'target/surefire-reports/*.xml'
                            }
                        }
                    }
                }

                stage('Search Service') {
                    steps {
                        dir('search-service') {
                            sh '''
                                mvn clean compile test-compile
                                mvn test -Dmaven.test.failure.ignore=true
                                mvn package -DskipTests
                            '''
                        }
                    }
                    post {
                        always {
                            dir('search-service') {
                                publishTestResults testResultsPattern: 'target/surefire-reports/*.xml'
                            }
                        }
                    }
                }

                stage('Community Service') {
                    steps {
                        dir('community-service') {
                            sh '''
                                mvn clean compile test-compile
                                mvn test -Dmaven.test.failure.ignore=true
                                mvn package -DskipTests
                            '''
                        }
                    }
                    post {
                        always {
                            dir('community-service') {
                                publishTestResults testResultsPattern: 'target/surefire-reports/*.xml'
                            }
                        }
                    }
                }

                stage('API Gateway') {
                    steps {
                        dir('api-gateway') {
                            sh '''
                                mvn clean compile test-compile
                                mvn test -Dmaven.test.failure.ignore=true
                                mvn package -DskipTests
                            '''
                        }
                    }
                    post {
                        always {
                            dir('api-gateway') {
                                publishTestResults testResultsPattern: 'target/surefire-reports/*.xml'
                            }
                        }
                    }
                }
            }
        }

        stage('Build Docker Images') {
            parallel {
                stage('Build Discovery') {
                    steps {
                        script {
                            def image = docker.build("${DOCKER_REGISTRY}/move-discovery:${BUILD_TAG}", "discovery-service")
                            docker.withRegistry('https://registry.hub.docker.com', DOCKER_CREDENTIALS_ID) {
                                image.push()
                                image.push("latest")
                            }
                        }
                    }
                }

                stage('Build Auth') {
                    steps {
                        script {
                            def image = docker.build("${DOCKER_REGISTRY}/move-auth:${BUILD_TAG}", "auth-service")
                            docker.withRegistry('https://registry.hub.docker.com', DOCKER_CREDENTIALS_ID) {
                                image.push()
                                image.push("latest")
                            }
                        }
                    }
                }

                stage('Build Content') {
                    steps {
                        script {
                            def image = docker.build("${DOCKER_REGISTRY}/move-content:${BUILD_TAG}", "content-service")
                            docker.withRegistry('https://registry.hub.docker.com', DOCKER_CREDENTIALS_ID) {
                                image.push()
                                image.push("latest")
                            }
                        }
                    }
                }

                stage('Build Search') {
                    steps {
                        script {
                            def image = docker.build("${DOCKER_REGISTRY}/move-search:${BUILD_TAG}", "search-service")
                            docker.withRegistry('https://registry.hub.docker.com', DOCKER_CREDENTIALS_ID) {
                                image.push()
                                image.push("latest")
                            }
                        }
                    }
                }

                stage('Build Community') {
                    steps {
                        script {
                            def image = docker.build("${DOCKER_REGISTRY}/move-community:${BUILD_TAG}", "community-service")
                            docker.withRegistry('https://registry.hub.docker.com', DOCKER_CREDENTIALS_ID) {
                                image.push()
                                image.push("latest")
                            }
                        }
                    }
                }

                stage('Build Gateway') {
                    steps {
                        script {
                            def image = docker.build("${DOCKER_REGISTRY}/move-gateway:${BUILD_TAG}", "api-gateway")
                            docker.withRegistry('https://registry.hub.docker.com', DOCKER_CREDENTIALS_ID) {
                                image.push()
                                image.push("latest")
                            }
                        }
                    }
                }
            }
        }

        stage('Security Scan') {
            parallel {
                stage('Trivy Security Scan') {
                    steps {
                        script {
                            def services = ['discovery', 'auth', 'content', 'search', 'community', 'gateway']
                            services.each { service ->
                                sh """
                                    docker run --rm -v /var/run/docker.sock:/var/run/docker.sock \
                                    aquasec/trivy:latest image \
                                    --format json --output ${service}-security-report.json \
                                    ${DOCKER_REGISTRY}/move-${service}:${BUILD_TAG}
                                """
                            }
                        }
                    }
                    post {
                        always {
                            archiveArtifacts artifacts: '*-security-report.json', allowEmptyArchive: true
                        }
                    }
                }
            }
        }
    }
     
    post {
        always {
            // Nettoyage final
            sh '''
                docker system prune -f
                docker volume prune -f
            '''

            // Archive des artefacts
            archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: true

            // Nettoyage du workspace
            cleanWs()
        }

        success {
            script {
                def message = """
                ✅ *Pipeline SUCCESS* for ${env.JOB_NAME}
                📋 *Build*: ${env.BUILD_NUMBER}
                🏷️ *Tag*: ${env.BUILD_TAG}
                🌿 *Branch*: ${env.BRANCH_NAME}
                👤 *User*: ${env.BUILD_USER ?: 'System'}
                ⏱️ *Duration*: ${currentBuild.durationString}

                🐳 *Images pushed to Docker Hub:*
                • ${DOCKER_REGISTRY}/move-discovery:${BUILD_TAG}
                • ${DOCKER_REGISTRY}/move-auth:${BUILD_TAG}
                • ${DOCKER_REGISTRY}/move-content:${BUILD_TAG}
                • ${DOCKER_REGISTRY}/move-search:${BUILD_TAG}
                • ${DOCKER_REGISTRY}/move-community:${BUILD_TAG}
                • ${DOCKER_REGISTRY}/move-gateway:${BUILD_TAG}
                """

                // Slack notification
                slackSend channel: '#deployments',
                         color: 'good',
                         message: message
            }
        }

        failure {
            script {
                def message = """
                ❌ *Pipeline FAILED* for ${env.JOB_NAME}
                📋 *Build*: ${env.BUILD_NUMBER}
                🌿 *Branch*: ${env.BRANCH_NAME}
                ⏱️ *Duration*: ${currentBuild.durationString}
                🔗 *Build URL*: ${env.BUILD_URL}
                """

                slackSend channel: '#deployments',
                         color: 'danger',
                         message: message
            }
        }

        unstable {
            slackSend channel: '#deployments',
                     color: 'warning',
                     message: "⚠️ Pipeline unstable for ${env.JOB_NAME} - ${env.BUILD_NUMBER}"
        }
    }
}
