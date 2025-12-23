// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("dev.nx.gradle.project-graph") version("0.1.10")
    alias(shared.plugins.android.application) apply false
    alias(shared.plugins.kotlin.android) apply false
    alias(shared.plugins.kotlin.compose) apply false
}
allprojects {
    apply {
        plugin("dev.nx.gradle.project-graph")
    }
}