/*
 * Audio preview played directly from the song cards (and from the list view).
 * One shared Audio object: starting a song stops the previous one.
 * Buttons: <button data-songid="..." data-start="seconds" onclick="togglePlay(event, this)">
 * The labels (playerLabels.play / playerLabels.pause) are injected by index.html in the page language.
 */
(function () {
    var player = new Audio();
    player.preload = 'none';
    var currentSongId = null;

    function labels() {
        return window.playerLabels || {play: 'Play', pause: 'Pause'};
    }

    // Updates every play button: the one of the song currently playing shows "pause".
    function refreshButtons() {
        var playing = currentSongId !== null && !player.paused && !player.ended;
        document.querySelectorAll('.play-button, .play-button-small').forEach(function (btn) {
            var isCurrent = playing && btn.getAttribute('data-songid') === currentSongId;
            if (btn.classList.contains('playing') === isCurrent && btn.dataset.rendered) {
                return; // nothing changed for this button
            }
            btn.dataset.rendered = 'true';
            btn.classList.toggle('playing', isCurrent);
            var label = isCurrent ? labels().pause : labels().play;
            btn.title = label;
            btn.setAttribute('aria-label', label);
            // A new <i> is created each time, so it also works when Font Awesome replaces icons by SVG
            btn.innerHTML = '<i class="fa-solid ' + (isCurrent ? 'fa-pause' : 'fa-play') + '"></i>';
        });
    }

    function start(songId, startSeconds) {
        currentSongId = songId;
        player.pause();
        player.src = '/audio/' + encodeURIComponent(songId) + '.mp3';
        if (startSeconds > 0) {
            player.addEventListener('loadedmetadata', function () {
                if (isNaN(player.duration) || startSeconds < player.duration) {
                    player.currentTime = startSeconds;
                }
            }, {once: true});
        }
        var promise = player.play(); // called inside the click: allowed on iOS / Android
        if (promise && promise.catch) {
            promise.catch(function (e) {
                if (e && e.name !== 'AbortError') {
                    console.warn('Audio preview failed', e);
                }
                refreshButtons();
            });
        }
    }

    window.togglePlay = function (event, btn) {
        event.stopPropagation(); // do not open the song popup
        event.preventDefault();
        var songId = btn.getAttribute('data-songid');
        var startSeconds = parseFloat(btn.getAttribute('data-start')) || 0;
        if (songId === currentSongId && player.src && !player.ended) {
            if (player.paused) {
                player.play(); // resume where it was paused
            } else {
                player.pause();
            }
            return;
        }
        start(songId, startSeconds); // new song, or same song again after it ended
    };

    ['play', 'playing', 'pause', 'ended', 'error', 'emptied'].forEach(function (name) {
        player.addEventListener(name, refreshButtons);
    });
})();
