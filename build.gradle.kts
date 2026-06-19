plugins {
    buildsrc.plugins.`kmp-js` apply false
    buildsrc.plugins.`kmp-jvm` apply false
    buildsrc.plugins.`kmp-native` apply false
    buildsrc.plugins.publishing apply false
    buildsrc.plugins.benchmark apply false
    id("org.jetbrains.dokka") apply false
}

group = ProjectInfo.GROUP
version = ProjectInfo.VERSION
