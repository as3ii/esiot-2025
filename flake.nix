{
  description = "ESIOT 2025/2026";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixos-25.11";
    flake-parts.url = "github:hercules-ci/flake-parts";
  };

  outputs =
    { flake-parts, ... }@inputs:
    flake-parts.lib.mkFlake { inherit inputs; } {
      systems = [
        "x86_64-linux"
        "aarch64-linux"
      ];

      perSystem =
        {
          config,
          pkgs,
          system,
          ...
        }:
        {
          formatter = pkgs.nixfmt-tree;

          devShells.default = pkgs.mkShell {
            packages = with pkgs; [
              glibc.dev
              clang-tools # clang-tidy, clang-format
              arduino-cli
              #arduino-ide # broken, see: https://github.com/NixOS/nixpkgs/issues/421018
              just
            ];

            shellHook = let
              ardu = pkgs.lib.getExe pkgs.arduino-cli;
            in ''
              ${ardu} update
              ${ardu} upgrade
              ${ardu} core update-index
              ${ardu} core install arduino:avr
              ${ardu} lib install liquidcrystal_i2c
              ${ardu} lib install enableinterrupt
              ${ardu} lib install timerone
              echo 1>&2 "Welcome to the development shell!"
            '';
          };
        };

      flake = { };
    };
}
