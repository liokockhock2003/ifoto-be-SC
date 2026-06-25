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
    // remove the post block entirely
}