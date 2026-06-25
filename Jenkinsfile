pipeline {
    agent any

    stages {

        stage('Build') {
            steps {
                script {
                    env.COMMIT_MSG = sh(
                        script: 'git log -1 --pretty=%B',
                        returnStdout: true
                    ).trim()
                    def keys = (env.COMMIT_MSG =~ /SOFTCON-\d+/).collect { it }
                    keys.each { key ->
                        try {
                            jiraExecuteWorkflow(
                                jqlSearch: "issueKey = ${key}",
                                workflowActionName: 'In Progress'
                            )
                        } catch (err) {
                            echo "Transition skipped for ${key}: ${err.message}"
                        }
                        jiraComment(
                            issueKey: key,
                            body: "Build #${env.BUILD_NUMBER} started.\n${env.BUILD_URL}"
                        )
                    }
                }
                sh './mvnw clean package -DskipTests -B'
            }
        }

        stage('Lint') {
            steps {
                echo 'SOFTCON-3 Lint stage — Checkstyle coming in A5'
            }
        }

        stage('Test') {
            steps {
                echo 'SOFTCON-3 Test stage — unit tests coming in A5'
            }
        }

        // ── A3: SonarQube analysis (also serves as B3 evidence) ──────────
        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh '''
                        ./mvnw sonar:sonar \
                            -Dsonar.projectKey=ifoto-backend \
                            -Dsonar.projectName="iFoto Backend" \
                            -Dsonar.host.url=http://136.113.123.68:9000 \
                            -B
                    '''
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 2, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: false
                }
            }
        }

        // ── A3: JMeter integration ────────────────────────────────────────
        stage('Performance Test') {
            steps {
                sh '''
                    mkdir -p jmeter/results/html-report
                    /opt/apache-jmeter/bin/jmeter -n \
                        -t  jmeter/ifoto-performance-test.jmx \
                        -l  jmeter/results/results.jtl \
                        -e  -o jmeter/results/html-report \
                        -j  jmeter/results/jmeter.log \
                        -JBASE_HOST=136.113.123.68 \
                        -JBASE_PORT=8082
                '''
            }
            post {
                always {
                    publishHTML([
                        allowMissing:          true,
                        alwaysLinkToLastBuild: true,
                        keepAll:               true,
                        reportDir:             'jmeter/results/html-report',
                        reportFiles:           'index.html',
                        reportName:            'JMeter Performance Report'
                    ])
                    archiveArtifacts artifacts: 'jmeter/results/**',
                    allowEmptyArchive: true
                }
            }
        }

        // ── A3: Docker plugin (dind) ──────────────────────────────────────
        stage('Docker Build') {
            steps {
                script {
                    def commitSha = sh(
                        script: 'git rev-parse --short HEAD',
                        returnStdout: true
                    ).trim()
                    sh """
                        docker build \
                            -t ifoto-backend:latest \
                            -t ifoto-backend:${commitSha} \
                            .
                        echo "Docker image built: ifoto-backend:${commitSha}"
                        docker images | grep ifoto-backend
                    """
                }
            }
        }

        stage('Review') {
            steps {
                script {
                    def keys = (env.COMMIT_MSG =~ /SOFTCON-\d+/).collect { it }
                    keys.each { key ->
                        try {
                            jiraExecuteWorkflow(
                                jqlSearch: "issueKey = ${key}",
                                workflowActionName: 'In Review'
                            )
                        } catch (err) {
                            echo "Transition skipped for ${key}: ${err.message}"
                        }
                        jiraComment(
                            issueKey: key,
                            body: "Build #${env.BUILD_NUMBER} — all stages passed, in review.\n${env.BUILD_URL}"
                        )
                    }
                }
            }
        }

    }

    post {
        success {
            script {
                def keys = (env.COMMIT_MSG =~ /SOFTCON-\d+/).collect { it }
                keys.each { key ->
                    try {
                        jiraExecuteWorkflow(
                            jqlSearch: "issueKey = ${key}",
                            workflowActionName: 'Done'
                        )
                    } catch (err) {
                        echo "Transition skipped for ${key}: ${err.message}"
                    }
                    jiraComment(
                        issueKey: key,
                        body: "Build #${env.BUILD_NUMBER} PASSED — closed.\n${env.BUILD_URL}"
                    )
                }
            }
        }
        failure {
            script {
                def keys = (env.COMMIT_MSG =~ /SOFTCON-\d+/).collect { it }
                keys.each { key ->
                    try {
                        jiraExecuteWorkflow(
                            jqlSearch: "issueKey = ${key}",
                            workflowActionName: 'To Do'
                        )
                    } catch (err) {
                        echo "Transition skipped for ${key}: ${err.message}"
                    }
                    jiraComment(
                        issueKey: key,
                        body: "Build #${env.BUILD_NUMBER} FAILED.\n${env.BUILD_URL}"
                    )
                }
            }
        }
    }
}