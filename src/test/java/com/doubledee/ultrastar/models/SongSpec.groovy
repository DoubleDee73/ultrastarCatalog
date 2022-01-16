package com.doubledee.ultrastar.models

import spock.lang.Specification

class SongSpec extends Specification {
    def 'should' () {
        given:
        Song song = new Song(1, title, 'some artist', 'mp3', 'path', 'text')

        expect:
        song.getFeaturedArtist() == artist

        where:
        title             || artist
        'test (feat. me)' || 'me'
    }

    def 'should also'() {
        expect:
        long test = Long.parseUnsignedLong('18446744073709551614')
        println test
        println Long.toUnsignedString(test)
    }
}
