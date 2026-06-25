pipeline {
    agent any

    stages {

        stage('Build') {
            steps {
                script {
                    // Extract SOFTCON-N keys from the triggering commit
                    env.COMMIT_MSG = sh(
                        script: 'git log -1 --pretty=%B',
                        returnStdout: true
                    ).trim()

                    def keys = (env.COMMIT_MSG =~ /SOFTCON-\d+/).collect { it }

                    // To Do → In Progress (transition id: 21)
                    keys.each { key ->
                        try {
                            jiraExecuteWorkflow(
                                site: 'https://soft-con.atlassian.net',
                                idOrKey: key,
                                workflowName: 'In Progress'
                            )
                        } catch (err) {
                            echo "Transition skipped for ${key}: ${err.message}"
                        }
                        jiraComment(
                            site: 'https://soft-con.atlassian.net',
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

                    // In Progress → In Review (transition id: 31)
                    keys.each { key ->
                        try {
                            jiraExecuteWorkflow(
                                site: 'https://soft-con.atlassian.net',
                                idOrKey: key,
                                workflowName: 'In Review'
                            )
                        } catch (err) {
                            echo "Transition skipped for ${key}: ${err.message}"
                        }
                        jiraComment(
                            site: 'https://soft-con.atlassian.net',
                            issueKey: key,
                            body: "Build #${env.BUILD_NUMBER} passed build stage — moved to In Review.\n${env.BUILD_URL}"
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

                // In Review → Done (transition id: 41)
                keys.each { key ->
                    try {
                        jiraExecuteWorkflow(
                            site: 'https://soft-con.atlassian.net',
                            idOrKey: key,
                            workflowName: 'Done'
                        )
                    } catch (err) {
                        echo "Transition skipped for ${key}: ${err.message}"
                    }
                    jiraComment(
                        site: 'https://soft-con.atlassian.net',
                        issueKey: key,
                        body: "Build #${env.BUILD_NUMBER} PASSED — issue closed and moved to Done.\n${env.BUILD_URL}"
                    )
                }
            }
        }

        failure {
            script {
                def keys = (env.COMMIT_MSG =~ /SOFTCON-\d+/).collect { it }

                // On failure — move back to To Do (transition id: 11) + comment
                keys.each { key ->
                    try {
                        jiraExecuteWorkflow(
                            site: 'https://soft-con.atlassian.net',
                            idOrKey: key,
                            workflowName: 'To Do'
                        )
                    } catch (err) {
                        echo "Transition skipped for ${key}: ${err.message}"
                    }
                    jiraComment(
                        site: 'https://soft-con.atlassian.net',
                        issueKey: key,
                        body: "Build #${env.BUILD_NUMBER} FAILED — issue moved back to To Do.\n${env.BUILD_URL}"
                    )
                }
            }
        }
    }
}