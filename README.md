# Squid Common/Combined Log Format Log parser plugin for Embulk

TODO: Write short description here and build.gradle file.

## Overview

* **Plugin type**: parser
* **Guess supported**: no

## Configuration

- **format**: log format(common, combined) (string, default: combined)

## Example

```yaml
in:
  type: any file input plugin type
  parser:
    type: squid_clf_log
    format: common
```

## Build

```
$ cd embulk-parser-squid_clf_log
$ ./gradlew package
```
