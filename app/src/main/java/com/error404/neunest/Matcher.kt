package com.error404.neunest

enum class Type {
    TEXT, IMAGE, AUDIO
}

enum class Accelerator {
    ANY, CPU, GPU, NPU
}

enum class Vendor {
    GEMMA, QWEN, DEEPSEEK
}

data class DefaultConfig(
    val topK: Double,
    val topP: Double,
    val temperature: Double,
    val maxTokens: Int,
    val accelerators: List<Accelerator>
)

data class Model(
    val id: String,
    val name: String,
    val minDeviceMemoryInGb: Int,
    val types: List<Type>,
    val defaultConfig: DefaultConfig,
    val vendor: Vendor
)

val matcher = listOf(
    Model(
        id = "gemma-4-E2B-it.litertlm",
        name = "Gemma-4-E2B-it",
        minDeviceMemoryInGb = 8,
        types = listOf(Type.TEXT, Type.IMAGE, Type.AUDIO),
        defaultConfig = DefaultConfig(
            topK = 64.0,
            topP = 0.95,
            temperature = 1.0,
            maxTokens = 4000,
            accelerators = listOf(Accelerator.GPU, Accelerator.CPU)
        ),
        vendor = Vendor.GEMMA
    ),
    Model(
        id = "gemma-4-E4B-it.litertlm",
        name = "Gemma-4-E4B-it",
        minDeviceMemoryInGb = 12,
        types = listOf(Type.TEXT, Type.IMAGE, Type.AUDIO),
        defaultConfig = DefaultConfig(
            topK = 64.0,
            topP = 0.95,
            temperature = 1.0,
            maxTokens = 4000,
            accelerators = listOf(Accelerator.GPU, Accelerator.CPU)
        ),
        vendor = Vendor.GEMMA
    ),
    Model(
        id = "gemma-3n-E2B-it-int4.litertlm",
        name = "Gemma-3n-E2B-it",
        minDeviceMemoryInGb = 8,
        types = listOf(Type.TEXT, Type.IMAGE, Type.AUDIO),
        defaultConfig = DefaultConfig(
            topK = 64.0,
            topP = 0.95,
            temperature = 1.0,
            maxTokens = 4096,
            accelerators = listOf(Accelerator.CPU, Accelerator.GPU)
        ),
        vendor = Vendor.GEMMA
    ),
    Model(
        id = "gemma-3n-E4B-it-int4.litertlm",
        name = "Gemma-3n-E4B-it",
        minDeviceMemoryInGb = 12,
        types = listOf(Type.TEXT, Type.IMAGE, Type.AUDIO),
        defaultConfig = DefaultConfig(
            topK = 64.0,
            topP = 0.95,
            temperature = 1.0,
            maxTokens = 4096,
            accelerators = listOf(Accelerator.CPU, Accelerator.GPU)
        ),
        vendor = Vendor.GEMMA
    ),
    Model(
        id = "gemma3-1b-it-int4.litertlm",
        name = "Gemma3-1B-IT",
        minDeviceMemoryInGb = 6,
        types = listOf(Type.TEXT),
        defaultConfig = DefaultConfig(
            topK = 64.0,
            topP = 0.95,
            temperature = 1.0,
            maxTokens = 1024,
            accelerators = listOf(Accelerator.GPU, Accelerator.CPU)
        ),
        vendor = Vendor.GEMMA
    ),
    Model(
        id = "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv4096.litertlm",
        name = "Qwen2.5-1.5B-Instruct",
        minDeviceMemoryInGb = 6,
        types = listOf(Type.TEXT),
        defaultConfig = DefaultConfig(
            topK = 20.0,
            topP = 0.8,
            temperature = 0.7,
            maxTokens = 4096,
            accelerators = listOf(Accelerator.GPU, Accelerator.CPU)
        ),
        vendor = Vendor.QWEN
    ),
    Model(
        id = "DeepSeek-R1-Distill-Qwen-1.5B_multi-prefill-seq_q8_ekv4096.litertlm",
        name = "DeepSeek-R1-Distill-Qwen-1.5B",
        minDeviceMemoryInGb = 6,
        types = listOf(Type.TEXT),
        defaultConfig = DefaultConfig(
            topK = 64.0,
            topP = 0.95,
            temperature = 1.0,
            maxTokens = 4096,
            accelerators = listOf(Accelerator.GPU, Accelerator.CPU)
        ),
        vendor = Vendor.DEEPSEEK
    )
)