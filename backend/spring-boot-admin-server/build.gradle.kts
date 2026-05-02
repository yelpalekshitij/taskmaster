plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation("de.codecentric:spring-boot-admin-starter-server:3.3.4")
    implementation("org.springframework.cloud:spring-cloud-starter-netflix-eureka-client")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("io.micrometer:micrometer-registry-prometheus")
}
