pipeline {
    agent any
    environment {
        GITHUB_TOKEN = credentials('github-personal-access-token')
    }
    stages {
        stage('start') {
            steps {
                setBuildStatus('pending')
            }
        }
        stage('frontend test') {
            steps {
                sh './frontend/preflight.sh'
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
            steps {
                sh './gradlew build'
            }
        }
        stage('frontend test') {
            steps {
                sh './frontend/preflight.sh'

                sh 'mkdir -p ~/.npm'
                sh 'npm ci --prefix frontend --cache ~/.npm --no-audit --no-fund'
                sh 'npm --prefix frontend test'
            }
        }
        stage('test-release') {
            steps {
                // Clear testbutton releases
                sh "rm -rf ~testbutton/releases/*"
                // Create the release
                sh "mkdir ~testbutton/releases/$GIT_COMMIT"
                sh "tar -xvf build/distributions/button.tar -C ~testbutton/releases/$GIT_COMMIT"
                // Set it as current
                sh "ln -s ~testbutton/releases/$GIT_COMMIT ~testbutton/releases/current"
                // Restart the button service (only has sudo permissions for this command)
                sh "sudo systemctl restart testbutton"
            }
        }
        stage('migrate database - testbutton') {
            when {
                expression { env.GIT_BRANCH == 'origin/main' }
            }
            steps {
                // Copy migrations & script into ~testbutton
                sh "rm -rf ~testbutton/migrations/*"
                sh "cp -r db ~testbutton/migrations"

                // Run migrations as testbutton
                dir("/home/testbutton/migrations/db") {
                    sh "sudo -u testbutton ./migrate.sh"
                }
            }
        }
        stage('migrate database - button') {
            when {
                expression { env.GIT_BRANCH == 'origin/main' }
            }
            steps {
                // Copy migrations & script into ~testbutton
                sh "rm -rf ~button/migrations/*"
                sh "cp -r db ~button/migrations"

                // Run migrations as testbutton
                dir("/home/button/migrations/db") {
                    sh "sudo -u button ./migrate.sh"
                }
            }
        }
        stage('release') {
            when {
                expression { env.GIT_BRANCH == 'origin/main' }
            }
            steps {
                // Delete all but the three most recent releases
                sh "ls -t ~button/releases | tail -n +4 | xargs -I {} rm -rf ~button/releases/{}"

                // Create the release
                sh "mkdir ~button/releases/$GIT_COMMIT"
                sh "tar -xvf build/distributions/button.tar -C ~button/releases/$GIT_COMMIT"
                // Set it as current
                sh "rm ~button/releases/current"
                sh "ln -s ~button/releases/$GIT_COMMIT ~button/releases/current"
                // Restart the button service (only has sudo permissions for this command)
                sh "sudo systemctl restart button"
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
