## Introduction

This gradle project is intended for use by the 
[es-kotlin-wrapper-client](https://github.com/jillesvangurp/es-kotlin-wrapper-client) project. It works by
scanning the classes in the Elasticsearch Rest Highlevel client using reflection to generate some kotlin 
useful extension functions.

The wrapper client runs this plugin to get the generated code and adds additional packages that up. Unless you
know what you are doing and want to e.g. create a PR to change this, you should probably be using that.

## LICENSE

Licensed under the [MIT License](LICENSE).