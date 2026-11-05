<#
.SYNOPSIS
    Builds a self-contained Windows executable of PDFsam Basic (dist\pdfsam\pdfsam.exe).

.DESCRIPTION
    Unlike the upstream release profile, this needs no code signing, WiX or MSIX tooling.
    It builds the Maven modules, links a private Java runtime that includes JavaFX
    (whose Windows UI Automation bridge is what screen readers such as NVDA use),
    and packages everything with jpackage as an app image.

    Requirements: JDK 25 (with jlink/jpackage) and Maven on PATH.
    GNU gettext (msgfmt) is needed for translated UI languages; without it only English is built.
    JavaFX jmods are downloaded automatically if missing.

.EXAMPLE
    ./build-exe.ps1
    ./build-exe.ps1 -SkipMaven   # repackage without rebuilding the Java modules
#>
param(
    [string]$JavaFxJmods = "C:\tools\javafx-jmods-25.0.4",
    [string]$JavaFxVersion = "25.0.4",
    [string]$OutDir = "$PSScriptRoot\dist",
    [switch]$SkipMaven,
    [switch]$RunTests
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$work = Join-Path $root "pdfsam-basic\target\exe-build"

function Invoke-Checked([string]$exe, [string[]]$arguments) {
    & $exe @arguments
    if ($LASTEXITCODE -ne 0) { throw "$exe failed with exit code $LASTEXITCODE" }
}

# msgfmt is needed by the gettext plugin to compile the translations
if (-not (Get-Command msgfmt -ErrorAction SilentlyContinue)) {
    $candidates = @(
        "$env:LOCALAPPDATA\Programs\gettext-iconv\bin",
        "C:\Program Files\gettext-iconv\bin"
    )
    $found = $candidates | Where-Object { Test-Path (Join-Path $_ "msgfmt.exe") } | Select-Object -First 1
    if ($found) {
        $env:PATH = "$found;$env:PATH"
    } else {
        Write-Warning "msgfmt not found: only English will be available. Install with: winget install mlocati.GetText"
    }
}

if (-not (Test-Path (Join-Path $JavaFxJmods "javafx.graphics.jmod"))) {
    Write-Host "Downloading JavaFX $JavaFxVersion jmods..."
    $zip = Join-Path $env:TEMP "openjfx-$JavaFxVersion-jmods.zip"
    Invoke-WebRequest "https://download2.gluonhq.com/openjfx/$JavaFxVersion/openjfx-${JavaFxVersion}_windows-x64_bin-jmods.zip" -OutFile $zip
    Expand-Archive $zip -DestinationPath (Split-Path $JavaFxJmods -Parent) -Force
    Remove-Item $zip
}

if (-not $SkipMaven) {
    $mvnArgs = @('-B', 'clean', 'install')
    if (-not $RunTests) { $mvnArgs += '-DskipTests' }
    Invoke-Checked mvn $mvnArgs
}

$version = (Select-Xml -Path "$root\pdfsam-basic\pom.xml" -Namespace @{m = 'http://maven.apache.org/POM/4.0.0' } -XPath '/m:project/m:parent/m:version').Node.InnerText
$appVersion = ($version -split '-')[0]

Remove-Item $work -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force "$work\lib" | Out-Null

# Application modules. JavaFX comes from the jmods linked into the runtime instead of the Maven jars.
Copy-Item "$root\pdfsam-basic\target\pdfsam-basic-$version.jar" "$work\lib\"
Push-Location "$root\pdfsam-basic"
try {
    Invoke-Checked mvn @('-B', '-q', 'dependency:copy-dependencies', "-DoutputDirectory=$work\lib", '-DincludeScope=runtime', '-DexcludeGroupIds=org.openjfx')
} finally { Pop-Location }

Write-Host "Linking Java runtime..."
Invoke-Checked jlink @(
    '--module-path', $JavaFxJmods,
    '--add-modules', 'java.base,java.logging,java.naming,java.sql,java.desktop,java.xml,java.management,jdk.unsupported,java.prefs,jdk.localedata,javafx.controls,javafx.base,javafx.graphics,javafx.media',
    '--include-locales=en-*,af,ar,bg,bs,ca,co,cs,da,de,el,es,et,eu,fi,fr,he,hi,hu,hr,ko,ja,it,lo,lt,lv,ms,nl,nb,oc,pt-*,pl,pt,ro,ru,sk,sl,sr,sv,tr,th,zh-*,uk',
    '--output', "$work\runtime",
    '--compress', 'zip-6', '--no-header-files', '--no-man-pages', '--strip-debug'
)

Write-Host "Packaging executable..."
Remove-Item (Join-Path $OutDir "pdfsam") -Recurse -Force -ErrorAction SilentlyContinue
Invoke-Checked jpackage @(
    '-t', 'app-image',
    '-d', $OutDir,
    '-n', 'pdfsam',
    '--runtime-image', "$work\runtime",
    '--module-path', "$work\lib",
    '-m', 'org.pdfsam.basic/org.pdfsam.basic.App',
    '--app-version', $appVersion,
    '--description', 'Open source desktop application to split, merge, rotate, extract pages and mix PDF files',
    '--vendor', 'Sober Lemur S.r.l.',
    '--icon', "$root\pdfsam-basic\src\assembly\resources\pdfsam.ico",
    '--app-content', "$root\pdfsam-basic\LICENSE.txt",
    '--java-options', '--enable-preview -Dprism.lcdtext=false -Djava.library.path=$APPDIR\..\runtime\bin'
)

Write-Host ""
Write-Host "Done: $(Join-Path $OutDir 'pdfsam\pdfsam.exe')"
