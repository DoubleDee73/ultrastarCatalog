package com.doubledee.ultrastar.models

import spock.lang.Specification

class LanguageEnumSpec extends Specification {
    def 'getLanguage should find a corresponding language by displayname'() {
        when:
        Language language = LanguageEnum.getLanguage(languageTag);

        then:
        language.displayLanguage == expDisp
        language.languageCode == expCode

        where:
        languageTag || expDisp    | expCode
        null        || 'ALL'      | ''
        'Englisch'  || 'English'  | 'en'
        'Hebräisch' || 'Hebrew'   | 'he'
        'Croate'    || 'Croatian' | 'hr'
        'Klingon'   || 'Other'    | 'other'
    }
}
