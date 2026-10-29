
pipeline {
    agent { label 'built-in' }

    options {
        skipDefaultCheckout(true)
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Environment Check') {
            steps {
                bat 'java -version'
                bat 'javac -version'
                bat 'git --version'
                bat 'call mvnw.cmd -version'
            }
        }

        stage('Compile') {
            steps {
                bat 'call mvnw.cmd -B clean compile'
            }
        }

        stage('Test') {
            steps {
                bat 'call mvnw.cmd -B test'
            }
        }

        stage('Package') {
            steps {
                bat 'call mvnw.cmd -B -DskipTests package'
                bat 'if not exist target\\borrowdesk.jar exit /b 1'
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true,
                  testResults: 'target/surefire-reports/*.xml'
        }

        success {
            archiveArtifacts artifacts: 'target/borrowdesk.jar',
                             fingerprint: true
        }
    }
}
