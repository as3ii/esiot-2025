{
  description = "ESIOT 2025/2026";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixos-25.11";
    flake-parts.url = "github:hercules-ci/flake-parts";
    git-hooks-nix = {
      url = "github:cachix/git-hooks.nix";
      inputs.nixpkgs.follows = "nixpkgs";
    };
  };

  outputs =
    { flake-parts, git-hooks-nix, ... }@inputs:
    flake-parts.lib.mkFlake { inherit inputs; } {
      imports = [ git-hooks-nix.flakeModule ];

      systems = [
        "x86_64-linux"
        "aarch64-linux"
      ];

      perSystem =
        {
          config,
          pkgs,
          lib,
          ...
        }:
        {
          formatter = pkgs.nixfmt-tree;

          pre-commit = {
            check.enable = true;

            settings = {
              addGcRoot = true;

              hooks = {
                # C/C++
                clang-format = {
                  enable = true;
                  types_or = lib.mkForce [
                    "c"
                    "c++"
                  ];
                };
                platformio-check = {
                  enable = true;
                  name = "Platformio check";
                  entry = "${pkgs.writeShellScript "pio-check.sh" ''
                    pushd "''${1%platformio.ini}" >/dev/null
                    ${lib.getExe pkgs.platformio-core} check
                    val="$?"
                    popd >/dev/null
                    exit "$val"
                  ''}";
                  files = "platformio.ini$";
                  pass_filenames = true;
                };
                # Misc
                check-added-large-files.enable = true;
                check-yaml.enable = true;
                detect-private-keys.enable = true;
                end-of-file-fixer.enable = true;
                ripsecrets.enable = true;
                trim-trailing-whitespace.enable = true;
                # Nix
                deadnix.enable = true;
                nil.enable = true;
                nixfmt.enable = true;
              };
            };
          };

          devShells.default = pkgs.mkShell {
            packages = with pkgs; [
              llvmPackages.clang-unwrapped # clang-tidy, clang-format
              arduino-cli
              #arduino-ide # broken, see: https://github.com/NixOS/nixpkgs/issues/421018
              platformio-core
              just
            ];

            shellHook =
              let
                ardu = lib.getExe pkgs.arduino-cli;
              in
              ''
                ${config.pre-commit.installationScript}
                ${ardu} update
                ${ardu} upgrade
                ${ardu} core update-index
                rm -v ~/.platformio/packages/tool-clangtidy/clang-tidy
                ln -v -s ${lib.getExe' pkgs.llvmPackages.clang-unwrapped "clang-tidy"} ~/.platformio/packages/tool-clangtidy/clang-tidy
                sed -i "s/\/bin\/bash/\/usr\/bin\/env bash/" ~/.platformio/packages/tool-avrdude/avrdude
                echo 1>&2 "In platformio projects remember to run 'pio run -t compiledb' to have clangd working correctly"
                echo 1>&2 "Welcome to the development shell!"
              '';
          };
        };

      flake = { };
    };
}
