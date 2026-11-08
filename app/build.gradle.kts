import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.room)
}

// Assinatura de upload da Play Store (keystore.properties fica fora do Git).
val assinatura = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "br.com.viavalesistemas.manutencaobike"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "br.com.viavalesistemas.manutencaobike"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Chaves do Strava ficam no local.properties (fora do controle de versão)
        val localProps = Properties().apply {
            val file = rootProject.file("local.properties")
            if (file.exists()) file.inputStream().use { load(it) }
        }
        buildConfigField("String", "STRAVA_CLIENT_ID", "\"${localProps.getProperty("strava.clientId", "")}\"")
        // O client_secret do Strava fica só no servidor (servidor/manutencaobike_privado/config.php).
        buildConfigField(
            "String",
            "API_URL",
            "\"${localProps.getProperty("api.url", "https://api.viavalesistemas.com.br/manutencaobike/v1/")}\"",
        )
    }

    signingConfigs {
        if (assinatura.getProperty("storeFile") != null) {
            create("release") {
                storeFile = file(assinatura.getProperty("storeFile"))
                storePassword = assinatura.getProperty("storePassword")
                keyAlias = assinatura.getProperty("keyAlias")
                keyPassword = assinatura.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // java.time (LocalDate) em dispositivos com API < 26
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

// ./gradlew :app:notasPlayStore -> texto de "O que há de novo" da Play Store para a versão atual.
tasks.register("notasPlayStore") {
    description = "Gera as notas da Play Store a partir de src/main/assets/novidades.json."
    val entrada = file("src/main/assets/novidades.json")
    val saida = layout.buildDirectory.file("notas-play-store/pt-BR.txt")
    val codigo = android.defaultConfig.versionCode
    inputs.file(entrada)
    outputs.file(saida)
    doLast {
        @Suppress("UNCHECKED_CAST")
        val versoes = groovy.json.JsonSlurper().parse(entrada, "UTF-8") as List<Map<String, Any?>>
        val notas = versoes.firstOrNull { (it["codigo"] as Number).toInt() == codigo }
            ?: throw GradleException("novidades.json não tem a versão $codigo")
        fun secao(titulo: String, chave: String) =
            (notas[chave] as? List<*>).orEmpty().takeIf { it.isNotEmpty() }?.joinToString("\n", "$titulo\n") { "• $it" }
        val texto = listOfNotNull(
            secao("Novidades", "novidades"),
            secao("Correções", "correcoes"),
            secao("Melhorias", "melhorias"),
        ).joinToString("\n\n")
        saida.get().asFile.apply { parentFile.mkdirs(); writeText(texto, Charsets.UTF_8) }
        println(texto)
        println("\n${texto.length}/500 caracteres -> ${saida.get().asFile}")
        if (texto.length > 500) logger.warn("A Play Store aceita até 500 caracteres; resuma as notas desta versão.")
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp.logging)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.coil.compose)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
