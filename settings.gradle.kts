pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://maven.myket.ir")  // اول میرور ایرانی

        maven { url = uri("https://jitpack.io") }  // این خط رو اضافه کن
        maven {
            url = uri("https://dl.google.com/dl/android/maven2/")
        }
    }
}

rootProject.name = "invoiceapp"
include(":app")

