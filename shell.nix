{ pkgs ? import <nixpkgs> {} }:
let
  java25-package = pkgs.javaPackages.compiler.temurin-bin.jdk-25;
  java25-bin = pkgs.writeShellScriptBin "java25" ''
      exec ${java25-package}/bin/java "$@"
    '';
in
  pkgs.mkShellNoCC {
    packages = with pkgs; [
      java25-package
      java25-bin
      maven
    ];

    shellHook = ''
      JAVA_HOME=${java25-package}
      echo "❄️ NixShell is ready"
      echo "☕ Java $(java --version)"
      echo "📦 Maven $(mvn --version)"
    '';
  }