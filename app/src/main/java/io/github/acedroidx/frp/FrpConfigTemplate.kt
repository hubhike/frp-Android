package io.github.acedroidx.frp

import android.content.Context
import java.util.Locale

/**
 * frp 配置模板元数据.
 */
data class FrpConfigTemplate(
    val type: FrpType,
    val assetPath: String,
    val fileName: String,
    val displayName: String,
)

/**
 * 加载 frp 配置模板，按系统语言优先选择对应目录，缺失时回落到英文模板。
 */
fun getFrpConfigTemplates(context: Context): Map<FrpType, List<FrpConfigTemplate>> {
    val assets = context.assets
    val preferredLocales = buildList {
        val currentLocale = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            context.resources.configuration.locales.get(0)
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        val languageTag = currentLocale.language.lowercase(Locale.ROOT)
        if (languageTag.startsWith("zh")) {
            add("zh-cn")
        }
        add("en")
    }

    val templates = mutableMapOf<FrpType, List<FrpConfigTemplate>>()
    FrpType.values().forEach { type ->
        var found: List<FrpConfigTemplate> = emptyList()
        for (localeKey in preferredLocales) {
            val dir = "examples/$localeKey/${type.typeName}"
            val fileNames = assets.list(dir)?.sorted().orEmpty()
            if (fileNames.isNotEmpty()) {
                found = fileNames.map { fileName ->
                    val assetPath = "$dir/$fileName"
                    val firstLine = assets.open(assetPath).bufferedReader().use { reader ->
                        reader.readLine()
                    } ?: fileName
                    val displayName = firstLine
                        .removePrefix("#")
                        .removePrefix("//")
                        .trim()
                        .ifEmpty { fileName }
                    FrpConfigTemplate(type, assetPath, fileName, displayName)
                }
                break
            }
        }
        templates[type] = found
    }
    return templates
}
