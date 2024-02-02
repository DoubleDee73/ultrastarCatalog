package com.doubledee.ultrastar.utils

import spock.lang.Specification

class HashUtilSpec extends Specification {
    def 'md5hash should hash a text'() {
        expect:
        HashUtil.sha1('Hello World') == '0a4d55a8d778e5022fab701'
        HashUtil.md5Hash('Hello World') == 'b10a8db164e0754105b7a99be72e3fe5'
    }
}
