package com.error404.neunest.di

import com.error404.neunest.ChatViewModel
import com.error404.neunest.ExploreViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    viewModelOf(::ExploreViewModel)
    viewModelOf(::ChatViewModel)
}