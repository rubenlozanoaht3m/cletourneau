import com.qmuiteam.plugin.Dep

plugins {
    `java-library`
    kotlin("jvm")
    `maven-publish`
    id("qmui-publish")
}

version = Dep.QMUI.archVer

java {
    sourceCompatibility = Dep.javaVersion
    targetCompatibility = Dep.javaVersion
}