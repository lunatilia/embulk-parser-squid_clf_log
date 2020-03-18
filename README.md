# Squid "Common/Combined Log Format (CLF)" Log parser plugin for Embulk

Embulk parser plugin for Squid CLF log (common/combined).  
- Forked from [embulk-parser-apache-log](https://github.com/hiroyuki-sato/embulk-parser-apache-log) (Author: [Hiroyuki Sato](https://github.com/hiroyuki-sato))

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

## Supported log format

```
logformat common     %>a %[ui %[un [%tl] "%rm %ru HTTP/%rv" %>Hs %<st %Ss:%Sh
logformat combined   %>a %[ui %[un [%tl] "%rm %ru HTTP/%rv" %>Hs %<st "%{Referer}>h" "%{User-Agent}>h" %Ss:%Sh
```
