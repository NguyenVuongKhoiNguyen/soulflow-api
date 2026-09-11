$CollectionUrl = "https://botanicaflower.vn/collections/orchid-en"

$OutputFolder = Join-Path $PSScriptRoot "Orchid"
# $ZipFile = Join-Path $PSScriptRoot "Botanica_Flower_Basket.zip"

$Headers = @{
    "User-Agent"      = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/152.0 Safari/537.36"
    "Accept-Language" = "en-US,en;q=0.9"
}

function Get-SafeFileName {
    param([string]$Name)

    foreach ($char in [System.IO.Path]::GetInvalidFileNameChars()) {
        $Name = $Name.Replace($char, "_")
    }

    return ($Name -replace '\s+', ' ').Trim()
}

function Get-NormalText {
    param([string]$Text)

    if ($null -eq $Text) {
        return ""
    }

    $Text = [System.Net.WebUtility]::HtmlDecode($Text)
    $Text = $Text -replace '<[^>]+>', ''
    $Text = $Text -replace '\s+', ' '

    return $Text.Trim()
}

function Get-AbsoluteUrl {
    param(
        [string]$Url,
        [string]$BaseUrl
    )

    if ([string]::IsNullOrWhiteSpace($Url)) {
        return $null
    }

    $Url = [System.Net.WebUtility]::HtmlDecode($Url.Trim())

    $Url = $Url -replace '\\/', '/'
    $Url = $Url -replace '\\u0026', '&'

    if ($Url.StartsWith("//")) {
        return "https:$Url"
    }

    if ($Url.StartsWith("/")) {
        try {
            return (
                [System.Uri]::new(
                    [System.Uri]$BaseUrl,
                    $Url
                )
            ).AbsoluteUri
        }
        catch {
            return $null
        }
    }

    if ($Url -match '^https?://') {
        return $Url
    }

    try {
        return (
            [System.Uri]::new(
                [System.Uri]$BaseUrl,
                $Url
            )
        ).AbsoluteUri
    }
    catch {
        return $null
    }
}

