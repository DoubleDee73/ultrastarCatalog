/*
 * Top of the catalog: filter selection screens (Language, Decade, Tags, Playlists), sort menu,
 * favorites on/off and grid/list view. All of them write into the form #catalogForm and send it,
 * so the URL always describes the current selection.
 */
(function () {
    var openModal = null;       // filter screen currently open
    var initialSelection = [];  // its selection when it was opened (restored on cancel)

    function form() {
        return document.getElementById('catalogForm');
    }

    // Sends the form; an empty search field is left out of the URL
    function submitForm() {
        var search = document.getElementById('search');
        if (search && !search.value.trim()) {
            search.disabled = true;
        }
        form().submit();
    }

    function selectedValues(modal) {
        return Array.prototype.map.call(modal.querySelectorAll('.filter-tile.selected'), function (tile) {
            return tile.getAttribute('data-value');
        });
    }

    window.openFilter = function (param) {
        var modal = document.getElementById('filter-' + param);
        if (!modal) {
            return;
        }
        closeSortMenu();
        openModal = modal;
        initialSelection = selectedValues(modal);
        modal.hidden = false;
        document.body.classList.add('no-scroll');
    };

    // Close without applying: the selection goes back to what it was
    window.closeFilter = function () {
        if (!openModal) {
            return;
        }
        openModal.querySelectorAll('.filter-tile').forEach(function (tile) {
            tile.classList.toggle('selected', initialSelection.indexOf(tile.getAttribute('data-value')) !== -1);
        });
        openModal.hidden = true;
        openModal = null;
        document.body.classList.remove('no-scroll');
    };

    window.toggleTile = function (tile) {
        tile.classList.toggle('selected');
    };

    window.clearFilterSelection = function () {
        if (openModal) {
            openModal.querySelectorAll('.filter-tile.selected').forEach(function (tile) {
                tile.classList.remove('selected');
            });
        }
    };

    // Writes the chosen values into the hidden fields of the form, then reloads the catalog
    window.applyFilter = function () {
        if (!openModal) {
            return;
        }
        var param = openModal.getAttribute('data-param');
        var container = document.getElementById('hidden-' + param);
        container.innerHTML = '';
        selectedValues(openModal).forEach(function (value) {
            var input = document.createElement('input');
            input.type = 'hidden';
            input.name = param;
            input.value = value;
            container.appendChild(input);
        });
        submitForm();
    };

    // ---- Sort menu ----
    function closeSortMenu() {
        var menu = document.getElementById('sortMenu');
        if (menu) {
            menu.hidden = true;
        }
    }

    window.toggleSortMenu = function (event) {
        event.stopPropagation();
        var menu = document.getElementById('sortMenu');
        menu.hidden = !menu.hidden;
    };

    // Sort field (artist, title, language, year): the order (asc/desc) is kept
    window.chooseSort = function (sort) {
        var sortField = document.getElementById('f-sort');
        if (sortField.value === sort) {
            closeSortMenu();
            return;
        }
        sortField.value = sort;
        submitForm();
    };

    // Order button: ascending <-> descending
    window.toggleSortOrder = function () {
        var dirField = document.getElementById('f-dir');
        dirField.value = dirField.value === 'desc' ? 'asc' : 'desc';
        submitForm();
    };

    // ---- Favorites, view, clear ----
    window.toggleFavoritesFilter = function () {
        var fav = document.getElementById('f-fav');
        fav.disabled = !fav.disabled;
        submitForm();
    };

    window.toggleView = function () {
        var view = document.getElementById('f-view');
        view.disabled = !view.disabled;
        submitForm();
    };

    // Removes search and filters, keeps the sort and the view
    window.clearAllFilters = function (event) {
        event.preventDefault();
        var url = '/?sort=' + encodeURIComponent(document.getElementById('f-sort').value)
            + '&dir=' + encodeURIComponent(document.getElementById('f-dir').value);
        if (!document.getElementById('f-view').disabled) {
            url += '&view=list';
        }
        window.location.href = url;
    };

    // Favorites are stored in this browser: with "Favorites" on, hide the other songs
    function applyFavoritesFilter() {
        var fav = document.getElementById('f-fav');
        if (!fav || fav.disabled) {
            return;
        }
        var favorites;
        try {
            favorites = JSON.parse(localStorage.getItem('favorites')) || [];
        } catch (e) {
            favorites = [];
        }
        var visible = 0;
        document.querySelectorAll('.flip-card').forEach(function (card) {
            var inner = card.querySelector('.flip-card-inner');
            var keep = inner && favorites.indexOf(inner.getAttribute('data-songid')) !== -1;
            card.style.display = keep ? '' : 'none';
            if (keep) { visible++; }
        });
        document.querySelectorAll('tr.songlist[data-songid]').forEach(function (row) {
            var keep = favorites.indexOf(row.getAttribute('data-songid')) !== -1;
            row.style.display = keep ? '' : 'none';
            if (keep) { visible++; }
        });
        var count = document.getElementById('resultCount');
        if (count && window.catalogLabels) {
            count.textContent = window.catalogLabels.found.replace('{0}', visible);
        }
        var empty = document.getElementById('emptyState');
        if (empty) {
            empty.hidden = visible > 0;
        }
        var table = document.querySelector('table.songlist');
        if (table) {
            table.style.display = visible > 0 ? '' : 'none';
        }
    }

    document.addEventListener('DOMContentLoaded', function () {
        applyFavoritesFilter();
        // Search with the Enter key or the magnifier: same rule for the empty search field
        var f = form();
        if (f) {
            f.addEventListener('submit', function () {
                var search = document.getElementById('search');
                if (search && !search.value.trim()) {
                    search.disabled = true;
                }
            });
        }
    });

    // Click outside: close the sort menu; click on the dark background: close the filter screen
    document.addEventListener('click', function (e) {
        var menu = document.getElementById('sortMenu');
        if (menu && !menu.hidden && !menu.contains(e.target)) {
            closeSortMenu();
        }
        if (openModal && e.target === openModal) {
            window.closeFilter();
        }
    });
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            closeSortMenu();
            window.closeFilter();
        }
    });
})();
