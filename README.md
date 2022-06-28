# Button

![Button Status](https://healthchecks.io/badge/7ab6eb12-90e4-43cd-9f37-16d2d1/JEbd2RgO-2/button.svg)

This is a dumb website that lets you press a button and tells you how
many people are currently also pressing that button.

https://button.zachwal.sh

![big red button](button.png)

## How does it work

- [ktor](https://ktor.io/) - kotlin-native webserver
- web sockets
- coroutines
  - turns out coroutines + web sockets is a really nice pattern

## Why

- I wanted to learn a bit more about the practical uses of coroutines
for backend web dev
- I wanted to play with web sockets a bit more
- Also for https://github.com/suzannex
