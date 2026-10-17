param([switch]$CheckOnly)

$ErrorActionPreference = 'Stop'
$projectDirectory = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Set-Location -LiteralPath $projectDirectory

try {
    $candidates = [Collections.Generic.List[string]]::new()
    foreach ($candidate in @($env:JAVA_HOME, $env:JDK_HOME)) {
        if ($candidate) { $candidates.Add($candidate) }
    }
    foreach ($jdkDirectory in @(
        (Join-Path $env:USERPROFILE '.jdks'),
        (Join-Path $env:ProgramFiles 'Eclipse Adoptium'),
        (Join-Path $env:ProgramFiles 'Java'),
        (Join-Path $env:ProgramFiles 'Microsoft')
    )) {
        if (Test-Path -LiteralPath $jdkDirectory) {
            Get-ChildItem -LiteralPath $jdkDirectory -Directory | Sort-Object Name -Descending |
                ForEach-Object { $candidates.Add($_.FullName) }
        }
    }

    $selectedJdk = $null
    foreach ($candidate in $candidates) {
        $releaseFile = Join-Path $candidate 'release'
        if ((Test-Path -LiteralPath (Join-Path $candidate 'bin\java.exe')) -and
            (Test-Path -LiteralPath (Join-Path $candidate 'bin\javac.exe')) -and
            (Test-Path -LiteralPath $releaseFile) -and
            ([IO.File]::ReadAllText($releaseFile) -match '(?m)^JAVA_VERSION="21(?:\.|"|-)')) {
            $selectedJdk = $candidate
            break
        }
    }
    if (-not $selectedJdk) {
        throw 'Java JDK 21 was not found. Install JDK 21 or set JAVA_HOME to its folder.'
    }
    $env:JAVA_HOME = $selectedJdk
    Write-Host "Project: $projectDirectory"
    Write-Host "Java 21: $selectedJdk"
    if ($CheckOnly) {
        Write-Host 'Ready: gradlew.bat runClient --console=plain'
        exit 0
    }

    $sha = [Security.Cryptography.SHA256]::Create()
    try {
        $projectHash = [BitConverter]::ToString($sha.ComputeHash(
            [Text.Encoding]::UTF8.GetBytes($projectDirectory.ToLowerInvariant()))).Replace('-', '')
    } finally {
        $sha.Dispose()
    }
    $launchMutex = [Threading.Mutex]::new($false, "Local\CoolstuffClient_$projectHash")
    $acquired = $false
    try {
        try { $acquired = $launchMutex.WaitOne(0) }
        catch [Threading.AbandonedMutexException] { $acquired = $true }
        if (-not $acquired) {
            Write-Host 'A client is already starting or running through this launcher.'
            exit 0
        }
        Write-Host 'Building and starting Minecraft. Keep this console open while playing.'
        # runClient builds changed sources and prepares missing launch files automatically.
        & (Join-Path $projectDirectory 'gradlew.bat') runClient --console=plain
        $launchExitCode = $LASTEXITCODE
        if ($launchExitCode -ne 0) { throw "Gradle exited with code $launchExitCode." }
    } finally {
        if ($acquired) { $launchMutex.ReleaseMutex() }
        $launchMutex.Dispose()
    }
} catch {
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host 'Minecraft log: run\logs\latest.log'
    exit 1
}
