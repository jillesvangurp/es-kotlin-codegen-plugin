[![](https://jitpack.io/v/jillesvangurp/es-kotlin-codegen-plugin.svg)](https://jitpack.io/#jillesvangurp/es-kotlin-codegen-plugin)
[![Actions Status](https://github.com/jillesvangurp/es-kotlin-codegen-plugin/workflows/CI-gradle-build/badge.svg)](https://github.com/jillesvangurp/es-kotlin-codegen-plugin/actions)

## Introduction

This gradle project is intended for use by the 
[es-kotlin-wrapper-client](https://github.com/jillesvangurp/es-kotlin-wrapper-client) project. It works by
scanning the classes in the Elasticsearch Rest Highlevel client using reflection to generate some 
useful extension functions for Kotlin. Currently this is mainly limited to adding co-routine friendly versions of the 
many async functions in the Rest High Level Client. I may add some additional generated code in the future.

The build file of [es-kotlin-wrapper-client](https://github.com/jillesvangurp/es-kotlin-wrapper-client) runs this plugin to get the generated code so we can use that. Unless you
know what you are doing and want to e.g. create a PR to change the generated code, you should probably be using [es-kotlin-wrapper-client](https://github.com/jillesvangurp/es-kotlin-wrapper-client).

## Releases

Generally, I try to keep up with the stable Elasticsearch releases and tag releases from master. I use jitpack.io to distribute maven/gradle jars. Check the badge at the top of this page.

## LICENSE

Licensed under the [MIT License](LICENSE).
