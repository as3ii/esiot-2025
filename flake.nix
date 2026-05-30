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
        let
          jdk = pkgs.jdk21;
          gradle = pkgs.gradle.override { java = jdk; };
        in
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
                just-check = {
                  enable = true;
                  name = "Just check";
                  extraPackages = with pkgs; [
                    arduino-cli
                    llvmPackages.clang-unwrapped
                    gradle
                    just
                  ];
                  entry = "${pkgs.writeShellScript "just-check.sh" ''
                    res=0
                    while read -r i; do
                        printf "\nChecking: ''${i%justfile}\n\n"
                        pushd "''${i%justfile}" >/dev/null
                        ${lib.getExe pkgs.just} check || res=1
                        popd >/dev/null
                    done << EOF
                        $(find . -maxdepth 2 -name "justfile")
                    EOF
                    exit ''$res
                  ''}";
                  pass_filenames = false;
                  always_run = true;
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
            packages =
              with pkgs;
              [
                llvmPackages.clang-unwrapped # clang-tidy, clang-format
                arduino-cli
                #arduino-ide # broken, see: https://github.com/NixOS/nixpkgs/issues/421018
                platformio-core
                just
              ]
              ++ [
                jdk
                gradle
              ];

            shellHook = ''
              ${config.pre-commit.installationScript}
              export _JAVA_OPTIONS='-Dawt.useSystemAAFontSettings=lcd'
              rm -v ~/.platformio/packages/tool-clangtidy/clang-tidy
              ln -v -s ${lib.getExe' pkgs.llvmPackages.clang-unwrapped "clang-tidy"} ~/.platformio/packages/tool-clangtidy/clang-tidy
              sed -i "s/\/bin\/bash/\/usr\/bin\/env bash/" ~/.platformio/packages/tool-avrdude/avrdude
              echo 1>&2 "In platformio projects remember to run 'pio run -t compiledb' to have clangd working correctly"
              echo 1>&2 "Useful java environment variables: GDK_SCALE=2 _JAVA_AWT_WM_NONREPARENTING=1"
            '';
          };
        };

      flake = { };
    };
}
