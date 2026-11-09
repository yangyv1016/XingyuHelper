plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "studio.astratale.xingyuhelper"
    compileSdk = 34
    defaultConfig { applicationId = "studio.astratale.xingyuhelper"; minSdk = 30; targetSdk = 34; versionCode = 1; versionName = "0.1.0" }
    buildTypes { release { isMinifyEnabled = true; signingConfig = signingConfigs.getByName("debug"); proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt")) } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}

val bootstrapSdk = androidComponents.sdkComponents.sdkDirectory
val compileWifiBootstrap by tasks.registering(JavaCompile::class) {
    source(rootProject.file("bootstrap"))
    classpath = files(bootstrapSdk.map { it.file("platforms/android-${android.compileSdk}/android.jar") })
    destinationDirectory.set(layout.buildDirectory.dir("intermediates/wifi-bootstrap"))
    options.release.set(8)
}

tasks.register<Exec>("assembleWifiBootstrap") {
    val classes = compileWifiBootstrap.flatMap { it.destinationDirectory }
    val archive = layout.buildDirectory.file("outputs/bootstrap/XingyuWifiBootstrap.jar")
    dependsOn(compileWifiBootstrap)
    inputs.dir(classes)
    outputs.file(archive)
    doFirst {
        val sdk = bootstrapSdk.get().asFile
        val tool = File(sdk, "build-tools/${android.buildToolsVersion}/d8")
        val output = archive.get().asFile
        output.parentFile.mkdirs()
        val compiler = if (System.getProperty("os.name").startsWith("Windows")) listOf("cmd.exe", "/c", "${tool.absolutePath}.bat")
            else listOf(tool.absolutePath)
        commandLine(compiler + listOf("--min-api", "30", "--lib", File(sdk, "platforms/android-${android.compileSdk}/android.jar").absolutePath,
            "--output", output.absolutePath) + classes.get().asFile.walkTopDown().filter { it.extension == "class" }.map { it.absolutePath }.toList())
    }
}
