package com.error404.neunest.di

import com.error404.neunest.ChatViewModel
import com.error404.neunest.ExploreViewModel
import com.error404.neunest.Inference
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single {
        Inference(androidContext())
    }
    viewModelOf(::ExploreViewModel)
    viewModelOf(::ChatViewModel)
}