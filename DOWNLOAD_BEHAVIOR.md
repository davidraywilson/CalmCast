# CalmCast Download Behavior

This document describes the download functionality and expected user experience to ensure consistency with standard podcast app behavior.

## Overview

CalmCast uses the `AndroidDownloadManager` to handle podcast episode downloads. Downloads are stored locally in the app's files directory and tracked in a Room database.

## Download States

### DOWNLOADING
- Download is actively in progress
- Progress bar shows completion percentage
- User can pause or cancel
- Network requests are made with `Range` headers to support resuming from partial files

### DOWNLOADED
- Download completed successfully
- File is stored at: `{app.filesDir}/{episodeId}.mp3`
- Episode plays from local file instead of streaming
- File URI is stored in Download record for quick playback
- User can delete to free up space

### PAUSED
- Download was manually paused by user
- Partial file remains on disk
- User can resume to continue from the same position
- Progress is preserved

### FAILED
- Download encountered an error (network, disk space, etc.)
- Partial file is cleaned up
- User should retry

### CANCELED
- Download was manually canceled by user
- Partial file is deleted immediately
- Progress and byte tracking are cleared
- Download record is updated with status CANCELED

### DELETED
- User deleted the downloaded file
- Database record status changed to DELETED (not removed)
- File removed from disk
- Auto-download won't re-download this episode
- User can manually re-download (will replace DELETED record with DOWNLOADING)

## Auto-Download Behavior

### When Auto-Download Runs
1. **Daily periodic check** - WorkManager runs NewEpisodeWorker once per day
2. **On episode list refresh** - When user refreshes episodes manually for a podcast

### Auto-Download Decision Logic
- **Download** if: No download record exists for the latest episode (never attempted)
- **Skip** if: Status is DELETED (user explicitly deleted it)
- **Skip** if: Status is DOWNLOADING, DOWNLOADED, or PAUSED (already handled)
- **Skip** if: Status is FAILED (don't auto-retry failures)

## Expected Behavior

### Download Start
1. User taps download button on episode
2. `Download` record created with DOWNLOADING status and 0% progress
3. Network request begins with appropriate `Range` header if resuming
4. Download continues until complete or paused/canceled
5. Progress updates written to database on each buffer chunk

### Resume from Pause
1. App detects partial file exists
2. Network request includes `Range: bytes={fileSize}-` header
3. Download continues from byte offset, not from start
4. Progress is preserved and continues from where it paused
5. Final file size calculation includes all previously downloaded bytes

### Pause Download
1. `pausedDownloads` set gains the episodeId
2. Download loop checks this set on each iteration
3. When paused is detected, function returns with status = PAUSED
4. Partial file remains on disk for resume capability
5. Progress and byte counts are preserved

### Resume Download
1. App checks if download exists and status = PAUSED
2. Restarts download with `startDownload()`
3. Detects partial file and uses Range header
4. Download continues from byte offset
5. Final progress represents full file when complete

### Cancel Download
1. `pausedDownloads` set has episodeId removed (prevents blocking)
2. Active HTTP call is canceled
3. Active coroutine Job is canceled
4. Partial file on disk is immediately deleted
5. Download record updated to CANCELED with cleared byte tracking

### Delete Downloaded File
1. Both new-format (`{episodeId}.mp3`) and old-format (`{episodeTitle}.mp3`) files checked
2. Files deleted from disk
3. Download record status changed to DELETED (not removed)
4. Episode's downloadPath cleared
5. Next playback uses streaming instead
6. Auto-download won't re-download DELETED episodes
7. Manual re-download is always possible

## Database Schema

Download entity uses:
- `id` (PrimaryKey): episodeId (unique per episode)
- `episode`: Embedded Episode object with full metadata
- `status`: DownloadStatus enum (DOWNLOADING, DOWNLOADED, PAUSED, FAILED, CANCELED, DELETED)
- `progress`: Float 0.0-1.0 (or -1f for indeterminate)
- `downloadUri`: file:// URI for completed downloads
- `downloadedBytes`: Bytes downloaded so far (for resume tracking)
- `totalBytes`: Expected total file size (-1 if unknown)

## Implementation Details

### Resume Logic
- File existence check: `if (file.exists()) { bytesAlready = file.length() }`
- Range header: `Range: bytes={bytesAlready}-`
- Content-Length from server is bytes remaining (not total)
- Total calculation: `totalBytes = bytesAlready + serverBytes`
- FileOutputStream opened in append mode: `FileOutputStream(file, appendMode=true)`

### State Cleanup
- App startup calls `downloadDao.deleteInvalidDownloads()` to clean up corrupted records
- Finally blocks ensure `activeDownloads`, `activeCalls`, and `pausedDownloads` are cleaned
- Cancel operation clears all tracking for that episodeId

## User Expectations (Standard Podcast App Behavior)

1. **Downloaded files are available offline** - User can listen to downloaded episodes without internet
2. **Resume capability** - Paused downloads can be resumed without re-downloading
3. **Cancel cleans up** - Canceled downloads don't leave partial files
4. **Progress tracking** - User can see accurate progress percentage
5. **Automatic playback switch** - App plays from downloaded file if available, streams otherwise
6. **Delete frees space** - Deleting a download removes the file and allows re-downloading
7. **Respects deletion** - Auto-download won't re-download episodes user deleted
8. **Responsive auto-download** - Auto-download checks run when episodes are refreshed, not just once per day
