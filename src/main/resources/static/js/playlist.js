/*
 * "+" button of the song cards: shows the list of playlists, a click on one adds the song to it.
 * A click anywhere else (or the Escape key) closes the list.
 * Texts (playlistLabels) are injected by index.html in the page language.
 */
(function () {
    var menu = null;          // the floating list, created once
    var menuSongId = null;    // song the list is currently open for
    var menuButton = null;    // "+" button the list is attached to

    function labels() {
        return window.playlistLabels || {
            added: 'Added to "{0}"', already: 'Already in "{0}"', error: 'Could not add the song',
            loading: 'Loading...', choose: 'Choose a playlist'
        };
    }

    function format(text, name) {
        return text.replace('{0}', name);
    }

    // Short message at the bottom of the screen, disappears by itself
    var toastTimer = null;
    function showToast(text, isError) {
        var toast = document.getElementById('playlistToast');
        if (!toast) {
            toast = document.createElement('div');
            toast.id = 'playlistToast';
            toast.className = 'playlist-toast';
            toast.setAttribute('role', 'status');
            document.body.appendChild(toast);
        }
        toast.textContent = text;
        toast.classList.toggle('error', !!isError);
        toast.classList.add('visible');
        clearTimeout(toastTimer);
        toastTimer = setTimeout(function () { toast.classList.remove('visible'); }, 2500);
    }

    function closeMenu() {
        if (menu) {
            menu.hidden = true;
        }
        if (menuButton) {
            menuButton.classList.remove('open');
        }
        menuSongId = null;
        menuButton = null;
    }

    function getMenu() {
        if (!menu) {
            menu = document.createElement('div');
            menu.className = 'playlist-menu';
            menu.hidden = true;
            // clicks inside the list must not reach the page (e.g. the popup "click outside" handler)
            menu.addEventListener('click', function (e) { e.stopPropagation(); });
            document.body.appendChild(menu);
        }
        return menu;
    }

    // Places the list next to the "+" button: above it if there is room, otherwise below
    function placeMenu() {
        if (!menu || menu.hidden || !menuButton) {
            return;
        }
        var r = menuButton.getBoundingClientRect();
        var w = menu.offsetWidth, h = menu.offsetHeight;
        var left = Math.max(8, Math.min(r.right - w, document.documentElement.clientWidth - w - 8));
        var top = r.top - h - 8;
        if (top < 8) {
            top = r.bottom + 8;
        }
        menu.style.left = (left + window.scrollX) + 'px';
        menu.style.top = (top + window.scrollY) + 'px';
    }

    function renderItems(songId, playlists) {
        var m = getMenu();
        m.innerHTML = '';
        var title = document.createElement('div');
        title.className = 'playlist-menu-title';
        title.textContent = labels().choose;
        m.appendChild(title);
        playlists.forEach(function (pl) {
            var item = document.createElement('button');
            item.type = 'button';
            item.className = 'playlist-menu-item' + (pl.contains ? ' contains' : '');
            item.textContent = pl.name;
            if (pl.contains) {
                var check = document.createElement('i');
                check.className = 'fa-solid fa-check';
                item.appendChild(check);
            }
            item.addEventListener('click', function () {
                closeMenu();
                if (pl.contains) {
                    showToast(format(labels().already, pl.name));
                } else {
                    window.addSongToPlaylist(songId, pl.id, pl.name);
                }
            });
            m.appendChild(item);
        });
        placeMenu();
    }

    window.togglePlaylistMenu = function (event, btn) {
        event.stopPropagation(); // do not open the song popup
        event.preventDefault();
        var songId = btn.getAttribute('data-songid');
        if (menuSongId === songId && menu && !menu.hidden) {
            closeMenu();
            return;
        }
        closeMenu();
        menuSongId = songId;
        menuButton = btn;
        btn.classList.add('open');
        var m = getMenu();
        m.innerHTML = '<div class="playlist-menu-title">' + labels().loading + '</div>';
        m.hidden = false;
        placeMenu();
        fetch('/playlists/song/' + encodeURIComponent(songId))
            .then(function (r) { if (!r.ok) { throw new Error(r.status); } return r.json(); })
            .then(function (playlists) {
                if (menuSongId === songId) {
                    renderItems(songId, playlists);
                }
            })
            .catch(function () {
                closeMenu();
                showToast(labels().error, true);
            });
    };

    // Also used by the "Add to playlist" button of the song popup
    window.addSongToPlaylist = function (songId, playlistId, playlistName) {
        fetch('/playlist/' + encodeURIComponent(playlistId) + '/' + encodeURIComponent(songId), {method: 'PUT'})
            .then(function (r) {
                if (r.ok) {
                    showToast(format(labels().added, playlistName));
                } else if (r.status === 409) {
                    showToast(format(labels().already, playlistName));
                } else {
                    showToast(labels().error, true);
                }
            })
            .catch(function () { showToast(labels().error, true); });
    };

    // Click anywhere else, Escape key, resize: close the list.
    // Capture phase: also works when the click lands on an element that stops propagation (play button, star...)
    document.addEventListener('click', function (e) {
        if (!menu || menu.hidden) {
            return;
        }
        if (menu.contains(e.target) || (menuButton && menuButton.contains(e.target))) {
            return; // inside the list, or on its own "+" button (handled by togglePlaylistMenu)
        }
        closeMenu();
    }, true);
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            closeMenu();
        }
    });
    window.addEventListener('resize', closeMenu);
})();
