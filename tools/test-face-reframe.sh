#!/bin/sh
set -eu

root=$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd)
classes=$(mktemp -d)
trap 'rm -rf "$classes"' EXIT

javac -d "$classes" \
  "$root/android/src/main/java/io/livekit/reactnative/webrtc/FaceReframeGeometry.java" \
  "$root/tools/android/FaceReframeGeometryCheck.java"
java -cp "$classes" io.livekit.reactnative.webrtc.FaceReframeGeometryCheck
