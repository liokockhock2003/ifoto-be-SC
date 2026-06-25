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

                    // Move To Do → In Progress
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
                            body: "Build #${env.BUILD_NUMBER} started — compiling iFoto backend.\n${env.BUILD_URL}"
                        )
                    }
                }

                sh './mvnw clean package -DskipTests -B'
            }
        }

        stage('Lint') {
            steps {
                echo 'SOFTCON-3 Lint stage placeholder — Checkstyle coming in A5'
            }
        }

        stage('Review') {
            steps {
                script {
                    def keys = (env.COMMIT_MSG =~ /SOFTCON-\d+/).collect { it }

                    // Move In Progress → In Review
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
                            body: "Build #${env.BUILD_NUMBER} passed — moved to In Review.\n${env.BUILD_URL}"
                        )
                    }
                }
                echo 'Code review checkpoint'
            }
        }

    }

    post {
        success {
            script {
                def keys = (env.COMMIT_MSG =~ /SOFTCON-\d+/).collect { it }

                // Move In Review → Done
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
                        body: "Build #${env.BUILD_NUMBER} PASSED — issue closed and moved to Done.\n${env.BUILD_URL}"
                    )
                }
            }
        }

        failure {
            script {
                def keys = (env.COMMIT_MSG =~ /SOFTCON-\d+/).collect { it }

                // Move back to To Do on failure
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
                        body: "Build #${env.BUILD_NUMBER} FAILED — issue moved back to To Do.\n${env.BUILD_URL}"
                    )
                }
            }
        }
    }
}