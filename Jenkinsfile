pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                echo 'SOFTCON-1 Building iFoto backend'
                sh './mvnw clean package -DskipTests -B'
            }
        }
    }

    post {
        always {
            jiraSendBuildInfo site: 'https://soft-con.atlassian.net'
        }
    }
}