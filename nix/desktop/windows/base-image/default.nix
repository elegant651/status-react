{ pkgs, stdenv, fetchurl }:

with pkgs;

stdenv.mkDerivation rec {
  name = "StatusIm-Windows-base-image";
  version = "20190515";

  src =
    if stdenv.hostPlatform.system == "x86_64-linux" then
      fetchurl {
        url = "https://desktop-app-files.ams3.digitaloceanspaces.com/${name}_${version}.zip";
        sha256 = "0wkq0khllms2hnbznb1j8l8yfw6z7phzrdg4ndyik20jkl0faj8f";
      }
    else throw "${name} is not supported on ${stdenv.hostPlatform.system}";

  nativeBuildInputs = [ unzip ];

  phases = [ "unpackPhase" "installPhase" ];
  unpackPhase = ''
    mkdir -p $out/src
    unzip $src -d $out/src
  '';
  installPhase = ''
    runHook preInstall

    echo $out
    ls $out -al

    runHook postInstall
  '';

  meta = {
    description = "A base image for Windows Status Desktop release distributions";
    homepage = https://desktop-app-files.ams3.digitaloceanspaces.com/;
    license = stdenv.lib.licenses.gpl3;
    maintainers = [ stdenv.lib.maintainers.pombeirp ];
    platforms = stdenv.lib.platforms.linux;
  };
}
