package ru.kot.sked.network.interfaces

import ru.kot.sked.Diary

abstract class BaseUrls {
    abstract fun getBaseUrl(diary: Diary): String
}