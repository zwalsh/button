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
        stage('test') {
            steps {
                sh './gradlew clean build'
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
                sh "rm ~testbutton/releases/current"
                sh "ln -s ~button/releases/$GIT_COMMIT ~button/releases/current"
                // Restart the button service (only has sudo permissions for this command)
                sh "sudo systemctl restart testbutton"
            }
        }
        stage('release') {
            when {
                expression { env.GIT_BRANCH == 'origin/main' }
            }
            steps {
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
            junit '**/build/test-results/test/TEST-*.xml'
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
