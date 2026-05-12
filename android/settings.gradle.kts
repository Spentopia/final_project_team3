pluginManagement { // 이 블록 안의 내용이 시작됨
    repositories { // 이 블록 안의 내용이 시작됨
        google { // 이 블록 안의 내용이 시작됨
            content { // 이 블록 안의 내용이 시작됨
                includeGroupByRegex("com\\.android.*") // include Group By Regex 함수를 실행함
                includeGroupByRegex("com\\.google.*") // include Group By Regex 함수를 실행함
                includeGroupByRegex("androidx.*") // include Group By Regex 함수를 실행함
            }
        }
        mavenCentral() // maven Central 함수를 실행함
        gradlePluginPortal() // gradle Plugin Portal 함수를 실행함
    }
}
dependencyResolutionManagement { // 이 블록 안의 내용이 시작됨
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { // 이 블록 안의 내용이 시작됨
        google() // google 함수를 실행함
        mavenCentral() // maven Central 함수를 실행함
        maven { // 이 블록 안의 내용이 시작됨
            url = uri("https://devrepo.kakao.com/nexus/content/groups/public/") // url 값을 정해줌
        }
    }
}

rootProject.name = "Spentopia" // rootProject.name 값을 정해줌
include(":app") // include 함수를 실행함