function Get-OriginalImageUrl {
    param([string]$Url)

    if ([string]::IsNullOrWhiteSpace($Url)) {
        return $null
    }

    $Url = $Url -replace '\?.*$', ''

    $Url = $Url -replace `
        '_(thumb|small|compact|medium|large|grande|master)(?=\.)',
        ''

    $Url = $Url -replace '_\d+x\d*(?=\.)', ''
    $Url = $Url -replace '_\d+x(?=\.)', ''
    $Url = $Url -replace '_x\d+(?=\.)', ''

    return $Url
}

function Get-AttributeValue {
    param(
        [string]$Tag,
        [string]$Attribute
    )

    $Pattern = '(?i)\b' +
        [regex]::Escape($Attribute) +
        '\s*=\s*["''](?<value>[^"'']*)["'']'

    $Match = [regex]::Match(
        $Tag,
        $Pattern
    )

    if ($Match.Success) {
        return $Match.Groups["value"].Value
    }

    return $null
}

function Get-ProductTitle {
    param(
        [string]$Html,
        [string]$ProductUrl
    )

    $Match = [regex]::Match(
        $Html,
        '(?is)<h1[^>]*>(?<title>.*?)</h1>'
    )

    if ($Match.Success) {

        $Title = Get-NormalText `
            $Match.Groups["title"].Value

        if ($Title) {
            return $Title
        }
    }

    $OgMatch = [regex]::Match(
        $Html,
        '(?is)<meta[^>]+property=["'']og:title["''][^>]+content=["''](?<title>[^"'']+)["'']'
    )

    if ($OgMatch.Success) {

        $Title = Get-NormalText `
            $OgMatch.Groups["title"].Value

        if ($Title) {
            return $Title
        }
    }

    return (
        $ProductUrl.TrimEnd("/") -split "/"
    )[-1]
}

function Get-ThumbnailGalleryImages {
    param(
        [string]$Html,
        [string]$ProductUrl,
        [string]$ProductTitle
    )

    $Images = New-Object `
        System.Collections.Generic.List[string]

    $TitleMatch = [regex]::Match(
        $Html,
        '(?is)<h1[^>]*>'
    )

    if ($TitleMatch.Success) {

        $BeforeTitle = $Html.Substring(
            0,
            $TitleMatch.Index
        )
    }
    else {
        $BeforeTitle = $Html
    }

    $ImgTags = [regex]::Matches(
        $BeforeTitle,
        '(?is)<img\b[^>]*>'
    )

    foreach ($ImgMatch in $ImgTags) {

        $Tag = $ImgMatch.Value

        $Alt = Get-AttributeValue `
            -Tag $Tag `
            -Attribute "alt"

        $Alt = Get-NormalText $Alt

        if ([string]::IsNullOrWhiteSpace($Alt)) {
            continue
        }

        if (
            $Alt.ToLowerInvariant() -ne
            $ProductTitle.ToLowerInvariant()
        ) {
            continue
        }

        $Url = $null

        $AttributesToTry = @(
            "data-zoom",
            "data-image",
            "data-original",
            "data-src",
            "src"
        )

        foreach ($Attribute in $AttributesToTry) {

            $Candidate = Get-AttributeValue `
                -Tag $Tag `
                -Attribute $Attribute

            if (
                -not [string]::IsNullOrWhiteSpace(
                    $Candidate
                )
            ) {
                $Url = $Candidate
                break
            }
        }

        if (-not $Url) {
            continue
        }

        if (
            $Url -notmatch
            '\.(jpg|jpeg|png|webp)(\?|$)'
        ) {
            continue
        }

        if (
            $Url -notmatch
            'hstatic\.net'
        ) {
            continue
        }

        $Url = Get-AbsoluteUrl `
            -Url $Url `
            -BaseUrl $ProductUrl

        if (-not $Url) {
            continue
        }

        $Url = Get-OriginalImageUrl $Url

        if (
            $Url -and
            -not $Images.Contains($Url)
        ) {
            $Images.Add($Url)
        }
    }

    if ($Images.Count -le 1) {

        $Images.Clear()

        foreach ($ImgMatch in $ImgTags) {

            $Tag = $ImgMatch.Value

            $Url = $null

            foreach (
                $Attribute in @(
                    "data-zoom",
                    "data-image",
                    "data-original",
                    "data-src",
                    "src"
                )
            ) {

                $Candidate = Get-AttributeValue `
                    -Tag $Tag `
                    -Attribute $Attribute

                if (
                    $Candidate -and
                    $Candidate -match
                    'cdn\.hstatic\.net|product\.hstatic\.net'
                ) {
                    $Url = $Candidate
                    break
                }
            }

            if (-not $Url) {
                continue
            }

            if (
                $Url -notmatch
                '\.(jpg|jpeg|png|webp)(\?|$)'
            ) {
                continue
            }

            $Url = Get-AbsoluteUrl `
                -Url $Url `
                -BaseUrl $ProductUrl

            $Url = Get-OriginalImageUrl $Url

            if (
                $Url -and
                -not $Images.Contains($Url)
            ) {
                $Images.Add($Url)
            }
        }
    }

    return @($Images)
}

