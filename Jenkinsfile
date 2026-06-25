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
            script {
                // Extract SOFTCON-N keys from the commit message
                def commitMsg = sh(
                    script: 'git log -1 --pretty=%B',
                    returnStdout: true
                ).trim()

                def keys = (commitMsg =~ /SOFTCON-\d+/).collect { it }

                keys.each { key ->
                    jiraComment(
                        site: 'https://soft-con.atlassian.net',
                        issueKey: key,
                        body: "Jenkins Build #${env.BUILD_NUMBER} — ${currentBuild.result ?: 'SUCCESS'}\n${env.BUILD_URL}"
                    )
                }
            }
        }
    }
}