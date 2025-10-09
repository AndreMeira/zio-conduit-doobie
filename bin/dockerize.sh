#!/bin/bash

sbt Universal/stage
docker build . -t com.conduit/1.0
