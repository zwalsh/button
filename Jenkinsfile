pipeline {
    agent any
    tools {
        jdk 'JDK 17'
    }
    environment {
        GITHUB_TOKEN = credentials('github-personal-access-token')
    }
    stages {
        stage('start') {
            steps {
                setBuildStatus('pending')
            }
        }
        stage('assemble') {
            steps {
                sh './gradlew assemble testClasses'
            }
        }
        stage('lint') {
            steps {
                sh './gradlew ktlintCheck'
            }
        }
        stage('test') {
            parallel {
                stage('Gradle test') {
                    steps {
                        sh './gradlew check'
                    }
                }
                stage('frontend test') {
                    steps {
                        sh './frontend/preflight.sh'
                        sh './frontend/test.sh'
                    }
                }
            }
        }
        stage('release') {
            when { branch 'main' }
            steps {
                sh '''
                    gh release create "sha-${GIT_COMMIT}" \
                      build/distributions/button.tar \
                      --title "sha-${GIT_COMMIT}" \
                      --notes "" \
                      --latest \
                      --repo zwalsh/button
                '''
            }
        }
    }
    post {
        success {
            echo 'Success!'
            setBuildStatus('success')
        }
        unstable {
            echo 'I am unstable :/'
            setBuildStatus('failure')
        }
        failure {
            echo 'I failed :('
            setBuildStatus('failure')
        }
        always {
            junit testResults: '**/build/test-results/test/TEST-*.xml, frontend/test-results/**/*.xml'
        }
    }
}

void setBuildStatus(state) {
    sh """
        curl "https://api.GitHub.com/repos/zwalsh/button/statuses/$GIT_COMMIT" \
                -H "Content-Type: application/json" \
                -H "Authorization: token $GITHUB_TOKEN" \
                -X POST \
                -d '{\"state\": \"$state\",\"context\": \"continuous-integration/jenkins\",
                \"description\": \"Jenkins\", \"target_url\": \"https://jenkins.zachwal.sh/job/button/$BUILD_NUMBER/console\"}'
    """
}
