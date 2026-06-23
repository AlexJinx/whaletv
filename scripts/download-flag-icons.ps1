param(
    [string] $OutputDir = "app/src/main/res/drawable-nodpi",
    [string] $Width = "80",
    [switch] $Force
)

$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$targetDir = Join-Path $root $OutputDir
New-Item -ItemType Directory -Force -Path $targetDir | Out-Null

$codesUrl = "https://flagcdn.com/en/codes.json"
$codes = Invoke-RestMethod -Uri $codesUrl
$downloaded = 0
$skipped = 0

$codes.PSObject.Properties |
    Sort-Object Name |
    ForEach-Object {
        $code = $_.Name.ToLowerInvariant()
        $resourceCode = $code -replace "[^a-z0-9_]", "_"
        $fileName = "flag_$resourceCode.webp"
        $targetPath = Join-Path $targetDir $fileName
        $url = "https://flagcdn.com/w$Width/$code.webp"

        try {
            if ((Test-Path $targetPath) -and -not $Force) {
                $skipped += 1
                Write-Host "exists $fileName"
                return
            }
            Invoke-WebRequest -Uri $url -OutFile $targetPath
            $downloaded += 1
            Write-Host "downloaded $fileName"
        } catch {
            $skipped += 1
            Write-Warning "skipped $code : $($_.Exception.Message)"
        }
    }

Write-Host "Flag download complete. downloaded=$downloaded skipped=$skipped output=$targetDir"