function Download-Image {
    param(
        [string]$Url,
        [string]$Destination
    )

    try {

        Invoke-WebRequest `
            -Uri $Url `
            -Headers $Headers `
            -OutFile $Destination `
            -MaximumRedirection 10 `
            -ErrorAction Stop

        return $true
    }
    catch {

        Write-Warning "Failed: $Url"

        if (Test-Path $Destination) {
            Remove-Item `
                $Destination `
                -Force
        }

        return $false
    }
}

Write-Host ""
Write-Host "=========================================="
Write-Host " BOTANICA THUMBNAIL-ONLY DOWNLOADER"
Write-Host "=========================================="
Write-Host ""

if (Test-Path $OutputFolder) {

    Remove-Item `
        $OutputFolder `
        -Recurse `
        -Force
}

New-Item `
    -ItemType Directory `
    -Path $OutputFolder `
    -Force |
    Out-Null

Write-Host "Reading collection..."
Write-Host $CollectionUrl
Write-Host ""

try {

    $CollectionResponse = Invoke-WebRequest `
        -Uri $CollectionUrl `
        -Headers $Headers `
        -MaximumRedirection 10 `
        -ErrorAction Stop
}
catch {

    Write-Host `
        "Cannot open collection." `
        -ForegroundColor Red

    Write-Host $_.Exception.Message

    Read-Host "Press Enter"
    exit
}

$ProductMatches = [regex]::Matches(
    $CollectionResponse.Content,
    'href=["''](?<url>/products/[^"'']+)["'']',
    [System.Text.RegularExpressions.RegexOptions]::IgnoreCase
)

$ProductUrls = foreach (
    $Match in $ProductMatches
) {

    $RelativeUrl = `
        $Match.Groups["url"].Value

    $RelativeUrl = `
        $RelativeUrl.Split("?")[0]

    $RelativeUrl = `
        $RelativeUrl.Split("#")[0]

    Get-AbsoluteUrl `
        -Url $RelativeUrl `
        -BaseUrl $CollectionUrl
}

$ProductUrls = @(
    $ProductUrls |
    Where-Object { $_ } |
    Sort-Object -Unique
)

if ($ProductUrls.Count -eq 0) {

    Write-Host `
        "No products found." `
        -ForegroundColor Red

    Read-Host "Press Enter"
    exit
}

Write-Host `
    "Found $($ProductUrls.Count) products."

Write-Host ""

$ProductNumber = 0

foreach ($ProductUrl in $ProductUrls) {

    $ProductNumber++

    Write-Host "------------------------------------------"

    Write-Host `
        "[$ProductNumber/$($ProductUrls.Count)]"

    Write-Host $ProductUrl

    try {

        $ProductResponse = Invoke-WebRequest `
            -Uri $ProductUrl `
            -Headers $Headers `
            -MaximumRedirection 10 `
            -ErrorAction Stop
    }
    catch {

        Write-Warning `
            "Cannot open product."

        continue
    }

    $Html = $ProductResponse.Content

    $ProductTitle = Get-ProductTitle `
        -Html $Html `
        -ProductUrl $ProductUrl

    $ProductTitle = `
        Get-SafeFileName $ProductTitle

    Write-Host `
        "Product: $ProductTitle"

    $GalleryImages = @(
        Get-ThumbnailGalleryImages `
            -Html $Html `
            -ProductUrl $ProductUrl `
            -ProductTitle $ProductTitle
    )

    Write-Host `
        "Thumbnail images: $($GalleryImages.Count)"

    if ($GalleryImages.Count -eq 0) {

        Write-Warning `
            "No thumbnail images detected."

        continue
    }

    $ProductFolder = Join-Path `
        $OutputFolder `
        $ProductTitle

    New-Item `
        -ItemType Directory `
        -Path $ProductFolder `
        -Force |
        Out-Null

    $ImageNumber = 0

    foreach ($ImageUrl in $GalleryImages) {

        $ImageNumber++

        try {

            $Uri = [System.Uri]$ImageUrl

            $Extension = `
                [System.IO.Path]::GetExtension(
                    $Uri.AbsolutePath
                )

            if (
                $Extension -notmatch
                '^\.(jpg|jpeg|png|webp)$'
            ) {
                $Extension = ".jpg"
            }
        }
        catch {
            $Extension = ".jpg"
        }

        $FileName = `
            "{0:D2}{1}" -f `
            $ImageNumber,
            $Extension

        $Destination = Join-Path `
            $ProductFolder `
            $FileName

        Write-Host `
            "  Downloading $FileName"

        Write-Host `
            "    $ImageUrl"

        Download-Image `
            -Url $ImageUrl `
            -Destination $Destination |
            Out-Null

        Start-Sleep `
            -Milliseconds 150
    }

    Start-Sleep `
        -Milliseconds 250
}

Write-Host ""
Write-Host "=========================================="
Write-Host " FINISHED" -ForegroundColor Green
Write-Host "=========================================="
Write-Host ""

Write-Host "Images saved to:"
Write-Host $OutputFolder
Write-Host ""

Read-Host "Press Enter to close"