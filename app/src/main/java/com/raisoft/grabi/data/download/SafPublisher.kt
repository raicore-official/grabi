package com.raisoft.grabi.data.download

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.raisoft.grabi.domain.model.MediaKind
import java.io.File

object SafPublisher {

    fun publish(context: Context, file: File, kind: MediaKind, treeUri: Uri): Uri? = runCatching {
        val resolver = context.contentResolver
        val parent = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        val target = DocumentsContract.createDocument(
            resolver,
            parent,
            MediaStorePublisher.mimeFor(file.extension, kind),
            file.name,
        ) ?: error("createDocument returned null")
        resolver.openOutputStream(target)?.use { out -> file.inputStream().use { it.copyTo(out) } }
            ?: error("no output stream")
        file.delete()
        target
    }.getOrNull()
}