package com.doubledee.ultrastar.models

import spock.lang.Specification

class TagsEnumSpec extends Specification {
    def 'valueOf should return the respecitve TagsEnum'() {
        expect:
        TagsEnum.valueOf('DISNEY') == TagsEnum.DISNEY
        TagsEnum.valueOf('Hello') == TagsEnum.DISNEY
    }
}
