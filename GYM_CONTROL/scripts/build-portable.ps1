$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$jdkRoot = $null
$candidates = @()

if ($env:JAVA_HOME) {
    $candidates += $env:JAVA_HOME
}
if (Test-Path -LiteralPath "$env:ProgramFiles\Java") {
    $candidates += Get-ChildItem -LiteralPath "$env:ProgramFiles\Java" -Directory |
        Where-Object { $_.Name -match '^jdk-?21' } |
        Sort-Object Name -Descending |
        ForEach-Object FullName
}

foreach ($candidate in $candidates) {
    if ((Test-Path -LiteralPath (Join-Path $candidate 'bin\javac.exe')) -and
        (Test-Path -LiteralPath (Join-Path $candidate 'bin\jar.exe'))) {
        $version = & (Join-Path $candidate 'bin\javac.exe') --version 2>&1 | Out-String
        if ($version -match '\b21\.') {
            $jdkRoot = $candidate
            break
        }
    }
}

if (-not $jdkRoot) {
    throw 'No encontré un JDK 21. Instala JDK 21 o configura JAVA_HOME.'
}

$javac = Join-Path $jdkRoot 'bin\javac.exe'
$jar = Join-Path $jdkRoot 'bin\jar.exe'
$sqlite = Join-Path $projectRoot 'lib\sqlite-jdbc.jar'
$sourceRoot = Join-Path $projectRoot 'src'
$resourceRoot = Join-Path $projectRoot 'recursos'
$outputRoot = Join-Path $projectRoot 'dist'
$outputJar = Join-Path $outputRoot 'GYM_CONTROL_portable.jar'
$buildRoot = Join-Path ([IO.Path]::GetTempPath()) ('gym-control-build-' + [guid]::NewGuid().ToString('N'))
$classes = Join-Path $buildRoot 'classes'
$bundle = Join-Path $buildRoot 'bundle'
$manifest = Join-Path $buildRoot 'manifest.mf'

if (-not (Test-Path -LiteralPath $sqlite)) { throw 'No encontré lib\sqlite-jdbc.jar.' }
if (-not (Test-Path -LiteralPath $resourceRoot)) { throw 'No encontré la carpeta recursos.' }

New-Item -ItemType Directory -Path $classes, $bundle, $outputRoot -Force | Out-Null
$sources = Get-ChildItem -LiteralPath $sourceRoot -Recurse -Filter '*.java' | ForEach-Object FullName
& $javac --release 21 -encoding UTF-8 -d $classes $sources
if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación Java.' }

Copy-Item -Path (Join-Path $resourceRoot '*') -Destination $classes -Recurse -Force
Copy-Item -Path (Join-Path $classes '*') -Destination $bundle -Recurse -Force

Push-Location $bundle
try {
    & $jar xf $sqlite
    if ($LASTEXITCODE -ne 0) { throw 'No pude extraer el controlador SQLite.' }
} finally {
    Pop-Location
}

$embeddedSources = Join-Path $bundle 'META-INF\sources'
$embeddedLicenses = Join-Path $bundle 'META-INF\licenses'
New-Item -ItemType Directory -Path $embeddedSources, $embeddedLicenses -Force | Out-Null
Copy-Item -Path (Join-Path $sourceRoot '*') -Destination $embeddedSources -Recurse -Force
Copy-Item -LiteralPath (Join-Path $projectRoot 'lib\LICENSE-sqlite-jdbc.txt') -Destination $embeddedLicenses -Force
Copy-Item -LiteralPath (Join-Path $projectRoot 'lib\LICENSE-zentus.txt') -Destination $embeddedLicenses -Force

$oldManifest = Join-Path $bundle 'META-INF\MANIFEST.MF'
if (Test-Path -LiteralPath $oldManifest) { Remove-Item -LiteralPath $oldManifest -Force }
Get-ChildItem -LiteralPath (Join-Path $bundle 'META-INF') -Recurse -File |
    Where-Object { $_.Extension -in '.SF', '.RSA', '.DSA' } |
    Remove-Item -Force
Set-Content -LiteralPath $manifest -Encoding ascii -Value @(
    'Manifest-Version: 1.0'
    'Main-Class: aplicacion.App'
    'Multi-Release: true'
    ''
)

if (Test-Path -LiteralPath $outputJar) { Remove-Item -LiteralPath $outputJar -Force }
& $jar cfm $outputJar $manifest -C $bundle .
if ($LASTEXITCODE -ne 0) { throw 'Falló la creación del JAR portátil.' }

Write-Output "JAR portátil creado: $outputJar"
Write-Output 'Incluye la aplicación, SQLite JDBC, recursos, licencias y fuentes Java.'
$tempBase = [IO.Path]::GetFullPath([IO.Path]::GetTempPath())
$buildFull = [IO.Path]::GetFullPath($buildRoot)
if ($buildFull.StartsWith($tempBase, [StringComparison]::OrdinalIgnoreCase) -and
    (Test-Path -LiteralPath $buildFull)) {
    Remove-Item -LiteralPath $buildFull -Recurse -Force
}
