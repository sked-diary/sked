package ru.kot.sked.components.changelog

import ru.kot.sked.R


class Changelog32 : Changelog() {
    override val versionName: Int
        get() = R.string.c32_version_name
    override val versionShortname: Int
        get() = R.string.c32_version_shortname
    override val shortDescription: Int
        get() = R.string.c32_description
    override val elements = null
}