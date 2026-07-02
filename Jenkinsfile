pipeline {
    agent any

    environment {
        DOCKERHUB_REPO = 'liokockhock2003/ifoto-backend-sc'
        // VM_HOST comes from Manage Jenkins -> System -> Global properties -> Environment variables
    }

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
                sh './mvnw checkstyle:check -B'
            }
            post {
                always {
                    recordIssues(
                        enabledForFailure: false,
                        tools: [checkStyle(pattern: 'target/checkstyle-result.xml')]
                    )
                }
            }
        }

        stage('Test') {
            steps {
                sh '''
                    # Start MySQL sidecar container
                    docker run -d \
                    --name ifoto-mysql-${BUILD_NUMBER} \
                    --network host \
                    -e MYSQL_ROOT_PASSWORD=root \
                    -e MYSQL_DATABASE=ifotodb_test \
                    -e MYSQL_USER=ifoto_admin_test \
                    -e MYSQL_PASSWORD=ifoto_admin_test \
                    mysql:8.0

                    echo "Waiting for MySQL to be ready..."
                    timeout 90 bash -c \
                    'until docker exec ifoto-mysql-${BUILD_NUMBER} mysqladmin ping -h 127.0.0.1 --silent 2>/dev/null; \
                    do echo "still waiting..."; sleep 3; done'
                    echo "MySQL ready."

                    ./mvnw test \
                    -Dspring.profiles.active=dev \
                    -Dspring.datasource.url="jdbc:mysql://127.0.0.1:3306/ifotodb_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kuala_Lumpur" \
                    -Dspring.datasource.username=ifoto_admin_test \
                    -Dspring.datasource.password=ifoto_admin_test \
                    -Djwt.secret=test-secret-key-32-chars-minimum-length-ci-only \
                    -Djwt.expiration-ms=86400000 \
                    -B
                '''
            }
            post {
                always {
                    sh 'docker stop ifoto-mysql-${BUILD_NUMBER} && docker rm ifoto-mysql-${BUILD_NUMBER} || true'
                    junit 'target/surefire-reports/**/*.xml'
                }
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
                    env.COMMIT_SHA = sh(
                        script: 'git rev-parse --short HEAD',
                        returnStdout: true
                    ).trim()
                    sh """
                        docker build \
                            -t ${DOCKERHUB_REPO}:latest \
                            -t ${DOCKERHUB_REPO}:${env.COMMIT_SHA} \
                            .
                        echo "Docker image built: ${DOCKERHUB_REPO}:${env.COMMIT_SHA}"
                        docker images | grep ifoto-backend
                    """
                }
            }
        }

        // ── A7: Push image to Docker Hub ──────────────────────────────────
        stage('Docker Push') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-creds',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh """
                        echo "\$DOCKER_PASS" | docker login -u "\$DOCKER_USER" --password-stdin
                        docker push ${DOCKERHUB_REPO}:latest
                        docker push ${DOCKERHUB_REPO}:${env.COMMIT_SHA}
                        docker logout
                    """
                }
            }
        }

        // ── A5: Deploy to GCP VM ───────────────────────────────────────────
        stage('Deploy') {
            steps {
                sshagent(credentials: ['gcp-vm-ssh-key']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ${env.VM_HOST} '
                            docker pull ${DOCKERHUB_REPO}:latest &&
                            docker stop ifoto-backend-sc-prod || true &&
                            docker rm ifoto-backend-sc-prod || true &&
                            docker run -d \
                                --name ifoto-backend-sc-prod \
                                --network ifoto-prod-net \
                                --restart unless-stopped \
                                --env-file /opt/ifoto/.env.prod \
                                -p 8082:8080 \
                                ${DOCKERHUB_REPO}:latest
                        '
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