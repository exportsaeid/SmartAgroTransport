plugins {
    id("com.android.application")
}

android {
    namespace = "com.example.SmartAgroTransport"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.SmartAgroTransport"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/LICENSE-notice.md"
            excludes += "/META-INF/NOTICE.md"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true  // ✅ فعال کردن Data Binding
    }
}

dependencies {
    implementation ("com.mikepenz:aboutlibraries:10.9.2")
    // کتابخانه‌های پایدار و رسمی
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment:2.5.3")
    implementation("androidx.navigation:navigation-ui:2.5.3")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    // Mail و WorkManager
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")
    implementation("androidx.work:work-runtime:2.9.0")
    // OSMDroid
    implementation("org.osmdroid:osmdroid-android:6.1.14")
    implementation("org.osmdroid:osmdroid-wms:6.1.10")
    // iText7 و ICU4J
    implementation("com.itextpdf:itext7-core:7.2.5")
    implementation("com.itextpdf:io:7.2.5")
    implementation("com.itextpdf:kernel:7.2.5")
    implementation("com.itextpdf:layout:7.2.5")
    implementation("com.ibm.icu:icu4j:74.2")
    // BCrypt
    implementation("org.mindrot:jbcrypt:0.4")
    // تست
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    // فایل‌های محلی
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
}
