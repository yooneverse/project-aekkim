pipeline {
    agent any

    options {
        skipDefaultCheckout(true)
        timestamps()
    }

    environment {
        BACKEND_IMAGE = 'ungnam0509/e106-backend'
        AI_IMAGE = 'ungnam0509/e106-ai-scraper'
        DOCKER_CREDENTIALS_ID = 'dockerhub-creds'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Resolve Target') {
            steps {
                script {
                    def branch = env.BRANCH_NAME ?: env.GIT_LOCAL_BRANCH ?: env.GIT_BRANCH ?: ''
                    branch = branch.replaceFirst(/^origin\//, '').replaceFirst(/^\*\//, '').trim()

                    if (!branch) {
                        branch = sh(
                            script: '''git for-each-ref --format='%(refname:short)' --contains HEAD refs/remotes/origin | sed 's#^origin/##' | grep -E '^(develop|main)$' | head -n 1''',
                            returnStdout: true
                        ).trim()
                    }

                    if (branch == 'develop') {
                        env.DEPLOY_ENV = 'dev'
                        env.DEPLOY_SCRIPT = './INFRA/deploy-dev.sh'
                    } else if (branch == 'main') {
                        env.DEPLOY_ENV = 'prod'
                        env.DEPLOY_SCRIPT = './INFRA/deploy-prod.sh'
                    } else {
                        error("Unsupported branch for deployment: ${branch}. Only develop and main are allowed.")
                    }

                    env.BRANCH_NAME = branch
                    env.BACKEND_IMAGE_TAG = "${env.BACKEND_IMAGE}:${env.DEPLOY_ENV}"
                    env.AI_IMAGE_TAG = "${env.AI_IMAGE}:${env.DEPLOY_ENV}"

                    echo "Branch: ${env.BRANCH_NAME}"
                    echo "Deploy environment: ${env.DEPLOY_ENV}"
                    echo "Backend image: ${env.BACKEND_IMAGE_TAG}"
                    echo "AI image: ${env.AI_IMAGE_TAG}"
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                sh 'docker build -t "$BACKEND_IMAGE_TAG" ./BE'
                sh 'docker build -t "$AI_IMAGE_TAG" ./AI'
            }
        }

        stage('Push Docker Images') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: env.DOCKER_CREDENTIALS_ID,
                    usernameVariable: 'DOCKER_USERNAME',
                    passwordVariable: 'DOCKER_PASSWORD'
                )]) {
                    sh 'echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin'
                    sh 'docker push "$BACKEND_IMAGE_TAG"'
                    sh 'docker push "$AI_IMAGE_TAG"'
                }
            }
        }

        stage('Prepare Env File') {
            steps {
                script {
                    def envFileCredentialsId = env.DEPLOY_ENV == 'dev' ? 'e106-env-dev' : 'e106-env-prod'
                    def targetEnvFile = env.DEPLOY_ENV == 'dev' ? 'INFRA/.env.dev' : 'INFRA/.env.prod'
                    def aiEnvFileCredentialsId = env.DEPLOY_ENV == 'dev' ? 'e106-ai-env-dev' : 'e106-ai-env-prod'
                    def aiTargetEnvFile = 'AI/scraper/.env'

                    sh 'mkdir -p INFRA AI/scraper'

                    withCredentials([file(credentialsId: envFileCredentialsId, variable: 'ENV_FILE')]) {
                        sh 'cp "$ENV_FILE" "' + targetEnvFile + '"'
                    }
                    sh 'sed -i \'s/\r$//\' "' + targetEnvFile + '"'
                    sh 'test -f "' + targetEnvFile + '" && ls -al "' + targetEnvFile + '"'

                    withCredentials([file(credentialsId: aiEnvFileCredentialsId, variable: 'AI_ENV_FILE')]) {
                        sh 'cp "$AI_ENV_FILE" "' + aiTargetEnvFile + '"'
                    }
                    sh 'sed -i \'s/\r$//\' "' + aiTargetEnvFile + '"'
                    sh 'test -f "' + aiTargetEnvFile + '" && ls -al "' + aiTargetEnvFile + '"'
                }
            }
        }

        stage('Deploy') {
            steps {
                sh 'chmod +x ./INFRA/deploy-dev.sh ./INFRA/deploy-prod.sh'
                sh '"$DEPLOY_SCRIPT"'
            }
        }
    }

    post {
        success {
            echo "Deployment completed for ${env.BRANCH_NAME} -> ${env.DEPLOY_ENV}"
        }
        failure {
            echo "Pipeline failed on branch ${env.BRANCH_NAME ?: 'unknown'}"
        }
        always {
            sh 'docker logout || true'
        }
    }
}
