# Compatible with Windows PowerShell 5.1 and PowerShell on Windows.
# Secrets never leave this process except through the private dotenv file.
#Requires -Version 5.1
$ErrorActionPreference = 'Stop'

function Get-ExistingConfig {
    param([string] $Path)
    # GetAttributes also detects dangling reparse points; Test-Path alone does not.
    try { $attributes = [System.IO.File]::GetAttributes($Path) }
    catch [System.IO.FileNotFoundException] { return $false }
    catch [System.IO.DirectoryNotFoundException] { return $false }
    if (($attributes -band [System.IO.FileAttributes]::ReparsePoint) -ne 0) {
        throw 'Se rechaza .env: es un reparse point o enlace simbolico.'
    }
    if (($attributes -band [System.IO.FileAttributes]::Directory) -ne 0) {
        throw '.env debe ser un archivo regular.'
    }
    return $true
}

function Read-AdminPassword {
    $first = $null
    $second = $null
    $firstPointer = [IntPtr]::Zero
    $secondPointer = [IntPtr]::Zero
    try {
        $first = Read-Host 'Contrasena admin (minimo 12 caracteres)' -AsSecureString
        $second = Read-Host 'Confirmar contrasena' -AsSecureString
        $firstPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($first)
        $secondPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($second)
        $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($firstPointer)
        $confirmation = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($secondPointer)
        if ($password -cne $confirmation) { throw 'Las contrasenas no coinciden.' }
        if ($password.Length -lt 12) { throw 'La contrasena requiere al menos 12 caracteres.' }
        if ($password.IndexOfAny([char[]] "`r`n`0") -ge 0) { throw 'La contrasena contiene caracteres no permitidos.' }
        return $password
    }
    finally {
        if ($firstPointer -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($firstPointer) }
        if ($secondPointer -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($secondPointer) }
        if ($null -ne $first) { $first.Dispose() }
        if ($null -ne $second) { $second.Dispose() }
        $password = $null
        $confirmation = $null
    }
}

function New-RandomSecret {
    $bytes = New-Object byte[] 32
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $rng.GetBytes($bytes)
        return [BitConverter]::ToString($bytes).Replace('-', '').ToLowerInvariant()
    }
    finally { $rng.Dispose(); [Array]::Clear($bytes, 0, $bytes.Length) }
}

function New-PrivateConfigStream {
    param([string] $Path)
    $sid = [Security.Principal.WindowsIdentity]::GetCurrent().User
    $acl = New-Object Security.AccessControl.FileSecurity
    $acl.SetOwner($sid)
    $acl.SetAccessRuleProtection($true, $false)
    $rule = New-Object Security.AccessControl.FileSystemAccessRule($sid, 'FullControl', 'Allow')
    $acl.AddAccessRule($rule)
    # Apply the protected ACL at CreateNew, never after writing secret bytes.
    if ($PSVersionTable.PSEdition -eq 'Desktop') {
        return New-Object System.IO.FileStream($Path, [IO.FileMode]::CreateNew,
            [Security.AccessControl.FileSystemRights]::FullControl, [IO.FileShare]::None,
            4096, [IO.FileOptions]::None, $acl)
    }
    return [IO.FileSystemAclExtensions]::Create([IO.FileInfo]::new($Path),
        [IO.FileMode]::CreateNew, [Security.AccessControl.FileSystemRights]::FullControl,
        [IO.FileShare]::None, 4096, [IO.FileOptions]::None, $acl)
}

$names = @('DB_PASSWORD', 'RABBITMQ_PASSWORD', 'JWT_SECRET', 'CORE_MACHINE_SECRET',
    'BOOTSTRAP_ADMIN_PASSWORD', 'COMPOSE_ENV_FILES', 'COMPOSE_DISABLE_ENV_FILE')
$savedEnvironment = @{}
$previousLocation = Get-Location
$tempPath = $null
$stream = $null
$exitCode = 1
try {
    Set-Location -LiteralPath $PSScriptRoot
    $root = [IO.Path]::GetFullPath($PSScriptRoot)
    $configPath = [IO.Path]::Combine($root, '.env')
    $docker = Get-Command docker -CommandType Application -ErrorAction SilentlyContinue
    if ($null -eq $docker) { throw 'Docker no esta instalado.' }
    & $docker.Source info *> $null
    if ($LASTEXITCODE -ne 0) { throw 'Docker no esta disponible; inicia el daemon.' }
    & $docker.Source compose version *> $null
    if ($LASTEXITCODE -ne 0) { throw 'Se requiere Docker Compose v2.' }

    foreach ($name in $names) {
        $savedEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
        [Environment]::SetEnvironmentVariable($name, $null, 'Process')
    }
    if (-not (Get-ExistingConfig $configPath)) {
        $password = Read-AdminPassword
        try {
            $lines = foreach ($name in $names[0..3]) { $name + '=' + (New-RandomSecret) }
            # Compose double-quoted dotenv: escape backslash, quote, then dollar.
            $escaped = $password.Replace('\', '\\').Replace('"', '\"').Replace('$', '$$')
            $content = ($lines -join "`n") + "`nBOOTSTRAP_ADMIN_PASSWORD=" + '"' + $escaped + '"' + "`n"
            $candidate = [IO.Path]::Combine($root, '.start.' + [Guid]::NewGuid().ToString('N') + '.tmp')
            $stream = New-PrivateConfigStream $candidate
            # Only a successfully created file belongs to our cleanup.
            $tempPath = $candidate
            $bytes = [Text.UTF8Encoding]::new($false).GetBytes($content)
            try { $stream.Write($bytes, 0, $bytes.Length); $stream.Flush($true) }
            finally { [Array]::Clear($bytes, 0, $bytes.Length); $stream.Dispose(); $stream = $null }
            # Same-volume atomic rename; the two-argument Move NEVER replaces a path.
            [IO.File]::Move($tempPath, $configPath)
            $tempPath = $null
        }
        finally { $password = $null; $escaped = $null; $content = $null; $lines = $null }
    }
    # Recheck before Compose, including existing configurations.
    if (-not (Get-ExistingConfig $configPath)) { throw '.env no esta disponible.' }
    & $docker.Source compose up --build --wait --wait-timeout 300
    $exitCode = $LASTEXITCODE
    if ($exitCode -eq 0) { [Console]::WriteLine('La aplicacion ya esta corriendo en http://localhost:5173') }
}
catch {
    # Never print exception details: native/IO diagnostics might contain secrets.
    [Console]::Error.WriteLine('No se pudo iniciar. Verifica Docker, Compose y .env; la contrasena debe coincidir y tener al menos 12 caracteres.')
    $exitCode = 1
}
finally {
    try {
        if ($null -ne $stream) { $stream.Dispose() }
        if ($null -ne $tempPath) {
            # The path is set only after successful CreateNew, in the repository root.
            if ([IO.Path]::GetDirectoryName($tempPath) -eq $root -and
                [IO.Path]::GetFileName($tempPath) -match '^\.start\.[0-9a-f]{32}\.tmp$') {
                [IO.File]::Delete($tempPath)
            }
        }
    }
    catch {
        [Console]::Error.WriteLine('No se pudo limpiar el archivo temporal privado.')
        $exitCode = 1
    }
    finally {
        # Restoration must run even when cleanup fails.
        try {
            foreach ($name in $savedEnvironment.Keys) {
                [Environment]::SetEnvironmentVariable($name, $savedEnvironment[$name], 'Process')
            }
        }
        finally { Set-Location -LiteralPath $previousLocation.Path }
    }
}
exit $exitCode
