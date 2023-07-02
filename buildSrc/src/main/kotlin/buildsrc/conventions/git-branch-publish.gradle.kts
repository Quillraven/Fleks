package buildsrc.conventions

plugins {
    `maven-publish`
}

val githubPublishDir: Provider<File> =
    providers.environmentVariable("GIT_BRANCH_PUBLISH_DIR").map { file(it) }

publishing {
    repositories {
        maven(githubPublishDir) {
            name = "GitBranchPublish"
        }
    }
}
