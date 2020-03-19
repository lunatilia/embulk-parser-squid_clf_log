# Squid "Common/Combined Log Format (CLF)" Log parser plugin for Embulk

Embulk parser plugin for Squid CLF(Common/Combined Log Format) log.  
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
```
$ embulk gem install embulk-parser-squid_clf_log
```

## Build

```
$ cd embulk-parser-squid_clf_log
$ ./gradlew gem
```

## Supported log format

```
logformat common     %>a %[ui %[un [%tl] "%rm %ru HTTP/%rv" %>Hs %<st %Ss:%Sh
logformat combined   %>a %[ui %[un [%tl] "%rm %ru HTTP/%rv" %>Hs %<st "%{Referer}>h" "%{User-Agent}>h" %Ss:%Sh
```
